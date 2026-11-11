package com.virtualclassroom.service;

import com.virtualclassroom.dto.*;
import com.virtualclassroom.model.*;
import com.virtualclassroom.repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FacultyService {

    private final FacultySubjectAssignmentRepository assignmentRepository;
    private final SubjectRepository subjectRepository;
    private final StudyMaterialRepository studyMaterialRepository;
    private final VideoLectureRepository videoLectureRepository;
    private final CourseService courseService;
    private final AuditLogService auditLogService;

    public FacultyService(FacultySubjectAssignmentRepository assignmentRepository, SubjectRepository subjectRepository, StudyMaterialRepository studyMaterialRepository, VideoLectureRepository videoLectureRepository, CourseService courseService, AuditLogService auditLogService) {
        this.assignmentRepository = assignmentRepository;
        this.subjectRepository = subjectRepository;
        this.studyMaterialRepository = studyMaterialRepository;
        this.videoLectureRepository = videoLectureRepository;
        this.courseService = courseService;
        this.auditLogService = auditLogService;
    }


    @Transactional(readOnly = true)
    public List<SubjectDto> getAssignedSubjects(Long facultyUserId) {
        List<FacultySubjectAssignment> assignments = assignmentRepository.findByFacultyId(facultyUserId);
        return assignments.stream()
                .map(a -> courseService.toSubjectDto(a.getSubject()))
                .collect(Collectors.toList());
    }

    private void verifyFacultyAssignment(User facultyUser, Integer subjectId) {
        if (facultyUser.getRole() == Role.ROLE_ADMIN) {
            return;
        }
        boolean isAssigned = assignmentRepository.existsByFacultyIdAndSubjectId(facultyUser.getId(), subjectId);
        if (!isAssigned) {
            throw new AccessDeniedException("Access denied: You are not assigned to instruct this subject");
        }
    }

    @Transactional(readOnly = true)
    public List<StudyMaterialDto> getMaterialsByFaculty(Long facultyId, Integer subjectId) {
        List<StudyMaterial> materials = (subjectId != null)
                ? studyMaterialRepository.findByUploaderIdAndSubjectId(facultyId, subjectId)
                : studyMaterialRepository.findByUploaderId(facultyId);
        return materials.stream().map(this::toMaterialDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<VideoLectureDto> getLecturesByFaculty(Long facultyId, Integer subjectId) {
        List<VideoLecture> lectures = (subjectId != null)
                ? videoLectureRepository.findByFacultyIdAndSubjectId(facultyId, subjectId)
                : videoLectureRepository.findByFacultyId(facultyId);
        return lectures.stream().map(this::toLectureDto).collect(Collectors.toList());
    }

    @Transactional
    public StudyMaterialDto uploadStudyMaterial(CreateMaterialRequest request, User facultyUser, String ipAddress) {
        verifyFacultyAssignment(facultyUser, request.getSubjectId());

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new IllegalArgumentException("Subject not found"));

        StudyMaterial material = StudyMaterial.builder()
                .subject(subject)
                .uploader(facultyUser)
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .fileUrl(request.getFileUrl().trim())
                .fileSizeBytes(request.getFileSizeBytes() != null ? request.getFileSizeBytes() : 0L)
                .mimeType(request.getMimeType() != null ? request.getMimeType() : "application/pdf")
                .build();

        StudyMaterial saved = studyMaterialRepository.save(material);

        auditLogService.logAction(facultyUser, "UPLOAD_STUDY_MATERIAL", "study_materials", saved.getId().toString(),
                "Uploaded material: " + saved.getTitle(), ipAddress);

        return toMaterialDto(saved);
    }

    @Transactional
    public VideoLectureDto createVideoLecture(CreateLectureRequest request, User facultyUser, String ipAddress) {
        verifyFacultyAssignment(facultyUser, request.getSubjectId());

        Subject subject = subjectRepository.findById(request.getSubjectId())
                .orElseThrow(() -> new IllegalArgumentException("Subject not found"));

        VideoLecture lecture = VideoLecture.builder()
                .subject(subject)
                .faculty(facultyUser)
                .title(request.getTitle().trim())
                .description(request.getDescription())
                .videoUrl(request.getVideoUrl().trim())
                .durationSeconds(request.getDurationSeconds() != null ? request.getDurationSeconds() : 0)
                .build();

        VideoLecture saved = videoLectureRepository.save(lecture);

        auditLogService.logAction(facultyUser, "CREATE_VIDEO_LECTURE", "video_lectures", saved.getId().toString(),
                "Published video lecture: " + saved.getTitle(), ipAddress);

        return toLectureDto(saved);
    }

    private StudyMaterialDto toMaterialDto(StudyMaterial m) {
        return StudyMaterialDto.builder()
                .id(m.getId())
                .subjectId(m.getSubject().getId())
                .subjectTitle(m.getSubject().getTitle())
                .uploaderId(m.getUploader().getId())
                .uploaderName(m.getUploader().getFullName())
                .title(m.getTitle())
                .description(m.getDescription())
                .fileUrl(m.getFileUrl())
                .fileSizeBytes(m.getFileSizeBytes())
                .mimeType(m.getMimeType())
                .createdAt(m.getCreatedAt())
                .build();
    }

    private VideoLectureDto toLectureDto(VideoLecture l) {
        return VideoLectureDto.builder()
                .id(l.getId())
                .subjectId(l.getSubject().getId())
                .subjectTitle(l.getSubject().getTitle())
                .facultyId(l.getFaculty().getId())
                .facultyName(l.getFaculty().getFullName())
                .title(l.getTitle())
                .description(l.getDescription())
                .videoUrl(l.getVideoUrl())
                .durationSeconds(l.getDurationSeconds())
                .createdAt(l.getCreatedAt())
                .build();
    }

    @Transactional
    public void deleteMaterial(Long materialId, User facultyUser) {
        StudyMaterial material = studyMaterialRepository.findById(materialId)
                .orElseThrow(() -> new IllegalArgumentException("Material not found"));

        if (!material.getUploader().getId().equals(facultyUser.getId()) && facultyUser.getRole() != Role.ROLE_ADMIN) {
            throw new AccessDeniedException("You are not authorized to delete this study material");
        }

        studyMaterialRepository.delete(material);
    }

    @Transactional
    public void deleteLecture(Long lectureId, User facultyUser) {
        VideoLecture lecture = videoLectureRepository.findById(lectureId)
                .orElseThrow(() -> new IllegalArgumentException("Lecture not found"));

        if (!lecture.getFaculty().getId().equals(facultyUser.getId()) && facultyUser.getRole() != Role.ROLE_ADMIN) {
            throw new AccessDeniedException("You are not authorized to delete this video lecture");
        }

        videoLectureRepository.delete(lecture);
    }
}
