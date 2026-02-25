import { useMemo, useState } from 'react';
import { uploadIncidents } from './api/incidentsApi';
import AnalyticsPanel from './components/AnalyticsPanel';
import FiltersPanel from './components/FiltersPanel';
import IncidentsTable from './components/IncidentsTable';
import UploadPanel from './components/UploadPanel';
import type { IncidentRow } from './types/api';
import { applyFilters, EMPTY_FILTERS, type FiltersState } from './utils/filters';

const EXPORT_FILENAME = 'tsc_report_normalized.xlsx';

export default function App() {
  const [healthStatus, setHealthStatus] = useState<string>('not checked');
  const [pingLoading, setPingLoading] = useState(false);

  const [items, setItems] = useState<IncidentRow[]>([]);
  const [hasUploaded, setHasUploaded] = useState(false);
  const [uploadLoading, setUploadLoading] = useState(false);
  const [downloadLoading, setDownloadLoading] = useState(false);
  const [uploadError, setUploadError] = useState<string | null>(null);

  const [filters, setFilters] = useState<FiltersState>({ ...EMPTY_FILTERS });

  const filteredItems = useMemo(() => applyFilters(items, filters), [items, filters]);

  const handlePing = async () => {
    setPingLoading(true);
    try {
      const response = await fetch('/api/health');
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`);
      }
      const text = await response.text();
      setHealthStatus(text);
    } catch (error) {
      const message = error instanceof Error ? error.message : 'unknown error';
      setHealthStatus(`error: ${message}`);
    } finally {
      setPingLoading(false);
    }
  };

  const handleUpload = async (file: File) => {
    setUploadLoading(true);
    setUploadError(null);

    try {
      const result = await uploadIncidents(file);
      setItems(result.items);
      setFilters({ ...EMPTY_FILTERS });
      setHasUploaded(true);
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Upload failed';
      setUploadError(message);
      setHasUploaded(false);
      setItems([]);
      setFilters({ ...EMPTY_FILTERS });
    } finally {
      setUploadLoading(false);
    }
  };

  const handleDownload = async () => {
    setDownloadLoading(true);
    setUploadError(null);

    try {
      const response = await fetch('/api/incidents/export/xlsx');

      if (!response.ok) {
        let message = 'Download failed';
        try {
          const payload = (await response.json()) as { message?: string };
          if (payload.message && payload.message.trim()) {
            message = payload.message;
          }
        } catch {
          // keep fallback message
        }
        throw new Error(message);
      }

      const blob = await response.blob();
      const blobUrl = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = blobUrl;
      link.download = EXPORT_FILENAME;
      document.body.append(link);
      link.click();
      link.remove();
      URL.revokeObjectURL(blobUrl);
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Download failed';
      setUploadError(message);
    } finally {
      setDownloadLoading(false);
    }
  };

  const toggleIssueLinkFilter = (value: string) => {
    setFilters((prev) => ({
      ...prev,
      selectedIssueLinks: toggleFilterValue(prev.selectedIssueLinks, value)
    }));
  };

  const toggleLabelFilter = (value: string) => {
    setFilters((prev) => ({
      ...prev,
      selectedLabels: toggleFilterValue(prev.selectedLabels, value)
    }));
  };

  return (
    <main className="app">
      <h1>TSC Incident Reviewer</h1>

      <section className="panel">
        <h2>Upload</h2>
        <UploadPanel loading={uploadLoading} onUpload={handleUpload} />
        <div className="actions-row">
          <button
            type="button"
            onClick={handleDownload}
            disabled={!hasUploaded || downloadLoading}
          >
            {downloadLoading ? 'Preparing XLSX...' : 'Download normalized XLSX'}
          </button>
        </div>
        {uploadError ? <p className="error">{uploadError}</p> : null}
        {hasUploaded ? <p>Showing {filteredItems.length} of {items.length} incidents</p> : null}
      </section>

      <section className="panel">
        <h2>Filters</h2>
        <FiltersPanel
          items={items}
          filters={filters}
          onChange={setFilters}
          onClear={() => setFilters({ ...EMPTY_FILTERS })}
        />
      </section>

      <section className="panel">
        <h2>Analytics</h2>
        <AnalyticsPanel
          items={filteredItems}
          onToggleIssueLink={toggleIssueLinkFilter}
          onToggleLabel={toggleLabelFilter}
        />
      </section>

      <section className="panel">
        <h2>Incidents</h2>
        <IncidentsTable items={filteredItems} />
      </section>

      <section className="panel">
        <h2>Backend Check</h2>
        <button type="button" onClick={handlePing} disabled={pingLoading}>
          {pingLoading ? 'Pinging...' : 'Ping backend'}
        </button>
        <p>Backend response: {healthStatus}</p>
      </section>
    </main>
  );
}

function toggleFilterValue(values: string[], value: string): string[] {
  if (values.includes(value)) {
    return values.filter((entry) => entry !== value);
  }
  return [...values, value];
}
