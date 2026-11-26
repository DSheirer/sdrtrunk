/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */

package io.github.dsheirer.module.decode.dmr.audio;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.audio.AudioSegment;
import io.github.dsheirer.audio.squelch.SquelchState;
import io.github.dsheirer.audio.squelch.SquelchStateEvent;
import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.module.decode.dmr.message.data.lc.full.GroupVoiceChannelUser;
import io.github.dsheirer.module.decode.dmr.message.voice.VoiceAMessage;
import io.github.dsheirer.module.decode.dmr.sync.DMRSyncPattern;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import jmbe.frame.ICorrectedFrame;
import jmbe.iface.IAudioWithMetadata;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for the DMR audio module encrypted call state handling.
 */
public class DMRAudioModuleTest
{
    private static final int TIMESLOT = 1;
    //Base station voice frame A burst where all three voice frames pass error detection (see AMBEFrameValidatorTest)
    private static final String VOICE_BURST = "5A3AC4B9E881526173002A6BB9E881526755FD7DF75F7173002A6BB9E881526173002A6B";
    //Service options encryption bit in a voice channel user full link control (ETSI TS 102 361-2 7.2.1)
    private static final int SERVICE_OPTIONS_ENCRYPTION_BIT = 17;

    private static GroupVoiceChannelUser getVoiceChannelUser(boolean encrypted)
    {
        CorrectedBinaryMessage message = new CorrectedBinaryMessage(72);

        if(encrypted)
        {
            message.set(SERVICE_OPTIONS_ENCRYPTION_BIT);
        }

        return new GroupVoiceChannelUser(message, 0, TIMESLOT);
    }

    private static VoiceAMessage getVoice()
    {
        return new VoiceAMessage(DMRSyncPattern.BASE_STATION_VOICE, CorrectedBinaryMessage.loadHex(VOICE_BURST), null,
            0, TIMESLOT);
    }

    /**
     * An encrypted call that ends without a terminator (ie lost to a fade) followed by a clear call on the same
     * timeslot.  The squelch at call end must reset the encrypted call state so that the clear call's audio is not
     * muted.
     */
    @Test
    void encryptedStateResetAtCallEndWithoutTerminator()
    {
        TestDMRAudioModule module = new TestDMRAudioModule();
        List<AudioSegment> segments = new ArrayList<>();
        module.setAudioSegmentListener(segments::add);

        module.receive(getVoiceChannelUser(true));
        module.receive(getVoice());
        assertEquals(0, module.mFramesProduced, "Encrypted call should not produce audio");

        module.getSquelchStateListener().receive(SquelchStateEvent.create(SquelchState.SQUELCH, TIMESLOT));

        module.receive(getVoiceChannelUser(false));
        module.receive(getVoice());
        assertEquals(3, module.mFramesProduced, "Clear call after squelch should produce audio");
        assertEquals(1, segments.size(), "Clear call should produce an audio segment");
    }

    /**
     * A squelch event for the other timeslot does not reset this timeslot's encrypted call state.
     */
    @Test
    void otherTimeslotSquelchDoesNotResetState()
    {
        TestDMRAudioModule module = new TestDMRAudioModule();

        module.receive(getVoiceChannelUser(true));
        module.getSquelchStateListener().receive(SquelchStateEvent.create(SquelchState.SQUELCH, TIMESLOT + 1));
        module.receive(getVoiceChannelUser(false));
        module.receive(getVoice());
        assertEquals(0, module.mFramesProduced, "Encrypted call should not produce audio");
    }

    /**
     * DMR audio module with a stub audio codec that counts the synthesized voice frames.
     */
    private static class TestDMRAudioModule extends DMRAudioModule
    {
        private int mFramesProduced = 0;

        public TestDMRAudioModule()
        {
            super(null, new AliasList("test"), TIMESLOT);
        }

        @Override
        protected void loadConverter()
        {
            //No JMBE library for unit tests
        }

        @Override
        protected boolean hasAudioCodec()
        {
            return true;
        }

        @Override
        protected IAudioWithMetadata getAudioWithMetadata(byte[] frame, ICorrectedFrame correctedFrame)
        {
            mFramesProduced++;

            return new IAudioWithMetadata()
            {
                @Override
                public float[] getAudio()
                {
                    return new float[160];
                }

                @Override
                public boolean hasMetadata()
                {
                    return false;
                }

                @Override
                public Map<String,String> getMetadata()
                {
                    return Collections.emptyMap();
                }
            };
        }
    }
}
