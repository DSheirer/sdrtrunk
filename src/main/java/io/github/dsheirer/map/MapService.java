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
package io.github.dsheirer.map;

import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.icon.IconModel;
import io.github.dsheirer.module.decode.event.IDecodeEvent;
import io.github.dsheirer.module.decode.event.PlottableDecodeEvent;
import io.github.dsheirer.sample.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapService implements Listener<IDecodeEvent>
{
    private final static Logger mLog = LoggerFactory.getLogger(MapService.class);
    private IconModel mIconModel;
    private PlottableEntityModel mPlottableEntityModel;

    /**
     * Constructs an instance
     * @param aliasModel to lookup aliases
     * @param iconModel to lookup icons from entity aliases.
     */
    public MapService(AliasModel aliasModel, IconModel iconModel)
    {
        mPlottableEntityModel = new PlottableEntityModel(aliasModel);
        mIconModel = iconModel;
    }

    /**
     * Table model that holds the plottable entity history
     */
    public PlottableEntityModel getPlottableEntityModel()
    {
        return mPlottableEntityModel;
    }

    @Override
    public void receive(IDecodeEvent decodeEvent)
    {
        if(decodeEvent instanceof PlottableDecodeEvent plottableDecodeEvent)
        {
            mPlottableEntityModel.receive(plottableDecodeEvent);
        }
    }

    public void addListener(IPlottableUpdateListener listener)
    {
        mPlottableEntityModel.addListener(listener);
    }

    public void removeListener(IPlottableUpdateListener listener)
    {
        mPlottableEntityModel.removeListener(listener);
    }
}
