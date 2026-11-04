"use client";

import { useEffect } from "react";
import { supabase, recoverSession } from "@/lib/supabase";
import { clearOwnershipCache } from "@/lib/ownership";
import { initFcmPushBackground } from "@/lib/fcm-client";
import { clearAllSwrCache, setActiveUserId, setCachedAuthUser } from "@/lib/swr-cache";

/**
 * Keeps auth session alive across tab close / return.
 */
export default function AuthProvider({ children }: { children: React.ReactNode }) {
  useEffect(() => {
    void recoverSession().then((session) => {
      if (session?.user) {
        setCachedAuthUser(session.user);
        setActiveUserId(session.user.id);
        void initFcmPushBackground(session.user.id);
      } else {
        setCachedAuthUser(null);
        setActiveUserId(null);
        void initFcmPushBackground();
      }
    });

    const {
      data: { subscription },
    } = supabase.auth.onAuthStateChange((event, session) => {
      if (event === "SIGNED_OUT") {
        clearAllSwrCache();
        clearOwnershipCache();
        setCachedAuthUser(null);
        setActiveUserId(null);
      } else if (
        event === "SIGNED_IN" ||
        event === "TOKEN_REFRESHED" ||
        event === "USER_UPDATED"
      ) {
        clearOwnershipCache();
        if (session?.user) {
          setCachedAuthUser(session.user);
          setActiveUserId(session.user.id);
        }
        if (event === "SIGNED_IN" && session?.user?.id) {
          void initFcmPushBackground(session.user.id);
        }
      }
    });

    const onVisible = () => {
      if (document.visibilityState === "visible") void recoverSession();
    };

    document.addEventListener("visibilitychange", onVisible);
    window.addEventListener("focus", () => void recoverSession());

    // Periodic refresh while tab is open (helps long sessions)
    const interval = window.setInterval(() => {
      void recoverSession();
    }, 4 * 60 * 1000);

    return () => {
      subscription.unsubscribe();
      document.removeEventListener("visibilitychange", onVisible);
      window.clearInterval(interval);
    };
  }, []);

  return <>{children}</>;
}
