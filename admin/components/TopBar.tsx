import React from 'react';
import { Menu, Moon, Sun } from 'lucide-react';
import type { AdminSectionId } from '../types/admin';

const SECTION_TITLES: Record<AdminSectionId, string> = {
  overview: 'Overview',
  academic: 'Academic Management',
  classes: 'Classes (9–12)',
  subjects: 'Subjects',
  chapters: 'Chapters',
  'content-studio': 'Content Studio',
  'ai-submissions': 'AI Agent Submissions',
  'review-queue': 'Review & Approval Queue',
  'pdf-library': 'PDF Library',
  students: 'Students',
  feedback: 'Feedback & Error Reports',
  analytics: 'Analytics',
  infrastructure: 'Infrastructure Monitoring',
  capacity: 'Capacity & Quotas',
  settings: 'Security & Settings',
};

interface TopBarProps {
  activeSection: AdminSectionId;
  isDark: boolean;
  onToggleTheme: () => void;
  onOpenMobileMenu: () => void;
  onQuickAction: () => void;
}

export function TopBar({
  activeSection,
  isDark,
  onToggleTheme,
  onOpenMobileMenu,
  onQuickAction,
}: TopBarProps) {
  return (
    <header className="sticky top-0 z-30 h-16 flex items-center justify-between gap-4 px-4 sm:px-6 lg:px-8 border-b border-[#CBD8EE] bg-white/90 backdrop-blur-md dark:border-[#1E3566] dark:bg-[#091530]/90">
      <div className="flex items-center gap-3 min-w-0">
        <button
          type="button"
          onClick={onOpenMobileMenu}
          aria-label="Open navigation menu"
          className="lg:hidden min-h-[40px] min-w-[40px] flex items-center justify-center rounded-lg border border-[#CBD8EE] text-[#1E293B] hover:bg-[#EDF2FA] dark:border-[#24407A] dark:text-[#E2ECFC] dark:hover:bg-[#13264D] focus-visible:outline-2 focus-visible:outline-[#3B82F6]"
        >
          <Menu className="h-5 w-5" />
        </button>
        <div className="min-w-0 flex items-center gap-2 text-sm">
          <span className="hidden sm:inline text-[#64748B] dark:text-[#8FA7D1] whitespace-nowrap">
            Imtihan Pro
          </span>
          <span className="hidden sm:inline text-[#94A3B8] dark:text-[#4F6B9E]" aria-hidden="true">
            /
          </span>
          <h1 className="font-display font-semibold text-base text-[#0F172A] dark:text-[#F0F6FF] truncate">
            {SECTION_TITLES[activeSection]}
          </h1>
        </div>
      </div>

      <div className="flex items-center gap-3 shrink-0">
        <button
          type="button"
          onClick={onToggleTheme}
          aria-label={isDark ? 'Switch to light theme' : 'Switch to dark theme'}
          className="min-h-[40px] px-3 flex items-center gap-2 rounded-lg border border-[#CBD8EE] bg-[#F8FAFD] text-xs font-medium text-[#1E293B] hover:bg-[#EDF2FA] dark:border-[#24407A] dark:bg-[#102145] dark:text-[#DCE8FC] dark:hover:bg-[#172D5C] transition-colors whitespace-nowrap shrink-0 focus-visible:outline-2 focus-visible:outline-[#3B82F6]"
        >
          {isDark ? <Sun className="h-4 w-4 text-[#FBBF24]" /> : <Moon className="h-4 w-4 text-[#1D4ED8]" />}
          <span className="hidden sm:inline">{isDark ? 'Light' : 'Dark'}</span>
        </button>

        <button
          type="button"
          onClick={onQuickAction}
          className="min-h-[40px] px-3.5 sm:px-4 rounded-lg bg-[#1D4ED8] hover:bg-[#1E40AF] dark:bg-[#2563EB] dark:hover:bg-[#3B82F6] text-white text-xs font-semibold transition-colors whitespace-nowrap shrink-0 focus-visible:outline-2 focus-visible:outline-[#60A5FA]"
        >
          <span className="sm:hidden">Studio</span>
          <span className="hidden sm:inline">Open Content Studio</span>
        </button>
      </div>
    </header>
  );
}
