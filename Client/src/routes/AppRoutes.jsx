import { Routes, Route, Navigate } from "react-router-dom";

import Login from "@/pages/auth/Login";
import Register from "@/pages/auth/Register";
import VerifySignupOtp from "@/pages/auth/VerifySignupOtp";
import ForgotPasswordOtp from "@/pages/auth/ForgotPasswordOtp";
import VerifyForgotOtp from "@/pages/auth/VerifyForgotOtp";
import ResetPassword from "@/pages/auth/ResetPassword";

import AdminLayout from "@/components/admin-layout/AdminLayout";
import AdminDashboard from "@/pages/admin/AdminDashboard";

/* ================= LEAGUES ================= */
import CreateLeague from "@/pages/admin/League/CreateLeague";
import Leagues from "@/pages/admin/League/Leagues";
import LeagueDetails from "@/pages/admin/League/LeagueDetails";
import UpdateLeague from "@/pages/admin/League/UpdateLeague";
import DeleteLeague from "@/pages/admin/League/DeleteLeague";

/* ================= TEAMS ================= */
import ManageTeams from "@/pages/admin/Team/Teams";
import TeamDetails from "@/pages/admin/Team/TeamDetails";
import AddTeam from "@/pages/admin/Team/CreateTeam";

/* ================= AUTH GUARD ================= */
const ProtectedRoute = ({ children }) => {
  const token = localStorage.getItem("token");
  return token ? children : <Navigate to="/login" replace />;
};

export default function AppRoutes() {
  return (
    <Routes>
      {/* ================= PUBLIC ROUTES ================= */}
      <Route path="/login" element={<Login />} />
      <Route path="/signup" element={<Register />} />
      <Route path="/verify-otp" element={<VerifySignupOtp />} />

      {/* FORGOT PASSWORD FLOW */}
      <Route path="/forgot-password" element={<ForgotPasswordOtp />} />
      <Route path="/forgot-password/verify-otp" element={<VerifyForgotOtp />} />

      {/* ================= ADMIN (PROTECTED) ================= */}
      <Route
        path="/admin"
        element={
          <ProtectedRoute>
            <AdminLayout />
          </ProtectedRoute>
        }
      >
        <Route path="dashboard" element={<AdminDashboard />} />

        {/* 🔐 RESET PASSWORD (LOGGED IN ONLY) */}
        <Route path="reset-password" element={<ResetPassword />} />

        {/* -------- LEAGUES -------- */}
        <Route path="leagues" element={<Leagues />} />
        <Route path="leagues/create" element={<CreateLeague />} />
        <Route path="leagues/:id" element={<LeagueDetails />} />
        <Route path="leagues/update/:id" element={<UpdateLeague />} />
        <Route path="leagues/delete/:id" element={<DeleteLeague />} />

        {/* -------- TEAMS -------- */}
        <Route path="teams" element={<ManageTeams />} />
        <Route path="teams/:name" element={<TeamDetails />} />
        <Route path="leagues/:leagueId/teams/create" element={<AddTeam />} />
      </Route>
    </Routes>
  );
}
