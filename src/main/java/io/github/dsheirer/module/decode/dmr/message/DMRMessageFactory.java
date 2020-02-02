package io.github.dsheirer.module.decode.dmr.message;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.dmr.DMRSyncPattern;
import io.github.dsheirer.module.decode.dmr.message.data.DMRDataMessageFactory;
import io.github.dsheirer.module.decode.dmr.message.voice.VoiceAMessage;
import io.github.dsheirer.module.decode.dmr.message.voice.VoiceEMBMessage;
import io.github.dsheirer.module.decode.dmr.message.voice.VoiceMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DMRMessageFactory
{
    private final static Logger mLog = LoggerFactory.getLogger(DMRMessageFactory.class);

    /**
     * Creates a DMR message wrapper for the binary message.  DMR message type is specified by the sync pattern.
     * @param syncPattern for the message
     * @param binaryMessage containing the raw DMR message burst
     * @param timestamp for the burst
     * @param timeslot for the burst
     * @return DMRMessage instance or null for UNKNOWN sync pattern
     */
    public static DMRBurst create(DMRSyncPattern syncPattern, CorrectedBinaryMessage binaryMessage, CACH cach,
                                  long timestamp, int timeslot)
    {
        switch(syncPattern)
        {
            case BASE_STATION_VOICE:
            case MOBILE_STATION_VOICE:
            case BS_VOICE_FRAME_B:
            case BS_VOICE_FRAME_C:
            case BS_VOICE_FRAME_D:
            case BS_VOICE_FRAME_E:
            case BS_VOICE_FRAME_F:
                return createVoiceMessage(syncPattern, binaryMessage, cach, timestamp, timeslot);
            case DIRECT_MODE_VOICE_TIMESLOT_1:
                return createVoiceMessage(syncPattern, binaryMessage, cach, timestamp, 1);
            case DIRECT_MODE_VOICE_TIMESLOT_2:
                return createVoiceMessage(syncPattern, binaryMessage, cach, timestamp, 2);
            case BASE_STATION_DATA:
            case MOBILE_STATION_DATA:
                return DMRDataMessageFactory.create(syncPattern, binaryMessage, cach, timestamp, timeslot);
            case DIRECT_MODE_DATA_TIMESLOT_1:
                return DMRDataMessageFactory.create(syncPattern, binaryMessage, cach, timestamp, 1);
            case DIRECT_MODE_DATA_TIMESLOT_2:
                return DMRDataMessageFactory.create(syncPattern, binaryMessage, cach, timestamp, 2);
            case UNKNOWN:
                return null;
            case MOBILE_STATION_REVERSE_CHANNEL:
            case RESERVED:
            default:
                return new UnknownDMRMessage(syncPattern, binaryMessage, cach, timestamp, timeslot);
        }
    }

    /**
     * Creates a voice message from the raw transmitted DMR burst frame
     * @param syncPattern for the frame
     * @param message containing the DMR burst
     * @param cach extracted from the first 24 bits of the message
     * @param timestamp the message was received
     * @param timeslot for the DMR burst
     * @return constructed voice message
     */
    private static VoiceMessage createVoiceMessage(DMRSyncPattern syncPattern, CorrectedBinaryMessage message, CACH cach,
                                                  long timestamp, int timeslot)
    {
        switch(syncPattern)
        {
            case BASE_STATION_VOICE:
            case MOBILE_STATION_VOICE:
            case DIRECT_MODE_VOICE_TIMESLOT_1:
            case DIRECT_MODE_VOICE_TIMESLOT_2:
                return new VoiceAMessage(syncPattern, message, cach, timestamp, timeslot);
            case BS_VOICE_FRAME_B:
            case BS_VOICE_FRAME_C:
            case BS_VOICE_FRAME_D:
            case BS_VOICE_FRAME_E:
            case BS_VOICE_FRAME_F:
            default:
                return new VoiceEMBMessage(syncPattern, message, cach, timestamp, timeslot);
        }
    }
}

