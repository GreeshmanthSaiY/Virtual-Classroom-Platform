import { apiClient } from '../api/client';
import {
  ApiResponse,
  FileUploadResponse,
  SignedUploadUrlRequest,
  SignedUploadUrlResponse,
} from '@/types';

export const storageService = {
  /**
   * Uploads a file multipart attachment to secure server storage
   */
  uploadFile: async (file: File, folder: string = 'general'): Promise<FileUploadResponse> => {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('folder', folder);

    const res = await apiClient.post<ApiResponse<FileUploadResponse>>('/storage/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    return res.data.data;
  },

  /**
   * Generates a signed direct upload URL for client uploads
   */
  getSignedUploadUrl: async (request: SignedUploadUrlRequest): Promise<SignedUploadUrlResponse> => {
    const res = await apiClient.post<ApiResponse<SignedUploadUrlResponse>>(
      '/storage/signed-upload-url',
      request
    );
    return res.data.data;
  },

  /**
   * Generates a secure, time-limited download URL for a storage object
   */
  getSignedDownloadUrl: async (storagePath: string, expiresIn: number = 3600): Promise<string> => {
    const res = await apiClient.get<ApiResponse<string>>('/storage/signed-download-url', {
      params: { path: storagePath, expiresIn },
    });
    return res.data.data;
  },

  /**
   * Deletes an uploaded file by path (authorized faculty/admin)
   */
  deleteFile: async (storagePath: string): Promise<void> => {
    await apiClient.delete('/storage/files', {
      params: { path: storagePath },
    });
  },
};
