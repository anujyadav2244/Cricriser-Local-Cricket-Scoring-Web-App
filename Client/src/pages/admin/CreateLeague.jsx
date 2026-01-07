import { useState } from "react"
import { Card, CardContent } from "@/components/ui/card"
import { Input } from "@/components/ui/input"
import { Button } from "@/components/ui/button"
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select"
import { X } from "lucide-react"

export default function CreateLeague() {
  const [teamInput, setTeamInput] = useState("")
  const [umpireInput, setUmpireInput] = useState("")

  const [form, setForm] = useState({
    name: "",
    leagueType: "",
    noOfTeams: "",
    noOfMatches: "",
    teams: [],
    startDate: "",
    endDate: "",
    tour: "",
    leagueFormat: "",
    leagueFormatType: "",
    oversPerInnings: "",
    testDays: "",
    umpires: [],
    logo: null,
  })

  const update = (key, value) => {
    setForm({ ...form, [key]: value })
  }

  /* ===================== TEAMS ===================== */

  const addTeam = () => {
    if (!teamInput.trim()) return
    if (form.teams.includes(teamInput.trim())) return

    update("teams", [...form.teams, teamInput.trim()])
    setTeamInput("")
  }

  const removeTeam = (team) => {
    update(
      "teams",
      form.teams.filter((t) => t !== team)
    )
  }

  /* ===================== UMPIRES ===================== */

  const addUmpire = () => {
    if (!umpireInput.trim()) return
    if (form.umpires.includes(umpireInput.trim())) return

    update("umpires", [...form.umpires, umpireInput.trim()])
    setUmpireInput("")
  }

  const removeUmpire = (umpire) => {
    update(
      "umpires",
      form.umpires.filter((u) => u !== umpire)
    )
  }

  return (
    <Card className="max-w-4xl bg-white">
      <CardContent className="p-6 space-y-6">
        <h1 className="text-2xl font-semibold text-slate-900">
          Create League
        </h1>

        {/* League Name */}
        <Input
          placeholder="League Name"
          value={form.name}
          onChange={(e) => update("name", e.target.value)}
        />

        {/* League Type */}
        <Select onValueChange={(v) => update("leagueType", v)}>
          <SelectTrigger>
            <SelectValue placeholder="League Type" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="TOURNAMENT">Tournament</SelectItem>
            <SelectItem value="BILATERAL">Bilateral</SelectItem>
          </SelectContent>
        </Select>

        {/* Match Format */}
        <Select onValueChange={(v) => update("leagueFormat", v)}>
          <SelectTrigger>
            <SelectValue placeholder="Match Format" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="T20">T20</SelectItem>
            <SelectItem value="ODI">ODI</SelectItem>
            <SelectItem value="TEST">Test</SelectItem>
          </SelectContent>
        </Select>

        {/* League Format Type */}
        <Select onValueChange={(v) => update("leagueFormatType", v)}>
          <SelectTrigger>
            <SelectValue placeholder="League Format Type" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="SINGLE_ROUND_ROBIN">
              Single Round Robin
            </SelectItem>
            <SelectItem value="DOUBLE_ROUND_ROBIN">
              Double Round Robin
            </SelectItem>
            <SelectItem value="GROUP">
              Group Stage
            </SelectItem>
          </SelectContent>
        </Select>

        {/* Numbers */}
        <div className="grid grid-cols-2 gap-4">
          <Input
            type="number"
            placeholder="Number of Teams"
            value={form.noOfTeams}
            onChange={(e) => update("noOfTeams", e.target.value)}
          />
          <Input
            type="number"
            placeholder="Number of Matches"
            value={form.noOfMatches}
            onChange={(e) => update("noOfMatches", e.target.value)}
          />
        </div>

        {/* Overs / Test Days */}
        <div className="grid grid-cols-2 gap-4">
          <Input
            type="number"
            placeholder="Overs per Innings"
            value={form.oversPerInnings}
            onChange={(e) =>
              update("oversPerInnings", e.target.value)
            }
          />
          <Input
            type="number"
            placeholder="Test Days"
            value={form.testDays}
            onChange={(e) => update("testDays", e.target.value)}
          />
        </div>

        {/* ===================== TEAMS SECTION ===================== */}
        <div className="space-y-2">
          <label className="text-sm font-medium text-slate-700">
            Teams
          </label>

          <div className="flex gap-2">
            <Input
              placeholder="Enter team name"
              value={teamInput}
              onChange={(e) => setTeamInput(e.target.value)}
            />
            <Button
              type="button"
              onClick={addTeam}
              className="bg-orange-500 hover:bg-orange-600"
            >
              Add
            </Button>
          </div>

          <div className="flex flex-wrap gap-2">
            {form.teams.map((team) => (
              <div
                key={team}
                className="flex items-center gap-2 bg-slate-100 px-3 py-1 rounded-full text-sm"
              >
                {team}
                <button
                  type="button"
                  onClick={() => removeTeam(team)}
                >
                  <X className="h-4 w-4 text-red-500" />
                </button>
              </div>
            ))}
          </div>
        </div>

        {/* ===================== UMPIRES SECTION ===================== */}
        <div className="space-y-2">
          <label className="text-sm font-medium text-slate-700">
            Umpires
          </label>

          <div className="flex gap-2">
            <Input
              placeholder="Enter umpire name"
              value={umpireInput}
              onChange={(e) => setUmpireInput(e.target.value)}
            />
            <Button
              type="button"
              onClick={addUmpire}
              className="bg-orange-500 hover:bg-orange-600"
            >
              Add
            </Button>
          </div>

          <div className="flex flex-wrap gap-2">
            {form.umpires.map((umpire) => (
              <div
                key={umpire}
                className="flex items-center gap-2 bg-slate-100 px-3 py-1 rounded-full text-sm"
              >
                {umpire}
                <button
                  type="button"
                  onClick={() => removeUmpire(umpire)}
                >
                  <X className="h-4 w-4 text-red-500" />
                </button>
              </div>
            ))}
          </div>
        </div>

        {/* Tour */}
        <Input
          placeholder="Tour / Location"
          value={form.tour}
          onChange={(e) => update("tour", e.target.value)}
        />

        {/* Dates */}
        <div className="grid grid-cols-2 gap-4">
          <Input
            type="date"
            value={form.startDate}
            onChange={(e) => update("startDate", e.target.value)}
          />
          <Input
            type="date"
            value={form.endDate}
            onChange={(e) => update("endDate", e.target.value)}
          />
        </div>

        {/* Logo */}
        <Input
          type="file"
          accept="image/*"
          onChange={(e) => update("logo", e.target.files[0])}
        />

        {/* Submit (UI only) */}
        <Button
          type="button"
          className="bg-orange-500 hover:bg-orange-600"
        >
          Create League
        </Button>
      </CardContent>
    </Card>
  )
}
