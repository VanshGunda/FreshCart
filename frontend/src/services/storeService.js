import api from "./api";

const CATALOG_CACHE_TTL = 15_000;
const DETAIL_CACHE_TTL = 30_000;
const CART_CACHE_TTL = 2_000;

const catalogCache = new Map();
const detailCache = new Map();
let cartCache = null;
let cartPromise = null;

const stableParams = (params = {}) =>
  Object.fromEntries(
    Object.entries(params)
      .filter(([, value]) => value !== undefined && value !== null && value !== "")
      .sort(([a], [b]) => a.localeCompare(b)),
  );

export const productApi = {
  all: (config) => api.get("/product/all", config),
  catalog: (params = {}) => {
    const normalized = stableParams(params);
    const key = JSON.stringify(normalized);
    const now = Date.now();
    const cached = catalogCache.get(key);

    if (cached?.data && cached.expiresAt > now) {
      return Promise.resolve({ data: cached.data, cached: true });
    }
    if (cached?.promise) return cached.promise;

    const promise = api.get("/product/catalog", { params: normalized })
      .then((response) => {
        catalogCache.set(key, {
          data: response.data,
          expiresAt: Date.now() + CATALOG_CACHE_TTL,
        });
        return response;
      })
      .finally(() => {
        const current = catalogCache.get(key);
        if (current?.promise === promise) catalogCache.delete(key);
      });

    catalogCache.set(key, { promise });
    return promise;
  },
  invalidateCatalogCache: () => catalogCache.clear(),
  byId: (id) => {
    const key = String(id);
    const now = Date.now();
    const cached = detailCache.get(key);
    if (cached?.data && cached.expiresAt > now) {
      return Promise.resolve({ data: cached.data, cached: true });
    }
    if (cached?.promise) return cached.promise;

    const promise = api.get(`/product/${id}`)
      .then((response) => {
        detailCache.set(key, {
          data: response.data,
          expiresAt: Date.now() + DETAIL_CACHE_TTL,
        });
        return response;
      })
      .finally(() => {
        const current = detailCache.get(key);
        if (current?.promise === promise) detailCache.delete(key);
      });

    detailCache.set(key, { promise });
    return promise;
  },
  suggestions: (q) => api.get("/product/suggest", { params: { q } }),
  related: (id) => api.get(`/product/${id}/related`),
  search: (name, config) => api.get("/product/search", { ...config, params: { name } }),
  category: (name, config) => api.get("/product/category", { ...config, params: { name } }),
  brand: (name, config) => api.get("/product/brand", { ...config, params: { name } }),
  price: (min, max, config) => api.get("/product/price", { ...config, params: { min, max } }),
};

export const cartApi = {
  get: () => {
    const now = Date.now();
    if (cartCache && cartCache.expiresAt > now) return Promise.resolve({ data: cartCache.data, cached: true });
    if (cartPromise) return cartPromise;

    cartPromise = api.get("/cart/getC")
      .then((response) => {
        cartCache = { data: response.data, expiresAt: Date.now() + CART_CACHE_TTL };
        return response;
      })
      .finally(() => { cartPromise = null; });

    return cartPromise;
  },
  invalidateCache: () => { cartCache = null; },
  add: async (productId, quantity = 1) => {
    const response = await api.post(`/cart/add/${productId}`, null, { params: { quantity } });
    cartApi.invalidateCache();
    return response;
  },
  increase: async (itemId) => {
    const response = await api.put(`/cart/increase/${itemId}`);
    cartApi.invalidateCache();
    return response;
  },
  decrease: async (itemId) => {
    const response = await api.put(`/cart/decrease/${itemId}`);
    cartApi.invalidateCache();
    return response;
  },
  update: async (itemId, quantity) => {
    const response = await api.put(`/cart/item/${itemId}`, { quantity });
    cartApi.invalidateCache();
    return response;
  },
  remove: async (itemId) => {
    const response = await api.delete(`/cart/item/${itemId}`);
    cartApi.invalidateCache();
    return response;
  },
  clear: async (cartId) => {
    const response = await api.post(`/cart/clear/${cartId}`);
    cartApi.invalidateCache();
    return response;
  },
};

