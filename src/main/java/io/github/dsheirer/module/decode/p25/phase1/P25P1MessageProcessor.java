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
import io.github.dsheirer.module.decode.p25.phase1.message.lc.l3harris.HarrisTalkerAliasAssembler;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.motorola.LCMotorolaTalkerAliasAssembler;
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
 * Performs post-message creation processing and enrichment before the message is sent downstream.
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

    /**
     * Motorola talker alias assembler for link control header and data blocks.
     */
    private LCMotorolaTalkerAliasAssembler mMotorolaTalkerAliasAssembler = new LCMotorolaTalkerAliasAssembler();

    /**
     * Harris talker alias assembler for link control talker alias blocks
     */
    private HarrisTalkerAliasAssembler mHarrisTalkerAliasAssembler = new HarrisTalkerAliasAssembler();

    /**
     * Constructs an instance
     */
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

    /**
     * Processes the message for enrichment or reassembly of fragments and sends the enriched message and any additional
     * messages that were created during the enrichment to the registered message listener.
     * @param message to process
     */
    @Override
    public void receive(IMessage message)
    {
        //Optional message created during processing that should be sent after the current argument is sent.
        IMessage additionalMessageToSend = null;

        if(message.isValid())
        {
            //Reassemble extended source link control messages.
            if(message instanceof LDU1Message ldu)
            {
                reassembleLC(ldu.getLinkControlWord());

                //Send the LCW to the harris talker alias assembler
                additionalMessageToSend = mHarrisTalkerAliasAssembler.process(ldu.getLinkControlWord(), ldu.getTimestamp());
            }
            else if(message instanceof TDULinkControlMessage tdu)
            {
                LinkControlWord lcw = tdu.getLinkControlWord();
                reassembleLC(lcw);

                //Motorola carries the talker alias in the TDULC
                if(mMotorolaTalkerAliasAssembler.add(lcw, message.getTimestamp()))
                {
                    additionalMessageToSend = mMotorolaTalkerAliasAssembler.assemble();
                }

                //Harris carries the talker alias in the LDU voice messages, so reset the assembler on TDULC.
                mHarrisTalkerAliasAssembler.reset();
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

                //Only store the frequency band if it's new so we don't hold on to more than one instance of the
                //frequency band message.  Otherwise, we'll hold on to several instances of each message as they get
                //injected into other messages with channel information.
                if(!mFrequencyBandMap.containsKey(bandIdentifier.getIdentifier()))
                {
                    mFrequencyBandMap.put(bandIdentifier.getIdentifier(), bandIdentifier);
                }
            }
        }

        if(mMessageListener != null)
        {
            mMessageListener.receive(message);

            if(additionalMessageToSend != null)
            {
                mMessageListener.receive(additionalMessageToSend);
            }
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

    /**
     * Prepares for disposal of this instance.
     */
    public void dispose()
    {
        mFrequencyBandMap.clear();
        mMessageListener = null;
    }

    /**
     * Sets the message listener to receive the output from this processor.
     * @param listener to receive output messages
     */
    public void setMessageListener(Listener<IMessage> listener)
    {
        mMessageListener = listener;
    }

    /**
     * Clears a registered message listener.
     */
    public void removeMessageListener()
    {
        mMessageListener = null;
    }
}
