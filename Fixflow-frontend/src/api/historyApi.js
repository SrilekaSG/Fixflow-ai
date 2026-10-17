import { deleteRequest, getJson } from './httpClient.js';

export const listDiagnoses = (page = 0, size = 10) => getJson(`/api/v1/diagnoses?page=${page}&size=${size}`);

export const getDiagnosisDetail = (id) => getJson(`/api/v1/diagnoses/${encodeURIComponent(id)}`);

export const deleteDiagnosis = (id) => deleteRequest(`/api/v1/diagnoses/${encodeURIComponent(id)}`);

export const getStats = () => getJson('/api/v1/stats');