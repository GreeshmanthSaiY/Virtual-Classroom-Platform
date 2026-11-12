import { apiClient } from '../api/client';
import {
  ApiResponse,
  AuthResponse,
  LoginRequest,
  StudentRegisterRequest,
  FacultyRegisterRequest,
  ForgotPasswordRequest,
  ResetPasswordRequest,
  ChangePasswordRequest,
  User,
  ProfileUpdateRequest,
} from '@/types';

export const authService = {
  login: async (credentials: LoginRequest): Promise<AuthResponse> => {
    const res = await apiClient.post<ApiResponse<AuthResponse>>('/auth/login', credentials);
    return res.data.data;
  },

  registerStudent: async (data: StudentRegisterRequest): Promise<ApiResponse<void>> => {
    const res = await apiClient.post<ApiResponse<void>>('/auth/register/student', data);
    return res.data;
  },

  registerFaculty: async (data: FacultyRegisterRequest): Promise<ApiResponse<void>> => {
    const res = await apiClient.post<ApiResponse<void>>('/auth/register/faculty', data);
    return res.data;
  },

  refresh: async (): Promise<AuthResponse> => {
    const res = await apiClient.post<ApiResponse<AuthResponse>>('/auth/refresh');
    return res.data.data;
  },

  logout: async (): Promise<void> => {
    try {
      await apiClient.post('/auth/logout');
    } catch {
      // Continue client logout even if backend fails
    }
  },

  forgotPassword: async (data: ForgotPasswordRequest): Promise<ApiResponse<void>> => {
    const res = await apiClient.post<ApiResponse<void>>('/auth/forgot-password', data);
    return res.data;
  },

  resetPassword: async (data: ResetPasswordRequest): Promise<ApiResponse<void>> => {
    const res = await apiClient.post<ApiResponse<void>>('/auth/reset-password', data);
    return res.data;
  },

  changePassword: async (data: ChangePasswordRequest): Promise<ApiResponse<void>> => {
    const res = await apiClient.post<ApiResponse<void>>('/auth/change-password', data);
    return res.data;
  },

  getProfile: async (): Promise<User> => {
    const res = await apiClient.get<ApiResponse<User>>('/users/profile');
    return res.data.data;
  },

  updateProfile: async (data: ProfileUpdateRequest): Promise<User> => {
    const res = await apiClient.put<ApiResponse<User>>('/users/profile', data);
    return res.data.data;
  },
};
