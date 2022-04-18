/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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
package io.github.dsheirer.module.decode.p25.phase2;

import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.SyncLossMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBand;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.phase2.message.EncryptionSynchronizationSequence;
import io.github.dsheirer.module.decode.p25.phase2.message.EncryptionSynchronizationSequenceProcessor;
import io.github.dsheirer.module.decode.p25.phase2.message.SuperFrameFragment;
import io.github.dsheirer.module.decode.p25.phase2.message.mac.MacMessage;
import io.github.dsheirer.module.decode.p25.phase2.timeslot.AbstractSignalingTimeslot;
import io.github.dsheirer.module.decode.p25.phase2.timeslot.AbstractVoiceTimeslot;
import io.github.dsheirer.module.decode.p25.phase2.timeslot.Timeslot;
import io.github.dsheirer.module.decode.p25.phase2.timeslot.Voice2Timeslot;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class P25P2MessageProcessor implements Listener<IMessage>
{
    private final static Logger mLog = LoggerFactory.getLogger(P25P2MessageProcessor.class);

    private EncryptionSynchronizationSequenceProcessor mESSProcessor0 = new EncryptionSynchronizationSequenceProcessor(0);
    private EncryptionSynchronizationSequenceProcessor mESSProcessor1 = new EncryptionSynchronizationSequenceProcessor(1);
    private Listener<IMessage> mMessageListener;

    //Map of up to 16 band identifiers per RFSS.  These identifier update messages are inserted into any message that
    // conveys channel information so that the uplink/downlink frequencies can be calculated
    private Map<Integer,IFrequencyBand> mFrequencyBandMap = new TreeMap<Integer,IFrequencyBand>();

    public P25P2MessageProcessor()
    {
    }

    @Override
    public void receive(IMessage message)
    {
        if(mMessageListener != null)
        {
            if(message instanceof SuperFrameFragment)
            {
                SuperFrameFragment sff = (SuperFrameFragment)message;

                for(Timeslot timeslot: sff.getTimeslots())
                {
                    if(timeslot instanceof AbstractSignalingTimeslot)
                    {
                        AbstractSignalingTimeslot ast = (AbstractSignalingTimeslot)timeslot;

                        for(MacMessage macMessage: ast.getMacMessages())
                        {
                            /* Insert frequency band identifier update messages into channel-type messages */
                            if(macMessage instanceof IFrequencyBandReceiver)
                            {
                                IFrequencyBandReceiver receiver = (IFrequencyBandReceiver)macMessage;

                                List<IChannelDescriptor> channels = receiver.getChannels();

                                for(IChannelDescriptor channel : channels)
                                {
                                    int[] frequencyBandIdentifiers = channel.getFrequencyBandIdentifiers();

                                    for(int id : frequencyBandIdentifiers)
                                    {
                                        if(mFrequencyBandMap.containsKey(id))
                                        {
                                            channel.setFrequencyBand(mFrequencyBandMap.get(id));
                                        }
                                    }
                                }
                            }

                            /* Store band identifiers so that they can be injected into channel
                             * type messages */
                            if(macMessage instanceof IFrequencyBand)
                            {
                                IFrequencyBand bandIdentifier = (IFrequencyBand)macMessage;
                                mFrequencyBandMap.put(bandIdentifier.getIdentifier(), bandIdentifier);
                            }

                            mMessageListener.receive(macMessage);
                        }
                    }
                    else if(timeslot instanceof AbstractVoiceTimeslot)
                    {
                        if(timeslot.getTimeslot() == 0)
                        {
                            mESSProcessor0.process((AbstractVoiceTimeslot)timeslot);

                            if(timeslot instanceof Voice2Timeslot)
                            {
                                EncryptionSynchronizationSequence ess = mESSProcessor0.getSequence();

                                if(ess != null)
                                {
                                    mMessageListener.receive(ess);
                                }

                                mESSProcessor0.reset();
                            }
                        }
                        else
                        {
                            mESSProcessor1.process((AbstractVoiceTimeslot)timeslot);

                            if(timeslot instanceof Voice2Timeslot)
                            {
                                EncryptionSynchronizationSequence ess = mESSProcessor1.getSequence();

                                if(ess != null)
                                {
                                    mMessageListener.receive(ess);
                                }

                                mESSProcessor1.reset();
                            }
                        }

                        mMessageListener.receive(timeslot);

                    }
                    else
                    {
                        mMessageListener.receive(timeslot);
                    }
                }
            }
            else if(message instanceof SyncLossMessage)
            {
                mMessageListener.receive(message);
            }
        }
    }

    public void dispose()
    {
        mMessageListener = null;
    }

    public void setMessageListener(Listener<IMessage> listener)
    {
        mMessageListener = listener;
    }

    public void removeMessageListener()
    {
        mMessageListener = null;
    }
}
