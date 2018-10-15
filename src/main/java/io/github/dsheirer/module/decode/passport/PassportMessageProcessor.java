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
package io.github.dsheirer.module.decode.passport;

import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.bits.BinaryMessage;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.MessageType;
import io.github.dsheirer.sample.Listener;

import java.util.HashMap;

public class PassportMessageProcessor implements Listener<BinaryMessage>
{
    private Listener<IMessage> mMessageListener;
    private IdleMessageFinder mIdleFinder = new IdleMessageFinder();
    private AliasList mAliasList;
    private PassportMessage mIdleMessage;

    public PassportMessageProcessor(AliasList list)
    {
        mAliasList = list;
    }

    @Override
    public void receive(BinaryMessage buffer)
    {
        if(mMessageListener != null)
        {
            PassportMessage message;

            if(mIdleMessage != null)
            {
                message = new PassportMessage(buffer, mIdleMessage, mAliasList);
            }
            else
            {
                message = new PassportMessage(buffer, mAliasList);
                mIdleFinder.receive(message);
            }

            mMessageListener.receive(message);
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

    public class IdleMessageFinder
    {
        public HashMap<String, Integer> mMessageCounts =
                new HashMap<String, Integer>();

        public boolean mIdleMessageFound = false;

        public IdleMessageFinder()
        {
        }

        public void receive(PassportMessage message)
        {
            if(!mIdleMessageFound &&
                    message.isValid() &&
                    message.getMessageType() == MessageType.SY_IDLE)
            {
                if(mMessageCounts.containsKey(message.toString()))
                {
                    int count = mMessageCounts.get(message.toString());

                    if(count >= 3)
                    {
                        mIdleMessageFound = true;
                        mIdleMessage = message;
                        mMessageCounts = null;
                    }
                    else
                    {
                        count++;
                        mMessageCounts.put(message.toString(), count);
                    }
                }
                else
                {
                    mMessageCounts.put(message.toString(), 1);
                }
            }
        }
    }
}
