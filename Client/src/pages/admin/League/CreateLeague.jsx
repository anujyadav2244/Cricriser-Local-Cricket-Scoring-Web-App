import { useEffect, useState } from "react";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Separator } from "@/components/ui/separator";
import { X, Upload, Loader2 } from "lucide-react";
import { toast } from "sonner";

/* ========================================================= */

const API_BASE =
  import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

/* ========================================================= */

export default function CreateLeague() {
  const [teamInput, setTeamInput] = useState("");
  const [umpireInput, setUmpireInput] = useState("");
  const [loading, setLoading] = useState(false);

  const [logoPreview, setLogoPreview] = useState(null);

  const [form, setForm] = useState({
    name: "",
    leagueType: "",
    leagueFormat: "",
    leagueFormatType: "",
    noOfMatches: "",
    oversPerInnings: "",
    testDays: "",
    teams: [],
    umpires: [],
    tour: "",
    startDate: "",
    endDate: "",
    logo: null,
  });

  const update = (key, value) =>
    setForm((prev) => ({ ...prev, [key]: value }));

  const isTournament = form.leagueType === "TOURNAMENT";
  const isBilateral = form.leagueType === "BILATERAL";
  const isTest = form.leagueFormat === "TEST";
  const isLimited = form.leagueFormat === "LIMITED";

  /* ===================== LOGO PREVIEW ===================== */

  useEffect(() => {
    if (!form.logo) {
      setLogoPreview(null);
      return;
    }

    const url = URL.createObjectURL(form.logo);
    setLogoPreview(url);
    return () => URL.revokeObjectURL(url);
  }, [form.logo]);

  /* ===================== TEAMS ===================== */

  const addTeam = () => {
    if (!teamInput.trim()) return;
    if (isBilateral && form.teams.length >= 2) return;
    if (form.teams.includes(teamInput.trim())) return;

    update("teams", [...form.teams, teamInput.trim()]);
    setTeamInput("");
  };

  const removeTeam = (team) =>
    update("teams", form.teams.filter((t) => t !== team));

  /* ===================== UMPIRES ===================== */

  const addUmpire = () => {
    if (!umpireInput.trim()) return;
    if (form.umpires.includes(umpireInput.trim())) return;

    update("umpires", [...form.umpires, umpireInput.trim()]);
    setUmpireInput("");
  };

  const removeUmpire = (u) =>
    update("umpires", form.umpires.filter((x) => x !== u));

  /* ===================== VALIDATION ===================== */

  const validate = () => {
    if (!form.name) return "League / Series name is required";
    if (!form.leagueType) return "League type is required";
    if (!form.leagueFormat) return "Match type is required";

    if (isBilateral && form.teams.length !== 2)
      return "Bilateral series must have exactly 2 teams";

    if (isTournament && form.teams.length < 3)
      return "Tournament requires minimum 3 teams";

    if (isBilateral && !form.noOfMatches)
      return "Number of matches is required";

    if (isTournament && !form.leagueFormatType)
      return "Tournament format is required";

    if (isLimited && !form.oversPerInnings)
      return "Overs per innings is required";

    if (isTest && !form.testDays)
      return "Test match days (4 or 5) required";

    if (!form.startDate || !form.endDate)
      return "Start and end dates are required";

    return null;
  };

  /* ===================== SUBMIT ===================== */

  const handleSubmit = async () => {
    const validationError = validate();
    if (validationError) {
      toast.error(validationError);
      return;
    }

    try {
      setLoading(true);

      const payload = {
        name: form.name,
        leagueType: form.leagueType,
        leagueFormat: form.leagueFormat,
        leagueFormatType: isTournament ? form.leagueFormatType : null,
        noOfMatches: isBilateral ? Number(form.noOfMatches) : null,
        oversPerInnings: isLimited ? Number(form.oversPerInnings) : null,
        testDays: isTest ? Number(form.testDays) : null,
        teams: form.teams,
        umpires: form.umpires,
        tour: form.tour,
        startDate: form.startDate,
        endDate: form.endDate,
      };

      const fd = new FormData();
      fd.append("league", JSON.stringify(payload));
      if (form.logo) fd.append("logo", form.logo);

      const token = localStorage.getItem("token");

      const res = await fetch(`${API_BASE}/api/leagues/create`, {
        method: "POST",
        headers: { Authorization: `Bearer ${token}` },
        body: fd,
      });

      const data = await res.json();
      if (!res.ok) throw new Error(data.message || data.error);

      toast.success("League created successfully 🎉");
    } catch (e) {
      toast.error(e.message);
    } finally {
      setLoading(false);
    }
  };

  const removeLogo = () => {
    update("logo", null);
    setLogoPreview(null);
  };

  /* ========================================================= */

  return (
    <div className="max-w-5xl mx-auto px-4 py-6">
      <Card>
        <CardContent className="space-y-8 p-6">

          <div>
            <h1 className="text-2xl font-semibold">Create League / Series</h1>
            <p className="text-sm text-slate-500">
              Configure structure, format, teams and schedule
            </p>
          </div>

          <Separator />

          {/* LEAGUE NAME */}
          <Field label="League / Series Name">
            <Input
              placeholder="e.g. IPL 2025"
              value={form.name}
              onChange={(e) => update("name", e.target.value)}
            />
          </Field>

          {/* TYPE & FORMAT */}
          <div className="grid sm:grid-cols-2 gap-4">
            <Field label="League Type">
              <Select onValueChange={(v) => update("leagueType", v)}>
                <SelectTrigger>
                  <SelectValue placeholder="Select league type" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="BILATERAL">Bilateral Series</SelectItem>
                  <SelectItem value="TOURNAMENT">Tournament</SelectItem>
                </SelectContent>
              </Select>
            </Field>

            <Field label="Match Type">
              <Select onValueChange={(v) => update("leagueFormat", v)}>
                <SelectTrigger>
                  <SelectValue placeholder="Select match type" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="LIMITED">Limited Overs</SelectItem>
                  <SelectItem value="TEST">Test Match</SelectItem>
                </SelectContent>
              </Select>
            </Field>
          </div>

          {isTournament && (
            <Field label="Tournament Format">
              <Select onValueChange={(v) => update("leagueFormatType", v)}>
                <SelectTrigger>
                  <SelectValue placeholder="Select tournament format" />
                </SelectTrigger>
                <SelectContent>
                  <SelectItem value="SINGLE_ROUND_ROBIN">
                    Single Round Robin
                  </SelectItem>
                  <SelectItem value="DOUBLE_ROUND_ROBIN">
                    Double Round Robin
                  </SelectItem>
                  <SelectItem value="GROUP">Group Stage</SelectItem>
                </SelectContent>
              </Select>
            </Field>
          )}

          {isBilateral && (
            <Field label="Number of Matches">
              <Input
                type="number"
                value={form.noOfMatches}
                onChange={(e) => update("noOfMatches", e.target.value)}
              />
            </Field>
          )}

          {isLimited && (
            <Field label="Overs per Innings">
              <Input
                type="number"
                value={form.oversPerInnings}
                onChange={(e) =>
                  update("oversPerInnings", e.target.value)
                }
              />
            </Field>
          )}

          {isTest && (
            <Field label="Test Match Days">
              <Input
                type="number"
                value={form.testDays}
                onChange={(e) => update("testDays", e.target.value)}
              />
            </Field>
          )}

          <Section
            label="Teams"
            hint={isBilateral ? "Exactly 2 teams" : "Minimum 3 teams"}
            input={teamInput}
            setInput={setTeamInput}
            items={form.teams}
            onAdd={addTeam}
            onRemove={removeTeam}
          />

          <Section
            label="Umpires"
            hint="Optional"
            input={umpireInput}
            setInput={setUmpireInput}
            items={form.umpires}
            onAdd={addUmpire}
            onRemove={removeUmpire}
          />

          <Field label="Tour / Venue">
            <Input
              placeholder="e.g. India"
              value={form.tour}
              onChange={(e) => update("tour", e.target.value)}
            />
          </Field>

          <div className="grid sm:grid-cols-2 gap-4">
            <Field label="Start Date">
              <Input
                type="date"
                value={form.startDate}
                onChange={(e) => update("startDate", e.target.value)}
              />
            </Field>

            <Field label="End Date">
              <Input
                type="date"
                value={form.endDate}
                onChange={(e) => update("endDate", e.target.value)}
              />
            </Field>
          </div>

          {/* LOGO */}
          <Field label="League Logo">
            <label className="flex items-center gap-2 cursor-pointer text-sm">
              <Upload className="h-4 w-4" />
              Upload Logo
              <input
                type="file"
                hidden
                accept="image/*"
                onChange={(e) =>
                  update("logo", e.target.files?.[0] || null)
                }
              />
            </label>

            {logoPreview && (
              <div className="relative w-40 h-40 mt-2 border rounded">
                <img
                  src={logoPreview}
                  alt="logo"
                  className="w-full h-full object-contain"
                />
                <button
                  type="button"
                  onClick={removeLogo}
                  className="absolute top-1 right-1 bg-white p-1 rounded"
                >
                  <X className="h-4 w-4 text-red-500" />
                </button>
              </div>
            )}
          </Field>

          <Button
            onClick={handleSubmit}
            disabled={loading}
            className="w-full bg-orange-500 hover:bg-orange-600"
          >
            {loading && (
              <Loader2 className="h-4 w-4 animate-spin mr-2" />
            )}
            Create League
          </Button>

        </CardContent>
      </Card>
    </div>
  );
}

/* ===================== HELPERS ===================== */

function Field({ label, children }) {
  return (
    <div className="space-y-1">
      <label className="text-sm font-medium">{label}</label>
      {children}
    </div>
  );
}

function Section({ label, hint, input, setInput, items, onAdd, onRemove }) {
  return (
    <div className="space-y-2">
      <label className="text-sm font-medium">{label}</label>
      {hint && <p className="text-xs text-slate-500">{hint}</p>}
      <div className="flex gap-2">
        <Input
          placeholder={`Enter ${label.toLowerCase()} name`}
          value={input}
          onChange={(e) => setInput(e.target.value)}
        />
        <Button onClick={onAdd}>Add</Button>
      </div>
      <div className="flex flex-wrap gap-2">
        {items.map((i) => (
          <Chip key={i} label={i} onRemove={() => onRemove(i)} />
        ))}
      </div>
    </div>
  );
}

function Chip({ label, onRemove }) {
  return (
    <span className="flex items-center gap-2 bg-slate-100 px-3 py-1 rounded-full text-sm">
      {label}
      <button onClick={onRemove}>
        <X className="h-4 w-4 text-red-500" />
      </button>
    </span>
  );
}
