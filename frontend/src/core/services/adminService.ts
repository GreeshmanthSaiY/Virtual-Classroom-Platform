import { apiClient } from '../api/client';
import {
  ApiResponse,
  PlatformMetrics,
  User,
  Course,
  PageResponse,
} from '@/types';

export const adminService = {
  getMetrics: async (): Promise<PlatformMetrics> => {
    const res = await apiClient.get<ApiResponse<PlatformMetrics>>('/admin/metrics');
    return res.data.data;
  },

  getUsers: async (params?: { page?: number; size?: number; role?: string; status?: string }): Promise<PageResponse<User>> => {
    const res = await apiClient.get<ApiResponse<PageResponse<User>>>('/admin/users', { params });
    return res.data.data;
  },

  searchUsers: async (params?: {
    q?: string;
    role?: string;
    status?: string;
    page?: number;
    size?: number;
    sort?: string;
  }): Promise<PageResponse<User>> => {
    const res = await apiClient.get<ApiResponse<PageResponse<User>>>('/admin/users/search', { params });
    return res.data.data;
  },

  getPendingStudents: async (): Promise<User[]> => {
    const res = await apiClient.get<ApiResponse<User[]>>('/admin/students/pending');
    return res.data.data;
  },

  getPendingFaculty: async (): Promise<User[]> => {
    const res = await apiClient.get<ApiResponse<User[]>>('/admin/faculty/pending');
    return res.data.data;
  },

  approveUser: async (id: number): Promise<void> => {
    await apiClient.post('/admin/approvals', { userIds: [id] });
  },

  rejectUser: async (id: number, reason: string): Promise<void> => {
    await apiClient.post('/admin/rejections', { userIds: [id], reason });
  },

  suspendUser: async (id: number, reason: string): Promise<void> => {
    await apiClient.post(`/admin/users/${id}/suspend`, { reason });
  },

  reactivateUser: async (id: number): Promise<void> => {
    await apiClient.post(`/admin/users/${id}/reactivate`);
  },

  getCourses: async (): Promise<Course[]> => {
    const res = await apiClient.get<ApiResponse<Course[]>>('/courses');
    return res.data.data;
  },

  getDegrees: async () => {
    const res = await apiClient.get<ApiResponse<any[]>>('/courses/degrees');
    return res.data.data;
  },

  archiveCourse: async (id: number): Promise<void> => {
    await apiClient.patch(`/courses/${id}/archive`);
  },

  unarchiveCourse: async (id: number): Promise<void> => {
    await apiClient.patch(`/courses/${id}/unarchive`);
  },

  assignFacultyToSubject: async (subjectId: number, facultyId: number, academicTerm: string = 'Current'): Promise<void> => {
    await apiClient.post(`/admin/subjects/${subjectId}/assign-faculty`, { facultyId, academicTerm });
  },

  getAuditLogs: async (page = 0, size = 20) => {
    const res = await apiClient.get<ApiResponse<PageResponse<any>>>('/admin/audit-logs', {
      params: { page, size },
    });
    return res.data.data;
  },
};
