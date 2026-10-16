'use strict';

  const API_PATH = '/api/v1/radio-systems';
  const DIRECTORY_LIMIT = 25;

  function invalidResponse(message) {
    const error = new Error(message || 'The radio systems directory response is invalid.');
    error.code = 'invalid_response';
    error.path = API_PATH;
    return error;
  }

  function requiredPreviewLimit(value) {
    if (typeof value !== 'number' || !Number.isInteger(value) || value < 1) {
      throw invalidResponse('The radio systems directory channel preview limit is invalid.');
    }
    return value;
  }

  function decode(page) {
    if (!page || typeof page !== 'object' || Array.isArray(page) || !Array.isArray(page.rows)) {
      throw invalidResponse();
    }

    const parentRows = page.rows;
    const tableRows = [];
    let truncatedParentCount = 0;

    parentRows.forEach((system) => {
      if (!system || typeof system !== 'object' || Array.isArray(system) ||
          !Array.isArray(system.channel_preview) || typeof system.channel_preview_truncated !== 'boolean') {
        throw invalidResponse('A radio system row has an invalid channel preview.');
      }

      tableRows.push({ ...system, directory_type: 'radio_system' });
      system.channel_preview.forEach((channel) => {
        if (!channel || typeof channel !== 'object' || Array.isArray(channel)) {
          throw invalidResponse('A radio system channel preview is invalid.');
        }
        tableRows.push({ ...channel, directory_type: 'channel' });
      });

      if (system.channel_preview_truncated) truncatedParentCount += 1;
    });

    return {
      parentRows,
      tableRows,
      page,
      previewLimit: requiredPreviewLimit(page.channel_preview_limit_per_system),
      truncatedParentCount
    };
  }

  async function load(apiPage, parameters = {}) {
    if (typeof apiPage !== 'function') throw new TypeError('A radio systems page request function is required.');
    const page = await apiPage(API_PATH, {
      ...parameters,
      limit: DIRECTORY_LIMIT,
      includeChannelPreview: true
    });
    return decode(page);
  }

export { load, decode };
