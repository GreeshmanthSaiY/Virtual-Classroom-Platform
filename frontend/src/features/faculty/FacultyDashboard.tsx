import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { facultyService } from '@/core/services/facultyService';
import { FacultyDashboardData } from '@/types';
import {
  BookOpen,
  Video,
  FileText,
  Clock,
  Radio,
  ArrowRight,
  ExternalLink,
  Library,
  Users,
} from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '@/shared/components/Card';
import { Button } from '@/shared/components/Button';
import { StatusBadge } from '@/shared/components/Badge';
import { LoadingSpinner } from '@/shared/components/LoadingSpinner';

export const FacultyDashboard: React.FC = () => {
  const [data, setData] = useState<FacultyDashboardData | null>(null);
  const [loading, setLoading] = useState(true);

  const loadData = async () => {
    setLoading(true);
    try {
      const res = await facultyService.getDashboard();
      setData(res);
    } catch (err) {
      console.error('Failed to load faculty dashboard', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  if (loading) {
    return (
      <div className="flex justify-center items-center h-72">
        <LoadingSpinner size="lg" label="Loading faculty instructional dashboard..." />
      </div>
    );
  }

  const assignedSubjects = data?.assignedSubjects || [];
  const upcomingSessions = data?.upcomingLiveSessions || [];
  const pendingSubmissions = data?.pendingSubmissionsToGrade || [];

  return (
    <div className="space-y-6">
      {/* Banner */}
      <div className="bg-gradient-to-r from-indigo-800 to-slate-900 rounded-2xl p-6 sm:p-8 text-white shadow-lg flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <span className="px-2.5 py-0.5 rounded-full bg-indigo-500/20 text-indigo-300 border border-indigo-500/30 text-xs font-semibold uppercase tracking-wider">
            Faculty Teaching Workspace
          </span>
          <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight mt-2">Instructional Dashboard</h1>
          <p className="text-xs sm:text-sm text-indigo-200 mt-1">
            Assigned subjects, student grading queues, and virtual classroom orchestration
          </p>
        </div>
        <div className="flex gap-2">
          <Link to="/faculty/assignments">
            <Button variant="primary" size="sm" className="bg-indigo-600 hover:bg-indigo-500 text-white">
              <FileText className="w-3.5 h-3.5 mr-1.5" /> Grade Submissions
            </Button>
          </Link>
          <Link to="/faculty/live-sessions">
            <Button variant="outline" size="sm" className="bg-white/10 hover:bg-white/20 text-white border-white/20">
              <Radio className="w-3.5 h-3.5 mr-1.5" /> Start Live Class
            </Button>
          </Link>
        </div>
      </div>

      {/* Metrics Row */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <Card hover>
          <CardContent className="p-5 flex items-center gap-4">
            <div className="p-3 bg-indigo-50 dark:bg-indigo-950/50 rounded-xl text-indigo-600 dark:text-indigo-400">
              <Library className="w-6 h-6" />
            </div>
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Assigned Subjects</p>
              <h3 className="text-2xl font-bold text-slate-900 dark:text-white mt-0.5">
                {assignedSubjects.length}
              </h3>
            </div>
          </CardContent>
        </Card>

        <Card hover>
          <CardContent className="p-5 flex items-center gap-4">
            <div className="p-3 bg-amber-50 dark:bg-amber-950/50 rounded-xl text-amber-600 dark:text-amber-400">
              <Clock className="w-6 h-6" />
            </div>
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Pending Grading</p>
              <h3 className="text-2xl font-bold text-amber-600 dark:text-amber-400 mt-0.5">
                {data?.pendingSubmissionsCount ?? pendingSubmissions.length}
              </h3>
            </div>
          </CardContent>
        </Card>

        <Card hover>
          <CardContent className="p-5 flex items-center gap-4">
            <div className="p-3 bg-emerald-50 dark:bg-emerald-950/50 rounded-xl text-emerald-600 dark:text-emerald-400">
              <BookOpen className="w-6 h-6" />
            </div>
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Study Materials</p>
              <h3 className="text-2xl font-bold text-slate-900 dark:text-white mt-0.5">
                {data?.totalMaterialsUploaded ?? 0}
              </h3>
            </div>
          </CardContent>
        </Card>

        <Card hover>
          <CardContent className="p-5 flex items-center gap-4">
            <div className="p-3 bg-purple-50 dark:bg-purple-950/50 rounded-xl text-purple-600 dark:text-purple-400">
              <Video className="w-6 h-6" />
            </div>
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Video Lectures</p>
              <h3 className="text-2xl font-bold text-slate-900 dark:text-white mt-0.5">
                {data?.totalLecturesPublished ?? 0}
              </h3>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Main Split Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Pending Submissions Queue */}
        <Card>
          <CardHeader className="flex items-center justify-between">
            <div>
              <CardTitle>Submissions Awaiting Grading</CardTitle>
              <p className="text-xs text-slate-500 mt-0.5">Student coursework requiring assessment</p>
            </div>
            <Link to="/faculty/assignments">
              <Button variant="outline" size="sm">
                View All
              </Button>
            </Link>
          </CardHeader>
          <CardContent className="p-0">
            {pendingSubmissions.length === 0 ? (
              <div className="p-8 text-center text-xs text-slate-400">
                All submitted coursework is up to date!
              </div>
            ) : (
              <div className="divide-y divide-slate-100 dark:divide-slate-800">
                {pendingSubmissions.slice(0, 5).map((sub) => (
                  <div key={sub.id} className="p-4 flex items-center justify-between hover:bg-slate-50/50 transition-colors">
                    <div>
                      <p className="text-sm font-semibold text-slate-900 dark:text-white">
                        {sub.assignmentTitle}
                      </p>
                      <p className="text-xs text-slate-500 mt-0.5">
                        Student: <span className="font-medium">{sub.studentName}</span> • Submitted:{' '}
                        {new Date(sub.submittedAt).toLocaleDateString()}
                      </p>
                    </div>
                    <Link to="/faculty/assignments">
                      <Button variant="outline" size="sm" className="text-indigo-600">
                        Grade
                      </Button>
                    </Link>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>

        {/* Upcoming Live Sessions */}
        <Card>
          <CardHeader className="flex items-center justify-between">
            <div>
              <CardTitle>Upcoming Live Sessions</CardTitle>
              <p className="text-xs text-slate-500 mt-0.5">Scheduled virtual classes and broadcasts</p>
            </div>
            <Link to="/faculty/live-sessions">
              <Button variant="outline" size="sm">
                Manage
              </Button>
            </Link>
          </CardHeader>
          <CardContent className="p-0">
            {upcomingSessions.length === 0 ? (
              <div className="p-8 text-center text-xs text-slate-400">
                No upcoming live classes scheduled today.
              </div>
            ) : (
              <div className="divide-y divide-slate-100 dark:divide-slate-800">
                {upcomingSessions.slice(0, 5).map((sess) => (
                  <div key={sess.id} className="p-4 flex items-center justify-between hover:bg-slate-50/50 transition-colors">
                    <div>
                      <div className="flex items-center gap-2">
                        <p className="text-sm font-semibold text-slate-900 dark:text-white">{sess.title}</p>
                        <StatusBadge status={sess.status} size="sm" />
                      </div>
                      <p className="text-xs text-slate-500 mt-0.5">
                        {sess.subjectTitle} • {new Date(sess.scheduledStart).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                      </p>
                    </div>
                    <a href={sess.meetingLink} target="_blank" rel="noopener noreferrer">
                      <Button variant="primary" size="sm">
                        <ExternalLink className="w-3.5 h-3.5 mr-1" /> Launch
                      </Button>
                    </a>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Teaching Subjects Quick Roster Bar */}
      <Card>
        <CardHeader className="flex justify-between items-center">
          <CardTitle>My Teaching Subjects ({assignedSubjects.length})</CardTitle>
          <Link to="/faculty/subjects">
            <Button variant="outline" size="sm">
              View Rosters
              <ArrowRight className="w-3.5 h-3.5 ml-1" />
            </Button>
          </Link>
        </CardHeader>
        <CardContent className="p-4 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
          {assignedSubjects.map((sub) => (
            <div
              key={sub.id}
              className="p-3.5 rounded-xl border border-slate-200 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-800/40 flex items-center justify-between"
            >
              <div>
                <span className="font-mono text-xs font-bold text-indigo-600 dark:text-indigo-400">{sub.code}</span>
                <p className="text-sm font-semibold text-slate-900 dark:text-white mt-0.5">{sub.title}</p>
                <span className="text-[11px] text-slate-500">Year {sub.yearLevel} Level</span>
              </div>
              <Link to={`/faculty/subjects?subjectId=${sub.id}`}>
                <Button variant="outline" size="sm" className="text-xs">
                  <Users className="w-3 h-3 mr-1" /> Roster
                </Button>
              </Link>
            </div>
          ))}
        </CardContent>
      </Card>
    </div>
  );
};
