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
  avatarUrl?: string;
  phoneNumber?: string;
  bio?: string;
  designation?: string;
  officeHours?: string;
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
  refreshToken?: string | null;
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
  archived?: boolean;
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
  isEndorsed?: boolean;
  endorsedById?: number;
  endorsedByName?: string;
  updatedAt?: string;
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
  totalAssignments?: number;
  totalExaminations?: number;
  upcomingLiveSessions?: number;
  suspendedUsers?: number;
  approvedUsers?: number;
}

export interface Assignment {
  id: number;
  subjectId: number;
  subjectTitle: string;
  facultyId: number;
  facultyName: string;
  title: string;
  description?: string;
  dueDate: string;
  maxPoints: number;
  attachmentUrl?: string;
  createdAt: string;
}

export type SubmissionStatus = 'SUBMITTED' | 'LATE' | 'GRADED' | 'RESUBMITTED';

export interface AssignmentSubmission {
  id: number;
  assignmentId: number;
  assignmentTitle: string;
  studentId: number;
  studentName: string;
  studentRollNumber?: string;
  submissionText?: string;
  fileUrl?: string;
  status: SubmissionStatus;
  gradePoints?: number;
  maxPoints?: number;
  feedback?: string;
  submittedAt: string;
  gradedAt?: string;
}

export type ExamType = 'MIDTERM' | 'FINAL' | 'QUIZ' | 'ASSIGNMENT_ASSESSMENT';

export interface Examination {
  id: number;
  subjectId: number;
  subjectTitle: string;
  creatorId: number;
  creatorName: string;
  title: string;
  examType: ExamType;
  totalMarks: number;
  durationMinutes: number;
  scheduledAt: string;
  instructions?: string;
  status?: string;
  createdAt: string;
}

export interface ExaminationResult {
  id: number;
  examinationId: number;
  examinationTitle: string;
  studentId: number;
  studentName: string;
  studentRollNumber?: string;
  marksObtained: number;
  totalMarks: number;
  grade: string;
  remarks?: string;
  gradedAt: string;
}

export type LiveSessionStatus = 'SCHEDULED' | 'LIVE' | 'COMPLETED' | 'CANCELLED';

export interface LiveSession {
  id: number;
  subjectId: number;
  subjectTitle: string;
  hostId: number;
  hostName: string;
  title: string;
  description?: string;
  meetingLink: string;
  platform: string;
  scheduledStart: string;
  scheduledEnd: string;
  status: LiveSessionStatus;
  createdAt: string;
}

export interface StudentEnrollment {
  id: number;
  studentId: number;
  studentName: string;
  subjectId: number;
  subjectCode: string;
  subjectTitle: string;
  courseName: string;
  academicTerm: string;
  status: string;
  enrolledAt: string;
}

export interface StudentDashboardData {
  course: Course | null;
  enrolledSubjects: Subject[];
  upcomingLiveSessions: LiveSession[];
  pendingAssignments: Assignment[];
  recentSubmissions: AssignmentSubmission[];
  recentExamResults: ExaminationResult[];
  unreadNotificationCount: number;
}

export interface FacultyDashboardData {
  assignedSubjects: Subject[];
  totalMaterialsUploaded: number;
  totalLecturesPublished: number;
  upcomingLiveSessions: LiveSession[];
  pendingSubmissionsToGrade: AssignmentSubmission[];
  pendingSubmissionsCount: number;
  recentForumQuestions: Question[];
}

export interface ApiResponse<T> {
  success: boolean;
  message: string;
  data: T;
  timestamp?: string;
}

export interface ApiErrorResponse {
  status: number;
  error: string;
  message: string;
  timestamp: string;
  path: string;
  validationErrors?: Record<string, string>;
}

export interface PageResponse<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface StudentRegisterRequest {
  email: string;
  password: string;
  fullName: string;
  courseId: number;
  currentYear: number;
  rollNumber: string;
  phoneNumber?: string;
}

export interface FacultyRegisterRequest {
  email: string;
  password: string;
  fullName: string;
  department: string;
  qualification: string;
  designation?: string;
  employeeId?: string;
  phoneNumber?: string;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  token: string;
  newPassword: string;
}

export interface ChangePasswordRequest {
  currentPassword: string;
  newPassword: string;
}

export interface ProfileUpdateRequest {
  fullName?: string;
  phoneNumber?: string;
  bio?: string;
  avatarUrl?: string;
  officeHours?: string;
}

export interface CourseCreateRequest {
  degreeId: number;
  code: string;
  name: string;
}

export interface SubjectCreateRequest {
  courseId: number;
  code: string;
  title: string;
  yearLevel: number;
}

export interface AssignmentCreateRequest {
  subjectId: number;
  title: string;
  description?: string;
  dueDate: string;
  maxPoints: number;
  attachmentUrl?: string;
}

export interface ExaminationCreateRequest {
  subjectId: number;
  title: string;
  examType: ExamType;
  totalMarks: number;
  durationMinutes: number;
  scheduledAt: string;
  instructions?: string;
}

export interface LiveSessionCreateRequest {
  subjectId: number;
  title: string;
  description?: string;
  meetingLink: string;
  platform?: string;
  scheduledStart: string;
  scheduledEnd: string;
}

