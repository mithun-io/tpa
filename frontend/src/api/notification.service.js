import axiosInstance from './axios';

export const getNotifications = async () => {
  const response = await axiosInstance.get('/notifications');
  return response.data;
};

export const markNotificationsAsRead = async () => {
  await axiosInstance.post('/notifications/mark-read');
};
