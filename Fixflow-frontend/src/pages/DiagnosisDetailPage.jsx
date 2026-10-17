import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { deleteDiagnosis, getDiagnosisDetail } from '../api/historyApi.js';
import CauseCard from '../components/CauseCard.jsx';
import ConfirmDialog from '../components/ConfirmDialog.jsx';
import CopyButton from '../components/CopyButton.jsx';
import DiagnosisSummary from '../components/DiagnosisSummary.jsx';
import ErrorState from '../components/ErrorState.jsx';
import FeedbackControl from '../components/FeedbackControl.jsx';
import KnowledgeReferences from '../components/KnowledgeReferences.jsx';
import LoadingState from '../components/LoadingState.jsx';
import TroubleshootingSteps from '../components/TroubleshootingSteps.jsx';
import useAsyncData from '../hooks/useAsyncData.js';
import { formatDate } from '../utils/formatDate.js';
import { STATUS_LABELS } from '../utils/labels.js';

export default function DiagnosisDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { data, error, loading, reload } = useAsyncData(() => getDiagnosisDetail(id), [id]);
  const [confirming, setConfirming] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState(null);

  async function handleDelete() {
    setDeleting(true);
    setDeleteError(null);
    try {
      await deleteDiagnosis(id);
      navigate('/history');
    } catch (caught) {
      setDeleteError(caught);
      setDeleting(false);
      setConfirming(false);
    }
  }

  if (loading) return <LoadingState message="Loading diagnosis..." />;
  if (error) {
    const notFound = error.status === 404;
    return (
      <>
        <ErrorState
          error={error}
          onRetry={notFound ? undefined : reload}
          title={notFound ? 'Diagnosis not found' : 'Could not load the diagnosis'}
        />
        <p>
          <Link to="/history">Back to history</Link>
        </p>
      </>
    );
  }

  const diagnosis = data.diagnosis;
  const verificationText = (diagnosis.verificationSteps || []).map((step, index) => `${index + 1}. ${step}`).join('\n');

  return (
    <>
      <header className="page-header">
        <div>
          <h1>{data.title || 'Diagnosis'}</h1>
          <p className="muted">
            {formatDate(data.createdAt)} &middot; {STATUS_LABELS[data.status] || data.status}
            {data.fileName ? ` · File: ${data.fileName}` : ''}
          </p>
        </div>
        <button type="button" className="btn btn-danger" onClick={() => setConfirming(true)}>
          Delete
        </button>
      </header>

      {deleteError && <ErrorState error={deleteError} title="Could not delete the diagnosis" />}

      {data.status === 'PARTIAL' && (
        <div className="callout callout-warn" role="alert">
          <strong>AI analysis was not used for this result.</strong> The AI service was unavailable or returned
          an invalid response, so this is the rule-based result.
        </div>
      )}

      <DiagnosisSummary diagnosis={diagnosis} />

      <section className="card" aria-labelledby="evidence-title">
        <h2 id="evidence-title">Evidence</h2>
        <p className="muted">
          {data.redactionCount > 0
            ? `${data.redactionCount} likely secret(s) were masked before analysis and storage.`
            : 'No likely secrets were detected in this input.'}
        </p>
        <details>
          <summary>Show the sanitized log that was analyzed</summary>
          <pre className="log-box" tabIndex={0}>
            {data.sanitizedInput}
          </pre>
        </details>
      </section>

      <section className="card" aria-labelledby="causes-title">
        <h2 id="causes-title">Probable causes</h2>
        {diagnosis.probableCauses?.length > 0 ? (
          diagnosis.probableCauses.map((cause) => <CauseCard key={cause.cause} cause={cause} />)
        ) : (
          <p className="muted">No probable causes could be listed from the available evidence.</p>
        )}
      </section>

      <section className="card" aria-labelledby="steps-title">
        <h2 id="steps-title">Recommended steps</h2>
        <TroubleshootingSteps steps={diagnosis.recommendedSteps} />
      </section>

      <section className="card" aria-labelledby="fix-title">
        <h2 id="fix-title">Suggested fix</h2>
        <p>{diagnosis.suggestedFix}</p>
        <p className="muted">This is a proposal only. FixFlow never changes or runs anything. Review it before applying.</p>
      </section>

      <section className="card" aria-labelledby="verify-title">
        <h2 id="verify-title">Verification</h2>
        {diagnosis.verificationSteps?.length > 0 ? (
          <>
            <ol>
              {diagnosis.verificationSteps.map((step) => (
                <li key={step}>{step}</li>
              ))}
            </ol>
            <CopyButton text={verificationText} label="Copy verification steps" />
          </>
        ) : (
          <p className="muted">No verification steps were suggested.</p>
        )}
      </section>

      <section className="card" aria-labelledby="refs-title">
        <h2 id="refs-title">Knowledge references</h2>
        <KnowledgeReferences references={diagnosis.knowledgeReferences} />
      </section>

      <section className="card" aria-labelledby="limits-title">
        <h2 id="limits-title">Limitations</h2>
        <ul>
          {(diagnosis.limitations || []).map((limit) => (
            <li key={limit}>{limit}</li>
          ))}
        </ul>
      </section>

      <section className="card" aria-labelledby="feedback-title">
        <h2 id="feedback-title">Was this helpful?</h2>
        <FeedbackControl
          diagnosisId={data.id}
          initialRating={data.feedbackRating}
          initialComment={data.feedbackComment}
        />
      </section>

      <ConfirmDialog
        open={confirming}
        title="Delete this diagnosis?"
        message="This permanently removes the saved diagnosis and its sanitized log. This cannot be undone."
        busy={deleting}
        onConfirm={handleDelete}
        onCancel={() => setConfirming(false)}
      />
    </>
  );
}