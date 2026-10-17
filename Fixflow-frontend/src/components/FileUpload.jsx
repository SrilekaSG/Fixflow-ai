import { useState } from 'react';

// Keep MAX_MB equal to APP_MAX_UPLOAD_MB on the backend (the backend always enforces the real limit).
const MAX_MB = 1;
const ALLOWED_EXTENSIONS = ['.log', '.txt'];

export default function FileUpload({ file, onChange }) {
  const [problem, setProblem] = useState('');

  function reject(event, message) {
    setProblem(message);
    onChange(null);
    event.target.value = '';
  }

  function handleChange(event) {
    const selected = event.target.files?.[0];
    if (!selected) return;
    const lowerName = selected.name.toLowerCase();
    if (!ALLOWED_EXTENSIONS.some((extension) => lowerName.endsWith(extension))) {
      reject(event, 'Only .log and .txt files are supported.');
      return;
    }
    if (selected.size === 0) {
      reject(event, 'The selected file is empty.');
      return;
    }
    if (selected.size > MAX_MB * 1024 * 1024) {
      reject(event, `The file is larger than the ${MAX_MB} MB limit.`);
      return;
    }
    setProblem('');
    onChange(selected);
  }

  return (
    <div className="field">
      <label htmlFor="log-file">Log file (.log or .txt, up to {MAX_MB} MB)</label>
      <input id="log-file" type="file" accept=".log,.txt,text/plain" onChange={handleChange} />
      {file && (
        <p className="muted">
          Selected: {file.name} ({Math.max(1, Math.ceil(file.size / 1024))} KB)
        </p>
      )}
      {problem && (
        <p className="field-error" role="alert">
          {problem}
        </p>
      )}
    </div>
  );
}