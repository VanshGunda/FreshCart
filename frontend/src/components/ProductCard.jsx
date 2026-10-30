import { memo, useState } from "react";
import { useNavigate } from "react-router-dom";
import { cartApi, wishlistApi } from "../services/storeService";
import { getApiError } from "../services/api";
import { useAuth } from "../context/AuthContext";

const imageFallbacks = {
  Fruits: "https://images.unsplash.com/photo-1571771894821-ce9b6c11b08e?auto=format&fit=crop&w=480&q=72",
  Vegetables: "https://images.unsplash.com/photo-1546094096-0df4bcaaa337?auto=format&fit=crop&w=480&q=72",
  Dairy: "https://images.unsplash.com/photo-1550583724-b2692b85b150?auto=format&fit=crop&w=480&q=72",
  Bakery: "https://images.unsplash.com/photo-1509440159596-0249088772ff?auto=format&fit=crop&w=480&q=72",
  Staples: "https://images.unsplash.com/photo-1586201375761-83865001e31c?auto=format&fit=crop&w=480&q=72",
  Beverages: "https://images.unsplash.com/photo-1600271886742-f049cd451bba?auto=format&fit=crop&w=480&q=72",
};

function optimizeImageUrl(url, category) {
  if (!url) return imageFallbacks[category] || "/placeholder.svg";
  try {
    const parsed = new URL(url, window.location.origin);
    const isDeadLocal = ["localhost", "127.0.0.1"].includes(parsed.hostname)
      || ["7070", "7071", "37857"].includes(parsed.port);
    if (isDeadLocal) return imageFallbacks[category] || "/placeholder.svg";
    if (parsed.hostname.includes("images.unsplash.com")) {
      parsed.searchParams.set("auto", "format");
      parsed.searchParams.set("fit", "crop");
      parsed.searchParams.set("w", "480");
      parsed.searchParams.set("q", "72");
    }
    return parsed.toString();
  } catch {
    return imageFallbacks[category] || "/placeholder.svg";
  }
}

function ProductCard({ product, onCartChange }) {
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();
  const [adding, setAdding] = useState(false);
  const [message, setMessage] = useState("");
  const [saved, setSaved] = useState(false);
  const [imageUrl, setImageUrl] = useState(() => optimizeImageUrl(product.imageUrl, product.category));

  const toggleWishlist = async (event) => {
    event.stopPropagation();
    if (!isAuthenticated) { navigate("/login", { state: { from: "/shop" } }); return; }
    try {
      if (saved) { await wishlistApi.remove(product.id); setSaved(false); setMessage("Removed from wishlist"); }
      else { await wishlistApi.add(product.id); setSaved(true); setMessage("Saved to wishlist"); }
    } catch (error) { setMessage(getApiError(error)); }
  };

  const add = async () => {
    if (!isAuthenticated) { navigate("/login", { state: { from: "/shop" } }); return; }
    setAdding(true);
    setMessage("");
    try {
      await cartApi.add(product.id, 1);
      setMessage("Added");
      onCartChange?.();
    } catch (error) {
      setMessage(getApiError(error));
    } finally {
      setAdding(false);
    }
  };

  return <article className="product-card">
    <button className="product-image" onClick={() => navigate(`/product/${product.id}`)} aria-label={`View ${product.name}`}>
      <img src={imageUrl} alt={product.name} width="320" height="320" loading="lazy" decoding="async" onError={() => setImageUrl(imageFallbacks[product.category] || "/placeholder.svg")} />
      <span className="chip chip-category">{product.category}</span>
      {product.stock <= 0 && <span className="badge badge-danger">Out of stock</span>}
    </button>
    <button type="button" className="wishlist-fab" onClick={toggleWishlist} aria-label={saved ? `Remove ${product.name} from wishlist` : `Save ${product.name} to wishlist`} aria-pressed={saved}>{saved ? "♥" : "♡"}</button>
    <div className="product-body">
      <div className="product-meta">{product.category}</div>
      <h3>{product.name}</h3>
      <p className="muted">{product.brand} · {product.unit}</p>
      <div className="product-bottom">
        <strong>₹{Number(product.price).toFixed(2)}</strong>
        <button className="add-fab" onClick={add} disabled={adding || product.stock <= 0} aria-label={adding ? `Adding ${product.name} to cart` : `Add ${product.name} to cart`}>{adding ? "…" : "+"}</button>
      </div>
      {message && <div role="status" className={/Added|Saved|Removed/.test(message) ? "success-text" : "error-text"}>{message}</div>}
    </div>
  </article>;
}

export default memo(ProductCard);
