import { ApiResponse, ErrorCode } from "@/types/api";

const BASE_URL = process.env.NEXT_PUBLIC_API_URL || "";

export class ApiError extends Error {
  code: string;
  status: number;
  data?: any;

  constructor(message: string, code: string = "ERR_INTERNAL", status: number = 500, data?: any) {
    super(message);
    this.name = "ApiError";
    this.code = code;
    this.status = status;
    this.data = data;
  }
}

// Token storage helpers (localStorage with window check for SSR safety)
export const TokenStorage = {
  getAccessToken: (): string | null => {
    if (typeof window === "undefined") return null;
    return localStorage.getItem("b2b_access_token");
  },
  getRefreshToken: (): string | null => {
    if (typeof window === "undefined") return null;
    return localStorage.getItem("b2b_refresh_token");
  },
  setTokens: (accessToken: string, refreshToken: string) => {
    if (typeof window === "undefined") return;
    localStorage.setItem("b2b_access_token", accessToken);
    localStorage.setItem("b2b_refresh_token", refreshToken);
  },
  clearTokens: () => {
    if (typeof window === "undefined") return;
    localStorage.removeItem("b2b_access_token");
    localStorage.removeItem("b2b_refresh_token");
    localStorage.removeItem("b2b_user_profile");
  },
};

// Queue handling for concurrent 401s
let isRefreshing = false;
let refreshSubscribers: ((token: string) => void)[] = [];

function subscribeTokenRefresh(cb: (token: string) => void) {
  refreshSubscribers.push(cb);
}

function onTokenRefreshed(token: string) {
  refreshSubscribers.forEach((cb) => cb(token));
  refreshSubscribers = [];
}

function onRefreshFailed() {
  refreshSubscribers = [];
  TokenStorage.clearTokens();
  if (typeof window !== "undefined" && !window.location.pathname.startsWith("/login")) {
    window.location.href = "/login?expired=true";
  }
}

interface RequestOptions extends RequestInit {
  requiresAuth?: boolean;
  idempotent?: boolean;
}

export async function apiClient<T>(
  endpoint: string,
  options: RequestOptions = {}
): Promise<T> {
  const { requiresAuth = true, idempotent = false, headers = {}, ...rest } = options;

  const url = endpoint.startsWith("http") ? endpoint : `${BASE_URL}${endpoint}`;

  const requestHeaders = new Headers(headers);
  if (!requestHeaders.has("Content-Type") && !(rest.body instanceof FormData)) {
    requestHeaders.set("Content-Type", "application/json");
  }

  // Attach idempotency key if requested
  if (idempotent && !requestHeaders.has("X-Idempotency-Key")) {
    requestHeaders.set(
      "X-Idempotency-Key",
      `req-${Date.now()}-${Math.random().toString(36).substring(2, 9)}`
    );
  }

  // Attach Bearer Access Token
  const token = TokenStorage.getAccessToken();
  if (requiresAuth && token) {
    requestHeaders.set("Authorization", `Bearer ${token}`);
  }

  const response = await fetch(url, {
    ...rest,
    headers: requestHeaders,
  });

  // Handle 401 Unauthorized (Expired Access Token)
  if (response.status === 401 && requiresAuth) {
    const refreshToken = TokenStorage.getRefreshToken();
    if (!refreshToken) {
      onRefreshFailed();
      throw new ApiError("Vui lòng đăng nhập để tiếp tục", "ERR_UNAUTHORIZED", 401);
    }

    if (!isRefreshing) {
      isRefreshing = true;
      try {
        const refreshRes = await fetch(`${BASE_URL}/api/v1/auth/refresh`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ refreshToken }),
        });

        if (!refreshRes.ok) {
          throw new Error("Refresh token expired");
        }

        const refreshData: ApiResponse<{
          accessToken: string;
          refreshToken: string;
        }> = await refreshRes.json();

        const newAccessToken = refreshData.data.accessToken;
        const newRefreshToken = refreshData.data.refreshToken;
        TokenStorage.setTokens(newAccessToken, newRefreshToken);
        isRefreshing = false;
        onTokenRefreshed(newAccessToken);
      } catch (err) {
        isRefreshing = false;
        onRefreshFailed();
        throw new ApiError("Phiên đăng nhập đã hết hạn", "ERR_TOKEN_EXPIRED", 401);
      }
    }

    // Wait for the ongoing refresh to complete and retry this request
    return new Promise<T>((resolve, reject) => {
      subscribeTokenRefresh((newToken: string) => {
        requestHeaders.set("Authorization", `Bearer ${newToken}`);
        fetch(url, { ...rest, headers: requestHeaders })
          .then(async (retryRes) => {
            const json: ApiResponse<T> = await retryRes.json();
            if (!retryRes.ok || !json.success) {
              reject(new ApiError(json.message || "Request failed", json.code, retryRes.status, json.data));
            } else {
              resolve(json.data);
            }
          })
          .catch(reject);
      });
    });
  }

  let json: ApiResponse<T>;
  try {
    json = await response.json();
  } catch {
    throw new ApiError(
      `Máy chủ phản hồi trạng thái ${response.status}`,
      "ERR_INTERNAL",
      response.status
    );
  }

  if (!response.ok || !json.success) {
    throw new ApiError(
      json.message || "Thao tác không thành công",
      json.code || "ERR_INTERNAL",
      response.status,
      json.data
    );
  }

  return json.data;
}

// Convenience REST methods
export const api = {
  get: <T>(url: string, options?: RequestOptions) =>
    apiClient<T>(url, { ...options, method: "GET" }),

  post: <T>(url: string, body?: any, options?: RequestOptions) =>
    apiClient<T>(url, {
      ...options,
      method: "POST",
      body: body instanceof FormData ? body : JSON.stringify(body),
    }),

  put: <T>(url: string, body?: any, options?: RequestOptions) =>
    apiClient<T>(url, {
      ...options,
      method: "PUT",
      body: body instanceof FormData ? body : JSON.stringify(body),
    }),

  patch: <T>(url: string, body?: any, options?: RequestOptions) =>
    apiClient<T>(url, {
      ...options,
      method: "PATCH",
      body: body instanceof FormData ? body : JSON.stringify(body),
    }),

  delete: <T>(url: string, options?: RequestOptions) =>
    apiClient<T>(url, { ...options, method: "DELETE" }),
};
