import { apiClient } from '../api/client';
import { ApiResponse, NotificationItem } from '@/types';

export const notificationService = {
  getNotifications: async (): Promise<NotificationItem[]> => {
    const res = await apiClient.get<ApiResponse<NotificationItem[]>>('/notifications');
    return res.data.data;
  },

  markAsRead: async (id: number): Promise<void> => {
    await apiClient.patch(`/notifications/${id}/read`);
  },

  markAllAsRead: async (): Promise<void> => {
    await apiClient.patch('/notifications/read-all');
  },
};
