import { MODE_LABELS, humanize } from '../utils/labels.js';

export default function DiagnosisSummary({ diagnosis }) {
  return (
    <section className="card" aria-labelledby="summary-title">
      <h2 id="summary-title">Summary</h2>

      {diagnosis.needsMoreInformation && (
        <div className="callout callout-warn" role="note">
          <strong>Insufficient evidence.</strong> FixFlow could not reach a reliable conclusion from this log.
          {diagnosis.followUpQuestions?.length > 0 && (
            <>
              {' '}To get a better diagnosis, please provide:
              <ul>
                {diagnosis.followUpQuestions.map((question) => (
                  <li key={question}>{question}</li>
                ))}
              </ul>
            </>
          )}
        </div>
      )}

      <p className="lead">{diagnosis.summary}</p>

      <ul className="badges" aria-label="Diagnosis labels">
        <li className="badge">Category: {humanize(diagnosis.category)}</li>
        <li className="badge">Severity estimate: {humanize(diagnosis.severity)}</li>
        <li className="badge">Confidence: {humanize(diagnosis.confidenceLevel)}</li>
        <li className="badge">Method: {MODE_LABELS[diagnosis.analysisMode] || diagnosis.analysisMode}</li>
      </ul>

      <p className="muted">
        <strong>Why this confidence:</strong> {diagnosis.confidenceExplanation}
      </p>
      {diagnosis.analysisNote && <p className="muted">{diagnosis.analysisNote}</p>}
    </section>
  );
}