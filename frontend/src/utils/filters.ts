import type { IncidentRow } from '../types/api';

export const EMPTY_FILTER_VALUE = '(empty)';

export type FiltersState = {
  selectedIssueLinks: string[];
  selectedLabels: string[];
  issueKeyQuery: string;
  commentQuery: string;
};

export const EMPTY_FILTERS: FiltersState = {
  selectedIssueLinks: [],
  selectedLabels: [],
  issueKeyQuery: '',
  commentQuery: ''
};

export function normalize(text: string): string {
  return text.trim().toLowerCase();
}

export function toFilterValue(value: string): string {
  const trimmed = value.trim();
  return trimmed ? trimmed : EMPTY_FILTER_VALUE;
}

export function applyFilters(items: IncidentRow[], filters: FiltersState): IncidentRow[] {
  const issueKeyQuery = normalize(filters.issueKeyQuery);
  const commentQuery = normalize(filters.commentQuery);

  return items.filter((item) => {
    const issueLinksValue = toFilterValue(item.issueLinks);
    const labelValue = toFilterValue(item.label);

    if (
      filters.selectedIssueLinks.length > 0
      && !filters.selectedIssueLinks.includes(issueLinksValue)
    ) {
      return false;
    }

    if (filters.selectedLabels.length > 0 && !filters.selectedLabels.includes(labelValue)) {
      return false;
    }

    if (issueKeyQuery && !normalize(item.issueKey).includes(issueKeyQuery)) {
      return false;
    }

    if (commentQuery && !normalize(item.comment).includes(commentQuery)) {
      return false;
    }

    return true;
  });
}
