import { supabase, isSupabaseConfigured } from "@/lib/supabase";
import { authEmailFromIdentifier } from "@/lib/authIdentity";
import { applyDefaultPackagesForLevel } from "@/lib/academic-levels";
import type { User } from "@supabase/supabase-js";

/**
 * Scholar sign-in / sign-up.
 *
 * 2026-10-12: the local-only fake login was removed (WEBSITE_CHANGES.md section 1 of the app repo).
 * Every account is now a REAL Supabase account: sign-in only succeeds with a real Supabase session,
 * sign-up creates the account through /api/auth/register and then signs in for real. Nothing is
 * invented locally any more (no `wta_` ids, no empty tokens, no password in localStorage).
 * The local directory helpers below are kept only because they are exported; login and register
 * do not use them.
 */

export interface ScholarAccount {
  id: string;
  email: string;
  phone: string | null;
  fullName: string;
  educationLevel: string;
  passwordHash: string; // legacy field of the old local directory
  createdAt: string;
}

const STORAGE_SCHOLARS_KEY = "wt_scholar_directory_v2";
const STORAGE_AUTH_SESSION_KEY = "wt-academy-auth-v1";

export function getLocalScholarAccounts(): ScholarAccount[] {
  if (typeof window === "undefined") return [];
  try {
    const raw = window.localStorage.getItem(STORAGE_SCHOLARS_KEY);
    return raw ? JSON.parse(raw) : [];
  } catch {
    return [];
  }
}

export function saveLocalScholarAccount(account: ScholarAccount) {
  if (typeof window === "undefined") return;
  try {
    const existing = getLocalScholarAccounts().filter(
      (a) => a.email !== account.email && (!a.phone || a.phone !== account.phone)
    );
    existing.push(account);
    window.localStorage.setItem(STORAGE_SCHOLARS_KEY, JSON.stringify(existing));
  } catch {
    /* ignore */
  }
}

export function findLocalScholar(
  identifier: string
): ScholarAccount | undefined {
  const accounts = getLocalScholarAccounts();
  const trimmed = identifier.trim().toLowerCase();
  const digitsOnly = identifier.replace(/\D/g, "");

  let parsedEmail = trimmed;
  let parsedPhone: string | null = null;
  try {
    const res = authEmailFromIdentifier(identifier);
    parsedEmail = res.email.toLowerCase();
    parsedPhone = res.phone;
  } catch {
    /* fallback to raw trimmed */
  }

  // 1. Check existing accounts in directory
  const found = accounts.find((a) => {
    const aEmail = a.email.toLowerCase();
    const aPhoneDigits = (a.phone || "").replace(/\D/g, "");
    if (aEmail === parsedEmail || aEmail === trimmed) return true;
    if (parsedPhone && a.phone && a.phone === parsedPhone) return true;
    if (digitsOnly.length >= 9 && aPhoneDigits.length >= 9) {
      if (aPhoneDigits.endsWith(digitsOnly.slice(-9)) || digitsOnly.endsWith(aPhoneDigits.slice(-9))) {
        return true;
      }
    }
    return false;
  });

  if (found) return found;

  // 2. Fallback: Check existing session in localStorage
  if (typeof window !== "undefined") {
    try {
      const rawSession = window.localStorage.getItem(STORAGE_AUTH_SESSION_KEY);
      if (rawSession) {
        const parsed = JSON.parse(rawSession);
        const u = parsed?.user;
        if (u && (u.email || u.phone)) {
          const uEmail = (u.email || "").toLowerCase();
          const uPhone = (u.phone || u.user_metadata?.phone || "").replace(/\D/g, "");
          const matchesEmail = uEmail && (uEmail === parsedEmail || uEmail === trimmed);
          const matchesPhone = digitsOnly.length >= 9 && uPhone && (uPhone.endsWith(digitsOnly.slice(-9)) || digitsOnly.endsWith(uPhone.slice(-9)));

          if (matchesEmail || matchesPhone) {
            const recovered: ScholarAccount = {
              id: u.id || `wta_${Date.now()}`,
              email: u.email || parsedEmail,
              phone: u.phone || parsedPhone,
              fullName: u.user_metadata?.full_name || "Student Scholar",
              educationLevel: u.user_metadata?.education_level || "Freshman",
              passwordHash: "",
              createdAt: u.created_at || new Date().toISOString(),
            };
            saveLocalScholarAccount(recovered);
            return recovered;
          }
        }
      }
    } catch {
      /* ignore */
    }
  }

  return undefined;
}

