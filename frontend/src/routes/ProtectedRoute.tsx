import { Navigate, Outlet } from "react-router-dom";
import { isAuthenticated } from "../utils/Auth";

export default function ProtectedRoute() {
  return isAuthenticated() ? <Outlet /> : <Navigate to="/login" replace />;
}