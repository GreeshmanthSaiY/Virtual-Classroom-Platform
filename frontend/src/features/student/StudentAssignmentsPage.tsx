import React, { useState, useEffect } from 'react';
import { studentService } from '@/core/services/studentService';
import { storageService } from '@/core/services/storageService';
import { Subject, Assignment, AssignmentSubmission, PageResponse } from '@/types';
import { Calendar, CheckCircle2, Upload, AlertCircle } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '@/shared/components/Card';
import { Table, Thead, Tbody, Tr, Th, Td } from '@/shared/components/Table';
import { Button } from '@/shared/components/Button';
import { Badge, StatusBadge } from '@/shared/components/Badge';
import { LoadingSpinner } from '@/shared/components/LoadingSpinner';
import { Modal } from '@/shared/components/Modal';
import { Input } from '@/shared/components/Input';
import { Textarea } from '@/shared/components/Textarea';
import { Select } from '@/shared/components/Select';
import { Tabs } from '@/shared/components/Tabs';
import { Breadcrumbs } from '@/shared/components/Breadcrumbs';

export const StudentAssignmentsPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'assignments' | 'submissions'>('assignments');
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [selectedSubjectId, setSelectedSubjectId] = useState<number | ''>('');
  const [assignments, setAssignments] = useState<Assignment[]>([]);
  const [mySubmissions, setMySubmissions] = useState<AssignmentSubmission[]>([]);
  const [loading, setLoading] = useState(true);

  // Submit Modal
  const [submitModalOpen, setSubmitModalOpen] = useState(false);
  const [selectedAssignment, setSelectedAssignment] = useState<Assignment | null>(null);
  const [submissionText, setSubmissionText] = useState('');
  const [fileUrl, setFileUrl] = useState('');
  const [uploadingFile, setUploadingFile] = useState(false);
  const [submitLoading, setSubmitLoading] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [submitSuccess, setSubmitSuccess] = useState(false);

  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setUploadingFile(true);
    setSubmitError(null);
    try {
      const uploaded = await storageService.uploadFile(file, 'assignments');
      setFileUrl(uploaded.fileUrl);
    } catch (err: any) {
      setSubmitError(err?.message || 'File upload failed. You may manually provide a link instead.');
    } finally {
      setUploadingFile(false);
    }
  };

  useEffect(() => {
    const fetchInit = async () => {
      try {
        const subs = await studentService.getEnrolledSubjects();
        setSubjects(subs || []);
        if (subs && subs.length > 0) {
          setSelectedSubjectId(subs[0].id);
        }
      } catch (err) {
        console.error('Failed to load enrolled subjects', err);
      }
    };
    fetchInit();
  }, []);

  const fetchData = async () => {
    setLoading(true);
    try {
      if (selectedSubjectId) {
        const asgData: PageResponse<Assignment> = await studentService.getAssignmentsForSubject(Number(selectedSubjectId));
        setAssignments(asgData?.content || []);
      }
      const subs = await studentService.getMySubmissions();
      setMySubmissions(subs || []);
    } catch (err) {
      console.error('Failed to load student assignments/submissions', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (selectedSubjectId) {
      fetchData();
    }
  }, [selectedSubjectId]);

  const handleOpenSubmit = (asg: Assignment) => {
    setSelectedAssignment(asg);
    setSubmissionText('');
    setFileUrl('');
    setSubmitError(null);
    setSubmitSuccess(false);
    setSubmitModalOpen(true);
  };

  const handleSubmitSolution = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedAssignment) return;

    if (!submissionText.trim() && !fileUrl.trim()) {
      setSubmitError('Please enter a response or provide a document link.');
      return;
    }

    setSubmitLoading(true);
    setSubmitError(null);
    try {
      await studentService.submitAssignment(selectedAssignment.id, {
        submissionText: submissionText.trim() || undefined,
        fileUrl: fileUrl.trim() || undefined,
      });
      setSubmitSuccess(true);
      setTimeout(() => {
        setSubmitModalOpen(false);
        fetchData();
      }, 1500);
    } catch (err: any) {
      setSubmitError(err?.message || 'Failed to submit assignment response.');
    } finally {
      setSubmitLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      <Breadcrumbs
        items={[
          { label: 'Student Portal', href: '/student' },
          { label: 'Assignments & Work' },
        ]}
      />

      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-white">
            Assignments & Coursework
          </h1>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
            Submit coursework solutions, review academic rubrics, and view instructor grading
          </p>
        </div>
      </div>

      <Tabs
        tabs={[
          { id: 'assignments', label: 'Active Assignments', count: assignments.length },
          { id: 'submissions', label: 'My Submissions & Grades', count: mySubmissions.length },
        ]}
        activeTab={activeTab}
        onChange={(tabId) => setActiveTab(tabId as 'assignments' | 'submissions')}
      />

      {activeTab === 'assignments' && (
        <div className="space-y-4">
          {/* Subject Filter */}
          <Card>
            <CardContent className="p-4 flex flex-col sm:flex-row items-center gap-4">
              <label className="text-xs font-semibold text-slate-500 uppercase tracking-wider flex-shrink-0">
                Enrolled Subject:
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

          {/* Assignments List */}
          <Card>
            <CardHeader className="py-4">
              <CardTitle className="text-base">Coursework Deliverables</CardTitle>
            </CardHeader>
            <CardContent className="p-0">
              {loading ? (
                <div className="p-12 flex justify-center">
                  <LoadingSpinner size="md" label="Loading coursework..." />
                </div>
              ) : assignments.length === 0 ? (
                <div className="p-12 text-center text-xs text-slate-400">
                  No active assignments currently published for this subject.
                </div>
              ) : (
                <Table>
                  <Thead>
                    <Tr>
                      <Th>Assignment Title</Th>
                      <Th>Due Date</Th>
                      <Th>Max Score</Th>
                      <Th className="text-right">Action</Th>
                    </Tr>
                  </Thead>
                  <Tbody>
                    {assignments.map((asg) => {
                      const existingSub = mySubmissions.find((s) => s.assignmentId === asg.id);
                      return (
                        <Tr key={asg.id}>
                          <Td>
                            <div className="font-semibold text-slate-900 dark:text-white">{asg.title}</div>
                            <div className="text-xs text-slate-500 line-clamp-2 mt-0.5">{asg.description}</div>
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
                          <Td className="text-right">
                            {existingSub ? (
                              <Badge variant="success" size="sm">
                                Submitted ({existingSub.status})
                              </Badge>
                            ) : (
                              <Button
                                variant="primary"
                                size="sm"
                                onClick={() => handleOpenSubmit(asg)}
                                className="bg-emerald-600 hover:bg-emerald-700 text-white"
                              >
                                <Upload className="w-3.5 h-3.5 mr-1" /> Submit Work
                              </Button>
                            )}
                          </Td>
                        </Tr>
                      );
                    })}
                  </Tbody>
                </Table>
              )}
            </CardContent>
          </Card>
        </div>
      )}

      {activeTab === 'submissions' && (
        <Card>
          <CardHeader className="py-4">
            <CardTitle className="text-base">Submission History & Evaluation</CardTitle>
          </CardHeader>
          <CardContent className="p-0">
            {mySubmissions.length === 0 ? (
              <div className="p-12 text-center text-xs text-slate-400">
                You haven't submitted any assignments yet.
              </div>
            ) : (
              <Table>
                <Thead>
                  <Tr>
                    <Th>Assignment</Th>
                    <Th>Date Submitted</Th>
                    <Th>Status</Th>
                    <Th>Awarded Score</Th>
                    <Th>Instructor Feedback</Th>
                  </Tr>
                </Thead>
                <Tbody>
                  {mySubmissions.map((sub) => (
                    <Tr key={sub.id}>
                      <Td className="font-semibold text-slate-900 dark:text-white">
                        {sub.assignmentTitle}
                      </Td>
                      <Td className="text-xs text-slate-500">
                        {new Date(sub.submittedAt).toLocaleDateString()}
                      </Td>
                      <Td>
                        <StatusBadge status={sub.status} size="sm" />
                      </Td>
                      <Td>
                        {sub.gradePoints !== undefined && sub.gradePoints !== null ? (
                          <span className="font-bold text-emerald-600">
                            {sub.gradePoints} pts
                          </span>
                        ) : (
                          <span className="text-xs text-slate-400">Evaluation Pending</span>
                        )}
                      </Td>
                      <Td className="text-xs text-slate-600 dark:text-slate-300 max-w-xs truncate">
                        {sub.feedback || '—'}
                      </Td>
                    </Tr>
                  ))}
                </Tbody>
              </Table>
            )}
          </CardContent>
        </Card>
      )}

      {/* Submit Assignment Modal */}
      <Modal
        isOpen={submitModalOpen}
        onClose={() => setSubmitModalOpen(false)}
        title={`Submit Solution — ${selectedAssignment?.title || ''}`}
        maxWidth="lg"
      >
        <form onSubmit={handleSubmitSolution} className="p-6 space-y-4">
          {submitSuccess ? (
            <div className="p-4 rounded-xl bg-emerald-50 text-emerald-800 dark:bg-emerald-950/40 dark:text-emerald-200 text-xs flex items-center gap-2 font-medium">
              <CheckCircle2 className="w-5 h-5 text-emerald-600" />
              Your assignment solution has been submitted successfully!
            </div>
          ) : (
            <>
              <div className="p-3 bg-slate-50 dark:bg-slate-800 rounded-xl space-y-1 text-xs">
                <p className="font-semibold text-slate-900 dark:text-white">Assignment Prompt</p>
                <p className="text-slate-600 dark:text-slate-300">{selectedAssignment?.description}</p>
                <p className="text-slate-400 mt-1">Maximum Points: {selectedAssignment?.maxPoints} pts</p>
              </div>

              {submitError && (
                <div className="p-3 bg-rose-50 text-rose-700 text-xs rounded-lg flex items-center gap-2">
                  <AlertCircle className="w-4 h-4 flex-shrink-0" />
                  <span>{submitError}</span>
                </div>
              )}

              <Textarea
                label="Written Solution / Analysis"
                value={submissionText}
                onChange={(e) => setSubmissionText(e.target.value)}
                placeholder="Enter your comprehensive solution or notes here..."
                rows={5}
              />

              <div className="space-y-2">
                <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 uppercase tracking-wider">
                  Upload Assignment Document (PDF / DOCX / ZIP)
                </label>
                <div className="flex items-center gap-3">
                  <input
                    type="file"
                    id="assignment-file-input"
                    onChange={handleFileUpload}
                    disabled={uploadingFile || submitLoading}
                    className="text-xs text-slate-600 dark:text-slate-400 file:mr-3 file:py-1.5 file:px-3 file:rounded-lg file:border-0 file:text-xs file:font-semibold file:bg-primary-50 file:text-primary-700 hover:file:bg-primary-100 dark:file:bg-slate-800 dark:file:text-primary-400"
                  />
                  {uploadingFile && <LoadingSpinner size="sm" label="Uploading..." />}
                </div>
              </div>

              <Input
                label="Or Direct Solution Attachment URL"
                type="url"
                value={fileUrl}
                onChange={(e) => setFileUrl(e.target.value)}
                placeholder="https://..."
                helperText="Auto-filled on upload or link to Google Drive, GitHub repository, or Cloud Storage file."
              />

              <div className="flex justify-end gap-2 pt-2">
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => setSubmitModalOpen(false)}
                  disabled={submitLoading}
                >
                  Cancel
                </Button>
                <Button
                  type="submit"
                  variant="primary"
                  size="sm"
                  isLoading={submitLoading}
                  className="bg-emerald-600 hover:bg-emerald-700 text-white"
                >
                  Submit Solution
                </Button>
              </div>
            </>
          )}
        </form>
      </Modal>
    </div>
  );
};
