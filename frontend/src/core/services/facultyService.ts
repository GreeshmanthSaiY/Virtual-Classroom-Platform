import { apiClient } from '../api/client';
import {
  ApiResponse,
  FacultyDashboardData,
  Subject,
  User,
  Assignment,
  AssignmentCreateRequest,
  AssignmentSubmission,
  Examination,
  ExaminationCreateRequest,
  ExaminationResult,
  LiveSession,
  LiveSessionCreateRequest,
  PageResponse,
} from '@/types';

export const facultyService = {
  getDashboard: async (): Promise<FacultyDashboardData> => {
    const res = await apiClient.get<ApiResponse<FacultyDashboardData>>('/faculty/dashboard');
    return res.data.data;
  },

  getMySubjects: async (): Promise<Subject[]> => {
    const res = await apiClient.get<ApiResponse<Subject[]>>('/faculty/subjects');
    return res.data.data;
  },

  getEnrolledStudents: async (subjectId: number): Promise<User[]> => {
    const res = await apiClient.get<ApiResponse<User[]>>(`/faculty/subjects/${subjectId}/students`);
    return res.data.data;
  },

  getAssignmentsForSubject: async (subjectId: number, page = 0, size = 20): Promise<PageResponse<Assignment>> => {
    const res = await apiClient.get<ApiResponse<PageResponse<Assignment>>>(`/assignments/subject/${subjectId}`, {
      params: { page, size },
    });
    return res.data.data;
  },

  createAssignment: async (data: AssignmentCreateRequest): Promise<Assignment> => {
    const res = await apiClient.post<ApiResponse<Assignment>>('/assignments', data);
    return res.data.data;
  },

  getSubmissions: async (assignmentId: number): Promise<AssignmentSubmission[]> => {
    const res = await apiClient.get<ApiResponse<AssignmentSubmission[]>>(`/assignments/${assignmentId}/submissions`);
    return res.data.data;
  },

  gradeSubmission: async (
    submissionId: number,
    data: { gradePoints: number; feedback?: string }
  ): Promise<AssignmentSubmission> => {
    const res = await apiClient.post<ApiResponse<AssignmentSubmission>>(
      `/assignments/submissions/${submissionId}/grade`,
      data
    );
    return res.data.data;
  },

  getExaminationsForSubject: async (subjectId: number, page = 0, size = 20): Promise<PageResponse<Examination>> => {
    const res = await apiClient.get<ApiResponse<PageResponse<Examination>>>(`/examinations/subject/${subjectId}`, {
      params: { page, size },
    });
    return res.data.data;
  },

  createExamination: async (data: ExaminationCreateRequest): Promise<Examination> => {
    const res = await apiClient.post<ApiResponse<Examination>>('/examinations', data);
    return res.data.data;
  },

  getExamResults: async (examinationId: number): Promise<ExaminationResult[]> => {
    const res = await apiClient.get<ApiResponse<ExaminationResult[]>>(`/examinations/${examinationId}/results`);
    return res.data.data;
  },

  gradeExamResult: async (
    data: { examinationId: number; studentId: number; marksObtained: number; grade: string; remarks?: string }
  ): Promise<ExaminationResult> => {
    const res = await apiClient.post<ApiResponse<ExaminationResult>>('/examinations/grades', data);
    return res.data.data;
  },

  getLiveSessionsForSubject: async (subjectId: number, page = 0, size = 20): Promise<PageResponse<LiveSession>> => {
    const res = await apiClient.get<ApiResponse<PageResponse<LiveSession>>>(`/sessions/subject/${subjectId}`, {
      params: { page, size },
    });
    return res.data.data;
  },

  createLiveSession: async (data: LiveSessionCreateRequest): Promise<LiveSession> => {
    const res = await apiClient.post<ApiResponse<LiveSession>>('/sessions', data);
    return res.data.data;
  },

  updateLiveSessionStatus: async (sessionId: number, status: string): Promise<LiveSession> => {
    const res = await apiClient.patch<ApiResponse<LiveSession>>(`/sessions/${sessionId}/status`, null, {
      params: { status },
    });
    return res.data.data;
  },
};
