export default function ErrorState({ error, onRetry, title = 'Something went wrong' }) {
  const message = error?.message || 'Unexpected error.';
  const details = Array.isArray(error?.details) ? error.details : [];
  return (
    <div className="state state-error" role="alert">
      <h2>{title}</h2>
      <p>{message}</p>
      {details.length > 0 && (
        <ul>
          {details.map((detail) => (
            <li key={detail}>{detail}</li>
          ))}
        </ul>
      )}
      {onRetry && (
        <button type="button" className="btn" onClick={onRetry}>
          Try again
        </button>
      )}
    </div>
  );
}