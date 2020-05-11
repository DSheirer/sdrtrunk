/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2020 Dennis Sheirer, Zhenyu Mao
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */
package io.github.dsheirer.module.decode.dmr.message.voice;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.audio.codec.mbe.AmbeAudioModule;
import io.github.dsheirer.audio.codec.mbe.ImbeAudioModule;
import io.github.dsheirer.audio.squelch.SquelchState;
import io.github.dsheirer.audio.squelch.SquelchStateEvent;
import io.github.dsheirer.dsp.gain.NonClippingGain;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.dmr.DMRSyncPattern;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.rrapi.type.Voice;
import io.github.dsheirer.sample.Listener;

import java.util.ArrayList;
import java.util.List;

/**
 * DMR Audio Module for converting transmitted AMBE audio frames to 8 kHz PCM audio
 */
public class DMRAudioModule extends AmbeAudioModule
{

    private SquelchStateListener mSquelchStateListener = new SquelchStateListener();
    private NonClippingGain mGain = new NonClippingGain(5.0f, 0.95f);
    private VoiceMessage[] cachedMessage = new VoiceMessage[6];
    private List<byte[]> frames = new ArrayList<byte[]>();
    private int mTimeslot;

    public DMRAudioModule(UserPreferences userPreferences, AliasList aliasList, int timeslot)
    {
        super(userPreferences, aliasList);
        mTimeslot = timeslot;
    }

    @Override
    public Listener<SquelchStateEvent> getSquelchStateListener()
    {
        return mSquelchStateListener;
    }

    /**
     * Timeslot for this audio module
     */
    @Override
    protected int getTimeslot() {
        return mTimeslot;
    }
    @Override
    public void reset()
    {
        getIdentifierCollection().clear();
    }

    @Override
    public void start()
    {

    }


    /**
     * Processes DMR AMBE audio frame
     */
    public void receive(IMessage message)
    {
        if(hasAudioCodec() && message.getTimeslot() == getTimeslot()) //
        {
            if(message instanceof VoiceAMessage) {
                cachedMessage[0] = (VoiceMessage) message;
            } else if(message instanceof VoiceEMBMessage) {
                VoiceEMBMessage vm = (VoiceEMBMessage)message;
                cachedMessage[(int)-vm.getSyncPattern().getPattern()-1] = vm;

                if(vm.getSyncPattern() == DMRSyncPattern.VOICE_FRAME_F) {
                    frames.clear();
                    for(int i = 0; i < 6; i++) {
                        cachedMessage[i].getAMBEFrames(frames);
                    }
                    processAudio();
                    frames.clear();
                }
            }
        }
    }

    /**
     * Processes an audio packet by decoding the AMBE audio frames and rebroadcasting them as PCM audio packets.
     */
    private void processAudio()
    {
        for(byte[] frame : frames)
        {
            float[] audio = getAudioCodec().getAudio(frame);
            audio = mGain.apply(audio);
            addAudio(audio);
        }
    }

    /**
     * Wrapper for squelch state to process end of call actions.  At call end the encrypted call state established
     * flag is reset so that the encrypted audio state for the next call can be properly detected and we send an
     * END audio packet so that downstream processors like the audio recorder can properly close out a call sequence.
     */
    public class SquelchStateListener implements Listener<SquelchStateEvent>
    {
        @Override
        public void receive(SquelchStateEvent event)
        {
            if(event.getSquelchState() == SquelchState.SQUELCH)
            {
                closeAudioSegment();
            }
        }
    }
}
