import { getJson } from './httpClient.js';

export const searchKnowledge = (query) =>
  getJson(query ? `/api/v1/knowledge?query=${encodeURIComponent(query)}` : '/api/v1/knowledge');