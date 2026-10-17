export class ApiError extends Error {
  constructor(message, { status = 0, code = 'UNKNOWN', details = [] } = {}) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
    this.details = details;
  }
}

async function request(path, options = {}) {
  let response;
  try {
    response = await fetch(path, {
      ...options,
      headers: { Accept: 'application/json', ...(options.headers || {}) },
    });
  } catch {
    throw new ApiError('Cannot reach the backend. Check that it is running.', { code: 'NETWORK_ERROR' });
  }
  if (response.status === 204) {
    return null;
  }
  const body = await response.json().catch(() => null);
  if (!response.ok) {
    throw new ApiError(body?.message || `Request failed (status ${response.status}).`, {
      status: response.status,
      code: body?.code || 'HTTP_ERROR',
      details: body?.details || [],
    });
  }
  return body;
}

export const getJson = (path) => request(path);

export const postJson = (path, data) =>
  request(path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(data),
  });

export const postFile = (path, file) => {
  const form = new FormData();
  form.append('file', file);
  return request(path, { method: 'POST', body: form });
};

export const deleteRequest = (path) => request(path, { method: 'DELETE' });