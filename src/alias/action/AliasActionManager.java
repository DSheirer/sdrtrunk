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
package alias.action;

import alias.Alias;
import message.Message;
import sample.Listener;
import util.ThreadPool;

import java.util.List;

/**
 * Manages all alias action events.  Each received message is interrogated for
 * any alias entries and then each alias is interrogated for any alias actions.
 *
 * Each alias action is executed and provided a copy of the source message so
 * that the contents of the message can be used as part of the action.
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
							 * message to be used as part of the action (e.g. sending
							 * the message as a text message to a cell phone */
                            action.execute(ThreadPool.SCHEDULED, alias, message);
                        }
                    }
                }
            }
        }
    }
}
