import { Menu } from "lucide-react"
import { Button } from "@/components/ui/button"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { Avatar, AvatarFallback } from "@/components/ui/avatar"
import { useAuthStore } from "@/store/auth.store"

export default function AdminNavbar({ onMenuClick }) {
  const logout = useAuthStore((s) => s.logout)

  const handleLogout = () => {
    logout()
    window.location.href = "/login"
  }

  return (
    <header className="h-14 bg-slate-900 border-b border-slate-800 flex items-center justify-between px-4">
      
      {/* LEFT */}
      <div className="flex items-center gap-3">
        <Button
          variant="ghost"
          size="icon"
          onClick={onMenuClick}
          className="text-slate-300 hover:bg-slate-800"
        >
          <Menu className="h-5 w-5" />
        </Button>

        <span className="text-slate-100 font-semibold tracking-wide">
          CricRiser
        </span>
      </div>

      {/* RIGHT */}
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <button className="outline-none">
            <Avatar className="h-9 w-9 bg-orange-500 text-white">
              <AvatarFallback className="font-semibold">
                A
              </AvatarFallback>
            </Avatar>
          </button>
        </DropdownMenuTrigger>

        <DropdownMenuContent
          align="end"
          className="w-44"
        >
          <DropdownMenuItem
            onClick={() => (window.location.href = "/admin/profile")}
          >
            Profile
          </DropdownMenuItem>

          <DropdownMenuItem
            onClick={() => (window.location.href = "/reset-password")}
          >
            Reset Password
          </DropdownMenuItem>

          <DropdownMenuSeparator />

          <DropdownMenuItem
            onClick={handleLogout}
            className="text-red-600 focus:text-red-600"
          >
            Logout
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
    </header>
  )
}
