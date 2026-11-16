import React from 'react';
import {
  BarChart3,
  BookOpen,
  Bot,
  CheckSquare,
  FileText,
  FolderTree,
  Gauge,
  GraduationCap,
  Layers,
  LayoutDashboard,
  LogOut,
  MessageSquareWarning,
  PenTool,
  Server,
  Shield,
  Users,
  X,
} from 'lucide-react';
import type { AdminConnectionMode, AdminSectionId, AdminSessionState } from '../types/admin';

interface NavGroup {
  label: string;
  items: Array<{
    id: AdminSectionId;
    label: string;
    icon: React.ComponentType<{ className?: string }>;
    badgeCount?: number;
  }>;
}

interface SidebarProps {
  activeSection: AdminSectionId;
  onSelectSection: (section: AdminSectionId) => void;
  pendingReviewCount: number;
  openFeedbackCount?: number;
  mobileOpen: boolean;
  onCloseMobile: () => void;
  connectionMode: AdminConnectionMode;
  session: AdminSessionState | null;
  onSignOut: () => void;
  onSwitchToLiveAuth: () => void;
}

export function Sidebar({
  activeSection,
  onSelectSection,
  pendingReviewCount,
  openFeedbackCount = 0,
  mobileOpen,
  onCloseMobile,
  connectionMode,
  session,
  onSignOut,
  onSwitchToLiveAuth,
}: SidebarProps) {
  const groups: NavGroup[] = [
    {
      label: 'Workspace',
      items: [
        { id: 'overview', label: 'Overview', icon: LayoutDashboard },
        { id: 'analytics', label: 'Analytics', icon: BarChart3 },
      ],
    },
    {
      label: 'Curriculum & Content',
      items: [
        { id: 'academic', label: 'Academic Management', icon: GraduationCap },
        { id: 'classes', label: 'Classes', icon: Layers },
        { id: 'subjects', label: 'Subjects', icon: BookOpen },
        { id: 'chapters', label: 'Chapters', icon: FolderTree },
        { id: 'content-studio', label: 'Content Studio', icon: PenTool },
      ],
    },
    {
      label: 'Editorial Pipeline',
      items: [
        { id: 'ai-submissions', label: 'AI Agent Submissions', icon: Bot },
        {
          id: 'review-queue',
          label: 'Review & Approval',
          icon: CheckSquare,
          badgeCount: pendingReviewCount,
        },
        { id: 'pdf-library', label: 'PDF Library', icon: FileText },
      ],
    },
    {
      label: 'Operations & Platform',
      items: [
        { id: 'students', label: 'Students', icon: Users },
        {
          id: 'feedback',
          label: 'Feedback & Errors',
          icon: MessageSquareWarning,
          badgeCount: openFeedbackCount,
        },
        { id: 'infrastructure', label: 'Infrastructure', icon: Server },
        { id: 'capacity', label: 'Capacity & Quotas', icon: Gauge },
        { id: 'settings', label: 'Security & Settings', icon: Shield },
      ],
    },
  ];

  const handleSelect = (id: AdminSectionId) => {
    onSelectSection(id);
    onCloseMobile();
  };

  const navContent = (
    <div className="flex h-full flex-col bg-[#08132B] text-[#E6EFFD] border-r border-[#1B3263] dark:bg-[#08132B] dark:border-[#1B3263]">
      <div className="flex h-16 items-center justify-between px-5 border-b border-[#1B3263]">
        <div className="flex items-center gap-3 min-w-0">
          <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg bg-[#2563EB] text-white font-display font-bold text-base">
            IP
          </div>
          <div className="min-w-0">
            <div className="font-display text-sm font-bold tracking-tight text-white truncate">
              Imtihan Pro
            </div>
            <div className="text-[11px] text-[#8FA7D1] truncate">
              {connectionMode === 'live_api' ? 'Admin Console · Live API' : 'Admin Console · Preview'}
            </div>
          </div>
        </div>
        <button
          type="button"
          onClick={onCloseMobile}
          aria-label="Close navigation menu"
          className="lg:hidden min-h-[40px] min-w-[40px] flex items-center justify-center rounded-lg text-[#94A8CE] hover:text-white hover:bg-[#13264D] focus-visible:outline-2 focus-visible:outline-[#60A5FA]"
        >
          <X className="h-5 w-5" />
        </button>
      </div>

      <nav aria-label="Admin navigation" className="flex-1 overflow-y-auto px-3 py-4 space-y-5">
        {groups.map((group) => (
          <div key={group.label}>
            <div className="px-3 mb-2 text-[11px] font-semibold tracking-wider text-[#7A93C2] uppercase">
              {group.label}
            </div>
            <ul className="space-y-1">
              {group.items.map((item) => {
                const Icon = item.icon;
                const active = activeSection === item.id;
                return (
                  <li key={item.id}>
                    <button
                      type="button"
                      onClick={() => handleSelect(item.id)}
                      aria-current={active ? 'page' : undefined}
                      className={`w-full min-h-[40px] flex items-center justify-between gap-2.5 px-3 py-2 rounded-lg text-sm font-medium transition-colors duration-150 focus-visible:outline-2 focus-visible:outline-[#60A5FA] ${
                        active
                          ? 'bg-[#1D4ED8]/25 text-white border border-[#3B82F6]/45'
                          : 'text-[#B5C6E6] hover:bg-[#11234A] hover:text-white border border-transparent'
                      }`}
                    >
                      <span className="flex items-center gap-2.5 min-w-0">
                        <Icon
                          className={`h-4 w-4 shrink-0 ${
                            active ? 'text-[#60A5FA]' : 'text-[#7A93C2]'
                          }`}
                        />
                        <span className="truncate">{item.label}</span>
                      </span>
                      {typeof item.badgeCount === 'number' && item.badgeCount > 0 && (
                        <span className="tabular-nums text-xs font-semibold text-[#93C5FD]">
                          {item.badgeCount}
                        </span>
                      )}
                    </button>
                  </li>
                );
              })}
            </ul>
          </div>
        ))}
      </nav>

      <div className="px-4 py-3.5 border-t border-[#1B3263] text-xs text-[#8FA7D1] space-y-2">
        {connectionMode === 'live_api' && session ? (
          <>
            <div className="truncate font-medium text-[#D8E5FA]">{session.admin.displayName}</div>
            <div className="truncate text-[11px] text-[#7A93C2]">{session.admin.email}</div>
            <button
              type="button"
              onClick={onSignOut}
              className="w-full h-8 px-2.5 inline-flex items-center justify-center gap-1.5 rounded-lg border border-[#24407A] text-[11px] font-semibold text-[#DCE8FC] hover:bg-[#13264D]"
            >
              <LogOut className="h-3.5 w-3.5" />
              Sign Out
            </button>
          </>
        ) : (
          <>
            <div className="font-medium text-[#D8E5FA]">Offline UI Preview</div>
            <div className="text-[11px] text-[#7A93C2]">Local sample data active</div>
            <button
              type="button"
              onClick={onSwitchToLiveAuth}
              className="w-full h-8 px-2.5 inline-flex items-center justify-center gap-1.5 rounded-lg bg-[#2563EB] text-[11px] font-semibold text-white hover:bg-[#3B82F6]"
            >
              Connect Worker API
            </button>
          </>
        )}
      </div>
    </div>
  );

  return (
    <>
      <aside className="hidden lg:block w-[264px] shrink-0 h-screen sticky top-0">
        {navContent}
      </aside>

      {mobileOpen && (
        <div className="fixed inset-0 z-50 lg:hidden flex">
          <div
            className="fixed inset-0 bg-black/60 backdrop-blur-xs"
            onClick={onCloseMobile}
            aria-hidden="true"
          />
          <div className="relative z-10 w-[272px] max-w-[85vw] h-full">
            {navContent}
          </div>
        </div>
      )}
    </>
  );
}
