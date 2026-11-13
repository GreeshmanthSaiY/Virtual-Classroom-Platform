import React, { useState, useEffect } from 'react';
import { adminService } from '@/core/services/adminService';
import { User, PageResponse } from '@/types';
import { Search, UserCheck, UserX, ShieldAlert, RotateCcw, Users, X } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '@/shared/components/Card';
import { Table, Thead, Tbody, Tr, Th, Td } from '@/shared/components/Table';
import { Input } from '@/shared/components/Input';
import { Select } from '@/shared/components/Select';
import { Button } from '@/shared/components/Button';
import { Badge, StatusBadge } from '@/shared/components/Badge';
import { Pagination } from '@/shared/components/Pagination';
import { LoadingSpinner } from '@/shared/components/LoadingSpinner';
import { Modal } from '@/shared/components/Modal';
import { Textarea } from '@/shared/components/Textarea';
import { ConfirmDialog } from '@/shared/components/ConfirmDialog';
import { Breadcrumbs } from '@/shared/components/Breadcrumbs';

export const AdminUsersPage: React.FC = () => {
  const [users, setUsers] = useState<User[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);

  // Filters
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedRole, setSelectedRole] = useState('ROLE_STUDENT');
  const [selectedStatus, setSelectedStatus] = useState('');

  // Modals
  const [suspendModalOpen, setSuspendModalOpen] = useState(false);
  const [selectedUser, setSelectedUser] = useState<User | null>(null);
  const [suspendReason, setSuspendReason] = useState('');
  const [actionLoading, setActionLoading] = useState(false);

  // Confirm Reactivate
  const [reactivateDialogOpen, setReactivateDialogOpen] = useState(false);

  const fetchUsers = async () => {
    setLoading(true);
    try {
      const data: PageResponse<User> = await adminService.searchUsers({
        page,
        size: 15,
        q: searchQuery.trim() || undefined,
        role: selectedRole || undefined,
        status: selectedStatus || undefined,
      });
      setUsers(data?.content || []);
      setTotalPages(data?.totalPages || 1);
      setTotalElements(data?.totalElements || 0);
    } catch (err) {
      console.error('Failed to load users', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    const handler = setTimeout(() => {
      fetchUsers();
    }, 250);
    return () => clearTimeout(handler);
  }, [page, selectedRole, selectedStatus, searchQuery]);

  const handleOpenSuspend = (user: User) => {
    setSelectedUser(user);
    setSuspendReason('');
    setSuspendModalOpen(true);
  };

  const handleConfirmSuspend = async () => {
    if (!selectedUser) return;
    setActionLoading(true);
    try {
      await adminService.suspendUser(selectedUser.id, suspendReason || 'Administrative suspension');
      setSuspendModalOpen(false);
      setSelectedUser(null);
      await fetchUsers();
    } catch (err) {
      console.error('Failed to suspend user', err);
    } finally {
      setActionLoading(false);
    }
  };

  const handleOpenReactivate = (user: User) => {
    setSelectedUser(user);
    setReactivateDialogOpen(true);
  };

  const handleConfirmReactivate = async () => {
    if (!selectedUser) return;
    setActionLoading(true);
    try {
      await adminService.reactivateUser(selectedUser.id);
      setReactivateDialogOpen(false);
      setSelectedUser(null);
      await fetchUsers();
    } catch (err) {
      console.error('Failed to reactivate user', err);
    } finally {
      setActionLoading(false);
    }
  };

  const handleApprove = async (userId: number) => {
    setActionLoading(true);
    try {
      await adminService.approveUser(userId);
      await fetchUsers();
    } catch (err) {
      console.error('Approval failed', err);
    } finally {
      setActionLoading(false);
    }
  };

  const resetFilters = () => {
    setSearchQuery('');
    setSelectedRole('');
    setSelectedStatus('');
    setPage(0);
  };

  const hasActiveFilters = searchQuery !== '' || selectedRole !== 'ROLE_STUDENT' || selectedStatus !== '';

  return (
    <div className="space-y-6 sm:space-y-8 animate-fade-in">
      <Breadcrumbs
        items={[
          { label: 'Admin Portal', href: '/admin' },
          { label: 'User Governance' },
        ]}
      />

      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <div className="flex items-center gap-2">
            <div className="p-2 rounded-xl bg-primary-50 dark:bg-primary-950/60 text-primary-600 dark:text-primary-400">
              <Users className="w-5 h-5" />
            </div>
            <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-slate-900 dark:text-white">
              User Governance Directory
            </h1>
          </div>
          <p className="text-xs sm:text-sm text-slate-500 dark:text-slate-400 mt-1.5">
            Audit user records, manage academic roles, and execute access governance
          </p>
        </div>

        {hasActiveFilters && (
          <Button
            variant="ghost"
            size="sm"
            onClick={resetFilters}
            className="text-slate-500 hover:text-slate-800 dark:text-slate-400 dark:hover:text-slate-100 text-xs"
          >
            <X className="w-3.5 h-3.5 mr-1" /> Reset Filters
          </Button>
        )}
      </div>

      {/* Filters Bar */}
      <Card className="border-slate-200/80 dark:border-slate-800/80 shadow-soft-sm bg-white/90 dark:bg-slate-900/90 backdrop-blur-sm">
        <CardContent className="p-4 sm:p-5 grid grid-cols-1 sm:grid-cols-3 gap-3.5">
          <Input
            placeholder="Search by name, email, roll number..."
            value={searchQuery}
            onChange={(e) => {
              setSearchQuery(e.target.value);
              setPage(0);
            }}
            icon={<Search className="w-4 h-4 text-slate-400" />}
          />

          <Select
            label="Filter by Role"
            options={[
              { value: '', label: 'All Roles' },
              { value: 'ROLE_STUDENT', label: 'Students (ROLE_STUDENT)' },
              { value: 'ROLE_FACULTY', label: 'Faculty Members (ROLE_FACULTY)' },
              { value: 'ROLE_ADMIN', label: 'Administrators (ROLE_ADMIN)' },
            ]}
            value={selectedRole}
            onChange={(e) => {
              setSelectedRole(e.target.value);
              setPage(0);
            }}
          />

          <Select
            label="Filter by Status"
            options={[
              { value: '', label: 'All Statuses' },
              { value: 'APPROVED', label: 'Active / Approved' },
              { value: 'PENDING', label: 'Pending Approval' },
              { value: 'SUSPENDED', label: 'Suspended' },
              { value: 'REJECTED', label: 'Rejected' },
            ]}
            value={selectedStatus}
            onChange={(e) => {
              setSelectedStatus(e.target.value);
              setPage(0);
            }}
          />
        </CardContent>
      </Card>

      {/* Users Table */}
      <Card className="border-slate-200/80 dark:border-slate-800/80 shadow-soft-sm overflow-hidden">
        <CardHeader className="flex justify-between items-center py-4 px-6 border-b border-slate-100 dark:border-slate-800">
          <CardTitle className="text-base font-bold flex items-center gap-2">
            <span>Academic Users Registry</span>
            <Badge variant="neutral" size="sm">
              {totalElements} Total
            </Badge>
          </CardTitle>
          <span className="text-xs text-slate-400">Page {page + 1} of {Math.max(totalPages, 1)}</span>
        </CardHeader>
        <CardContent className="p-0">
          {loading ? (
            <div className="p-16 flex justify-center">
              <LoadingSpinner size="md" label="Loading user directory..." />
            </div>
          ) : users.length === 0 ? (
            <div className="py-16 px-4 text-center space-y-2">
              <div className="w-12 h-12 rounded-2xl bg-slate-100 dark:bg-slate-800 text-slate-400 flex items-center justify-center mx-auto">
                <Users className="w-6 h-6" />
              </div>
              <h4 className="text-sm font-semibold text-slate-800 dark:text-slate-200">
                No Users Matching Filter Criteria
              </h4>
              <p className="text-xs text-slate-400">
                Try clearing or modifying the search terms above.
              </p>
            </div>
          ) : (
            <Table>
              <Thead>
                <Tr>
                  <Th>Scholar / Faculty</Th>
                  <Th>Platform Role</Th>
                  <Th>Academic Detail</Th>
                  <Th>Access Status</Th>
                  <Th>Joined</Th>
                  <Th className="text-right">Governance Actions</Th>
                </Tr>
              </Thead>
              <Tbody>
                {users.map((u) => (
                  <Tr key={u.id}>
                    <Td>
                      <div className="flex items-center gap-3">
                        <div className="w-9 h-9 rounded-xl bg-slate-100 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 flex items-center justify-center font-bold text-xs text-slate-700 dark:text-slate-200 shrink-0">
                          {u.fullName ? u.fullName.charAt(0).toUpperCase() : 'U'}
                        </div>
                        <div className="min-w-0">
                          <div className="font-bold text-slate-900 dark:text-white truncate">
                            {u.fullName}
                          </div>
                          <div className="text-xs text-slate-500 dark:text-slate-400 truncate">
                            {u.email}
                          </div>
                        </div>
                      </div>
                    </Td>
                    <Td>
                      <Badge
                        variant={
                          u.role === 'ROLE_ADMIN'
                            ? 'danger'
                            : u.role === 'ROLE_FACULTY'
                            ? 'primary'
                            : 'success'
                        }
                        size="sm"
                      >
                        {u.role.replace('ROLE_', '')}
                      </Badge>
                    </Td>
                    <Td>
                      <span className="text-xs font-medium text-slate-600 dark:text-slate-300">
                        {u.rollNumber ? `Roll: ${u.rollNumber}` : u.department || 'General'}
                      </span>
                    </Td>
                    <Td>
                      <StatusBadge status={u.status} size="sm" />
                    </Td>
                    <Td>
                      <span className="text-xs text-slate-500 dark:text-slate-400">
                        {new Date(u.createdAt).toLocaleDateString(undefined, {
                          month: 'short',
                          day: 'numeric',
                          year: 'numeric',
                        })}
                      </span>
                    </Td>
                    <Td className="text-right">
                      <div className="flex items-center justify-end gap-1.5">
                        {u.status === 'PENDING' && (
                          <Button
                            variant="primary"
                            size="sm"
                            onClick={() => handleApprove(u.id)}
                            disabled={actionLoading}
                            className="bg-emerald-600 hover:bg-emerald-700 text-white shadow-soft-sm"
                          >
                            <UserCheck className="w-3.5 h-3.5 mr-1" /> Approve
                          </Button>
                        )}

                        {u.status === 'APPROVED' && u.role !== 'ROLE_ADMIN' && (
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => handleOpenSuspend(u)}
                            disabled={actionLoading}
                            className="text-rose-600 border-rose-200 dark:border-rose-900/60 hover:bg-rose-50 dark:hover:bg-rose-950/30"
                          >
                            <UserX className="w-3.5 h-3.5 mr-1" /> Suspend
                          </Button>
                        )}

                        {u.status === 'SUSPENDED' && (
                          <Button
                            variant="outline"
                            size="sm"
                            onClick={() => handleOpenReactivate(u)}
                            disabled={actionLoading}
                            className="text-emerald-600 border-emerald-200 dark:border-emerald-900/60 hover:bg-emerald-50 dark:hover:bg-emerald-950/30"
                          >
                            <RotateCcw className="w-3.5 h-3.5 mr-1" /> Reactivate
                          </Button>
                        )}
                      </div>
                    </Td>
                  </Tr>
                ))}
              </Tbody>
            </Table>
          )}

          <div className="p-4 border-t border-slate-100 dark:border-slate-800">
            <Pagination
              currentPage={page}
              totalPages={totalPages}
              totalItems={totalElements}
              pageSize={15}
              onPageChange={(newPage) => setPage(newPage)}
            />
          </div>
        </CardContent>
      </Card>

      {/* Suspend Account Modal */}
      <Modal
        isOpen={suspendModalOpen}
        onClose={() => setSuspendModalOpen(false)}
        title="Suspend Academic Account Access"
      >
        <div className="p-6 space-y-4">
          <div className="p-3.5 bg-rose-50 dark:bg-rose-950/30 border border-rose-200 dark:border-rose-900/40 rounded-2xl flex items-start gap-3 text-xs text-rose-800 dark:text-rose-200">
            <ShieldAlert className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
            <span>
              Suspending <strong className="font-semibold">{selectedUser?.fullName}</strong> ({selectedUser?.email})
              will instantly revoke all active refresh tokens and prohibit subsequent sign-ins.
            </span>
          </div>

          <Textarea
            label="Suspension Justification"
            value={suspendReason}
            onChange={(e) => setSuspendReason(e.target.value)}
            placeholder="e.g. Academic integrity violation or disciplinary measure."
            rows={3}
            required
            helperText="Reason will be recorded into the persistent audit trail."
          />

          <div className="flex justify-end gap-2.5 pt-2">
            <Button
              variant="outline"
              size="sm"
              onClick={() => setSuspendModalOpen(false)}
              disabled={actionLoading}
            >
              Cancel
            </Button>
            <Button
              variant="danger"
              size="sm"
              onClick={handleConfirmSuspend}
              isLoading={actionLoading}
            >
              Confirm Account Suspension
            </Button>
          </div>
        </div>
      </Modal>

      {/* Reactivate Account Confirm Dialog */}
      <ConfirmDialog
        isOpen={reactivateDialogOpen}
        title="Reactivate Academic Account"
        message={`Are you sure you want to restore active access for ${selectedUser?.fullName}? They will be permitted to authenticate and access their coursework again.`}
        confirmLabel="Reactivate Account"
        variant="primary"
        isLoading={actionLoading}
        onConfirm={handleConfirmReactivate}
        onCancel={() => setReactivateDialogOpen(false)}
      />
    </div>
  );
};
