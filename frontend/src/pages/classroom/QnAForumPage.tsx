import React, { useState, useEffect } from 'react';
import { useAuth } from '../../context/AuthContext';
import { apiClient } from '../../api/client';
import { ApiResponse, Question, Subject, Answer } from '../../types';
import { Badge } from '../../components/common/Badge';
import { LoadingSpinner } from '../../components/common/LoadingSpinner';
import { Modal } from '../../components/common/Modal';
import {
  MessageSquare,
  Search,
  PlusCircle,
  CheckCircle2,
  User,
  Send,
  HelpCircle,
  Filter,
} from 'lucide-react';

export const QnAForumPage: React.FC = () => {
  const { user, isAuthenticated } = useAuth();
  const [questions, setQuestions] = useState<Question[]>([]);
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedSubject, setSelectedSubject] = useState('ALL');

  // Question Modal
  const [askModalOpen, setAskModalOpen] = useState(false);
  const [newQuestion, setNewQuestion] = useState({
    subjectId: 1,
    title: '',
    body: '',
  });

  // Active Question Detail
  const [activeQuestion, setActiveQuestion] = useState<Question | null>(null);
  const [answers, setAnswers] = useState<Answer[]>([]);
  const [newAnswerBody, setNewAnswerBody] = useState('');
  const [submittingAnswer, setSubmittingAnswer] = useState(false);

  const loadForumData = async () => {
    setLoading(true);
    try {
      const [qRes, sRes] = await Promise.all([
        apiClient.get<ApiResponse<Question[]>>('/forum/questions'),
        apiClient.get<ApiResponse<Subject[]>>('/students/subjects').catch(() =>
          apiClient.get<ApiResponse<Subject[]>>('/faculty/subjects')
        ),
      ]);

      if (qRes.data.success) setQuestions(qRes.data.data);
      if (sRes.data.success) setSubjects(sRes.data.data);
    } catch (err) {
      console.error('Failed to load forum', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadForumData();
  }, []);

  const handleOpenThread = async (q: Question) => {
    setActiveQuestion(q);
    try {
      const res = await apiClient.get<ApiResponse<Question>>(`/forum/questions/${q.id}`);
      if (res.data.success && res.data.data) {
        setAnswers(res.data.data.answers || []);
      }
    } catch (err) {
      console.error('Failed to load answers', err);
    }
  };

  const handleAskQuestion = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await apiClient.post('/forum/questions', newQuestion);
      setAskModalOpen(false);
      setNewQuestion({ subjectId: subjects[0]?.id || 1, title: '', body: '' });
      await loadForumData();
    } catch (err) {
      console.error('Failed to post question', err);
    }
  };

  const handlePostAnswer = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!activeQuestion || !newAnswerBody.trim()) return;
    setSubmittingAnswer(true);
    try {
      const res = await apiClient.post<ApiResponse<Answer>>(
        `/forum/questions/${activeQuestion.id}/answers`,
        { body: newAnswerBody }
      );
      if (res.data.success && res.data.data) {
        setAnswers([...answers, res.data.data]);
        setNewAnswerBody('');
      }
    } catch (err) {
      console.error('Failed to post answer', err);
    } finally {
      setSubmittingAnswer(false);
    }
  };

  const handleAcceptAnswer = async (answerId: number) => {
    try {
      await apiClient.patch(`/forum/answers/${answerId}/accept`);
      if (activeQuestion) {
        handleOpenThread(activeQuestion);
      }
    } catch (err) {
      console.error('Failed to accept answer', err);
    }
  };

  const filteredQuestions = questions.filter((q) => {
    const matchesSearch =
      q.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
      q.body.toLowerCase().includes(searchQuery.toLowerCase()) ||
      q.subjectTitle.toLowerCase().includes(searchQuery.toLowerCase());

    const matchesSubject = selectedSubject === 'ALL' || q.subjectId === Number(selectedSubject);
    return matchesSearch && matchesSubject;
  });

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8 space-y-8">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div className="space-y-1">
          <div className="flex items-center gap-2">
            <div className="p-2 rounded-xl bg-emerald-100 dark:bg-emerald-950/80 text-emerald-600 dark:text-emerald-400">
              <MessageSquare className="w-6 h-6" />
            </div>
            <div>
              <h1 className="text-2xl sm:text-3xl font-bold text-slate-900 dark:text-white">
                Academic Q&A Forum
              </h1>
              <p className="text-xs sm:text-sm text-slate-500 dark:text-slate-400">
                Ask questions, discuss topics, and receive peer and faculty verified explanations
              </p>
            </div>
          </div>
        </div>

        {isAuthenticated && (
          <button
            onClick={() => setAskModalOpen(true)}
            className="inline-flex items-center gap-2 px-5 py-2.5 rounded-xl font-semibold text-xs text-white bg-primary-600 hover:bg-primary-700 shadow-sm transition-all"
          >
            <PlusCircle className="w-4 h-4" />
            <span>Ask Question</span>
          </button>
        )}
      </div>

      {/* Filter and Search Bar */}
      <div className="flex flex-col sm:flex-row gap-4">
        <div className="relative flex-1">
          <div className="absolute inset-y-0 left-0 pl-3.5 flex items-center pointer-events-none text-slate-400">
            <Search className="w-4 h-4" />
          </div>
          <input
            type="text"
            placeholder="Search discussion threads, questions, topics..."
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

      {/* Main Forum Layout */}
      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8">
        {/* Questions List (Left/Main) */}
        <div className={`${activeQuestion ? 'lg:col-span-5' : 'lg:col-span-12'} space-y-4`}>
          {loading ? (
            <div className="py-16">
              <LoadingSpinner size="lg" label="Loading discussion threads..." />
            </div>
          ) : filteredQuestions.length === 0 ? (
            <div className="p-12 text-center bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 space-y-3">
              <HelpCircle className="w-12 h-12 text-slate-300 dark:text-slate-600 mx-auto" />
              <h3 className="text-base font-semibold text-slate-800 dark:text-slate-200">
                No Questions Found
              </h3>
              <p className="text-xs text-slate-500 max-w-sm mx-auto">
                Be the first to ask a question in this subject or adjust your search filter!
              </p>
            </div>
          ) : (
            filteredQuestions.map((q) => (
              <div
                key={q.id}
                onClick={() => handleOpenThread(q)}
                className={`p-5 rounded-2xl border cursor-pointer transition-all space-y-3 ${
                  activeQuestion?.id === q.id
                    ? 'border-primary-500 bg-primary-50/20 dark:bg-primary-950/20 shadow-sm'
                    : 'border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-900 hover:border-slate-300 dark:hover:border-slate-700'
                }`}
              >
                <div className="flex items-center justify-between">
                  <Badge variant="primary">{q.subjectTitle}</Badge>
                  {q.isResolved && (
                    <Badge variant="success" size="sm" className="gap-1">
                      <CheckCircle2 className="w-3 h-3" /> Resolved
                    </Badge>
                  )}
                </div>

                <h3 className="font-bold text-slate-900 dark:text-white text-sm line-clamp-2">
                  {q.title}
                </h3>

                <p className="text-xs text-slate-500 dark:text-slate-400 line-clamp-2">
                  {q.body}
                </p>

                <div className="pt-2 border-t border-slate-100 dark:border-slate-800 flex items-center justify-between text-xs text-slate-400">
                  <div className="flex items-center gap-1.5">
                    <User className="w-3.5 h-3.5" />
                    <span>{q.authorName}</span>
                  </div>
                  <div className="flex items-center gap-1 font-semibold text-slate-600 dark:text-slate-300">
                    <MessageSquare className="w-3.5 h-3.5" />
                    <span>{q.answersCount} answers</span>
                  </div>
                </div>
              </div>
            ))
          )}
        </div>

        {/* Selected Thread View (Right Pane) */}
        {activeQuestion && (
          <div className="lg:col-span-7 bg-white dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-800 p-6 space-y-6 shadow-sm">
            {/* Question Details */}
            <div className="space-y-4 border-b border-slate-100 dark:border-slate-800 pb-6">
              <div className="flex items-center justify-between">
                <Badge variant="primary">{activeQuestion.subjectTitle}</Badge>
                <button
                  onClick={() => setActiveQuestion(null)}
                  className="text-xs text-slate-400 hover:text-slate-600"
                >
                  Close Thread
                </button>
              </div>

              <h2 className="text-xl font-bold text-slate-900 dark:text-white">
                {activeQuestion.title}
              </h2>

              <p className="text-sm text-slate-700 dark:text-slate-300 leading-relaxed whitespace-pre-line">
                {activeQuestion.body}
              </p>

              <div className="flex items-center gap-2 text-xs text-slate-400">
                <span>Asked by <strong className="text-slate-700 dark:text-slate-300">{activeQuestion.authorName}</strong></span>
              </div>
            </div>

            {/* Answers Section */}
            <div className="space-y-4">
              <h3 className="text-sm font-bold uppercase tracking-wider text-slate-500 dark:text-slate-400">
                Responses ({answers.length})
              </h3>

              {answers.length === 0 ? (
                <p className="text-xs text-slate-400 italic py-4">No answers posted yet. Be the first to explain!</p>
              ) : (
                <div className="space-y-3">
                  {answers.map((ans) => (
                    <div
                      key={ans.id}
                      className={`p-4 rounded-xl border space-y-2 ${
                        ans.isAccepted
                          ? 'border-emerald-300 bg-emerald-50/30 dark:border-emerald-800 dark:bg-emerald-950/20'
                          : 'border-slate-200 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-800/40'
                      }`}
                    >
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-2 text-xs">
                          <span className="font-semibold text-slate-900 dark:text-white">{ans.authorName}</span>
                          <Badge variant={ans.authorRole === 'ROLE_FACULTY' ? 'primary' : 'neutral'} size="sm">
                            {ans.authorRole.replace('ROLE_', '')}
                          </Badge>
                        </div>
                        {ans.isAccepted && (
                          <span className="inline-flex items-center gap-1 text-[11px] font-semibold text-emerald-600 dark:text-emerald-400">
                            <CheckCircle2 className="w-3.5 h-3.5" /> Accepted Answer
                          </span>
                        )}
                      </div>

                      <p className="text-xs text-slate-700 dark:text-slate-300 leading-relaxed whitespace-pre-line">
                        {ans.body}
                      </p>

                      {user && (user.role === 'ROLE_FACULTY' || user.id === activeQuestion.authorId) && !ans.isAccepted && (
                        <div className="pt-2 flex justify-end">
                          <button
                            onClick={() => handleAcceptAnswer(ans.id)}
                            className="text-[11px] font-semibold text-emerald-600 hover:text-emerald-700 inline-flex items-center gap-1"
                          >
                            <CheckCircle2 className="w-3 h-3" /> Mark as Accepted
                          </button>
                        </div>
                      )}
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* Answer Input */}
            {isAuthenticated ? (
              <form onSubmit={handlePostAnswer} className="space-y-3 pt-4 border-t border-slate-100 dark:border-slate-800">
                <textarea
                  rows={3}
                  required
                  value={newAnswerBody}
                  onChange={(e) => setNewAnswerBody(e.target.value)}
                  placeholder="Write your academic explanation or answer here..."
                  className="w-full p-3 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white text-xs focus:ring-2 focus:ring-primary-500"
                />
                <div className="flex justify-end">
                  <button
                    type="submit"
                    disabled={submittingAnswer || !newAnswerBody.trim()}
                    className="inline-flex items-center gap-1.5 px-4 py-2 rounded-xl font-semibold text-xs text-white bg-primary-600 hover:bg-primary-700 disabled:opacity-50"
                  >
                    <Send className="w-3.5 h-3.5" />
                    <span>Post Answer</span>
                  </button>
                </div>
              </form>
            ) : (
              <p className="text-xs text-slate-400 text-center py-2">
                Sign in to post an answer to this thread.
              </p>
            )}
          </div>
        )}
      </div>

      {/* Ask Question Modal */}
      <Modal
        isOpen={askModalOpen}
        onClose={() => setAskModalOpen(false)}
        title="Post Academic Question"
      >
        <form onSubmit={handleAskQuestion} className="space-y-4 text-xs">
          <div>
            <label className="block font-semibold uppercase tracking-wider text-slate-700 dark:text-slate-300 mb-1">
              Subject
            </label>
            <select
              value={newQuestion.subjectId}
              onChange={(e) => setNewQuestion({ ...newQuestion, subjectId: Number(e.target.value) })}
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
              Question Title
            </label>
            <input
              type="text"
              required
              value={newQuestion.title}
              onChange={(e) => setNewQuestion({ ...newQuestion, title: e.target.value })}
              placeholder="e.g. How to maintain balance in Red-Black tree deletion?"
              className="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white"
            />
          </div>

          <div>
            <label className="block font-semibold uppercase tracking-wider text-slate-700 dark:text-slate-300 mb-1">
              Detailed Question Body
            </label>
            <textarea
              rows={4}
              required
              value={newQuestion.body}
              onChange={(e) => setNewQuestion({ ...newQuestion, body: e.target.value })}
              placeholder="Explain the specific scenario, what you have tried, and code snippets or theorem steps..."
              className="w-full p-2.5 rounded-xl border border-slate-300 dark:border-slate-700 bg-white dark:bg-slate-800 text-slate-900 dark:text-white"
            />
          </div>

          <div className="flex justify-end gap-2 pt-2">
            <button
              type="button"
              onClick={() => setAskModalOpen(false)}
              className="px-4 py-2 rounded-xl text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800"
            >
              Cancel
            </button>
            <button
              type="submit"
              className="px-4 py-2 rounded-xl font-semibold text-white bg-primary-600 hover:bg-primary-700"
            >
              Post Question
            </button>
          </div>
        </form>
      </Modal>
    </div>
  );
};

