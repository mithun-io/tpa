import axiosInstance from './axios';

export const getDashboardAnalytics = async () => {
  const response = await axiosInstance.get('/analytics/dashboard');
  return response.data;
};
