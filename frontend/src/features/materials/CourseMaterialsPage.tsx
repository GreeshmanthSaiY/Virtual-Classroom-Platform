import React, { useState, useEffect } from 'react';
import { apiClient } from '../../api/client';
import { ApiResponse, StudyMaterial, Subject } from '../../types';
import { Badge } from '../../components/common/Badge';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';
import {
  BookOpen,
  Search,
  Download,
  FileText,
  User,
  Filter,
} from 'lucide-react';

export const CourseMaterialsPage: React.FC = () => {
  const [materials, setMaterials] = useState<StudyMaterial[]>([]);
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [selectedSubject, setSelectedSubject] = useState<string>('ALL');

  useEffect(() => {
    const fetchData = async () => {
      setLoading(true);
      try {
        const [matRes, subRes] = await Promise.all([
          apiClient.get<ApiResponse<StudyMaterial[]>>('/students/materials').catch(() =>
            apiClient.get<ApiResponse<StudyMaterial[]>>('/faculty/materials')
          ),
          apiClient.get<ApiResponse<Subject[]>>('/students/subjects').catch(() =>
            apiClient.get<ApiResponse<Subject[]>>('/faculty/subjects')
          ),
        ]);

        if (matRes.data.success) setMaterials(matRes.data.data);
        if (subRes.data.success) setSubjects(subRes.data.data);
      } catch (err) {
        console.error('Failed to load course materials', err);
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, []);

  const filteredMaterials = materials.filter((m) => {
    const matchesSearch =
      m.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (m.description && m.description.toLowerCase().includes(searchQuery.toLowerCase())) ||
      m.subjectTitle.toLowerCase().includes(searchQuery.toLowerCase());

    const matchesSubject = selectedSubject === 'ALL' || m.subjectId === Number(selectedSubject);

    return matchesSearch && matchesSubject;
  });

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      {/* Header */}
      <div className="space-y-2">
        <div className="flex items-center gap-2">
          <div className="p-2 rounded-xl bg-primary-100 dark:bg-primary-950/80 text-primary-600 dark:text-primary-400">
            <BookOpen className="w-6 h-6" />
          </div>
          <div>
            <h1 className="text-2xl sm:text-3xl font-bold text-slate-900 dark:text-white">
              Academic Course Materials
            </h1>
            <p className="text-xs sm:text-sm text-slate-500 dark:text-slate-400">
              Verified lecture slides, syllabus reference documents, and laboratory workbooks
            </p>
          </div>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div className="flex flex-col sm:flex-row gap-4">
        <div className="relative flex-1">
          <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
            <Search className="w-4 h-4" />
          </div>
          <input
            type="text"
            placeholder="Search notes, chapters, topics..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-10 pr-4 py-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 shadow-sm"
          />
        </div>

        <div className="flex items-center gap-2">
          <Filter className="w-4 h-4 text-slate-400 shrink-0" />
          <select
            value={selectedSubject}
            onChange={(e) => setSelectedSubject(e.target.value)}
            className="px-3.5 py-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white text-sm focus:outline-none focus:ring-2 focus:ring-primary-500 shadow-sm"
          >
            <option value="ALL">All Enrolled Subjects</option>
            {subjects.map((sub) => (
              <option key={sub.id} value={sub.id}>
                {sub.code} - {sub.title}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Grid of Materials */}
      {loading ? (
        <div className="py-20">
          <LoadingSpinner size="lg" label="Retrieving subject documents..." />
        </div>
      ) : filteredMaterials.length === 0 ? (
        <div className="p-12 text-center bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 space-y-3">
          <FileText className="w-12 h-12 text-slate-300 dark:text-slate-600 mx-auto" />
          <h3 className="text-base font-semibold text-slate-800 dark:text-slate-200">
            No Course Materials Found
          </h3>
          <p className="text-xs text-slate-500 max-w-sm mx-auto">
            Try adjusting your search terms or filter to see documents published by instructors.
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {filteredMaterials.map((mat) => (
            <div
              key={mat.id}
              className="p-6 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-sm hover:shadow-md transition-shadow flex flex-col justify-between space-y-4"
            >
              <div className="space-y-3">
                <div className="flex items-center justify-between">
                  <Badge variant="primary">{mat.subjectTitle}</Badge>
                  <span className="text-[11px] font-mono text-slate-400">
                    {(mat.fileSizeBytes / (1024 * 1024)).toFixed(1)} MB
                  </span>
                </div>

                <h3 className="font-bold text-slate-900 dark:text-white text-base line-clamp-2">
                  {mat.title}
                </h3>

                {mat.description && (
                  <p className="text-xs text-slate-500 dark:text-slate-400 line-clamp-3 leading-relaxed">
                    {mat.description}
                  </p>
                )}
              </div>

              <div className="pt-4 border-t border-slate-100 dark:border-slate-800 flex items-center justify-between text-xs text-slate-500">
                <div className="flex items-center gap-1.5 truncate">
                  <User className="w-3.5 h-3.5 text-slate-400" />
                  <span className="truncate">{mat.uploaderName}</span>
                </div>

                <a
                  href={mat.fileUrl}
                  target="_blank"
                  rel="noreferrer"
                  className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg font-semibold text-white bg-primary-600 hover:bg-primary-700 shadow-sm transition-all"
                >
                  <Download className="w-3.5 h-3.5" />
                  <span>Download</span>
                </a>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
