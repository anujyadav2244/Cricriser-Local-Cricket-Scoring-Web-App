import { useState, useEffect } from "react";
import { useNavigate, useParams, useSearchParams } from "react-router-dom";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import { toast } from "sonner";
import { Upload, X, Plus } from "lucide-react";
import axios from "@/api/axios";

export default function AddTeam() {
  const navigate = useNavigate();
  const { leagueId } = useParams();            // league from URL
  const [searchParams] = useSearchParams();    // team from query

  const teamNameFromLeague = searchParams.get("teamName");

  const [form, setForm] = useState({
    name: "",
    coach: "",
    captain: "",
    viceCaptain: "",
    squadPlayerIds: [],
  });

  const [playerInput, setPlayerInput] = useState("");
  const [logo, setLogo] = useState(null);
  const [preview, setPreview] = useState(null);
  const [loading, setLoading] = useState(false);

  /* ================= PREFILL TEAM NAME ================= */
  useEffect(() => {
    if (teamNameFromLeague) {
      setForm((p) => ({ ...p, name: teamNameFromLeague }));
    }
  }, [teamNameFromLeague]);

  const update = (k, v) => setForm((p) => ({ ...p, [k]: v }));

  /* ================= VALIDATION ================= */
  const validate = () => {
    if (!form.name) return "Team name required";
    if (!form.coach) return "Coach name required";
    if (!form.captain) return "Captain name required";
    if (!form.viceCaptain) return "Vice captain name required";

    if (form.squadPlayerIds.length < 15 || form.squadPlayerIds.length > 18)
      return "Squad must have 15–18 players";

    if (!form.squadPlayerIds.includes(form.captain))
      return "Captain must be in squad";

    if (!form.squadPlayerIds.includes(form.viceCaptain))
      return "Vice captain must be in squad";

    return null;
  };

  /* ================= ADD PLAYER ================= */
  const addPlayer = () => {
    if (!playerInput.trim()) return;

    if (form.squadPlayerIds.includes(playerInput.trim())) {
      toast.error("Player already added");
      return;
    }

    update("squadPlayerIds", [...form.squadPlayerIds, playerInput.trim()]);
    setPlayerInput("");
  };

  const removePlayer = (name) => {
    update(
      "squadPlayerIds",
      form.squadPlayerIds.filter((p) => p !== name)
    );
  };

  /* ================= SUBMIT ================= */
  const handleSubmit = async () => {
    const err = validate();
    if (err) return toast.error(err);

    try {
      setLoading(true);

      const fd = new FormData();
      fd.append("team", JSON.stringify(form));
      if (logo) fd.append("logo", logo);

      const token = localStorage.getItem("token");

      await axios.post(
        `/api/leagues/${leagueId}/teams`,
        fd,
        {
          headers: {
            Authorization: `Bearer ${token}`,
            "Content-Type": "multipart/form-data",
          },
        }
      );

      toast.success("Team created successfully 🏏");
      navigate(`/admin/leagues/${leagueId}`);
    } catch (e) {
      toast.error(e.response?.data?.error || "Creation failed");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto p-6">
      <Card>
        <CardContent className="space-y-6 p-6">
          <h1 className="text-xl font-semibold">Create Team</h1>
          <Separator />

          {/* TEAM NAME (READONLY FOR BILATERAL) */}
          <div>
            <label className="text-sm font-medium">Team Name</label>
            <Input
              value={form.name}
              disabled={!!teamNameFromLeague}
              placeholder="Enter team name"
              onChange={(e) => update("name", e.target.value)}
            />
          </div>

          {/* COACH */}
          <div>
            <label className="text-sm font-medium">Coach Name</label>
            <Input
              placeholder="Enter coach name"
              onChange={(e) => update("coach", e.target.value)}
            />
          </div>

          {/* CAPTAIN */}
          <div>
            <label className="text-sm font-medium">Captain Name</label>
            <Input
              placeholder="Enter captain name"
              onChange={(e) => update("captain", e.target.value)}
            />
          </div>

          {/* VICE CAPTAIN */}
          <div>
            <label className="text-sm font-medium">Vice Captain Name</label>
            <Input
              placeholder="Enter vice captain name"
              onChange={(e) => update("viceCaptain", e.target.value)}
            />
          </div>

          {/* SQUAD */}
          <div>
            <label className="text-sm font-medium">Add Squad Players</label>

            <div className="flex gap-2 mt-1">
              <Input
                placeholder="Enter player name"
                value={playerInput}
                onChange={(e) => setPlayerInput(e.target.value)}
              />
              <Button onClick={addPlayer} type="button">
                <Plus className="h-4 w-4" />
              </Button>
            </div>

            <div className="flex flex-wrap gap-2 mt-3">
              {form.squadPlayerIds.map((p) => (
                <span
                  key={p}
                  className="flex items-center gap-1 bg-slate-200 px-2 py-1 rounded text-sm"
                >
                  {p}
                  <X
                    className="h-3 w-3 cursor-pointer text-red-500"
                    onClick={() => removePlayer(p)}
                  />
                </span>
              ))}
            </div>
          </div>

          {/* LOGO */}
          <div>
            <label className="flex gap-2 text-sm cursor-pointer">
              <Upload className="h-4 w-4" /> Upload Team Logo
              <input
                type="file"
                hidden
                accept="image/*"
                onChange={(e) => {
                  setLogo(e.target.files[0]);
                  setPreview(URL.createObjectURL(e.target.files[0]));
                }}
              />
            </label>

            {preview && (
              <div className="relative w-28 h-28 mt-2">
                <img
                  src={preview}
                  className="w-full h-full object-contain border"
                />
                <button
                  onClick={() => {
                    setLogo(null);
                    setPreview(null);
                  }}
                >
                  <X className="h-4 w-4 text-red-500" />
                </button>
              </div>
            )}
          </div>

          <Button onClick={handleSubmit} disabled={loading}>
            {loading ? "Creating..." : "Create Team"}
          </Button>
        </CardContent>
      </Card>
    </div>
  );
}
