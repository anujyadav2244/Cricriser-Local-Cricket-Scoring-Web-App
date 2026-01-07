import { Routes, Route } from "react-router-dom"

import Login from "@/pages/auth/Login"
import Register from "@/pages/auth/Register"
import VerifySignupOtp from "@/pages/auth/VerifySignupOtp"
import ForgotPasswordOtp from "@/pages/auth/ForgotPasswordOtp"

import AdminLayout from "@/components/admin-layout/AdminLayout"
import AdminDashboard from "@/pages/admin/AdminDashboard"
import CreateLeague from "@/pages/admin/CreateLeague"


export default function AppRoutes() {
  return (
    <Routes>
      {/* ================= PUBLIC ROUTES ================= */}
      <Route path="/login" element={<Login />} />
      <Route path="/signup" element={<Register />} />
      <Route path="/verify-otp" element={<VerifySignupOtp />} />
      <Route
        path="/forgot-password/verify-otp"
        element={<ForgotPasswordOtp />}
      />

      {/* ================= ADMIN ROUTES ================= */}
      <Route path="/admin" element={<AdminLayout />}>
        <Route path="dashboard" element={<AdminDashboard />} />
        <Route path="leagues/create" element={<CreateLeague />} />
        {/* future */}
        {/* <Route path="leagues" element={<Leagues />} /> */}
        {/* <Route path="teams" element={<Teams />} /> */}
      </Route>
    </Routes>
  )
}
