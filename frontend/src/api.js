export class ApiError extends Error {
  constructor(status, message, fieldErrors = {}) {
    super(message);
    this.status = status;
    this.fieldErrors = fieldErrors;
  }
}

function csrfToken() {
  const row = document.cookie.split('; ').find((part) => part.startsWith('XSRF-TOKEN='));
  return row ? decodeURIComponent(row.split('=').slice(1).join('=')) : '';
}

export async function api(path, { method = 'GET', body } = {}) {
  const headers = { Accept: 'application/json' };
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
  }
  const token = csrfToken();
  if (token && method !== 'GET' && method !== 'HEAD') {
    headers['X-XSRF-TOKEN'] = token;
  }
  const response = await fetch(path, {
    method,
    headers,
    credentials: 'include',
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const text = await response.text();
  const payload = text ? JSON.parse(text) : null;
  if (!response.ok) {
    throw new ApiError(response.status, payload?.message || 'Request failed', payload?.fieldErrors || {});
  }
  return payload;
}
