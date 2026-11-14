import { useState } from 'react';
import { Navigate } from 'react-router-dom';
import { ApiError } from '../api';
import { useAuth } from '../auth';
import { Button, Field, controlClass } from '../ui';

export default function LoginPage() {
  const { user, loading, login } = useAuth();
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  if (loading) {
    return <p className="p-8 text-stone-600">Loading…</p>;
  }
  if (user) {
    return <Navigate to="/" replace />;
  }

  async function onSubmit(event) {
    event.preventDefault();
    setBusy(true);
    setError('');
    try {
      await login(email, password);
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Unable to sign in');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center px-4">
      <form onSubmit={onSubmit} className="w-full max-w-md rounded-3xl border border-[#e7dfd6] bg-[#fffdfb] p-8 shadow-sm">
        <p className="text-sm font-semibold uppercase tracking-[0.16em] text-[#c4552a]">LeadNest</p>
        <h1 className="mt-2 text-2xl font-semibold">Sign in</h1>
        <p className="mt-1 text-sm text-stone-600">Use the account your administrator created.</p>
        {error ? <p className="mt-4 rounded-lg bg-rose-50 px-3 py-2 text-sm text-rose-800">{error}</p> : null}
        <div className="mt-6 space-y-4">
          <Field label="Email">
            <input className={controlClass} type="email" autoComplete="username" value={email} onChange={(event) => setEmail(event.target.value)} required />
          </Field>
          <Field label="Password">
            <input className={controlClass} type="password" autoComplete="current-password" value={password} onChange={(event) => setPassword(event.target.value)} required />
          </Field>
        </div>
        <Button className="mt-6 w-full" disabled={busy}>{busy ? 'Signing in…' : 'Sign in'}</Button>
      </form>
    </div>
  );
}
