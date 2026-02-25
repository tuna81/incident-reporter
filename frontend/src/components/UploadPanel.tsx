import { useState, type FormEvent } from 'react';

type UploadPanelProps = {
  loading: boolean;
  onUpload: (file: File) => Promise<void>;
};

export default function UploadPanel({ loading, onUpload }: UploadPanelProps) {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!selectedFile) {
      return;
    }
    await onUpload(selectedFile);
  };

  return (
    <form className="upload-form" onSubmit={handleSubmit}>
      <label htmlFor="xlsx-file">Select .xlsx or csv file</label>
      <input
  id="xlsx-file"
  type="file"
  accept=".xlsx,.csv,text/csv"
  onChange={(event) => setSelectedFile(event.target.files?.[0] ?? null)}
/>
      <p className="file-name">Selected: {selectedFile?.name ?? 'None'}</p>
      <button type="submit" disabled={!selectedFile || loading}>
        {loading ? 'Uploading...' : 'Upload'}
      </button>
    </form>
  );
}
