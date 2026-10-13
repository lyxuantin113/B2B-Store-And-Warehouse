"use client";

import React, { createContext, useContext, useEffect, useState } from "react";
import { AuthResponse, B2bPartnerProfile, UserSummary } from "@/types/auth";
import { api, TokenStorage } from "@/lib/api-client";
import { useRouter } from "next/navigation";

interface AuthContextType {
  user: UserSummary | null;
  profile: B2bPartnerProfile | null;
  loading: boolean;
  isAuthenticated: boolean;
  isStaff: boolean;
  isPartner: boolean;
  isActivePartner: boolean;
  login: (email: string, password: string) => Promise<AuthResponse>;
  logout: () => Promise<void>;
  refreshProfile: () => Promise<void>;
  setUser: (user: UserSummary | null) => void;
  setAuthSession: (authData: AuthResponse) => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<UserSummary | null>(null);
  const [profile, setProfile] = useState<B2bPartnerProfile | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const router = useRouter();

  const setAuthSession = (data: AuthResponse) => {
    TokenStorage.setTokens(data.accessToken, data.refreshToken);
    const summary: UserSummary = {
      id: data.userId,
      email: data.email,
      fullName: data.fullName,
      roleCode: data.role,
      accountStatus: data.accountStatus,
      onboardingStep: data.onboardingStep,
      permissions: data.permissions || [],
    };
    setUser(summary);
    if (typeof window !== "undefined") {
      localStorage.setItem("b2b_user_summary", JSON.stringify(summary));
    }
  };

  const fetchProfileAndSession = async () => {
    const token = TokenStorage.getAccessToken();
    if (!token) {
      setLoading(false);
      return;
    }

    try {
      const stored = localStorage.getItem("b2b_user_summary");
      if (stored) {
        setUser(JSON.parse(stored));
      }

      // If user is B2B Partner, fetch partner profile
      try {
        const p = await api.get<B2bPartnerProfile>("/api/v1/b2b/profile");
        setProfile(p);
      } catch {
        // May be in onboarding state (PENDING_ADMIN / REJECTED / ADMIN_APPROVED)
      }
    } catch (err) {
      console.warn("Could not load stored session", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProfileAndSession();
  }, []);

  const login = async (email: string, password: string): Promise<AuthResponse> => {
    const data = await api.post<AuthResponse>(
      "/api/v1/auth/login",
      { email, password },
      { requiresAuth: false }
    );
    setAuthSession(data);

    if (data.role === "B2B_PARTNER") {
      try {
        const p = await api.get<B2bPartnerProfile>("/api/v1/b2b/profile");
        setProfile(p);
      } catch {
        // Ignored
      }
    }

    return data;
  };

  const logout = async () => {
    const refreshToken = TokenStorage.getRefreshToken();
    try {
      if (refreshToken) {
        await api.post("/api/v1/auth/logout", { refreshToken }, { requiresAuth: false });
      }
    } catch {
      // Ignored
    } finally {
      TokenStorage.clearTokens();
      if (typeof window !== "undefined") {
        localStorage.removeItem("b2b_user_summary");
      }
      setUser(null);
      setProfile(null);
      router.push("/login");
    }
  };

  const refreshProfile = async () => {
    try {
      const p = await api.get<B2bPartnerProfile>("/api/v1/b2b/profile");
      setProfile(p);
    } catch (err) {
      console.warn("Failed to refresh profile", err);
    }
  };

  const isStaff =
    user != null &&
    (user.roleCode === "ADMIN" ||
      user.roleCode === "MANAGER" ||
      user.roleCode === "SELLER");

  const isPartner = user != null && user.roleCode === "B2B_PARTNER";

  const isActivePartner = isPartner && user.accountStatus === "ACTIVE";

  return (
    <AuthContext.Provider
      value={{
        user,
        profile,
        loading,
        isAuthenticated: !!user,
        isStaff,
        isPartner,
        isActivePartner,
        login,
        logout,
        refreshProfile,
        setUser,
        setAuthSession,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextType {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
}
