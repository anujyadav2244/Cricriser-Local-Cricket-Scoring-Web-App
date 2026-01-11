import { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import { leagueApi } from "@/api/league.api";
import { formatDate } from "@/lib/utils";
import { toast } from "sonner";

export default function LeagueDetails() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [league, setLeague] = useState(null);

  useEffect(() => {
    leagueApi
      .getById(id)
      .then((res) => setLeague(res.data))
      .catch(() => toast.error("Failed to load league details"));
  }, [id]);

  if (!league) return <p className="p-6">Loading league details...</p>;

  const teamCount = league.teams?.length || 0;

  const canAddTeam =
    (league.leagueType === "BILATERAL" && teamCount <= 2) ||
    league.leagueType === "TOURNAMENT";

  return (
    <div className="max-w-5xl mx-auto p-6 space-y-6">

      {/* HEADER */}
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-semibold">{league.name}</h1>

        <div className="flex gap-2">
          <Button
            className="bg-green-600 hover:bg-green-700"
            onClick={() => navigate(`/admin/leagues/update/${league.id}`)}
          >
            Update
          </Button>

          <Button
            variant="destructive"
            onClick={() => navigate(`/admin/leagues/delete/${league.id}`)}
          >
            Delete
          </Button>
        </div>
      </div>

      <Separator />

      {/* BASIC INFO */}
      <Card>
        <CardContent className="space-y-2 p-4 text-sm">
          <p><b>Type:</b> {league.leagueType}</p>
          <p><b>Format:</b> {league.leagueFormat}</p>
          <p>
            <b>Duration:</b>{" "}
            {formatDate(league.startDate)} → {formatDate(league.endDate)}
          </p>
          <p><b>Venue / Tour:</b> {league.tour}</p>
          <p><b>Total Matches:</b> {league.noOfMatches}</p>
        </CardContent>
      </Card>

      {/* TEAMS */}
      <Card>
        <CardContent className="p-4 space-y-3">
          <h2 className="font-semibold">Teams</h2>

          <ul className="list-disc list-inside text-sm space-y-1">
            {league.teams.map((teamName) => (
              <li
                key={teamName}
                className="cursor-pointer text-blue-600 hover:underline"
                onClick={() =>
                  navigate(
                    `/admin/leagues/${league.id}/teams/create?teamName=${encodeURIComponent(teamName)}`
                  )
                }
              >
                {teamName}
              </li>
            ))}
          </ul>

          {league.leagueType === "BILATERAL" && (
            <p className="text-xs text-red-500">
              Bilateral series allows only 2 teams
            </p>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
