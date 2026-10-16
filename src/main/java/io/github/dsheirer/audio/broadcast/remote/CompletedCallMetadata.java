package io.github.dsheirer.audio.broadcast.remote;

import io.github.dsheirer.audio.broadcast.AudioRecording;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.configuration.ConfigurationLongIdentifier;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Stable, transport-neutral metadata snapshot for a completed call. */
public class CompletedCallMetadata
{
    private final String callId;
    private final long timestamp;
    private final String timestampIso;
    private final long durationMs;
    private final long frequencyHz;
    private final List<String> identifiers;
    private final Map<String, List<String>> identifiersByRole;
    private CallTranscription transcription = CallTranscription.none();

    private CompletedCallMetadata(AudioRecording recording)
    {
        timestamp = recording.getStartTime();
        durationMs = recording.getRecordingLength();
        timestampIso = Instant.ofEpochMilli(timestamp).toString();
        callId = UUID.nameUUIDFromBytes((timestamp + ":" + recording.getPath().getFileName()).getBytes()).toString();
        Identifier frequency = recording.getIdentifierCollection().getIdentifier(IdentifierClass.CONFIGURATION,
            Form.CHANNEL_FREQUENCY, Role.ANY);
        frequencyHz = frequency instanceof ConfigurationLongIdentifier configuration ? configuration.getValue() : 0;
        identifiers = recording.getIdentifierCollection().getIdentifiers().stream().map(Object::toString).toList();
        identifiersByRole = new LinkedHashMap<>();
        for(Role role: Role.values())
        {
            List<String> values = recording.getIdentifierCollection().getIdentifiers(role).stream()
                .map(Object::toString).toList();
            if(!values.isEmpty())
            {
                identifiersByRole.put(role.name(), values);
            }
        }
    }

    public static CompletedCallMetadata from(AudioRecording recording) { return new CompletedCallMetadata(recording); }
    public String getCallId() { return callId; }
    public long getTimestamp() { return timestamp; }
    public String getTimestampIso() { return timestampIso; }
    public long getDurationMs() { return durationMs; }
    public long getFrequencyHz() { return frequencyHz; }
    public List<String> getIdentifiers() { return identifiers; }
    public Map<String, List<String>> getIdentifiersByRole() { return identifiersByRole; }
    public CallTranscription getTranscription() { return transcription; }
    public void setTranscription(CallTranscription value) { transcription = value != null ? value : CallTranscription.none(); }
}
