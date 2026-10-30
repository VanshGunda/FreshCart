import axios from "axios";

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "http://localhost:8080",
  headers: { "Content-Type": "application/json" },
  timeout: 12000,
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem("freshcart_token");
  if (token) {
    config.headers = config.headers || {};
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export function getApiError(error) {
  if (!error?.response) return "Unable to connect to FreshCart backend.";
  const data = error.response.data;
  if (data?.validationErrors) {
    return Object.values(data.validationErrors).join(" • ");
  }
  return data?.message || data?.error || "Request failed.";
}

export default api;
