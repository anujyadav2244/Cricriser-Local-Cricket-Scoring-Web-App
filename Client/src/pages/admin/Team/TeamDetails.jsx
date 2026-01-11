import { useEffect, useState } from "react"
import { useParams } from "react-router-dom"
import { Card, CardContent } from "@/components/ui/card"
import { Separator } from "@/components/ui/separator"
import { toast } from "sonner"
import axios from "@/api/axios"

/* ================= ROLE LABELS ================= */

const ROLE_LABELS = {
  BATSMAN: "BATSMEN",
  ALL_ROUNDER: "ALL ROUNDERS",
  BOWLER: "BOWLERS",
  WICKET_KEEPER: "WICKET KEEPERS",
}

export default function TeamDetails() {
  const { name } = useParams()
  const [team, setTeam] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    axios
      .get(`/api/teams/name/${decodeURIComponent(name)}`)
      .then((res) => {
        setTeam(res.data)
        setLoading(false)
      })
      .catch(() => {
        toast.error("Failed to load team details")
        setLoading(false)
      })
  }, [name])

  if (loading) {
    return <p className="p-6">Loading team details...</p>
  }

  if (!team) {
    return <p className="p-6">Team not found</p>
  }

  /* ================= GROUP PLAYERS BY ROLE ================= */
  const groupedPlayers = (team.players || []).reduce((acc, player) => {
    if (!player?.role) return acc
    acc[player.role] = acc[player.role] || []
    acc[player.role].push(player)
    return acc
  }, {})

  return (
    <div className="max-w-5xl mx-auto p-6 space-y-6">

      {/* ================= HEADER ================= */}
      <h1 className="text-3xl font-semibold">{team.name}</h1>

      <Separator />

      {/* ================= BASIC INFO ================= */}
      <Card>
        <CardContent className="space-y-2 p-4 text-sm">
          <p><b>League:</b> {team.leagueName}</p>
          <p><b>Coach:</b> {team.coach}</p>
          <p><b>Captain:</b> {team.captainName}</p>
          <p><b>Vice Captain:</b> {team.viceCaptainName}</p>
        </CardContent>
      </Card>

      {/* ================= SQUAD ================= */}
      <div className="space-y-6">
        {Object.keys(ROLE_LABELS).map((role) =>
          groupedPlayers[role]?.length ? (
            <RoleSection
              key={role}
              title={ROLE_LABELS[role]}
              players={groupedPlayers[role]}
            />
          ) : null
        )}

        {/* Fallback if no players */}
        {(!team.players || team.players.length === 0) && (
          <p className="text-sm text-slate-500">
            No squad players found for this team.
          </p>
        )}
      </div>
    </div>
  )
}

/* ================= ROLE SECTION ================= */

function RoleSection({ title, players }) {
  return (
    <Card>
      <CardContent className="p-4">
        <div className="bg-emerald-100 text-emerald-900 px-3 py-1 text-sm font-semibold mb-4 rounded">
          {title}
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          {players.map((player) => (
            <PlayerRow key={player.id} player={player} />
          ))}
        </div>
      </CardContent>
    </Card>
  )
}

/* ================= PLAYER ROW ================= */

function PlayerRow({ player }) {
  return (
    <div className="flex items-center gap-3">
      <img
        src={player.photoUrl || "/default-player.png"}
        alt={player.name}
        className="h-12 w-12 rounded-full object-cover border"
      />
      <span className="font-medium">{player.name}</span>
    </div>
  )
}
