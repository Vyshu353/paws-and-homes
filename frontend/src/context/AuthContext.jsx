import React, { createContext, useContext, useState, useEffect } from "react";
import { apiRequest } from "../services/api.js";

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!localStorage.getItem("pet-adoption-token")) {
      setLoading(false);
      return;
    }
    apiRequest("/auth/me")
      .then(({ user: currentUser }) => setUser(currentUser))
      .catch(() => {
        localStorage.removeItem("pet-adoption-token");
        setUser(null);
      })
      .finally(() => setLoading(false));
  }, []);

  async function signIn(credentials) {
    const data = await apiRequest("/auth/login", {
      method: "POST",
      body: JSON.stringify(credentials),
    });
    localStorage.setItem("pet-adoption-token", data.token);
    setUser(data.user);
    return data.user;
  }

  async function signUp(details) {
    const data = await apiRequest("/auth/signup", {
      method: "POST",
      body: JSON.stringify(details),
    });
    localStorage.setItem("pet-adoption-token", data.token);
    setUser(data.user);
    return data.user;
  }

  function signOut() {
    localStorage.removeItem("pet-adoption-token");
    setUser(null);
  }

  return (
    <AuthContext.Provider value={{ user, loading, signIn, signUp, signOut }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used within AuthProvider");
  return context;
}
