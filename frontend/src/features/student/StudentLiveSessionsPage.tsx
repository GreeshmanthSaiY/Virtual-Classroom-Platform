import React, { useState, useEffect } from 'react';
import { studentService } from '@/core/services/studentService';
import { Subject, LiveSession, PageResponse } from '@/types';
import { Calendar, ExternalLink } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '@/shared/components/Card';
import { Table, Thead, Tbody, Tr, Th, Td } from '@/shared/components/Table';
import { Button } from '@/shared/components/Button';
import { StatusBadge } from '@/shared/components/Badge';
import { LoadingSpinner } from '@/shared/components/LoadingSpinner';
import { Select } from '@/shared/components/Select';
import { Breadcrumbs } from '@/shared/components/Breadcrumbs';

export const StudentLiveSessionsPage: React.FC = () => {
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [selectedSubjectId, setSelectedSubjectId] = useState<number | ''>('');
  const [sessions, setSessions] = useState<LiveSession[]>([]);
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

  const fetchSessions = async () => {
    if (!selectedSubjectId) return;
    setLoading(true);
    try {
      const pageData: PageResponse<LiveSession> = await studentService.getLiveSessionsForSubject(Number(selectedSubjectId));
      setSessions(pageData?.content || []);
    } catch (err) {
      console.error('Failed to load live sessions', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (selectedSubjectId) {
      fetchSessions();
    }
  }, [selectedSubjectId]);

  return (
    <div className="space-y-6">
      <Breadcrumbs
        items={[
          { label: 'Student Portal', href: '/student' },
          { label: 'Live Video Classrooms' },
        ]}
      />

      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-white">
            Live Virtual Classrooms
          </h1>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
            Join scheduled live interactive lectures, webinars, and office hours with faculty
          </p>
        </div>
      </div>

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

      {/* Sessions Table */}
      <Card>
        <CardHeader className="py-4">
          <CardTitle className="text-base">Interactive Class Broadcasts ({sessions.length})</CardTitle>
        </CardHeader>
        <CardContent className="p-0">
          {loading ? (
            <div className="p-12 flex justify-center">
              <LoadingSpinner size="md" label="Loading live classroom sessions..." />
            </div>
          ) : sessions.length === 0 ? (
            <div className="p-12 text-center text-xs text-slate-400">
              No live sessions currently scheduled for this subject.
            </div>
          ) : (
            <Table>
              <Thead>
                <Tr>
                  <Th>Session Title</Th>
                  <Th>Host / Instructor</Th>
                  <Th>Platform</Th>
                  <Th>Scheduled Time</Th>
                  <Th>Status</Th>
                  <Th className="text-right">Action</Th>
                </Tr>
              </Thead>
              <Tbody>
                {sessions.map((sess) => (
                  <Tr key={sess.id}>
                    <Td>
                      <div className="font-semibold text-slate-900 dark:text-white flex items-center gap-2">
                        {sess.status === 'LIVE' && (
                          <span className="w-2.5 h-2.5 rounded-full bg-rose-500 animate-ping" />
                        )}
                        {sess.title}
                      </div>
                      <div className="text-xs text-slate-500 line-clamp-1">{sess.description}</div>
                    </Td>
                    <Td className="text-xs font-medium text-slate-700 dark:text-slate-300">
                      {sess.hostName || 'Faculty Instructor'}
                    </Td>
                    <Td className="text-xs text-slate-500">
                      {sess.platform || 'Virtual Meeting'}
                    </Td>
                    <Td>
                      <span className="text-xs text-slate-600 dark:text-slate-300 flex items-center gap-1.5">
                        <Calendar className="w-3.5 h-3.5 text-slate-400" />
                        {new Date(sess.scheduledStart).toLocaleString()}
                      </span>
                    </Td>
                    <Td>
                      <StatusBadge status={sess.status} size="sm" />
                    </Td>
                    <Td className="text-right">
                      {sess.status === 'CANCELLED' || sess.status === 'COMPLETED' ? (
                        <span className="text-xs text-slate-400">Concluded</span>
                      ) : (
                        <a href={sess.meetingLink} target="_blank" rel="noopener noreferrer">
                          <Button
                            variant="primary"
                            size="sm"
                            className={sess.status === 'LIVE' ? 'bg-rose-600 hover:bg-rose-700 animate-pulse text-white' : 'bg-emerald-600 hover:bg-emerald-700 text-white'}
                          >
                            <ExternalLink className="w-3.5 h-3.5 mr-1" />
                            {sess.status === 'LIVE' ? 'Join Live Now' : 'Open Link'}
                          </Button>
                        </a>
                      )}
                    </Td>
                  </Tr>
                ))}
              </Tbody>
            </Table>
          )}
        </CardContent>
      </Card>
    </div>
  );
};
