import { useState } from 'react';
import { copyToClipboard } from '../utils/copyToClipboard.js';

export default function CopyButton({ text, label = 'Copy' }) {
  const [status, setStatus] = useState('idle');

  async function handleCopy() {
    const ok = await copyToClipboard(text);
    setStatus(ok ? 'copied' : 'failed');
    setTimeout(() => setStatus('idle'), 2000);
  }

  return (
    <span className="copy-wrap">
      <button type="button" className="btn btn-small" onClick={handleCopy}>
        {label}
      </button>
      <span className="copy-status" role="status" aria-live="polite">
        {status === 'copied' && 'Copied'}
        {status === 'failed' && 'Copy failed'}
      </span>
    </span>
  );
}