import { postFile, postJson } from './httpClient.js';

export const previewPasted = (content) => postJson('/api/v1/diagnoses/preview', { content });

export const previewFile = (file) => postFile('/api/v1/diagnoses/preview/upload', file);

export const analyzePasted = ({ content, title }) =>
  postJson('/api/v1/diagnoses', { content, title: title || null });

export const analyzeFile = (file) => postFile('/api/v1/diagnoses/upload', file);