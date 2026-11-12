import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { studentService } from '@/core/services/studentService';
import { StudentDashboardData } from '@/types';
import {
  BookOpen,
  FileCheck,
  Award,
  Radio,
  Clock,
  ExternalLink,
  ArrowRight,
  BookMarked,
} from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '@/shared/components/Card';
import { Button } from '@/shared/components/Button';
import { StatusBadge } from '@/shared/components/Badge';
import { LoadingSpinner } from '@/shared/components/LoadingSpinner';

export const StudentDashboard: React.FC = () => {
  const [data, setData] = useState<StudentDashboardData | null>(null);
  const [loading, setLoading] = useState(true);

  const loadData = async () => {
    setLoading(true);
    try {
      const res = await studentService.getDashboard();
      setData(res);
    } catch (err) {
      console.error('Failed to load student dashboard', err);
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
        <LoadingSpinner size="lg" label="Loading student academic portal..." />
      </div>
    );
  }

  const enrolledSubjects = data?.enrolledSubjects || [];
  const upcomingSessions = data?.upcomingLiveSessions || [];
  const pendingAssignments = data?.pendingAssignments || [];
  const recentExamResults = data?.recentExamResults || [];

  return (
    <div className="space-y-6">
      {/* Student Banner */}
      <div className="bg-gradient-to-r from-emerald-900 to-slate-900 rounded-2xl p-6 sm:p-8 text-white shadow-lg flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <span className="px-2.5 py-0.5 rounded-full bg-emerald-500/20 text-emerald-300 border border-emerald-500/30 text-xs font-semibold uppercase tracking-wider">
            Student Academic Portal
          </span>
          <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight mt-2">
            Welcome to Your Virtual Classroom
          </h1>
          <p className="text-xs sm:text-sm text-emerald-200 mt-1">
            Program: <span className="font-semibold">{data?.course?.name || 'Academic Degree Program'}</span>
            {data?.course?.code ? ` (${data.course.code})` : ''}
          </p>
        </div>
        <div className="flex gap-2">
          <Link to="/student/assignments">
            <Button variant="primary" size="sm" className="bg-emerald-600 hover:bg-emerald-500 text-white">
              <FileCheck className="w-3.5 h-3.5 mr-1.5" /> Submit Work
            </Button>
          </Link>
          <Link to="/student/live-sessions">
            <Button variant="outline" size="sm" className="bg-white/10 hover:bg-white/20 text-white border-white/20">
              <Radio className="w-3.5 h-3.5 mr-1.5" /> Join Live
            </Button>
          </Link>
        </div>
      </div>

      {/* Metrics Row */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <Card hover>
          <CardContent className="p-5 flex items-center gap-4">
            <div className="p-3 bg-emerald-50 dark:bg-emerald-950/50 rounded-xl text-emerald-600 dark:text-emerald-400">
              <BookMarked className="w-6 h-6" />
            </div>
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Enrolled Subjects</p>
              <h3 className="text-2xl font-bold text-slate-900 dark:text-white mt-0.5">
                {enrolledSubjects.length}
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
              <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Due Assignments</p>
              <h3 className="text-2xl font-bold text-amber-600 dark:text-amber-400 mt-0.5">
                {pendingAssignments.length}
              </h3>
            </div>
          </CardContent>
        </Card>

        <Card hover>
          <CardContent className="p-5 flex items-center gap-4">
            <div className="p-3 bg-indigo-50 dark:bg-indigo-950/50 rounded-xl text-indigo-600 dark:text-indigo-400">
              <Radio className="w-6 h-6" />
            </div>
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Live Classes</p>
              <h3 className="text-2xl font-bold text-slate-900 dark:text-white mt-0.5">
                {upcomingSessions.length}
              </h3>
            </div>
          </CardContent>
        </Card>

        <Card hover>
          <CardContent className="p-5 flex items-center gap-4">
            <div className="p-3 bg-purple-50 dark:bg-purple-950/50 rounded-xl text-purple-600 dark:text-purple-400">
              <Award className="w-6 h-6" />
            </div>
            <div>
              <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Published Grades</p>
              <h3 className="text-2xl font-bold text-slate-900 dark:text-white mt-0.5">
                {recentExamResults.length}
              </h3>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Main Split Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Pending Assignments */}
        <Card>
          <CardHeader className="flex items-center justify-between">
            <div>
              <CardTitle>Coursework Due</CardTitle>
              <p className="text-xs text-slate-500 mt-0.5">Assignments requiring submission</p>
            </div>
            <Link to="/student/assignments">
              <Button variant="outline" size="sm">
                View All
              </Button>
            </Link>
          </CardHeader>
          <CardContent className="p-0">
            {pendingAssignments.length === 0 ? (
              <div className="p-8 text-center text-xs text-slate-400">
                You have no pending coursework assignments!
              </div>
            ) : (
              <div className="divide-y divide-slate-100 dark:divide-slate-800">
                {pendingAssignments.slice(0, 5).map((asg) => (
                  <div key={asg.id} className="p-4 flex items-center justify-between hover:bg-slate-50/50 transition-colors">
                    <div>
                      <p className="text-sm font-semibold text-slate-900 dark:text-white">{asg.title}</p>
                      <p className="text-xs text-slate-500 mt-0.5">
                        Due: {new Date(asg.dueDate).toLocaleDateString()} • {asg.maxPoints} pts
                      </p>
                    </div>
                    <Link to="/student/assignments">
                      <Button variant="outline" size="sm" className="text-emerald-600">
                        Submit Solution
                      </Button>
                    </Link>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>

        {/* Live Video Sessions */}
        <Card>
          <CardHeader className="flex items-center justify-between">
            <div>
              <CardTitle>Live Virtual Classrooms</CardTitle>
              <p className="text-xs text-slate-500 mt-0.5">Upcoming scheduled lectures and webinars</p>
            </div>
            <Link to="/student/live-sessions">
              <Button variant="outline" size="sm">
                View All
              </Button>
            </Link>
          </CardHeader>
          <CardContent className="p-0">
            {upcomingSessions.length === 0 ? (
              <div className="p-8 text-center text-xs text-slate-400">
                No live sessions scheduled right now.
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
                      <Button variant="primary" size="sm" className="bg-emerald-600 hover:bg-emerald-500">
                        <ExternalLink className="w-3.5 h-3.5 mr-1" /> Join Meeting
                      </Button>
                    </a>
                  </div>
                ))}
              </div>
            )}
          </CardContent>
        </Card>
      </div>

      {/* Enrolled Subjects Cards */}
      <Card>
        <CardHeader className="flex justify-between items-center">
          <CardTitle>My Enrolled Subjects ({enrolledSubjects.length})</CardTitle>
          <Link to="/student/subjects">
            <Button variant="outline" size="sm">
              Explore All Subjects
              <ArrowRight className="w-3.5 h-3.5 ml-1" />
            </Button>
          </Link>
        </CardHeader>
        <CardContent className="p-4 grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
          {enrolledSubjects.map((sub) => (
            <div
              key={sub.id}
              className="p-3.5 rounded-xl border border-slate-200 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-800/40 flex items-center justify-between"
            >
              <div>
                <span className="font-mono text-xs font-bold text-emerald-600 dark:text-emerald-400">{sub.code}</span>
                <p className="text-sm font-semibold text-slate-900 dark:text-white mt-0.5">{sub.title}</p>
                <span className="text-[11px] text-slate-500">Year {sub.yearLevel} Level</span>
              </div>
              <Link to="/materials">
                <Button variant="outline" size="sm" className="text-xs">
                  <BookOpen className="w-3 h-3 mr-1" /> Materials
                </Button>
              </Link>
            </div>
          ))}
        </CardContent>
      </Card>
    </div>
  );
};
