import { useState, useEffect } from "react"
import { useNavigate, useParams, useSearchParams } from "react-router-dom"
import { Card, CardContent } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Button } from "@/components/ui/button"
import { Separator } from "@/components/ui/separator"
import { toast } from "sonner"
import { Upload, X, Plus } from "lucide-react"
import axios from "@/api/axios"

export default function AddTeam() {
  const navigate = useNavigate()
  const { leagueId } = useParams()
  const [searchParams] = useSearchParams()

  const teamNameFromLeague = searchParams.get("teamName")

  const [form, setForm] = useState({
    name: "",
    coach: "",
    captain: "",
    viceCaptain: "",
    leagueId: leagueId,          // ✅ IMPORTANT
    squadPlayerIds: [],
  })

  const [playerInput, setPlayerInput] = useState("")
  const [logo, setLogo] = useState(null)
  const [preview, setPreview] = useState(null)
  const [loading, setLoading] = useState(false)

  /* ================= PREFILL TEAM NAME ================= */
  useEffect(() => {
    if (teamNameFromLeague) {
      setForm((p) => ({ ...p, name: teamNameFromLeague }))
    }
  }, [teamNameFromLeague])

  const update = (k, v) => setForm((p) => ({ ...p, [k]: v }))

  /* ================= VALIDATION ================= */
  const validate = () => {
    if (!form.name) return "Team name required"
    if (!form.coach) return "Coach required"
    if (!form.captain) return "Captain required"
    if (!form.viceCaptain) return "Vice captain required"

    if (form.squadPlayerIds.length < 15 || form.squadPlayerIds.length > 18)
      return "Squad must have 15–18 players"

    if (!form.squadPlayerIds.includes(form.captain))
      return "Captain must be in squad"

    if (!form.squadPlayerIds.includes(form.viceCaptain))
      return "Vice captain must be in squad"

    return null
  }

  /* ================= ADD PLAYER ================= */
  const addPlayer = () => {
    const value = playerInput.trim()
    if (!value) return

    if (form.squadPlayerIds.includes(value)) {
      toast.error("Player already added")
      return
    }

    update("squadPlayerIds", [...form.squadPlayerIds, value])
    setPlayerInput("")
  }

  const removePlayer = (id) => {
    update(
      "squadPlayerIds",
      form.squadPlayerIds.filter((p) => p !== id)
    )
  }

  /* ================= SUBMIT ================= */
  const handleSubmit = async () => {
    const err = validate()
    if (err) return toast.error(err)

    try {
      setLoading(true)

      const fd = new FormData()
      fd.append("team", JSON.stringify(form))
      if (logo) fd.append("logo", logo)

      // ✅ CORRECT ENDPOINT
      await axios.post("/api/teams/create", fd)

      toast.success("Team created successfully 🏏")
      navigate(`/admin/leagues/${leagueId}`)
    } catch (e) {
      toast.error(e.response?.data?.error || "Creation failed")
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="max-w-4xl mx-auto p-6">
      <Card>
        <CardContent className="space-y-6 p-6">
          <h1 className="text-xl font-semibold">Create Team</h1>
          <Separator />

          <Input
            placeholder="Team Name"
            value={form.name}
            disabled={!!teamNameFromLeague}
            onChange={(e) => update("name", e.target.value)}
          />

          <Input
            placeholder="Coach Name"
            onChange={(e) => update("coach", e.target.value)}
          />

          <Input
            placeholder="Captain Player ID"
            onChange={(e) => update("captain", e.target.value)}
          />

          <Input
            placeholder="Vice Captain Player ID"
            onChange={(e) => update("viceCaptain", e.target.value)}
          />

          {/* SQUAD */}
          <div>
            <div className="flex gap-2">
              <Input
                placeholder="Player ID"
                value={playerInput}
                onChange={(e) => setPlayerInput(e.target.value)}
              />
              <Button type="button" onClick={addPlayer}>
                <Plus className="h-4 w-4" />
              </Button>
            </div>

            <div className="flex flex-wrap gap-2 mt-2">
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
          <label className="flex gap-2 text-sm cursor-pointer">
            <Upload className="h-4 w-4" /> Upload Logo
            <input
              type="file"
              hidden
              accept="image/*"
              onChange={(e) => {
                setLogo(e.target.files[0])
                setPreview(URL.createObjectURL(e.target.files[0]))
              }}
            />
          </label>

          {preview && (
            <img
              src={preview}
              className="w-28 h-28 object-contain border"
            />
          )}

          <Button onClick={handleSubmit} disabled={loading}>
            {loading ? "Creating..." : "Create Team"}
          </Button>
        </CardContent>
      </Card>
    </div>
  )
}
