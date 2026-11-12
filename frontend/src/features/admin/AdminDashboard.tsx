import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { adminService } from '@/core/services/adminService';
import { PlatformMetrics, User } from '@/types';
import {
  Users,
  GraduationCap,
  Clock,
  BookOpen,
  Check,
  X,
  AlertTriangle,
  RefreshCw,
  Video,
  Radio,
  FileCheck,
  ShieldCheck,
  Layers,
  ArrowRight,
} from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '@/shared/components/Card';
import { Button } from '@/shared/components/Button';
import { Badge, StatusBadge } from '@/shared/components/Badge';
import { LoadingSpinner } from '@/shared/components/LoadingSpinner';
import { Modal } from '@/shared/components/Modal';
import { Textarea } from '@/shared/components/Textarea';

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
      const [m, s, f] = await Promise.all([
        adminService.getMetrics(),
        adminService.getPendingStudents(),
        adminService.getPendingFaculty(),
      ]);
      setMetrics(m);
      setPendingStudents(s || []);
      setPendingFaculty(f || []);
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
      await adminService.approveUser(userId);
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
      await adminService.rejectUser(selectedUserToReject.id, rejectReason);
      setRejectModalOpen(false);
      setSelectedUserToReject(null);
      await loadData();
    } catch (err) {
      console.error('Rejection failed', err);
    } finally {
      setActionLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="flex justify-center items-center h-72">
        <LoadingSpinner size="lg" label="Aggregating platform metrics and pending requests..." />
      </div>
    );
  }

  const allPending = [...pendingStudents, ...pendingFaculty];

  return (
    <div className="space-y-6">
      {/* Header Banner */}
      <div className="bg-gradient-to-r from-indigo-900 to-slate-900 rounded-2xl p-6 sm:p-8 text-white shadow-lg flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <div className="flex items-center gap-2 mb-2">
            <span className="px-2.5 py-0.5 rounded-full bg-rose-500/20 text-rose-300 border border-rose-500/30 text-xs font-semibold uppercase tracking-wider flex items-center gap-1.5">
              <ShieldCheck className="w-3.5 h-3.5" /> Institutional Administration
            </span>
          </div>
          <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight">Administrative Overview</h1>
          <p className="text-xs sm:text-sm text-indigo-200 mt-1">
            System health, verified users, curriculum entities, and pending academic approvals
          </p>
        </div>
        <Button variant="outline" size="sm" onClick={loadData} className="bg-white/10 hover:bg-white/20 text-white border-white/20">
          <RefreshCw className="w-3.5 h-3.5 mr-1.5" /> Refresh Telemetry
        </Button>
      </div>

      {/* Metrics Grid */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4">
        <Card hover>
          <CardContent className="p-5 flex items-center gap-4">
            <div className="p-3 bg-indigo-50 dark:bg-indigo-950/50 rounded-xl text-indigo-600 dark:text-indigo-400">
              <Users className="w-6 h-6" />
            </div>
            <div>
              <p className="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">
                Total Students
              </p>
              <h3 className="text-2xl font-bold text-slate-900 dark:text-white mt-0.5">
                {metrics?.totalStudents ?? 0}
              </h3>
            </div>
          </CardContent>
        </Card>

        <Card hover>
          <CardContent className="p-5 flex items-center gap-4">
            <div className="p-3 bg-emerald-50 dark:bg-emerald-950/50 rounded-xl text-emerald-600 dark:text-emerald-400">
              <GraduationCap className="w-6 h-6" />
            </div>
            <div>
              <p className="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">
                Active Faculty
              </p>
              <h3 className="text-2xl font-bold text-slate-900 dark:text-white mt-0.5">
                {metrics?.totalFaculty ?? 0}
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
              <p className="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">
                Pending Approvals
              </p>
              <h3 className="text-2xl font-bold text-amber-600 dark:text-amber-400 mt-0.5">
                {(metrics?.pendingStudentApprovals ?? 0) + (metrics?.pendingFacultyApprovals ?? 0)}
              </h3>
            </div>
          </CardContent>
        </Card>

        <Card hover>
          <CardContent className="p-5 flex items-center gap-4">
            <div className="p-3 bg-purple-50 dark:bg-purple-950/50 rounded-xl text-purple-600 dark:text-purple-400">
              <Layers className="w-6 h-6" />
            </div>
            <div>
              <p className="text-xs font-semibold text-slate-500 dark:text-slate-400 uppercase tracking-wider">
                Degrees & Courses
              </p>
              <h3 className="text-2xl font-bold text-slate-900 dark:text-white mt-0.5">
                {metrics?.totalCourses ?? 0}
              </h3>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Secondary Metrics Bar */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
        <div className="p-4 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl flex items-center justify-between">
          <div>
            <span className="text-xs text-slate-500">Curriculum Subjects</span>
            <p className="text-lg font-bold text-slate-800 dark:text-slate-100">{metrics?.totalSubjects ?? 0}</p>
          </div>
          <BookOpen className="w-5 h-5 text-slate-400" />
        </div>
        <div className="p-4 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl flex items-center justify-between">
          <div>
            <span className="text-xs text-slate-500">Learning Materials</span>
            <p className="text-lg font-bold text-slate-800 dark:text-slate-100">{metrics?.totalMaterials ?? 0}</p>
          </div>
          <FileCheck className="w-5 h-5 text-slate-400" />
        </div>
        <div className="p-4 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl flex items-center justify-between">
          <div>
            <span className="text-xs text-slate-500">Video Lectures</span>
            <p className="text-lg font-bold text-slate-800 dark:text-slate-100">{metrics?.totalLectures ?? 0}</p>
          </div>
          <Video className="w-5 h-5 text-slate-400" />
        </div>
        <div className="p-4 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl flex items-center justify-between">
          <div>
            <span className="text-xs text-slate-500">Live Sessions</span>
            <p className="text-lg font-bold text-slate-800 dark:text-slate-100">{metrics?.upcomingLiveSessions ?? 0}</p>
          </div>
          <Radio className="w-5 h-5 text-slate-400" />
        </div>
      </div>

      {/* Quick Links & Pending Approvals Queue */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Pending Requests Queue (2 Columns) */}
        <div className="lg:col-span-2">
          <Card>
            <CardHeader className="flex items-center justify-between">
              <div>
                <CardTitle>Pending Account Approvals</CardTitle>
                <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
                  Review new student and faculty registration requests before activating system access.
                </p>
              </div>
              <Badge variant={allPending.length > 0 ? 'warning' : 'neutral'} size="sm">
                {allPending.length} Waiting
              </Badge>
            </CardHeader>
            <CardContent className="p-0">
              {allPending.length === 0 ? (
                <div className="p-8 text-center text-slate-500 dark:text-slate-400 text-xs">
                  All registration requests have been reviewed and approved.
                </div>
              ) : (
                <div className="divide-y divide-slate-100 dark:divide-slate-800">
                  {allPending.map((pUser) => (
                    <div key={pUser.id} className="p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3 hover:bg-slate-50/50 dark:hover:bg-slate-800/30 transition-colors">
                      <div className="space-y-1">
                        <div className="flex items-center gap-2">
                          <span className="font-semibold text-sm text-slate-900 dark:text-white">
                            {pUser.fullName}
                          </span>
                          <Badge variant={pUser.role === 'ROLE_FACULTY' ? 'primary' : 'success'} size="sm">
                            {pUser.role === 'ROLE_FACULTY' ? 'Faculty' : 'Student'}
                          </Badge>
                          <StatusBadge status={pUser.status} size="sm" />
                        </div>
                        <p className="text-xs text-slate-500 dark:text-slate-400">
                          {pUser.email} • {pUser.role === 'ROLE_FACULTY' ? pUser.department : `Roll: ${pUser.rollNumber}`}
                        </p>
                      </div>

                      <div className="flex items-center gap-2">
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => handleOpenReject(pUser)}
                          disabled={actionLoading}
                          className="border-rose-300 dark:border-rose-800 text-rose-700 dark:text-rose-300 hover:bg-rose-50"
                        >
                          <X className="w-3.5 h-3.5 mr-1" /> Reject
                        </Button>
                        <Button
                          variant="primary"
                          size="sm"
                          onClick={() => handleApprove(pUser.id)}
                          disabled={actionLoading}
                          className="bg-emerald-600 hover:bg-emerald-700 text-white"
                        >
                          <Check className="w-3.5 h-3.5 mr-1" /> Approve
                        </Button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>
        </div>

        {/* Administration Quick Shortcuts (1 Column) */}
        <div className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle>Governance Shortcuts</CardTitle>
            </CardHeader>
            <CardContent className="p-4 space-y-2.5">
              <Link
                to="/admin/users"
                className="flex items-center justify-between p-3 rounded-xl border border-slate-200 dark:border-slate-800 hover:border-indigo-500 dark:hover:border-indigo-500 hover:bg-indigo-50/20 transition-all text-xs font-semibold text-slate-700 dark:text-slate-200 group"
              >
                <div className="flex items-center gap-2.5">
                  <Users className="w-4 h-4 text-indigo-600" />
                  <span>User Governance Directory</span>
                </div>
                <ArrowRight className="w-4 h-4 text-slate-400 group-hover:translate-x-1 transition-transform" />
              </Link>

              <Link
                to="/admin/courses"
                className="flex items-center justify-between p-3 rounded-xl border border-slate-200 dark:border-slate-800 hover:border-indigo-500 dark:hover:border-indigo-500 hover:bg-indigo-50/20 transition-all text-xs font-semibold text-slate-700 dark:text-slate-200 group"
              >
                <div className="flex items-center gap-2.5">
                  <Layers className="w-4 h-4 text-purple-600" />
                  <span>Degrees & Courses Catalog</span>
                </div>
                <ArrowRight className="w-4 h-4 text-slate-400 group-hover:translate-x-1 transition-transform" />
              </Link>

              <Link
                to="/materials"
                className="flex items-center justify-between p-3 rounded-xl border border-slate-200 dark:border-slate-800 hover:border-indigo-500 dark:hover:border-indigo-500 hover:bg-indigo-50/20 transition-all text-xs font-semibold text-slate-700 dark:text-slate-200 group"
              >
                <div className="flex items-center gap-2.5">
                  <BookOpen className="w-4 h-4 text-emerald-600" />
                  <span>Course Materials Repository</span>
                </div>
                <ArrowRight className="w-4 h-4 text-slate-400 group-hover:translate-x-1 transition-transform" />
              </Link>
            </CardContent>
          </Card>
        </div>
      </div>

      {/* Reject Reason Modal */}
      <Modal
        isOpen={rejectModalOpen}
        onClose={() => setRejectModalOpen(false)}
        title="Reject Academic Registration"
      >
        <div className="p-6 space-y-4">
          <div className="p-3 bg-rose-50 dark:bg-rose-950/30 border border-rose-200 dark:border-rose-900/40 rounded-xl flex items-start gap-2.5 text-xs text-rose-800 dark:text-rose-200">
            <AlertTriangle className="w-4 h-4 text-rose-600 flex-shrink-0 mt-0.5" />
            <span>
              Rejecting this account will notify the user and block account activation. Please provide a formal justification.
            </span>
          </div>

          <Textarea
            label="Formal Rejection Justification"
            value={rejectReason}
            onChange={(e) => setRejectReason(e.target.value)}
            rows={3}
            required
            helperText="Reason will be saved to the academic audit log and visible on the user's login rejection notice."
          />

          <div className="flex justify-end gap-2 pt-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => setRejectModalOpen(false)}
              disabled={actionLoading}
            >
              Cancel
            </Button>
            <Button
              variant="danger"
              size="sm"
              onClick={handleConfirmReject}
              isLoading={actionLoading}
            >
              Confirm Rejection
            </Button>
          </div>
        </div>
      </Modal>
    </div>
  );
};
