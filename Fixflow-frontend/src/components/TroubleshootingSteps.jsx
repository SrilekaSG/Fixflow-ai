import CopyButton from './CopyButton.jsx';
import { humanize } from '../utils/labels.js';

export default function TroubleshootingSteps({ steps }) {
  if (!steps || steps.length === 0) {
    return <p className="muted">No steps could be suggested from the available evidence.</p>;
  }
  const allText = steps.map((step) => `${step.order}. ${step.action}`).join('\n');

  return (
    <>
      <ol className="steps">
        {steps.map((step) => (
          <li key={step.order}>
            <p className="step-action">{step.action}</p>
            <p className="muted">
              Why: {step.reason} &middot; Risk: {humanize(step.risk)}
            </p>
            <CopyButton text={step.action} label="Copy step" />
          </li>
        ))}
      </ol>
      <CopyButton text={allText} label="Copy all steps" />
    </>
  );
}