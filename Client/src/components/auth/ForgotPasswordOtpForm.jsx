import { useState } from "react"
import { useSearchParams } from "react-router-dom"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import {
  InputOTP,
  InputOTPGroup,
  InputOTPSlot,
} from "@/components/ui/input-otp"
import { verifyForgotOtp } from "@/api/auth.api"

export function ForgotPasswordOtpForm() {
  const [searchParams] = useSearchParams()
  const email = searchParams.get("email")

  const [otp, setOtp] = useState("")
  const [newPassword, setNewPassword] = useState("")
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError(null)

    if (otp.length !== 6) {
      setError("Please enter the 6-digit OTP")
      return
    }

    if (newPassword.length < 6) {
      setError("Password must be at least 6 characters")
      return
    }

    try {
      setLoading(true)
      await verifyForgotOtp({ email, otp, newPassword })

      // Success → back to login
      window.location.href = "/login"
    } catch (err) {
      setError(err.response?.data?.error || "OTP verification failed")
    } finally {
      setLoading(false)
    }
  }

  return (
    <Card className="w-full max-w-md bg-white">
      <CardContent className="p-6 space-y-6">
        <div className="text-center space-y-1">
          <h1 className="text-2xl font-bold text-slate-900">
            Reset Password
          </h1>
          <p className="text-slate-500 text-sm">
            Enter the OTP sent to
          </p>
          <p className="text-slate-700 text-sm font-medium">
            {email}
          </p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-5">
          {/* OTP INPUT */}
          <div className="flex justify-center">
            <InputOTP maxLength={6} value={otp} onChange={setOtp}>
              <InputOTPGroup>
                <InputOTPSlot index={0} />
                <InputOTPSlot index={1} />
                <InputOTPSlot index={2} />
                <InputOTPSlot index={3} />
                <InputOTPSlot index={4} />
                <InputOTPSlot index={5} />
              </InputOTPGroup>
            </InputOTP>
          </div>

          <p className="text-xs text-slate-500 text-center">
            OTP is valid for <span className="font-medium">10 minutes</span>
          </p>

          {/* NEW PASSWORD */}
          <Input
            type="password"
            placeholder="New password"
            required
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
          />

          {error && (
            <p className="text-sm text-red-500 text-center">
              {error}
            </p>
          )}

          <Button
            type="submit"
            className="w-full bg-orange-500 hover:bg-orange-600"
            disabled={loading}
          >
            {loading ? "Updating password..." : "Update Password"}
          </Button>
        </form>
      </CardContent>
    </Card>
  )
}