export const authApi = {
  register: (body) => api.post("/auth/reg", body),
  verifyOtp: (body) => api.post("/auth/verify-otp", body),
  login: (body) => api.post("/auth/login", body),
};

export const userApi = {
  profile: () => api.get("/user/profile"),
  byId: (id) => api.get(`/user/${id}`),
  updateProfile: (body) => api.put("/user/profile", body),
  changePassword: (body) => api.put("/user/password", body),
};

export const addressApi = {
  list: () => api.get("/user/addresses"),
  create: (body) => api.post("/user/addresses", body),
  update: (id, body) => api.put(`/user/addresses/${id}`, body),
  remove: (id) => api.delete(`/user/addresses/${id}`),
  makeDefault: (id) => api.put(`/user/addresses/${id}/default`),
};

export const wishlistApi = {
  list: () => api.get("/wishlist"),
  contains: (id) => api.get(`/wishlist/${id}`),
  add: (id) => api.post(`/wishlist/${id}`),
  remove: (id) => api.delete(`/wishlist/${id}`),
};

export const reviewApi = {
  list: (productId) => api.get(`/product/${productId}/reviews`),
  summary: (productId) => api.get(`/product/${productId}/reviews/summary`),
  save: (productId, body) => api.post(`/product/${productId}/reviews`, body),
};

export const notificationApi = {
  list: () => api.get("/notifications"),
  unreadCount: () => api.get("/notifications/unread-count"),
  markRead: (id) => api.put(`/notifications/${id}/read`),
  markAllRead: () => api.put("/notifications/read-all"),
};

export const couponApi = {
  apply: (code) => api.post("/coupon/apply", { code }),
};

export const restockApi = {
  subscribe: (id) => api.post(`/product/${id}/restock-notify`),
  unsubscribe: (id) => api.delete(`/product/${id}/restock-notify`),
};

export const orderApi = {
  myOrders: () => api.get("/order/MyOrders"),
  byId: (id) => api.get(`/order/${id}`),
  tracking: (id) => api.get(`/order/${id}/tracking`),
  createRazorpayOrder: (body = {}) => api.post("/order/create-order", body),
  placeOrder: (body) => api.post("/order/place-order", body),
};

export const adminApi = {
  dashboard: () => api.get("/admin/dashboard"),
  products: () => api.get("/admin/products"),
  createProduct: async (body) => { const r = await api.post("/admin/products", body); productApi.invalidateCatalogCache(); return r; },
  updateProduct: async (id, body) => { const r = await api.put(`/admin/products/${id}`, body); productApi.invalidateCatalogCache(); detailCache.delete(String(id)); return r; },
  deleteProduct: async (id) => { const r = await api.delete(`/admin/products/${id}`); productApi.invalidateCatalogCache(); detailCache.delete(String(id)); return r; },
  users: () => api.get("/admin/users"),
  deleteUser: (id) => api.delete(`/admin/users/${id}`),
  orders: () => api.get("/admin/orders"),
  lowStock: (threshold = 10) => api.get("/admin/inventory/low-stock", { params: { threshold } }),
  coupons: () => api.get("/admin/coupons"),
  createCoupon: (body) => api.post("/admin/coupons", body),
  updateCoupon: (id, body) => api.put(`/admin/coupons/${id}`, body),
  deleteCoupon: (id) => api.delete(`/admin/coupons/${id}`),
  updateOrderStatus: (id, status) => api.put(`/admin/orders/${id}/status`, { status }),
  reviews: (productId) => api.get(`/admin/reviews/${productId}`),
  deleteReview: (id) => api.delete(`/admin/reviews/${id}`),
};
