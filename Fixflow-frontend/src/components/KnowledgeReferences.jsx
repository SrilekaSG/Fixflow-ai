import { Link } from 'react-router-dom';

export default function KnowledgeReferences({ references }) {
  if (!references || references.length === 0) {
    return <p className="muted">No built-in guides matched this error.</p>;
  }
  return (
    <ul>
      {references.map((reference) => (
        <li key={reference.articleId}>
          <Link to={`/knowledge?article=${encodeURIComponent(reference.articleId)}`}>{reference.title}</Link>{' '}
          <span className="muted">(Built-in FixFlow guide)</span>
        </li>
      ))}
    </ul>
  );
}