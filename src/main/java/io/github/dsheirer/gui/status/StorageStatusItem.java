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

import io.github.dsheirer.monitor.ResourceMonitor.StorageUsage;
import io.github.dsheirer.settings.Setting;
import javafx.beans.InvalidationListener;
import javafx.beans.property.ObjectProperty;

/**
 * Space used by a storage folder (recordings or event logs), against its limit in the storage settings.
 */
public class StorageStatusItem extends ResourceStatusItem
{
    static final double WARNING_LEVEL = 0.80;
    static final double ERROR_LEVEL = 0.95;
    private final String mTitle;
    private final ObjectProperty<StorageUsage> mUsage;
    private final InvalidationListener mListener = observable -> update();

    /**
     * Constructs an instance.  JavaFX thread only.
     * @param title for the menu and tooltip, for example Recordings
     * @param name shown in the status bar, for example Rec
     * @param shownSetting that stores whether the item is shown
     * @param usage of the folder, from the resource monitor
     */
    public StorageStatusItem(String title, String name, Setting shownSetting, ObjectProperty<StorageUsage> usage)
    {
        super(title, name, shownSetting);
        mTitle = title;
        mUsage = usage;
        mUsage.addListener(mListener);
        update();
    }

    @Override
    protected void update()
    {
        StorageUsage usage = mUsage.get();

        if(usage == null)
        {
            show(0, StatusSeverity.NEUTRAL, "…", mTitle + ": measuring the folder");
            return;
        }

        double fraction = usage.fraction();
        StatusSeverity severity = StatusSeverity.forLevel(fraction, WARNING_LEVEL, ERROR_LEVEL);

        StringBuilder sb = new StringBuilder();
        sb.append(mTitle).append(": ").append(bytes(usage.used())).append(" of ")
            .append(bytes(usage.effectiveLimit())).append(" (").append(percent(fraction)).append(")");

        if(usage.isDriveLimited())
        {
            sb.append("\nLimited by the free space on the drive: ").append(bytes(usage.free())).append(" free.  ")
                .append("The limit in the storage settings is ").append(bytes(usage.limit())).append(".");
        }
        else
        {
            sb.append("\nThe limit is set in Settings > Storage.  ").append(bytes(usage.free()))
                .append(" free on the drive.");
        }

        sb.append("\nFolder: ").append(usage.folder());

        if(severity != StatusSeverity.OK)
        {
            sb.append("\n\nThe folder is close to its limit.  Delete or move old files, or raise the limit.");
        }

        if(isClickable())
        {
            sb.append("\n\nClick to open the folder.");
        }

        show(fraction, severity, bytes(usage.used()), sb.toString());
    }

    private boolean isClickable()
    {
        return getStyleClass().contains("status-clickable");
    }

    @Override
    public void setAction(Runnable action)
    {
        super.setAction(action);
        update();
    }

    @Override
    public void dispose()
    {
        mUsage.removeListener(mListener);
    }
}
