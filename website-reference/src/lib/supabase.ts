import { createClient, type SupportedStorage } from "@supabase/supabase-js";

/**
 * Browser Supabase client — session persists in localStorage.
 *
 * IMPORTANT: storage methods must read window at call time (not once at
 * module init). Evaluating localStorage during SSR leaves storage=undefined
 * and users must sign in every visit.
 */
export const supabaseUrl =
  process.env.NEXT_PUBLIC_SUPABASE_URL?.trim() ||
  "https://placeholder.supabase.co";
export const supabaseAnonKey =
  process.env.NEXT_PUBLIC_SUPABASE_ANON_KEY?.trim() ||
  "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.placeholder";

export const STORAGE_KEY = "wt-academy-auth-v1";

/**
 * Checks if a JWT token is synthetic, placeholder, or has an invalid signature
 * that would fail cryptographic verification in PostgREST (causing "No suitable key or wrong key type").
 */
export function isCorruptOrSyntheticToken(token: unknown): boolean {
  if (typeof token !== "string" || !token.trim()) return true;
  const t = token.trim();
  if (
    t.includes("sig_") ||
    t.includes("c2lnX") ||
    t.startsWith("wt-token-") ||
    t.startsWith("mock-") ||
    t.includes("placeholder")
  ) {
    return true;
  }
  const parts = t.split(".");
  if (parts.length !== 3) return true;
  // If the signature part decodes to something starting with "sig_"
  if (parts[2].startsWith("c2lnX")) return true;
  return false;
}

/**
 * Purges corrupt or synthetic tokens from local storage while preserving student identity.
 */
export function cleanCorruptAuthTokens(): void {
  if (typeof window === "undefined") return;
  try {
    // 1. Inspect wt-academy-auth-v1
    const raw = window.localStorage.getItem(STORAGE_KEY);
    if (raw) {
      try {
        const parsed = JSON.parse(raw);
        if (parsed && typeof parsed === "object") {
          if (isCorruptOrSyntheticToken(parsed.access_token)) {
            // Keep local user profile so user stays logged in locally, but remove broken tokens
            delete parsed.access_token;
            delete parsed.refresh_token;
            window.localStorage.setItem(STORAGE_KEY, JSON.stringify(parsed));
          }
        }
      } catch {
        window.localStorage.removeItem(STORAGE_KEY);
      }
    }

    // 2. Inspect and clean any Supabase default keys (sb-*-auth-token)
    for (let i = window.localStorage.length - 1; i >= 0; i--) {
      const k = window.localStorage.key(i);
      if (k && k.startsWith("sb-") && k.endsWith("-auth-token")) {
        const item = window.localStorage.getItem(k);
        if (
          item &&
          (item.includes("c2lnX") ||
            item.includes("sig_") ||
            item.includes("wt-token-"))
        ) {
          window.localStorage.removeItem(k);
        }
      }
    }
  } catch {
    /* ignore */
  }
}

// Clean on module load if in browser
if (typeof window !== "undefined") {
  cleanCorruptAuthTokens();
}

/** Always touch localStorage at read/write time (safe on server + client). */
const authStorage: SupportedStorage = {
  getItem: (key) => {
    if (typeof window === "undefined") return null;
    try {
      const raw = window.localStorage.getItem(key);
      if (!raw) return null;
      if (key === STORAGE_KEY) {
        try {
          const parsed = JSON.parse(raw);
          // If access_token is synthetic/corrupt, don't pass it to Supabase client
          // because Supabase will attach it as Bearer header, triggering PostgREST "No suitable key"
          if (isCorruptOrSyntheticToken(parsed?.access_token)) {
            return null;
          }
        } catch {
          return null;
        }
      }
      return raw;
    } catch {
      return null;
    }
  },
  setItem: (key, value) => {
    if (typeof window === "undefined") return;
    try {
      window.localStorage.setItem(key, value);
    } catch {
      /* private mode / quota */
    }
  },
  removeItem: (key) => {
    if (typeof window === "undefined") return;
    try {
      window.localStorage.removeItem(key);
    } catch {
      /* ignore */
    }
  },
};

