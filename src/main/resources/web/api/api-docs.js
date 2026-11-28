// API reference: Swagger UI over the generated OpenAPI document.  When the browser is signed in, "Try it out"
// requests use the session cookie and send its CSRF token.
(async () => {
  let csrfToken = null;
  try {
    const response = await fetch('/api/v1/auth/me', {credentials: 'same-origin'});
    if (response.ok) {
      csrfToken = (await response.json()).csrfToken;
    }
  } catch (e) {
    // Not signed in
  }
  window.ui = SwaggerUIBundle({
    url: '/api/v1/openapi.json',
    dom_id: '#swagger-ui',
    deepLinking: true,
    tryItOutEnabled: false,
    persistAuthorization: false,
    requestInterceptor: (request) => {
      if (csrfToken && request.method && request.method.toUpperCase() !== 'GET') {
        request.headers['X-CSRF-Token'] = csrfToken;
      }
      return request;
    }
  });
})();
