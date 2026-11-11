export type Role = 'ROLE_STUDENT' | 'ROLE_FACULTY' | 'ROLE_ADMIN';
export type UserStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'SUSPENDED';

export interface User {
  id: number;
  email: string;
  fullName: string;
  role: Role;
  status: UserStatus;
  rejectionReason?: string;
  rollNumber?: string;
  employeeId?: string;
  department?: string;
  courseName?: string;
  currentYear?: number;
  createdAt: string;
}

export interface AuthResponse {
  token: string;
  tokenType: string;
  id: number;
  email: string;
  fullName: string;
  role: Role;
  status: UserStatus;
}

export interface Degree {
  id: number;
  code: string;
  name: string;
  durationYears: number;
  courses?: Course[];
}

export interface Course {
  id: number;
  degreeId: number;
  degreeCode?: string;
  code: string;
  name: string;
  subjects?: Subject[];
}

export interface Subject {
  id: number;
  courseId: number;
  courseName?: string;
  code: string;
  title: string;
  yearLevel: number;
}

export interface StudyMaterial {
  id: number;
  subjectId: number;
  subjectTitle: string;
  uploaderId: number;
  uploaderName: string;
  title: string;
  description?: string;
  fileUrl: string;
  fileSizeBytes: number;
  mimeType: string;
  createdAt: string;
}

export interface VideoLecture {
  id: number;
  subjectId: number;
  subjectTitle: string;
  facultyId: number;
  facultyName: string;
  title: string;
  description?: string;
  videoUrl: string;
  durationSeconds: number;
  createdAt: string;
}

export interface Question {
  id: number;
  subjectId: number;
  subjectTitle: string;
  authorId: number;
  authorName: string;
  authorRole: Role;
  title: string;
  body: string;
  isResolved: boolean;
  answersCount: number;
  answers?: Answer[];
  createdAt: string;
}

export interface Answer {
  id: number;
  questionId: number;
  authorId: number;
  authorName: string;
  authorRole: Role;
  body: string;
  isAccepted: boolean;
  createdAt: string;
}

export interface NotificationItem {
  id: number;
  title: string;
  message: string;
  type: string;
  isRead: boolean;
  createdAt: string;
}

export interface PlatformMetrics {
  totalStudents: number;
  totalFaculty: number;
  pendingStudentApprovals: number;
  pendingFacultyApprovals: number;
  totalCourses: number;
  totalSubjects: number;
  totalMaterials: number;
  totalLectures: number;
  totalQuestions: number;
  resolvedQuestions: number;
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
}

