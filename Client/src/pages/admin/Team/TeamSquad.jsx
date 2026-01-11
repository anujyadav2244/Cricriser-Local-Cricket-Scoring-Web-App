import { Card, CardContent } from "@/components/ui/card"

const ROLE_LABELS = {
  BATSMAN: "BATSMEN",
  ALL_ROUNDER: "ALL ROUNDER",
  BOWLER: "BOWLERS",
  WICKET_KEEPER: "WICKET KEEPER",
}

export default function TeamSquad({ players }) {
  // group players by role
  const grouped = players.reduce((acc, p) => {
    acc[p.role] = acc[p.role] || []
    acc[p.role].push(p)
    return acc
  }, {})

  return (
    <div className="space-y-6">
      {Object.keys(ROLE_LABELS).map((role) =>
        grouped[role]?.length ? (
          <RoleSection
            key={role}
            title={ROLE_LABELS[role]}
            players={grouped[role]}
          />
        ) : null
      )}
    </div>
  )
}

/* ================= ROLE SECTION ================= */

function RoleSection({ title, players }) {
  return (
    <Card>
      <CardContent className="p-4">
        <div className="bg-emerald-100 text-emerald-900 px-3 py-1 text-sm font-semibold mb-4">
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
