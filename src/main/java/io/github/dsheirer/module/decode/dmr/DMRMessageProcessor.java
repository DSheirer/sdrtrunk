/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
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
package io.github.dsheirer.module.decode.dmr;

import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.dmr.message.CACH;
import io.github.dsheirer.module.decode.dmr.message.DMRBurst;
import io.github.dsheirer.module.decode.dmr.message.data.lc.full.FLCAssembler;
import io.github.dsheirer.module.decode.dmr.message.data.lc.full.FullLCMessage;
import io.github.dsheirer.module.decode.dmr.message.data.lc.shorty.SLCAssembler;
import io.github.dsheirer.module.decode.dmr.message.data.lc.shorty.ShortLCMessage;
import io.github.dsheirer.module.decode.dmr.message.voice.VoiceEMBMessage;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processes DMR messages and performs re-assembly of link control fragments
 */
public class DMRMessageProcessor implements Listener<IMessage>
{
    private final static Logger mLog = LoggerFactory.getLogger(DMRMessageProcessor.class);

    private SLCAssembler mSLCAssembler = new SLCAssembler();
    private FLCAssembler mFLCAssemblerTimeslot0 = new FLCAssembler(0);
    private FLCAssembler mFLCAssemblerTimeslot1 = new FLCAssembler(1);
    private Listener<IMessage> mMessageListener;

    /**
     * Constructs an instance
     */
    public DMRMessageProcessor()
    {
    }

    /**
     * Primary message processing
     */
    @Override
    public void receive(IMessage message)
    {
        dispatch(message);

        //Extract the Full Link Control message fragment from the Voice with embedded signalling message
        if(message instanceof VoiceEMBMessage)
        {
            VoiceEMBMessage voice = (VoiceEMBMessage)message;

            if(message.getTimeslot() == 0)
            {
                FullLCMessage flco = mFLCAssemblerTimeslot0.process(voice.getEMB().getLCSS(),
                    voice.getFLCFragment(), message.getTimestamp());
                dispatch(flco);
            }
            else
            {
                FullLCMessage flco = mFLCAssemblerTimeslot1.process(voice.getEMB().getLCSS(),
                    voice.getFLCFragment(), message.getTimestamp());
                dispatch(flco);
            }
        }
        //Extract the Short Link Control message fragment from the DMR burst message when it has one
        else if(message instanceof DMRBurst)
        {
            DMRBurst dmrBurst = (DMRBurst)message;

            if(dmrBurst.hasCACH())
            {
                CACH cach = dmrBurst.getCACH();
                ShortLCMessage slco = mSLCAssembler.process(cach.getLCSS(), cach.getPayload(), message.getTimestamp());
                dispatch(slco);
            }
        }

        //TODO: perform packet sequence re-assembly here
    }

    /**
     * Dispatches the non-null message to the registered listener
     */
    private void dispatch(IMessage message)
    {
        if(mMessageListener != null && message != null)
        {
            mMessageListener.receive(message);
        }
    }

    /**
     * Prepares for disposal
     */
    public void dispose()
    {
        mMessageListener = null;
    }

    /**
     * Registers the listener to receive messages from this processor
     */
    public void setMessageListener(Listener<IMessage> listener)
    {
        mMessageListener = listener;
    }

    /**
     * Removes the listener from receiving messages from this processor
     */
    public void removeMessageListener()
    {
        mMessageListener = null;
    }
}
