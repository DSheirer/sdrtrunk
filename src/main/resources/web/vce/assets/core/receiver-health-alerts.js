'use strict';

  function alert(id, name, description) {
    return Object.freeze({ id, name, description });
  }

  function group(id, name, description, alerts) {
    return Object.freeze({ id, name, description, alerts: Object.freeze(alerts) });
  }

  const receiverHealthAlertGroups = Object.freeze([
    group('receiver', 'Tuners and radio data', '', [
      alert('tuner-error', 'Tuner stopped working',
        'Assigned channels cannot receive data from this tuner.'),
      alert('receiver-iq-drop', 'Radio data was lost before decoding',
        'Calls may have gaps or be missed.'),
      alert('receiver-ingress-drop', 'Radio data was lost entering the receiver',
        'Radio data was lost after leaving the USB tuner.'),
      alert('receiver-listener-failure', 'Part of the receiver missed radio data',
        'Radio data could not reach a receiver function or open diagnostic view.'),
      alert('receiver-queue-pressure', 'Receiver is falling behind',
        'Incoming radio data is building up and may be lost.'),
      alert('tuner-allocation-failure', 'No tuner was available for a channel',
        'No enabled tuner could receive the requested frequency.')
    ]),
    group('usb', 'USB connection', '', [
      alert('usb-sample-loss', 'USB tuner data is incomplete',
        'Missing or unusable USB data can interrupt decoding even when signal strength looks normal.'),
      alert('usb-delivery-rate-low', 'USB tuner data is arriving too slowly',
        'Repeated slowdowns can interrupt decoding.'),
      alert('usb-transfer-gap', 'USB tuner data paused',
        'A USB data pause can interrupt decoding or clip call audio.'),
      alert('usb-transfer-pool-degraded', 'USB tuner has reduced transfer capacity',
        'Fewer active USB transfers make data gaps more likely.')
    ]),
    group('channels', 'Channel decoding', '', [
      alert('channelizer-drop', 'Channels lost radio data',
        'Data was lost while separating one tuner signal into channels.'),
      alert('channelizer-queue-pressure', 'Channel separation is falling behind',
        'Radio data may be lost if channel separation falls further behind.'),
      alert('channel-queue-pressure', 'One channel is falling behind',
        'Its decoding or audio may be interrupted.'),
      alert('channel-output-drop', 'One or more channels lost radio data',
        'Missing data can interrupt decoding or audio.'),
      alert('control-channel-lock-lost', 'Control channel stopped decoding',
        'The receiver may miss new calls.')
    ]),
    group('host', 'Computer resources', '', [
      alert('host-cpu-pressure', 'Computer is overloaded',
        'Radio processing may fall behind.'),
      alert('heap-pressure', 'VCE is low on memory',
        'Receiving may be interrupted.'),
      alert('gc-pause', 'VCE spent extra time freeing memory',
        'Receiving may fall behind.'),
      alert('disk-space', 'Storage space is low',
        'Recordings and activity history may not be saved.')
    ]),
    group('outputs', 'Recordings and listening', '', [
      alert('audio-coordinator-ingress', 'A call’s outputs were incomplete',
        'Its recording, stream, or browser audio may be incomplete.'),
      alert('audio-coordinator-aborted', 'A call’s outputs were interrupted',
        'Its recording, stream, or browser audio may be missing.'),
      alert('audio-output-pressure', 'Call outputs are falling behind',
        'Recordings, streams, or browser audio may be missed.'),
      alert('recording', 'A call recording was not saved',
        'The completed call could not be written to storage.'),
      alert('recording-output-pressure', 'Saving recordings is falling behind',
        'Some recordings may not be saved.'),
      alert('streaming', 'A call was not sent to the streaming service',
        'The completed call could not be prepared or delivered.'),
      alert('streaming-output-pressure', 'Streaming is falling behind',
        'Some calls may not be streamed.'),
      alert('web-audio-drop', 'Browser audio was not available for a call',
        'Browser listeners may miss the call.')
    ])
  ]);

  const receiverHealthAlertIds = Object.freeze(receiverHealthAlertGroups.flatMap(({ alerts }) =>
    alerts.map(({ id }) => id)));

  function isReceiverHealthAlertEnabled(preferences, incidentCode) {
    const disabledCodes = preferences?.health_alerts?.disabled_codes;
    return !Array.isArray(disabledCodes) || typeof incidentCode !== 'string' ||
      !disabledCodes.includes(incidentCode);
  }

export { receiverHealthAlertGroups, receiverHealthAlertIds, isReceiverHealthAlertEnabled };
