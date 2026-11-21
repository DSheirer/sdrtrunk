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

import io.github.dsheirer.audio.broadcast.activity.StreamActivity;
import io.github.dsheirer.audio.broadcast.activity.StreamActivityModel;
import io.github.dsheirer.audio.broadcast.activity.StreamStatus;
import io.github.dsheirer.database.streaming.StreamActivityRecorder;
import io.github.dsheirer.database.streaming.StreamActivitySearchCriteria;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Streaming tab rows: stored rows of the page merged with the live rows of this session.
 */
public class StreamingViewTest
{
    private static StreamActivity add(StreamActivityModel model, long start, String stream, StreamStatus status)
    {
        StreamActivity activity = model.add(new StreamActivity.Builder().callKey(start).callStart(start).to("1001"),
            stream, null, StreamStatus.PENDING, null);
        model.update(activity, status, null);
        return activity;
    }

    /**
     * Stored copy of the live row, as the database search returns it
     */
    private static StreamActivity stored(StreamActivity activity)
    {
        return StreamActivityRecorder.toActivity(StreamActivityRecorder.toRecord(activity));
    }

    @Test
    public void testMerge()
    {
        StreamActivityModel model = new StreamActivityModel();
        StreamActivity old = add(model, 1000, "A", StreamStatus.SENT);
        StreamActivity saved = add(model, 2000, "A", StreamStatus.UPLOADING);
        StreamActivity unsaved = add(model, 3000, "B", StreamStatus.PENDING);
        StreamActivity earlierRun = stored(add(new StreamActivityModel(), 500, "A", StreamStatus.ERROR));

        //Page 1: the live row replaces its stored copy, and the unsaved live row is added
        List<StreamActivity> stored = List.of(stored(saved), stored(old), earlierRun);
        List<StreamActivity> rows = StreamingView.getRows(model.getActivities(), stored,
            StreamActivitySearchCriteria.all(), 0, 500);
        assertEquals(List.of(unsaved.getKey(), saved.getKey(), old.getKey(), earlierRun.getKey()),
            rows.stream().map(StreamActivity::getKey).toList());
        assertSame(saved, rows.get(1));
        assertSame(earlierRun, rows.get(3));

        //Page size limits the first page
        assertEquals(2, StreamingView.getRows(model.getActivities(), stored, StreamActivitySearchCriteria.all(), 0,
            2).size());

        //Live rows that don't match the criteria aren't added
        StreamActivitySearchCriteria streamA = new StreamActivitySearchCriteria("A", Set.of(), "");
        rows = StreamingView.getRows(model.getActivities(), stored, streamA, 0, 500);
        assertEquals(3, rows.size());

        //Later pages show only the stored rows, with live rows in place of their stored copies
        rows = StreamingView.getRows(model.getActivities(), List.of(stored(old), earlierRun), streamA, 500, 500);
        assertSame(old, rows.getFirst());
        assertEquals(2, rows.size());
    }
}
