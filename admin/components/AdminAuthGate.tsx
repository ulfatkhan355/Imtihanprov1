import React, { useState } from 'react';
import { AlertTriangle, KeyRound, Lock, RefreshCw, Server, ShieldCheck } from 'lucide-react';
import type { AdminBootstrapStatusResponse } from '../../shared-contracts/types';
import { validateApiBaseUrl } from '../../shared-contracts/rules';

interface AdminAuthGateProps {
  apiBaseUrl: string;
  bootstrapStatus: AdminBootstrapStatusResponse | null;
  connectionError: string | null;
  busy: boolean;
  onUpdateApiUrl: (url: string) => void;
  onRetryConnection: () => void;
  onBootstrap: (params: {
    bootstrapSecret: string;
    email: string;
    displayName: string;
    password: string;
  }) => Promise<string | null>;
  onLogin: (email: string, password: string) => Promise<string | null>;
  onEnterOfflinePreview: () => void;
}

export function AdminAuthGate({
  apiBaseUrl,
  bootstrapStatus,
  connectionError,
  busy,
  onUpdateApiUrl,
  onRetryConnection,
  onBootstrap,
  onLogin,
  onEnterOfflinePreview,
}: AdminAuthGateProps) {
  const [urlDraft, setUrlDraft] = useState(apiBaseUrl);
  const [email, setEmail] = useState('');
  const [displayName, setDisplayName] = useState('Super Admin');
  const [password, setPassword] = useState('');
  const [bootstrapSecret, setBootstrapSecret] = useState('');
  const [formError, setFormError] = useState<string | null>(null);

  const needsBootstrap = bootstrapStatus && !bootstrapStatus.hasAdmin;

  const handleUrlApply = (e: React.FormEvent) => {
    e.preventDefault();
    const check = validateApiBaseUrl(urlDraft);
    if (!check.valid) {
      setFormError(check.reason || 'Invalid Worker API URL.');
      return;
    }
    setFormError(null);
    onUpdateApiUrl(urlDraft.trim());
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setFormError(null);
    if (needsBootstrap) {
      if (!bootstrapSecret.trim() || !email.trim() || password.length < 10) {
        setFormError('Bootstrap secret, valid email, and a 10+ character password are required.');
        return;
      }
      const err = await onBootstrap({
        bootstrapSecret: bootstrapSecret.trim(),
        email: email.trim(),
        displayName: displayName.trim() || 'Super Admin',
        password,
      });
      if (err) setFormError(err);
    } else {
      if (!email.trim() || !password) {
        setFormError('Email and password are required.');
        return;
      }
      const err = await onLogin(email.trim(), password);
      if (err) setFormError(err);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-[#F4F7FC] text-[#0F172A] dark:bg-[#060E21] dark:text-[#F0F6FF] p-4">
      <div className="w-full max-w-lg rounded-2xl border border-[#CBD8EE] bg-white p-6 sm:p-8 shadow-lg dark:border-[#223B70] dark:bg-[#0D1B38] space-y-6">
        <div className="flex items-center gap-3">
          <div className="flex h-11 w-11 shrink-0 items-center justify-center rounded-xl bg-[#2563EB] text-white font-display font-bold text-lg">
            IP
          </div>
          <div>
            <h1 className="font-display text-xl font-bold text-[#0F172A] dark:text-white">
              Imtihan Pro Admin Console
            </h1>
            <p className="text-xs text-[#64748B] dark:text-[#8FA7D1]">
              Cloudflare Worker (<code className="font-mono">imtihan-pro-api</code>) &amp; D1 Authentication
            </p>
          </div>
        </div>

        <form onSubmit={handleUrlApply} className="space-y-2 rounded-xl border border-[#E2E8F0] bg-[#F8FAFE] p-3.5 dark:border-[#1B3263] dark:bg-[#091530]">
          <label htmlFor="gate-api-url" className="flex items-center gap-1.5 text-xs font-semibold text-[#334155] dark:text-[#B5C6E6]">
            <Server className="h-3.5 w-3.5 text-[#2563EB]" />
            Worker API Base URL
          </label>
          <div className="flex gap-2">
            <input
              id="gate-api-url"
              type="url"
              value={urlDraft}
              onChange={(e) => setUrlDraft(e.target.value)}
              className="flex-1 h-9 px-3 rounded-lg border border-[#CBD8EE] bg-white font-mono text-xs text-[#0F172A] dark:border-[#24407A] dark:bg-[#0D1B38] dark:text-white"
            />
            <button
              type="submit"
              disabled={busy}
              className="h-9 px-3 rounded-lg border border-[#CBD8EE] bg-white text-xs font-semibold text-[#1E293B] hover:bg-[#EDF2FA] dark:border-[#24407A] dark:bg-[#102145] dark:text-white whitespace-nowrap"
            >
              Connect
            </button>
          </div>
        </form>

        {connectionError && (
          <div
            role="alert"
            className="rounded-xl border border-[#F59E0B]/40 bg-[#FFFBEB] p-4 text-xs text-[#92400E] dark:border-[#B45309]/60 dark:bg-[#2B210B] dark:text-[#FEF3C7] space-y-2"
          >
            <div className="flex items-start gap-2">
              <AlertTriangle className="h-4 w-4 shrink-0 mt-0.5" />
              <div>
                <div className="font-semibold">Worker API Not Reachable</div>
                <p className="mt-0.5 opacity-90">{connectionError}</p>
                <p className="mt-1 font-mono text-[11px]">
                  Run locally: cd backend &amp;&amp; npm run dev
                </p>
              </div>
            </div>
            <div className="flex flex-wrap gap-2 pt-1">
              <button
                type="button"
                onClick={onRetryConnection}
                disabled={busy}
                className="h-8 px-3 inline-flex items-center gap-1.5 rounded-lg bg-[#D97706] text-white text-xs font-semibold hover:bg-[#B45309]"
              >
                <RefreshCw className="h-3.5 w-3.5" />
                Retry Worker Check
              </button>
              <button
                type="button"
                onClick={onEnterOfflinePreview}
                className="h-8 px-3 rounded-lg border border-[#D97706]/50 text-xs font-semibold hover:bg-[#FEF3C7]/40 dark:hover:bg-[#1E1606]"
              >
                Open Offline UI Preview
              </button>
            </div>
          </div>
        )}

        {bootstrapStatus && !connectionError && (
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="flex items-center gap-2 rounded-lg border border-[#CBD8EE] bg-[#EFF6FF] px-3.5 py-2.5 text-xs text-[#1E40AF] dark:border-[#24407A] dark:bg-[#102145] dark:text-[#93C5FD]">
              {needsBootstrap ? (
                <>
                  <KeyRound className="h-4 w-4 shrink-0" />
                  <span>
                    First-time setup: No admin user exists in D1 yet. Use your Worker{' '}
                    <span className="font-mono">ADMIN_BOOTSTRAP_SECRET</span> to initialize the first Super Admin.
                  </span>
                </>
              ) : (
                <>
                  <ShieldCheck className="h-4 w-4 shrink-0" />
                  <span>Sign in with your verified Imtihan Pro Admin credentials.</span>
                </>
              )}
            </div>

            {formError && (
              <div
                role="alert"
                className="rounded-lg border border-[#FCA5A5] bg-[#FEF2F2] px-3.5 py-2.5 text-xs font-medium text-[#991B1B] dark:border-[#7F1D1D] dark:bg-[#2A1215] dark:text-[#FECACA]"
              >
                {formError}
              </div>
            )}

            {needsBootstrap && (
              <>
                <div>
                  <label htmlFor="adm-boot-secret" className="block text-xs font-medium text-[#334155] dark:text-[#B5C6E6]">
                    Bootstrap Secret (<code className="font-mono">ADMIN_BOOTSTRAP_SECRET</code>)
                  </label>
                  <input
                    id="adm-boot-secret"
                    type="password"
                    required
                    value={bootstrapSecret}
                    onChange={(e) => setBootstrapSecret(e.target.value)}
                    placeholder="Configured in backend/.dev.vars or wrangler secret"
                    className="mt-1 w-full h-10 px-3 rounded-lg border border-[#CBD8EE] bg-white text-sm text-[#0F172A] dark:border-[#24407A] dark:bg-[#091530] dark:text-white"
                  />
                </div>
                <div>
                  <label htmlFor="adm-display-name" className="block text-xs font-medium text-[#334155] dark:text-[#B5C6E6]">
                    Display Name
                  </label>
                  <input
                    id="adm-display-name"
                    type="text"
                    required
                    value={displayName}
                    onChange={(e) => setDisplayName(e.target.value)}
                    className="mt-1 w-full h-10 px-3 rounded-lg border border-[#CBD8EE] bg-white text-sm text-[#0F172A] dark:border-[#24407A] dark:bg-[#091530] dark:text-white"
                  />
                </div>
              </>
            )}

            <div>
              <label htmlFor="adm-email" className="block text-xs font-medium text-[#334155] dark:text-[#B5C6E6]">
                Admin Email
              </label>
              <input
                id="adm-email"
                type="email"
                required
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="admin@imtihanpro.pk"
                className="mt-1 w-full h-10 px-3 rounded-lg border border-[#CBD8EE] bg-white text-sm text-[#0F172A] dark:border-[#24407A] dark:bg-[#091530] dark:text-white"
              />
            </div>

            <div>
              <label htmlFor="adm-password" className="block text-xs font-medium text-[#334155] dark:text-[#B5C6E6]">
                Password {needsBootstrap ? '(minimum 10 characters)' : ''}
              </label>
              <input
                id="adm-password"
                type="password"
                required
                minLength={needsBootstrap ? 10 : 1}
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="mt-1 w-full h-10 px-3 rounded-lg border border-[#CBD8EE] bg-white text-sm text-[#0F172A] dark:border-[#24407A] dark:bg-[#091530] dark:text-white"
              />
            </div>

            <div className="flex flex-col sm:flex-row gap-2 pt-1">
              <button
                type="submit"
                disabled={busy}
                className="flex-1 min-h-[42px] px-4 inline-flex items-center justify-center gap-2 rounded-lg bg-[#1D4ED8] hover:bg-[#1E40AF] dark:bg-[#2563EB] dark:hover:bg-[#3B82F6] text-white text-xs font-semibold disabled:opacity-60"
              >
                <Lock className="h-3.5 w-3.5" />
                {needsBootstrap ? 'Initialize Super Admin & Sign In' : 'Sign In to Admin Console'}
              </button>
              <button
                type="button"
                onClick={onEnterOfflinePreview}
                className="min-h-[42px] px-4 rounded-lg border border-[#CBD8EE] text-xs font-semibold text-[#475569] hover:bg-[#F8FAFE] dark:border-[#24407A] dark:text-[#B5C6E6] dark:hover:bg-[#11234A]"
              >
                Offline UI Preview
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}
