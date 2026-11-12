import React, { useState, useEffect } from 'react';
import { facultyService } from '@/core/services/facultyService';
import { Subject, LiveSession, PageResponse } from '@/types';
import { Plus, Calendar, ExternalLink, Play, CheckCircle2, XCircle } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '@/shared/components/Card';
import { Table, Thead, Tbody, Tr, Th, Td } from '@/shared/components/Table';
import { Button } from '@/shared/components/Button';
import { StatusBadge } from '@/shared/components/Badge';
import { LoadingSpinner } from '@/shared/components/LoadingSpinner';
import { Modal } from '@/shared/components/Modal';
import { Input } from '@/shared/components/Input';
import { Textarea } from '@/shared/components/Textarea';
import { Select } from '@/shared/components/Select';
import { Breadcrumbs } from '@/shared/components/Breadcrumbs';

export const FacultyLiveSessionsPage: React.FC = () => {
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [selectedSubjectId, setSelectedSubjectId] = useState<number | ''>('');
  const [sessions, setSessions] = useState<LiveSession[]>([]);
  const [loading, setLoading] = useState(true);

  // Schedule Modal
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [meetingLink, setMeetingLink] = useState('');
  const [platform, setPlatform] = useState('Google Meet');
  const [scheduledStart, setScheduledStart] = useState('');
  const [scheduledEnd, setScheduledEnd] = useState('');
  const [createLoading, setCreateLoading] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);

  const [actionLoadingId, setActionLoadingId] = useState<number | null>(null);

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

  const fetchSessions = async () => {
    if (!selectedSubjectId) return;
    setLoading(true);
    try {
      const pageData: PageResponse<LiveSession> = await facultyService.getLiveSessionsForSubject(Number(selectedSubjectId));
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

  const handleCreateSession = async (e: React.FormEvent) => {
    e.preventDefault();
    setCreateError(null);

    if (!selectedSubjectId) {
      setCreateError('Please select a subject.');
      return;
    }

    setCreateLoading(true);
    try {
      await facultyService.createLiveSession({
        subjectId: Number(selectedSubjectId),
        title,
        description,
        meetingLink,
        platform,
        scheduledStart: new Date(scheduledStart).toISOString(),
        scheduledEnd: new Date(scheduledEnd).toISOString(),
      });
      setCreateModalOpen(false);
      setTitle('');
      setDescription('');
      setMeetingLink('');
      setScheduledStart('');
      setScheduledEnd('');
      await fetchSessions();
    } catch (err: any) {
      setCreateError(err?.message || 'Failed to schedule live session.');
    } finally {
      setCreateLoading(false);
    }
  };

  const handleUpdateStatus = async (sessionId: number, status: string) => {
    setActionLoadingId(sessionId);
    try {
      await facultyService.updateLiveSessionStatus(sessionId, status);
      await fetchSessions();
    } catch (err) {
      console.error('Failed to update session status', err);
    } finally {
      setActionLoadingId(null);
    }
  };

  return (
    <div className="space-y-6">
      <Breadcrumbs
        items={[
          { label: 'Faculty Portal', href: '/faculty' },
          { label: 'Live Video Classrooms' },
        ]}
      />

      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-white">
            Live Video Classrooms & Broadcasts
          </h1>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
            Orchestrate real-time lectures, start live broadcasts, and track meeting statuses
          </p>
        </div>

        <Button
          variant="primary"
          size="sm"
          onClick={() => setCreateModalOpen(true)}
          disabled={subjects.length === 0}
        >
          <Plus className="w-4 h-4 mr-1.5" /> Schedule Live Classroom
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

      {/* Live Sessions Table */}
      <Card>
        <CardHeader className="py-4 flex justify-between items-center">
          <CardTitle className="text-base">Classroom Broadcasts ({sessions.length})</CardTitle>
        </CardHeader>
        <CardContent className="p-0">
          {loading ? (
            <div className="p-12 flex justify-center">
              <LoadingSpinner size="md" label="Loading live classroom sessions..." />
            </div>
          ) : sessions.length === 0 ? (
            <div className="p-12 text-center text-xs text-slate-400">
              No live sessions scheduled for this subject yet.
            </div>
          ) : (
            <Table>
              <Thead>
                <Tr>
                  <Th>Session Title</Th>
                  <Th>Platform</Th>
                  <Th>Scheduled Start</Th>
                  <Th>Status</Th>
                  <Th className="text-right">Actions</Th>
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
                    <Td>
                      <span className="text-xs font-medium text-slate-600 dark:text-slate-300">
                        {sess.platform || 'Virtual Meeting'}
                      </span>
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
                      <div className="flex items-center justify-end gap-1.5">
                        <a href={sess.meetingLink} target="_blank" rel="noopener noreferrer">
                          <Button variant="outline" size="sm" className="text-indigo-600">
                            <ExternalLink className="w-3.5 h-3.5 mr-1" /> Open
                          </Button>
                        </a>

                        {sess.status === 'SCHEDULED' && (
                          <Button
                            variant="primary"
                            size="sm"
                            onClick={() => handleUpdateStatus(sess.id, 'LIVE')}
                            isLoading={actionLoadingId === sess.id}
                            className="bg-emerald-600 hover:bg-emerald-700 text-white"
                          >
                            <Play className="w-3.5 h-3.5 mr-1" /> Start Live
                          </Button>
                        )}

                        {sess.status === 'LIVE' && (
                          <Button
                            variant="primary"
                            size="sm"
                            onClick={() => handleUpdateStatus(sess.id, 'COMPLETED')}
                            isLoading={actionLoadingId === sess.id}
                            className="bg-slate-700 hover:bg-slate-800 text-white"
                          >
                            <CheckCircle2 className="w-3.5 h-3.5 mr-1" /> End Session
                          </Button>
                        )}

                        {(sess.status === 'SCHEDULED' || sess.status === 'LIVE') && (
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => handleUpdateStatus(sess.id, 'CANCELLED')}
                            disabled={actionLoadingId === sess.id}
                            className="text-rose-600 border-rose-300 hover:bg-rose-50"
                          >
                            <XCircle className="w-3.5 h-3.5 mr-1" /> Cancel
                          </Button>
                        )}
                      </div>
                    </Td>
                  </Tr>
                ))}
              </Tbody>
            </Table>
          )}
        </CardContent>
      </Card>

      {/* Schedule Live Session Modal */}
      <Modal
        isOpen={createModalOpen}
        onClose={() => setCreateModalOpen(false)}
        title="Schedule Live Video Classroom"
        maxWidth="lg"
      >
        <form onSubmit={handleCreateSession} className="p-6 space-y-4">
          {createError && (
            <div className="p-3 bg-rose-50 text-rose-700 text-xs rounded-lg">
              {createError}
            </div>
          )}

          <Input
            label="Classroom Session Title"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="e.g. Interactive Seminar: Neural Networks Architecture"
            required
          />

          <Textarea
            label="Session Agenda & Overview"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Key discussion points, topics, and prerequisite reading..."
            rows={3}
          />

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Input
              label="Meeting Platform"
              value={platform}
              onChange={(e) => setPlatform(e.target.value)}
              placeholder="Google Meet, Zoom, MS Teams"
              required
            />
            <Input
              label="Meeting Link / URL"
              type="url"
              value={meetingLink}
              onChange={(e) => setMeetingLink(e.target.value)}
              placeholder="https://meet.google.com/..."
              required
            />
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Input
              label="Scheduled Start Time"
              type="datetime-local"
              value={scheduledStart}
              onChange={(e) => setScheduledStart(e.target.value)}
              required
            />
            <Input
              label="Scheduled End Time"
              type="datetime-local"
              value={scheduledEnd}
              onChange={(e) => setScheduledEnd(e.target.value)}
              required
            />
          </div>

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
              Schedule Classroom
            </Button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
