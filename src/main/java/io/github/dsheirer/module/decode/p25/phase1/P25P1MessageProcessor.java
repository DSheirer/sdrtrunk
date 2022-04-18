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
package io.github.dsheirer.module.decode.p25.phase1;

import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.Message;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBand;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBandReceiver;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class P25P1MessageProcessor implements Listener<Message>
{
    private final static Logger mLog = LoggerFactory.getLogger(P25P1MessageProcessor.class);

    private Listener<IMessage> mMessageListener;

    /* Map of up to 16 band identifiers per RFSS.  These identifier update
     * messages are inserted into any message that conveys channel information
     * so that the uplink/downlink frequencies can be calculated */
    private Map<Integer,IFrequencyBand> mFrequencyBandMap = new TreeMap<Integer,IFrequencyBand>();

    public P25P1MessageProcessor()
    {
    }

    @Override
    public void receive(Message message)
    {
        /**
         * Capture frequency band identifier messages and inject them into any
         * messages that require band information in order to calculate the
         * up-link and down-link frequencies for any numeric channel references
         * contained within the message.
         */
        if(message.isValid())
        {
            /* Insert frequency band identifier update messages into channel-type messages */
            if(message instanceof IFrequencyBandReceiver)
            {
                IFrequencyBandReceiver receiver = (IFrequencyBandReceiver)message;

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
            if(message instanceof IFrequencyBand)
            {
                IFrequencyBand bandIdentifier = (IFrequencyBand)message;
                mFrequencyBandMap.put(bandIdentifier.getIdentifier(), bandIdentifier);
            }
        }

        if(mMessageListener != null)
        {
            mMessageListener.receive(message);
        }
    }

    public void dispose()
    {
        mFrequencyBandMap.clear();
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
