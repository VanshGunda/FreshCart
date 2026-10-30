import { Component, lazy, Suspense } from "react";
import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { AuthProvider } from "./context/AuthContext";
import Layout from "./components/Layout";
import ProtectedRoute from "./components/ProtectedRoute";
import NotFound from "./pages/NotFound";

const Home = lazy(() => import("./pages/Home"));
const Shop = lazy(() => import("./pages/Shop"));
const ProductDetails = lazy(() => import("./pages/ProductDetails"));
const Login = lazy(() => import("./pages/Login"));
const Register = lazy(() => import("./pages/Register"));
const VerifyOtp = lazy(() => import("./pages/VerifyOtp"));
const Cart = lazy(() => import("./pages/Cart"));
const Checkout = lazy(() => import("./pages/Checkout"));
const Orders = lazy(() => import("./pages/Orders"));
const OrderSuccess = lazy(() => import("./pages/OrderSuccess"));
const Account = lazy(() => import("./pages/Account"));
const Wishlist = lazy(() => import("./pages/Wishlist"));
const Notifications = lazy(() => import("./pages/Notifications"));
const OrderDetails = lazy(() => import("./pages/OrderDetails"));
const Forbidden = lazy(() => import("./pages/Forbidden"));
const Admin = lazy(() => import("./components/Admin"));

class AppErrorBoundary extends Component {
  state = { hasError: false };

  static getDerivedStateFromError() {
    return { hasError: true };
  }

  componentDidCatch(error, info) {
    if (import.meta.env.DEV) console.error("FreshCart UI error", error, info);
  }

  reset = () => {
    this.setState({ hasError: false });
    window.location.reload();
  };

  render() {
    if (this.state.hasError) {
      return <main className="container section">
        <div className="panel error-box error-boundary" role="alert">
          <h1>Something went wrong</h1>
          <p>FreshCart hit an unexpected display error. Your account and cart data are kept on the server.</p>
          <button className="btn btn-primary" onClick={this.reset}>Reload FreshCart</button>
        </div>
      </main>;
    }
    return this.props.children;
  }
}

function RouteFallback() {
  return <div className="container section"><div className="panel route-loading" role="status">Loading FreshCart…</div></div>;
}

function AppRoutes() {
  return <Suspense fallback={<RouteFallback />}>
    <Routes>
      <Route element={<Layout />}>
        <Route path="/" element={<Home />} />
        <Route path="/shop" element={<Shop />} />
        <Route path="/product/:id" element={<ProductDetails />} />
        <Route path="/login" element={<Login />} />
        <Route path="/register" element={<Register />} />
        <Route path="/verify-otp" element={<VerifyOtp />} />
        <Route path="/forbidden" element={<Forbidden />} />
        <Route element={<ProtectedRoute />}>
          <Route path="/cart" element={<Cart />} />
          <Route path="/checkout" element={<Checkout />} />
          <Route path="/orders" element={<Orders />} />
          <Route path="/order-success/:id" element={<OrderSuccess />} />
          <Route path="/account" element={<Account />} />
          <Route path="/wishlist" element={<Wishlist />} />
          <Route path="/notifications" element={<Notifications />} />
          <Route path="/orders/:id" element={<OrderDetails />} />
        </Route>
        <Route element={<ProtectedRoute admin />}>
          <Route path="/admin" element={<Admin />} />
        </Route>
        <Route path="/Checkout" element={<Navigate to="/checkout" replace />} />
        <Route path="/MyOrders" element={<Navigate to="/orders" replace />} />
        <Route path="/wishlist-page" element={<Navigate to="/wishlist" replace />} />
        <Route path="*" element={<NotFound />} />
      </Route>
    </Routes>
  </Suspense>;
}

export default function App() {
  return <AuthProvider><BrowserRouter><AppErrorBoundary><AppRoutes /></AppErrorBoundary></BrowserRouter></AuthProvider>;
}
