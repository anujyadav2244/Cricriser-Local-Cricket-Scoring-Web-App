import api from "./axios"

export const loginAdmin = async ({ email, password }) => {
  const res = await api.post("/api/admin/login", {
    email,
    password,
  })
  return res.data
}

export const getCurrentAdmin = async () => {
  const res = await api.get("/api/admin/me")
  return res.data
}


export const verifySignupOtp = async ({ email, otp }) => {
  const res = await api.post("/api/admin/verify-otp", {
    email,
    otp,
  })
  return res.data
}

export const registerAdmin = async ({ name, email, password }) => {
  const res = await api.post("/api/admin/signup", {
    name,
    email,
    password,
  })
  return res.data
}


export const forgotPassword = async (email) => {
  const res = await api.post("/api/admin/forgot-password", {
    email,
  })
  return res.data
}


export const verifyForgotOtp = async ({ email, otp, newPassword }) => {
  const res = await api.post("/api/admin/verify-forgot-otp", {
    email,
    otp,
    newPassword,
  })
  return res.data
}

export const resetPassword = (data) => {
  return axios.put("/api/admin/reset-password", data);
};