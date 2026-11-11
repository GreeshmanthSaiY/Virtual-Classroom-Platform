import React, { useState, useEffect } from 'react';
import { apiClient } from '../../api/client';
import { ApiResponse, PlatformMetrics, User } from '../../types';
import { Sidebar } from '../../components/common/Sidebar';
import { Badge } from '../../components/common/Badge';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';
import { Modal } from '../../components/common/Modal';
import {
  Users,
  GraduationCap,
  Clock,
  BookOpen,
  Check,
  X,
  AlertTriangle,
  RefreshCw,
} from 'lucide-react';

export const AdminDashboard: React.FC = () => {
  const [metrics, setMetrics] = useState<PlatformMetrics | null>(null);
  const [pendingStudents, setPendingStudents] = useState<User[]>([]);
  const [pendingFaculty, setPendingFaculty] = useState<User[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [actionLoading, setActionLoading] = useState<boolean>(false);
  const [rejectModalOpen, setRejectModalOpen] = useState<boolean>(false);
  const [selectedUserToReject, setSelectedUserToReject] = useState<User | null>(null);
  const [rejectReason, setRejectReason] = useState<string>('');

  const loadData = async () => {
    setLoading(true);
    try {
      const [mRes, sRes, fRes] = await Promise.all([
        apiClient.get<ApiResponse<PlatformMetrics>>('/admin/metrics'),
        apiClient.get<ApiResponse<User[]>>('/admin/students/pending'),
        apiClient.get<ApiResponse<User[]>>('/admin/faculty/pending'),
      ]);

      if (mRes.data.success) setMetrics(mRes.data.data);
      if (sRes.data.success) setPendingStudents(sRes.data.data);
      if (fRes.data.success) setPendingFaculty(fRes.data.data);
    } catch (e) {
      console.error('Failed to load admin telemetry', e);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleApprove = async (userId: number) => {
    setActionLoading(true);
    try {
      await apiClient.post('/admin/approvals', { userIds: [userId] });
      await loadData();
    } catch (err) {
      console.error('Approval failed', err);
    } finally {
      setActionLoading(false);
    }
  };

  const handleOpenReject = (user: User) => {
    setSelectedUserToReject(user);
    setRejectReason('Academic criteria or identification documents not verified.');
    setRejectModalOpen(true);
  };

  const handleConfirmReject = async () => {
    if (!selectedUserToReject) return;
    setActionLoading(true);
    try {
      await apiClient.post('/admin/rejections', {
        userIds: [selectedUserToReject.id],
        reason: rejectReason,
      });
      setRejectModalOpen(false);
      setSelectedUserToReject(null);
      await loadData();
    } catch (err) {
      console.error('Rejection failed', err);
    } finally {
      setActionLoading(false);
    }
  };

  return (
    <div className="flex min-h-[calc(100vh-4rem)]">
      <Sidebar />

      <main className="flex-1 p-6 lg:p-8 space-y-8 overflow-y-auto">
        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div>
            <h1 className="text-2xl font-bold text-slate-900 dark:text-white">
              Administrator Governance Console
            </h1>
            <p className="text-sm text-slate-500 dark:text-slate-400">
              Institutional security overview, verification queue, and academic management
            </p>
          </div>
          <button
            onClick={loadData}
            disabled={loading}
            className="inline-flex items-center gap-2 px-4 py-2 text-sm font-medium rounded-xl border border-slate-200 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-700 dark:text-slate-200 hover:bg-slate-50 dark:hover:bg-slate-700/50 transition-colors shadow-sm"
          >
            <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
            Refresh Telemetry
          </button>
        </div>

        {/* Metrics Cards */}
        {loading && !metrics ? (
          <div className="py-12">
            <LoadingSpinner size="lg" label="Loading platform telemetry..." />
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            <div className="p-5 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-sm space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">Total Students</span>
                <div className="p-2 rounded-lg bg-emerald-50 dark:bg-emerald-950/60 text-emerald-600 dark:text-emerald-400">
                  <GraduationCap className="w-5 h-5" />
                </div>
              </div>
              <p className="text-3xl font-extrabold text-slate-900 dark:text-white">{metrics?.totalStudents || 0}</p>
              <p className="text-xs text-slate-500">Approved enrolled scholars</p>
            </div>

            <div className="p-5 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-sm space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">Faculty Members</span>
                <div className="p-2 rounded-lg bg-indigo-50 dark:bg-indigo-950/60 text-indigo-600 dark:text-indigo-400">
                  <Users className="w-5 h-5" />
                </div>
              </div>
              <p className="text-3xl font-extrabold text-slate-900 dark:text-white">{metrics?.totalFaculty || 0}</p>
              <p className="text-xs text-slate-500">Verified instructors</p>
            </div>

            <div className="p-5 rounded-2xl bg-white dark:bg-slate-900 border border-amber-200 dark:border-amber-900/60 bg-amber-50/20 dark:bg-amber-950/20 shadow-sm space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold uppercase tracking-wider text-amber-700 dark:text-amber-400">Pending Approvals</span>
                <div className="p-2 rounded-lg bg-amber-100 dark:bg-amber-950/80 text-amber-700 dark:text-amber-400">
                  <Clock className="w-5 h-5" />
                </div>
              </div>
              <p className="text-3xl font-extrabold text-amber-900 dark:text-amber-200">
                {(metrics?.pendingStudentApprovals || 0) + (metrics?.pendingFacultyApprovals || 0)}
              </p>
              <p className="text-xs text-amber-700/80 dark:text-amber-400/80">
                {metrics?.pendingStudentApprovals || 0} students, {metrics?.pendingFacultyApprovals || 0} faculty
              </p>
            </div>

            <div className="p-5 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-sm space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-xs font-semibold uppercase tracking-wider text-slate-500 dark:text-slate-400">Academic Artifacts</span>
                <div className="p-2 rounded-lg bg-sky-50 dark:bg-sky-950/60 text-sky-600 dark:text-sky-400">
                  <BookOpen className="w-5 h-5" />
                </div>
              </div>
              <p className="text-3xl font-extrabold text-slate-900 dark:text-white">
                {(metrics?.totalMaterials || 0) + (metrics?.totalLectures || 0)}
              </p>
              <p className="text-xs text-slate-500">
                {metrics?.totalMaterials || 0} materials, {metrics?.totalLectures || 0} videos
              </p>
            </div>
          </div>
        )}

        {/* Pending Student Registrations Queue */}
        <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 p-6 space-y-4 shadow-sm">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <GraduationCap className="w-5 h-5 text-primary-600" />
              <h2 className="text-lg font-bold text-slate-900 dark:text-white">
                Student Verification Queue
              </h2>
            </div>
            <Badge variant="warning">{pendingStudents.length} Awaiting Review</Badge>
          </div>

          {pendingStudents.length === 0 ? (
            <p className="text-xs text-slate-500 dark:text-slate-400 py-6 text-center italic">
              All student enrollment requests have been cleared.
            </p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead className="bg-slate-50 dark:bg-slate-800/60 text-slate-500 dark:text-slate-400 uppercase font-semibold">
                  <tr>
                    <th className="p-3 rounded-l-lg">Student Name</th>
                    <th className="p-3">Roll Number</th>
                    <th className="p-3">Academic Email</th>
                    <th className="p-3">Course / Year</th>
                    <th className="p-3 rounded-r-lg text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                  {pendingStudents.map((st) => (
                    <tr key={st.id} className="hover:bg-slate-50/50 dark:hover:bg-slate-800/40">
                      <td className="p-3 font-semibold text-slate-900 dark:text-white">{st.fullName}</td>
                      <td className="p-3 font-mono text-slate-600 dark:text-slate-300">{st.rollNumber}</td>
                      <td className="p-3 text-slate-600 dark:text-slate-300">{st.email}</td>
                      <td className="p-3 text-slate-600 dark:text-slate-300">
                        {st.courseName || 'Computer Science'} (Yr {st.currentYear || 1})
                      </td>
                      <td className="p-3 text-right space-x-2">
                        <button
                          onClick={() => handleApprove(st.id)}
                          disabled={actionLoading}
                          className="px-3 py-1.5 rounded-lg bg-emerald-600 hover:bg-emerald-700 text-white font-medium inline-flex items-center gap-1 transition-colors"
                        >
                          <Check className="w-3.5 h-3.5" />
                          Approve
                        </button>
                        <button
                          onClick={() => handleOpenReject(st)}
                          disabled={actionLoading}
                          className="px-3 py-1.5 rounded-lg bg-rose-50 text-rose-700 hover:bg-rose-100 dark:bg-rose-950/60 dark:text-rose-300 font-medium inline-flex items-center gap-1 transition-colors"
                        >
                          <X className="w-3.5 h-3.5" />
                          Reject
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {/* Pending Faculty Applications Queue */}
        <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 p-6 space-y-4 shadow-sm">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Users className="w-5 h-5 text-indigo-600" />
              <h2 className="text-lg font-bold text-slate-900 dark:text-white">
                Faculty Instructor Approvals
              </h2>
            </div>
            <Badge variant="warning">{pendingFaculty.length} Awaiting Verification</Badge>
          </div>

          {pendingFaculty.length === 0 ? (
            <p className="text-xs text-slate-500 dark:text-slate-400 py-6 text-center italic">
              All faculty instructor credentials are up to date.
            </p>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead className="bg-slate-50 dark:bg-slate-800/60 text-slate-500 dark:text-slate-400 uppercase font-semibold">
                  <tr>
                    <th className="p-3 rounded-l-lg">Faculty Name</th>
                    <th className="p-3">Employee ID</th>
                    <th className="p-3">Department</th>
                    <th className="p-3">Email</th>
                    <th className="p-3 rounded-r-lg text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 dark:divide-slate-800">
                  {pendingFaculty.map((fac) => (
                    <tr key={fac.id} className="hover:bg-slate-50/50 dark:hover:bg-slate-800/40">
                      <td className="p-3 font-semibold text-slate-900 dark:text-white">{fac.fullName}</td>
                      <td className="p-3 font-mono text-slate-600 dark:text-slate-300">{fac.employeeId}</td>
                      <td className="p-3 text-slate-600 dark:text-slate-300">{fac.department}</td>
                      <td className="p-3 text-slate-600 dark:text-slate-300">{fac.email}</td>
                      <td className="p-3 text-right space-x-2">
                        <button
                          onClick={() => handleApprove(fac.id)}
                          disabled={actionLoading}
                          className="px-3 py-1.5 rounded-lg bg-emerald-600 hover:bg-emerald-700 text-white font-medium inline-flex items-center gap-1 transition-colors"
                        >
                          <Check className="w-3.5 h-3.5" />
                          Approve
                        </button>
                        <button
                          onClick={() => handleOpenReject(fac)}
                          disabled={actionLoading}
                          className="px-3 py-1.5 rounded-lg bg-rose-50 text-rose-700 hover:bg-rose-100 dark:bg-rose-950/60 dark:text-rose-300 font-medium inline-flex items-center gap-1 transition-colors"
                        >
                          <X className="w-3.5 h-3.5" />
                          Reject
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </main>

      {/* Reject Modal */}
      <Modal
        isOpen={rejectModalOpen}
        onClose={() => setRejectModalOpen(false)}
        title="Document Reason for Rejection"
      >
        <div className="space-y-4">
          <div className="p-3 rounded-xl bg-amber-50 dark:bg-amber-950/40 text-amber-800 dark:text-amber-300 text-xs flex items-start gap-2">
            <AlertTriangle className="w-4 h-4 shrink-0 mt-0.5" />
            <span>
              Rejecting applicant <strong>{selectedUserToReject?.fullName}</strong>. This rejection notice and reason will be presented when they attempt to sign in.
            </span>
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 uppercase tracking-wider mb-1.5">
              Rejection Reason & Feedback
            </label>
            <textarea
              rows={3}
              value={rejectReason}
              onChange={(e) => setRejectReason(e.target.value)}
              className="w-full p-3 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white text-xs focus:ring-2 focus:ring-primary-500"
            />
          </div>

          <div className="flex justify-end gap-2 pt-2">
            <button
              onClick={() => setRejectModalOpen(false)}
              className="px-4 py-2 rounded-xl text-xs font-medium text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800"
            >
              Cancel
            </button>
            <button
              onClick={handleConfirmReject}
              disabled={actionLoading || !rejectReason.trim()}
              className="px-4 py-2 rounded-xl text-xs font-semibold text-white bg-rose-600 hover:bg-rose-700 disabled:opacity-50"
            >
              {actionLoading ? 'Processing...' : 'Confirm Rejection'}
            </button>
          </div>
        </div>
      </Modal>
    </div>
  );
};
