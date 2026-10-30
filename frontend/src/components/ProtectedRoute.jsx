import { Navigate, Outlet, useLocation, useOutletContext } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function ProtectedRoute({ admin = false }) {
  const { isAuthenticated, isAdmin } = useAuth();
  const location = useLocation();
  const layoutContext = useOutletContext();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  }

  if (admin && !isAdmin) {
    return <Navigate to="/forbidden" replace />;
  }

  // Preserve the context supplied by Layout so protected pages such as
  // Cart and Checkout can access shared actions like refreshCart().
  return <Outlet context={layoutContext} />;
}
