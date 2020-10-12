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

package io.github.dsheirer.eventbus;

import com.google.common.eventbus.EventBus;

/**
 * System wide event bus for dispatching/broadcasting system wide events or objects
 */
public class MyEventBus
{
    private static final EventBus GLOBAL_EVENT_BUS = new EventBus();

    //TODO: this is primarily used by the Preference Service ... move this event bus to the Preference Service
    public static EventBus getGlobalEventBus()
    {
        return GLOBAL_EVENT_BUS;
    }
}
