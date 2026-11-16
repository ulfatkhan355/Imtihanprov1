import React from 'react';
import { AlertTriangle, CheckCircle2, Info, RefreshCw, WifiOff } from 'lucide-react';
import type { ViewLoadState } from '../types/admin';

interface StageNoticeProps {
  title: string;
  message: string;
}

export function StageMockNotice({ title, message }: StageNoticeProps) {
  return (
    <div className="flex items-start gap-3 rounded-xl border border-[#CBD8EE] bg-[#F8FAFE] px-4 py-3 text-xs text-[#334155] dark:border-[#24407A] dark:bg-[#0E1E40] dark:text-[#C5D5F2]">
      <Info className="h-4 w-4 shrink-0 mt-0.5 text-[#1D4ED8] dark:text-[#60A5FA]" />
      <div>
        <span className="font-semibold text-[#0F172A] dark:text-white">{title}: </span>
        <span>{message}</span>
      </div>
    </div>
  );
}

interface StateGateProps {
  state: ViewLoadState;
  emptyTitle: string;
  emptyDescription: string;
  emptyActionLabel?: string;
  onEmptyAction?: () => void;
  onRetry?: () => void;
  children: React.ReactNode;
}

export function StateGate({
  state,
  emptyTitle,
  emptyDescription,
  emptyActionLabel,
  onEmptyAction,
  onRetry,
  children,
}: StateGateProps) {
  if (state === 'loading') {
    return (
      <div
        aria-busy="true"
        aria-label="Loading records"
        className="rounded-xl border border-[#CBD8EE] bg-white p-6 dark:border-[#223B70] dark:bg-[#0D1B38] space-y-3"
      >
        <div className="h-5 w-48 rounded bg-[#E2E8F0] dark:bg-[#193060] animate-pulse" />
        <div className="h-10 w-full rounded bg-[#EEF3FB] dark:bg-[#13264D] animate-pulse" />
        <div className="h-10 w-full rounded bg-[#EEF3FB] dark:bg-[#13264D] animate-pulse" />
        <div className="h-10 w-3/4 rounded bg-[#EEF3FB] dark:bg-[#13264D] animate-pulse" />
      </div>
    );
  }

  if (state === 'error') {
    return (
      <div
        role="alert"
        className="rounded-xl border border-[#FCA5A5] bg-[#FEF2F2] p-6 text-[#991B1B] dark:border-[#7F1D1D] dark:bg-[#2A1215] dark:text-[#FECACA] flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4"
      >
        <div className="flex items-start gap-3">
          <AlertTriangle className="h-5 w-5 shrink-0 mt-0.5 text-[#DC2626] dark:text-[#F87171]" />
          <div>
            <h3 className="font-display font-semibold text-sm">Simulated Data Load Error</h3>
            <p className="text-xs mt-1 opacity-90">
              Unable to reach Stage 2 endpoint or simulated mock request failed. Check network or retry.
            </p>
          </div>
        </div>
        {onRetry && (
          <button
            type="button"
            onClick={onRetry}
            className="min-h-[40px] px-4 inline-flex items-center gap-2 rounded-lg bg-[#DC2626] text-white text-xs font-semibold hover:bg-[#B91C1C] whitespace-nowrap shrink-0"
          >
            <RefreshCw className="h-3.5 w-3.5" />
            Retry Load
          </button>
        )}
      </div>
    );
  }

  if (state === 'empty') {
    return (
      <div className="rounded-xl border border-[#CBD8EE] bg-white p-10 text-center dark:border-[#223B70] dark:bg-[#0D1B38]">
        <WifiOff className="h-7 w-7 mx-auto text-[#64748B] dark:text-[#7A93C2]" />
        <h3 className="mt-3 font-display font-semibold text-base text-[#0F172A] dark:text-white">
          {emptyTitle}
        </h3>
        <p className="mt-1.5 text-sm text-[#475569] dark:text-[#94A8CE] max-w-md mx-auto">
          {emptyDescription}
        </p>
        {emptyActionLabel && onEmptyAction && (
          <button
            type="button"
            onClick={onEmptyAction}
            className="mt-4 min-h-[40px] px-4 rounded-lg bg-[#1D4ED8] hover:bg-[#1E40AF] dark:bg-[#2563EB] dark:hover:bg-[#3B82F6] text-white text-xs font-semibold"
          >
            {emptyActionLabel}
          </button>
        )}
      </div>
    );
  }

  return <>{children}</>;
}

export function StatusIndicator({
  label,
  tone,
}: {
  label: string;
  tone: 'success' | 'warning' | 'danger' | 'info' | 'neutral';
}) {
  const styles = {
    success: 'text-[#15803D] dark:text-[#4ADE80]',
    warning: 'text-[#B45309] dark:text-[#FBBF24]',
    danger: 'text-[#B91C1C] dark:text-[#F87171]',
    info: 'text-[#1D4ED8] dark:text-[#60A5FA]',
    neutral: 'text-[#475569] dark:text-[#94A8CE]',
  };
  const Icon =
    tone === 'success'
      ? CheckCircle2
      : tone === 'warning' || tone === 'danger'
        ? AlertTriangle
        : Info;

  return (
    <span className={`inline-flex items-center gap-1.5 text-xs font-semibold ${styles[tone]}`}>
      <Icon className="h-3.5 w-3.5 shrink-0" />
      <span>{label}</span>
    </span>
  );
}
