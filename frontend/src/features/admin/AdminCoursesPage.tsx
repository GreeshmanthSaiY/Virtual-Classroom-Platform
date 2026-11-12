import React, { useState, useEffect } from 'react';
import { adminService } from '@/core/services/adminService';
import { Course, Subject, User } from '@/types';
import { Layers, Archive, ArchiveRestore, UserPlus, BookOpen, CheckCircle2 } from 'lucide-react';
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
    <div className="space-y-6">
      <Breadcrumbs
        items={[
          { label: 'Admin Portal', href: '/admin' },
          { label: 'Degrees & Courses' },
        ]}
      />

      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-white">
            Curriculum, Degrees & Courses
          </h1>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
            Manage degree programs, archive completed courses, and assign faculty instructors to subjects
          </p>
        </div>
      </div>

      {loading ? (
        <div className="p-12 flex justify-center">
          <LoadingSpinner size="md" label="Loading degree and course catalog..." />
        </div>
      ) : courses.length === 0 ? (
        <Card>
          <CardContent className="p-12 text-center text-xs text-slate-400">
            No courses found in platform catalog.
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-6">
          {courses.map((course) => (
            <Card key={course.id} className={course.archived ? 'opacity-75 bg-slate-50/50' : ''}>
              <CardHeader className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 pb-3">
                <div className="flex items-center gap-3">
                  <div className="p-2.5 rounded-xl bg-purple-50 dark:bg-purple-950/50 text-purple-600 dark:text-purple-400">
                    <Layers className="w-5 h-5" />
                  </div>
                  <div>
                    <div className="flex items-center gap-2">
                      <CardTitle className="text-base">{course.name}</CardTitle>
                      <Badge variant="neutral" size="sm">
                        {course.code}
                      </Badge>
                      {course.archived && (
                        <Badge variant="warning" size="sm">
                          ARCHIVED
                        </Badge>
                      )}
                    </div>
                    <p className="text-xs text-slate-500 mt-0.5">
                      Degree Program: {course.degreeCode || `Degree #${course.degreeId}`}
                    </p>
                  </div>
                </div>

                <div className="flex items-center gap-2">
                  {course.archived ? (
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => handleUnarchive(course.id)}
                      disabled={actionLoading}
                      className="text-emerald-600 border-emerald-300 dark:border-emerald-800"
                    >
                      <ArchiveRestore className="w-3.5 h-3.5 mr-1" /> Unarchive Course
                    </Button>
                  ) : (
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => handleArchive(course.id)}
                      disabled={actionLoading}
                      className="text-amber-600 border-amber-300 dark:border-amber-800"
                    >
                      <Archive className="w-3.5 h-3.5 mr-1" /> Archive Course
                    </Button>
                  )}
                </div>
              </CardHeader>

              <CardContent className="p-0 border-t border-slate-100 dark:border-slate-800">
                {course.subjects && course.subjects.length > 0 ? (
                  <Table>
                    <Thead>
                      <Tr>
                        <Th>Subject Code</Th>
                        <Th>Subject Title</Th>
                        <Th>Year Level</Th>
                        <Th className="text-right">Instructional Assignment</Th>
                      </Tr>
                    </Thead>
                    <Tbody>
                      {course.subjects.map((sub) => (
                        <Tr key={sub.id}>
                          <Td className="font-mono text-xs">{sub.code}</Td>
                          <Td className="font-medium text-slate-900 dark:text-white flex items-center gap-2">
                            <BookOpen className="w-4 h-4 text-slate-400" />
                            {sub.title}
                          </Td>
                          <Td>Year {sub.yearLevel}</Td>
                          <Td className="text-right">
                            <Button
                              variant="outline"
                              size="sm"
                              onClick={() => handleOpenAssign(sub)}
                              className="text-indigo-600 border-indigo-200 hover:bg-indigo-50"
                            >
                              <UserPlus className="w-3.5 h-3.5 mr-1" /> Assign Faculty
                            </Button>
                          </Td>
                        </Tr>
                      ))}
                    </Tbody>
                  </Table>
                ) : (
                  <div className="p-4 text-center text-xs text-slate-400">
                    No subjects configured under this course.
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
            <div className="p-4 rounded-xl bg-emerald-50 text-emerald-800 dark:bg-emerald-950/40 dark:text-emerald-200 text-xs flex items-center gap-2 font-medium">
              <CheckCircle2 className="w-4 h-4 text-emerald-600" />
              {successMessage}
            </div>
          ) : (
            <>
              <div>
                <span className="text-xs text-slate-500">Selected Subject</span>
                <p className="text-sm font-semibold text-slate-800 dark:text-slate-200">
                  {selectedSubject?.code} - {selectedSubject?.title}
                </p>
              </div>

              <Select
                label="Faculty Instructor"
                options={facultyList.map((f) => ({
                  value: f.id,
                  label: `${f.fullName} (${f.department || f.email})`,
                }))}
                value={selectedFacultyId}
                onChange={(e) => setSelectedFacultyId(Number(e.target.value))}
                required
              />

              <Input
                label="Academic Term"
                value={academicTerm}
                onChange={(e) => setAcademicTerm(e.target.value)}
                placeholder="e.g. 2026-SPRING or CURRENT"
                required
              />

              <div className="flex justify-end gap-2 pt-2">
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
                  Save Assignment
                </Button>
              </div>
            </>
          )}
        </div>
      </Modal>
    </div>
  );
};