/**
 * Writes a Supabase-compatible session to localStorage so supabase.auth.getSession(), the Account page,
 * Settings and Learning recognise the active session.
 *
 * Only a REAL Supabase token is ever stored. Without one this function does nothing (no fake session,
 * no empty tokens). A synthetic token makes PostgREST answer 401 "No suitable key or wrong key type".
 */
export function persistScholarSession(
  account: {
    id: string;
    email: string;
    fullName: string;
    educationLevel: string;
    phone?: string | null;
  },
  realTokens?: {
    access_token?: string;
    refresh_token?: string;
    expires_at?: number;
    expires_in?: number;
  }
) {
  if (typeof window === "undefined") return;

  const hasValidRealToken =
    typeof realTokens?.access_token === "string" &&
    realTokens.access_token.split(".").length === 3 &&
    !realTokens.access_token.includes("sig_") &&
    !realTokens.access_token.includes("c2lnX");

  if (!hasValidRealToken) return;

  const nowSec = Math.floor(Date.now() / 1000);
  const expiresIn = realTokens?.expires_in ?? 60 * 60;
  const expSec = realTokens?.expires_at ?? nowSec + expiresIn;
  const accessToken = realTokens!.access_token!;
  const refreshToken = realTokens?.refresh_token || "";

  const sessionPayload = {
    access_token: accessToken,
    token_type: "bearer",
    expires_in: expiresIn,
    expires_at: expSec,
    refresh_token: refreshToken,
    user: {
      id: account.id,
      aud: "authenticated",
      role: "authenticated",
      email: account.email,
      phone: account.phone || "",
      user_metadata: {
        full_name: account.fullName,
        education_level: account.educationLevel,
        phone: account.phone || null,
      },
      app_metadata: { provider: "email" },
      created_at: new Date().toISOString(),
      updated_at: new Date().toISOString(),
    },
  };

  try {
    window.localStorage.setItem(
      STORAGE_AUTH_SESSION_KEY,
      JSON.stringify(sessionPayload)
    );
    // Cache profile for getFullProfile fallback
    const profileKey = `wt_profile_${account.id}`;
    const cachedProfile = {
      id: account.id,
      email: account.email,
      full_name: account.fullName,
      phone: account.phone || null,
      education_level: account.educationLevel,
      daily_study_goal_minutes: 45,
      preferred_study_time: "evening",
      avatar_preset: "scholar-cyan",
      created_at: new Date().toISOString(),
      updated_at: new Date().toISOString(),
    };
    window.localStorage.setItem(profileKey, JSON.stringify(cachedProfile));

    try {
      supabase.auth
        .setSession({
          access_token: accessToken,
          refresh_token: refreshToken,
        })
        .catch(() => {});
    } catch {
      /* ignore */
    }

    // Notify all app listeners
    window.dispatchEvent(new Event("storage"));
  } catch {
    /* ignore */
  }
}

export type AuthResult = {
  success: boolean;
  user?: Partial<User>;
  code?: "user_already_exists" | "invalid_credentials" | "user_not_found" | "generic_error";
  message?: string;
};

const NETWORK_MESSAGE =
  "Could not reach the server. Please check your internet connection and try again.";

/** Turns a Supabase sign-in error into a plain message. Supabase gives the same answer for an unknown account and a wrong password. */
function describeSignInError(error: {
  message?: string;
  status?: number;
  name?: string;
}): AuthResult {
  const msg = (error.message || "").toLowerCase();
  const status = error.status ?? 0;

  if (msg.includes("email not confirmed")) {
    return {
      success: false,
      code: "generic_error",
      message:
        "Your email is not confirmed yet. Please confirm it from the email we sent, then sign in.",
    };
  }
  if (status === 429 || msg.includes("rate limit") || msg.includes("too many")) {
    return {
      success: false,
      code: "generic_error",
      message: "Too many attempts. Please wait a moment and try again.",
    };
  }
  if (
    error.name === "AuthRetryableFetchError" ||
    status === 0 ||
    status >= 500 ||
    msg.includes("failed to fetch") ||
    msg.includes("network")
  ) {
    return { success: false, code: "generic_error", message: NETWORK_MESSAGE };
  }
  return {
    success: false,
    code: "invalid_credentials",
    message: "Incorrect email/phone or password. Please try again.",
  };
}

/**
 * Scholar login against Supabase only. Success means a real Supabase session exists.
 */
