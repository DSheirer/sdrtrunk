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

package io.github.dsheirer.gui.broadcast;

import io.github.dsheirer.audio.broadcast.BroadcastState;
import io.github.dsheirer.audio.broadcast.ConfiguredBroadcast;
import io.github.dsheirer.audio.broadcast.icecast.IcecastHTTPConfiguration;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Streaming status severity and summary.
 */
public class BroadcastStatusViewTest
{
    @Test
    public void severityMatchesTheSwingColors()
    {
        assertEquals(BroadcastStatusView.Severity.OK, BroadcastStatusView.getSeverity(BroadcastState.CONNECTED));
        assertEquals(BroadcastStatusView.Severity.DISABLED, BroadcastStatusView.getSeverity(BroadcastState.DISABLED));
        assertEquals(BroadcastStatusView.Severity.WARNING,
            BroadcastStatusView.getSeverity(BroadcastState.INVALID_SETTINGS));
        assertEquals(BroadcastStatusView.Severity.WARNING,
            BroadcastStatusView.getSeverity(BroadcastState.NETWORK_UNAVAILABLE));
        assertEquals(BroadcastStatusView.Severity.WARNING,
            BroadcastStatusView.getSeverity(BroadcastState.MOUNT_POINT_IN_USE));
        assertEquals(BroadcastStatusView.Severity.ERROR,
            BroadcastStatusView.getSeverity(BroadcastState.INVALID_CREDENTIALS));
        assertEquals(BroadcastStatusView.Severity.NEUTRAL, BroadcastStatusView.getSeverity(BroadcastState.CONNECTING));
        assertEquals(BroadcastStatusView.Severity.NEUTRAL, BroadcastStatusView.getSeverity(null));
    }

    @Test
    public void summaryCountsDisabledAndUnstartedStreams()
    {
        IcecastHTTPConfiguration disabled = new IcecastHTTPConfiguration();
        disabled.setEnabled(false);
        IcecastHTTPConfiguration enabled = new IcecastHTTPConfiguration();
        enabled.setEnabled(true);

        //An enabled stream without a broadcaster has invalid settings (warning) or failed to start (error)
        String summary = BroadcastStatusView.getSummary(List.of(new ConfiguredBroadcast(disabled),
            new ConfiguredBroadcast(enabled)));
        assertEquals(true, summary.startsWith("0 of 2 connected"), summary);
        assertEquals(true, summary.contains("1 disabled"), summary);
        assertEquals("", BroadcastStatusView.getSummary(List.of()));
    }
}
