import axiosInstance from './axios';

export const login = async (credentials) => {
  const response = await axiosInstance.post('/auth/login', credentials);
  return response.data;
};

export const register = async (userData) => {
  const response = await axiosInstance.post('/auth/customer/register', userData);
  return response.data;
};

export const verifyOtp = async (email, otp) => {
  const response = await axiosInstance.patch('/auth/customer/verify', { email, otp });
  return response.data;
};

export const resendOtp = async (email) => {
  const response = await axiosInstance.patch(`/auth/resend-otp/${email}`);
  return response.data;
};

/**
 * Step 1: Send OTP to email for password reset.
 * Backend: PATCH /api/v1/auth/forget-password/{email}
 */
export const forgotPassword = async (email) => {
  const response = await axiosInstance.patch(`/auth/forget-password/${email}`);
  return response.data;
};

/**
 * Step 2+3: Submit email + OTP + new password to reset.
 * Backend: PATCH /api/v1/auth/password-reset
 */
export const resetPassword = async ({ email, otp, newPassword }) => {
  const response = await axiosInstance.patch('/auth/password-reset', { email, otp, newPassword });
  return response.data;
};

/**
 * Authenticated: Change password (requires valid JWT).
 * Backend: PATCH /api/v1/auth/password-change
 */
export const changePassword = async ({ previousPassword, newPassword }) => {
  const response = await axiosInstance.patch('/auth/password-change', { previousPassword, newPassword });
  return response.data;
};
