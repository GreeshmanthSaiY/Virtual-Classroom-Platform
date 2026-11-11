import React, { useState, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import { apiClient } from '../../api/client';
import { ApiResponse, Subject, StudyMaterial, VideoLecture } from '../../types';
import { Sidebar } from '../../components/common/Sidebar';
import { Badge } from '../../components/common/Badge';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';
import { Link } from 'react-router-dom';
import {
  GraduationCap,
  BookOpen,
  Video,
  ArrowRight,
  FileText,
  Clock,
  Sparkles,
} from 'lucide-react';

export const StudentDashboard: React.FC = () => {
  const { user } = useAuth();
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [materials, setMaterials] = useState<StudyMaterial[]>([]);
  const [lectures, setLectures] = useState<VideoLecture[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchStudentData = async () => {
      setLoading(true);
      try {
        const [subRes, matRes, lecRes] = await Promise.all([
          apiClient.get<ApiResponse<Subject[]>>('/students/subjects'),
          apiClient.get<ApiResponse<StudyMaterial[]>>('/students/materials'),
          apiClient.get<ApiResponse<VideoLecture[]>>('/students/lectures'),
        ]);

        if (subRes.data.success) setSubjects(subRes.data.data);
        if (matRes.data.success) setMaterials(matRes.data.data);
        if (lecRes.data.success) setLectures(lecRes.data.data);
      } catch (err) {
        console.error('Failed to fetch student curriculum data', err);
      } finally {
        setLoading(false);
      }
    };

    fetchStudentData();
  }, []);

  return (
    <div className="flex min-h-[calc(100vh-4rem)]">
      <Sidebar />

      <main className="flex-1 p-6 lg:p-8 space-y-8 overflow-y-auto">
        {loading ? (
          <div className="py-20 flex justify-center">
            <LoadingSpinner size="lg" label="Loading curriculum portal..." />
          </div>
        ) : (
          <>
            {/* Student Banner */}
            <div className="p-6 rounded-2xl bg-gradient-to-r from-primary-900 via-primary-800 to-indigo-900 text-white shadow-md space-y-2">
          <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-primary-800/80 text-[11px] font-semibold text-primary-200">
            <Sparkles className="w-3.5 h-3.5 text-amber-300" />
            Verified Student Scholar
          </div>
          <h1 className="text-2xl font-bold">Welcome back, {user?.fullName}!</h1>
          <p className="text-xs text-primary-200">
            Roll Number: <span className="font-mono text-white font-semibold">{user?.rollNumber || 'CS-2025-01'}</span> • Program: {user?.courseName || 'B.Tech Computer Science'} (Year {user?.currentYear || 1})
          </p>
        </div>

        {/* Quick Stats Grid */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div className="p-5 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-sm flex items-center gap-4">
            <div className="p-3 rounded-xl bg-primary-50 dark:bg-primary-950/60 text-primary-600 dark:text-primary-400">
              <BookOpen className="w-6 h-6" />
            </div>
            <div>
              <p className="text-2xl font-bold text-slate-900 dark:text-white">{subjects.length}</p>
              <p className="text-xs text-slate-500">Enrolled Subjects</p>
            </div>
          </div>

          <div className="p-5 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-sm flex items-center gap-4">
            <div className="p-3 rounded-xl bg-indigo-50 dark:bg-indigo-950/60 text-indigo-600 dark:text-indigo-400">
              <FileText className="w-6 h-6" />
            </div>
            <div>
              <p className="text-2xl font-bold text-slate-900 dark:text-white">{materials.length}</p>
              <p className="text-xs text-slate-500">Course Materials</p>
            </div>
          </div>

          <div className="p-5 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-sm flex items-center gap-4">
            <div className="p-3 rounded-xl bg-emerald-50 dark:bg-emerald-950/60 text-emerald-600 dark:text-emerald-400">
              <Video className="w-6 h-6" />
            </div>
            <div>
              <p className="text-2xl font-bold text-slate-900 dark:text-white">{lectures.length}</p>
              <p className="text-xs text-slate-500">Video Lectures</p>
            </div>
          </div>
        </div>

        {/* Academic Subjects List */}
        <div className="space-y-4">
          <div className="flex items-center justify-between">
            <h2 className="text-base font-bold text-slate-900 dark:text-white flex items-center gap-2">
              <GraduationCap className="w-5 h-5 text-primary-600" />
              Your Enrolled Curriculum
            </h2>
            <Link to="/forum" className="text-xs font-semibold text-primary-600 hover:text-primary-700 flex items-center gap-1">
              Ask in Q&A Forum <ArrowRight className="w-3.5 h-3.5" />
            </Link>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
            {subjects.length === 0 ? (
              <div className="p-4 bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-800 text-xs text-slate-500">
                Loading enrolled subjects...
              </div>
            ) : (
              subjects.map((s) => (
                <div
                  key={s.id}
                  className="p-5 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-sm space-y-3"
                >
                  <div className="flex items-center justify-between">
                    <Badge variant="primary">{s.code}</Badge>
                    <span className="text-[11px] font-semibold text-slate-400">Year {s.yearLevel}</span>
                  </div>
                  <h3 className="font-bold text-slate-900 dark:text-white text-sm">{s.title}</h3>
                  <div className="pt-2 border-t border-slate-100 dark:border-slate-800 flex items-center justify-between text-xs">
                    <Link
                      to={`/materials?subject=${s.id}`}
                      className="text-primary-600 hover:text-primary-700 font-medium"
                    >
                      Materials
                    </Link>
                    <span className="text-slate-300 dark:text-slate-700">•</span>
                    <Link
                      to={`/lectures?subject=${s.id}`}
                      className="text-indigo-600 hover:text-indigo-700 font-medium"
                    >
                      Lectures
                    </Link>
                    <span className="text-slate-300 dark:text-slate-700">•</span>
                    <Link
                      to={`/forum?subject=${s.id}`}
                      className="text-emerald-600 hover:text-emerald-700 font-medium"
                    >
                      Forum
                    </Link>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>

        {/* Recent Course Materials & Lectures Section */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 p-6 space-y-4 shadow-sm">
            <div className="flex items-center justify-between">
              <h3 className="font-bold text-sm text-slate-900 dark:text-white flex items-center gap-2">
                <BookOpen className="w-4 h-4 text-primary-600" />
                Latest Study Notes
              </h3>
              <Link to="/materials" className="text-xs font-semibold text-primary-600">View All</Link>
            </div>
            <div className="space-y-2.5">
              {materials.slice(0, 4).map((m) => (
                <div key={m.id} className="p-3 rounded-xl bg-slate-50 dark:bg-slate-800/50 flex items-center justify-between text-xs">
                  <div className="flex items-center gap-3">
                    <FileText className="w-4 h-4 text-primary-500" />
                    <div>
                      <p className="font-semibold text-slate-900 dark:text-white">{m.title}</p>
                      <p className="text-slate-400">{m.subjectTitle} • by {m.uploaderName}</p>
                    </div>
                  </div>
                  <a
                    href={m.fileUrl}
                    target="_blank"
                    rel="noreferrer"
                    className="px-2.5 py-1 rounded-lg bg-primary-50 text-primary-700 dark:bg-primary-950 dark:text-primary-300 font-medium"
                  >
                    View
                  </a>
                </div>
              ))}
            </div>
          </div>

          <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 p-6 space-y-4 shadow-sm">
            <div className="flex items-center justify-between">
              <h3 className="font-bold text-sm text-slate-900 dark:text-white flex items-center gap-2">
                <Video className="w-4 h-4 text-indigo-600" />
                Latest Video Lectures
              </h3>
              <Link to="/lectures" className="text-xs font-semibold text-indigo-600">View All</Link>
            </div>
            <div className="space-y-2.5">
              {lectures.slice(0, 4).map((l) => (
                <div key={l.id} className="p-3 rounded-xl bg-slate-50 dark:bg-slate-800/50 flex items-center justify-between text-xs">
                  <div className="flex items-center gap-3">
                    <Video className="w-4 h-4 text-indigo-500" />
                    <div>
                      <p className="font-semibold text-slate-900 dark:text-white">{l.title}</p>
                      <p className="text-slate-400">{l.subjectTitle} • by {l.facultyName}</p>
                    </div>
                  </div>
                  <div className="flex items-center gap-1 text-[11px] text-slate-400">
                    <Clock className="w-3.5 h-3.5" />
                    <span>{Math.floor(l.durationSeconds / 60)} min</span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
        </>
      )}
      </main>
    </div>
  );
};
