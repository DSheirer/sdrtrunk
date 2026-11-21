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

import io.github.dsheirer.audio.broadcast.BroadcastEvent;
import io.github.dsheirer.audio.broadcast.BroadcastModel;
import io.github.dsheirer.audio.broadcast.BroadcastState;
import io.github.dsheirer.audio.broadcast.ConfiguredBroadcast;
import io.github.dsheirer.gui.broadcast.BroadcastStatusView;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.settings.Setting;
import java.util.ArrayList;
import java.util.List;
import javafx.collections.ListChangeListener;

/**
 * Streaming status: connected streams out of the enabled streams, with an error dot when any stream has an error.
 * A failed stream is easy to miss while the Streaming tab isn't showing.
 */
public class StreamingStatusItem extends ServiceStatusItem
{
    private final BroadcastModel mBroadcastModel;
    private final Listener<BroadcastEvent> mBroadcastEventListener = event -> scheduleUpdate();
    private final ListChangeListener<ConfiguredBroadcast> mListChangeListener = change -> scheduleUpdate();
    private String mActionHint = "";

    /**
     * Constructs an instance.  JavaFX thread only.
     */
    public StreamingStatusItem(BroadcastModel broadcastModel)
    {
        super("Streaming", Setting.MAIN_WINDOW_STATUS_ITEM_STREAMING);
        mBroadcastModel = broadcastModel;
        mBroadcastModel.addListener(mBroadcastEventListener);
        mBroadcastModel.getConfiguredBroadcasts().addListener(mListChangeListener);
        update();
    }

    /**
     * Sets the click action and the hint that the tooltip shows for it.
     */
    public void setAction(Runnable action, String hint)
    {
        mActionHint = hint != null ? hint : "";
        setAction(action);
        update();
    }

    @Override
    protected void update()
    {
        List<ConfiguredBroadcast> broadcasts = new ArrayList<>(mBroadcastModel.getConfiguredBroadcasts());
        int enabled = 0;
        int connected = 0;
        StatusSeverity severity = StatusSeverity.NEUTRAL;
        StringBuilder details = new StringBuilder();

        for(ConfiguredBroadcast broadcast: broadcasts)
        {
            BroadcastState state = broadcast.getStatus();
            BroadcastStatusView.Severity broadcastSeverity = BroadcastStatusView.getSeverity(state);

            if(broadcastSeverity != BroadcastStatusView.Severity.DISABLED)
            {
                enabled++;
            }

            switch(broadcastSeverity)
            {
                case OK ->
                {
                    connected++;
                    severity = severity.max(StatusSeverity.OK);
                }
                case WARNING -> severity = severity.max(StatusSeverity.WARNING);
                case ERROR -> severity = severity.max(StatusSeverity.ERROR);
                default -> {}
            }

            details.append("\n").append(broadcast.getBroadcastConfiguration().getName()).append(": ")
                .append(state != null ? state : "Unknown");
        }

        //Streams that are enabled but none connected, for example while they connect, are worth a warning
        if(enabled > 0 && connected == 0 && severity == StatusSeverity.NEUTRAL)
        {
            severity = StatusSeverity.WARNING;
        }

        String text;
        String summary;

        if(broadcasts.isEmpty())
        {
            text = "No streams";
            summary = "No streams are configured.  Add streams in the playlist editor.";
        }
        else if(enabled == 0)
        {
            text = "Streams off";
            summary = "All " + broadcasts.size() + " streams are disabled.";
        }
        else
        {
            text = "Streaming " + connected + "/" + enabled;
            summary = connected + " of " + enabled + " enabled streams connected.";
        }

        String tooltip = summary + (details.length() > 0 ? "\n" + details : "") +
            (mActionHint.isEmpty() ? "" : "\n\n" + mActionHint);
        show(severity, text, tooltip);
    }

    @Override
    public void dispose()
    {
        mBroadcastModel.removeListener(mBroadcastEventListener);
        mBroadcastModel.getConfiguredBroadcasts().removeListener(mListChangeListener);
    }
}
