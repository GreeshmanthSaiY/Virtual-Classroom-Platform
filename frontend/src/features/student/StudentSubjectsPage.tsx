import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { studentService } from '@/core/services/studentService';
import { Subject } from '@/types';
import { BookOpen, Video, FileCheck, MessageSquare, BookMarked, Search } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '@/shared/components/Card';
import { Button } from '@/shared/components/Button';
import { Badge } from '@/shared/components/Badge';
import { LoadingSpinner } from '@/shared/components/LoadingSpinner';
import { Input } from '@/shared/components/Input';
import { Breadcrumbs } from '@/shared/components/Breadcrumbs';

export const StudentSubjectsPage: React.FC = () => {
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');

  useEffect(() => {
    const fetchSubjects = async () => {
      setLoading(true);
      try {
        const data = await studentService.getEnrolledSubjects();
        setSubjects(data || []);
      } catch (err) {
        console.error('Failed to load student subjects', err);
      } finally {
        setLoading(false);
      }
    };
    fetchSubjects();
  }, []);

  const filteredSubjects = subjects.filter(
    (s) =>
      s.title.toLowerCase().includes(searchTerm.toLowerCase()) ||
      s.code.toLowerCase().includes(searchTerm.toLowerCase()) ||
      (s.courseName && s.courseName.toLowerCase().includes(searchTerm.toLowerCase()))
  );

  return (
    <div className="space-y-6 sm:space-y-8 animate-fade-in">
      <Breadcrumbs
        items={[
          { label: 'Student Portal', href: '/student' },
          { label: 'My Enrolled Subjects' },
        ]}
      />

      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <div className="flex items-center gap-2">
            <div className="p-2 rounded-xl bg-emerald-50 dark:bg-emerald-950/60 text-emerald-600 dark:text-emerald-400">
              <BookMarked className="w-5 h-5" />
            </div>
            <h1 className="text-2xl sm:text-3xl font-extrabold tracking-tight text-slate-900 dark:text-white">
              My Enrolled Academic Subjects
            </h1>
          </div>
          <p className="text-xs sm:text-sm text-slate-500 dark:text-slate-400 mt-1.5">
            Curriculum subjects and digital resources in your active degree program
          </p>
        </div>

        {subjects.length > 0 && (
          <div className="w-full sm:w-64">
            <Input
              placeholder="Search enrolled subjects..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              icon={<Search className="w-4 h-4 text-slate-400" />}
            />
          </div>
        )}
      </div>

      {loading ? (
        <div className="p-16 flex justify-center">
          <LoadingSpinner size="md" label="Loading enrolled academic subjects..." />
        </div>
      ) : subjects.length === 0 ? (
        <Card className="border-slate-200/80 dark:border-slate-800/80 shadow-soft-sm">
          <CardContent className="p-16 text-center space-y-3">
            <div className="w-12 h-12 rounded-2xl bg-emerald-50 dark:bg-emerald-950/60 text-emerald-600 dark:text-emerald-400 flex items-center justify-center mx-auto">
              <BookMarked className="w-6 h-6" />
            </div>
            <h4 className="text-sm font-semibold text-slate-800 dark:text-slate-200">
              No Subjects Currently Enrolled
            </h4>
            <p className="text-xs text-slate-400">
              You are not registered in any subjects for this semester yet.
            </p>
          </CardContent>
        </Card>
      ) : filteredSubjects.length === 0 ? (
        <Card className="border-slate-200/80 dark:border-slate-800/80 shadow-soft-sm">
          <CardContent className="p-12 text-center text-xs text-slate-400">
            No subjects match your search query &quot;{searchTerm}&quot;.
          </CardContent>
        </Card>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5">
          {filteredSubjects.map((sub) => (
            <Card
              key={sub.id}
              hover
              className="border-slate-200/80 dark:border-slate-800/80 shadow-soft-sm bg-white/90 dark:bg-slate-900/90 backdrop-blur-sm flex flex-col justify-between overflow-hidden"
            >
              <CardHeader className="p-5 pb-3">
                <div className="flex items-center justify-between">
                  <span className="font-mono text-xs font-bold text-emerald-600 dark:text-emerald-400 bg-emerald-50 dark:bg-emerald-950/60 px-2.5 py-1 rounded-lg">
                    {sub.code}
                  </span>
                  <Badge variant="neutral" size="sm">
                    Year {sub.yearLevel} Level
                  </Badge>
                </div>
                <CardTitle className="text-base sm:text-lg font-bold text-slate-900 dark:text-white mt-3 line-clamp-1">
                  {sub.title}
                </CardTitle>
                <p className="text-xs text-slate-500 dark:text-slate-400 mt-1 line-clamp-1">
                  Program: {sub.courseName || 'Core Curriculum'}
                </p>
              </CardHeader>

              <CardContent className="p-5 pt-3">
                <div className="pt-3 border-t border-slate-100 dark:border-slate-800/80 grid grid-cols-2 gap-2">
                  <Link to="/materials">
                    <Button variant="outline" size="sm" className="w-full text-xs shadow-soft-sm">
                      <BookOpen className="w-3.5 h-3.5 mr-1 text-slate-400" /> Materials
                    </Button>
                  </Link>
                  <Link to="/lectures">
                    <Button variant="outline" size="sm" className="w-full text-xs shadow-soft-sm">
                      <Video className="w-3.5 h-3.5 mr-1 text-slate-400" /> Lectures
                    </Button>
                  </Link>
                  <Link to="/student/assignments">
                    <Button variant="outline" size="sm" className="w-full text-xs shadow-soft-sm">
                      <FileCheck className="w-3.5 h-3.5 mr-1 text-slate-400" /> Work
                    </Button>
                  </Link>
                  <Link to="/forum">
                    <Button variant="outline" size="sm" className="w-full text-xs shadow-soft-sm">
                      <MessageSquare className="w-3.5 h-3.5 mr-1 text-slate-400" /> Q&A Forum
                    </Button>
                  </Link>
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
};
