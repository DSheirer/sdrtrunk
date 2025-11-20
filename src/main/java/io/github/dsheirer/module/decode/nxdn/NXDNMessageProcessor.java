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
package io.github.dsheirer.module.decode.nxdn;

import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.nxdn.channel.ChannelFrequency;
import io.github.dsheirer.module.decode.nxdn.layer2.SACCHAssembler;
import io.github.dsheirer.module.decode.nxdn.layer2.SACCHFragment;
import io.github.dsheirer.module.decode.nxdn.layer3.broadcast.IChannelInformationReceiver;
import io.github.dsheirer.module.decode.nxdn.layer3.broadcast.Idle;
import io.github.dsheirer.module.decode.nxdn.layer3.broadcast.SiteInformation;
import io.github.dsheirer.module.decode.nxdn.layer3.call.Disconnect;
import io.github.dsheirer.module.decode.nxdn.layer3.proprietary.TalkerAlias;
import io.github.dsheirer.module.decode.nxdn.layer3.proprietary.TalkerAliasAssembler;
import io.github.dsheirer.module.decode.nxdn.layer3.type.ChannelAccessInformation;
import io.github.dsheirer.sample.Listener;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * NXDN Message Processor.
 *
 * Performs post-message creation processing and enrichment before the message is sent downstream.
 */
public class NXDNMessageProcessor implements Listener<IMessage>
{
    private final static Logger LOGGER = LoggerFactory.getLogger(NXDNMessageProcessor.class);
    private ChannelAccessInformation mChannelAccessInformation;
    private final Map<Integer, ChannelFrequency> mChannelFrequencyMap = new HashMap<>();
    private final SACCHAssembler mSACCHAssembler = new SACCHAssembler();
    private final TalkerAliasAssembler mTalkerAliasAssembler;

    /**
     * Downstream message listener
     */
    private Listener<IMessage> mMessageListener;

    /**
     * Constructs an instance
     * @param configNXDN that optionally contains a channel map for channel mode systems
     */
    public NXDNMessageProcessor(DecodeConfigNXDN configNXDN)
    {
        for(ChannelFrequency channelFrequency: configNXDN.getChannelMap())
        {
            mChannelFrequencyMap.put(channelFrequency.getChannel(), channelFrequency);
        }

        mTalkerAliasAssembler = new TalkerAliasAssembler(configNXDN.getEncoding());
    }

    /**
     * Processes the message for enrichment or reassembly of fragments and sends the enriched message and any additional
     * messages that were created during the enrichment to the registered message listener.
     * @param message to process
     */
    @Override
    public void receive(IMessage message)
    {
        if(message != null && message.isValid())
        {
            //Capture channel access information from Site Information message
            if(message instanceof SiteInformation siteInformation)
            {
                mChannelAccessInformation = siteInformation.getChannelAccessInformation();
            }

            //Enrich messages that carry channel information with the channel access info.
            if(mChannelAccessInformation != null && message instanceof IChannelInformationReceiver receiver)
            {
                receiver.receive(mChannelAccessInformation, mChannelFrequencyMap);
            }

            //SACCH fragments are either standalone 4/4, or a sequence of 4 in a superframe. Reassemble them.
            if(message instanceof SACCHFragment fragment)
            {
                dispatch(message);
                message = null;
                //Publish the completed sacch message to this method so that it can be processed correctly and dispatch
                //the original fragment and nullify the message to ensure correct sequencing where fragment 4 is
                //dispatched first, and the assembled SACCH message.
                receive(mSACCHAssembler.process(fragment));
            }

            if(message instanceof TalkerAlias fragment)
            {
                dispatch(message);
                //Assign the completed alias to the message to dispatch below, to ensure proper message output sequence
                message = mTalkerAliasAssembler.process(fragment);
            }
            else if(message instanceof Idle || message instanceof Disconnect)
            {
                //Clear any captured talker alias fragments at the end of the call or idle mode
                mTalkerAliasAssembler.reset();
            }
        }

        dispatch(message);
    }

    /**
     * Post-process the message for frequency band details.
     * @param message to post process and dispatch
     */
    private void dispatch(IMessage message)
    {
        if(message != null && mMessageListener != null)
        {
            mMessageListener.receive(message);
        }
    }

    /**
     * Prepares for disposal of this instance.
     */
    public void dispose()
    {
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
