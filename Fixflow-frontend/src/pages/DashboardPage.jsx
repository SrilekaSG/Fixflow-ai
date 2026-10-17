import { Link } from 'react-router-dom';
import { getHealth } from '../api/healthApi.js';
import { getStats, listDiagnoses } from '../api/historyApi.js';
import ErrorState from '../components/ErrorState.jsx';
import LoadingState from '../components/LoadingState.jsx';
import useAsyncData from '../hooks/useAsyncData.js';
import { formatDate } from '../utils/formatDate.js';
import { MODE_LABELS, humanize } from '../utils/labels.js';

export default function DashboardPage() {
  const { data, error, loading, reload } = useAsyncData(
    () => Promise.all([getStats(), getHealth(), listDiagnoses(0, 5)]),
    []
  );

  if (loading) return <LoadingState message="Loading dashboard..." />;
  if (error) return <ErrorState error={error} onRetry={reload} title="Could not load the dashboard" />;

  const [stats, health, recent] = data;
  const rated = stats.helpfulCount + stats.notHelpfulCount;

  return (
    <>
      <header className="page-header">
        <div>
          <h1>Dashboard</h1>
          <p className="muted">Diagnose Java and Spring Boot errors from pasted text or log files.</p>
        </div>
        <Link className="btn btn-primary" to="/new">
          Analyze an error
        </Link>
      </header>

      <div className="card-grid">
        <section className="card stat" aria-label="Total diagnoses">
          <p className="stat-value">{stats.totalDiagnoses}</p>
          <p className="muted">Total diagnoses saved</p>
        </section>
        <section className="card stat" aria-label="Helpful ratings">
          <p className="stat-value">{stats.helpfulCount}</p>
          <p className="muted">Marked helpful (out of {rated} rated)</p>
        </section>
        <section className="card stat" aria-label="Analysis methods">
          <p className="stat-value">
            {stats.aiAnalysisCount} / {stats.ruleBasedCount}
          </p>
          <p className="muted">AI-assisted / rule-based results</p>
        </section>
        <section className="card" aria-label="System status">
          <h2 className="small-heading">System status</h2>
          <dl className="facts">
            <dt>Backend</dt>
            <dd>{health.status}</dd>
            <dt>Database</dt>
            <dd>{health.database}</dd>
            <dt>Analysis mode</dt>
            <dd>{health.analysisMode === 'AI_CONFIGURED' ? 'AI configured' : 'Rule-based (AI not configured)'}</dd>
          </dl>
        </section>
      </div>

      <section className="card" aria-labelledby="recent-title">
        <h2 id="recent-title">Recent analyses</h2>
        {recent.items.length === 0 ? (
          <div className="state">
            <p>No diagnoses yet. Analyze your first error to see it here.</p>
            <Link className="btn btn-primary" to="/new">
              Analyze an error
            </Link>
          </div>
        ) : (
          <ul className="list">
            {recent.items.map((item) => (
              <li key={item.id}>
                <Link to={`/diagnoses/${item.id}`}>{item.title || 'Untitled analysis'}</Link>
                <span className="muted">
                  {' '}
                  {humanize(item.category)} &middot; {MODE_LABELS[item.analysisMode]} &middot; {formatDate(item.createdAt)}
                </span>
              </li>
            ))}
          </ul>
        )}
        {recent.totalElements > recent.items.length && <Link to="/history">View all history</Link>}
      </section>
    </>
  );
}