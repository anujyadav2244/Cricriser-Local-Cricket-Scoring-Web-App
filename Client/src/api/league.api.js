import axios from "./axios";

export const leagueApi = {
  getAll: () => axios.get("/api/leagues"),

  getById: (id) => axios.get(`/api/leagues/id/${id}`),

  delete: (id) => axios.delete(`/api/leagues/delete/${id}`),

  update: (id, formData) =>
    axios.put(`/api/leagues/update/${id}`, formData, {
      headers: { "Content-Type": "multipart/form-data" },
    }),
};
