'use strict';

  const ENDPOINT = '/api/v1/me/preferences';

  function copy(value) {
    return JSON.parse(JSON.stringify(value));
  }

  function freeze(value) {
    if (value && typeof value === 'object') {
      Object.values(value).forEach(freeze);
      Object.freeze(value);
    }
    return value;
  }

  function plainObject(value) {
    return Boolean(value) && typeof value === 'object' && !Array.isArray(value);
  }

  function decodeResponse(value) {
    if (!plainObject(value) || !Number.isInteger(value.revision) || value.revision < 1 ||
        !plainObject(value.preferences)) {
      const error = new Error('The server returned an invalid user preference profile.');
      error.code = 'invalid_preferences';
      throw error;
    }
    return { revision: value.revision, preferences: copy(value.preferences) };
  }

  function requestError(response, fallback) {
    const error = new Error(fallback);
    error.status = response?.status || 0;
    if (error.status === 409) error.code = 'preference_conflict';
    return error;
  }

  class Controller {
    constructor(options = {}) {
      if (typeof options.fetch !== 'function') throw new TypeError('A fetch implementation is required.');
      if (typeof options.validate !== 'function') throw new TypeError('A preference validator is required.');
      this.fetch = options.fetch;
      this.validate = options.validate;
      this.defaults = this.validate(copy(options.defaults || {}));
      this.onChange = typeof options.onChange === 'function' ? options.onChange : () => {};
      this.onError = typeof options.onError === 'function' ? options.onError : () => {};
      this.identity = null;
      this.generation = 0;
      this.revision = null;
      this.preferences = copy(this.defaults);
      this.pending = Promise.resolve();
      this.readSnapshot = null;
    }

    snapshot() {
      return Object.freeze({
        identity: this.identity,
        loaded: this.identity !== null && this.revision !== null,
        revision: this.revision,
        preferences: copy(this.preferences)
      });
    }

    // Rendering can read one immutable profile until a confirmed preference change.
    // snapshot() continues to provide independent copies for drafts and callers that edit them.
    read() {
      if (!this.readSnapshot) this.readSnapshot = freeze(this.snapshot());
      return this.readSnapshot;
    }

    emit() {
      this.readSnapshot = null;
      this.onChange(this.snapshot());
    }

    reset(identity = null) {
      this.generation += 1;
      this.identity = identity === null ? null : String(identity);
      this.revision = null;
      this.preferences = copy(this.defaults);
      this.pending = Promise.resolve();
      this.emit();
      return this.generation;
    }

    async activate(identity) {
      const generation = this.reset(identity);
      if (this.identity === null) return this.snapshot();
      try {
        const response = await this.fetch(ENDPOINT, { method: 'GET', cache: 'no-store' });
        if (!response?.ok) throw requestError(response, 'Unable to load user preferences.');
        const decoded = decodeResponse(await response.json());
        const validated = this.validate(decoded.preferences);
        if (generation !== this.generation) return { state: 'stale' };
        this.revision = decoded.revision;
        this.preferences = copy(validated);
        this.emit();
        return this.snapshot();
      } catch (error) {
        if (generation === this.generation) this.onError(error);
        throw error;
      }
    }

    update(mutator) {
      if (typeof mutator !== 'function') throw new TypeError('A preference update function is required.');
      if (this.identity === null || this.revision === null) {
        throw new Error('User preferences are not loaded.');
      }
      return this.enqueue(() => {
        const draft = copy(this.preferences);
        const returned = mutator(draft);
        return this.validate(copy(returned === undefined ? draft : returned));
      });
    }

    enqueue(nextProfile) {
      const generation = this.generation;
      const identity = this.identity;
      const operation = async () => {
        if (generation !== this.generation || identity !== this.identity) return { state: 'stale' };
        const validated = nextProfile();
        const revision = this.revision;
        const previous = copy(this.preferences);
        try {
          const response = await this.fetch(ENDPOINT, {
            method: 'PUT',
            cache: 'no-store',
            headers: { 'Content-Type': 'application/json', Accept: 'application/json', 'If-Match': `"${revision}"` },
            body: JSON.stringify(validated)
          });
          if (!response?.ok) throw requestError(response,
            response?.status === 409 ? 'User preferences changed in another session.' :
              'Unable to save user preferences.');
          const decoded = decodeResponse(await response.json());
          const confirmed = this.validate(decoded.preferences);
          if (generation !== this.generation || identity !== this.identity) return { state: 'stale' };
          this.revision = decoded.revision;
          this.preferences = copy(confirmed);
          this.emit();
          return this.snapshot();
        } catch (error) {
          if (generation !== this.generation || identity !== this.identity) return { state: 'stale', error };
          if (error?.code === 'preference_conflict') {
            try {
              const current = await this.fetch(ENDPOINT, { method: 'GET', cache: 'no-store' });
              if (!current?.ok) throw requestError(current, 'Unable to reload user preferences.');
              const decoded = decodeResponse(await current.json());
              if (generation !== this.generation || identity !== this.identity) return { state: 'stale', error };
              this.revision = decoded.revision;
              this.preferences = copy(this.validate(decoded.preferences));
            } catch (reloadError) {
              this.preferences = previous;
              error.reloadError = reloadError;
            }
          } else this.preferences = previous;
          this.emit();
          this.onError(error);
          throw error;
        }
      };
      this.pending = this.pending.then(operation, operation);
      return this.pending;
    }
  }

export { ENDPOINT, Controller, decodeResponse };
