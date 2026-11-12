import React, { useState, useEffect } from 'react';
import { studentService } from '@/core/services/studentService';
import { Subject, Examination, ExaminationResult, PageResponse } from '@/types';
import { Calendar, Clock } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '@/shared/components/Card';
import { Table, Thead, Tbody, Tr, Th, Td } from '@/shared/components/Table';
import { Badge } from '@/shared/components/Badge';
import { LoadingSpinner } from '@/shared/components/LoadingSpinner';
import { Select } from '@/shared/components/Select';
import { Tabs } from '@/shared/components/Tabs';
import { Breadcrumbs } from '@/shared/components/Breadcrumbs';

export const StudentExamsPage: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'upcoming' | 'results'>('upcoming');
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [selectedSubjectId, setSelectedSubjectId] = useState<number | ''>('');
  const [exams, setExams] = useState<Examination[]>([]);
  const [results, setResults] = useState<ExaminationResult[]>([]);
  const [loading, setLoading] = useState(true);

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
        const examData: PageResponse<Examination> = await studentService.getExaminationsForSubject(Number(selectedSubjectId));
        setExams(examData?.content || []);
      }
      const myRes = await studentService.getMyResults();
      setResults(myRes || []);
    } catch (err) {
      console.error('Failed to load examinations and results', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (selectedSubjectId) {
      fetchData();
    }
  }, [selectedSubjectId]);

  return (
    <div className="space-y-6">
      <Breadcrumbs
        items={[
          { label: 'Student Portal', href: '/student' },
          { label: 'Examinations & Grades' },
        ]}
      />

      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-white">
            Examinations & Official Grades
          </h1>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
            Official examination schedules, invigilation guidelines, and permanent grade reports
          </p>
        </div>
      </div>

      <Tabs
        tabs={[
          { id: 'upcoming', label: 'Examination Schedule', count: exams.length },
          { id: 'results', label: 'Official Grade Report', count: results.length },
        ]}
        activeTab={activeTab}
        onChange={(tabId) => setActiveTab(tabId as 'upcoming' | 'results')}
      />

      {activeTab === 'upcoming' && (
        <div className="space-y-4">
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

          <Card>
            <CardHeader className="py-4">
              <CardTitle className="text-base">Upcoming Examination Assessments</CardTitle>
            </CardHeader>
            <CardContent className="p-0">
              {loading ? (
                <div className="p-12 flex justify-center">
                  <LoadingSpinner size="md" label="Loading examination schedules..." />
                </div>
              ) : exams.length === 0 ? (
                <div className="p-12 text-center text-xs text-slate-400">
                  No upcoming examinations scheduled for this subject.
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
                      <Th>Instructions</Th>
                    </Tr>
                  </Thead>
                  <Tbody>
                    {exams.map((exam) => (
                      <Tr key={exam.id}>
                        <Td className="font-semibold text-slate-900 dark:text-white">{exam.title}</Td>
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
                        <Td className="text-xs text-slate-500 max-w-xs truncate">
                          {exam.instructions || 'Standard academic regulations apply.'}
                        </Td>
                      </Tr>
                    ))}
                  </Tbody>
                </Table>
              )}
            </CardContent>
          </Card>
        </div>
      )}

      {activeTab === 'results' && (
        <Card>
          <CardHeader className="py-4">
            <CardTitle className="text-base">Official Academic Grades</CardTitle>
          </CardHeader>
          <CardContent className="p-0">
            {results.length === 0 ? (
              <div className="p-12 text-center text-xs text-slate-400">
                No examination results officially published yet.
              </div>
            ) : (
              <Table>
                <Thead>
                  <Tr>
                    <Th>Examination</Th>
                    <Th>Marks Obtained</Th>
                    <Th>Maximum Marks</Th>
                    <Th>Letter Grade</Th>
                    <Th>Evaluation Remarks</Th>
                    <Th>Graded At</Th>
                  </Tr>
                </Thead>
                <Tbody>
                  {results.map((res) => (
                    <Tr key={res.id}>
                      <Td className="font-semibold text-slate-900 dark:text-white">
                        {res.examinationTitle}
                      </Td>
                      <Td>
                        <span className="font-bold text-emerald-600 dark:text-emerald-400 text-sm">
                          {res.marksObtained}
                        </span>
                      </Td>
                      <Td className="text-xs text-slate-500">{res.totalMarks}</Td>
                      <Td>
                        <Badge
                          variant={res.grade === 'F' ? 'danger' : res.grade === 'A' || res.grade === 'A+' ? 'success' : 'primary'}
                          size="sm"
                        >
                          {res.grade}
                        </Badge>
                      </Td>
                      <Td className="text-xs text-slate-600 dark:text-slate-300 max-w-xs truncate">
                        {res.remarks || '—'}
                      </Td>
                      <Td className="text-xs text-slate-500">
                        {new Date(res.gradedAt).toLocaleDateString()}
                      </Td>
                    </Tr>
                  ))}
                </Tbody>
              </Table>
            )}
          </CardContent>
        </Card>
      )}
    </div>
  );
};
