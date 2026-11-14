import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { api } from '../api';
import { useAuth } from '../auth';
import { ACTIVITY_LABELS, LEAD_STATUS_LABELS, SOURCE_LABELS, formatDate, formatDateTime, formatInr, toDateTimeLocal } from '../format';
import { Banner, Button, Card, ConfirmDialog, Empty, Field, PageHeader, Pill, controlClass, dealTone, leadTone } from '../ui';

const nextStatuses = {
  NEW: ['CONTACTED', 'LOST'],
  CONTACTED: ['QUALIFIED', 'LOST'],
  QUALIFIED: ['LOST'],
  LOST: [],
};

export default function LeadDetailPage() {
  const { id } = useParams();
  const { user } = useAuth();
  const [detail, setDetail] = useState(null);
  const [agents, setAgents] = useState([]);
  const [error, setError] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});
  const [busy, setBusy] = useState(false);
  const [form, setForm] = useState(null);
  const [activity, setActivity] = useState({ type: 'NOTE', description: '' });
  const [followUp, setFollowUp] = useState({ description: '', dueAt: toDateTimeLocal() });
  const [deal, setDeal] = useState({ title: '', amount: '', expectedCloseDate: '' });
  const [confirm, setConfirm] = useState(null);
  const [lostReason, setLostReason] = useState('');

  async function load() {
    const data = await api(`/api/leads/${id}`);
    setDetail(data);
    setForm({
      name: data.lead.name,
      email: data.lead.email || '',
      phone: data.lead.phone || '',
      company: data.lead.company || '',
      source: data.lead.source,
      notes: data.lead.notes || '',
      agentId: String(data.lead.assignedAgentId),
    });
  }

  useEffect(() => {
    load().catch((caught) => setError(caught.message));
    if (user.role === 'ADMIN') {
      api('/api/agents').then(setAgents).catch(() => setAgents([]));
    }
  }, [id, user.role]);

  async function run(action) {
    setBusy(true);
    setError('');
    setFieldErrors({});
    try {
      await action();
      await load();
      setConfirm(null);
    } catch (caught) {
      setError(caught.message);
      setFieldErrors(caught.fieldErrors || {});
    } finally {
      setBusy(false);
    }
  }

  if (!detail || !form) {
    return <div><Banner>{error}</Banner>{error ? null : <p className="text-sm text-stone-500">Loading lead…</p>}</div>;
  }

  const lead = detail.lead;
  const transitions = (nextStatuses[lead.status] || []).filter((status) => !(status === 'LOST' && detail.deal));

  return (
    <div>
      <PageHeader title={lead.name} subtitle={lead.company || 'No company recorded'}>
        <Pill tone={leadTone(lead.status)}>{LEAD_STATUS_LABELS[lead.status]}</Pill>
      </PageHeader>
      <Banner>{error}</Banner>
      <div className="grid gap-4 lg:grid-cols-[1.1fr_0.9fr]">
        <div className="space-y-4">
          <Card>
            <h2 className="font-semibold">Details</h2>
            <form className="mt-4 grid gap-4 md:grid-cols-2" onSubmit={(event) => {
              event.preventDefault();
              run(() => api(`/api/leads/${id}`, {
                method: 'PUT',
                body: {
                  ...form,
                  email: form.email || null,
                  phone: form.phone || null,
                  company: form.company || null,
                  notes: form.notes || null,
                  agentId: user.role === 'ADMIN' ? Number(form.agentId) : null,
                },
              }));
            }}>
              <Field label="Name" error={fieldErrors.name}><input className={controlClass} value={form.name} onChange={(event) => setForm({ ...form, name: event.target.value })} /></Field>
              <Field label="Company"><input className={controlClass} value={form.company} onChange={(event) => setForm({ ...form, company: event.target.value })} /></Field>
              <Field label="Email" error={fieldErrors.email}><input className={controlClass} value={form.email} onChange={(event) => setForm({ ...form, email: event.target.value })} /></Field>
              <Field label="Phone" error={fieldErrors.phone}><input className={controlClass} value={form.phone} onChange={(event) => setForm({ ...form, phone: event.target.value })} /></Field>
              <Field label="Source">
                <select className={controlClass} value={form.source} onChange={(event) => setForm({ ...form, source: event.target.value })}>
                  {Object.entries(SOURCE_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
                </select>
              </Field>
              {user.role === 'ADMIN' ? (
                <Field label="Assigned agent">
                  <select className={controlClass} value={form.agentId} onChange={(event) => setForm({ ...form, agentId: event.target.value })}>
                    <option value={lead.assignedAgentId}>{lead.assignedAgentName}</option>
                    {agents.filter((agent) => agent.active && String(agent.id) !== String(lead.assignedAgentId)).map((agent) => (
                      <option key={agent.id} value={agent.id}>{agent.name}</option>
                    ))}
                  </select>
                </Field>
              ) : <Field label="Owner"><input className={controlClass} value={lead.assignedAgentName} disabled /></Field>}
              <div className="md:col-span-2">
                <Field label="Notes"><textarea className={controlClass} rows="3" value={form.notes} onChange={(event) => setForm({ ...form, notes: event.target.value })} /></Field>
              </div>
              <div className="md:col-span-2 flex flex-wrap gap-2">
                <Button disabled={busy || lead.archived}>Save changes</Button>
                {lead.archived ? (
                  <Button type="button" variant="ghost" disabled={busy} onClick={() => setConfirm({ kind: 'restore' })}>Restore</Button>
                ) : (
                  <Button type="button" variant="ghost" disabled={busy} onClick={() => setConfirm({ kind: 'archive' })}>Archive</Button>
                )}
              </div>
            </form>
            <p className="mt-4 text-xs text-stone-500">Created {formatDateTime(lead.createdAt)} · Updated {formatDateTime(lead.updatedAt)}</p>
            {lead.lostReason ? <p className="mt-2 text-sm text-rose-800">Lost reason: {lead.lostReason}</p> : null}
          </Card>
          <Card>
            <h2 className="font-semibold">Status</h2>
            <div className="mt-3 flex flex-wrap gap-2">
              {transitions.length === 0 ? <p className="text-sm text-stone-500">No further status changes are available.</p> : null}
              {transitions.map((status) => (
                <Button key={status} type="button" variant={status === 'LOST' ? 'danger' : 'ghost'} disabled={busy || lead.archived} onClick={() => {
                  if (status === 'LOST') {
                    setLostReason('');
                    setConfirm({ kind: 'lost' });
                  } else {
                    run(() => api(`/api/leads/${id}/status`, { method: 'POST', body: { status } }));
                  }
                }}>{LEAD_STATUS_LABELS[status]}</Button>
              ))}
            </div>
          </Card>
          <Card>
            <h2 className="font-semibold">History</h2>
            {detail.history.length === 0 ? <div className="mt-3"><Empty>No history yet.</Empty></div> : (
              <ol className="mt-3 space-y-3">
                {detail.history.map((event) => (
                  <li key={event.id} className="border-l-2 border-[#e7c3b2] pl-3">
                    <p className="text-sm">{event.detail}</p>
                    <p className="text-xs text-stone-500">{event.actorName} · {formatDateTime(event.createdAt)}</p>
                  </li>
                ))}
              </ol>
            )}
          </Card>
        </div>
        <div className="space-y-4">
          <Card>
            <h2 className="font-semibold">Deal</h2>
            {detail.deal ? (
              <div className="mt-3 space-y-2 text-sm">
                <div className="flex items-center justify-between">
                  <Link className="font-semibold text-[#9a3f1d] hover:underline" to="/deals">{detail.deal.title}</Link>
                  <Pill tone={dealTone(detail.deal.stage)}>{detail.deal.stage}</Pill>
                </div>
                <p>{formatInr(detail.deal.amount)}</p>
                <p className="text-stone-500">Expected close {formatDate(detail.deal.expectedCloseDate)}</p>
                <p className="text-stone-500">Owner follows the lead: {detail.deal.assignedAgentName}</p>
              </div>
            ) : lead.status === 'QUALIFIED' && !lead.archived ? (
              <form className="mt-3 space-y-3" onSubmit={(event) => {
                event.preventDefault();
                run(async () => {
                  await api(`/api/leads/${id}/deal`, {
                    method: 'POST',
                    body: { title: deal.title, amount: Number(deal.amount), expectedCloseDate: deal.expectedCloseDate },
                  });
                  setDeal({ title: '', amount: '', expectedCloseDate: '' });
                });
              }}>
                <Field label="Title"><input className={controlClass} value={deal.title} onChange={(event) => setDeal({ ...deal, title: event.target.value })} required /></Field>
                <Field label="Amount (INR)"><input className={controlClass} type="number" min="0.01" step="0.01" value={deal.amount} onChange={(event) => setDeal({ ...deal, amount: event.target.value })} required /></Field>
                <Field label="Expected close"><input className={controlClass} type="date" value={deal.expectedCloseDate} onChange={(event) => setDeal({ ...deal, expectedCloseDate: event.target.value })} required /></Field>
                <Button disabled={busy}>Convert to deal</Button>
              </form>
            ) : <p className="mt-3 text-sm text-stone-500">Qualify the lead before converting it. The lead stays in place after conversion.</p>}
          </Card>
          <Card>
            <h2 className="font-semibold">Activities</h2>
            <form className="mt-3 space-y-3" onSubmit={(event) => {
              event.preventDefault();
              run(async () => {
                await api(`/api/leads/${id}/activities`, { method: 'POST', body: activity });
                setActivity({ type: 'NOTE', description: '' });
              });
            }}>
              <div className="grid gap-3 sm:grid-cols-[140px_1fr]">
                <select className={controlClass} value={activity.type} onChange={(event) => setActivity({ ...activity, type: event.target.value })}>
                  {Object.entries(ACTIVITY_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
                </select>
                <input className={controlClass} placeholder="What happened?" value={activity.description} onChange={(event) => setActivity({ ...activity, description: event.target.value })} required />
              </div>
              <Button disabled={busy || lead.archived}>Add activity</Button>
            </form>
            <ul className="mt-4 space-y-3">
              {detail.activities.length === 0 ? <Empty>No activities yet.</Empty> : detail.activities.map((item) => (
                <li key={item.id}>
                  <p className="text-xs font-semibold uppercase tracking-wide text-[#c4552a]">{ACTIVITY_LABELS[item.type]}</p>
                  <p className="text-sm">{item.description}</p>
                  <p className="text-xs text-stone-500">{item.authorName} · {formatDateTime(item.createdAt)}</p>
                </li>
              ))}
            </ul>
          </Card>
          <Card>
            <h2 className="font-semibold">Follow-ups</h2>
            <form className="mt-3 space-y-3" onSubmit={(event) => {
              event.preventDefault();
              run(async () => {
                await api(`/api/leads/${id}/follow-ups`, {
                  method: 'POST',
                  body: { description: followUp.description, dueAt: new Date(followUp.dueAt).toISOString() },
                });
                setFollowUp({ description: '', dueAt: toDateTimeLocal() });
              });
            }}>
              <input className={controlClass} placeholder="What needs to happen?" value={followUp.description} onChange={(event) => setFollowUp({ ...followUp, description: event.target.value })} required />
              <input className={controlClass} type="datetime-local" value={followUp.dueAt} onChange={(event) => setFollowUp({ ...followUp, dueAt: event.target.value })} required />
              <Button disabled={busy || lead.archived}>Schedule</Button>
            </form>
            <ul className="mt-4 space-y-3">
              {detail.followUps.length === 0 ? <Empty>No follow-ups yet.</Empty> : detail.followUps.map((item) => (
                <li key={item.id} className="flex items-start justify-between gap-3">
                  <div>
                    <p className="text-sm">{item.description}</p>
                    <p className="text-xs text-stone-500">{item.state} · due {formatDateTime(item.dueAt)} · {item.createdByName}</p>
                  </div>
                  {!item.completed && !lead.archived ? (
                    <Button type="button" variant="ghost" disabled={busy} onClick={() => setConfirm({ kind: 'followup', id: item.id })}>Complete</Button>
                  ) : null}
                </li>
              ))}
            </ul>
          </Card>
        </div>
      </div>
      {confirm?.kind === 'lost' ? (
        <ConfirmDialog title="Mark lead lost" body="This records a reason and stops the lead from moving forward." confirmLabel="Mark lost" tone="danger" busy={busy} onCancel={() => setConfirm(null)} onConfirm={() => run(() => api(`/api/leads/${id}/status`, { method: 'POST', body: { status: 'LOST', lostReason } }))}>
          <textarea className={controlClass} rows="3" value={lostReason} onChange={(event) => setLostReason(event.target.value)} placeholder="Reason" />
        </ConfirmDialog>
      ) : null}
      {confirm?.kind === 'archive' ? (
        <ConfirmDialog title="Archive this lead" body="The lead and its history stay in the database, and it drops out of operational totals." confirmLabel="Archive" tone="danger" busy={busy} onCancel={() => setConfirm(null)} onConfirm={() => run(() => api(`/api/leads/${id}/archive`, { method: 'POST' }))} />
      ) : null}
      {confirm?.kind === 'restore' ? (
        <ConfirmDialog title="Restore this lead" body="The lead returns to operational lists and can be updated again." confirmLabel="Restore" busy={busy} onCancel={() => setConfirm(null)} onConfirm={() => run(() => api(`/api/leads/${id}/restore`, { method: 'POST' }))} />
      ) : null}
      {confirm?.kind === 'followup' ? (
        <ConfirmDialog title="Complete follow-up" body="This marks the follow-up done and keeps the record." confirmLabel="Complete" busy={busy} onCancel={() => setConfirm(null)} onConfirm={() => run(() => api(`/api/follow-ups/${confirm.id}/complete`, { method: 'POST' }))} />
      ) : null}
    </div>
  );
}
