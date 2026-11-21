// REST client for the sdrtrunk web API.  Browser sessions send the CSRF token on state-changing requests.
export const API = '/api/v1';
let csrfToken = null;

export function setCsrfToken(token) {
  csrfToken = token;
}

export class ApiError extends Error {
  constructor(message, status, problem) {
    super(message);
    this.status = status;
    this.problem = problem;
  }
}

export async function api(method, path, body) {
  const headers = {};
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
  }
  if (csrfToken && method !== 'GET') {
    headers['X-CSRF-Token'] = csrfToken;
  }
  const response = await fetch(API + path, {
    method, headers, credentials: 'same-origin',
    body: body !== undefined ? JSON.stringify(body) : undefined
  });
  if (response.status === 204) {
    return null;
  }
  const data = await response.json().catch(() => null);
  if (!response.ok) {
    throw new ApiError(describeProblem(data, response.status), response.status, data);
  }
  return data;
}

// Query string from an object, leaving out empty values
export function query(params) {
  const search = new URLSearchParams();
  for (const [name, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== '') {
      search.set(name, value);
    }
  }
  const text = search.toString();
  return text ? '?' + text : '';
}

export function describeProblem(problem, status) {
  if (!problem) {
    return 'Request failed (' + status + ')';
  }
  let text = problem.detail || problem.title || ('Request failed (' + status + ')');
  if (problem.errors && problem.errors.length) {
    text += ': ' + problem.errors.map((e) => e.field + ' ' + e.message).join('; ');
  }
  return text;
}
