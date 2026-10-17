import { useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { searchKnowledge } from '../api/knowledgeApi.js';
import ErrorState from '../components/ErrorState.jsx';
import LoadingState from '../components/LoadingState.jsx';
import useAsyncData from '../hooks/useAsyncData.js';
import { humanize } from '../utils/labels.js';

export default function KnowledgeBasePage() {
  const [searchParams] = useSearchParams();
  const selected = searchParams.get('article');
  const [text, setText] = useState('');
  const [query, setQuery] = useState('');
  const { data, error, loading, reload } = useAsyncData(() => searchKnowledge(query), [query]);

  function handleSearch(event) {
    event.preventDefault();
    setQuery(text.trim());
  }

  return (
    <>
      <header className="page-header">
        <div>
          <h1>Knowledge base</h1>
          <p className="muted">Built-in FixFlow guides for common Java and Spring Boot errors.</p>
        </div>
      </header>

      <form className="card search-form" onSubmit={handleSearch} role="search">
        <label htmlFor="kb-search">Search guides</label>
        <div className="search-row">
          <input
            id="kb-search"
            type="search"
            value={text}
            maxLength={200}
            onChange={(event) => setText(event.target.value)}
            placeholder="For example: password, port, NullPointerException"
          />
          <button type="submit" className="btn btn-primary">
            Search
          </button>
        </div>
      </form>

      {loading && <LoadingState message="Searching..." />}
      {error && <ErrorState error={error} onRetry={reload} title="Could not load the knowledge base" />}

      {data && data.length === 0 && <p className="card state">No guides match this search.</p>}

      {data &&
        data.map((article) => (
          <details key={article.id} className="card article" open={article.id === selected}>
            <summary>
              <strong>{article.title}</strong> <span className="muted">({humanize(article.category)})</span>
            </summary>
            <p>{article.explanation}</p>

            <h3>Common causes</h3>
            <ul>
              {article.commonCauses.map((cause) => (
                <li key={cause}>{cause}</li>
              ))}
            </ul>

            <h3>Diagnostic steps</h3>
            <ol>
              {article.diagnosticSteps.map((step) => (
                <li key={step.action}>
                  {step.action} <span className="muted">Why: {step.reason}</span>
                </li>
              ))}
            </ol>

            <h3>Verification</h3>
            <ul>
              {article.verificationSteps.map((step) => (
                <li key={step}>{step}</li>
              ))}
            </ul>

            <h3>Suggested fix</h3>
            <p>{article.suggestedFix}</p>
            <p className="muted">Source: {article.sourceLabel}. Verify environment-specific changes before applying them.</p>
          </details>
        ))}
    </>
  );
}