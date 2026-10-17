import { postJson } from './httpClient.js';

export const sendFeedback = (id, rating, comment) =>
  postJson(`/api/v1/diagnoses/${encodeURIComponent(id)}/feedback`, { rating, comment: comment || null });