import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import { Plus } from "lucide-react";
import { toast } from "sonner";
import { teamApi } from "@/api/team.api";

export default function ManageTeams() {
  const [teams, setTeams] = useState([]);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  const loadTeams = async () => {
    try {
      const res = await teamApi.getAll();
      setTeams(res.data);
    } catch (e) {
      toast.error(e.response?.data?.error || "Failed to load teams");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadTeams();
  }, []);

  const handleDelete = async (id) => {
    if (!confirm("Are you sure you want to delete this team?")) return;

    try {
      await teamApi.delete(id);
      toast.success("Team deleted successfully");
      setTeams((prev) => prev.filter((t) => t.id !== id));
    } catch (e) {
      toast.error(e.response?.data?.error || "Delete failed");
    }
  };

  if (loading) return <p className="p-6">Loading teams...</p>;

  return (
    <div className="max-w-6xl mx-auto p-6 space-y-4">

      {/* HEADER */}
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-semibold">Manage Teams</h1>

        <Button
          onClick={() => navigate("/admin/teams/create")}
          className="flex items-center gap-2 bg-orange-500 hover:bg-orange-600"
        >
          <Plus className="h-4 w-4" />
          Create Team
        </Button>
      </div>

      <Separator />

      {teams.length === 0 && (
        <p className="text-slate-500">No teams found.</p>
      )}

      {/* TEAM CARDS */}
      <div className="space-y-4">
        {teams.map((team) => (
          <Card key={team.id}>
            <CardContent className="flex items-center justify-between p-4">
              <div className="flex items-center gap-4">
                {team.logoUrl && (
                  <img
                    src={team.logoUrl}
                    alt="logo"
                    className="h-14 w-14 border rounded object-contain"
                  />
                )}

                <div>
                  <h2 className="font-semibold">{team.name}</h2>
                  <p className="text-sm text-slate-500">
                    Coach: {team.coach}
                  </p>
                  <p className="text-xs text-slate-400">
                    Squad: {team.squadPlayerIds?.length || 0} players
                  </p>
                </div>
              </div>

              <div className="flex gap-2">
                <Button
                  variant="outline"
                  onClick={() =>
                    navigate(`/admin/teams/update/${team.id}`)
                  }
                >
                  Update
                </Button>

                <Button
                  variant="destructive"
                  onClick={() => handleDelete(team.id)}
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
