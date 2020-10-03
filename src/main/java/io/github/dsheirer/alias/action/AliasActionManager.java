/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2020 Dennis Sheirer
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
package io.github.dsheirer.alias.action;

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasList;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.message.IMessageListener;
import io.github.dsheirer.module.Module;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Manages all alias action events.  Each received message is interrogated for any identifiers that may be aliasable
 * and have associated alias actions.
 *
 * Each alias action is executed and provided a copy of the source message so that the contents of the message can be
 * used as part of the action.
 */
public class AliasActionManager extends Module implements IMessageListener, Listener<IMessage>
{
    private final static Logger mLog = LoggerFactory.getLogger(AliasActionManager.class);
    private AliasList mAliasList;

    public AliasActionManager(AliasList aliasList)
    {
        mAliasList = aliasList;
    }

    @Override
    public void receive(IMessage message)
    {
        if(mAliasList != null && message.isValid())
        {
            List<Identifier> identifiers = message.getIdentifiers();

            for(Identifier identifier: identifiers)
            {
                List<Alias> aliases = mAliasList.getAliases(identifier);

                for(Alias alias: aliases)
                {
                    if(alias != null && alias.hasActions())
                    {
                        for(AliasAction action: alias.getAliasActions())
                        {
                            action.execute(alias, message);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void reset()
    {
        //No actions needed
    }

    @Override
    public void start()
    {
        //No actions needed
    }

    @Override
    public void stop()
    {
        //No actions neeeded
    }

    /**
     * IMessageListener interface ... delegates to this class implementation of Listener<Message>
     */
    @Override
    public Listener<IMessage> getMessageListener()
    {
        return this;
    }
}
