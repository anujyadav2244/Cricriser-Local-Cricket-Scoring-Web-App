import axios from "./axios";

export const teamApi = {
  // ================= GET =================
  getAll: () => axios.get("/api/teams/get-all"),

  getById: (id) => axios.get(`/api/teams/${id}`),

  // ================= CREATE =================
  create: (formData) =>
    axios.post("/api/teams/create", formData, {
      headers: { "Content-Type": "multipart/form-data" },
    }),

  // ================= UPDATE =================
  update: (id, formData) =>
    axios.put(`/api/teams/update/${id}`, formData, {
      headers: { "Content-Type": "multipart/form-data" },
    }),

  // ================= DELETE =================
  delete: (id) => axios.delete(`/api/teams/delete/${id}`),

  // ================= ADD TEAM TO LEAGUE (BY TEAM NAME) =================
  addTeamToLeague: (leagueId, teamName) => {
    const formData = new FormData();
    formData.append("teamName", teamName);

    return axios.post(
      `/api/teams/add-to-league/${leagueId}`,
      formData,
      {
        headers: { "Content-Type": "multipart/form-data" },
      }
    );
  },
};
