import { describe, it, expect, vi, beforeEach } from 'vitest';
import { apiClient } from '../core/api/client';
import { adminService } from '../core/services/adminService';
import { facultyService } from '../core/services/facultyService';
import { academicContentService } from '../core/services/academicContentService';
import { storageService } from '../core/services/storageService';

describe('Frontend-Backend API Contract Verification', () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  describe('adminService Contract', () => {
    it('searchUsers sends request to /admin/users/search with valid query parameters', async () => {
      const spy = vi.spyOn(apiClient, 'get').mockResolvedValueOnce({
        data: {
          success: true,
          message: 'User search completed',
          data: {
            content: [],
            pageNumber: 0,
            pageSize: 15,
            totalElements: 0,
            totalPages: 1,
            first: true,
            last: true,
          },
        },
      });

      await adminService.searchUsers({
        q: 'Gaurav',
        role: 'ROLE_STUDENT',
        status: 'ACTIVE',
        page: 0,
        size: 15,
      });

      expect(spy).toHaveBeenCalledWith('/admin/users/search', {
        params: {
          q: 'Gaurav',
          role: 'ROLE_STUDENT',
          status: 'ACTIVE',
          page: 0,
          size: 15,
        },
      });
    });
  });

  describe('facultyService Contract', () => {
    it('gradeExamResult includes mandatory grade field conforming to RecordGradeRequest', async () => {
      const spy = vi.spyOn(apiClient, 'post').mockResolvedValueOnce({
        data: {
          success: true,
          message: 'Grade recorded successfully',
          data: {
            id: 1,
            examinationId: 10,
            studentId: 20,
            studentName: 'Student Candidate',
            marksObtained: 88,
            grade: 'A',
            remarks: 'Strong performance',
            gradedAt: '2026-11-12T00:00:00Z',
          },
        },
      });

      await facultyService.gradeExamResult({
        examinationId: 10,
        studentId: 20,
        marksObtained: 88,
        grade: 'A',
        remarks: 'Strong performance',
      });

      expect(spy).toHaveBeenCalledWith('/examinations/grades', {
        examinationId: 10,
        studentId: 20,
        marksObtained: 88,
        grade: 'A',
        remarks: 'Strong performance',
      });
    });
  });

  describe('academicContentService Contract', () => {
    it('getDiscussions targets /discussions/questions', async () => {
      const spy = vi.spyOn(apiClient, 'get').mockResolvedValueOnce({
        data: {
          success: true,
          message: 'Questions retrieved',
          data: [],
        },
      });

      await academicContentService.getDiscussions(5);

      expect(spy).toHaveBeenCalledWith('/discussions/questions', {
        params: { subjectId: 5 },
      });
    });

    it('createQuestion posts to /discussions/questions', async () => {
      const spy = vi.spyOn(apiClient, 'post').mockResolvedValueOnce({
        data: {
          success: true,
          message: 'Question posted',
          data: { id: 1, title: 'Test Q', body: 'Body' },
        },
      });

      await academicContentService.createQuestion({
        subjectId: 3,
        title: 'Binary Search Edge Cases',
        body: 'How to handle empty subarray?',
      });

      expect(spy).toHaveBeenCalledWith('/discussions/questions', {
        subjectId: 3,
        title: 'Binary Search Edge Cases',
        body: 'How to handle empty subarray?',
      });
    });

    it('endorseAnswer patches /discussions/answers/{id}/endorse', async () => {
      const spy = vi.spyOn(apiClient, 'patch').mockResolvedValueOnce({
        data: {
          success: true,
          message: 'Answer endorsed',
          data: { id: 42, endorsedByFaculty: true },
        },
      });

      await academicContentService.endorseAnswer(42);

      expect(spy).toHaveBeenCalledWith('/discussions/answers/42/endorse');
    });
  });

  describe('storageService Contract', () => {
    it('uploadFile posts multipart/form-data to /storage/upload', async () => {
      const spy = vi.spyOn(apiClient, 'post').mockResolvedValueOnce({
        data: {
          success: true,
          message: 'File uploaded',
          data: {
            fileUrl: 'http://localhost:8080/api/v1/storage/files/assignments/hw1.pdf',
            storagePath: 'assignments/hw1.pdf',
            originalFilename: 'hw1.pdf',
            mimeType: 'application/pdf',
            fileSizeBytes: 2048,
          },
        },
      });

      const file = new File(['mock content'], 'hw1.pdf', { type: 'application/pdf' });
      const result = await storageService.uploadFile(file, 'assignments');

      expect(spy).toHaveBeenCalledWith(
        '/storage/upload',
        expect.any(FormData),
        expect.objectContaining({
          headers: { 'Content-Type': 'multipart/form-data' },
        })
      );
      expect(result.fileUrl).toContain('hw1.pdf');
    });

    it('getSignedDownloadUrl requests /storage/signed-download-url with path parameter', async () => {
      const spy = vi.spyOn(apiClient, 'get').mockResolvedValueOnce({
        data: {
          success: true,
          message: 'Signed download URL generated',
          data: 'https://storage.supabase.co/signed/assignments/hw1.pdf?token=abc',
        },
      });

      const url = await storageService.getSignedDownloadUrl('assignments/hw1.pdf', 1800);

      expect(spy).toHaveBeenCalledWith('/storage/signed-download-url', {
        params: { path: 'assignments/hw1.pdf', expiresIn: 1800 },
      });
      expect(url).toContain('https://storage.supabase.co');
    });

    it('deleteFile issues DELETE to /storage/files with storagePath', async () => {
      const spy = vi.spyOn(apiClient, 'delete').mockResolvedValueOnce({
        data: { success: true, message: 'File deleted' },
      });

      await storageService.deleteFile('materials/lecture1.pdf');

      expect(spy).toHaveBeenCalledWith('/storage/files', {
        params: { path: 'materials/lecture1.pdf' },
      });
    });
  });
});
