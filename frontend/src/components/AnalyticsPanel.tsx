import {
  PieChart,
  Pie,
  Cell,
  ResponsiveContainer,
  Tooltip
} from 'recharts';
import type { IncidentRow } from '../types/api';
import { buildDistribution, groupTopN, type DistributionRow } from '../utils/analytics';

const COLORS = [
  '#0B57D0',
  '#2E7D32',
  '#EF6C00',
  '#AD1457',
  '#00838F',
  '#6D4C41',
  '#5E35B1',
  '#3949AB',
  '#7CB342'
];

type AnalyticsPanelProps = {
  items: IncidentRow[];
  onToggleIssueLink: (value: string) => void;
  onToggleLabel: (value: string) => void;
};

type SlicePayload = {
  name?: string;
};

export default function AnalyticsPanel({
  items,
  onToggleIssueLink,
  onToggleLabel
}: AnalyticsPanelProps) {
  if (items.length === 0) {
    return <p>Upload a file to see analytics.</p>;
  }

  const byIssueLinks = buildDistribution(items, (item) => item.issueLinks);
  const byLabel = buildDistribution(items, (item) => item.label);

  const issueLinksChart = groupTopN(byIssueLinks, 8);
  const labelChart = groupTopN(byLabel, 8);

  return (
    <div className="analytics-panel">
      <p>
        Total items: <strong>{items.length}</strong> | Unique issueLinks: <strong>{byIssueLinks.length}</strong> |
        {' '}Unique labels: <strong>{byLabel.length}</strong>
      </p>

      <div className="analytics-grid">
        <AnalyticsChart
          title="By Issue Links"
          data={issueLinksChart}
          onSliceClick={onToggleIssueLink}
        />
        <AnalyticsChart
          title="By Label"
          data={labelChart}
          onSliceClick={onToggleLabel}
        />
      </div>

      <p className="analytics-hint">Tip: Click a slice to filter</p>
    </div>
  );
}

type AnalyticsChartProps = {
  title: string;
  data: DistributionRow[];
  onSliceClick: (value: string) => void;
};

function AnalyticsChart({ title, data, onSliceClick }: AnalyticsChartProps) {
  const handleSliceClick = (payload: SlicePayload) => {
    const value = payload.name;
    if (!value || value === 'Other') {
      return;
    }
    onSliceClick(value);
  };

  return (
    <div className="chart-card">
      <h3>{title}</h3>
      <div className="chart-wrap">
        <ResponsiveContainer width="100%" height="100%">
          <PieChart>
            <Pie
              data={data}
              dataKey="count"
              nameKey="name"
              outerRadius={90}
              onClick={(payload: unknown) => handleSliceClick(payload as SlicePayload)}
            >
              {data.map((entry, index) => (
                <Cell key={`${title}-${entry.name}`} fill={COLORS[index % COLORS.length]} />
              ))}
            </Pie>
            <Tooltip formatter={(value) => String(value)} />
          </PieChart>
        </ResponsiveContainer>
      </div>
      <ul className="chart-legend">
        {data.map((row, index) => (
          <li key={`${title}-legend-${row.name}`}>
            <span
              className="legend-dot"
              style={{ backgroundColor: COLORS[index % COLORS.length] }}
              aria-hidden="true"
            />
            <span>{row.name} — {row.count} ({row.percent}%)</span>
          </li>
        ))}
      </ul>
    </div>
  );
}
