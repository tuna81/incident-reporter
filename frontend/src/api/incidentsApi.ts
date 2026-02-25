import type { UploadResponse } from '../types/api';

export async function uploadIncidents(file: File): Promise<UploadResponse> {
  const formData = new FormData();
  formData.append('file', file);

  const response = await fetch('/api/incidents/upload', {
    method: 'POST',
    body: formData
  });

  if (!response.ok) {
    let message = 'Upload failed';
    try {
      const payload = (await response.json()) as { message?: string };
      if (payload.message && payload.message.trim()) {
        message = payload.message;
      }
    } catch {
      // Keep fallback message when response is not JSON.
    }
    throw new Error(message);
  }

  return response.json() as Promise<UploadResponse>;
}
