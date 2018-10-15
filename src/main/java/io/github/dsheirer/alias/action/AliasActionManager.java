/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
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
 *
 ******************************************************************************/
package io.github.dsheirer.alias.action;

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.gui.SDRTrunk;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.Message;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.util.ThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Manages all alias action events.  Each received message is interrogated for
 * any alias entries and then each alias is interrogated for any alias actions.
 *
 * Each alias action is executed and provided a copy of the source message so
 * that the contents of the message can be used as part of the action.
 */
public class AliasActionManager implements Listener<IMessage>
{
    private final static Logger mLog = LoggerFactory.getLogger(AliasActionManager.class);

    public AliasActionManager()
    {
        //TODO: add support for multiple alias lists and merge in the changes for alias in the new alias branch
    }

    @Override
    public void receive(IMessage message)
    {
        if(message.isValid())
        {
            mLog.debug("Update alias action manager to support new alias list construct");
//            List<Alias> aliases = message.getAliases();
//
//            if(aliases != null)
//            {
//                for(Alias alias : aliases)
//                {
//                    if(alias.hasActions())
//                    {
//                        List<AliasAction> actions = alias.getAction();
//
//                        for(AliasAction action : actions)
//                        {
//                            /* Provide access to the thread pool manager in case the
//							 * action needs to setup a timer, and provide the original
//							 * message to be used as part of the action (e.g. sending
//							 * the message as a text message to a cell phone */
//                            action.execute(ThreadPool.SCHEDULED, alias, message);
//                        }
//                    }
//                }
//            }
        }
    }
}
