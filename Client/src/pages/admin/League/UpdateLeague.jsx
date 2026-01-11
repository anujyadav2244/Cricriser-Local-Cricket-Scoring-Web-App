import { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { Card, CardContent } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Separator } from "@/components/ui/separator";
import { leagueApi } from "@/api/league.api";
import { toast } from "sonner";

export default function UpdateLeague() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [form, setForm] = useState({});
  const [logo, setLogo] = useState(null);
  const [preview, setPreview] = useState(null);
  const [loading, setLoading] = useState(false);

  const update = (k, v) => setForm((p) => ({ ...p, [k]: v }));

  useEffect(() => {
    leagueApi
      .getById(id)
      .then((res) => {
        setForm(res.data);
        setPreview(res.data.logoUrl);
      })
      .catch(() => toast.error("Failed to load league"));
  }, [id]);

  const handleSubmit = async () => {
    try {
      setLoading(true);

      const fd = new FormData();
      fd.append("leagueJson", JSON.stringify(form));
      if (logo) fd.append("logo", logo);

      await leagueApi.update(id, fd);

      toast.success("League updated successfully");
      navigate("/admin/leagues");
    } catch (e) {
      toast.error(e.response?.data?.message || "Update failed");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto p-6">
      <Card>
        <CardContent className="space-y-6 p-6">
          <h1 className="text-xl font-semibold">Update League</h1>
          <Separator />

          <Input
            value={form.name || ""}
            onChange={(e) => update("name", e.target.value)}
            placeholder="League name"
          />

          <Input
            value={form.tour || ""}
            onChange={(e) => update("tour", e.target.value)}
            placeholder="Venue / Tour"
          />

          <div className="grid grid-cols-2 gap-4">
            <Input
              type="date"
              value={form.startDate || ""}
              onChange={(e) => update("startDate", e.target.value)}
            />
            <Input
              type="date"
              value={form.endDate || ""}
              onChange={(e) => update("endDate", e.target.value)}
            />
          </div>

          <div className="space-y-2">
            <input
              type="file"
              accept="image/*"
              onChange={(e) => {
                setLogo(e.target.files[0]);
                setPreview(URL.createObjectURL(e.target.files[0]));
              }}
            />

            {preview && (
              <img
                src={preview}
                alt="preview"
                className="h-28 w-28 border rounded object-contain"
              />
            )}
          </div>

          <Button onClick={handleSubmit} disabled={loading}>
            {loading ? "Updating..." : "Update League"}
          </Button>
        </CardContent>
      </Card>
    </div>
  );
}
