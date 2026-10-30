import { Link, NavLink, useNavigate } from "react-router-dom";
import { useEffect, useState } from "react";
import { notificationApi } from "../services/storeService";
import { useAuth } from "../context/AuthContext";

export default function Navbar({ cartCount = 0 }) {
  const { user, isAuthenticated, isAdmin, logout } = useAuth();
  const navigate = useNavigate();
  const [unread, setUnread] = useState(0);

  useEffect(() => {
    if (!isAuthenticated) {
      setUnread(0);
      return;
    }

    let active = true;
    const load = () =>
      notificationApi.unreadCount()
        .then((response) => active && setUnread(Number(response.data?.count || 0)))
        .catch(() => active && setUnread(0));

    load();
    const timer = setInterval(load, 15000);
    return () => {
      active = false;
      clearInterval(timer);
    };
  }, [isAuthenticated]);

  const doLogout = () => {
    logout();
    navigate("/");
  };

  const submitSearch = (event) => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const query = String(form.get("query") || "").trim();
    navigate(query ? `/shop?q=${encodeURIComponent(query)}` : "/shop");
  };

  return (
    <header className="navbar">
      <div className="nav-top">
        <div className="nav-top-inner">
          <span>Save on everyday groceries. Fast local delivery.</span>
          <span>Secure online checkout · FreshCart</span>
        </div>
      </div>

      <div className="nav-inner">
        <Link className="brand" to="/" aria-label="FreshCart home">
          <span className="brand-mark" aria-hidden="true">F</span>
          <span>Fresh<span>Cart</span></span>
        </Link>

        <form className="nav-search" role="search" onSubmit={submitSearch}>
          <span className="nav-search-icon" aria-hidden="true">⌕</span>
          <label className="sr-only" htmlFor="nav-search-input">Search groceries</label>
          <input
            id="nav-search-input"
            name="query"
            placeholder="Search groceries, brands or categories"
            autoComplete="off"
          />
          <button type="submit">Search</button>
        </form>

        <div className="nav-actions">
          <span className="delivery-pill" aria-label="Delivery information">
            <span aria-hidden="true">⌁</span>
            <span>Local delivery</span>
          </span>
          {!isAuthenticated ? (
            <>
              <Link className="btn btn-ghost" to="/login">Login</Link>
              <Link className="btn btn-primary" to="/register">Register</Link>
            </>
          ) : (
            <>
              <span className="user-chip">{user?.username}</span>
              <button className="btn btn-ghost" onClick={doLogout}>Logout</button>
            </>
          )}
        </div>

        <nav className="nav-links" aria-label="Primary navigation">
          <NavLink to="/">Home</NavLink>
          <NavLink to="/shop">Shop Categories</NavLink>
          {isAuthenticated && <NavLink to="/cart">Cart <b className="count">{cartCount}</b></NavLink>}
          {isAuthenticated && !isAdmin && <NavLink to="/orders">My Orders</NavLink>}
          {isAuthenticated && <NavLink to="/wishlist">Wishlist</NavLink>}
          {isAuthenticated && <NavLink to="/notifications">Updates <b className="count">{unread}</b></NavLink>}
          {isAuthenticated && <NavLink to="/account">Account</NavLink>}
          {isAdmin && <NavLink to="/admin">Admin</NavLink>}
          <span className="nav-promo">⚡ Order online and shop in minutes</span>
        </nav>
      </div>
    </header>
  );
}