export const supabase = createClient(supabaseUrl, supabaseAnonKey, {
  auth: {
    persistSession: true,
    autoRefreshToken: true,
    detectSessionInUrl: true,
    flowType: "implicit",
    storage: authStorage,
    storageKey: STORAGE_KEY,
  },
});

let _anonClient: ReturnType<typeof createClient> | null = null;

/**
 * Isolated anonymous Supabase client that never reads or sends stored auth tokens.
 * Guaranteed to succeed on public/published rows without encountering "No suitable key or wrong key type".
 */
export function getAnonSupabaseClient() {
  if (!_anonClient) {
    _anonClient = createClient(supabaseUrl, supabaseAnonKey, {
      auth: {
        persistSession: false,
        autoRefreshToken: false,
      },
    });
  }
  return _anonClient;
}

export function isSupabaseConfigured(): boolean {
  const url = process.env.NEXT_PUBLIC_SUPABASE_URL?.trim() || "";
  return Boolean(url) && !url.includes("placeholder");
}

export type BrowserDeviceRegistration =
  | { ok: true }
  | { ok: false; reason: "device_already_registered" | "verification_failed" };

/** Registers this browser before the website accepts an authenticated session. */
export async function registerBrowserDevice(
  session: { access_token: string } | null | undefined
): Promise<BrowserDeviceRegistration> {
  if (typeof window === "undefined" || !session?.access_token) {
    return { ok: false, reason: "verification_failed" };
  }
  try {
    const storageKey = "wt-browser-installation-v1";
    let installationId = window.localStorage.getItem(storageKey);
    if (!installationId || !/^[0-9a-f-]{36}$/i.test(installationId)) {
      installationId = window.crypto.randomUUID();
      window.localStorage.setItem(storageKey, installationId);
    }
    const material = new TextEncoder().encode(window.location.origin + ":" + installationId);
    const digest = await window.crypto.subtle.digest("SHA-256", material);
    const deviceHash = Array.from(new Uint8Array(digest))
      .map((byte) => byte.toString(16).padStart(2, "0"))
      .join("");
    const response = await fetch(supabaseUrl + "/functions/v1/register-device", {
      method: "POST",
      headers: {
        apikey: supabaseAnonKey,
        Authorization: "Bearer " + session.access_token,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        device_hash: deviceHash,
        device_label: "Wisdom Tower website browser",
      }),
    });
    const payload = await response.json().catch(() => null);
    if (response.ok && payload?.ok === true) return { ok: true };
    if (response.status === 403 && payload?.reason === "device_already_registered") {
      return { ok: false, reason: "device_already_registered" };
    }
    return { ok: false, reason: "verification_failed" };
  } catch {
    return { ok: false, reason: "verification_failed" };
  }
}
export async function recoverSession() {
  try {
    const { data, error } = await supabase.auth.getSession();
    if (error) console.warn("[auth] getSession", error.message);
    // Proactively refresh if session exists but access token may be stale
    if (data.session) {
      const expiresAt = data.session.expires_at ?? 0;
      const soon = Math.floor(Date.now() / 1000) + 60;
      let activeSession = data.session;
      if (expiresAt < soon) {
        const { data: refreshed, error: refreshError } = await supabase.auth.refreshSession();
        if (refreshError || !refreshed.session) return null;
        activeSession = refreshed.session;
      }
      const deviceCheck = await registerBrowserDevice(activeSession);
      if (!deviceCheck.ok) {
        await supabase.auth.signOut({ scope: "local" });
        return null;
      }
      return activeSession;
    }
    return data.session;
  } catch (e) {
    console.warn("[auth] recoverSession", e);
    return null;
  }
}
