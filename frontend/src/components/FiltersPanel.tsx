import { buildDistribution } from '../utils/analytics';
import type { IncidentRow } from '../types/api';
import type { FiltersState } from '../utils/filters';
import { EMPTY_FILTER_VALUE, toFilterValue } from '../utils/filters';

type FiltersPanelProps = {
  items: IncidentRow[];
  filters: FiltersState;
  onChange: (nextFilters: FiltersState) => void;
  onClear: () => void;
};

type OptionRow = {
  name: string;
  count: number;
};

export default function FiltersPanel({ items, filters, onChange, onClear }: FiltersPanelProps) {
  const issueLinkOptions = toOptions(items, (item) => toFilterValue(item.issueLinks));
  const labelOptions = toOptions(items, (item) => toFilterValue(item.label));

  const hasActiveFilters =
    filters.selectedIssueLinks.length > 0
    || filters.selectedLabels.length > 0
    || filters.issueKeyQuery.trim().length > 0
    || filters.commentQuery.trim().length > 0;

  return (
    <div className="filters-panel">
      <div className="filters-grid">
        <div className="filter-card">
          <h3>Issue Links</h3>
          <div className="filter-options-scroll">
            {issueLinkOptions.map((option) => (
              <label key={`issue-link-${option.name}`} className="filter-option">
                <input
                  type="checkbox"
                  checked={filters.selectedIssueLinks.includes(option.name)}
                  onChange={() => {
                    onChange({
                      ...filters,
                      selectedIssueLinks: toggleSelection(filters.selectedIssueLinks, option.name)
                    });
                  }}
                />
                <span>{option.name} ({option.count})</span>
              </label>
            ))}
            {issueLinkOptions.length === 0 ? <p className="muted">No options</p> : null}
          </div>
        </div>

        <div className="filter-card">
          <h3>Label</h3>
          <div className="filter-options-scroll">
            {labelOptions.map((option) => (
              <label key={`label-${option.name}`} className="filter-option">
                <input
                  type="checkbox"
                  checked={filters.selectedLabels.includes(option.name)}
                  onChange={() => {
                    onChange({
                      ...filters,
                      selectedLabels: toggleSelection(filters.selectedLabels, option.name)
                    });
                  }}
                />
                <span>{option.name} ({option.count})</span>
              </label>
            ))}
            {labelOptions.length === 0 ? <p className="muted">No options</p> : null}
          </div>
        </div>

        <div className="filter-card">
          <h3>Search</h3>
          <div className="search-fields">
            <label>
              <span>Issue Key contains</span>
              <input
                type="text"
                value={filters.issueKeyQuery}
                onChange={(event) => {
                  onChange({ ...filters, issueKeyQuery: event.target.value });
                }}
                placeholder="e.g. TSC-123"
              />
            </label>

            <label>
              <span>Comment contains</span>
              <input
                type="text"
                value={filters.commentQuery}
                onChange={(event) => {
                  onChange({ ...filters, commentQuery: event.target.value });
                }}
                placeholder="search comment"
              />
            </label>
          </div>

          <button type="button" onClick={onClear} disabled={!hasActiveFilters}>
            Clear filters
          </button>
        </div>
      </div>

      <p className="muted">Blank issueLinks/label values are grouped as {EMPTY_FILTER_VALUE}.</p>
    </div>
  );
}

function toOptions(items: IncidentRow[], selector: (item: IncidentRow) => string): OptionRow[] {
  return buildDistribution(items, selector).map((row) => ({
    name: row.name,
    count: row.count
  }));
}

function toggleSelection(values: string[], value: string): string[] {
  if (values.includes(value)) {
    return values.filter((entry) => entry !== value);
  }
  return [...values, value];
}
