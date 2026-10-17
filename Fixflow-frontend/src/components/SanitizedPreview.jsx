import { humanize } from '../utils/labels.js';

export default function SanitizedPreview({ preview, busy, onBack, onAnalyze }) {
  const types = Object.entries(preview.redactionsByType || {});
  const parsed = preview.parsed;

  return (
    <section className="card" aria-labelledby="preview-title">
      <h2 id="preview-title">Review before analysis</h2>

      <div className="callout callout-warn" role="note">
        <strong>Check this text yourself.</strong> {preview.warning}
      </div>

      <p>
        {preview.redactionCount === 0
          ? 'No likely secrets were detected.'
          : `${preview.redactionCount} likely secret(s) were masked: ${types
              .map(([type, count]) => `${humanize(type)} x${count}`)
              .join(', ')}.`}
      </p>

      <h3>Sanitized text</h3>
      <pre className="log-box" tabIndex={0} aria-label="Sanitized log text">
        {preview.sanitizedContent}
      </pre>

      <h3>What FixFlow found</h3>
      {parsed?.hasEvidence ? (
        <dl className="facts">
          <dt>Exception</dt>
          <dd>{parsed.exceptionClass || 'None found'}</dd>
          <dt>Message</dt>
          <dd>{parsed.errorMessage || 'None'}</dd>
          <dt>Root cause</dt>
          <dd>{parsed.rootCauseClass || 'None found'}</dd>
          <dt>Known patterns</dt>
          <dd>{parsed.keyPatterns?.length ? parsed.keyPatterns.map(humanize).join(', ') : 'None'}</dd>
          <dt>Stack frames</dt>
          <dd>{parsed.stackFrames?.length ?? 0}</dd>
        </dl>
      ) : (
        <p className="muted">
          No recognizable error or exception was found. The result will probably say there is insufficient evidence.
        </p>
      )}

      <div className="actions">
        <button type="button" className="btn" onClick={onBack} disabled={busy}>
          Edit input
        </button>
        <button type="button" className="btn btn-primary" onClick={onAnalyze} disabled={busy}>
          Analyze
        </button>
      </div>
    </section>
  );
}