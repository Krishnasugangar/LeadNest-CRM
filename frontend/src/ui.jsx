import { NavLink, Navigate, Outlet } from 'react-router-dom';
import { useState } from 'react';
import { useAuth } from './auth';

const linkClass = ({ isActive }) =>
  `block rounded-lg px-3 py-2 text-sm font-medium ${isActive ? 'bg-[#c4552a] text-white' : 'text-stone-200 hover:bg-white/10'}`;

export function RequireAuth() {
  const { user, loading } = useAuth();
  if (loading) {
    return <p className="p-8 text-stone-600">Loading workspace…</p>;
  }
  if (!user) {
    return <Navigate to="/login" replace />;
  }
  return <Outlet />;
}

export function RequireAdmin() {
  const { user } = useAuth();
  if (user?.role !== 'ADMIN') {
    return <Navigate to="/" replace />;
  }
  return <Outlet />;
}

export function Layout() {
  const { user, logout } = useAuth();
  const [open, setOpen] = useState(false);
  const items = [
    ['/', 'Dashboard'],
    ['/leads', 'Leads'],
    ['/deals', 'Deals'],
    ['/follow-ups', 'Follow-ups'],
  ];
  if (user.role === 'ADMIN') {
    items.push(['/agents', 'Agents']);
  }
  items.push(['/profile', 'Profile']);

  return (
    <div className="min-h-screen md:grid md:grid-cols-[240px_1fr]">
      <aside className={`${open ? 'block' : 'hidden'} bg-[#241c18] text-white md:block`}>
        <div className="flex items-center justify-between px-5 py-5">
          <div>
            <p className="text-lg font-semibold tracking-tight">LeadNest</p>
            <p className="text-xs text-stone-400">Sales workspace</p>
          </div>
        </div>
        <nav className="space-y-1 px-3">
          {items.map(([to, label]) => (
            <NavLink key={to} to={to} end={to === '/'} className={linkClass} onClick={() => setOpen(false)}>
              {label}
            </NavLink>
          ))}
        </nav>
        <div className="mt-8 border-t border-white/10 px-5 py-4 text-sm">
          <p className="font-medium">{user.name}</p>
          <p className="text-stone-400">{user.role === 'ADMIN' ? 'Administrator' : 'Agent'}</p>
          <button type="button" className="mt-3 text-[#f0b89a] hover:text-white" onClick={logout}>Sign out</button>
        </div>
      </aside>
      <div>
        <header className="flex items-center justify-between border-b border-[#e5dcd2] bg-[#fffdfb] px-4 py-3 md:hidden">
          <p className="font-semibold">LeadNest</p>
          <button type="button" className="rounded-lg border border-stone-300 px-3 py-1 text-sm" onClick={() => setOpen((value) => !value)}>
            Menu
          </button>
        </header>
        <main className="mx-auto max-w-6xl px-4 py-6 md:px-8">
          <Outlet />
        </main>
      </div>
    </div>
  );
}

export function PageHeader({ title, subtitle, children }) {
  return (
    <div className="mb-6 flex flex-wrap items-end justify-between gap-3">
      <div>
        <h1 className="text-2xl font-semibold tracking-tight text-stone-900">{title}</h1>
        {subtitle ? <p className="mt-1 text-sm text-stone-600">{subtitle}</p> : null}
      </div>
      {children}
    </div>
  );
}

export function Banner({ children }) {
  if (!children) {
    return null;
  }
  return <div className="mb-4 rounded-xl border border-rose-200 bg-rose-50 px-4 py-3 text-sm text-rose-800">{children}</div>;
}

export function Empty({ children }) {
  return <div className="rounded-2xl border border-dashed border-[#e5dcd2] bg-white px-4 py-10 text-center text-sm text-stone-500">{children}</div>;
}

export function Card({ children, className = '' }) {
  return <section className={`rounded-2xl border border-[#e7dfd6] bg-[#fffdfb] p-5 shadow-sm ${className}`}>{children}</section>;
}

export function Field({ label, error, children }) {
  return (
    <label className="block text-sm">
      <span className="mb-1 block font-medium text-stone-700">{label}</span>
      {children}
      {error ? <span className="mt-1 block text-xs text-rose-700">{error}</span> : null}
    </label>
  );
}

export const controlClass = 'w-full rounded-lg border border-stone-300 bg-white px-3 py-2 text-sm outline-none focus:border-[#c4552a] focus:ring-2 focus:ring-[#c4552a]/20';

export function Button({ variant = 'primary', className = '', ...props }) {
  const styles = {
    primary: 'bg-[#c4552a] text-white hover:bg-[#9a3f1d]',
    ghost: 'border border-stone-300 bg-white text-stone-800 hover:bg-stone-50',
    danger: 'bg-rose-800 text-white hover:bg-rose-900',
  }[variant];
  return <button className={`inline-flex items-center justify-center rounded-lg px-4 py-2 text-sm font-semibold disabled:opacity-60 ${styles} ${className}`} {...props} />;
}

export function Pill({ children, tone = 'stone' }) {
  const tones = {
    stone: 'bg-stone-100 text-stone-700',
    amber: 'bg-amber-100 text-amber-900',
    orange: 'bg-orange-100 text-orange-900',
    rose: 'bg-rose-100 text-rose-800',
    green: 'bg-emerald-100 text-emerald-900',
    sky: 'bg-sky-100 text-sky-900',
  };
  return <span className={`inline-flex rounded-full px-2.5 py-1 text-xs font-semibold ${tones[tone]}`}>{children}</span>;
}

export function leadTone(status) {
  return { NEW: 'stone', CONTACTED: 'amber', QUALIFIED: 'orange', LOST: 'rose' }[status] || 'stone';
}

export function dealTone(stage) {
  return { PROPOSAL: 'sky', NEGOTIATION: 'amber', WON: 'green', LOST: 'rose' }[stage] || 'stone';
}

export function ConfirmDialog({ title, body, confirmLabel, tone = 'primary', busy, onCancel, onConfirm, children }) {
  return (
    <div className="fixed inset-0 z-40 flex items-center justify-center bg-stone-900/40 p-4">
      <div className="w-full max-w-md rounded-2xl bg-white p-6 shadow-xl">
        <h2 className="text-lg font-semibold">{title}</h2>
        <p className="mt-2 text-sm text-stone-600">{body}</p>
        <div className="mt-4">{children}</div>
        <div className="mt-6 flex justify-end gap-3">
          <Button variant="ghost" type="button" onClick={onCancel}>Cancel</Button>
          <Button variant={tone} type="button" disabled={busy} onClick={onConfirm}>{busy ? 'Working…' : confirmLabel}</Button>
        </div>
      </div>
    </div>
  );
}

export function Pager({ page, totalPages, onPage }) {
  if (totalPages <= 1) {
    return null;
  }
  return (
    <div className="mt-4 flex items-center justify-between text-sm">
      <Button variant="ghost" type="button" disabled={page <= 0} onClick={() => onPage(page - 1)}>Previous</Button>
      <span className="text-stone-600">Page {page + 1} of {totalPages}</span>
      <Button variant="ghost" type="button" disabled={page + 1 >= totalPages} onClick={() => onPage(page + 1)}>Next</Button>
    </div>
  );
}
