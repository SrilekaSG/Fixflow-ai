import { Link } from 'react-router-dom';

export default function NotFoundPage() {
  return (
    <div className="state">
      <h1>Page not found</h1>
      <p>The page you asked for does not exist.</p>
      <Link className="btn btn-primary" to="/">
        Back to dashboard
      </Link>
    </div>
  );
}