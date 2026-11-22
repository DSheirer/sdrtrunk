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

package io.github.dsheirer.module.decode.edacs.provoice;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.audio.AudioSegment;
import io.github.dsheirer.preference.UserPreferences;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import jmbe.iface.IAudioCodec;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests: a ProVoice transmission whose voice frames hold no sound is flagged as silent, so that it is not
 * recorded or streamed; a transmission with speech, however short, is not.
 *
 * The codec is replaced by one that returns a frame at the level given in the first byte of the frame (thousandths of
 * full scale): Cambridge 2026-11-13 measured 0.0012 for a silent transmission (the codec's comfort noise) and 0.31
 * for the quietest transmission with speech.
 */
public class ProVoiceSilentSegmentTest
{
    private final List<AudioSegment> mSegments = new ArrayList<>();

    private ProVoiceAudioModule module()
    {
        IAudioCodec codec = (IAudioCodec)Proxy.newProxyInstance(getClass().getClassLoader(),
            new Class[]{IAudioCodec.class}, (proxy, method, args) -> {
                if(method.getName().equals("getAudio"))
                {
                    float[] audio = new float[160];
                    Arrays.fill(audio, ((byte[])args[0])[0] / 1000.0f);
                    return audio;
                }

                return null;
            });

        ProVoiceAudioModule module = new ProVoiceAudioModule(new UserPreferences(), new AliasList("test"))
        {
            @Override
            public IAudioCodec getAudioCodec()
            {
                return codec;
            }

            @Override
            protected boolean hasLibraryGainControl()
            {
                return true;
            }
        };

        module.setAudioSegmentListener(mSegments::add);
        return module;
    }

    private static ProVoiceMessage frames(int count, int level)
    {
        List<byte[]> frames = new ArrayList<>();

        for(int x = 0; x < count; x++)
        {
            frames.add(new byte[]{(byte)level, 0, 0});
        }

        return new ProVoiceMessage(frames, 0, 0, 0);
    }

    @Test
    void testSilentTransmissionIsFlagged()
    {
        ProVoiceAudioModule module = module();

        for(int x = 0; x < 24; x++)
        {
            module.receive(frames(4, 1));       //0.001 of full scale: comfort noise
        }

        module.stop(); //closes the segment in progress
        assertEquals(1, mSegments.size());
        assertTrue(mSegments.get(0).isSilent());
        assertEquals(1, module.getSilentSegmentCount());
    }

    @Test
    void testSpeechIsNotFlagged()
    {
        ProVoiceAudioModule module = module();
        module.receive(frames(4, 1));
        module.receive(frames(1, 100));         //one frame at 0.1 of full scale among silence
        module.receive(frames(3, 1));
        module.stop(); //closes the segment in progress
        assertEquals(1, mSegments.size());
        assertFalse(mSegments.get(0).isSilent());

        //The next transmission starts from scratch
        module.receive(frames(8, 1));
        module.stop(); //closes the segment in progress
        assertEquals(2, mSegments.size());
        assertTrue(mSegments.get(1).isSilent());
        assertFalse(mSegments.get(0).isSilent());
    }

    @Test
    void testDamagedFramesAloneAreSilence()
    {
        ProVoiceAudioModule module = module();
        List<byte[]> concealed = new ArrayList<>();
        concealed.add(null);
        concealed.add(null);
        module.receive(new ProVoiceMessage(concealed, 0, 2, 0));
        module.stop(); //closes the segment in progress
        assertEquals(1, mSegments.size());
        assertTrue(mSegments.get(0).isSilent());
    }
}
