import { useState } from "react"

import {
  Sheet,
  SheetContent,
} from "@/components/ui/sheet"
import { Button } from "@/components/ui/button"
import { Separator } from "@/components/ui/separator"
import {
  DropdownMenu,
  DropdownMenuTrigger,
  DropdownMenuContent,
  DropdownMenuItem,
} from "@/components/ui/dropdown-menu"

import {
  LayoutDashboard,
  Trophy,
  Users,
  CalendarDays,
  BarChart3,
  User,
  LogOut,
  Key,
  Plus,
  Pencil,
  Trash2,
} from "lucide-react"

import { useAuthStore } from "@/store/auth.store"

/* ===================== ADMIN SIDEBAR ===================== */

export default function AdminSidebar({ open, onClose }) {
  const logout = useAuthStore((s) => s.logout)

  const [leagueMenuOpen, setLeagueMenuOpen] = useState(false)

  const handleLogout = () => {
    logout()
    window.location.href = "/login"
  }

  return (
    <Sheet open={open} onOpenChange={onClose}>
      <SheetContent
        side="left"
        className="w-64 bg-slate-950 text-slate-200 border-r border-slate-800 p-0"
      >
        {/* ================= HEADER ================= */}
        <div className="h-14 flex items-center px-4 border-b border-slate-800">
          <span className="text-lg font-semibold tracking-wide text-white">
            CricRiser
          </span>
        </div>

        {/* ================= MAIN NAV ================= */}
        <nav className="flex flex-col gap-1 px-3 py-4">

          <SidebarItem
            icon={LayoutDashboard}
            label="Dashboard"
            href="/admin/dashboard"
          />

          {/* ========== LEAGUES (HOVER + CLICK MENU) ========== */}
          <DropdownMenu
            open={leagueMenuOpen}
            onOpenChange={setLeagueMenuOpen}
          >
            <div
              onMouseEnter={() => setLeagueMenuOpen(true)}
              onMouseLeave={() => setLeagueMenuOpen(false)}
            >
              <DropdownMenuTrigger asChild>
                <Button
                  variant="ghost"
                  className="w-full justify-start gap-3 text-slate-300 hover:bg-slate-800 hover:text-white"
                >
                  <Trophy className="h-4 w-4" />
                  Leagues
                </Button>
              </DropdownMenuTrigger>

              <DropdownMenuContent
                side="right"
                align="start"
                className="bg-slate-900 border border-slate-800 text-slate-200"
              >
                <DropdownMenuItem
                  onClick={() => (window.location.href = "/admin/leagues/create")}
                >
                  <Plus className="h-4 w-4 mr-2" />
                  Create League
                </DropdownMenuItem>

                <DropdownMenuItem
                  onClick={() => (window.location.href = "/admin/leagues")}
                >
                  <Pencil className="h-4 w-4 mr-2" />
                  Update League
                </DropdownMenuItem>

                <DropdownMenuItem
                  className="text-red-500 focus:text-red-500"
                  onClick={() => (window.location.href = "/admin/leagues")}
                >
                  <Trash2 className="h-4 w-4 mr-2" />
                  Delete League
                </DropdownMenuItem>
              </DropdownMenuContent>
            </div>
          </DropdownMenu>

          <SidebarItem icon={Users} label="Teams" href="/admin/teams" />
          <SidebarItem icon={CalendarDays} label="Matches" href="/admin/matches" />
          <SidebarItem icon={BarChart3} label="Points Table" href="/admin/points" />
          <SidebarItem icon={User} label="Players" href="/admin/players" />
        </nav>

        <Separator className="bg-slate-800" />

        {/* ================= USER ACTIONS ================= */}
        <nav className="flex flex-col gap-1 px-3 py-4">
          <SidebarItem icon={User} label="Profile" href="/admin/profile" />
          <SidebarItem icon={Key} label="Reset Password" href="/reset-password" />

          <Button
            variant="ghost"
            onClick={handleLogout}
            className="justify-start gap-3 text-red-500 hover:bg-slate-800 hover:text-red-500"
          >
            <LogOut className="h-4 w-4" />
            Logout
          </Button>
        </nav>
      </SheetContent>
    </Sheet>
  )
}

/* ===================== HELPER COMPONENT ===================== */

function SidebarItem({ icon: Icon, label, href }) {
  return (
    <Button
      variant="ghost"
      className="w-full justify-start gap-3 text-slate-300 hover:bg-slate-800 hover:text-white"
      onClick={() => (window.location.href = href)}
    >
      <Icon className="h-4 w-4" />
      {label}
    </Button>
  )
}
