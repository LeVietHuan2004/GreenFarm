"use client";

import { create } from "zustand";
import { createJSONStorage, persist } from "zustand/middleware";

import type { AuthSession, User } from "@/types/auth";

type AuthState = {
  token: string | null;
  user: User | null;
  hasHydrated: boolean;
  setSession: (session: AuthSession) => void;
  setUser: (user: User) => void;
  clearSession: () => void;
  markHydrated: () => void;
};

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      token: null,
      user: null,
      hasHydrated: false,
      setSession: (session) =>
        set({ token: session.accessToken, user: session.user }),
      setUser: (user) => set({ user }),
      clearSession: () => set({ token: null, user: null }),
      markHydrated: () => set({ hasHydrated: true })
    }),
    {
      name: "greenfarm-auth",
      storage: createJSONStorage(() => localStorage),
      partialize: (state) => ({ token: state.token, user: state.user }),
      onRehydrateStorage: () => (state) => state?.markHydrated()
    }
  )
);
