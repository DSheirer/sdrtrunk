'use strict';

  function invalidResponse(path, message) {
    const error = new Error(message || 'The API returned an invalid collection response.');
    error.code = 'invalid_response';
    error.path = path || null;
    return error;
  }

  function requiredInteger(value, path, field, minimum = 0) {
    if (typeof value !== 'number' || !Number.isInteger(value) || value < minimum) {
      throw invalidResponse(path, `The ${field} value is invalid.`);
    }
    return value;
  }

  function decodeCollection(response, path) {
    if (!response || typeof response !== 'object' || Array.isArray(response) || !Array.isArray(response.rows)) {
      throw invalidResponse(path);
    }
    return { ...response, rows: response.rows.slice() };
  }

  function decodeOffsetPage(response, path) {
    const page = decodeCollection(response, path);
    const limit = requiredInteger(page.limit, path, 'page limit', 1);
    const offset = requiredInteger(page.offset, path, 'page offset');
    if (page.rows.length > limit || typeof page.has_more !== 'boolean') {
      throw invalidResponse(path, 'The collection paging state is invalid.');
    }

    const nextOffset = page.next_offset === null || page.next_offset === undefined ? null :
      requiredInteger(page.next_offset, path, 'next page offset');
    if ((page.has_more && (nextOffset === null || nextOffset <= offset)) ||
        (!page.has_more && nextOffset !== null)) {
      throw invalidResponse(path, 'The collection next page offset is invalid.');
    }

    const decoded = { ...page, limit, offset, has_more: page.has_more, next_offset: nextOffset };
    if (page.total_count !== undefined && page.total_count !== null) {
      decoded.total_count = requiredInteger(page.total_count, path, 'total count');
    }
    return decoded;
  }

  function requiresPageHandling(error) {
    return error?.name === 'AbortError' || error?.status === 401 || error?.status === 403;
  }

  async function run(options = {}) {
    if (typeof options.load !== 'function') throw new TypeError('A lifecycle loader is required.');
    const isCurrent = typeof options.isCurrent === 'function' ? options.isCurrent : () => true;
    let attempt = 0;

    const execute = async () => {
      const currentAttempt = ++attempt;
      const current = () => currentAttempt === attempt && isCurrent();
      if (!current()) return { state: 'stale' };
      options.onLoading?.({ attempt: currentAttempt, retry: currentAttempt > 1 });

      try {
        const value = await options.load();
        if (!current()) return { state: 'stale' };
        options.onReady?.(value);
        return { state: 'ready', value };
      } catch (error) {
        if (!current()) return { state: 'stale', error };
        if (requiresPageHandling(error)) throw error;
        options.onError?.(error, execute);
        return { state: 'error', error };
      }
    };

    return execute();
  }

export { decodeCollection, decodeOffsetPage, requiresPageHandling, run };
