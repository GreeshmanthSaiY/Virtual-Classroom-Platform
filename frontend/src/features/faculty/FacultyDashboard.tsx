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
  Sparkles,
  CheckCircle2,
  Calendar,
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
      <div className="flex flex-col justify-center items-center h-80 space-y-4">
        <LoadingSpinner size="lg" label="Loading faculty instructional dashboard..." />
      </div>
    );
  }

  const assignedSubjects = data?.assignedSubjects || [];
  const upcomingSessions = data?.upcomingLiveSessions || [];
  const pendingSubmissions = data?.pendingSubmissionsToGrade || [];

  return (
    <div className="space-y-6 sm:space-y-8 animate-fade-in">
      {/* Banner */}
      <div className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-slate-900 via-indigo-950 to-slate-900 border border-slate-800 p-6 sm:p-8 text-white shadow-soft-xl">
        <div className="absolute top-0 right-0 -mt-10 -mr-10 w-80 h-80 rounded-full bg-primary-600/15 blur-3xl pointer-events-none" />
        <div className="absolute bottom-0 left-1/3 -mb-10 w-60 h-60 rounded-full bg-teal-500/10 blur-3xl pointer-events-none" />

        <div className="relative z-10 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-6">
          <div className="space-y-2 max-w-2xl">
            <div className="flex flex-wrap items-center gap-2">
              <span className="px-3 py-1 rounded-full bg-primary-500/20 text-primary-300 border border-primary-500/30 text-xs font-semibold uppercase tracking-wider flex items-center gap-1.5 backdrop-blur-sm">
                <Library className="w-3.5 h-3.5" /> Teaching Workspace
              </span>
              <span className="px-2.5 py-0.5 rounded-full bg-white/10 text-slate-300 text-xs font-medium backdrop-blur-sm flex items-center gap-1">
                <Sparkles className="w-3 h-3 text-amber-400" /> Active Semester
              </span>
            </div>
            <h1 className="text-2xl sm:text-3xl lg:text-4xl font-extrabold tracking-tight text-white">
              Instructional Dashboard
            </h1>
            <p className="text-sm text-slate-300 leading-relaxed">
              Assigned curriculum subjects, student grading queues, and virtual video classroom broadcasts.
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-3 self-stretch sm:self-auto">
            <Link to="/faculty/assignments" className="flex-1 sm:flex-none">
              <Button
                variant="primary"
                size="md"
                className="w-full sm:w-auto bg-primary-600 hover:bg-primary-500 text-white shadow-soft-sm"
              >
                <FileText className="w-4 h-4 mr-2" /> Grade Work
              </Button>
            </Link>
            <Link to="/faculty/live-sessions" className="flex-1 sm:flex-none">
              <Button
                variant="outline"
                size="md"
                className="w-full sm:w-auto bg-white/10 hover:bg-white/20 text-white border-white/20 shadow-soft-sm backdrop-blur-sm"
              >
                <Radio className="w-4 h-4 mr-2 text-rose-400 animate-pulse" /> Launch Live Class
              </Button>
            </Link>
          </div>
        </div>
      </div>

      {/* Metrics Row */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 sm:gap-5">
        <Card hover className="border-slate-200/80 dark:border-slate-800/80 bg-white/90 dark:bg-slate-900/90 backdrop-blur-sm">
          <CardContent className="p-5 flex items-center gap-4">
            <div className="p-3.5 bg-primary-50 dark:bg-primary-950/60 rounded-2xl text-primary-600 dark:text-primary-400 shadow-soft-sm shrink-0">
              <Library className="w-6 h-6" />
            </div>
            <div className="min-w-0">
              <p className="text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider truncate">
                Assigned Subjects
              </p>
              <h3 className="text-2xl sm:text-3xl font-extrabold text-slate-900 dark:text-white mt-0.5 tracking-tight">
                {assignedSubjects.length}
              </h3>
              <p className="text-[11px] text-slate-400 dark:text-slate-500 flex items-center gap-1 mt-0.5">
                Teaching assignments
              </p>
            </div>
          </CardContent>
        </Card>

        <Card hover className="border-slate-200/80 dark:border-slate-800/80 bg-white/90 dark:bg-slate-900/90 backdrop-blur-sm">
          <CardContent className="p-5 flex items-center gap-4">
            <div className="p-3.5 bg-amber-50 dark:bg-amber-950/60 rounded-2xl text-amber-600 dark:text-amber-400 shadow-soft-sm shrink-0">
              <Clock className="w-6 h-6" />
            </div>
            <div className="min-w-0">
              <p className="text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider truncate">
                Pending Grading
              </p>
              <h3 className="text-2xl sm:text-3xl font-extrabold text-amber-600 dark:text-amber-400 mt-0.5 tracking-tight">
                {data?.pendingSubmissionsCount ?? pendingSubmissions.length}
              </h3>
              <p className="text-[11px] text-slate-400 dark:text-slate-500 flex items-center gap-1 mt-0.5">
                Submissions to assess
              </p>
            </div>
          </CardContent>
        </Card>

        <Card hover className="border-slate-200/80 dark:border-slate-800/80 bg-white/90 dark:bg-slate-900/90 backdrop-blur-sm">
          <CardContent className="p-5 flex items-center gap-4">
            <div className="p-3.5 bg-emerald-50 dark:bg-emerald-950/60 rounded-2xl text-emerald-600 dark:text-emerald-400 shadow-soft-sm shrink-0">
              <BookOpen className="w-6 h-6" />
            </div>
            <div className="min-w-0">
              <p className="text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider truncate">
                Study Materials
              </p>
              <h3 className="text-2xl sm:text-3xl font-extrabold text-slate-900 dark:text-white mt-0.5 tracking-tight">
                {data?.totalMaterialsUploaded ?? 0}
              </h3>
              <p className="text-[11px] text-slate-400 dark:text-slate-500 flex items-center gap-1 mt-0.5">
                Published resources
              </p>
            </div>
          </CardContent>
        </Card>

        <Card hover className="border-slate-200/80 dark:border-slate-800/80 bg-white/90 dark:bg-slate-900/90 backdrop-blur-sm">
          <CardContent className="p-5 flex items-center gap-4">
            <div className="p-3.5 bg-purple-50 dark:bg-purple-950/60 rounded-2xl text-purple-600 dark:text-purple-400 shadow-soft-sm shrink-0">
              <Video className="w-6 h-6" />
            </div>
            <div className="min-w-0">
              <p className="text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider truncate">
                Video Lectures
              </p>
              <h3 className="text-2xl sm:text-3xl font-extrabold text-slate-900 dark:text-white mt-0.5 tracking-tight">
                {data?.totalLecturesPublished ?? 0}
              </h3>
              <p className="text-[11px] text-slate-400 dark:text-slate-500 flex items-center gap-1 mt-0.5">
                Curated lectures
              </p>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Main Split Grid: Pending Submissions (Left) + Upcoming Sessions (Right) */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Pending Submissions Queue */}
        <Card className="border-slate-200/80 dark:border-slate-800/80 shadow-soft-sm overflow-hidden flex flex-col justify-between">
          <div>
            <CardHeader className="flex items-center justify-between p-5 border-b border-slate-100 dark:border-slate-800">
              <div>
                <CardTitle className="text-base font-bold flex items-center gap-2">
                  <FileText className="w-5 h-5 text-amber-500" />
                  Submissions Awaiting Grading
                </CardTitle>
                <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
                  Student coursework requiring academic evaluation
                </p>
              </div>
              <Link to="/faculty/assignments">
                <Button variant="outline" size="sm" className="text-xs">
                  View All
                </Button>
              </Link>
            </CardHeader>
            <CardContent className="p-0">
              {pendingSubmissions.length === 0 ? (
                <div className="py-12 px-4 text-center space-y-2">
                  <div className="w-10 h-10 rounded-2xl bg-emerald-50 dark:bg-emerald-950/60 text-emerald-600 dark:text-emerald-400 flex items-center justify-center mx-auto">
                    <CheckCircle2 className="w-5 h-5" />
                  </div>
                  <h4 className="text-sm font-semibold text-slate-800 dark:text-slate-200">
                    Grading Queue is Clear
                  </h4>
                  <p className="text-xs text-slate-400">
                    All submitted student assignments have been reviewed.
                  </p>
                </div>
              ) : (
                <div className="divide-y divide-slate-100 dark:divide-slate-800/80">
                  {pendingSubmissions.slice(0, 5).map((sub) => (
                    <div
                      key={sub.id}
                      className="p-4 flex items-center justify-between gap-3 hover:bg-slate-50/70 dark:hover:bg-slate-800/40 transition-colors"
                    >
                      <div className="min-w-0 space-y-0.5">
                        <p className="text-sm font-bold text-slate-900 dark:text-white truncate">
                          {sub.assignmentTitle}
                        </p>
                        <p className="text-xs text-slate-500 dark:text-slate-400 truncate">
                          Student: <span className="font-semibold text-slate-700 dark:text-slate-300">{sub.studentName}</span>
                          {' • '}
                          Submitted {new Date(sub.submittedAt).toLocaleDateString()}
                        </p>
                      </div>
                      <Link to="/faculty/assignments" className="shrink-0">
                        <Button
                          variant="primary"
                          size="sm"
                          className="bg-primary-600 hover:bg-primary-700 text-white text-xs shadow-soft-sm"
                        >
                          Grade Work
                        </Button>
                      </Link>
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </div>
          {pendingSubmissions.length > 5 && (
            <div className="p-3 bg-slate-50/50 dark:bg-slate-800/30 border-t border-slate-100 dark:border-slate-800 text-center">
              <Link to="/faculty/assignments" className="text-xs font-semibold text-primary-600 dark:text-primary-400 hover:underline">
                +{pendingSubmissions.length - 5} more pending assignments
              </Link>
            </div>
          )}
        </Card>

        {/* Upcoming Live Sessions */}
        <Card className="border-slate-200/80 dark:border-slate-800/80 shadow-soft-sm overflow-hidden flex flex-col justify-between">
          <div>
            <CardHeader className="flex items-center justify-between p-5 border-b border-slate-100 dark:border-slate-800">
              <div>
                <CardTitle className="text-base font-bold flex items-center gap-2">
                  <Radio className="w-5 h-5 text-rose-500" />
                  Upcoming Live Classes
                </CardTitle>
                <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
                  Scheduled virtual lectures and live Q&A broadcasts
                </p>
              </div>
              <Link to="/faculty/live-sessions">
                <Button variant="outline" size="sm" className="text-xs">
                  Schedule New
                </Button>
              </Link>
            </CardHeader>
            <CardContent className="p-0">
              {upcomingSessions.length === 0 ? (
                <div className="py-12 px-4 text-center space-y-2">
                  <div className="w-10 h-10 rounded-2xl bg-slate-100 dark:bg-slate-800 text-slate-400 flex items-center justify-center mx-auto">
                    <Calendar className="w-5 h-5" />
                  </div>
                  <h4 className="text-sm font-semibold text-slate-800 dark:text-slate-200">
                    No Live Classes Scheduled Today
                  </h4>
                  <p className="text-xs text-slate-400">
                    Create a virtual classroom session anytime for your students.
                  </p>
                </div>
              ) : (
                <div className="divide-y divide-slate-100 dark:divide-slate-800/80">
                  {upcomingSessions.slice(0, 5).map((sess) => (
                    <div
                      key={sess.id}
                      className="p-4 flex items-center justify-between gap-3 hover:bg-slate-50/70 dark:hover:bg-slate-800/40 transition-colors"
                    >
                      <div className="min-w-0 space-y-0.5">
                        <div className="flex items-center gap-2">
                          <p className="text-sm font-bold text-slate-900 dark:text-white truncate">
                            {sess.title}
                          </p>
                          <StatusBadge status={sess.status} size="sm" />
                        </div>
                        <p className="text-xs text-slate-500 dark:text-slate-400 truncate">
                          {sess.subjectTitle} • {new Date(sess.scheduledStart).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                        </p>
                      </div>
                      <a
                        href={sess.meetingLink}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="shrink-0"
                      >
                        <Button variant="primary" size="sm" className="bg-rose-600 hover:bg-rose-700 text-white text-xs shadow-soft-sm">
                          <ExternalLink className="w-3.5 h-3.5 mr-1" /> Start
                        </Button>
                      </a>
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </div>
          {upcomingSessions.length > 5 && (
            <div className="p-3 bg-slate-50/50 dark:bg-slate-800/30 border-t border-slate-100 dark:border-slate-800 text-center">
              <Link to="/faculty/live-sessions" className="text-xs font-semibold text-primary-600 dark:text-primary-400 hover:underline">
                View all scheduled sessions
              </Link>
            </div>
          )}
        </Card>
      </div>

      {/* Teaching Subjects Grid */}
      <Card className="border-slate-200/80 dark:border-slate-800/80 shadow-soft-sm">
        <CardHeader className="flex justify-between items-center p-5 border-b border-slate-100 dark:border-slate-800">
          <div>
            <CardTitle className="text-base font-bold flex items-center gap-2">
              <Library className="w-5 h-5 text-primary-600 dark:text-primary-400" />
              Assigned Subjects & Rosters ({assignedSubjects.length})
            </CardTitle>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
              Course units allocated to your instructional portfolio
            </p>
          </div>
          <Link to="/faculty/subjects">
            <Button variant="outline" size="sm" className="text-xs">
              View All Rosters <ArrowRight className="w-3.5 h-3.5 ml-1" />
            </Button>
          </Link>
        </CardHeader>
        <CardContent className="p-4 sm:p-5">
          {assignedSubjects.length === 0 ? (
            <div className="py-8 text-center text-xs text-slate-400">
              No subjects have been assigned to you yet by the platform administrator.
            </div>
          ) : (
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3.5">
              {assignedSubjects.map((sub) => (
                <div
                  key={sub.id}
                  className="p-4 rounded-2xl border border-slate-200/80 dark:border-slate-800/80 bg-slate-50/50 dark:bg-slate-800/30 hover:border-primary-500/50 hover:bg-white dark:hover:bg-slate-800/70 transition-all flex flex-col justify-between shadow-soft-sm group"
                >
                  <div className="space-y-1.5">
                    <div className="flex items-center justify-between">
                      <span className="font-mono text-xs font-bold text-primary-600 dark:text-primary-400 bg-primary-50 dark:bg-primary-950/60 px-2.5 py-0.5 rounded-lg">
                        {sub.code}
                      </span>
                      <span className="text-[11px] font-semibold text-slate-400">Year {sub.yearLevel}</span>
                    </div>
                    <p className="text-sm font-bold text-slate-900 dark:text-white line-clamp-1">
                      {sub.title}
                    </p>
                    <p className="text-xs text-slate-500 dark:text-slate-400">
                      {sub.courseName || 'Core Curriculum'}
                    </p>
                  </div>
                  <div className="mt-4 pt-3 border-t border-slate-200/60 dark:border-slate-700/60 flex items-center justify-between">
                    <span className="text-xs text-slate-500 flex items-center gap-1">
                      <Users className="w-3.5 h-3.5" /> Enrolled Students
                    </span>
                    <Link to="/faculty/subjects">
                      <Button variant="ghost" size="sm" className="text-xs text-primary-600 dark:text-primary-400 p-0 hover:bg-transparent hover:underline">
                        Roster &rarr;
                      </Button>
                    </Link>
                  </div>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
};
