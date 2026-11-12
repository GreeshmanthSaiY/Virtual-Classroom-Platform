import { apiClient } from '../api/client';
import { ApiResponse, Course, StudyMaterial, VideoLecture, Question, Answer } from '@/types';

export const academicContentService = {
  getCourses: async (): Promise<Course[]> => {
    const res = await apiClient.get<ApiResponse<Course[]>>('/courses');
    return res.data.data;
  },

  getCourseById: async (id: number): Promise<Course> => {
    const res = await apiClient.get<ApiResponse<Course>>(`/courses/${id}`);
    return res.data.data;
  },

  getMaterials: async (subjectId?: number): Promise<StudyMaterial[]> => {
    const res = await apiClient.get<ApiResponse<StudyMaterial[]>>('/materials', {
      params: subjectId ? { subjectId } : undefined,
    });
    return res.data.data;
  },

  getLectures: async (subjectId?: number): Promise<VideoLecture[]> => {
    const res = await apiClient.get<ApiResponse<VideoLecture[]>>('/lectures', {
      params: subjectId ? { subjectId } : undefined,
    });
    return res.data.data;
  },

  getDiscussions: async (subjectId?: number): Promise<Question[]> => {
    const res = await apiClient.get<ApiResponse<Question[]>>('/discussions', {
      params: subjectId ? { subjectId } : undefined,
    });
    return res.data.data;
  },

  createQuestion: async (data: { subjectId: number; title: string; body: string }): Promise<Question> => {
    const res = await apiClient.post<ApiResponse<Question>>('/discussions', data);
    return res.data.data;
  },

  addAnswer: async (questionId: number, body: string): Promise<Answer> => {
    const res = await apiClient.post<ApiResponse<Answer>>(`/discussions/${questionId}/answers`, { body });
    return res.data.data;
  },

  endorseAnswer: async (answerId: number): Promise<Answer> => {
    const res = await apiClient.post<ApiResponse<Answer>>(`/discussions/answers/${answerId}/endorse`);
    return res.data.data;
  },
};
