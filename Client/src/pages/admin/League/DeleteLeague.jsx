import { useParams, useNavigate } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";
import { Separator } from "@/components/ui/separator";
import { leagueApi } from "@/api/league.api";
import { toast } from "sonner";

export default function DeleteLeague() {
  const { id } = useParams();
  const navigate = useNavigate();

  const handleDelete = async () => {
    try {
      await leagueApi.delete(id);
      toast.success("League deleted successfully");
      navigate("/admin/leagues");
    } catch (e) {
      toast.error(e.response?.data?.message || "Delete failed");
    }
  };

  return (
    <div className="max-w-xl mx-auto p-6">
      <Card>
        <CardContent className="space-y-4 p-6">
          <h1 className="text-xl font-semibold text-red-600">
            Delete League
          </h1>

          <Separator />

          <p className="text-sm text-slate-600">
            This action is <b>permanent</b>. All matches, teams, scores,
            and points related to this league will be deleted.
          </p>

          <div className="flex gap-3">
            <Button variant="destructive" onClick={handleDelete}>
              Yes, Delete League
            </Button>
            <Button variant="outline" onClick={() => navigate(-1)}>
              Cancel
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
