import type { IncidentRow } from '../types/api';

const JIRA_BASE = 'https://bitpace.atlassian.net/jira/servicedesk/projects/TSC/queues/custom/189/';

type IncidentsTableProps = {
  items: IncidentRow[];
};

export default function IncidentsTable({ items }: IncidentsTableProps) {
  if (items.length === 0) {
    return <p>No incidents to display.</p>;
  }

  return (
    <div className="table-wrapper">
      <table>
        <thead>
          <tr>
            <th>Issue Key</th>
            <th>Custom Field (Issue Links)</th>
            <th>Label</th>
            <th>Comment</th>
          </tr>
        </thead>
        <tbody>
          {items.map((item, index) => (
            <tr key={`${item.issueKey}-${index}`}>
              <td>
                {item.issueKey ? (
                  <a
                    href={`${JIRA_BASE}${encodeURIComponent(item.issueKey)}`}
                    target="_blank"
                    rel="noreferrer"
                  >
                    {item.issueKey}
                  </a>
                ) : (
                  ''
                )}
              </td>
              <td>{item.issueLinks}</td>
              <td>{item.label}</td>
              <td>{item.comment}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
