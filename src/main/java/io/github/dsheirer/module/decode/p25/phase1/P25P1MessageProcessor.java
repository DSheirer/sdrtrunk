/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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
import io.github.dsheirer.module.decode.p25.P25FrequencyBandPreloadDataContent;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBand;
import io.github.dsheirer.module.decode.p25.phase1.message.IFrequencyBandReceiver;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.ExtendedSourceLinkControlWord;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.LinkControlWord;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCSourceIDExtension;
import io.github.dsheirer.module.decode.p25.phase1.message.ldu.LDU1Message;
import io.github.dsheirer.module.decode.p25.phase1.message.tdu.TDULinkControlMessage;
import io.github.dsheirer.sample.Listener;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * APCO25 Phase 1 Message Processor.
 *
 * Performs post-message creation processing before the message is sent downstream.
 */
public class P25P1MessageProcessor implements Listener<IMessage>
{
    private final static Logger mLog = LoggerFactory.getLogger(P25P1MessageProcessor.class);

    /**
     * Downstream message listener
     */
    private Listener<IMessage> mMessageListener;

    /**
     * Map of up to 16 band identifiers per RFSS.  These identifier update messages are inserted into any message that
     * conveys channel information so that the uplink/downlink frequencies can be calculated
     */
    private Map<Integer,IFrequencyBand> mFrequencyBandMap = new TreeMap<Integer,IFrequencyBand>();

    /**
     * Temporary holding of an extended source link control message while it awaits the extension message to arrive.
     */
    private ExtendedSourceLinkControlWord mExtendedSourceLinkControlWord;

    public P25P1MessageProcessor()
    {
    }

    /**
     * Preloads frequency band information from the control channel.
     * @param content to preload
     */
    public void preload(P25FrequencyBandPreloadDataContent content)
    {
        if(content.hasData())
        {
            for(IFrequencyBand frequencyBand: content.getData())
            {
                mFrequencyBandMap.put(frequencyBand.getIdentifier(), frequencyBand);
            }
        }
    }

    @Override
    public void receive(IMessage message)
    {
        if(message.isValid())
        {
            //Reassemble extended source link control messages.
            if(message instanceof LDU1Message ldu)
            {
                reassembleLC(ldu.getLinkControlWord());
            }
            else if(message instanceof TDULinkControlMessage tdu)
            {
                reassembleLC(tdu.getLinkControlWord());
            }

            //Insert frequency band identifier update messages into channel-type messages */
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

            //Store band identifiers so that they can be injected into channel type messages
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

    /**
     * Processes link control words to reassemble source ID extension messages.
     * @param linkControlWord to process
     */
    private void reassembleLC(LinkControlWord linkControlWord)
    {
        if(linkControlWord instanceof ExtendedSourceLinkControlWord eslcw)
        {
            mExtendedSourceLinkControlWord = eslcw;
        }
        else if(linkControlWord instanceof LCSourceIDExtension sie && mExtendedSourceLinkControlWord != null)
        {
            mExtendedSourceLinkControlWord.setSourceIDExtension(sie);
            mExtendedSourceLinkControlWord = null;
        }
        else
        {
            //The source extension message should always immediately follow the message that is being extended, so if
            //we get a message that is not an extended message or the extension, then we've missed the extension and we
            //should nullify any extended message that's waiting for the extension.
            mExtendedSourceLinkControlWord = null;
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
