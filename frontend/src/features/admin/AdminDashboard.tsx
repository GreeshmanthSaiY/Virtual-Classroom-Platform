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
  TrendingUp,
  Sparkles,
  UserCheck,
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
  const [approvalTab, setApprovalTab] = useState<'ALL' | 'STUDENT' | 'FACULTY'>('ALL');

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
      <div className="flex flex-col justify-center items-center h-80 space-y-4">
        <LoadingSpinner size="lg" label="Aggregating platform metrics and academic requests..." />
      </div>
    );
  }

  const allPending = [...pendingStudents, ...pendingFaculty];
  const filteredPending = allPending.filter((u) => {
    if (approvalTab === 'STUDENT') return u.role === 'ROLE_STUDENT';
    if (approvalTab === 'FACULTY') return u.role === 'ROLE_FACULTY';
    return true;
  });

  return (
    <div className="space-y-6 sm:space-y-8 animate-fade-in">
      {/* Header Card */}
      <div className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-slate-900 via-indigo-950 to-slate-900 border border-slate-800 p-6 sm:p-8 text-white shadow-soft-xl">
        <div className="absolute top-0 right-0 -mt-10 -mr-10 w-80 h-80 rounded-full bg-primary-600/10 blur-3xl pointer-events-none" />
        <div className="absolute bottom-0 left-1/3 -mb-10 w-60 h-60 rounded-full bg-rose-600/10 blur-3xl pointer-events-none" />

        <div className="relative z-10 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-6">
          <div className="space-y-2 max-w-2xl">
            <div className="flex flex-wrap items-center gap-2">
              <span className="px-3 py-1 rounded-full bg-rose-500/20 text-rose-300 border border-rose-500/30 text-xs font-semibold uppercase tracking-wider flex items-center gap-1.5 backdrop-blur-sm">
                <ShieldCheck className="w-3.5 h-3.5" /> Institutional Administration
              </span>
              <span className="px-2.5 py-0.5 rounded-full bg-white/10 text-slate-300 text-xs font-medium backdrop-blur-sm flex items-center gap-1">
                <Sparkles className="w-3 h-3 text-amber-400" /> Active Session 2026
              </span>
            </div>
            <h1 className="text-2xl sm:text-3xl lg:text-4xl font-extrabold tracking-tight text-white">
              Institutional Governance
            </h1>
            <p className="text-sm text-slate-300 leading-relaxed">
              Real-time platform metrics, user access lifecycle, degree curriculum structure, and pending identity approvals.
            </p>
          </div>

          <div className="flex items-center gap-3 self-stretch sm:self-auto">
            <Button
              variant="outline"
              size="md"
              onClick={loadData}
              className="w-full sm:w-auto bg-white/10 hover:bg-white/20 text-white border-white/20 shadow-soft-sm backdrop-blur-sm"
            >
              <RefreshCw className="w-4 h-4 mr-2" /> Refresh Telemetry
            </Button>
          </div>
        </div>
      </div>

      {/* Primary KPI Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 sm:gap-5">
        <Card hover className="border-slate-200/80 dark:border-slate-800/80 bg-white/90 dark:bg-slate-900/90 backdrop-blur-sm">
          <CardContent className="p-5 flex items-center gap-4">
            <div className="p-3.5 bg-primary-50 dark:bg-primary-950/60 rounded-2xl text-primary-600 dark:text-primary-400 shadow-soft-sm shrink-0">
              <Users className="w-6 h-6" />
            </div>
            <div className="min-w-0">
              <p className="text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider truncate">
                Total Students
              </p>
              <h3 className="text-2xl sm:text-3xl font-extrabold text-slate-900 dark:text-white mt-0.5 tracking-tight">
                {metrics?.totalStudents ?? 0}
              </h3>
              <p className="text-[11px] text-slate-400 dark:text-slate-500 flex items-center gap-1 mt-0.5">
                <TrendingUp className="w-3 h-3 text-emerald-500" /> Enrolled scholars
              </p>
            </div>
          </CardContent>
        </Card>

        <Card hover className="border-slate-200/80 dark:border-slate-800/80 bg-white/90 dark:bg-slate-900/90 backdrop-blur-sm">
          <CardContent className="p-5 flex items-center gap-4">
            <div className="p-3.5 bg-emerald-50 dark:bg-emerald-950/60 rounded-2xl text-emerald-600 dark:text-emerald-400 shadow-soft-sm shrink-0">
              <GraduationCap className="w-6 h-6" />
            </div>
            <div className="min-w-0">
              <p className="text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider truncate">
                Active Faculty
              </p>
              <h3 className="text-2xl sm:text-3xl font-extrabold text-slate-900 dark:text-white mt-0.5 tracking-tight">
                {metrics?.totalFaculty ?? 0}
              </h3>
              <p className="text-[11px] text-slate-400 dark:text-slate-500 flex items-center gap-1 mt-0.5">
                <Check className="w-3 h-3 text-emerald-500" /> Verified educators
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
                Pending Approvals
              </p>
              <h3 className="text-2xl sm:text-3xl font-extrabold text-amber-600 dark:text-amber-400 mt-0.5 tracking-tight">
                {allPending.length}
              </h3>
              <p className="text-[11px] text-slate-400 dark:text-slate-500 flex items-center gap-1 mt-0.5">
                Requires identity audit
              </p>
            </div>
          </CardContent>
        </Card>

        <Card hover className="border-slate-200/80 dark:border-slate-800/80 bg-white/90 dark:bg-slate-900/90 backdrop-blur-sm">
          <CardContent className="p-5 flex items-center gap-4">
            <div className="p-3.5 bg-purple-50 dark:bg-purple-950/60 rounded-2xl text-purple-600 dark:text-purple-400 shadow-soft-sm shrink-0">
              <Layers className="w-6 h-6" />
            </div>
            <div className="min-w-0">
              <p className="text-xs font-bold text-slate-500 dark:text-slate-400 uppercase tracking-wider truncate">
                Degrees & Courses
              </p>
              <h3 className="text-2xl sm:text-3xl font-extrabold text-slate-900 dark:text-white mt-0.5 tracking-tight">
                {metrics?.totalCourses ?? 0}
              </h3>
              <p className="text-[11px] text-slate-400 dark:text-slate-500 flex items-center gap-1 mt-0.5">
                Active degree programs
              </p>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* Secondary Metrics Bar */}
      <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 sm:gap-4">
        <div className="p-4 bg-white/80 dark:bg-slate-900/80 border border-slate-200/80 dark:border-slate-800/80 rounded-2xl shadow-soft-sm flex items-center justify-between">
          <div>
            <span className="text-xs font-medium text-slate-500 dark:text-slate-400">Curriculum Subjects</span>
            <p className="text-lg font-bold text-slate-900 dark:text-slate-100">{metrics?.totalSubjects ?? 0}</p>
          </div>
          <BookOpen className="w-5 h-5 text-slate-400" />
        </div>

        <div className="p-4 bg-white/80 dark:bg-slate-900/80 border border-slate-200/80 dark:border-slate-800/80 rounded-2xl shadow-soft-sm flex items-center justify-between">
          <div>
            <span className="text-xs font-medium text-slate-500 dark:text-slate-400">Study Materials</span>
            <p className="text-lg font-bold text-slate-900 dark:text-slate-100">{metrics?.totalMaterials ?? 0}</p>
          </div>
          <FileCheck className="w-5 h-5 text-slate-400" />
        </div>

        <div className="p-4 bg-white/80 dark:bg-slate-900/80 border border-slate-200/80 dark:border-slate-800/80 rounded-2xl shadow-soft-sm flex items-center justify-between">
          <div>
            <span className="text-xs font-medium text-slate-500 dark:text-slate-400">Video Lectures</span>
            <p className="text-lg font-bold text-slate-900 dark:text-slate-100">{metrics?.totalLectures ?? 0}</p>
          </div>
          <Video className="w-5 h-5 text-slate-400" />
        </div>

        <div className="p-4 bg-white/80 dark:bg-slate-900/80 border border-slate-200/80 dark:border-slate-800/80 rounded-2xl shadow-soft-sm flex items-center justify-between">
          <div>
            <span className="text-xs font-medium text-slate-500 dark:text-slate-400">Live Sessions</span>
            <p className="text-lg font-bold text-slate-900 dark:text-slate-100">{metrics?.upcomingLiveSessions ?? 0}</p>
          </div>
          <Radio className="w-5 h-5 text-slate-400" />
        </div>
      </div>

      {/* Main Content Grid: Approvals Queue (2/3) + Shortcuts (1/3) */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Pending Requests Queue */}
        <div className="lg:col-span-2 space-y-4">
          <Card className="border-slate-200/80 dark:border-slate-800/80 shadow-soft-sm overflow-hidden">
            <CardHeader className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4">
              <div>
                <CardTitle className="text-lg font-bold flex items-center gap-2">
                  <UserCheck className="w-5 h-5 text-primary-600 dark:text-primary-400" />
                  Account Approval Queue
                </CardTitle>
                <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
                  Verify academic credentials and activate university portal access
                </p>
              </div>

              {/* Tabs */}
              <div className="flex items-center gap-1 p-1 bg-slate-100 dark:bg-slate-800/80 rounded-xl text-xs font-semibold">
                <button
                  onClick={() => setApprovalTab('ALL')}
                  className={`px-3 py-1.5 rounded-lg transition-colors ${
                    approvalTab === 'ALL'
                      ? 'bg-white dark:bg-slate-900 text-slate-900 dark:text-white shadow-soft-sm'
                      : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
                  }`}
                >
                  All ({allPending.length})
                </button>
                <button
                  onClick={() => setApprovalTab('STUDENT')}
                  className={`px-3 py-1.5 rounded-lg transition-colors ${
                    approvalTab === 'STUDENT'
                      ? 'bg-white dark:bg-slate-900 text-slate-900 dark:text-white shadow-soft-sm'
                      : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
                  }`}
                >
                  Students ({pendingStudents.length})
                </button>
                <button
                  onClick={() => setApprovalTab('FACULTY')}
                  className={`px-3 py-1.5 rounded-lg transition-colors ${
                    approvalTab === 'FACULTY'
                      ? 'bg-white dark:bg-slate-900 text-slate-900 dark:text-white shadow-soft-sm'
                      : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
                  }`}
                >
                  Faculty ({pendingFaculty.length})
                </button>
              </div>
            </CardHeader>

            <CardContent className="p-0">
              {filteredPending.length === 0 ? (
                <div className="py-14 px-4 text-center space-y-3">
                  <div className="w-12 h-12 rounded-2xl bg-emerald-50 dark:bg-emerald-950/60 text-emerald-600 dark:text-emerald-400 flex items-center justify-center mx-auto shadow-soft-sm">
                    <Check className="w-6 h-6" />
                  </div>
                  <div>
                    <h4 className="text-sm font-semibold text-slate-800 dark:text-slate-200">
                      Queue is Completely Clear
                    </h4>
                    <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
                      All prospective student and faculty registrations have been approved or audited.
                    </p>
                  </div>
                </div>
              ) : (
                <div className="divide-y divide-slate-100 dark:divide-slate-800/80">
                  {filteredPending.map((pUser) => (
                    <div
                      key={pUser.id}
                      className="p-4 sm:p-5 flex flex-col sm:flex-row sm:items-center justify-between gap-4 hover:bg-slate-50/70 dark:hover:bg-slate-800/40 transition-colors"
                    >
                      <div className="flex items-start gap-3.5">
                        <div className="w-10 h-10 rounded-2xl bg-gradient-to-tr from-slate-200 to-slate-300 dark:from-slate-700 dark:to-slate-800 flex items-center justify-center text-slate-700 dark:text-slate-200 font-bold text-sm shadow-soft-sm shrink-0">
                          {pUser.fullName ? pUser.fullName.charAt(0).toUpperCase() : 'U'}
                        </div>
                        <div className="space-y-1 min-w-0">
                          <div className="flex flex-wrap items-center gap-2">
                            <span className="font-bold text-sm text-slate-900 dark:text-white truncate">
                              {pUser.fullName}
                            </span>
                            <Badge
                              variant={pUser.role === 'ROLE_FACULTY' ? 'primary' : 'success'}
                              size="sm"
                            >
                              {pUser.role === 'ROLE_FACULTY' ? 'Faculty Applicant' : 'Student Applicant'}
                            </Badge>
                            <StatusBadge status={pUser.status} size="sm" />
                          </div>
                          <p className="text-xs text-slate-500 dark:text-slate-400 truncate">
                            {pUser.email}
                            {pUser.role === 'ROLE_FACULTY' && pUser.department
                              ? ` • Department: ${pUser.department}`
                              : pUser.rollNumber
                              ? ` • Roll: ${pUser.rollNumber}`
                              : ''}
                          </p>
                        </div>
                      </div>

                      <div className="flex items-center gap-2 self-end sm:self-center shrink-0">
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => handleOpenReject(pUser)}
                          disabled={actionLoading}
                          className="border-rose-200 dark:border-rose-800/80 text-rose-600 dark:text-rose-400 hover:bg-rose-50 dark:hover:bg-rose-950/30"
                        >
                          <X className="w-3.5 h-3.5 mr-1" /> Reject
                        </Button>
                        <Button
                          variant="primary"
                          size="sm"
                          onClick={() => handleApprove(pUser.id)}
                          disabled={actionLoading}
                          className="bg-emerald-600 hover:bg-emerald-700 text-white shadow-soft-sm"
                        >
                          <Check className="w-3.5 h-3.5 mr-1" /> Approve Access
                        </Button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </CardContent>
          </Card>
        </div>

        {/* Shortcuts & Governance Actions */}
        <div className="space-y-4">
          <Card className="border-slate-200/80 dark:border-slate-800/80 shadow-soft-sm">
            <CardHeader className="pb-3">
              <CardTitle className="text-base font-bold">Governance Center</CardTitle>
            </CardHeader>
            <CardContent className="p-4 space-y-2.5">
              <Link
                to="/admin/users"
                className="flex items-center justify-between p-3.5 rounded-2xl border border-slate-200/80 dark:border-slate-800/80 hover:border-primary-500/60 dark:hover:border-primary-500/60 hover:bg-primary-50/30 dark:hover:bg-primary-950/20 transition-all text-xs font-semibold text-slate-800 dark:text-slate-200 group shadow-soft-sm"
              >
                <div className="flex items-center gap-3">
                  <div className="p-2 rounded-xl bg-primary-50 dark:bg-primary-950/60 text-primary-600 dark:text-primary-400">
                    <Users className="w-4 h-4" />
                  </div>
                  <div>
                    <span className="block font-bold">User Directory</span>
                    <span className="text-[11px] text-slate-500 dark:text-slate-400 font-normal">
                      Search scholars, suspend or reactivate
                    </span>
                  </div>
                </div>
                <ArrowRight className="w-4 h-4 text-slate-400 group-hover:translate-x-1 transition-transform shrink-0" />
              </Link>

              <Link
                to="/admin/courses"
                className="flex items-center justify-between p-3.5 rounded-2xl border border-slate-200/80 dark:border-slate-800/80 hover:border-purple-500/60 dark:hover:border-purple-500/60 hover:bg-purple-50/30 dark:hover:bg-purple-950/20 transition-all text-xs font-semibold text-slate-800 dark:text-slate-200 group shadow-soft-sm"
              >
                <div className="flex items-center gap-3">
                  <div className="p-2 rounded-xl bg-purple-50 dark:bg-purple-950/60 text-purple-600 dark:text-purple-400">
                    <Layers className="w-4 h-4" />
                  </div>
                  <div>
                    <span className="block font-bold">Degrees & Subjects</span>
                    <span className="text-[11px] text-slate-500 dark:text-slate-400 font-normal">
                      Curriculum and faculty allocation
                    </span>
                  </div>
                </div>
                <ArrowRight className="w-4 h-4 text-slate-400 group-hover:translate-x-1 transition-transform shrink-0" />
              </Link>

              <Link
                to="/materials"
                className="flex items-center justify-between p-3.5 rounded-2xl border border-slate-200/80 dark:border-slate-800/80 hover:border-emerald-500/60 dark:hover:border-emerald-500/60 hover:bg-emerald-50/30 dark:hover:bg-emerald-950/20 transition-all text-xs font-semibold text-slate-800 dark:text-slate-200 group shadow-soft-sm"
              >
                <div className="flex items-center gap-3">
                  <div className="p-2 rounded-xl bg-emerald-50 dark:bg-emerald-950/60 text-emerald-600 dark:text-emerald-400">
                    <BookOpen className="w-4 h-4" />
                  </div>
                  <div>
                    <span className="block font-bold">Academic Materials</span>
                    <span className="text-[11px] text-slate-500 dark:text-slate-400 font-normal">
                      Course notes, syllabi and archives
                    </span>
                  </div>
                </div>
                <ArrowRight className="w-4 h-4 text-slate-400 group-hover:translate-x-1 transition-transform shrink-0" />
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
          <div className="p-3.5 bg-rose-50 dark:bg-rose-950/30 border border-rose-200 dark:border-rose-900/40 rounded-2xl flex items-start gap-3 text-xs text-rose-800 dark:text-rose-200">
            <AlertTriangle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
            <span>
              Rejecting this account will notify the user and block account activation. Please provide a formal justification for the academic audit log.
            </span>
          </div>

          <Textarea
            label="Formal Rejection Justification"
            value={rejectReason}
            onChange={(e) => setRejectReason(e.target.value)}
            rows={3}
            required
            helperText="Reason will be saved to the persistent audit trail."
          />

          <div className="flex justify-end gap-2.5 pt-2">
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
