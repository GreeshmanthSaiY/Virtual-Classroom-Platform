import React, { useState, useEffect } from 'react';
import { facultyService } from '@/core/services/facultyService';
import { Subject, User } from '@/types';
import { Users, Library, Search } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '@/shared/components/Card';
import { Table, Thead, Tbody, Tr, Th, Td } from '@/shared/components/Table';
import { Button } from '@/shared/components/Button';
import { Badge, StatusBadge } from '@/shared/components/Badge';
import { LoadingSpinner } from '@/shared/components/LoadingSpinner';
import { Modal } from '@/shared/components/Modal';
import { Input } from '@/shared/components/Input';
import { Breadcrumbs } from '@/shared/components/Breadcrumbs';

export const FacultySubjectsPage: React.FC = () => {
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');

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

  const filteredSubjects = subjects.filter(
    (s) =>
      s.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
      s.code.toLowerCase().includes(searchTerm.toLowerCase()) ||
      (s.courseName && s.courseName.toLowerCase().includes(searchTerm.toLowerCase()))
  );

  return (
    <div className="space-y-6 sm:space-y-8 animate-fade-in">
      <Breadcrumbs
        items={[
          { label: 'Faculty Portal', href: '/faculty' },
          { label: 'Teaching Subjects & Rosters' },
        ]}
      />

      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <div className="flex items-center gap-2">
            <div className="p-2 rounded-xl bg-primary-50 dark:bg-primary-950/60 text-primary-600 dark:text-primary-400">
              <Library className="w-5 h-5" />
            </div>
            <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-slate-900 dark:text-white">
              Teaching Subjects & Enrolled Rosters
            </h1>
          </div>
          <p className="text-xs sm:text-sm text-slate-500 dark:text-slate-400 mt-1.5">
            Curriculum subjects assigned to your instruction and official student rosters
          </p>
        </div>

        {subjects.length > 0 && (
          <div className="w-full sm:w-64">
            <Input
              placeholder="Search subjects..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              icon={<Search className="w-4 h-4 text-slate-400" />}
            />
          </div>
        )}
      </div>

      {loading ? (
        <div className="p-16 flex justify-center">
          <LoadingSpinner size="md" label="Loading assigned academic subjects..." />
        </div>
      ) : subjects.length === 0 ? (
        <Card className="border-slate-200/80 dark:border-slate-800/80 shadow-soft-sm">
          <CardContent className="p-16 text-center space-y-3">
            <div className="w-12 h-12 rounded-2xl bg-primary-50 dark:bg-primary-950/60 text-primary-600 dark:text-primary-400 flex items-center justify-center mx-auto">
              <Library className="w-6 h-6" />
            </div>
            <h4 className="text-sm font-semibold text-slate-800 dark:text-slate-200">
              No Subjects Assigned
            </h4>
            <p className="text-xs text-slate-400">
              No subjects are currently assigned to your instruction by the platform administrator.
            </p>
          </CardContent>
        </Card>
      ) : filteredSubjects.length === 0 ? (
        <Card className="border-slate-200/80 dark:border-slate-800/80 shadow-soft-sm">
          <CardContent className="p-12 text-center text-xs text-slate-400">
            No subjects match your search query &quot;{searchTerm}&quot;.
          </CardContent>
        </Card>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {filteredSubjects.map((sub) => (
            <Card
              key={sub.id}
              hover
              className="border-slate-200/80 dark:border-slate-800/80 shadow-soft-sm bg-white/90 dark:bg-slate-900/90 backdrop-blur-sm flex flex-col justify-between overflow-hidden"
            >
              <CardHeader className="p-5 pb-3">
                <div className="flex items-center justify-between">
                  <span className="font-mono text-xs font-bold text-primary-600 dark:text-primary-400 bg-primary-50 dark:bg-primary-950/60 px-2.5 py-1 rounded-lg">
                    {sub.code}
                  </span>
                  <Badge variant="neutral" size="sm">
                    Year {sub.yearLevel} Level
                  </Badge>
                </div>
                <CardTitle className="text-base sm:text-lg font-bold text-slate-900 dark:text-white mt-3 line-clamp-1">
                  {sub.title}
                </CardTitle>
                <p className="text-xs text-slate-500 dark:text-slate-400 mt-1 line-clamp-1">
                  Program: {sub.courseName || 'Core Curriculum'}
                </p>
              </CardHeader>

              <CardContent className="p-5 pt-3">
                <div className="pt-3 border-t border-slate-100 dark:border-slate-800/80 flex items-center justify-between">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => handleOpenRoster(sub)}
                    className="w-full text-primary-600 border-primary-200 dark:border-primary-900/60 hover:bg-primary-50 dark:hover:bg-primary-950/30 text-xs shadow-soft-sm"
                  >
                    <Users className="w-3.5 h-3.5 mr-1.5" /> Inspect Enrolled Roster
                  </Button>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {/* Student Roster Modal */}
      <Modal
        isOpen={rosterModalOpen}
        onClose={() => setRosterModalOpen(false)}
        title={`Student Roster — ${selectedSubject?.title || ''}`}
        maxWidth="lg"
      >
        <div className="p-6 space-y-4">
          <div className="flex items-center justify-between pb-3 border-b border-slate-100 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-800/30 -mx-6 -mt-6 p-6">
            <div>
              <span className="text-[11px] font-bold text-slate-400 uppercase tracking-wider">Subject</span>
              <p className="text-sm font-bold text-slate-900 dark:text-white mt-0.5">
                {selectedSubject?.code} — {selectedSubject?.title}
              </p>
            </div>
            <Badge variant="neutral" size="sm" className="font-semibold">
              {roster.length} Enrolled Scholars
            </Badge>
          </div>

          {rosterLoading ? (
            <div className="p-12 flex justify-center">
              <LoadingSpinner size="md" label="Fetching enrolled roster..." />
            </div>
          ) : roster.length === 0 ? (
            <div className="py-12 px-4 text-center space-y-2">
              <div className="w-10 h-10 rounded-2xl bg-slate-100 dark:bg-slate-800 text-slate-400 flex items-center justify-center mx-auto">
                <Users className="w-5 h-5" />
              </div>
              <h4 className="text-sm font-semibold text-slate-800 dark:text-slate-200">
                No Enrolled Students
              </h4>
              <p className="text-xs text-slate-400">
                No active students are currently registered in this subject.
              </p>
            </div>
          ) : (
            <div className="max-h-96 overflow-y-auto rounded-xl border border-slate-200/80 dark:border-slate-800/80">
              <Table>
                <Thead>
                  <Tr>
                    <Th>Roll Number</Th>
                    <Th>Scholar Name</Th>
                    <Th>Institutional Email</Th>
                    <Th>Status</Th>
                  </Tr>
                </Thead>
                <Tbody>
                  {roster.map((student) => (
                    <Tr key={student.id}>
                      <Td className="font-mono text-xs font-bold text-primary-600 dark:text-primary-400">
                        {student.rollNumber || 'PENDING'}
                      </Td>
                      <Td className="font-medium text-slate-900 dark:text-white">
                        <div className="flex items-center gap-2.5">
                          <div className="w-7 h-7 rounded-lg bg-slate-100 dark:bg-slate-800 flex items-center justify-center text-xs font-bold text-slate-700 dark:text-slate-200">
                            {student.fullName ? student.fullName.charAt(0).toUpperCase() : 'S'}
                          </div>
                          <span>{student.fullName}</span>
                        </div>
                      </Td>
                      <Td className="text-xs text-slate-500 dark:text-slate-400">
                        {student.email}
                      </Td>
                      <Td>
                        <StatusBadge status={student.status} size="sm" />
                      </Td>
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
