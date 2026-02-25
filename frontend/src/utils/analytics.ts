import type { IncidentRow } from '../types/api';

export type DistributionRow = {
  name: string;
  count: number;
  percent: number;
};

const EMPTY_NAME = '(empty)';

export function buildDistribution(
  items: IncidentRow[],
  keySelector: (item: IncidentRow) => string
): DistributionRow[] {
  if (items.length === 0) {
    return [];
  }

  const counts = new Map<string, number>();
  for (const item of items) {
    const raw = keySelector(item);
    const name = raw && raw.trim() ? raw.trim() : EMPTY_NAME;
    counts.set(name, (counts.get(name) ?? 0) + 1);
  }

  const total = items.length;
  return Array.from(counts.entries())
    .sort((a, b) => {
      const countDiff = b[1] - a[1];
      if (countDiff !== 0) {
        return countDiff;
      }
      return a[0].localeCompare(b[0]);
    })
    .map(([name, count]) => ({
      name,
      count,
      percent: roundToOneDecimal((count / total) * 100)
    }));
}

export function groupTopN(distribution: DistributionRow[], topN: number): DistributionRow[] {
  if (distribution.length <= topN) {
    return distribution;
  }

  const top = distribution.slice(0, topN);
  const otherCount = distribution.slice(topN).reduce((sum, row) => sum + row.count, 0);
  const totalCount = distribution.reduce((sum, row) => sum + row.count, 0);

  return [
    ...top,
    {
      name: 'Other',
      count: otherCount,
      percent: roundToOneDecimal((otherCount / totalCount) * 100)
    }
  ];
}

function roundToOneDecimal(value: number): number {
  return Math.round(value * 10) / 10;
}
