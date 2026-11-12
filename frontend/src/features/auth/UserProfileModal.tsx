import React, { useState, useEffect } from 'react';
import { useAuth } from '@/context/AuthContext';
import { authService } from '@/core/services/authService';
import { User, ProfileUpdateRequest } from '@/types';
import { Modal } from '@/shared/components/Modal';
import { Input } from '@/shared/components/Input';
import { Textarea } from '@/shared/components/Textarea';
import { Button } from '@/shared/components/Button';
import { Badge } from '@/shared/components/Badge';
import { LoadingSpinner } from '@/shared/components/LoadingSpinner';
import {
  User as UserIcon,
  Mail,
  Phone,
  Building,
  GraduationCap,
  Calendar,
  Clock,
  CheckCircle2,
  AlertCircle,
} from 'lucide-react';

interface UserProfileModalProps {
  isOpen: boolean;
  onClose: () => void;
}

export const UserProfileModal: React.FC<UserProfileModalProps> = ({ isOpen, onClose }) => {
  const { user, setUser } = useAuth();
  const [profile, setProfile] = useState<User | null>(user);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // Form Fields
  const [fullName, setFullName] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [bio, setBio] = useState('');
  const [avatarUrl, setAvatarUrl] = useState('');
  const [designation, setDesignation] = useState('');
  const [officeHours, setOfficeHours] = useState('');

  const loadProfile = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await authService.getProfile();
      setProfile(data);
      setFullName(data.fullName || '');
      setPhoneNumber(data.phoneNumber || '');
      setBio(data.bio || '');
      setAvatarUrl(data.avatarUrl || '');
      setDesignation(data.designation || '');
      setOfficeHours(data.officeHours || '');
    } catch {
      // Fallback to auth context user
      if (user) {
        setFullName(user.fullName || '');
        setPhoneNumber(user.phoneNumber || '');
        setBio(user.bio || '');
        setAvatarUrl(user.avatarUrl || '');
        setDesignation(user.designation || '');
        setOfficeHours(user.officeHours || '');
      }
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (isOpen) {
      setSuccessMessage(null);
      setError(null);
      loadProfile();
    }
  }, [isOpen]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    setError(null);
    setSuccessMessage(null);

    const payload: ProfileUpdateRequest = {
      fullName: fullName.trim(),
      phoneNumber: phoneNumber.trim() || undefined,
      bio: bio.trim() || undefined,
      avatarUrl: avatarUrl.trim() || undefined,
      designation: designation.trim() || undefined,
      officeHours: officeHours.trim() || undefined,
    };

    try {
      const updated = await authService.updateProfile(payload);
      setProfile(updated);
      setUser(updated);
      setSuccessMessage('Profile metadata updated successfully.');
      setTimeout(() => {
        setSuccessMessage(null);
      }, 3000);
    } catch (err: unknown) {
      const errorObj = err as { message?: string };
      setError(errorObj.message || 'Failed to update profile.');
    } finally {
      setSaving(false);
    }
  };

  const getRoleBadgeVariant = (role?: string) => {
    switch (role) {
      case 'ROLE_ADMIN':
        return 'danger';
      case 'ROLE_FACULTY':
        return 'primary';
      default:
        return 'success';
    }
  };

  const formatRole = (role?: string) => {
    switch (role) {
      case 'ROLE_ADMIN':
        return 'Administrator';
      case 'ROLE_FACULTY':
        return 'Faculty Member';
      default:
        return 'Student';
    }
  };

  return (
    <Modal isOpen={isOpen} onClose={onClose} title="Academic User Profile" maxWidth="lg">
      <div className="p-6 space-y-6">
        {loading ? (
          <div className="py-12 flex justify-center">
            <LoadingSpinner size="lg" label="Loading verified profile..." />
          </div>
        ) : (
          <>
            {/* Header / Identity Banner */}
            <div className="p-4 rounded-2xl bg-slate-50 dark:bg-slate-800/80 border border-slate-200 dark:border-slate-700/80 flex flex-col sm:flex-row items-center sm:items-start gap-4">
              <div className="w-16 h-16 rounded-2xl bg-gradient-to-tr from-indigo-600 to-primary-600 text-white flex items-center justify-center font-bold text-xl shadow-md shrink-0">
                {profile?.fullName ? profile.fullName.charAt(0).toUpperCase() : <UserIcon className="w-8 h-8" />}
              </div>

              <div className="flex-1 text-center sm:text-left space-y-1">
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                  <h3 className="text-lg font-bold text-slate-900 dark:text-white">
                    {profile?.fullName}
                  </h3>
                  <div className="flex items-center justify-center sm:justify-end gap-2">
                    <Badge variant={getRoleBadgeVariant(profile?.role)} size="sm">
                      {formatRole(profile?.role)}
                    </Badge>
                    {profile?.status && (
                      <span className="text-[10px] font-semibold uppercase tracking-wider text-emerald-600 dark:text-emerald-400">
                        • {profile.status}
                      </span>
                    )}
                  </div>
                </div>

                <p className="text-xs text-slate-500 dark:text-slate-400 flex items-center justify-center sm:justify-start gap-1.5">
                  <Mail className="w-3.5 h-3.5" />
                  {profile?.email}
                </p>

                {/* Academic Metadata Grid */}
                <div className="pt-2 flex flex-wrap gap-x-4 gap-y-1 text-[11px] text-slate-600 dark:text-slate-300">
                  {profile?.rollNumber && (
                    <span className="flex items-center gap-1 font-mono">
                      <GraduationCap className="w-3.5 h-3.5 text-primary-500" />
                      Roll: {profile.rollNumber}
                    </span>
                  )}
                  {profile?.employeeId && (
                    <span className="flex items-center gap-1 font-mono">
                      <Building className="w-3.5 h-3.5 text-primary-500" />
                      Emp ID: {profile.employeeId}
                    </span>
                  )}
                  {profile?.courseName && (
                    <span className="flex items-center gap-1">
                      <GraduationCap className="w-3.5 h-3.5 text-primary-500" />
                      Course: {profile.courseName} (Year {profile.currentYear || 1})
                    </span>
                  )}
                  {profile?.department && (
                    <span className="flex items-center gap-1">
                      <Building className="w-3.5 h-3.5 text-primary-500" />
                      Dept: {profile.department}
                    </span>
                  )}
                  {profile?.createdAt && (
                    <span className="flex items-center gap-1 text-slate-400">
                      <Calendar className="w-3.5 h-3.5" />
                      Enrolled: {new Date(profile.createdAt).toLocaleDateString()}
                    </span>
                  )}
                </div>
              </div>
            </div>

            {/* Notifications */}
            {successMessage && (
              <div className="p-3 bg-emerald-50 dark:bg-emerald-950/50 text-emerald-800 dark:text-emerald-200 text-xs rounded-xl flex items-center gap-2 border border-emerald-200 dark:border-emerald-800">
                <CheckCircle2 className="w-4 h-4 flex-shrink-0" />
                <span>{successMessage}</span>
              </div>
            )}

            {error && (
              <div className="p-3 bg-rose-50 dark:bg-rose-950/50 text-rose-800 dark:text-rose-200 text-xs rounded-xl flex items-center gap-2 border border-rose-200 dark:border-rose-800">
                <AlertCircle className="w-4 h-4 flex-shrink-0" />
                <span>{error}</span>
              </div>
            )}

            {/* Editable Profile Form */}
            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <Input
                  label="Full Academic Name"
                  value={fullName}
                  onChange={(e) => setFullName(e.target.value)}
                  placeholder="e.g. Dr. Jeevan Kumar"
                  icon={<UserIcon className="w-4 h-4" />}
                  required
                />

                <Input
                  label="Contact Phone"
                  value={phoneNumber}
                  onChange={(e) => setPhoneNumber(e.target.value)}
                  placeholder="e.g. +91 9876543210"
                  icon={<Phone className="w-4 h-4" />}
                />
              </div>

              {profile?.role === 'ROLE_FACULTY' && (
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <Input
                    label="Academic Designation"
                    value={designation}
                    onChange={(e) => setDesignation(e.target.value)}
                    placeholder="e.g. Associate Professor"
                    icon={<Building className="w-4 h-4" />}
                  />

                  <Input
                    label="Office Hours / Consultation Schedule"
                    value={officeHours}
                    onChange={(e) => setOfficeHours(e.target.value)}
                    placeholder="e.g. Mon-Wed 2:00 PM - 4:00 PM"
                    icon={<Clock className="w-4 h-4" />}
                  />
                </div>
              )}

              <Textarea
                label="Academic Bio & Research Interests"
                value={bio}
                onChange={(e) => setBio(e.target.value)}
                placeholder="Share your academic background, areas of study, or pedagogical interests..."
                rows={3}
              />

              <div className="flex justify-end gap-2 pt-2 border-t border-slate-100 dark:border-slate-800">
                <Button type="button" variant="outline" size="sm" onClick={onClose} disabled={saving}>
                  Cancel
                </Button>
                <Button type="submit" variant="primary" size="sm" isLoading={saving}>
                  Save Profile Changes
                </Button>
              </div>
            </form>
          </>
        )}
      </div>
    </Modal>
  );
};
