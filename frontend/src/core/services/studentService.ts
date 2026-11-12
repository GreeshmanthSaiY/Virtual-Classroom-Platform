import { apiClient } from '../api/client';
import {
  ApiResponse,
  StudentDashboardData,
  StudentEnrollment,
  Subject,
  Assignment,
  AssignmentSubmission,
  Examination,
  ExaminationResult,
  LiveSession,
  PageResponse,
} from '@/types';

export const studentService = {
  getDashboard: async (): Promise<StudentDashboardData> => {
    const res = await apiClient.get<ApiResponse<StudentDashboardData>>('/students/dashboard');
    return res.data.data;
  },

  getMyEnrollments: async (): Promise<StudentEnrollment[]> => {
    const res = await apiClient.get<ApiResponse<StudentEnrollment[]>>('/enrollments/my-subjects');
    return res.data.data;
  },

  getEnrolledSubjects: async (): Promise<Subject[]> => {
    const res = await apiClient.get<ApiResponse<Subject[]>>('/students/subjects');
    return res.data.data;
  },

  getAssignmentsForSubject: async (subjectId: number, page = 0, size = 20): Promise<PageResponse<Assignment>> => {
    const res = await apiClient.get<ApiResponse<PageResponse<Assignment>>>(`/assignments/subject/${subjectId}`, {
      params: { page, size },
    });
    return res.data.data;
  },

  submitAssignment: async (
    assignmentId: number,
    data: { submissionText?: string; fileUrl?: string }
  ): Promise<AssignmentSubmission> => {
    const res = await apiClient.post<ApiResponse<AssignmentSubmission>>(
      `/assignments/${assignmentId}/submit`,
      { assignmentId, ...data }
    );
    return res.data.data;
  },

  getMySubmissions: async (): Promise<AssignmentSubmission[]> => {
    const res = await apiClient.get<ApiResponse<AssignmentSubmission[]>>('/assignments/my-submissions');
    return res.data.data;
  },

  getExaminationsForSubject: async (subjectId: number, page = 0, size = 20): Promise<PageResponse<Examination>> => {
    const res = await apiClient.get<ApiResponse<PageResponse<Examination>>>(`/examinations/subject/${subjectId}`, {
      params: { page, size },
    });
    return res.data.data;
  },

  getMyResults: async (): Promise<ExaminationResult[]> => {
    const res = await apiClient.get<ApiResponse<ExaminationResult[]>>('/examinations/my-results');
    return res.data.data;
  },

  getLiveSessionsForSubject: async (subjectId: number, page = 0, size = 20): Promise<PageResponse<LiveSession>> => {
    const res = await apiClient.get<ApiResponse<PageResponse<LiveSession>>>(`/sessions/subject/${subjectId}`, {
      params: { page, size },
    });
    return res.data.data;
  },
};
