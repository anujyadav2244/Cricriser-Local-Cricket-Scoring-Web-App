import { useState } from "react";
import { resetPassword } from "@/api/auth.api";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { useNavigate } from "react-router-dom";

export default function ResetPasswordForm() {
  const [oldPassword, setOldPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError(null);

    if (newPassword.length < 6) {
      setError("New password must be at least 6 characters");
      return;
    }

    try {
      setLoading(true);
      await resetPassword({ oldPassword, newPassword });

      // logout after password change
      localStorage.removeItem("token");
      navigate("/login");
    } catch (err) {
      setError(err.response?.data?.error || "Password reset failed");
    } finally {
      setLoading(false);
    }
  };

  return (
    <Card className="w-full max-w-md bg-white">
      <CardContent className="p-6 space-y-6">
        <h1 className="text-2xl font-bold text-center">
          Reset Password
        </h1>

        <form onSubmit={handleSubmit} className="space-y-4">
          <Input
            type="password"
            placeholder="Old Password"
            required
            value={oldPassword}
            onChange={(e) => setOldPassword(e.target.value)}
          />

          <Input
            type="password"
            placeholder="New Password"
            required
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
          />

          {error && (
            <p className="text-sm text-red-500 text-center">
              {error}
            </p>
          )}

          <Button className="w-full" disabled={loading}>
            {loading ? "Updating..." : "Update Password"}
          </Button>
        </form>
      </CardContent>
    </Card>
  );
}
