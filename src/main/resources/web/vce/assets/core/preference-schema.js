'use strict';

  const defaults = Object.freeze({
    version: 11,
    appearance: Object.freeze({ theme: 'light', hue: null }),
    page_titles: Object.freeze({ prepend_playing_call: true }),
    playback: Object.freeze({
      volume: 1,
      selected_scan_list_ids: Object.freeze([]),
      target_grouping: true,
      target_burst_limit: 4
    }),
    scanner: Object.freeze({ detail_mode: 'normal' }),
    presentation: Object.freeze({
      source_name_display: 'talker_alias',
      show_encryption_details: true,
      show_control_decode_quality: true,
      show_voice_decode_quality: true,
      decode_quality_display_mode: 'percentage',
      live_detail_row_limit: 200,
      show_only_active_trunked_channels: true,
      live_channel_sort: 'order_appeared',
      live_row_density: 'normal',
      retain_last_call_on_idle_rows: false,
      clear_voice_quality_when_idle: false
    }),
    tuner: Object.freeze({
      floor_db: -140,
      ceiling_db: 0,
      waterfall_speed: 1,
      snap_frequency: true,
      smooth_fft: true,
      highlight_waterfall_channels: false,
      show_idle_channels: false,
      profile: 'balanced'
    }),
    health_alerts: Object.freeze({ disabled_codes: Object.freeze([]) }),
    tables: Object.freeze({})
  });

  function plain(value, name) {
    if (!value || typeof value !== 'object' || Array.isArray(value)) throw invalid(`${name} must be an object.`);
    return value;
  }

  function invalid(message) {
    const error = new Error(message || 'The user preference profile is invalid.');
    error.code = 'invalid_preferences';
    return error;
  }

  function exact(value, keys, name) {
    const actual = Object.keys(plain(value, name)).sort();
    const expected = keys.slice().sort();
    if (actual.length !== expected.length || actual.some((key, index) => key !== expected[index])) {
      throw invalid(`${name} contains unknown or missing settings.`);
    }
  }

  function oneOf(value, values, name) {
    if (!values.includes(value)) throw invalid(`${name} is invalid.`);
    return value;
  }

  function bool(value, name) {
    if (typeof value !== 'boolean') throw invalid(`${name} must be true or false.`);
    return value;
  }

  function number(value, minimum, maximum, name, integer = false) {
    if (typeof value !== 'number' || !Number.isFinite(value) || value < minimum || value > maximum ||
        (integer && !Number.isInteger(value))) throw invalid(`${name} is invalid.`);
    return value;
  }

  function columnIds(value, name) {
    if (!Array.isArray(value) || value.length > 128) throw invalid(`${name} is invalid.`);
    const values = value.map((id) => {
      if (typeof id !== 'string' || !/^[a-z][a-z0-9]*(?:[._-][a-z0-9]+)*$/.test(id) || id.length > 64) {
        throw invalid(`${name} is invalid.`);
      }
      return id;
    });
    if (new Set(values).size !== values.length) throw invalid(`${name} contains duplicate columns.`);
    return values;
  }

  function alertCodes(value, name) {
    if (!Array.isArray(value) || value.length > 128) throw invalid(`${name} is invalid.`);
    const values = value.map((code) => {
      if (typeof code !== 'string' || !/^[a-z][a-z0-9]*(?:[._-][a-z0-9]+)*$/.test(code) ||
          code.length > 64) {
        throw invalid(`${name} is invalid.`);
      }
      return code;
    });
    if (new Set(values).size !== values.length) throw invalid(`${name} contains duplicate alert codes.`);
    return values;
  }

  function table(value, name) {
    exact(value, ['schema', 'column_order', 'column_widths', 'hidden_columns', 'collapsed_groups'], name);
    const schema = columnIds(value.schema, `${name}.schema`);
    if (schema.length === 0) {
      throw invalid(`${name}.schema must contain at least one column.`);
    }
    const order = columnIds(value.column_order, `${name}.column_order`);
    if (schema.length !== order.length || schema.some((id) => !order.includes(id))) {
      throw invalid(`${name} column order does not match its schema.`);
    }
    const widths = plain(value.column_widths, `${name}.column_widths`);
    if (Object.keys(widths).length > schema.length || Object.keys(widths).some((id) => !schema.includes(id))) {
      throw invalid(`${name} contains an unknown column width.`);
    }
    const columnWidths = Object.fromEntries(Object.entries(widths).map(([id, width]) =>
      [id, number(width, 48, 1200, `${name}.${id} width`, true)]));
    const hidden = columnIds(value.hidden_columns, `${name}.hidden_columns`);
    if (hidden.some((id) => !schema.includes(id))) throw invalid(`${name} hides an unknown column.`);
    if (hidden.length === schema.length) {
      throw invalid(`${name} must keep at least one visible column.`);
    }
    const collapsedGroups = columnIds(value.collapsed_groups, `${name}.collapsed_groups`);
    return { schema, column_order: order, column_widths: columnWidths, hidden_columns: hidden,
      collapsed_groups: collapsedGroups };
  }

  function validate(value) {
    exact(value, ['version', 'appearance', 'page_titles', 'playback', 'scanner', 'presentation', 'tuner',
      'health_alerts', 'tables'], 'preferences');
    if (value.version !== 11) throw invalid('The user preference version is unsupported.');
    exact(value.appearance, ['theme', 'hue'], 'appearance');
    exact(value.page_titles, ['prepend_playing_call'], 'page_titles');
    exact(value.playback, ['volume', 'selected_scan_list_ids', 'target_grouping',
      'target_burst_limit'], 'playback');
    exact(value.scanner, ['detail_mode'], 'scanner');
    exact(value.presentation, ['source_name_display', 'show_encryption_details', 'show_control_decode_quality',
      'show_voice_decode_quality', 'decode_quality_display_mode', 'live_detail_row_limit',
      'show_only_active_trunked_channels', 'live_channel_sort', 'live_row_density', 'retain_last_call_on_idle_rows',
      'clear_voice_quality_when_idle'], 'presentation');
    exact(value.tuner, ['floor_db', 'ceiling_db', 'waterfall_speed', 'snap_frequency', 'smooth_fft',
      'highlight_waterfall_channels', 'show_idle_channels', 'profile'], 'tuner');
    exact(value.health_alerts, ['disabled_codes'], 'health_alerts');
    const tables = plain(value.tables, 'tables');
    if (Object.keys(tables).length > 128) throw invalid('Too many table layouts are stored.');
    const decodedTables = Object.fromEntries(Object.entries(tables).map(([id, layout]) => {
      if (!/^[a-z][a-z0-9]*(?:[._-][a-z0-9]+)*$/.test(id) || id.length > 64) {
        throw invalid('A table ID is invalid.');
      }
      return [id, table(layout, `tables.${id}`)];
    }));
    const scanListIds = value.playback.selected_scan_list_ids;
    if (!Array.isArray(scanListIds) || scanListIds.length > 16 ||
        scanListIds.some((id) => !Number.isSafeInteger(id) || id <= 0) ||
        new Set(scanListIds).size !== scanListIds.length) throw invalid('Selected scan lists are invalid.');
    const floor = number(value.tuner.floor_db, -200, -5, 'tuner.floor_db', true);
    const ceiling = number(value.tuner.ceiling_db, -195, 0, 'tuner.ceiling_db', true);
    if (ceiling - floor < 5) throw invalid('The tuner display range is too small.');
    return {
      version: 11,
      appearance: { theme: oneOf(value.appearance.theme, ['light', 'dark'], 'appearance.theme'),
        hue: value.appearance.hue === null ? null : number(value.appearance.hue, 0, 359, 'appearance.hue', true) },
      page_titles: { prepend_playing_call: bool(value.page_titles.prepend_playing_call,
        'page_titles.prepend_playing_call') },
      playback: {
        volume: number(value.playback.volume, 0, 1, 'playback.volume'),
        selected_scan_list_ids: scanListIds.slice(),
        target_grouping: bool(value.playback.target_grouping, 'playback.target_grouping'),
        target_burst_limit: number(value.playback.target_burst_limit, 1, 20,
          'playback.target_burst_limit', true)
      },
      scanner: { detail_mode: oneOf(value.scanner.detail_mode,
        ['simple', 'normal', 'advanced', 'engineer'], 'scanner.detail_mode') },
      presentation: {
        source_name_display: oneOf(value.presentation.source_name_display,
          ['talker_alias', 'source_alias', 'both'], 'presentation.source_name_display'),
        show_encryption_details: bool(value.presentation.show_encryption_details,
          'presentation.show_encryption_details'),
        show_control_decode_quality: bool(value.presentation.show_control_decode_quality,
          'presentation.show_control_decode_quality'),
        show_voice_decode_quality: bool(value.presentation.show_voice_decode_quality,
          'presentation.show_voice_decode_quality'),
        decode_quality_display_mode: oneOf(value.presentation.decode_quality_display_mode,
          ['percentage', 'detailed'], 'presentation.decode_quality_display_mode'),
        live_detail_row_limit: number(value.presentation.live_detail_row_limit, 25, 500,
          'presentation.live_detail_row_limit', true),
        show_only_active_trunked_channels: bool(value.presentation.show_only_active_trunked_channels,
          'presentation.show_only_active_trunked_channels'),
        live_channel_sort: oneOf(value.presentation.live_channel_sort,
          ['lcn', 'order_appeared', 'frequency'], 'presentation.live_channel_sort'),
        live_row_density: oneOf(value.presentation.live_row_density,
          ['normal', 'dense'], 'presentation.live_row_density'),
        retain_last_call_on_idle_rows: bool(value.presentation.retain_last_call_on_idle_rows,
          'presentation.retain_last_call_on_idle_rows'),
        clear_voice_quality_when_idle: bool(value.presentation.clear_voice_quality_when_idle,
          'presentation.clear_voice_quality_when_idle')
      },
      tuner: {
        floor_db: floor,
        ceiling_db: ceiling,
        waterfall_speed: number(value.tuner.waterfall_speed, 0.25, 4, 'tuner.waterfall_speed'),
        snap_frequency: bool(value.tuner.snap_frequency, 'tuner.snap_frequency'),
        smooth_fft: bool(value.tuner.smooth_fft, 'tuner.smooth_fft'),
        highlight_waterfall_channels: bool(value.tuner.highlight_waterfall_channels,
          'tuner.highlight_waterfall_channels'),
        show_idle_channels: bool(value.tuner.show_idle_channels, 'tuner.show_idle_channels'),
        profile: oneOf(value.tuner.profile,
          ['efficient', 'balanced', 'high-detail', 'maximum-detail'], 'tuner.profile')
      },
      health_alerts: {
        disabled_codes: alertCodes(value.health_alerts.disabled_codes, 'health_alerts.disabled_codes')
      },
      tables: decodedTables
    };
  }

export { defaults, validate };
