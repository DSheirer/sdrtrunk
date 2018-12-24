/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */
package io.github.dsheirer.map;

import io.github.dsheirer.icon.IconManager;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.module.decode.event.IDecodeEvent;
import io.github.dsheirer.module.decode.event.PlottableDecodeEvent;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapService implements Listener<IDecodeEvent>
{
    private final static Logger mLog = LoggerFactory.getLogger(MapService.class);

    private int mMaxHistory = 2;
    private List<IPlottableUpdateListener> mListeners = new ArrayList<IPlottableUpdateListener>();
    private Map<Identifier,PlottableEntityHistory> mEntityHistories = new HashMap<>();
    private IconManager mIconManager;

    public MapService(IconManager resourceManager)
    {
        mIconManager = resourceManager;
    }

    @Override
    public void receive(IDecodeEvent decodeEvent)
    {
        if(decodeEvent instanceof PlottableDecodeEvent)
        {
            PlottableDecodeEvent plottableDecodeEvent = (PlottableDecodeEvent)decodeEvent;
            Identifier from = plottableDecodeEvent.getIdentifierCollection().getFromIdentifier();

            if(from != null)
            {
                PlottableEntityHistory entityHistory = mEntityHistories.get(from);

                if(entityHistory == null)
                {
                    entityHistory = new PlottableEntityHistory(from, plottableDecodeEvent);
                    mEntityHistories.put(from, entityHistory);
                }
                else
                {
                    entityHistory.add(plottableDecodeEvent);
                }

                for(IPlottableUpdateListener listener : mListeners)
                {
                    listener.addPlottableEntity(entityHistory);
                }
            }
            else
            {
                mLog.warn("Received plottable decode event that does not contain a FROM identifier - cannot plot");
            }
        }
    }

    public void addListener(IPlottableUpdateListener listener)
    {
        mListeners.add(listener);
    }

    public void removeListener(IPlottableUpdateListener listener)
    {
        mListeners.remove(listener);
    }
}
