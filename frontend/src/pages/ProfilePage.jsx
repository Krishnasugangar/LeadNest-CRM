import { useState } from 'react';
import { api } from '../api';
import { useAuth } from '../auth';
import { Banner, Button, Card, Field, PageHeader, controlClass } from '../ui';

export default function ProfilePage() {
  const { user } = useAuth();
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  async function onSubmit(event) {
    event.preventDefault();
    setBusy(true);
    setError('');
    setMessage('');
    try {
      const response = await api('/api/auth/password', { method: 'PUT', body: { currentPassword, newPassword } });
      setMessage(response.message);
      setCurrentPassword('');
      setNewPassword('');
    } catch (caught) {
      setError(caught.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div>
      <PageHeader title="Profile" subtitle={`${user.name} · ${user.email}`} />
      <Banner>{error}</Banner>
      {message ? <p className="mb-4 rounded-xl bg-emerald-50 px-4 py-3 text-sm text-emerald-900">{message}</p> : null}
      <Card className="max-w-lg">
        <form onSubmit={onSubmit} className="space-y-4">
          <Field label="Current password">
            <input className={controlClass} type="password" value={currentPassword} onChange={(event) => setCurrentPassword(event.target.value)} required />
          </Field>
          <Field label="New password">
            <input className={controlClass} type="password" minLength="8" value={newPassword} onChange={(event) => setNewPassword(event.target.value)} required />
          </Field>
          <Button disabled={busy}>{busy ? 'Saving…' : 'Change password'}</Button>
        </form>
      </Card>
    </div>
  );
}
