import React, { useState, useEffect } from 'react';
import { facultyService } from '@/core/services/facultyService';
import { Subject, Assignment, AssignmentSubmission, PageResponse } from '@/types';
import { Plus, ExternalLink, Users, Calendar, AlertCircle } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '@/shared/components/Card';
import { Table, Thead, Tbody, Tr, Th, Td } from '@/shared/components/Table';
import { Button } from '@/shared/components/Button';
import { Badge, StatusBadge } from '@/shared/components/Badge';
import { LoadingSpinner } from '@/shared/components/LoadingSpinner';
import { Modal } from '@/shared/components/Modal';
import { Input } from '@/shared/components/Input';
import { Textarea } from '@/shared/components/Textarea';
import { Select } from '@/shared/components/Select';
import { Breadcrumbs } from '@/shared/components/Breadcrumbs';

export const FacultyAssignmentsPage: React.FC = () => {
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [selectedSubjectId, setSelectedSubjectId] = useState<number | ''>('');
  const [assignments, setAssignments] = useState<Assignment[]>([]);
  const [loading, setLoading] = useState(true);

  // Create Assignment Modal
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [dueDate, setDueDate] = useState('');
  const [maxPoints, setMaxPoints] = useState(100);
  const [attachmentUrl, setAttachmentUrl] = useState('');
  const [createLoading, setCreateLoading] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);

  // Submissions Modal
  const [submissionsModalOpen, setSubmissionsModalOpen] = useState(false);
  const [selectedAssignment, setSelectedAssignment] = useState<Assignment | null>(null);
  const [submissions, setSubmissions] = useState<AssignmentSubmission[]>([]);
  const [submissionsLoading, setSubmissionsLoading] = useState(false);

  // Grading Modal
  const [gradeModalOpen, setGradeModalOpen] = useState(false);
  const [selectedSubmission, setSelectedSubmission] = useState<AssignmentSubmission | null>(null);
  const [gradePoints, setGradePoints] = useState<number>(0);
  const [feedback, setFeedback] = useState('');
  const [gradeLoading, setGradeLoading] = useState(false);
  const [gradeError, setGradeError] = useState<string | null>(null);

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

  const fetchAssignments = async () => {
    if (!selectedSubjectId) return;
    setLoading(true);
    try {
      const pageData: PageResponse<Assignment> = await facultyService.getAssignmentsForSubject(Number(selectedSubjectId));
      setAssignments(pageData?.content || []);
    } catch (err) {
      console.error('Failed to load assignments', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (selectedSubjectId) {
      fetchAssignments();
    }
  }, [selectedSubjectId]);

  const handleCreateAssignment = async (e: React.FormEvent) => {
    e.preventDefault();
    setCreateError(null);

    if (!selectedSubjectId) {
      setCreateError('Please select an assigned subject.');
      return;
    }

    if (maxPoints <= 0) {
      setCreateError('Maximum points must be greater than 0.');
      return;
    }

    setCreateLoading(true);
    try {
      await facultyService.createAssignment({
        subjectId: Number(selectedSubjectId),
        title,
        description,
        dueDate: new Date(dueDate).toISOString(),
        maxPoints: Number(maxPoints),
        attachmentUrl: attachmentUrl || undefined,
      });
      setCreateModalOpen(false);
      setTitle('');
      setDescription('');
      setDueDate('');
      setMaxPoints(100);
      setAttachmentUrl('');
      await fetchAssignments();
    } catch (err: any) {
      setCreateError(err?.message || 'Failed to publish assignment.');
    } finally {
      setCreateLoading(false);
    }
  };

  const handleOpenSubmissions = async (assignment: Assignment) => {
    setSelectedAssignment(assignment);
    setSubmissionsModalOpen(true);
    setSubmissionsLoading(true);
    try {
      const data = await facultyService.getSubmissions(assignment.id);
      setSubmissions(data || []);
    } catch (err) {
      console.error('Failed to load submissions', err);
    } finally {
      setSubmissionsLoading(false);
    }
  };

  const handleOpenGrade = (sub: AssignmentSubmission) => {
    setSelectedSubmission(sub);
    setGradePoints(sub.gradePoints ?? 0);
    setFeedback(sub.feedback || '');
    setGradeError(null);
    setGradeModalOpen(true);
  };

  const handleConfirmGrade = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedSubmission || !selectedAssignment) return;

    if (gradePoints < 0 || gradePoints > selectedAssignment.maxPoints) {
      setGradeError(`Grade points must be between 0 and ${selectedAssignment.maxPoints}.`);
      return;
    }

    setGradeLoading(true);
    setGradeError(null);
    try {
      await facultyService.gradeSubmission(selectedSubmission.id, {
        gradePoints: Number(gradePoints),
        feedback,
      });
      setGradeModalOpen(false);
      // Refresh submissions
      const data = await facultyService.getSubmissions(selectedAssignment.id);
      setSubmissions(data || []);
    } catch (err: any) {
      setGradeError(err?.message || 'Failed to save submission grade.');
    } finally {
      setGradeLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      <Breadcrumbs
        items={[
          { label: 'Faculty Portal', href: '/faculty' },
          { label: 'Assignments & Grading' },
        ]}
      />

      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-white">
            Assignments & Coursework Grading
          </h1>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
            Author assignments, inspect student solutions, and enter official grade points
          </p>
        </div>

        <Button
          variant="primary"
          size="sm"
          onClick={() => setCreateModalOpen(true)}
          disabled={subjects.length === 0}
        >
          <Plus className="w-4 h-4 mr-1.5" /> Create Assignment
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

      {/* Assignments Table */}
      <Card>
        <CardHeader className="py-4 flex justify-between items-center">
          <CardTitle className="text-base">Published Assignments ({assignments.length})</CardTitle>
        </CardHeader>
        <CardContent className="p-0">
          {loading ? (
            <div className="p-12 flex justify-center">
              <LoadingSpinner size="md" label="Loading coursework assignments..." />
            </div>
          ) : assignments.length === 0 ? (
            <div className="p-12 text-center text-xs text-slate-400">
              No assignments published for this subject yet.
            </div>
          ) : (
            <Table>
              <Thead>
                <Tr>
                  <Th>Title</Th>
                  <Th>Due Date</Th>
                  <Th>Max Points</Th>
                  <Th>Created</Th>
                  <Th className="text-right">Actions</Th>
                </Tr>
              </Thead>
              <Tbody>
                {assignments.map((asg) => (
                  <Tr key={asg.id}>
                    <Td>
                      <div className="font-semibold text-slate-900 dark:text-white">{asg.title}</div>
                      <div className="text-xs text-slate-500 line-clamp-1">{asg.description}</div>
                    </Td>
                    <Td>
                      <span className="text-xs text-slate-600 dark:text-slate-300 flex items-center gap-1.5">
                        <Calendar className="w-3.5 h-3.5 text-slate-400" />
                        {new Date(asg.dueDate).toLocaleDateString()}
                      </span>
                    </Td>
                    <Td>
                      <Badge variant="neutral" size="sm">
                        {asg.maxPoints} pts
                      </Badge>
                    </Td>
                    <Td>
                      <span className="text-xs text-slate-400">
                        {new Date(asg.createdAt).toLocaleDateString()}
                      </span>
                    </Td>
                    <Td className="text-right">
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => handleOpenSubmissions(asg)}
                        className="text-indigo-600 border-indigo-200 hover:bg-indigo-50"
                      >
                        <Users className="w-3.5 h-3.5 mr-1" /> View Submissions
                      </Button>
                    </Td>
                  </Tr>
                ))}
              </Tbody>
            </Table>
          )}
        </CardContent>
      </Card>

      {/* Create Assignment Modal */}
      <Modal
        isOpen={createModalOpen}
        onClose={() => setCreateModalOpen(false)}
        title="Publish Coursework Assignment"
        maxWidth="lg"
      >
        <form onSubmit={handleCreateAssignment} className="p-6 space-y-4">
          {createError && (
            <div className="p-3 bg-rose-50 dark:bg-rose-950/30 border border-rose-200 text-xs text-rose-700 rounded-lg">
              {createError}
            </div>
          )}

          <Input
            label="Assignment Title"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="e.g. Laboratory Analysis Report 1"
            required
          />

          <Textarea
            label="Detailed Prompt / Instructions"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Describe deliverables, submission formats, and academic rubrics..."
            rows={4}
          />

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Input
              label="Submission Due Date & Time"
              type="datetime-local"
              value={dueDate}
              onChange={(e) => setDueDate(e.target.value)}
              required
            />
            <Input
              label="Maximum Score Points"
              type="number"
              value={maxPoints}
              onChange={(e) => setMaxPoints(Number(e.target.value))}
              min={1}
              required
            />
          </div>

          <Input
            label="Resource Attachment Link (Optional)"
            type="url"
            value={attachmentUrl}
            onChange={(e) => setAttachmentUrl(e.target.value)}
            placeholder="https://..."
            helperText="Link to problem sheet, datasets or syllabus document."
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
              Publish Assignment
            </Button>
          </div>
        </form>
      </Modal>

      {/* Submissions List Modal */}
      <Modal
        isOpen={submissionsModalOpen}
        onClose={() => setSubmissionsModalOpen(false)}
        title={`Submissions — ${selectedAssignment?.title || ''}`}
        maxWidth="2xl"
      >
        <div className="p-6 space-y-4">
          <div className="flex items-center justify-between pb-2 border-b border-slate-100 dark:border-slate-800">
            <div>
              <span className="text-xs text-slate-500">Max Points: {selectedAssignment?.maxPoints} pts</span>
            </div>
            <Badge variant="neutral" size="sm">
              {submissions.length} Total Submissions
            </Badge>
          </div>

          {submissionsLoading ? (
            <div className="p-8 flex justify-center">
              <LoadingSpinner size="md" label="Loading student submissions..." />
            </div>
          ) : submissions.length === 0 ? (
            <div className="p-8 text-center text-xs text-slate-400">
              No submissions recorded yet for this assignment.
            </div>
          ) : (
            <div className="max-h-96 overflow-y-auto">
              <Table>
                <Thead>
                  <Tr>
                    <Th>Student</Th>
                    <Th>Submitted At</Th>
                    <Th>Status</Th>
                    <Th>Grade</Th>
                    <Th className="text-right">Action</Th>
                  </Tr>
                </Thead>
                <Tbody>
                  {submissions.map((sub) => (
                    <Tr key={sub.id}>
                      <Td>
                        <div className="font-semibold text-slate-900 dark:text-white">{sub.studentName}</div>
                        <div className="text-xs text-slate-500">{sub.studentRollNumber || 'Student'}</div>
                      </Td>
                      <Td className="text-xs text-slate-500">
                        {new Date(sub.submittedAt).toLocaleDateString()}
                      </Td>
                      <Td>
                        <StatusBadge status={sub.status} size="sm" />
                      </Td>
                      <Td>
                        {sub.gradePoints !== undefined && sub.gradePoints !== null ? (
                          <span className="font-semibold text-emerald-600">
                            {sub.gradePoints} / {selectedAssignment?.maxPoints}
                          </span>
                        ) : (
                          <span className="text-xs text-slate-400">Ungraded</span>
                        )}
                      </Td>
                      <Td className="text-right">
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => handleOpenGrade(sub)}
                          className="text-indigo-600"
                        >
                          Grade Solution
                        </Button>
                      </Td>
                    </Tr>
                  ))}
                </Tbody>
              </Table>
            </div>
          )}

          <div className="flex justify-end pt-2">
            <Button variant="outline" size="sm" onClick={() => setSubmissionsModalOpen(false)}>
              Close
            </Button>
          </div>
        </div>
      </Modal>

      {/* Grade Individual Submission Modal */}
      <Modal
        isOpen={gradeModalOpen}
        onClose={() => setGradeModalOpen(false)}
        title="Grade Student Solution"
        maxWidth="md"
      >
        <form onSubmit={handleConfirmGrade} className="p-6 space-y-4">
          <div className="p-3 bg-slate-50 dark:bg-slate-800 rounded-xl space-y-1">
            <p className="text-xs text-slate-500">Student Submission</p>
            <p className="text-sm font-bold text-slate-900 dark:text-white">
              {selectedSubmission?.studentName}
            </p>
            {selectedSubmission?.fileUrl && (
              <a
                href={selectedSubmission.fileUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="text-xs text-indigo-600 dark:text-indigo-400 hover:underline flex items-center gap-1 mt-1"
              >
                <ExternalLink className="w-3.5 h-3.5" /> View Submitted Document
              </a>
            )}
            {selectedSubmission?.submissionText && (
              <div className="mt-2 p-2 bg-white dark:bg-slate-900 rounded border border-slate-200 text-xs text-slate-700">
                {selectedSubmission.submissionText}
              </div>
            )}
          </div>

          {gradeError && (
            <div className="p-3 bg-rose-50 text-rose-700 text-xs rounded-lg flex items-center gap-2">
              <AlertCircle className="w-4 h-4 flex-shrink-0" />
              <span>{gradeError}</span>
            </div>
          )}

          <Input
            label={`Awarded Grade Points (Max: ${selectedAssignment?.maxPoints ?? 100})`}
            type="number"
            min={0}
            max={selectedAssignment?.maxPoints ?? 100}
            value={gradePoints}
            onChange={(e) => setGradePoints(Number(e.target.value))}
            required
          />

          <Textarea
            label="Qualitative Instructor Feedback"
            value={feedback}
            onChange={(e) => setFeedback(e.target.value)}
            placeholder="Constructive remarks and grading rationale..."
            rows={3}
          />

          <div className="flex justify-end gap-2 pt-2">
            <Button
              type="button"
              variant="outline"
              size="sm"
              onClick={() => setGradeModalOpen(false)}
              disabled={gradeLoading}
            >
              Cancel
            </Button>
            <Button type="submit" variant="primary" size="sm" isLoading={gradeLoading}>
              Save Grade
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
