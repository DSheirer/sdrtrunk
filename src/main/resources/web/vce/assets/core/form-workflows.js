// Reuse map: shared feedback, action rows, and modal busy/dirty guards serve
// inline settings and dialog forms in both themes and viewport sizes.
export function createFormWorkflow({ form, submit, feedback, modal = null,
  ready = true, changed = null, renderFeedback = null }) {
  let busy = false;
  let dirty = false;

  const showFeedback = (state, message = '', error = null) => {
    if (renderFeedback) { renderFeedback(state, message, error); return; }
    if (!feedback) return;
    feedback.classList.remove('ui-feedback', 'ui-feedback-loading', 'ui-feedback-error',
      'ui-notice', 'ui-notice-warning');
    feedback.textContent = message;
    feedback.hidden = !message;
    feedback.setAttribute('role', state === 'error' ? 'alert' : 'status');
    if (!message) return;
    if (state === 'warning') feedback.classList.add('ui-notice', 'ui-notice-warning');
    else {
      feedback.classList.add('ui-feedback');
      if (state === 'loading' || state === 'error') feedback.classList.add(`ui-feedback-${state}`);
    }
  };

  const refresh = () => {
    if (typeof changed === 'function') dirty = Boolean(changed());
    submit.disabled = busy || !ready || (typeof changed === 'function' && !dirty);
    modal?.setDirty(dirty);
  };
  const edited = () => {
    if (busy) return;
    dirty = true;
    refresh();
    showFeedback('status', dirty ? 'Unsaved changes' : '');
  };
  form.addEventListener('input', edited);
  form.addEventListener('change', edited);
  refresh();

  const save = async (operation, { saving = 'Saving…', success = 'Saved.',
    onSuccess = null, onError = null } = {}) => {
    if (busy || !ready || submit.disabled || !form.reportValidity()) return false;
    busy = true;
    const controls = [...form.elements].filter((control) => typeof control.disabled === 'boolean');
    const disabled = controls.map((control) => control.disabled);
    controls.forEach((control) => { control.disabled = true; });
    modal?.setBusy(true);
    form.setAttribute('aria-busy', 'true');
    showFeedback('loading', saving);
    let result;
    let failure = null;
    try {
      result = await operation();
      dirty = false;
      showFeedback('status', success);
    } catch (error) {
      failure = error;
      let notice = null;
      try { notice = onError ? await onError(error) : null; }
      catch (mappingError) { notice = { state: 'error', message: mappingError?.message }; }
      showFeedback(notice?.state || 'error', notice?.message || error?.message || 'The changes could not be saved. Try again.', error);
    } finally {
      controls.forEach((control, index) => { control.disabled = disabled[index]; });
      busy = false;
      modal?.setBusy(false);
      form.removeAttribute('aria-busy');
      refresh();
    }
    if (failure) return false;
    if (onSuccess) {
      try { await onSuccess(result); }
      catch (error) {
        showFeedback('error', error?.message || 'The saved changes could not be refreshed. Try again.');
        return false;
      }
    }
    return true;
  };

  return { refresh, showFeedback, save,
    setDirty: (value) => { dirty = Boolean(value); refresh(); },
    setReady: (value) => { ready = Boolean(value); refresh(); },
    isBusy: () => busy };
}
