import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api';
import { formatDateTime } from '../format';
import { Banner, Button, Card, ConfirmDialog, Empty, PageHeader, Pager } from '../ui';

const views = [
  ['PENDING', 'Pending'],
  ['OVERDUE', 'Overdue'],
  ['COMPLETED', 'Completed'],
];

export default function FollowUpsPage() {
  const [view, setView] = useState('PENDING');
  const [page, setPage] = useState(0);
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');
  const [target, setTarget] = useState(null);
  const [busy, setBusy] = useState(false);

  function load() {
    return api(`/api/follow-ups?view=${view}&page=${page}&size=10`).then(setResult);
  }

  useEffect(() => {
    load().catch((caught) => setError(caught.message));
  }, [view, page]);

  async function complete() {
    setBusy(true);
    try {
      await api(`/api/follow-ups/${target}/complete`, { method: 'POST' });
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
      <PageHeader title="Follow-ups" subtitle="Pending work, overdue items, and completed history." />
      <Banner>{error}</Banner>
      <div className="mb-4 flex gap-2">
        {views.map(([value, label]) => (
          <Button key={value} type="button" variant={view === value ? 'primary' : 'ghost'} onClick={() => { setPage(0); setView(value); }}>{label}</Button>
        ))}
      </div>
      <Card>
        {!result ? <p className="text-sm text-stone-500">Loading follow-ups…</p> : null}
        {result && result.content.length === 0 ? <Empty>Nothing in this list.</Empty> : null}
        {result && result.content.length > 0 ? (
          <ul className="divide-y divide-[#efe7df]">
            {result.content.map((item) => (
              <li key={item.id} className="flex flex-wrap items-center justify-between gap-3 py-3">
                <div>
                  <Link className="font-semibold text-[#9a3f1d] hover:underline" to={`/leads/${item.leadId}`}>{item.leadName}</Link>
                  <p className="text-sm">{item.description}</p>
                  <p className="text-xs text-stone-500">Due {formatDateTime(item.dueAt)} · {item.createdByName}</p>
                </div>
                {view !== 'COMPLETED' ? <Button type="button" variant="ghost" onClick={() => setTarget(item.id)}>Complete</Button> : <span className="text-xs text-stone-500">Completed {formatDateTime(item.completedAt)}</span>}
              </li>
            ))}
          </ul>
        ) : null}
        {result ? <Pager page={result.page} totalPages={result.totalPages} onPage={setPage} /> : null}
      </Card>
      {target ? <ConfirmDialog title="Complete follow-up" body="The follow-up stays on the lead as a completed record." confirmLabel="Complete" busy={busy} onCancel={() => setTarget(null)} onConfirm={complete} /> : null}
    </div>
  );
}
