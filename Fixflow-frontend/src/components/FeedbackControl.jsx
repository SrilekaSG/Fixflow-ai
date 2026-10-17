import { useState } from 'react';
import { sendFeedback } from '../api/feedbackApi.js';

export default function FeedbackControl({ diagnosisId, initialRating, initialComment, onSaved }) {
  const [rating, setRating] = useState(initialRating || null);
  const [comment, setComment] = useState(initialComment || '');
  const [state, setState] = useState({ busy: false, message: '', error: '' });

  async function save() {
    if (!rating) {
      setState({ busy: false, message: '', error: 'Choose Helpful or Not helpful first.' });
      return;
    }
    setState({ busy: true, message: '', error: '' });
    try {
      const saved = await sendFeedback(diagnosisId, rating, comment.trim());
      setComment(saved.comment || '');
      setState({ busy: false, message: 'Feedback saved.', error: '' });
      onSaved?.(saved);
    } catch (error) {
      setState({ busy: false, message: '', error: error.message });
    }
  }

  return (
    <div>
      <div className="actions" role="group" aria-label="Was this diagnosis helpful?">
        <button
          type="button"
          className={rating === 'HELPFUL' ? 'btn btn-toggle on' : 'btn btn-toggle'}
          aria-pressed={rating === 'HELPFUL'}
          onClick={() => setRating('HELPFUL')}
        >
          Helpful
        </button>
        <button
          type="button"
          className={rating === 'NOT_HELPFUL' ? 'btn btn-toggle on' : 'btn btn-toggle'}
          aria-pressed={rating === 'NOT_HELPFUL'}
          onClick={() => setRating('NOT_HELPFUL')}
        >
          Not helpful
        </button>
      </div>
      <div className="field">
        <label htmlFor="feedback-comment">Comment (optional, up to 500 characters)</label>
        <textarea
          id="feedback-comment"
          rows={3}
          maxLength={500}
          value={comment}
          onChange={(event) => setComment(event.target.value)}
        />
      </div>
      <div className="actions">
        <button type="button" className="btn btn-primary" onClick={save} disabled={state.busy}>
          {state.busy ? 'Saving...' : 'Save feedback'}
        </button>
      </div>
      {state.message && (
        <p className="success" role="status">
          {state.message}
        </p>
      )}
      {state.error && (
        <p className="field-error" role="alert">
          {state.error}
        </p>
      )}
    </div>
  );
}