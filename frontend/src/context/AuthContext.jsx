import { createContext, useContext, useMemo, useState } from "react";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [token, setToken] = useState(() => localStorage.getItem("freshcart_token"));
  const [user, setUser] = useState(() => {
    try { return JSON.parse(localStorage.getItem("freshcart_user") || "null"); }
    catch { return null; }
  });

  const login = (loginResponse) => {
    localStorage.setItem("freshcart_token", loginResponse.token);
    localStorage.setItem("freshcart_user", JSON.stringify(loginResponse.user));
    setToken(loginResponse.token);
    setUser(loginResponse.user);
  };

  const logout = () => {
    localStorage.removeItem("freshcart_token");
    localStorage.removeItem("freshcart_user");
    setToken(null);
    setUser(null);
  };

  const value = useMemo(() => ({ token, user, login, logout, isAuthenticated: Boolean(token), isAdmin: user?.role === "ADMIN" }), [token, user]);
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const value = useContext(AuthContext);
  if (!value) throw new Error("useAuth must be used within AuthProvider");
  return value;
}
