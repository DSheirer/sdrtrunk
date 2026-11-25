/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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

package io.github.dsheirer.gui.status;

import io.github.dsheirer.monitor.ResourceMonitor;
import io.github.dsheirer.settings.Setting;
import javafx.beans.InvalidationListener;

/**
 * Java heap memory used, against the maximum heap (-Xmx).  The warning color follows the memory still in use after
 * garbage collection, since used memory rises and falls with every collection.
 */
public class MemoryStatusItem extends ResourceStatusItem
{
    static final double WARNING_LEVEL = 0.80;
    static final double ERROR_LEVEL = 0.90;
    private final ResourceMonitor mResourceMonitor;
    private final InvalidationListener mListener = observable -> update();

    /**
     * Constructs an instance.  JavaFX thread only.
     */
    public MemoryStatusItem(ResourceMonitor resourceMonitor)
    {
        super("Memory", "Mem", Setting.MAIN_WINDOW_STATUS_ITEM_MEMORY);
        mResourceMonitor = resourceMonitor;
        mResourceMonitor.memoryUsedProperty().addListener(mListener);
        setPercentValue();
        update();
    }

    @Override
    protected void update()
    {
        double used = mResourceMonitor.memoryUsedPercentageProperty().get();
        double level = mResourceMonitor.memoryLevelPercentageProperty().get();
        StatusSeverity severity = StatusSeverity.forLevel(level, WARNING_LEVEL, ERROR_LEVEL);

        StringBuilder sb = new StringBuilder();
        sb.append("Java memory: ").append(bytes(mResourceMonitor.memoryUsedProperty().get())).append(" used of ")
            .append(bytes(mResourceMonitor.memoryMaxProperty().get())).append(" maximum (").append(percent(used))
            .append(")\nReserved from the operating system: ")
            .append(bytes(mResourceMonitor.memoryAllocatedProperty().get()))
            .append("\n\nUsed memory rises and falls as Java collects unused memory.");

        if(severity != StatusSeverity.OK)
        {
            sb.append("\n\nMemory stays close to the maximum, even after collection.  sdrtrunk can slow down or stop.")
                .append("  Raise the maximum memory with the -Xmx option in the sdrtrunk launch script.");
        }

        show(used, severity, percent(used), sb.toString());
    }

    @Override
    public void dispose()
    {
        mResourceMonitor.memoryUsedProperty().removeListener(mListener);
    }
}
