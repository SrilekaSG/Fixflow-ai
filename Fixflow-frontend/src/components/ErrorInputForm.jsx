import { useState } from 'react';
import FileUpload from './FileUpload.jsx';

export default function ErrorInputForm({ busy, initial, onSubmit }) {
  const [mode, setMode] = useState(initial?.mode || 'paste');
  const [content, setContent] = useState(initial?.content || '');
  const [title, setTitle] = useState(initial?.title || '');
  const [file, setFile] = useState(initial?.file || null);
  const [problem, setProblem] = useState('');

  function handleSubmit(event) {
    event.preventDefault();
    if (mode === 'paste' && !content.trim()) {
      setProblem('Paste an error message or log first.');
      return;
    }
    if (mode === 'upload' && !file) {
      setProblem('Choose a .log or .txt file first.');
      return;
    }
    setProblem('');
    onSubmit({ mode, content, title: title.trim(), file });
  }

  return (
    <form className="card" onSubmit={handleSubmit} noValidate>
      <div className="tabs" role="tablist" aria-label="Input method">
        <button
          type="button"
          role="tab"
          id="tab-paste"
          aria-selected={mode === 'paste'}
          aria-controls="panel-input"
          className={mode === 'paste' ? 'tab active' : 'tab'}
          onClick={() => setMode('paste')}
        >
          Paste log
        </button>
        <button
          type="button"
          role="tab"
          id="tab-upload"
          aria-selected={mode === 'upload'}
          aria-controls="panel-input"
          className={mode === 'upload' ? 'tab active' : 'tab'}
          onClick={() => setMode('upload')}
        >
          Upload file
        </button>
      </div>

      <div id="panel-input" role="tabpanel" aria-labelledby={mode === 'paste' ? 'tab-paste' : 'tab-upload'}>
        {mode === 'paste' ? (
          <>
            <div className="field">
              <label htmlFor="log-title">Title (optional)</label>
              <input
                id="log-title"
                type="text"
                maxLength={200}
                value={title}
                onChange={(event) => setTitle(event.target.value)}
                placeholder="For example: Backend fails to start"
              />
            </div>
            <div className="field">
              <label htmlFor="log-content">Error message or log</label>
              <textarea
                id="log-content"
                rows={12}
                value={content}
                onChange={(event) => setContent(event.target.value)}
                placeholder="Paste the error message or stack trace here"
                spellCheck={false}
              />
            </div>
          </>
        ) : (
          <FileUpload file={file} onChange={setFile} />
        )}
      </div>

      {problem && (
        <p className="field-error" role="alert">
          {problem}
        </p>
      )}
      <div className="actions">
        <button type="submit" className="btn btn-primary" disabled={busy}>
          Review sanitized preview
        </button>
      </div>
    </form>
  );
}