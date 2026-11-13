import React, { useState, useEffect } from 'react';
import { adminService } from '@/core/services/adminService';
import { Course, Subject, User } from '@/types';
import { Layers, Archive, ArchiveRestore, UserPlus, BookOpen, CheckCircle2, GraduationCap } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '@/shared/components/Card';
import { Table, Thead, Tbody, Tr, Th, Td } from '@/shared/components/Table';
import { Button } from '@/shared/components/Button';
import { Badge } from '@/shared/components/Badge';
import { LoadingSpinner } from '@/shared/components/LoadingSpinner';
import { Modal } from '@/shared/components/Modal';
import { Select } from '@/shared/components/Select';
import { Input } from '@/shared/components/Input';
import { Breadcrumbs } from '@/shared/components/Breadcrumbs';

export const AdminCoursesPage: React.FC = () => {
  const [courses, setCourses] = useState<Course[]>([]);
  const [loading, setLoading] = useState(true);
  const [facultyList, setFacultyList] = useState<User[]>([]);

  // Assign Faculty Modal
  const [assignModalOpen, setAssignModalOpen] = useState(false);
  const [selectedSubject, setSelectedSubject] = useState<Subject | null>(null);
  const [selectedFacultyId, setSelectedFacultyId] = useState<number | ''>('');
  const [academicTerm, setAcademicTerm] = useState('2026-SPRING');
  const [actionLoading, setActionLoading] = useState(false);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  const fetchData = async () => {
    setLoading(true);
    try {
      const [cList, fPage] = await Promise.all([
        adminService.getCourses(),
        adminService.getUsers({ role: 'ROLE_FACULTY', size: 100 }),
      ]);
      setCourses(cList || []);
      setFacultyList(fPage?.content || []);
    } catch (err) {
      console.error('Failed to load courses', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleArchive = async (courseId: number) => {
    setActionLoading(true);
    try {
      await adminService.archiveCourse(courseId);
      await fetchData();
    } catch (err) {
      console.error('Archive failed', err);
    } finally {
      setActionLoading(false);
    }
  };

  const handleUnarchive = async (courseId: number) => {
    setActionLoading(true);
    try {
      await adminService.unarchiveCourse(courseId);
      await fetchData();
    } catch (err) {
      console.error('Unarchive failed', err);
    } finally {
      setActionLoading(false);
    }
  };

  const handleOpenAssign = (subject: Subject) => {
    setSelectedSubject(subject);
    setSelectedFacultyId(facultyList[0]?.id || '');
    setSuccessMessage(null);
    setAssignModalOpen(true);
  };

  const handleConfirmAssign = async () => {
    if (!selectedSubject || !selectedFacultyId) return;
    setActionLoading(true);
    try {
      await adminService.assignFacultyToSubject(selectedSubject.id, Number(selectedFacultyId), academicTerm);
      setSuccessMessage(`Faculty successfully assigned to ${selectedSubject.title}!`);
      setTimeout(() => {
        setAssignModalOpen(false);
        setSelectedSubject(null);
        setSuccessMessage(null);
      }, 1500);
    } catch (err) {
      console.error('Failed to assign faculty', err);
    } finally {
      setActionLoading(false);
    }
  };

  return (
    <div className="space-y-6 sm:space-y-8 animate-fade-in">
      <Breadcrumbs
        items={[
          { label: 'Admin Portal', href: '/admin' },
          { label: 'Degrees & Courses' },
        ]}
      />

      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <div className="flex items-center gap-2">
            <div className="p-2 rounded-xl bg-purple-50 dark:bg-purple-950/60 text-purple-600 dark:text-purple-400">
              <Layers className="w-5 h-5" />
            </div>
            <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-slate-900 dark:text-white">
              Curriculum, Degrees & Courses
            </h1>
          </div>
          <p className="text-xs sm:text-sm text-slate-500 dark:text-slate-400 mt-1.5">
            Manage degree programs, catalog entities, and instructional faculty assignments
          </p>
        </div>
      </div>

      {loading ? (
        <div className="p-16 flex justify-center">
          <LoadingSpinner size="md" label="Loading degree and course catalog..." />
        </div>
      ) : courses.length === 0 ? (
        <Card className="border-slate-200/80 dark:border-slate-800/80 shadow-soft-sm">
          <CardContent className="p-16 text-center space-y-3">
            <div className="w-12 h-12 rounded-2xl bg-purple-50 dark:bg-purple-950/60 text-purple-600 dark:text-purple-400 flex items-center justify-center mx-auto">
              <Layers className="w-6 h-6" />
            </div>
            <h4 className="text-sm font-semibold text-slate-800 dark:text-slate-200">
              No Degree Programs Found
            </h4>
            <p className="text-xs text-slate-400">
              Courses and subjects will appear here once academic data is seeded.
            </p>
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-6">
          {courses.map((course) => (
            <Card
              key={course.id}
              className={`border-slate-200/80 dark:border-slate-800/80 shadow-soft-sm overflow-hidden transition-all ${
                course.archived ? 'opacity-80 bg-slate-50/50 dark:bg-slate-900/50' : 'bg-white/90 dark:bg-slate-900/90'
              }`}
            >
              <CardHeader className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-5 sm:p-6 bg-slate-50/40 dark:bg-slate-800/20 border-b border-slate-100 dark:border-slate-800">
                <div className="flex items-center gap-3.5">
                  <div className="w-10 h-10 rounded-2xl bg-purple-50 dark:bg-purple-950/60 text-purple-600 dark:text-purple-400 flex items-center justify-center shadow-soft-sm shrink-0">
                    <GraduationCap className="w-5 h-5" />
                  </div>
                  <div>
                    <div className="flex flex-wrap items-center gap-2">
                      <CardTitle className="text-base sm:text-lg font-bold text-slate-900 dark:text-white">
                        {course.name}
                      </CardTitle>
                      <Badge variant="neutral" size="sm" className="font-mono">
                        {course.code}
                      </Badge>
                      {course.archived && (
                        <Badge variant="warning" size="sm">
                          ARCHIVED
                        </Badge>
                      )}
                    </div>
                    <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
                      Degree Code: <span className="font-semibold">{course.degreeCode || `Degree #${course.degreeId}`}</span>
                      {' • '}
                      {course.subjects?.length || 0} Curriculum Subjects
                    </p>
                  </div>
                </div>

                <div className="flex items-center gap-2 self-end sm:self-center shrink-0">
                  {course.archived ? (
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => handleUnarchive(course.id)}
                      disabled={actionLoading}
                      className="text-emerald-600 border-emerald-200 dark:border-emerald-900/60 hover:bg-emerald-50 dark:hover:bg-emerald-950/30"
                    >
                      <ArchiveRestore className="w-3.5 h-3.5 mr-1" /> Unarchive Course
                    </Button>
                  ) : (
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => handleArchive(course.id)}
                      disabled={actionLoading}
                      className="text-amber-600 border-amber-200 dark:border-amber-900/60 hover:bg-amber-50 dark:hover:bg-amber-950/30"
                    >
                      <Archive className="w-3.5 h-3.5 mr-1" /> Archive Course
                    </Button>
                  )}
                </div>
              </CardHeader>

              <CardContent className="p-0">
                {course.subjects && course.subjects.length > 0 ? (
                  <Table>
                    <Thead>
                      <Tr>
                        <Th>Subject Code</Th>
                        <Th>Curriculum Title</Th>
                        <Th>Academic Level</Th>
                        <Th className="text-right">Faculty Assignment</Th>
                      </Tr>
                    </Thead>
                    <Tbody>
                      {course.subjects.map((sub) => (
                        <Tr key={sub.id}>
                          <Td className="font-mono text-xs font-semibold text-primary-600 dark:text-primary-400">
                            {sub.code}
                          </Td>
                          <Td className="font-medium text-slate-900 dark:text-white">
                            <div className="flex items-center gap-2">
                              <BookOpen className="w-4 h-4 text-slate-400 shrink-0" />
                              <span>{sub.title}</span>
                            </div>
                          </Td>
                          <Td>
                            <Badge variant="neutral" size="sm">
                              Year {sub.yearLevel}
                            </Badge>
                          </Td>
                          <Td className="text-right">
                            <Button
                              variant="outline"
                              size="sm"
                              onClick={() => handleOpenAssign(sub)}
                              className="text-primary-600 border-primary-200 dark:border-primary-900/60 hover:bg-primary-50 dark:hover:bg-primary-950/30 shadow-soft-sm"
                            >
                              <UserPlus className="w-3.5 h-3.5 mr-1" /> Assign Faculty
                            </Button>
                          </Td>
                        </Tr>
                      ))}
                    </Tbody>
                  </Table>
                ) : (
                  <div className="p-6 text-center text-xs text-slate-400">
                    No curriculum subjects registered under this degree program.
                  </div>
                )}
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {/* Assign Faculty Modal */}
      <Modal
        isOpen={assignModalOpen}
        onClose={() => setAssignModalOpen(false)}
        title="Assign Faculty Member to Subject"
      >
        <div className="p-6 space-y-4">
          {successMessage ? (
            <div className="p-4 rounded-2xl bg-emerald-50 text-emerald-800 dark:bg-emerald-950/40 dark:text-emerald-200 text-xs flex items-center gap-2 font-medium shadow-soft-sm">
              <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0" />
              {successMessage}
            </div>
          ) : (
            <>
              <div className="p-3 bg-slate-50 dark:bg-slate-800/50 rounded-xl border border-slate-100 dark:border-slate-800">
                <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">
                  Target Subject
                </span>
                <p className="text-sm font-bold text-slate-900 dark:text-white mt-0.5">
                  {selectedSubject?.code} — {selectedSubject?.title}
                </p>
              </div>

              <Select
                label="Select Faculty Instructor"
                options={facultyList.map((f) => ({
                  value: f.id,
                  label: `${f.fullName} (${f.department || f.email})`,
                }))}
                value={selectedFacultyId}
                onChange={(e) => setSelectedFacultyId(Number(e.target.value))}
                required
              />

              <Input
                label="Academic Term / Semester"
                value={academicTerm}
                onChange={(e) => setAcademicTerm(e.target.value)}
                placeholder="e.g. 2026-SPRING"
                required
                helperText="Specify the institutional calendar term for this teaching assignment."
              />

              <div className="flex justify-end gap-2.5 pt-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => setAssignModalOpen(false)}
                  disabled={actionLoading}
                >
                  Cancel
                </Button>
                <Button
                  variant="primary"
                  size="sm"
                  onClick={handleConfirmAssign}
                  isLoading={actionLoading}
                >
                  Confirm Assignment
                </Button>
              </div>
            </>
          )}
        </div>
      </Modal>
    </div>
  );
};
