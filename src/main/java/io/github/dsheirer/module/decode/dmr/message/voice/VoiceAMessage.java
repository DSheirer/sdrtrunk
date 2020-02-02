package io.github.dsheirer.module.decode.dmr.message.voice;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.dmr.DMRSyncPattern;
import io.github.dsheirer.module.decode.dmr.message.CACH;

import java.util.Collections;
import java.util.List;

/**
 * DMR Voice Frame A with embedded sync pattern.
 */
public class VoiceAMessage extends VoiceMessage
{
    /**
     * Constructs an instance.
     *
     * @param syncPattern for the Voice A frame
     * @param message containing 288-bit DMR message with preliminary bit corrections indicated.
     */
    public VoiceAMessage(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach, long timestamp,
                         int timeslot)
    {
        super(syncPattern, message, cach, timestamp, timeslot);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("     ").append(getSyncPattern());
        return sb.toString();
    }

    @Override
    public List<Identifier> getIdentifiers()
    {
        return Collections.emptyList();
    }
}
