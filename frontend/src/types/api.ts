export type IncidentRow = {
  issueKey: string;
  issueLinks: string;
  label: string;
  comment: string;
};

export type UploadResponse = {
  items: IncidentRow[];
  stats: unknown;
};
