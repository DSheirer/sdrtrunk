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

package io.github.dsheirer.audio.broadcast.activity;

/**
 * Status of one call that was selected for one stream.
 */
public enum StreamStatus
{
    /**
     * Queued for the stream, waiting for the stream delay, the connection or the calls ahead of it.
     */
    PENDING("Pending", Category.IN_PROGRESS, false),
    /**
     * Icecast/Shoutcast: the call audio is being streamed.
     */
    STREAMING("Streaming", Category.IN_PROGRESS, false),
    /**
     * Call upload services: the call is being uploaded.
     */
    UPLOADING("Uploading", Category.IN_PROGRESS, false),
    /**
     * The call was streamed or uploaded.
     */
    SENT("Sent", Category.SENT, true),
    /**
     * The server rejected the call as a duplicate (e.g. Broadcastify Calls already has it from another uploader).
     */
    DUPLICATE("Duplicate", Category.SKIPPED, true),
    /**
     * Not sent because of a setting, for example duplicate call streaming suppression.
     */
    SUPPRESSED("Suppressed", Category.SKIPPED, true),
    /**
     * Not sent: the call waited longer than the maximum recording age of the stream.
     */
    AGED_OFF("Aged Off", Category.NOT_SENT, true),
    /**
     * Not sent: there's no such stream, the stream is disabled or stopped, not connected, or the call has no audio.
     */
    NOT_SENT("Not Sent", Category.NOT_SENT, true),
    /**
     * Streaming or uploading the call failed.
     */
    ERROR("Error", Category.ERROR, true);

    /**
     * Status group used for filtering and coloring.
     */
    public enum Category {IN_PROGRESS, SENT, SKIPPED, NOT_SENT, ERROR}

    private final String mLabel;
    private final Category mCategory;
    private final boolean mFinal;

    StreamStatus(String label, Category category, boolean isFinal)
    {
        mLabel = label;
        mCategory = category;
        mFinal = isFinal;
    }

    public Category getCategory()
    {
        return mCategory;
    }

    /**
     * Indicates if the stream is done with the call, so that the call status won't change again.
     */
    public boolean isFinal()
    {
        return mFinal;
    }

    /**
     * Indicates a call that was selected for the stream but wasn't sent, other than a duplicate or suppressed call.
     */
    public boolean isProblem()
    {
        return mCategory == Category.NOT_SENT || mCategory == Category.ERROR;
    }

    @Override
    public String toString()
    {
        return mLabel;
    }
}
