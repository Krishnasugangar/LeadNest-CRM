import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api';
import { useAuth } from '../auth';
import { LEAD_STATUS_LABELS, SOURCE_LABELS, formatDateTime } from '../format';
import { Banner, Button, Card, Empty, Field, PageHeader, Pager, Pill, controlClass, leadTone } from '../ui';

const emptyForm = { name: '', email: '', phone: '', company: '', source: 'WEBSITE', notes: '', agentId: '' };

export default function LeadsPage() {
  const { user } = useAuth();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [status, setStatus] = useState('');
  const [agentId, setAgentId] = useState('');
  const [includeArchived, setIncludeArchived] = useState(false);
  const [result, setResult] = useState(null);
  const [agents, setAgents] = useState([]);
  const [error, setError] = useState('');
  const [form, setForm] = useState(emptyForm);
  const [fieldErrors, setFieldErrors] = useState({});
  const [creating, setCreating] = useState(false);
  const [showForm, setShowForm] = useState(false);

  useEffect(() => {
    if (user.role === 'ADMIN') {
      api('/api/agents').then(setAgents).catch(() => setAgents([]));
    }
  }, [user.role]);

  useEffect(() => {
    const params = new URLSearchParams({ page, size: 10, includeArchived });
    if (search.trim()) params.set('search', search.trim());
    if (status) params.set('status', status);
    if (user.role === 'ADMIN' && agentId) params.set('agentId', agentId);
    api(`/api/leads?${params}`)
      .then((data) => {
        setResult(data);
        setError('');
      })
      .catch((caught) => setError(caught.message));
  }, [page, search, status, agentId, includeArchived, user.role, creating]);

  async function createLead(event) {
    event.preventDefault();
    setCreating(true);
    setFieldErrors({});
    setError('');
    const body = {
      name: form.name,
      email: form.email || null,
      phone: form.phone || null,
      company: form.company || null,
      source: form.source,
      notes: form.notes || null,
    };
    if (user.role === 'ADMIN' && form.agentId) {
      body.agentId = Number(form.agentId);
    }
    try {
      await api('/api/leads', { method: 'POST', body });
      setForm(emptyForm);
      setShowForm(false);
      setPage(0);
    } catch (caught) {
      setError(caught.message);
      setFieldErrors(caught.fieldErrors || {});
    } finally {
      setCreating(false);
    }
  }

  return (
    <div>
      <PageHeader title="Leads" subtitle="Search by name, email, or phone.">
        <Button type="button" onClick={() => setShowForm((value) => !value)}>{showForm ? 'Close form' : 'New lead'}</Button>
      </PageHeader>
      <Banner>{error}</Banner>
      {showForm ? (
        <Card className="mb-5">
          <form onSubmit={createLead} className="grid gap-4 md:grid-cols-2">
            <Field label="Name" error={fieldErrors.name}>
              <input className={controlClass} value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} required />
            </Field>
            <Field label="Company">
              <input className={controlClass} value={form.company} onChange={(event) => setForm({ ...form, company: event.target.value })} />
            </Field>
            <Field label="Email" error={fieldErrors.email}>
              <input className={controlClass} value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} />
            </Field>
            <Field label="Phone" error={fieldErrors.phone}>
              <input className={controlClass} value={form.phone} onChange={(event) => setForm({ ...form, phone: event.target.value })} />
            </Field>
            <Field label="Source">
              <select className={controlClass} value={form.source} onChange={(event) => setForm({ ...form, source: event.target.value })}>
                {Object.entries(SOURCE_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
              </select>
            </Field>
            {user.role === 'ADMIN' ? (
              <Field label="Assign to">
                <select className={controlClass} value={form.agentId} onChange={(event) => setForm({ ...form, agentId: event.target.value })}>
                  <option value="">Myself</option>
                  {agents.filter((agent) => agent.active).map((agent) => <option key={agent.id} value={agent.id}>{agent.name}</option>)}
                </select>
              </Field>
            ) : null}
            <Field label="Notes">
              <textarea className={controlClass} rows="3" value={form.notes} onChange={(event) => setForm({ ...form, notes: event.target.value })} />
            </Field>
            <div className="md:col-span-2">
              <Button disabled={creating}>{creating ? 'Saving…' : 'Create lead'}</Button>
            </div>
          </form>
        </Card>
      ) : null}
      <Card>
        <div className="grid gap-3 md:grid-cols-4">
          <input className={controlClass} placeholder="Name, email, or phone" value={search} onChange={(event) => { setPage(0); setSearch(event.target.value); }} />
          <select className={controlClass} value={status} onChange={(event) => { setPage(0); setStatus(event.target.value); }}>
            <option value="">All statuses</option>
            {Object.entries(LEAD_STATUS_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
          </select>
          {user.role === 'ADMIN' ? (
            <select className={controlClass} value={agentId} onChange={(event) => { setPage(0); setAgentId(event.target.value); }}>
              <option value="">All agents</option>
              {agents.map((agent) => <option key={agent.id} value={agent.id}>{agent.name}</option>)}
            </select>
          ) : null}
          <label className="flex items-center gap-2 text-sm text-stone-700">
            <input type="checkbox" checked={includeArchived} onChange={(event) => { setPage(0); setIncludeArchived(event.target.checked); }} />
            Include archived
          </label>
        </div>
        {!result ? <p className="mt-4 text-sm text-stone-500">Loading leads…</p> : null}
        {result && result.content.length === 0 ? <div className="mt-4"><Empty>No leads match these filters.</Empty></div> : null}
        {result && result.content.length > 0 ? (
          <div className="mt-4 overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="text-xs uppercase tracking-wide text-stone-500">
                <tr>
                  <th className="py-2">Lead</th>
                  <th>Status</th>
                  <th>Owner</th>
                  <th>Updated</th>
                </tr>
              </thead>
              <tbody>
                {result.content.map((lead) => (
                  <tr key={lead.id} className="border-t border-[#efe7df]">
                    <td className="py-3">
                      <Link className="font-semibold text-[#9a3f1d] hover:underline" to={`/leads/${lead.id}`}>{lead.name}</Link>
                      <p className="text-stone-500">{lead.email || lead.phone}{lead.archived ? ' · Archived' : ''}</p>
                    </td>
                    <td><Pill tone={leadTone(lead.status)}>{LEAD_STATUS_LABELS[lead.status]}</Pill></td>
                    <td>{lead.assignedAgentName}</td>
                    <td className="text-stone-500">{formatDateTime(lead.updatedAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
            <Pager page={result.page} totalPages={result.totalPages} onPage={setPage} />
          </div>
        ) : null}
      </Card>
    </div>
  );
}
