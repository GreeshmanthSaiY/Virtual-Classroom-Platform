package com.virtualclassroom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Aggregated student academic dashboard data")
public class StudentDashboardDto {

    private CourseDto course;
    private List<SubjectDto> enrolledSubjects;
    private List<LiveSessionDto> upcomingLiveSessions;
    private List<AssignmentDto> pendingAssignments;
    private List<AssignmentSubmissionDto> recentSubmissions;
    private List<ExaminationResultDto> recentExamResults;
    private long unreadNotificationCount;

    public StudentDashboardDto() {}

    public StudentDashboardDto(
            CourseDto course,
            List<SubjectDto> enrolledSubjects,
            List<LiveSessionDto> upcomingLiveSessions,
            List<AssignmentDto> pendingAssignments,
            List<AssignmentSubmissionDto> recentSubmissions,
            List<ExaminationResultDto> recentExamResults,
            long unreadNotificationCount
    ) {
        this.course = course;
        this.enrolledSubjects = enrolledSubjects;
        this.upcomingLiveSessions = upcomingLiveSessions;
        this.pendingAssignments = pendingAssignments;
        this.recentSubmissions = recentSubmissions;
        this.recentExamResults = recentExamResults;
        this.unreadNotificationCount = unreadNotificationCount;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private CourseDto course;
        private List<SubjectDto> enrolledSubjects;
        private List<LiveSessionDto> upcomingLiveSessions;
        private List<AssignmentDto> pendingAssignments;
        private List<AssignmentSubmissionDto> recentSubmissions;
        private List<ExaminationResultDto> recentExamResults;
        private long unreadNotificationCount;

        public Builder course(CourseDto course) { this.course = course; return this; }
        public Builder enrolledSubjects(List<SubjectDto> enrolledSubjects) { this.enrolledSubjects = enrolledSubjects; return this; }
        public Builder upcomingLiveSessions(List<LiveSessionDto> sessions) { this.upcomingLiveSessions = sessions; return this; }
        public Builder pendingAssignments(List<AssignmentDto> assignments) { this.pendingAssignments = assignments; return this; }
        public Builder recentSubmissions(List<AssignmentSubmissionDto> submissions) { this.recentSubmissions = submissions; return this; }
        public Builder recentExamResults(List<ExaminationResultDto> examResults) { this.recentExamResults = examResults; return this; }
        public Builder unreadNotificationCount(long count) { this.unreadNotificationCount = count; return this; }

        public StudentDashboardDto build() {
            return new StudentDashboardDto(course, enrolledSubjects, upcomingLiveSessions, pendingAssignments, recentSubmissions, recentExamResults, unreadNotificationCount);
        }
    }

    public CourseDto getCourse() { return course; }
    public void setCourse(CourseDto course) { this.course = course; }
    public List<SubjectDto> getEnrolledSubjects() { return enrolledSubjects; }
    public void setEnrolledSubjects(List<SubjectDto> enrolledSubjects) { this.enrolledSubjects = enrolledSubjects; }
    public List<LiveSessionDto> getUpcomingLiveSessions() { return upcomingLiveSessions; }
    public void setUpcomingLiveSessions(List<LiveSessionDto> upcomingLiveSessions) { this.upcomingLiveSessions = upcomingLiveSessions; }
    public List<AssignmentDto> getPendingAssignments() { return pendingAssignments; }
    public void setPendingAssignments(List<AssignmentDto> pendingAssignments) { this.pendingAssignments = pendingAssignments; }
    public List<AssignmentSubmissionDto> getRecentSubmissions() { return recentSubmissions; }
    public void setRecentSubmissions(List<AssignmentSubmissionDto> recentSubmissions) { this.recentSubmissions = recentSubmissions; }
    public List<ExaminationResultDto> getRecentExamResults() { return recentExamResults; }
    public void setRecentExamResults(List<ExaminationResultDto> recentExamResults) { this.recentExamResults = recentExamResults; }
    public long getUnreadNotificationCount() { return unreadNotificationCount; }
    public void setUnreadNotificationCount(long unreadNotificationCount) { this.unreadNotificationCount = unreadNotificationCount; }
}
