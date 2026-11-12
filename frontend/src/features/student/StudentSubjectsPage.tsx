import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { studentService } from '@/core/services/studentService';
import { Subject } from '@/types';
import { BookOpen, Video, FileCheck, MessageSquare } from 'lucide-react';
import { Card, CardHeader, CardTitle, CardContent } from '@/shared/components/Card';
import { Button } from '@/shared/components/Button';
import { Badge } from '@/shared/components/Badge';
import { LoadingSpinner } from '@/shared/components/LoadingSpinner';
import { Breadcrumbs } from '@/shared/components/Breadcrumbs';

export const StudentSubjectsPage: React.FC = () => {
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [loading, setLoading] = useState(true);

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

  return (
    <div className="space-y-6">
      <Breadcrumbs
        items={[
          { label: 'Student Portal', href: '/student' },
          { label: 'My Enrolled Subjects' },
        ]}
      />

      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold tracking-tight text-slate-900 dark:text-white">
            My Enrolled Academic Subjects
          </h1>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-1">
            Official curriculum subjects and resources for your academic degree program
          </p>
        </div>
      </div>

      {loading ? (
        <div className="p-12 flex justify-center">
          <LoadingSpinner size="md" label="Loading enrolled academic subjects..." />
        </div>
      ) : subjects.length === 0 ? (
        <Card>
          <CardContent className="p-12 text-center text-xs text-slate-400">
            No subjects currently associated with your academic enrollment.
          </CardContent>
        </Card>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {subjects.map((sub) => (
            <Card key={sub.id} hover className="flex flex-col justify-between">
              <CardHeader className="pb-3">
                <div className="flex items-center justify-between">
                  <Badge variant="primary" size="sm">
                    {sub.code}
                  </Badge>
                  <span className="text-xs font-semibold text-slate-500">Year {sub.yearLevel}</span>
                </div>
                <CardTitle className="text-base mt-2">{sub.title}</CardTitle>
                <p className="text-xs text-slate-500 mt-0.5">{sub.courseName || 'Core Curriculum'}</p>
              </CardHeader>

              <CardContent className="pt-2 border-t border-slate-100 dark:border-slate-800 grid grid-cols-2 gap-2">
                <Link to="/materials">
                  <Button variant="outline" size="sm" className="w-full text-xs">
                    <BookOpen className="w-3.5 h-3.5 mr-1" /> Materials
                  </Button>
                </Link>
                <Link to="/lectures">
                  <Button variant="outline" size="sm" className="w-full text-xs">
                    <Video className="w-3.5 h-3.5 mr-1" /> Lectures
                  </Button>
                </Link>
                <Link to="/student/assignments">
                  <Button variant="outline" size="sm" className="w-full text-xs">
                    <FileCheck className="w-3.5 h-3.5 mr-1" /> Work
                  </Button>
                </Link>
                <Link to="/forum">
                  <Button variant="outline" size="sm" className="w-full text-xs">
                    <MessageSquare className="w-3.5 h-3.5 mr-1" /> Forum
                  </Button>
                </Link>
              </CardContent>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
};
