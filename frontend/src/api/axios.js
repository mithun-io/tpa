import axios from 'axios';
import toast from 'react-hot-toast';

const baseURL = import.meta.env.VITE_API_BASE_URL || '/api/v1';

const axiosInstance = axios.create({
  baseURL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor for API calls
axiosInstance.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor for API calls
axiosInstance.interceptors.response.use(
  (response) => {
    return response;
  },
  async (error) => {
    const originalRequest = error.config;
    
    // Handle 401 Unauthorized
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;
      
      try {
        const refreshToken = localStorage.getItem('refreshToken');
        if (refreshToken) {
          // Attempt to refresh token
          const res = await axios.post(`${baseURL}/auth/refresh`, { refreshToken });
          
          if (res.data?.success) {
            const { token, refreshToken: newRefreshToken } = res.data.data;
            localStorage.setItem('token', token);
            localStorage.setItem('refreshToken', newRefreshToken);
            
            // Re-run original request
            axiosInstance.defaults.headers.common['Authorization'] = `Bearer ${token}`;
            return axiosInstance(originalRequest);
          }
        }
      } catch (refreshError) {
        // Refresh token failed, force logout
        localStorage.removeItem('token');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('user');
        window.location.href = '/login';
        toast.error('Session expired. Please log in again.');
      }
      
      // If no refresh token or refresh failed
      localStorage.removeItem('token');
      window.location.href = '/login';
    }
    
    // Global error handler
    const errorMessage = error.response?.data?.message || 'An error occurred. Please try again.';
    // Don't toast if it's just a 401, as we handled it above
    if (error.response?.status !== 401) {
      toast.error(errorMessage);
    }
    
    return Promise.reject(error);
  }
);

export default axiosInstance;
