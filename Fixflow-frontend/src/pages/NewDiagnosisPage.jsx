import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { analyzeFile, analyzePasted, previewFile, previewPasted } from '../api/diagnosisApi.js';
import ErrorInputForm from '../components/ErrorInputForm.jsx';
import ErrorState from '../components/ErrorState.jsx';
import LoadingState from '../components/LoadingState.jsx';
import SanitizedPreview from '../components/SanitizedPreview.jsx';

export default function NewDiagnosisPage() {
  const navigate = useNavigate();
  const [step, setStep] = useState('input');
  const [input, setInput] = useState(null);
  const [preview, setPreview] = useState(null);
  const [error, setError] = useState(null);

  async function handleReview(values) {
    setError(null);
    setInput(values);
    setStep('previewing');
    try {
      const result =
        values.mode === 'upload' ? await previewFile(values.file) : await previewPasted(values.content);
      setPreview(result);
      setStep('preview');
    } catch (caught) {
      setError(caught);
      setStep('input');
    }
  }

  async function handleAnalyze() {
    setError(null);
    setStep('analyzing');
    try {
      const result =
        input.mode === 'upload'
          ? await analyzeFile(input.file)
          : await analyzePasted({ content: input.content, title: input.title });
      navigate(`/diagnoses/${result.diagnosisId}`);
    } catch (caught) {
      setError(caught);
      setStep('preview');
    }
  }

  return (
    <>
      <header className="page-header">
        <div>
          <h1>Analyze an error</h1>
          <p className="muted">Paste an error message or upload a .log / .txt file.</p>
        </div>
      </header>

      {error && <ErrorState error={error} title="Could not complete the request" />}

      {step === 'input' && <ErrorInputForm initial={input} busy={false} onSubmit={handleReview} />}
      {step === 'previewing' && <LoadingState message="Masking secrets and reading the log..." />}
      {step === 'preview' && preview && (
        <SanitizedPreview
          preview={preview}
          busy={false}
          onBack={() => setStep('input')}
          onAnalyze={handleAnalyze}
        />
      )}
      {step === 'analyzing' && <LoadingState message="Analyzing... AI-assisted analysis can take up to 30 seconds." />}
    </>
  );
}