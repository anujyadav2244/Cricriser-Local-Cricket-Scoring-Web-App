import { useState } from "react"
import { Outlet } from "react-router-dom"
import AdminNavbar from "./AdminNavbar"
import AdminSidebar from "./AdminSidebar"

export default function AdminLayout() {
  const [sidebarOpen, setSidebarOpen] = useState(false)

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100">
      {/* Top Navbar */}
      <AdminNavbar onMenuClick={() => setSidebarOpen(true)} />

      {/* Sidebar */}
      <AdminSidebar
        open={sidebarOpen}
        onClose={setSidebarOpen}
      />

      {/* Page Content */}
      <main className="p-6">
        <Outlet />
      </main>
    </div>
  )
}
