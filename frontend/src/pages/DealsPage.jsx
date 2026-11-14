import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api';
import { DEAL_STAGE_LABELS, formatDate, formatInr } from '../format';
import { Banner, Button, Card, ConfirmDialog, Empty, PageHeader, Pager, Pill, controlClass, dealTone } from '../ui';

const nextStage = {
  PROPOSAL: ['NEGOTIATION', 'LOST'],
  NEGOTIATION: ['WON', 'LOST'],
  WON: [],
  LOST: [],
};

export default function DealsPage() {
  const [stage, setStage] = useState('');
  const [page, setPage] = useState(0);
  const [result, setResult] = useState(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [confirm, setConfirm] = useState(null);
  const [reason, setReason] = useState('');

  function load() {
    const params = new URLSearchParams({ page, size: 10 });
    if (stage) params.set('stage', stage);
    return api(`/api/deals?${params}`).then(setResult);
  }

  useEffect(() => {
    load().catch((caught) => setError(caught.message));
  }, [stage, page]);

  async function move(id, next, closingReason) {
    setBusy(true);
    setError('');
    try {
      await api(`/api/deals/${id}/stage`, { method: 'POST', body: { stage: next, closingReason } });
      setConfirm(null);
      await load();
    } catch (caught) {
      setError(caught.message);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div>
      <PageHeader title="Deals" subtitle="A qualified lead converts once. Ownership follows that lead." />
      <Banner>{error}</Banner>
      <Card>
        <select className={`${controlClass} max-w-xs`} value={stage} onChange={(event) => { setPage(0); setStage(event.target.value); }}>
          <option value="">All stages</option>
          {Object.entries(DEAL_STAGE_LABELS).map(([value, label]) => <option key={value} value={value}>{label}</option>)}
        </select>
        {!result ? <p className="mt-4 text-sm text-stone-500">Loading deals…</p> : null}
        {result && result.content.length === 0 ? <div className="mt-4"><Empty>No deals in this view.</Empty></div> : null}
        {result && result.content.length > 0 ? (
          <div className="mt-4 overflow-x-auto">
            <table className="w-full text-left text-sm">
              <thead className="text-xs uppercase tracking-wide text-stone-500">
                <tr><th className="py-2">Deal</th><th>Amount</th><th>Stage</th><th>Close</th><th>Next</th></tr>
              </thead>
              <tbody>
                {result.content.map((deal) => (
                  <tr key={deal.id} className="border-t border-[#efe7df]">
                    <td className="py-3">
                      <p className="font-semibold">{deal.title}</p>
                      <Link className="text-[#9a3f1d] hover:underline" to={`/leads/${deal.leadId}`}>{deal.leadName}</Link>
                      <p className="text-xs text-stone-500">{deal.assignedAgentName}</p>
                    </td>
                    <td>{formatInr(deal.amount)}</td>
                    <td><Pill tone={dealTone(deal.stage)}>{DEAL_STAGE_LABELS[deal.stage]}</Pill></td>
                    <td>{formatDate(deal.expectedCloseDate)}{deal.closingReason ? <p className="text-xs text-stone-500">{deal.closingReason}</p> : null}</td>
                    <td>
                      <div className="flex flex-wrap gap-2">
                        {nextStage[deal.stage].map((next) => (
                          <Button key={next} type="button" variant={next === 'LOST' ? 'danger' : 'ghost'} disabled={busy} onClick={() => {
                            if (next === 'LOST' || next === 'WON') {
                              setReason('');
                              setConfirm({ id: deal.id, next });
                            } else {
                              move(deal.id, next, null);
                            }
                          }}>{DEAL_STAGE_LABELS[next]}</Button>
                        ))}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            <Pager page={result.page} totalPages={result.totalPages} onPage={setPage} />
          </div>
        ) : null}
      </Card>
      {confirm ? (
        <ConfirmDialog
          title={confirm.next === 'WON' ? 'Mark deal won' : 'Mark deal lost'}
          body={confirm.next === 'WON' ? 'Won deals stay closed. You can add an optional note.' : 'A lost deal needs a reason and cannot be reopened.'}
          confirmLabel={confirm.next === 'WON' ? 'Mark won' : 'Mark lost'}
          tone="danger"
          busy={busy}
          onCancel={() => setConfirm(null)}
          onConfirm={() => move(confirm.id, confirm.next, reason || null)}
        >
          <textarea className={controlClass} rows="3" value={reason} onChange={(event) => setReason(event.target.value)} placeholder={confirm.next === 'LOST' ? 'Reason' : 'Optional note'} />
        </ConfirmDialog>
      ) : null}
    </div>
  );
}
