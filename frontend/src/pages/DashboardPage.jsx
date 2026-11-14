import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../api';
import { DEAL_STAGE_LABELS, LEAD_STATUS_LABELS, formatDateTime, formatInr } from '../format';
import { Banner, Card, Empty, PageHeader, Pill, leadTone } from '../ui';

export default function DashboardPage() {
  const [data, setData] = useState(null);
  const [error, setError] = useState('');

  useEffect(() => {
    api('/api/dashboard').then(setData).catch((caught) => setError(caught.message));
  }, []);

  const cards = data ? [
    ['Active leads', data.activeLeadCount],
    ['Qualified leads', data.qualifiedLeadCount],
    ['Open deal value', formatInr(data.openDealValue)],
    ['Won deal value', formatInr(data.wonDealValue)],
    ['Overdue follow-ups', data.overdueFollowUpCount],
  ] : [];

  return (
    <div>
      <PageHeader title="Dashboard" subtitle="Numbers stay inside the leads you are allowed to see." />
      <Banner>{error}</Banner>
      {!data && !error ? <p className="text-sm text-stone-500">Loading totals…</p> : null}
      {data ? (
        <>
          <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-5">
            {cards.map(([label, value]) => (
              <Card key={label}>
                <p className="text-xs font-semibold uppercase tracking-wide text-stone-500">{label}</p>
                <p className="mt-2 text-2xl font-semibold">{value}</p>
              </Card>
            ))}
          </div>
          <div className="mt-6 grid gap-4 lg:grid-cols-2">
            <Card>
              <h2 className="font-semibold">Leads by status</h2>
              <ul className="mt-4 space-y-3">
                {data.leadsByStatus.map((row) => (
                  <li key={row.status} className="flex items-center justify-between">
                    <Pill tone={leadTone(row.status)}>{LEAD_STATUS_LABELS[row.status]}</Pill>
                    <span className="font-semibold">{row.count}</span>
                  </li>
                ))}
              </ul>
            </Card>
            <Card>
              <h2 className="font-semibold">Upcoming follow-ups</h2>
              {data.upcomingFollowUps.length === 0 ? <div className="mt-4"><Empty>No upcoming follow-ups.</Empty></div> : (
                <ul className="mt-4 divide-y divide-[#efe7df]">
                  {data.upcomingFollowUps.map((item) => (
                    <li key={item.id} className="py-3">
                      <Link className="font-medium text-[#9a3f1d] hover:underline" to={`/leads/${item.leadId}`}>{item.leadName}</Link>
                      <p className="text-sm text-stone-700">{item.description}</p>
                      <p className="text-xs text-stone-500">{formatDateTime(item.dueAt)}</p>
                    </li>
                  ))}
                </ul>
              )}
            </Card>
          </div>
          <p className="mt-4 text-xs text-stone-500">Open deals are in {DEAL_STAGE_LABELS.PROPOSAL} or {DEAL_STAGE_LABELS.NEGOTIATION}. Archived leads are left out of these totals.</p>
        </>
      ) : null}
    </div>
  );
}
