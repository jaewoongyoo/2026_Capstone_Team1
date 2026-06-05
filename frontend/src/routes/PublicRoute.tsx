import { Navigate, Outlet } from "react-router-dom";
import { isAuthenticated } from "../utils/Auth";

export default function PublicRoute() {
  return isAuthenticated() ? <Navigate to="/dashboard" replace /> : <Outlet />;
}