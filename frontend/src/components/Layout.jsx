import { Link, Outlet } from "react-router-dom";
import { useCallback, useEffect, useState } from "react";
import Navbar from "./Navbar";
import { cartApi } from "../services/storeService";
import { useAuth } from "../context/AuthContext";

export default function Layout() {
  const { isAuthenticated } = useAuth();
  const [cartCount, setCartCount] = useState(0);

  const refreshCart = useCallback(async () => {
    if (!isAuthenticated) {
      setCartCount(0);
      return;
    }
    try {
      const response = await cartApi.get();
      setCartCount((response.data?.items || []).reduce((sum, item) => sum + Number(item.quantity || 0), 0));
    } catch {
      setCartCount(0);
    }
  }, [isAuthenticated]);

  useEffect(() => {
    refreshCart();
  }, [refreshCart]);

  return (
    <>
      <Navbar cartCount={cartCount} />
      <main><Outlet context={{ refreshCart }} /></main>
      <footer className="footer">
        <div className="footer-grid">
          <div className="footer-brand-block">
            <div className="footer-brand">Fresh<span>Cart</span></div>
            <p>Fresh groceries, simple discovery and a checkout you can trust.</p>
          </div>
          <div>
            <h3>Shop</h3>
            <div className="footer-links">
              <Link to="/shop">All groceries</Link>
              <Link to="/shop?category=Fruits">Fruits</Link>
              <Link to="/shop?category=Vegetables">Vegetables</Link>
              <Link to="/shop?category=Dairy">Dairy</Link>
            </div>
          </div>
          <div>
            <h3>Customer</h3>
            <div className="footer-links">
              <Link to={isAuthenticated ? "/orders" : "/login"}>Orders</Link>
              <Link to={isAuthenticated ? "/account" : "/login"}>Account</Link>
              <Link to={isAuthenticated ? "/wishlist" : "/login"}>Wishlist</Link>
              <Link to="/shop">Help &amp; discovery</Link>
            </div>
          </div>
          <div>
            <h3>FreshCart</h3>
            <div className="footer-links">
              <span>Secure Razorpay checkout</span>
              <span>Server-verified orders</span>
              <span>Responsive shopping</span>
              <span>© {new Date().getFullYear()} FreshCart</span>
            </div>
          </div>
        </div>
        <div className="footer-bottom">Prices, availability and order status are always confirmed by the FreshCart server.</div>
      </footer>
    </>
  );
}
