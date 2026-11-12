import React, { useState, useEffect } from 'react';
import { facultyService } from '@/core/services/facultyService';
import { Subject, Examination, ExaminationResult, User, PageResponse, ExamType } from '@/types';
import { Award, Plus, Calendar, Clock, AlertCircle } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '@/shared/components/Card';
import { Table, Thead, Tbody, Tr, Th, Td } from '@/shared/components/Table';
import { Button } from '@/shared/components/Button';
import { Badge } from '@/shared/components/Badge';
import { LoadingSpinner } from '@/shared/components/LoadingSpinner';
import { Modal } from '@/shared/components/Modal';
import { Input } from '@/shared/components/Input';
import { Textarea } from '@/shared/components/Textarea';
import { Select } from '@/shared/components/Select';
import { Breadcrumbs } from '@/shared/components/Breadcrumbs';

export const FacultyExamsPage: React.FC = () => {
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [selectedSubjectId, setSelectedSubjectId] = useState<number | ''>('');
  const [exams, setExams] = useState<Examination[]>([]);
  const [loading, setLoading] = useState(true);

  // Create Modal
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [title, setTitle] = useState('');
  const [examType, setExamType] = useState<ExamType>('MIDTERM');
  const [totalMarks, setTotalMarks] = useState(100);
  const [durationMinutes, setDurationMinutes] = useState(120);
  const [scheduledAt, setScheduledAt] = useState('');
  const [instructions, setInstructions] = useState('');
  const [createLoading, setCreateLoading] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);

  // Grades Modal
  const [gradesModalOpen, setGradesModalOpen] = useState(false);
  const [selectedExam, setSelectedExam] = useState<Examination | null>(null);
  const [students, setStudents] = useState<User[]>([]);
  const [examResults, setExamResults] = useState<ExaminationResult[]>([]);
  const [gradesLoading, setGradesLoading] = useState(false);

  // Enter Grade Submodal
  const [gradeModalOpen, setGradeModalOpen] = useState(false);
  const [targetStudent, setTargetStudent] = useState<User | null>(null);
  const [marksObtained, setMarksObtained] = useState<number>(0);
  const [grade, setGrade] = useState('A');
  const [remarks, setRemarks] = useState('');
  const [gradeError, setGradeError] = useState<string | null>(null);
  const [gradeActionLoading, setGradeActionLoading] = useState(false);

  const calculateLetterGrade = (marks: number, total: number): string => {
    if (total <= 0) return 'Pass';
    const pct = (marks / total) * 100;
    if (pct >= 90) return 'A+';
    if (pct >= 80) return 'A';
    if (pct >= 70) return 'B+';
    if (pct >= 60) return 'B';
    if (pct >= 50) return 'C';
    if (pct >= 40) return 'D';
    return 'F';
  };

  useEffect(() => {
    const fetchInit = async () => {
      try {
        const subs = await facultyService.getMySubjects();
        setSubjects(subs || []);
        if (subs && subs.length > 0) {
          setSelectedSubjectId(subs[0].id);
        }
      } catch (err) {
        console.error('Failed to load subjects', err);
      }
    };
    fetchInit();
  }, []);

  const fetchExams = async () => {
    if (!selectedSubjectId) return;
    setLoading(true);
    try {
      const pageData: PageResponse<Examination> = await facultyService.getExaminationsForSubject(Number(selectedSubjectId));
      setExams(pageData?.content || []);
    } catch (err) {
      console.error('Failed to load exams', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (selectedSubjectId) {
      fetchExams();
    }
  }, [selectedSubjectId]);

  const handleCreateExam = async (e: React.FormEvent) => {
    e.preventDefault();
    setCreateError(null);

    if (!selectedSubjectId) {
      setCreateError('Please select a subject.');
      return;
    }

    if (totalMarks <= 0) {
      setCreateError('Total marks must be greater than zero.');
      return;
    }

    setCreateLoading(true);
    try {
      await facultyService.createExamination({
        subjectId: Number(selectedSubjectId),
        title,
        examType,
        totalMarks: Number(totalMarks),
        durationMinutes: Number(durationMinutes),
        scheduledAt: new Date(scheduledAt).toISOString(),
        instructions,
      });
      setCreateModalOpen(false);
      setTitle('');
      setInstructions('');
      setScheduledAt('');
      await fetchExams();
    } catch (err: any) {
      setCreateError(err?.message || 'Failed to schedule examination.');
    } finally {
      setCreateLoading(false);
    }
  };

  const handleOpenGrades = async (exam: Examination) => {
    setSelectedExam(exam);
    setGradesModalOpen(true);
    setGradesLoading(true);
    try {
      const [roster, results] = await Promise.all([
        facultyService.getEnrolledStudents(exam.subjectId),
        facultyService.getExamResults(exam.id),
      ]);
      setStudents(roster || []);
      setExamResults(results || []);
    } catch (err) {
      console.error('Failed to load roster or results', err);
    } finally {
      setGradesLoading(false);
    }
  };

  const handleOpenEnterGrade = (student: User) => {
    const existing = examResults.find((r) => r.studentId === student.id);
    setTargetStudent(student);
    const initialMarks = existing ? existing.marksObtained : 0;
    setMarksObtained(initialMarks);
    setGrade(existing?.grade || (selectedExam ? calculateLetterGrade(initialMarks, selectedExam.totalMarks) : 'A'));
    setRemarks(existing?.remarks || '');
    setGradeError(null);
    setGradeModalOpen(true);
  };

  const handleSaveGrade = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedExam || !targetStudent) return;

    if (marksObtained < 0 || marksObtained > selectedExam.totalMarks) {
      setGradeError(`Marks obtained must be between 0 and ${selectedExam.totalMarks}.`);
      return;
    }

    if (!grade.trim()) {
      setGradeError('Grade designation is required (e.g. A, B+, Pass).');
      return;
    }

    setGradeActionLoading(true);
    try {
      await facultyService.gradeExamResult({
        examinationId: selectedExam.id,
        studentId: targetStudent.id,
        marksObtained: Number(marksObtained),
        grade: grade.trim(),
        remarks,
      });
      setGradeModalOpen(false);
      // Refresh results
      const refreshed = await facultyService.getExamResults(selectedExam.id);
      setExamResults(refreshed || []);
    } catch (err: any) {
      setGradeError(err?.message || 'Failed to record official grade.');
    } finally {
      setGradeActionLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      <Breadcrumbs
        items={[
          { label: 'Faculty Portal', href: '/faculty' },
          { label: 'Examinations & Marks' },
        ]}
      />

      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-white">
            Examinations & Official Marks
          </h1>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
            Schedule midterm, final or quiz assessments and record verified official student grades
          </p>
        </div>

        <Button
          variant="primary"
          size="sm"
          onClick={() => setCreateModalOpen(true)}
          disabled={subjects.length === 0}
        >
          <Plus className="w-4 h-4 mr-1.5" /> Schedule Examination
        </Button>
      </div>

      {/* Subject Filter Card */}
      <Card>
        <CardContent className="p-4 flex flex-col sm:flex-row items-center gap-4">
          <label className="text-xs font-semibold text-slate-500 uppercase tracking-wider flex-shrink-0">
            Teaching Subject:
          </label>
          <Select
            options={subjects.map((s) => ({
              value: s.id,
              label: `${s.code} - ${s.title}`,
            }))}
            value={selectedSubjectId}
            onChange={(e) => setSelectedSubjectId(Number(e.target.value))}
            className="sm:w-80"
          />
        </CardContent>
      </Card>

      {/* Examinations Table */}
      <Card>
        <CardHeader className="py-4 flex justify-between items-center">
          <CardTitle className="text-base">Scheduled Examinations ({exams.length})</CardTitle>
        </CardHeader>
        <CardContent className="p-0">
          {loading ? (
            <div className="p-12 flex justify-center">
              <LoadingSpinner size="md" label="Loading examination assessments..." />
            </div>
          ) : exams.length === 0 ? (
            <div className="p-12 text-center text-xs text-slate-400">
              No examinations scheduled for this subject yet.
            </div>
          ) : (
            <Table>
              <Thead>
                <Tr>
                  <Th>Assessment</Th>
                  <Th>Type</Th>
                  <Th>Scheduled Date</Th>
                  <Th>Duration</Th>
                  <Th>Total Marks</Th>
                  <Th className="text-right">Actions</Th>
                </Tr>
              </Thead>
              <Tbody>
                {exams.map((exam) => (
                  <Tr key={exam.id}>
                    <Td>
                      <div className="font-semibold text-slate-900 dark:text-white">{exam.title}</div>
                      <div className="text-xs text-slate-500 line-clamp-1">{exam.instructions}</div>
                    </Td>
                    <Td>
                      <Badge variant="primary" size="sm">
                        {exam.examType}
                      </Badge>
                    </Td>
                    <Td>
                      <span className="text-xs text-slate-600 dark:text-slate-300 flex items-center gap-1.5">
                        <Calendar className="w-3.5 h-3.5 text-slate-400" />
                        {new Date(exam.scheduledAt).toLocaleString()}
                      </span>
                    </Td>
                    <Td>
                      <span className="text-xs text-slate-500 flex items-center gap-1">
                        <Clock className="w-3.5 h-3.5" />
                        {exam.durationMinutes} mins
                      </span>
                    </Td>
                    <Td>
                      <Badge variant="neutral" size="sm">
                        {exam.totalMarks} Marks
                      </Badge>
                    </Td>
                    <Td className="text-right">
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => handleOpenGrades(exam)}
                        className="text-indigo-600 border-indigo-200 hover:bg-indigo-50"
                      >
                        <Award className="w-3.5 h-3.5 mr-1" /> Enter / View Grades
                      </Button>
                    </Td>
                  </Tr>
                ))}
              </Tbody>
            </Table>
          )}
        </CardContent>
      </Card>

      {/* Schedule Exam Modal */}
      <Modal
        isOpen={createModalOpen}
        onClose={() => setCreateModalOpen(false)}
        title="Schedule Academic Examination"
        maxWidth="lg"
      >
        <form onSubmit={handleCreateExam} className="p-6 space-y-4">
          {createError && (
            <div className="p-3 bg-rose-50 text-rose-700 text-xs rounded-lg">
              {createError}
            </div>
          )}

          <Input
            label="Examination Title"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="e.g. Midterm Examination Fall 2026"
            required
          />

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
            <Select
              label="Assessment Type"
              options={[
                { value: 'MIDTERM', label: 'Midterm Examination' },
                { value: 'FINAL', label: 'Final Examination' },
                { value: 'QUIZ', label: 'Periodic Quiz' },
                { value: 'ASSIGNMENT_ASSESSMENT', label: 'Assignment Assessment' },
              ]}
              value={examType}
              onChange={(e) => setExamType(e.target.value as ExamType)}
            />
            <Input
              label="Total Marks"
              type="number"
              value={totalMarks}
              onChange={(e) => setTotalMarks(Number(e.target.value))}
              min={1}
              required
            />
            <Input
              label="Duration (Minutes)"
              type="number"
              value={durationMinutes}
              onChange={(e) => setDurationMinutes(Number(e.target.value))}
              min={15}
              required
            />
          </div>

          <Input
            label="Scheduled Date & Time"
            type="datetime-local"
            value={scheduledAt}
            onChange={(e) => setScheduledAt(e.target.value)}
            required
          />

          <Textarea
            label="Examination Instructions"
            value={instructions}
            onChange={(e) => setInstructions(e.target.value)}
            placeholder="Authorized materials, calculator rules, venue details..."
            rows={3}
          />

          <div className="flex justify-end gap-2 pt-2">
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => setCreateModalOpen(false)}
              disabled={createLoading}
            >
              Cancel
            </Button>
            <Button type="submit" variant="primary" size="sm" isLoading={createLoading}>
              Schedule Examination
            </Button>
          </div>
        </form>
      </Modal>

      {/* Grades Management Modal */}
      <Modal
        isOpen={gradesModalOpen}
        onClose={() => setGradesModalOpen(false)}
        title={`Student Marks — ${selectedExam?.title || ''}`}
        maxWidth="2xl"
      >
        <div className="p-6 space-y-4">
          <div className="flex items-center justify-between pb-2 border-b border-slate-100 dark:border-slate-800">
            <div>
              <span className="text-xs text-slate-500">Total Marks: {selectedExam?.totalMarks}</span>
            </div>
            <Badge variant="neutral" size="sm">
              {students.length} Enrolled Candidates
            </Badge>
          </div>

          {gradesLoading ? (
            <div className="p-8 flex justify-center">
              <LoadingSpinner size="md" label="Loading enrolled candidates and results..." />
            </div>
          ) : students.length === 0 ? (
            <div className="p-8 text-center text-xs text-slate-400">
              No students enrolled in this subject.
            </div>
          ) : (
            <div className="max-h-96 overflow-y-auto">
              <Table>
                <Thead>
                  <Tr>
                    <Th>Roll Number</Th>
                    <Th>Candidate</Th>
                    <Th>Marks Obtained</Th>
                    <Th>Letter Grade</Th>
                    <Th className="text-right">Action</Th>
                  </Tr>
                </Thead>
                <Tbody>
                  {students.map((student) => {
                    const result = examResults.find((r) => r.studentId === student.id);
                    return (
                      <Tr key={student.id}>
                        <Td className="font-mono text-xs">{student.rollNumber || 'N/A'}</Td>
                        <Td className="font-medium text-slate-900 dark:text-white">{student.fullName}</Td>
                        <Td>
                          {result ? (
                            <span className="font-semibold text-slate-900 dark:text-white">
                              {result.marksObtained} / {selectedExam?.totalMarks}
                            </span>
                          ) : (
                            <span className="text-xs text-slate-400">Not Graded</span>
                          )}
                        </Td>
                        <Td>
                          {result ? (
                            <Badge variant={result.grade === 'F' ? 'danger' : 'success'} size="sm">
                              {result.grade}
                            </Badge>
                          ) : (
                            <span className="text-xs text-slate-400">—</span>
                          )}
                        </Td>
                        <Td className="text-right">
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => handleOpenEnterGrade(student)}
                            className="text-indigo-600"
                          >
                            {result ? 'Update Grade' : 'Record Grade'}
                          </Button>
                        </Td>
                      </Tr>
                    );
                  })}
                </Tbody>
              </Table>
            </div>
          )}

          <div className="flex justify-end pt-2">
            <Button variant="outline" size="sm" onClick={() => setGradesModalOpen(false)}>
              Close
            </Button>
          </div>
        </div>
      </Modal>

      {/* Enter Individual Grade Submodal */}
      <Modal
        isOpen={gradeModalOpen}
        onClose={() => setGradeModalOpen(false)}
        title="Record Official Examination Grade"
        maxWidth="md"
      >
        <form onSubmit={handleSaveGrade} className="p-6 space-y-4">
          <div className="p-3 bg-slate-50 dark:bg-slate-800 rounded-xl space-y-1">
            <p className="text-xs text-slate-500">Student Candidate</p>
            <p className="text-sm font-bold text-slate-900 dark:text-white">
              {targetStudent?.fullName} ({targetStudent?.rollNumber})
            </p>
            <span className="text-xs text-slate-500">Maximum Marks: {selectedExam?.totalMarks}</span>
          </div>

          {gradeError && (
            <div className="p-3 bg-rose-50 text-rose-700 text-xs rounded-lg flex items-center gap-2">
              <AlertCircle className="w-4 h-4 flex-shrink-0" />
              <span>{gradeError}</span>
            </div>
          )}

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <Input
              label={`Marks Obtained (0 to ${selectedExam?.totalMarks})`}
              type="number"
              min={0}
              max={selectedExam?.totalMarks ?? 100}
              step="0.5"
              value={marksObtained}
              onChange={(e) => {
                const val = Number(e.target.value);
                setMarksObtained(val);
                if (selectedExam) {
                  setGrade(calculateLetterGrade(val, selectedExam.totalMarks));
                }
              }}
              required
            />

            <div>
              <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 uppercase tracking-wider mb-1.5">
                Grade Letter / Mark
              </label>
              <Input
                value={grade}
                onChange={(e) => setGrade(e.target.value)}
                placeholder="e.g. A+, A, B, Pass"
                required
              />
            </div>
          </div>

          <Input
            label="Evaluation Remarks / Annotations"
            value={remarks}
            onChange={(e) => setRemarks(e.target.value)}
            placeholder="e.g. Excellent performance in section B."
          />

          <div className="flex justify-end gap-2 pt-2">
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => setGradeModalOpen(false)}
              disabled={gradeActionLoading}
            >
              Cancel
            </Button>
            <Button type="submit" variant="primary" size="sm" isLoading={gradeActionLoading}>
              Save Grade
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
