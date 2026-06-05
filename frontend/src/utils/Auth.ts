const AUTH_KEY = "isLoggedIn";
const AUTH_ROLE_KEY = "userRole";
const AUTH_USER_ID_KEY = "userId";
const AUTH_USERNAME_KEY = "username";
const AUTH_ACCESS_TOKEN_KEY = "accessToken";
const AUTH_REFRESH_TOKEN_KEY = "refreshToken";
const AUTH_LOCAL_TEST_KEY = "localTestSession";
const DEFAULT_USER_ID = import.meta.env.VITE_USER_ID ?? "1";

export type UserRole = "admin" | "user";

const legacyLocalStorageKeys = [
  AUTH_KEY,
  AUTH_ROLE_KEY,
  AUTH_USER_ID_KEY,
  AUTH_USERNAME_KEY,
  AUTH_ACCESS_TOKEN_KEY,
  AUTH_REFRESH_TOKEN_KEY,
];

function clearLegacyLocalAuth() {
  legacyLocalStorageKeys.forEach((key) => {
    localStorage.removeItem(key);
  });
}

function clearSessionAuth() {
  sessionStorage.removeItem(AUTH_ACCESS_TOKEN_KEY);
  sessionStorage.removeItem(AUTH_REFRESH_TOKEN_KEY);
  sessionStorage.removeItem(AUTH_LOCAL_TEST_KEY);
}

export function login(_role: UserRole = "user", _userId = DEFAULT_USER_ID) {
  clearLegacyLocalAuth();
}

export function loginWithToken(payload: {
  accessToken: string;
  refreshToken: string;
  userId: number | string;
  username: string;
  role: string;
}) {
  clearLegacyLocalAuth();
  sessionStorage.setItem(AUTH_ACCESS_TOKEN_KEY, payload.accessToken);
  sessionStorage.setItem(AUTH_REFRESH_TOKEN_KEY, payload.refreshToken);
}

export function loginWithLocalTestSession() {
  clearLegacyLocalAuth();
  sessionStorage.setItem(AUTH_ACCESS_TOKEN_KEY, "local-test-token");
  sessionStorage.setItem(AUTH_REFRESH_TOKEN_KEY, "local-test-refresh-token");
  sessionStorage.setItem(AUTH_LOCAL_TEST_KEY, "true");
}

export function logout() {
  clearLegacyLocalAuth();
  clearSessionAuth();
}

export function isLocalTestSession() {
  return import.meta.env.DEV && sessionStorage.getItem(AUTH_LOCAL_TEST_KEY) === "true";
}

export function isAuthenticated() {
  clearLegacyLocalAuth();
  return Boolean(sessionStorage.getItem(AUTH_ACCESS_TOKEN_KEY));
}

export function getUserRole(): UserRole {
  return "user";
}

export function getUserId() {
  return DEFAULT_USER_ID;
}

export function getUsername() {
  return "";
}

export function getAccessToken() {
  clearLegacyLocalAuth();
  return sessionStorage.getItem(AUTH_ACCESS_TOKEN_KEY) ?? undefined;
}

export function getRefreshToken() {
  clearLegacyLocalAuth();
  return sessionStorage.getItem(AUTH_REFRESH_TOKEN_KEY) ?? undefined;
}

export function isAdmin() {
  return false;
}
