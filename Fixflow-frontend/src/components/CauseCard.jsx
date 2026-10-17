import { humanize } from '../utils/labels.js';

export default function CauseCard({ cause }) {
  return (
    <article className="cause">
      <h3>{cause.cause}</h3>
      <p className="muted">Likelihood: {humanize(cause.likelihood)}</p>
      {cause.evidence?.length > 0 && (
        <>
          <p className="label">Evidence</p>
          <ul>
            {cause.evidence.map((item) => (
              <li key={item}>{item}</li>
            ))}
          </ul>
        </>
      )}
    </article>
  );
}