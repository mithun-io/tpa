import React, { createContext, useContext, useState, useEffect } from 'react';
import toast from 'react-hot-toast';
import {
  login as loginService,
  register as registerService,
  verifyOtp as verifyOtpService,
  resendOtp as resendOtpService,
} from '../api/auth.service';

const AuthContext = createContext();

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const token = localStorage.getItem('token');
    const userData = localStorage.getItem('user');
    if (token && userData) {
      try {
        setUser(JSON.parse(userData));
      } catch {
        localStorage.removeItem('user');
      }
    }
    setLoading(false);
  }, []);

  // ── Login ──────────────────────────────────────────────────────────────────
  const login = async (email, password) => {
    try {
      const data = await loginService({ email, password });
      if (data.success) {
        const { token, refreshToken, userResponse } = data.data;
        localStorage.setItem('token', token);
        localStorage.setItem('refreshToken', refreshToken);
        localStorage.setItem('user', JSON.stringify(userResponse));
        setUser(userResponse);
        toast.success(data.message || 'Login successful');
        return { success: true, role: userResponse.userRole };
      } else {
        const msg = data.message || 'Login failed';
        toast.error(msg);
        return { success: false, message: msg };
      }
    } catch (error) {
      const msg = error.response?.data?.message || 'Login failed. Please try again.';
      toast.error(msg);
      return { success: false, message: msg };
    }
  };

  // ── Register (Step 1 — sends OTP) ─────────────────────────────────────────
  const register = async (formData) => {
    try {
      const data = await registerService(formData);
      if (data.success) {
        toast.success('OTP sent to your email!');
        return { success: true };
      } else {
        toast.error(data.message || 'Registration failed');
        return { success: false };
      }
    } catch (error) {
      toast.error(error.response?.data?.message || 'Registration failed. Please try again.');
      return { success: false };
    }
  };

  // ── Verify OTP (Step 2 — activates account) ───────────────────────────────
  const verifyOtp = async (email, otp) => {
    try {
      const data = await verifyOtpService(email, otp);
      if (data.success) {
        toast.success('Account verified! You can now log in.');
        return { success: true };
      } else {
        toast.error(data.message || 'OTP verification failed');
        return { success: false };
      }
    } catch (error) {
      toast.error(error.response?.data?.message || 'Invalid or expired OTP.');
      return { success: false };
    }
  };

  // ── Resend OTP ─────────────────────────────────────────────────────────────
  const resendOtp = async (email) => {
    try {
      const data = await resendOtpService(email);
      if (data.success) {
        toast.success('OTP resent successfully!');
        return { success: true };
      }
      return { success: false };
    } catch (error) {
      toast.error(error.response?.data?.message || 'Failed to resend OTP.');
      return { success: false };
    }
  };

  // ── Logout ─────────────────────────────────────────────────────────────────
  const logout = () => {
    localStorage.removeItem('token');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('user');
    setUser(null);
    toast.success('Logged out successfully');
  };

  return (
    <AuthContext.Provider value={{ user, loading, login, logout, register, verifyOtp, resendOtp }}>
      {!loading && children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);
