import React, { useState, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import { apiClient } from '../../api/client';
import { ApiResponse, Subject, StudyMaterial, VideoLecture } from '../../types';
import { Sidebar } from '../../components/common/Sidebar';
import { Badge } from '../../components/common/Badge';
import { Modal } from '../../components/common/Modal';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';
import {
  BookOpen,
  Video,
  Upload,
  FileText,
  Clock,
  Sparkles,
} from 'lucide-react';

export const FacultyDashboard: React.FC = () => {
  const { user } = useAuth();
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [materials, setMaterials] = useState<StudyMaterial[]>([]);
  const [lectures, setLectures] = useState<VideoLecture[]>([]);
  const [loading, setLoading] = useState(true);

  // Modals
  const [uploadModalOpen, setUploadModalOpen] = useState(false);
  const [lectureModalOpen, setLectureModalOpen] = useState(false);

  // Upload Form State
  const [newMaterial, setNewMaterial] = useState({
    subjectId: 1,
    title: '',
    description: '',
    fileUrl: '',
    fileSizeBytes: 2048576,
    mimeType: 'application/pdf',
  });

  // Lecture Form State
  const [newLecture, setNewLecture] = useState({
    subjectId: 1,
    title: '',
    description: '',
    videoUrl: '',
    durationSeconds: 2400,
  });

  const loadFacultyData = async () => {
    setLoading(true);
    try {
      const [subRes, matRes, lecRes] = await Promise.all([
        apiClient.get<ApiResponse<Subject[]>>('/faculty/subjects'),
        apiClient.get<ApiResponse<StudyMaterial[]>>('/faculty/materials'),
        apiClient.get<ApiResponse<VideoLecture[]>>('/faculty/lectures'),
      ]);

      if (subRes.data.success) setSubjects(subRes.data.data);
      if (matRes.data.success) setMaterials(matRes.data.data);
      if (lecRes.data.success) setLectures(lecRes.data.data);
    } catch (e) {
      console.error('Failed to load faculty data', e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadFacultyData();
  }, []);

  const handleCreateMaterial = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await apiClient.post('/faculty/materials', newMaterial);
      setUploadModalOpen(false);
      setNewMaterial({
        subjectId: subjects[0]?.id || 1,
        title: '',
        description: '',
        fileUrl: '',
        fileSizeBytes: 2048576,
        mimeType: 'application/pdf',
      });
      await loadFacultyData();
    } catch (err) {
      console.error('Failed to create material', err);
    }
  };

  const handleCreateLecture = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await apiClient.post('/faculty/lectures', newLecture);
      setLectureModalOpen(false);
      setNewLecture({
        subjectId: subjects[0]?.id || 1,
        title: '',
        description: '',
        videoUrl: '',
        durationSeconds: 2400,
      });
      await loadFacultyData();
    } catch (err) {
      console.error('Failed to create lecture', err);
    }
  };

  return (
    <div className="flex min-h-[calc(100vh-4rem)]">
      <Sidebar />

      <main className="flex-1 p-6 lg:p-8 space-y-8 overflow-y-auto">
        {loading ? (
          <div className="py-20 flex justify-center">
            <LoadingSpinner size="lg" label="Loading faculty portal..." />
          </div>
        ) : (
          <>
            {/* Welcome Header */}
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 p-6 rounded-2xl bg-gradient-to-r from-indigo-900 to-slate-900 text-white shadow-md">
          <div className="space-y-1">
            <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-indigo-800/80 text-[11px] font-semibold text-indigo-200">
              <Sparkles className="w-3.5 h-3.5 text-amber-300" />
              Verified Faculty Portal
            </div>
            <h1 className="text-2xl font-bold">Welcome back, {user?.fullName}</h1>
            <p className="text-xs text-indigo-200">
              Department: {user?.department || 'Computer Science & Engineering'} • Employee ID: {user?.employeeId || 'FAC-001'}
            </p>
          </div>

          <div className="flex items-center gap-3">
            <button
              onClick={() => setUploadModalOpen(true)}
              className="px-4 py-2.5 rounded-xl font-semibold text-xs bg-white text-indigo-950 hover:bg-indigo-50 transition-all flex items-center gap-2 shadow-sm"
            >
              <Upload className="w-4 h-4" />
              Upload Material
            </button>
            <button
              onClick={() => setLectureModalOpen(true)}
              className="px-4 py-2.5 rounded-xl font-semibold text-xs bg-primary-600 hover:bg-primary-500 text-white transition-all flex items-center gap-2 shadow-sm"
            >
              <Video className="w-4 h-4" />
              Publish Lecture
            </button>
          </div>
        </div>

        {/* Assigned Subjects */}
        <div className="space-y-3">
          <h2 className="text-sm font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">
            Teaching Responsibilities
          </h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            {subjects.length === 0 ? (
              <div className="p-4 bg-white dark:bg-slate-900 rounded-xl border border-slate-200 dark:border-slate-800 text-xs text-slate-500">
                No active subjects assigned yet.
              </div>
            ) : (
              subjects.map((sub) => (
                <div
                  key={sub.id}
                  className="p-4 rounded-xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-sm space-y-2"
                >
                  <div className="flex items-center justify-between">
                    <Badge variant="primary">{sub.code}</Badge>
                    <span className="text-[11px] text-slate-400">Yr {sub.yearLevel}</span>
                  </div>
                  <h3 className="font-bold text-sm text-slate-900 dark:text-white">{sub.title}</h3>
                  <p className="text-xs text-slate-500">{sub.courseName || 'Computer Science'}</p>
                </div>
              ))
            )}
          </div>
        </div>

        {/* Uploaded Materials & Lectures Grid */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Materials Section */}
          <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 p-6 space-y-4 shadow-sm">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <BookOpen className="w-5 h-5 text-primary-600" />
                <h3 className="font-bold text-slate-900 dark:text-white">Uploaded Materials</h3>
              </div>
              <Badge variant="neutral">{materials.length} files</Badge>
            </div>

            {materials.length === 0 ? (
              <p className="text-xs text-slate-500 py-6 text-center italic">No study materials published yet.</p>
            ) : (
              <div className="space-y-2.5">
                {materials.slice(0, 5).map((m) => (
                  <div
                    key={m.id}
                    className="p-3 rounded-xl bg-slate-50 dark:bg-slate-800/50 flex items-center justify-between text-xs"
                  >
                    <div className="flex items-center gap-3">
                      <FileText className="w-4 h-4 text-primary-500" />
                      <div>
                        <p className="font-semibold text-slate-900 dark:text-white">{m.title}</p>
                        <p className="text-slate-400">{m.subjectTitle}</p>
                      </div>
                    </div>
                    <span className="text-[10px] text-slate-400">
                      {(m.fileSizeBytes / (1024 * 1024)).toFixed(1)} MB
                    </span>
                  </div>
                ))}
              </div>
            )}
          </div>

          {/* Lectures Section */}
          <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 p-6 space-y-4 shadow-sm">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Video className="w-5 h-5 text-indigo-600" />
                <h3 className="font-bold text-slate-900 dark:text-white">Published Lectures</h3>
              </div>
              <Badge variant="neutral">{lectures.length} videos</Badge>
            </div>

            {lectures.length === 0 ? (
              <p className="text-xs text-slate-500 py-6 text-center italic">No video lectures recorded yet.</p>
            ) : (
              <div className="space-y-2.5">
                {lectures.slice(0, 5).map((l) => (
                  <div
                    key={l.id}
                    className="p-3 rounded-xl bg-slate-50 dark:bg-slate-800/50 flex items-center justify-between text-xs"
                  >
                    <div className="flex items-center gap-3">
                      <Video className="w-4 h-4 text-indigo-500" />
                      <div>
                        <p className="font-semibold text-slate-900 dark:text-white">{l.title}</p>
                        <p className="text-slate-400">{l.subjectTitle}</p>
                      </div>
                    </div>
                    <div className="flex items-center gap-1 text-[11px] text-slate-400">
                      <Clock className="w-3.5 h-3.5" />
                      <span>{Math.floor(l.durationSeconds / 60)} min</span>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
        </>
      )}
      </main>

      {/* Upload Material Modal */}
      <Modal
        isOpen={uploadModalOpen}
        onClose={() => setUploadModalOpen(false)}
        title="Publish Course Material"
      >
        <form onSubmit={handleCreateMaterial} className="space-y-4 text-xs">
          <div>
            <label className="block font-semibold uppercase tracking-wider text-slate-700 dark:text-slate-300 mb-1">
              Subject
            </label>
            <select
              value={newMaterial.subjectId}
              onChange={(e) => setNewMaterial({ ...newMaterial, subjectId: Number(e.target.value) })}
              className="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white"
            >
              {subjects.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.code} - {s.title}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block font-semibold uppercase tracking-wider text-slate-700 dark:text-slate-300 mb-1">
              Document Title
            </label>
            <input
              type="text"
              required
              value={newMaterial.title}
              onChange={(e) => setNewMaterial({ ...newMaterial, title: e.target.value })}
              placeholder="e.g. Unit 3 - AVL Trees & Graph Traversal Notes"
              className="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white"
            />
          </div>

          <div>
            <label className="block font-semibold uppercase tracking-wider text-slate-700 dark:text-slate-300 mb-1">
              File Storage URL / Path
            </label>
            <input
              type="text"
              required
              value={newMaterial.fileUrl}
              onChange={(e) => setNewMaterial({ ...newMaterial, fileUrl: e.target.value })}
              placeholder="e.g. https://storage.supabase.co/materials/cs101_unit3.pdf"
              className="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white"
            />
          </div>

          <div>
            <label className="block font-semibold uppercase tracking-wider text-slate-700 dark:text-slate-300 mb-1">
              Description
            </label>
            <textarea
              rows={3}
              value={newMaterial.description}
              onChange={(e) => setNewMaterial({ ...newMaterial, description: e.target.value })}
              placeholder="Key concepts covered, reading references, and exercise questions..."
              className="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white"
            />
          </div>

          <div className="flex justify-end gap-2 pt-2">
            <button
              type="button"
              onClick={() => setUploadModalOpen(false)}
              className="px-4 py-2 rounded-xl text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="px-4 py-2 rounded-xl font-semibold text-white bg-primary-600 hover:bg-primary-700"
            >
              Save & Publish
            </button>
          </div>
        </form>
      </Modal>

      {/* Publish Lecture Modal */}
      <Modal
        isOpen={lectureModalOpen}
        onClose={() => setLectureModalOpen(false)}
        title="Publish Recorded Video Lecture"
      >
        <form onSubmit={handleCreateLecture} className="space-y-4 text-xs">
          <div>
            <label className="block font-semibold uppercase tracking-wider text-slate-700 dark:text-slate-300 mb-1">
              Subject
            </label>
            <select
              value={newLecture.subjectId}
              onChange={(e) => setNewLecture({ ...newLecture, subjectId: Number(e.target.value) })}
              className="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white"
            >
              {subjects.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.code} - {s.title}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="block font-semibold uppercase tracking-wider text-slate-700 dark:text-slate-300 mb-1">
              Lecture Title
            </label>
            <input
              type="text"
              required
              value={newLecture.title}
              onChange={(e) => setNewLecture({ ...newLecture, title: e.target.value })}
              placeholder="e.g. Session 14 - Normalization & BCNF Proofs"
              className="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white"
            />
          </div>

          <div>
            <label className="block font-semibold uppercase tracking-wider text-slate-700 dark:text-slate-300 mb-1">
              Stream / Video URL
            </label>
            <input
              type="text"
              required
              value={newLecture.videoUrl}
              onChange={(e) => setNewLecture({ ...newLecture, videoUrl: e.target.value })}
              placeholder="e.g. https://www.youtube.com/watch?v=dQw4w9WgXcQ"
              className="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white"
            />
          </div>

          <div className="flex justify-end gap-2 pt-2">
            <button
              type="button"
              onClick={() => setLectureModalOpen(false)}
              className="px-4 py-2 rounded-xl text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="px-4 py-2 rounded-xl font-semibold text-white bg-indigo-600 hover:bg-indigo-700"
            >
              Publish Video
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
};
