import React, { useState, useEffect } from 'react';
import { apiClient } from '../../api/client';
import { ApiResponse, VideoLecture, Subject } from '../../types';
import { Badge } from '../../components/common/Badge';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';
import {
  Video,
  Search,
  Play,
  Clock,
  User,
  Filter,
} from 'lucide-react';

export const VideoLecturesPage: React.FC = () => {
  const [lectures, setLectures] = useState<VideoLecture[]>([]);
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [selectedSubject, setSelectedSubject] = useState<string>('ALL');
  const [activeVideo, setActiveVideo] = useState<VideoLecture | null>(null);

  useEffect(() => {
    const fetchLectures = async () => {
      setLoading(true);
      try {
        const [lecRes, subRes] = await Promise.all([
          apiClient.get<ApiResponse<VideoLecture[]>>('/students/lectures').catch(() =>
            apiClient.get<ApiResponse<VideoLecture[]>>('/faculty/lectures')
          ),
          apiClient.get<ApiResponse<Subject[]>>('/students/subjects').catch(() =>
            apiClient.get<ApiResponse<Subject[]>>('/faculty/subjects')
          ),
        ]);

        if (lecRes.data.success) setLectures(lecRes.data.data);
        if (subRes.data.success) setSubjects(subRes.data.data);
      } catch (err) {
        console.error('Failed to load video lectures', err);
      } finally {
        setLoading(false);
      }
    };

    fetchLectures();
  }, []);

  const filteredLectures = lectures.filter((l) => {
    const matchesSearch =
      l.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (l.description && l.description.toLowerCase().includes(searchQuery.toLowerCase())) ||
      l.subjectTitle.toLowerCase().includes(searchQuery.toLowerCase());

    const matchesSubject = selectedSubject === 'ALL' || l.subjectId === Number(selectedSubject);

    return matchesSearch && matchesSubject;
  });

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      {/* Header */}
      <div className="space-y-2">
        <div className="flex items-center gap-2">
          <div className="p-2 rounded-xl bg-indigo-100 dark:bg-indigo-950/80 text-indigo-600 dark:text-indigo-400">
            <Video className="w-6 h-6" />
          </div>
          <div>
            <h1 className="text-2xl sm:text-3xl font-bold text-slate-900 dark:text-white">
              Recorded Video Lectures
            </h1>
            <p className="text-xs sm:text-sm text-slate-500 dark:text-slate-400">
              Curriculum sessions taught by approved academic department faculty
            </p>
          </div>
        </div>
      </div>

      {/* Video Player Modal/Display if active */}
      {activeVideo && (
        <div className="p-6 bg-slate-900 rounded-2xl text-white space-y-4 shadow-xl">
          <div className="flex items-center justify-between">
            <div className="space-y-1">
              <Badge variant="primary">{activeVideo.subjectTitle}</Badge>
              <h2 className="text-xl font-bold">{activeVideo.title}</h2>
              <p className="text-xs text-slate-400">Instructor: {activeVideo.facultyName}</p>
            </div>
            <button
              onClick={() => setActiveVideo(null)}
              className="text-xs text-slate-400 hover:text-white px-3 py-1.5 rounded-lg bg-slate-800"
            >
              Close Player
            </button>
          </div>
          <div className="aspect-video w-full rounded-xl overflow-hidden bg-black flex items-center justify-center">
            {activeVideo.videoUrl.includes('youtube') || activeVideo.videoUrl.includes('youtu.be') ? (
              <iframe
                src={activeVideo.videoUrl.replace('watch?v=', 'embed/')}
                title={activeVideo.title}
                className="w-full h-full"
                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                allowFullScreen
              />
            ) : (
              <video controls src={activeVideo.videoUrl} className="w-full h-full" />
            )}
          </div>
        </div>
      )}

      {/* Filter and Search Bar */}
      <div className="flex flex-col sm:flex-row gap-4">
        <div className="relative flex-1">
          <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
            <Search className="w-4 h-4" />
          </div>
          <input
            type="text"
            placeholder="Search recorded lectures, topics..."
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
            <option value="ALL">All Subjects</option>
            {subjects.map((sub) => (
              <option key={sub.id} value={sub.id}>
                {sub.code} - {sub.title}
              </option>
            ))}
          </select>
        </div>
      </div>

      {/* Video Grid */}
      {loading ? (
        <div className="py-20">
          <LoadingSpinner size="lg" label="Loading video sessions..." />
        </div>
      ) : filteredLectures.length === 0 ? (
        <div className="p-12 text-center bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 space-y-3">
          <Video className="w-12 h-12 text-slate-300 dark:text-slate-600 mx-auto" />
          <h3 className="text-base font-semibold text-slate-800 dark:text-slate-200">
            No Video Lectures Found
          </h3>
          <p className="text-xs text-slate-500 max-w-sm mx-auto">
            Try adjusting your search criteria or filter to see videos uploaded by faculty.
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {filteredLectures.map((lec) => (
            <div
              key={lec.id}
              className="p-5 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-sm hover:shadow-md transition-all space-y-4 flex flex-col justify-between"
            >
              <div className="space-y-3">
                {/* Video Thumbnail Placeholder / Card */}
                <div
                  onClick={() => setActiveVideo(lec)}
                  className="relative aspect-video rounded-xl bg-gradient-to-tr from-slate-900 to-indigo-950 flex items-center justify-center cursor-pointer group overflow-hidden"
                >
                  <div className="w-12 h-12 rounded-full bg-white/20 backdrop-blur-sm group-hover:scale-110 flex items-center justify-center text-white transition-transform">
                    <Play className="w-6 h-6 ml-0.5 fill-white" />
                  </div>
                  <div className="absolute bottom-2 right-2 px-2 py-0.5 rounded bg-black/70 text-[10px] font-mono text-white flex items-center gap-1">
                    <Clock className="w-3 h-3" />
                    <span>{Math.floor(lec.durationSeconds / 60)}:00</span>
                  </div>
                </div>

                <div className="space-y-1.5">
                  <Badge variant="primary">{lec.subjectTitle}</Badge>
                  <h3 className="font-bold text-slate-900 dark:text-white text-base line-clamp-2">
                    {lec.title}
                  </h3>
                  {lec.description && (
                    <p className="text-xs text-slate-500 dark:text-slate-400 line-clamp-2">
                      {lec.description}
                    </p>
                  )}
                </div>
              </div>

              <div className="pt-3 border-t border-slate-100 dark:border-slate-800 flex items-center justify-between text-xs text-slate-500">
                <div className="flex items-center gap-1.5 truncate">
                  <User className="w-3.5 h-3.5 text-slate-400" />
                  <span className="truncate">{lec.facultyName}</span>
                </div>
                <button
                  onClick={() => setActiveVideo(lec)}
                  className="font-semibold text-indigo-600 hover:text-indigo-700 flex items-center gap-1"
                >
                  <Play className="w-3 h-3 fill-indigo-600" /> Watch
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
