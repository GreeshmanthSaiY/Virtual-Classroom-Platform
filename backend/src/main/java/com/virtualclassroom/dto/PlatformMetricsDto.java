package com.virtualclassroom.dto;


public class PlatformMetricsDto {

    private long totalStudents;
    private long totalFaculty;
    private long pendingStudentApprovals;
    private long pendingFacultyApprovals;
    private long totalCourses;
    private long totalSubjects;
    private long totalMaterials;
    private long totalLectures;
    private long totalQuestions;
    private long resolvedQuestions;

    public PlatformMetricsDto() {}

    public PlatformMetricsDto(long totalStudents, long totalFaculty, long pendingStudentApprovals, long pendingFacultyApprovals, long totalCourses, long totalSubjects, long totalMaterials, long totalLectures, long totalQuestions, long resolvedQuestions) {
        this.totalStudents = totalStudents;
        this.totalFaculty = totalFaculty;
        this.pendingStudentApprovals = pendingStudentApprovals;
        this.pendingFacultyApprovals = pendingFacultyApprovals;
        this.totalCourses = totalCourses;
        this.totalSubjects = totalSubjects;
        this.totalMaterials = totalMaterials;
        this.totalLectures = totalLectures;
        this.totalQuestions = totalQuestions;
        this.resolvedQuestions = resolvedQuestions;
    }

    public long getTotalStudents() { return totalStudents; }
    public void setTotalStudents(long totalStudents) { this.totalStudents = totalStudents; }
    public long getTotalFaculty() { return totalFaculty; }
    public void setTotalFaculty(long totalFaculty) { this.totalFaculty = totalFaculty; }
    public long getPendingStudentApprovals() { return pendingStudentApprovals; }
    public void setPendingStudentApprovals(long pendingStudentApprovals) { this.pendingStudentApprovals = pendingStudentApprovals; }
    public long getPendingFacultyApprovals() { return pendingFacultyApprovals; }
    public void setPendingFacultyApprovals(long pendingFacultyApprovals) { this.pendingFacultyApprovals = pendingFacultyApprovals; }
    public long getTotalCourses() { return totalCourses; }
    public void setTotalCourses(long totalCourses) { this.totalCourses = totalCourses; }
    public long getTotalSubjects() { return totalSubjects; }
    public void setTotalSubjects(long totalSubjects) { this.totalSubjects = totalSubjects; }
    public long getTotalMaterials() { return totalMaterials; }
    public void setTotalMaterials(long totalMaterials) { this.totalMaterials = totalMaterials; }
    public long getTotalLectures() { return totalLectures; }
    public void setTotalLectures(long totalLectures) { this.totalLectures = totalLectures; }
    public long getTotalQuestions() { return totalQuestions; }
    public void setTotalQuestions(long totalQuestions) { this.totalQuestions = totalQuestions; }
    public long getResolvedQuestions() { return resolvedQuestions; }
    public void setResolvedQuestions(long resolvedQuestions) { this.resolvedQuestions = resolvedQuestions; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private long totalStudents;
        private long totalFaculty;
        private long pendingStudentApprovals;
        private long pendingFacultyApprovals;
        private long totalCourses;
        private long totalSubjects;
        private long totalMaterials;
        private long totalLectures;
        private long totalQuestions;
        private long resolvedQuestions;

        public Builder totalStudents(long totalStudents) {
            this.totalStudents = totalStudents;
            return this;
        }
        public Builder totalFaculty(long totalFaculty) {
            this.totalFaculty = totalFaculty;
            return this;
        }
        public Builder pendingStudentApprovals(long pendingStudentApprovals) {
            this.pendingStudentApprovals = pendingStudentApprovals;
            return this;
        }
        public Builder pendingFacultyApprovals(long pendingFacultyApprovals) {
            this.pendingFacultyApprovals = pendingFacultyApprovals;
            return this;
        }
        public Builder totalCourses(long totalCourses) {
            this.totalCourses = totalCourses;
            return this;
        }
        public Builder totalSubjects(long totalSubjects) {
            this.totalSubjects = totalSubjects;
            return this;
        }
        public Builder totalMaterials(long totalMaterials) {
            this.totalMaterials = totalMaterials;
            return this;
        }
        public Builder totalLectures(long totalLectures) {
            this.totalLectures = totalLectures;
            return this;
        }
        public Builder totalQuestions(long totalQuestions) {
            this.totalQuestions = totalQuestions;
            return this;
        }
        public Builder resolvedQuestions(long resolvedQuestions) {
            this.resolvedQuestions = resolvedQuestions;
            return this;
        }

        public PlatformMetricsDto build() {
            return new PlatformMetricsDto(totalStudents, totalFaculty, pendingStudentApprovals, pendingFacultyApprovals, totalCourses, totalSubjects, totalMaterials, totalLectures, totalQuestions, resolvedQuestions);
        }
    }
}