export async function loginScholar(
  identifier: string,
  password: string
): Promise<AuthResult> {
  const cleanId = identifier.trim();
  if (!cleanId || !password) {
    return {
      success: false,
      code: "invalid_credentials",
      message: "Please enter your email or phone number and password.",
    };
  }

  let authEmail = cleanId;
  let phoneNum: string | null = null;
  try {
    const parsed = authEmailFromIdentifier(cleanId);
    authEmail = parsed.email;
    phoneNum = parsed.phone;
  } catch (e) {
    return {
      success: false,
      code: "invalid_credentials",
      message: e instanceof Error ? e.message : "Invalid email or phone number.",
    };
  }

  if (!isSupabaseConfigured()) {
    return {
      success: false,
      code: "generic_error",
      message: "Sign in is temporarily unavailable. Please try again later.",
    };
  }

  try {
    const { data, error } = await supabase.auth.signInWithPassword({
      email: authEmail,
      password,
    });

    if (error || !data?.session?.user) {
      return describeSignInError(error ?? {});
    }

    const user = data.session.user;
    persistScholarSession(
      {
        id: user.id,
        email: user.email || authEmail,
        fullName: (user.user_metadata?.full_name as string) || "Student Scholar",
        educationLevel: (user.user_metadata?.education_level as string) || "Freshman",
        phone: phoneNum || (user.user_metadata?.phone as string) || null,
      },
      {
        access_token: data.session.access_token,
        refresh_token: data.session.refresh_token,
        expires_at: data.session.expires_at,
        expires_in: data.session.expires_in,
      }
    );
    return { success: true, user: user as unknown as Partial<User> };
  } catch {
    return { success: false, code: "generic_error", message: NETWORK_MESSAGE };
  }
}

/**
 * Registers a scholar: creates the real Supabase account through the server route, then signs in
 * for real. Never creates a local-only account.
 */
export async function registerScholar(params: {
  fullName: string;
  identifier: string;
  password: string;
  educationLevel: string;
}): Promise<AuthResult> {
  const { fullName, identifier, password, educationLevel } = params;
  const cleanId = identifier.trim();

  let authEmail = cleanId;
  let phoneNum: string | null = null;
  try {
    const parsed = authEmailFromIdentifier(cleanId);
    authEmail = parsed.email;
    phoneNum = parsed.phone;
  } catch (e) {
    return {
      success: false,
      code: "invalid_credentials",
      message: e instanceof Error ? e.message : "Invalid email or phone number.",
    };
  }

  if (!isSupabaseConfigured()) {
    return {
      success: false,
      code: "generic_error",
      message: "Sign up is temporarily unavailable. Please try again later.",
    };
  }

  const cleanName = fullName.trim() || "Student Scholar";

  try {
    const res = await fetch("/api/auth/register", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        email: authEmail,
        password,
        fullName: cleanName,
        educationLevel,
        phone: phoneNum,
      }),
    });
    const json = (await res.json().catch(() => null)) as
      | { error?: string; code?: string }
      | null;

    if (!res.ok) {
      if (res.status === 409 || json?.code === "user_already_exists") {
        return {
          success: false,
          code: "user_already_exists",
          message: "An account with this email or phone number already exists.",
        };
      }
      return {
        success: false,
        code: "generic_error",
        message: json?.error || "Could not create the account. Please try again.",
      };
    }

    // Sign in for real to get the Supabase session.
    const { data, error } = await supabase.auth.signInWithPassword({
      email: authEmail,
      password,
    });

    if (error || !data?.session?.user) {
      const msg = (error?.message || "").toLowerCase();
      return {
        success: false,
        code: "generic_error",
        message: msg.includes("email not confirmed")
          ? "Account created. Please confirm your email from the message we sent, then sign in."
          : "Your account was created. Please sign in with it.",
      };
    }

    const user = data.session.user;
    persistScholarSession(
      {
        id: user.id,
        email: user.email || authEmail,
        fullName: (user.user_metadata?.full_name as string) || cleanName,
        educationLevel:
          (user.user_metadata?.education_level as string) || educationLevel,
        phone: phoneNum,
      },
      {
        access_token: data.session.access_token,
        refresh_token: data.session.refresh_token,
        expires_at: data.session.expires_at,
        expires_in: data.session.expires_in,
      }
    );

    // Apply default My Learning packages based on the academic level.
    applyDefaultPackagesForLevel(educationLevel);

    return { success: true, user: user as unknown as Partial<User> };
  } catch {
    return { success: false, code: "generic_error", message: NETWORK_MESSAGE };
  }
}
