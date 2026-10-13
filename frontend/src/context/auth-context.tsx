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
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<UserSummary | null>(null);
  const [profile, setProfile] = useState<B2bPartnerProfile | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const router = useRouter();

  const fetchCurrentUser = async () => {
    const token = TokenStorage.getAccessToken();
    if (!token) {
      setLoading(false);
      return;
    }

    try {
      // Fetch authenticated user profile
      const userData = await api.get<UserSummary>("/api/v1/auth/me");
      setUser(userData);

      // If user is a B2B partner, also fetch their partner profile
      if (userData.roleCode === "B2B_PARTNER") {
        try {
          const partnerData = await api.get<B2bPartnerProfile>("/api/v1/b2b/partner/profile");
          setProfile(partnerData);
        } catch {
          // Profile may be in partial onboarding state
        }
      }
    } catch (err) {
      console.warn("Failed to load user profile on startup", err);
      TokenStorage.clearTokens();
      setUser(null);
      setProfile(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchCurrentUser();
  }, []);

  const login = async (email: string, password: string): Promise<AuthResponse> => {
    const data = await api.post<AuthResponse>("/api/v1/auth/login", { email, password }, { requiresAuth: false });
    TokenStorage.setTokens(data.accessToken, data.refreshToken);
    setUser(data.user);

    if (data.user.roleCode === "B2B_PARTNER") {
      try {
        const partnerData = await api.get<B2bPartnerProfile>("/api/v1/b2b/partner/profile");
        setProfile(partnerData);
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
      setUser(null);
      setProfile(null);
      router.push("/login");
    }
  };

  const refreshProfile = async () => {
    await fetchCurrentUser();
  };

  const isStaff =
    user != null &&
    (user.roleCode === "ADMIN" ||
      user.roleCode === "MANAGER" ||
      user.roleCode === "SELLER");

  const isPartner = user != null && user.roleCode === "B2B_PARTNER";

  const isActivePartner =
    isPartner && user.accountStatus === "ACTIVE";

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
