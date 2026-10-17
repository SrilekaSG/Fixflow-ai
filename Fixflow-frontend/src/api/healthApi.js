import { getJson } from './httpClient.js';

export async function getHealth() {
  try {
    return await getJson('/api/v1/health');
  } catch (error) {
    if (error.status === 503) {
      return { status: 'DEGRADED', database: 'DOWN', analysisMode: 'UNKNOWN' };
    }
    throw error;
  }
}