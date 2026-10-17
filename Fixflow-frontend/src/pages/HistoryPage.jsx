import { useState } from 'react';
import { Link } from 'react-router-dom';
import { deleteDiagnosis, listDiagnoses } from '../api/historyApi.js';
import ConfirmDialog from '../components/ConfirmDialog.jsx';
import ErrorState from '../components/ErrorState.jsx';
import LoadingState from '../components/LoadingState.jsx';
import useAsyncData from '../hooks/useAsyncData.js';
import { formatDate } from '../utils/formatDate.js';
import { MODE_LABELS, humanize } from '../utils/labels.js';

const PAGE_SIZE = 10;

export default function HistoryPage() {
  const [page, setPage] = useState(0);
  const { data, error, loading, reload } = useAsyncData(() => listDiagnoses(page, PAGE_SIZE), [page]);
  const [pending, setPending] = useState(null);
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState(null);

  async function confirmDelete() {
    setDeleting(true);
    setDeleteError(null);
    try {
      await deleteDiagnosis(pending.id);
      const wasLastOnPage = data.items.length === 1 && page > 0;
      setPending(null);
      if (wasLastOnPage) setPage(page - 1);
      else reload();
    } catch (caught) {
      setDeleteError(caught);
      setPending(null);
    } finally {
      setDeleting(false);
    }
  }

  return (
    <>
      <header className="page-header">
        <div>
          <h1>History</h1>
          <p className="muted">Saved diagnoses, newest first.</p>
        </div>
        <Link className="btn btn-primary" to="/new">
          Analyze an error
        </Link>
      </header>

      {deleteError && <ErrorState error={deleteError} title="Could not delete the diagnosis" />}
      {loading && <LoadingState message="Loading history..." />}
      {error && <ErrorState error={error} onRetry={reload} title="Could not load history" />}

      {data && data.items.length === 0 && (
        <div className="card state">
          <p>No diagnoses yet.</p>
          <Link className="btn btn-primary" to="/new">
            Analyze an error
          </Link>
        </div>
      )}

      {data && data.items.length > 0 && (
        <section className="card">
          <div className="table-wrap">
            <table>
              <caption className="sr-only">Saved diagnoses</caption>
              <thead>
                <tr>
                  <th scope="col">Title</th>
                  <th scope="col">Category</th>
                  <th scope="col">Method</th>
                  <th scope="col">Feedback</th>
                  <th scope="col">Date</th>
                  <th scope="col">Actions</th>
                </tr>
              </thead>
              <tbody>
                {data.items.map((item) => (
                  <tr key={item.id}>
                    <td>
                      <Link to={`/diagnoses/${item.id}`}>{item.title || 'Untitled analysis'}</Link>
                    </td>
                    <td>{humanize(item.category)}</td>
                    <td>{MODE_LABELS[item.analysisMode]}</td>
                    <td>{item.feedbackRating ? humanize(item.feedbackRating) : 'None yet'}</td>
                    <td>{formatDate(item.createdAt)}</td>
                    <td>
                      <button type="button" className="btn btn-small btn-danger" onClick={() => setPending(item)}>
                        Delete
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <nav className="pager" aria-label="History pages">
            <button type="button" className="btn" disabled={page === 0} onClick={() => setPage(page - 1)}>
              Previous
            </button>
            <span>
              Page {data.page + 1} of {Math.max(data.totalPages, 1)} ({data.totalElements} total)
            </span>
            <button
              type="button"
              className="btn"
              disabled={page + 1 >= data.totalPages}
              onClick={() => setPage(page + 1)}
            >
              Next
            </button>
          </nav>
        </section>
      )}

      <ConfirmDialog
        open={pending !== null}
        title="Delete this diagnosis?"
        message={`"${pending?.title || 'Untitled analysis'}" will be permanently removed. This cannot be undone.`}
        busy={deleting}
        onConfirm={confirmDelete}
        onCancel={() => setPending(null)}
      />
    </>
  );
}