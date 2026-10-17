export function humanize(value) {
  if (!value) return 'Unknown';
  const text = String(value).toLowerCase().replace(/_/g, ' ');
  return text.charAt(0).toUpperCase() + text.slice(1);
}

export const MODE_LABELS = {
  AI: 'AI-assisted',
  RULE_BASED: 'Rule-based (no AI)',
};

export const STATUS_LABELS = {
  COMPLETED: 'Completed',
  PARTIAL: 'Partial (AI unavailable)',
  FAILED: 'Failed',
};