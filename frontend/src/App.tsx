import { Routes, Route, Navigate, useNavigate } from "react-router-dom";

import { loginWithPassword, signup } from "./api/client";
import WebSocketTest from "./components/WebSocketTest";
import MainLayout from "./layouts/MainLayout";
import DashboardPage from "./pages/DashboardPage";
import EquipmentManagementPage from "./pages/EquipmentManagementPage";
import Login from "./pages/Login";
import PublicDashboardPage from "./pages/PublicDashboardPage";
import ProtectedRoute from "./routes/ProtectedRoute";
import PublicRoute from "./routes/PublicRoute";
import { loginWithLocalTestSession, loginWithToken } from "./utils/Auth";

function LoginPageWrapper() {
  const navigate = useNavigate();

  return (
    <Login
      onLogin={async ({ id, password }) => {
        try {
          const response = await loginWithPassword({ username: id, password });

          if (!response.success || !response.data) {
            alert(response.message ?? "Login failed.");
            return;
          }

          loginWithToken(response.data);
          navigate("/dashboard", { replace: true });
        } catch (error) {
          alert(error instanceof Error ? error.message : "An error occurred during login.");
        }
      }}
      onSignup={async (payload) => {
        const response = await signup(payload);

        if (!response.success) {
          throw new Error(response.message ?? "Sign up failed.");
        }
      }}
      onLocalTestLogin={() => {
        loginWithLocalTestSession();
        navigate("/dashboard", { replace: true });
      }}
    />
  );
}

export default function App() {
  return (
    <Routes>
      <Route path="/public/dashboards" element={<PublicDashboardPage />} />

      <Route element={<PublicRoute />}>
        <Route path="/login" element={<LoginPageWrapper />} />
      </Route>

      <Route element={<ProtectedRoute />}>
        <Route element={<MainLayout />}>
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/equipment" element={<EquipmentManagementPage />} />
          <Route path="/ws-test" element={<WebSocketTest />} />
        </Route>
      </Route>

      <Route path="*" element={<Navigate to="/dashboard" replace />} />
    </Routes>
  );
}
