import { useEffect, useState } from 'react';
import { api } from '../api';
import { formatDateTime } from '../format';
import { Banner, Button, Card, ConfirmDialog, Field, PageHeader, Pill, controlClass } from '../ui';

const empty = { name: '', email: '', password: '' };

export default function AgentsPage() {
  const [agents, setAgents] = useState(null);
  const [form, setForm] = useState(empty);
  const [error, setError] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});
  const [busy, setBusy] = useState(false);
  const [target, setTarget] = useState(null);

  function load() {
    return api('/api/agents').then(setAgents);
  }

  useEffect(() => {
    load().catch((caught) => setError(caught.message));
  }, []);

  async function createAgent(event) {
    event.preventDefault();
    setBusy(true);
    setError('');
    setFieldErrors({});
    try {
      await api('/api/agents', { method: 'POST', body: form });
      setForm(empty);
      await load();
    } catch (caught) {
      setError(caught.message);
      setFieldErrors(caught.fieldErrors || {});
    } finally {
      setBusy(false);
    }
  }

  async function applyStatus() {
    setBusy(true);
    setError('');
    try {
      await api(`/api/agents/${target.id}/active`, { method: 'PATCH', body: { active: !target.active } });
      setTarget(null);
      await load();
    } catch (caught) {
      setError(caught.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div>
      <PageHeader title="Agents" subtitle="Create agent accounts and turn access on or off." />
      <Banner>{error}</Banner>
      <Card className="mb-4">
        <form onSubmit={createAgent} className="grid gap-4 md:grid-cols-4">
          <Field label="Name" error={fieldErrors.name}><input className={controlClass} value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} required /></Field>
          <Field label="Email" error={fieldErrors.email}><input className={controlClass} type="email" value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} required /></Field>
          <Field label="Initial password" error={fieldErrors.password}><input className={controlClass} type="password" minLength="8" value={form.password} onChange={(event) => setForm({ ...form, password: event.target.value })} required /></Field>
          <div className="flex items-end"><Button disabled={busy}>Create agent</Button></div>
        </form>
      </Card>
      <Card>
        {!agents ? <p className="text-sm text-stone-500">Loading agents…</p> : null}
        {agents && agents.length === 0 ? <p className="text-sm text-stone-500">No agents yet.</p> : null}
        {agents && agents.length > 0 ? (
          <table className="w-full text-left text-sm">
            <thead className="text-xs uppercase tracking-wide text-stone-500"><tr><th className="py-2">Agent</th><th>Status</th><th>Created</th><th></th></tr></thead>
            <tbody>
              {agents.map((agent) => (
                <tr key={agent.id} className="border-t border-[#efe7df]">
                  <td className="py-3"><p className="font-semibold">{agent.name}</p><p className="text-stone-500">{agent.email}</p></td>
                  <td><Pill tone={agent.active ? 'green' : 'rose'}>{agent.active ? 'Active' : 'Inactive'}</Pill></td>
                  <td className="text-stone-500">{formatDateTime(agent.createdAt)}</td>
                  <td className="text-right">
                    <Button type="button" variant={agent.active ? 'danger' : 'ghost'} onClick={() => setTarget(agent)}>{agent.active ? 'Deactivate' : 'Activate'}</Button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : null}
      </Card>
      {target ? (
        <ConfirmDialog
          title={target.active ? `Deactivate ${target.name}` : `Activate ${target.name}`}
          body={target.active ? 'Their current session stops working immediately.' : 'They will be able to sign in again.'}
          confirmLabel={target.active ? 'Deactivate' : 'Activate'}
          tone={target.active ? 'danger' : 'primary'}
          busy={busy}
          onCancel={() => setTarget(null)}
          onConfirm={applyStatus}
        />
      ) : null}
    </div>
  );
}
