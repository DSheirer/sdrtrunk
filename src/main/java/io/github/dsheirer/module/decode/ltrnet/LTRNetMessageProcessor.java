/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014,2015 Dennis Sheirer
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package io.github.dsheirer.module.decode.ltrnet;

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.MessageDirection;
import io.github.dsheirer.module.decode.ltrnet.message.LtrNetMessage;
import io.github.dsheirer.module.decode.ltrnet.message.LtrNetMessageFactory;
import io.github.dsheirer.module.decode.ltrnet.message.isw.RegistrationRequestEsnHigh;
import io.github.dsheirer.module.decode.ltrnet.message.isw.RegistrationRequestEsnLow;
import io.github.dsheirer.module.decode.ltrnet.message.osw.ReceiveFrequencyHigh;
import io.github.dsheirer.module.decode.ltrnet.message.osw.ReceiveFrequencyLow;
import io.github.dsheirer.module.decode.ltrnet.message.osw.TransmitFrequencyHigh;
import io.github.dsheirer.module.decode.ltrnet.message.osw.TransmitFrequencyLow;
import io.github.dsheirer.sample.Listener;

import java.util.HashMap;
import java.util.Map;

public class LTRNetMessageProcessor implements Listener<CorrectedBinaryMessage>
{
    private Listener<IMessage> mMessageListener;

    private Map<Integer,ReceiveFrequencyHigh> mReceiveHighMessageMap = new HashMap<>();
    private Map<Integer,ReceiveFrequencyLow> mReceiveLowMessageMap = new HashMap<>();
    private Map<Integer,TransmitFrequencyHigh> mTransmitHighMessageMap = new HashMap<>();
    private Map<Integer,TransmitFrequencyLow> mTransmitLowMessageMap = new HashMap<>();

    private RegistrationRequestEsnHigh mRegistrationRequestEsnHighMessage;
    private RegistrationRequestEsnLow mRegistrationRequestEsnLowMessage;

    private MessageDirection mDirection;

    public LTRNetMessageProcessor(MessageDirection direction)
    {
        mDirection = direction;
    }

    @Override
    public void receive(CorrectedBinaryMessage buffer)
    {
        LtrNetMessage message = LtrNetMessageFactory.create(mDirection, buffer, System.currentTimeMillis());

        if(message.isValid())
        {
            switch(message.getLtrNetMessageType())
            {
                case OSW_TRANSMIT_FREQUENCY_HIGH:
                    if(message instanceof TransmitFrequencyHigh)
                    {
                        TransmitFrequencyHigh transmitFrequencyHigh = (TransmitFrequencyHigh)message;
                        mTransmitHighMessageMap.put(transmitFrequencyHigh.getChannel(), transmitFrequencyHigh);
                        transmitFrequencyHigh.setFrequencyLow(mTransmitLowMessageMap.get(transmitFrequencyHigh.getChannel()));
                    }
                    break;
                case OSW_TRANSMIT_FREQUENCY_LOW:
                    if(message instanceof TransmitFrequencyLow)
                    {
                        TransmitFrequencyLow transmitFrequencyLow = (TransmitFrequencyLow)message;
                        mTransmitLowMessageMap.put(transmitFrequencyLow.getChannel(), transmitFrequencyLow);
                        transmitFrequencyLow.setFrequencyHigh(mTransmitHighMessageMap.get(transmitFrequencyLow.getChannel()));
                    }
                    break;
                case OSW_RECEIVE_FREQUENCY_HIGH:
                    if(message instanceof ReceiveFrequencyHigh)
                    {
                        ReceiveFrequencyHigh receiveFrequencyHigh = (ReceiveFrequencyHigh)message;
                        mReceiveHighMessageMap.put(receiveFrequencyHigh.getChannel(), receiveFrequencyHigh);
                        receiveFrequencyHigh.setFrequencyLow(mReceiveLowMessageMap.get(receiveFrequencyHigh.getChannel()));
                    }
                    break;
                case OSW_RECEIVE_FREQUENCY_LOW:
                    if(message instanceof ReceiveFrequencyLow)
                    {
                        ReceiveFrequencyLow receiveFrequencyLow = (ReceiveFrequencyLow)message;
                        mReceiveLowMessageMap.put(receiveFrequencyLow.getChannel(), receiveFrequencyLow);
                        receiveFrequencyLow.setFrequencyHigh(mReceiveHighMessageMap.get(receiveFrequencyLow.getChannel()));
                    }
                    break;
                case ISW_REGISTRATION_REQUEST_ESN_HIGH:
                    if(message instanceof RegistrationRequestEsnHigh)
                    {
                        ((RegistrationRequestEsnHigh)message).setEsnLowMessage(mRegistrationRequestEsnLowMessage);
                    }
                    break;
                case ISW_REGISTRATION_REQUEST_ESN_LOW:
                    if(message instanceof RegistrationRequestEsnLow)
                    {
                        ((RegistrationRequestEsnLow)message).setESNHighMessage(mRegistrationRequestEsnHighMessage);
                    }
                    break;
                case ISW_CALL_START:
                case ISW_CALL_END:
                    //Reset the esn messages
                    mRegistrationRequestEsnHighMessage = null;
                    mRegistrationRequestEsnLowMessage = null;
                    break;
            }

            if(mMessageListener != null)
            {
                mMessageListener.receive(message);
            }
        }
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
