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
package ua.in.smartjava.alias.action;

import ua.in.smartjava.alias.Alias;
import ua.in.smartjava.message.Message;
import ua.in.smartjava.sample.Listener;
import ua.in.smartjava.util.ThreadPool;

import java.util.List;

/**
 * Manages all ua.in.smartjava.alias action events.  Each received ua.in.smartjava.message is interrogated for
 * any ua.in.smartjava.alias entries and then each ua.in.smartjava.alias is interrogated for any ua.in.smartjava.alias actions.
 *
 * Each ua.in.smartjava.alias action is executed and provided a copy of the ua.in.smartjava.source ua.in.smartjava.message so
 * that the contents of the ua.in.smartjava.message can be used as part of the action.
 */
public class AliasActionManager implements Listener<Message>
{
    public AliasActionManager()
    {
    }

    @Override
    public void receive(Message message)
    {
        if(message.isValid())
        {
            List<Alias> aliases = message.getAliases();

            if(aliases != null)
            {
                for(Alias alias : aliases)
                {
                    if(alias.hasActions())
                    {
                        List<AliasAction> actions = alias.getAction();

                        for(AliasAction action : actions)
                        {
                            /* Provide access to the thread pool manager in case the
							 * action needs to setup a timer, and provide the original
							 * ua.in.smartjava.message to be used as part of the action (e.g. sending
							 * the ua.in.smartjava.message as a text ua.in.smartjava.message to a cell phone */
                            action.execute(ThreadPool.SCHEDULED, alias, message);
                        }
                    }
                }
            }
        }
    }
}
