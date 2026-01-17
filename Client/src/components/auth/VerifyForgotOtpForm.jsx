import { useState } from "react"
import { useSearchParams, useNavigate } from "react-router-dom"
import { verifyForgotOtp } from "@/api/auth.api"
import { Button } from "@/components/ui/button"
import { Card, CardContent } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import {
  InputOTP,
  InputOTPGroup,
  InputOTPSlot,
} from "@/components/ui/input-otp"

export default function VerifyForgotOtpForm() {
  const [searchParams] = useSearchParams()
  const email = searchParams.get("email")
  const navigate = useNavigate()

  const [otp, setOtp] = useState("")
  const [newPassword, setNewPassword] = useState("")
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)

  if (!email) {
    navigate("/forgot-password")
    return null
  }

  const handleSubmit = async (e) => {
    e.preventDefault()
    setError(null)

    if (otp.length !== 6) {
      setError("Enter 6 digit OTP")
      return
    }

    if (newPassword.length < 6) {
      setError("Password must be at least 6 characters")
      return
    }

    try {
      setLoading(true)
      await verifyForgotOtp({ email, otp, newPassword })

      navigate("/login")
    } catch (err) {
      setError(err.response?.data?.error || "OTP verification failed")
    } finally {
      setLoading(false)
    }
  }

  return (
    <Card className="w-full max-w-md bg-white">
      <CardContent className="p-6 space-y-6">
        <div className="text-center">
          <h1 className="text-2xl font-bold">Reset Password</h1>
          <p className="text-sm text-slate-500 mt-1">{email}</p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-5">
          {/* OTP */}
          <div className="flex justify-center">
            <InputOTP maxLength={6} value={otp} onChange={setOtp}>
              <InputOTPGroup>
                {[0, 1, 2, 3, 4, 5].map((i) => (
                  <InputOTPSlot key={i} index={i} />
                ))}
              </InputOTPGroup>
            </InputOTP>
          </div>

          <Input
            type="password"
            placeholder="New password"
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            required
          />

          {error && (
            <p className="text-sm text-red-500 text-center">{error}</p>
          )}

          <Button className="w-full" disabled={loading}>
            {loading ? "Updating..." : "Update Password"}
          </Button>
        </form>
      </CardContent>
    </Card>
  )
}
