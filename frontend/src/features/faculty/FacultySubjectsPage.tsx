import React, { useState, useEffect } from 'react';
import { facultyService } from '@/core/services/facultyService';
import { Subject, User } from '@/types';
import { Users, GraduationCap } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '@/shared/components/Card';
import { Table, Thead, Tbody, Tr, Th, Td } from '@/shared/components/Table';
import { Button } from '@/shared/components/Button';
import { Badge } from '@/shared/components/Badge';
import { LoadingSpinner } from '@/shared/components/LoadingSpinner';
import { Modal } from '@/shared/components/Modal';
import { Breadcrumbs } from '@/shared/components/Breadcrumbs';

export const FacultySubjectsPage: React.FC = () => {
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [loading, setLoading] = useState(true);

  // Roster Modal
  const [rosterModalOpen, setRosterModalOpen] = useState(false);
  const [selectedSubject, setSelectedSubject] = useState<Subject | null>(null);
  const [roster, setRoster] = useState<User[]>([]);
  const [rosterLoading, setRosterLoading] = useState(false);

  useEffect(() => {
    const fetchSubjects = async () => {
      setLoading(true);
      try {
        const data = await facultyService.getMySubjects();
        setSubjects(data || []);
      } catch (err) {
        console.error('Failed to load faculty subjects', err);
      } finally {
        setLoading(false);
      }
    };
    fetchSubjects();
  }, []);

  const handleOpenRoster = async (subject: Subject) => {
    setSelectedSubject(subject);
    setRosterModalOpen(true);
    setRosterLoading(true);
    try {
      const students = await facultyService.getEnrolledStudents(subject.id);
      setRoster(students || []);
    } catch (err) {
      console.error('Failed to load student roster', err);
    } finally {
      setRosterLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      <Breadcrumbs
        items={[
          { label: 'Faculty Portal', href: '/faculty' },
          { label: 'Teaching Subjects & Rosters' },
        ]}
      />

      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-white">
            Teaching Subjects & Enrolled Rosters
          </h1>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
            Curriculum subjects assigned to your instruction and student enrollment rosters
          </p>
        </div>
      </div>

      {loading ? (
        <div className="p-12 flex justify-center">
          <LoadingSpinner size="md" label="Loading assigned academic subjects..." />
        </div>
      ) : subjects.length === 0 ? (
        <Card>
          <CardContent className="p-12 text-center text-xs text-slate-400">
            No subjects are currently assigned to your instruction.
          </CardContent>
        </Card>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {subjects.map((sub) => (
            <Card key={sub.id} hover className="flex flex-col justify-between">
              <CardHeader className="pb-3">
                <div className="flex items-center justify-between">
                  <Badge variant="primary" size="sm">
                    {sub.code}
                  </Badge>
                  <span className="text-xs font-medium text-slate-500">Year {sub.yearLevel}</span>
                </div>
                <CardTitle className="text-base mt-2">{sub.title}</CardTitle>
                <p className="text-xs text-slate-500 mt-0.5">{sub.courseName || 'Core Curriculum'}</p>
              </CardHeader>

              <CardContent className="pt-0">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => handleOpenRoster(sub)}
                  className="w-full text-indigo-600 border-indigo-200 hover:bg-indigo-50 dark:border-indigo-800"
                >
                  <Users className="w-3.5 h-3.5 mr-1.5" /> Inspect Enrolled Roster
                </Button>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {/* Student Roster Modal */}
      <Modal
        isOpen={rosterModalOpen}
        onClose={() => setRosterModalOpen(false)}
        title={`Enrolled Student Roster — ${selectedSubject?.title || ''}`}
        maxWidth="lg"
      >
        <div className="p-6 space-y-4">
          <div className="flex items-center justify-between pb-2 border-b border-slate-100 dark:border-slate-800">
            <div>
              <span className="text-xs text-slate-500">Subject Code</span>
              <p className="text-sm font-bold text-slate-900 dark:text-white">
                {selectedSubject?.code}
              </p>
            </div>
            <Badge variant="neutral" size="sm">
              {roster.length} Students Enrolled
            </Badge>
          </div>

          {rosterLoading ? (
            <div className="p-8 flex justify-center">
              <LoadingSpinner size="md" label="Fetching enrolled roster..." />
            </div>
          ) : roster.length === 0 ? (
            <div className="p-8 text-center text-xs text-slate-400">
              No students currently enrolled in this subject.
            </div>
          ) : (
            <div className="max-h-96 overflow-y-auto">
              <Table>
                <Thead>
                  <Tr>
                    <Th>Roll Number</Th>
                    <Th>Student Name</Th>
                    <Th>Institutional Email</Th>
                  </Tr>
                </Thead>
                <Tbody>
                  {roster.map((student) => (
                    <Tr key={student.id}>
                      <Td className="font-mono text-xs font-semibold">{student.rollNumber || 'N/A'}</Td>
                      <Td className="font-medium text-slate-900 dark:text-white flex items-center gap-2">
                        <GraduationCap className="w-4 h-4 text-slate-400" />
                        {student.fullName}
                      </Td>
                      <Td className="text-xs text-slate-500">{student.email}</Td>
                    </Tr>
                  ))}
                </Tbody>
              </Table>
            </div>
          )}

          <div className="flex justify-end pt-2">
            <Button variant="outline" size="sm" onClick={() => setRosterModalOpen(false)}>
              Close
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
};
