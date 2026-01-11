import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import { leagueApi } from "@/api/league.api";
import { toast } from "sonner";
import { formatDate } from "@/lib/utils";
import { Plus } from "lucide-react";

export default function Leagues() {
  const [leagues, setLeagues] = useState([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  const loadLeagues = async () => {
    try {
      const res = await leagueApi.getAll();
      setLeagues(res.data);
    } catch (e) {
      toast.error(e.response?.data?.message || "Failed to load leagues");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadLeagues();
  }, []);

  const handleDelete = async (id) => {
    if (!confirm("Are you sure you want to delete this league?")) return;

    try {
      await leagueApi.delete(id);
      toast.success("League deleted successfully");
      setLeagues((prev) => prev.filter((l) => l.id !== id));
    } catch (e) {
      toast.error(e.response?.data?.message || "Delete failed");
    }
  };

  if (loading) return <p className="p-6">Loading leagues...</p>;

  return (
    <div className="max-w-6xl mx-auto p-6 space-y-4">
      {/* ================= HEADER ================= */}
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Manage Leagues</h1>

        {/* CREATE LEAGUE BUTTON */}
        <Button
          onClick={() => navigate("/admin/leagues/create")}
          className="flex items-center gap-2 bg-orange-500 hover:bg-orange-600"
        >
          <Plus className="h-4 w-4" />
          Create League
        </Button>
      </div>

      <Separator />

      {leagues.length === 0 && (
        <p className="text-slate-500">No leagues found.</p>
      )}

      {/* ================= LEAGUE CARDS ================= */}
      <div className="space-y-4">
        {leagues.map((league) => (
          <Card
            key={league.id}
            onClick={() => navigate(`/admin/leagues/${league.id}`)}
            className="cursor-pointer hover:shadow-md transition"
          >
            <CardContent className="flex items-center justify-between gap-6 p-4">
              <div className="flex items-center gap-4">
                {league.logoUrl && (
                  <img
                    src={league.logoUrl}
                    alt="logo"
                    className="h-14 w-14 rounded object-contain border"
                  />
                )}

                <div>
                  <h2 className="font-semibold">{league.name}</h2>
                  <p className="text-sm text-slate-500">
                    {league.leagueType} • {league.leagueFormat}
                  </p>
                  <p className="text-xs text-slate-400">
                    {formatDate(league.startDate)} →{" "}
                    {formatDate(league.endDate)}
                  </p>
                </div>
              </div>

              <div className="flex gap-2">
                <Button
                  variant="outline"
                  onClick={() => navigate(`/admin/leagues/update/${league.id}`)}
                >
                  Update
                </Button>

                <Button
                  variant="destructive"
                  onClick={() => handleDelete(league.id)}
                >
                  Delete
                </Button>
              </div>
            </CardContent>
          </Card>
        ))}
      </div>
    </div>
  );
}
