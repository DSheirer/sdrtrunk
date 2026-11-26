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
 * CPU usage of the sdrtrunk process, across all processors.
 */
public class CpuStatusItem extends ResourceStatusItem
{
    static final double WARNING_LEVEL = 0.75;
    static final double ERROR_LEVEL = 0.90;
    private final ResourceMonitor mResourceMonitor;
    private final InvalidationListener mListener = observable -> update();

    /**
     * Constructs an instance.  JavaFX thread only.
     */
    public CpuStatusItem(ResourceMonitor resourceMonitor)
    {
        super("CPU", "CPU", Setting.MAIN_WINDOW_STATUS_ITEM_CPU);
        mResourceMonitor = resourceMonitor;
        mResourceMonitor.cpuPercentageProperty().addListener(mListener);
        mResourceMonitor.cpuAvailableProperty().addListener(mListener);
        setPercentValue();
        update();
    }

    @Override
    protected void update()
    {
        if(!mResourceMonitor.cpuAvailableProperty().get())
        {
            show(0, StatusSeverity.NEUTRAL, "n/a", "CPU usage isn't available from this Java runtime.");
            return;
        }

        double cpu = mResourceMonitor.cpuPercentageProperty().get();
        double level = mResourceMonitor.cpuLevelPercentageProperty().get();
        double system = mResourceMonitor.systemCpuPercentageProperty().get();
        StatusSeverity severity = StatusSeverity.forLevel(level, WARNING_LEVEL, ERROR_LEVEL);

        StringBuilder sb = new StringBuilder();
        sb.append("sdrtrunk CPU usage: ").append(percent(cpu)).append(" of all ")
            .append(Runtime.getRuntime().availableProcessors()).append(" processors");

        if(system >= 0)
        {
            sb.append("\nWhole system: ").append(percent(system));
        }

        if(severity != StatusSeverity.OK)
        {
            sb.append("\n\nsdrtrunk is using most of the processing power.  Audio and decoding can break up.  ")
                .append("Reduce the number of channels, or the tuner sample rate.");
        }

        show(cpu, severity, percent(cpu), sb.toString());
    }

    @Override
    public void dispose()
    {
        mResourceMonitor.cpuPercentageProperty().removeListener(mListener);
        mResourceMonitor.cpuAvailableProperty().removeListener(mListener);
    }
}
