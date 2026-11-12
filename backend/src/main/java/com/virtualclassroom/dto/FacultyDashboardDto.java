package com.virtualclassroom.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Aggregated faculty academic dashboard metrics and activity")
public class FacultyDashboardDto {

    private List<SubjectDto> assignedSubjects;
    private long totalMaterialsUploaded;
    private long totalLecturesPublished;
    private List<LiveSessionDto> upcomingLiveSessions;
    private List<AssignmentSubmissionDto> pendingSubmissionsToGrade;
    private long pendingSubmissionsCount;
    private List<QuestionDto> recentForumQuestions;

    public FacultyDashboardDto() {}

    public FacultyDashboardDto(
            List<SubjectDto> assignedSubjects,
            long totalMaterialsUploaded,
            long totalLecturesPublished,
            List<LiveSessionDto> upcomingLiveSessions,
            List<AssignmentSubmissionDto> pendingSubmissionsToGrade,
            long pendingSubmissionsCount,
            List<QuestionDto> recentForumQuestions
    ) {
        this.assignedSubjects = assignedSubjects;
        this.totalMaterialsUploaded = totalMaterialsUploaded;
        this.totalLecturesPublished = totalLecturesPublished;
        this.upcomingLiveSessions = upcomingLiveSessions;
        this.pendingSubmissionsToGrade = pendingSubmissionsToGrade;
        this.pendingSubmissionsCount = pendingSubmissionsCount;
        this.recentForumQuestions = recentForumQuestions;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private List<SubjectDto> assignedSubjects;
        private long totalMaterialsUploaded;
        private long totalLecturesPublished;
        private List<LiveSessionDto> upcomingLiveSessions;
        private List<AssignmentSubmissionDto> pendingSubmissionsToGrade;
        private long pendingSubmissionsCount;
        private List<QuestionDto> recentForumQuestions;

        public Builder assignedSubjects(List<SubjectDto> assignedSubjects) { this.assignedSubjects = assignedSubjects; return this; }
        public Builder totalMaterialsUploaded(long total) { this.totalMaterialsUploaded = total; return this; }
        public Builder totalLecturesPublished(long total) { this.totalLecturesPublished = total; return this; }
        public Builder upcomingLiveSessions(List<LiveSessionDto> sessions) { this.upcomingLiveSessions = sessions; return this; }
        public Builder pendingSubmissionsToGrade(List<AssignmentSubmissionDto> submissions) { this.pendingSubmissionsToGrade = submissions; return this; }
        public Builder pendingSubmissionsCount(long count) { this.pendingSubmissionsCount = count; return this; }
        public Builder recentForumQuestions(List<QuestionDto> questions) { this.recentForumQuestions = questions; return this; }

        public FacultyDashboardDto build() {
            return new FacultyDashboardDto(assignedSubjects, totalMaterialsUploaded, totalLecturesPublished, upcomingLiveSessions, pendingSubmissionsToGrade, pendingSubmissionsCount, recentForumQuestions);
        }
    }

    public List<SubjectDto> getAssignedSubjects() { return assignedSubjects; }
    public void setAssignedSubjects(List<SubjectDto> assignedSubjects) { this.assignedSubjects = assignedSubjects; }
    public long getTotalMaterialsUploaded() { return totalMaterialsUploaded; }
    public void setTotalMaterialsUploaded(long totalMaterialsUploaded) { this.totalMaterialsUploaded = totalMaterialsUploaded; }
    public long getTotalLecturesPublished() { return totalLecturesPublished; }
    public void setTotalLecturesPublished(long totalLecturesPublished) { this.totalLecturesPublished = totalLecturesPublished; }
    public List<LiveSessionDto> getUpcomingLiveSessions() { return upcomingLiveSessions; }
    public void setUpcomingLiveSessions(List<LiveSessionDto> upcomingLiveSessions) { this.upcomingLiveSessions = upcomingLiveSessions; }
    public List<AssignmentSubmissionDto> getPendingSubmissionsToGrade() { return pendingSubmissionsToGrade; }
    public void setPendingSubmissionsToGrade(List<AssignmentSubmissionDto> pendingSubmissionsToGrade) { this.pendingSubmissionsToGrade = pendingSubmissionsToGrade; }
    public long getPendingSubmissionsCount() { return pendingSubmissionsCount; }
    public void setPendingSubmissionsCount(long pendingSubmissionsCount) { this.pendingSubmissionsCount = pendingSubmissionsCount; }
    public List<QuestionDto> getRecentForumQuestions() { return recentForumQuestions; }
    public void setRecentForumQuestions(List<QuestionDto> recentForumQuestions) { this.recentForumQuestions = recentForumQuestions; }
}
