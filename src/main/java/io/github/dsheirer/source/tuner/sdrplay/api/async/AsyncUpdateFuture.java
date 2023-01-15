/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.source.tuner.sdrplay.api.async;

import io.github.dsheirer.source.tuner.sdrplay.api.Status;
import io.github.dsheirer.source.tuner.sdrplay.api.UpdateReason;
import io.github.dsheirer.source.tuner.sdrplay.api.device.TunerSelect;

/**
 * Asynchronous future for an SDRplay API update operation.
 */
public class AsyncUpdateFuture extends AsyncFuture<Status>
{
    private TunerSelect mTunerSelect;
    private UpdateReason mUpdateReason;
    private UpdateReason mExpectedResponse;
    private boolean mSubmitted;

    /**
     * Creates a to-be completed future where a successful update with return success or an unsuccessful update will
     * return an exception.
     * @param tunerSelect for the tuner to be updated
     * @param updateReason of what is to be updated
     * @param expectedResponse to be received to indicate the async operation is completed
     */
    public AsyncUpdateFuture(TunerSelect tunerSelect, UpdateReason updateReason, UpdateReason expectedResponse)
    {
        mTunerSelect = tunerSelect;
        mUpdateReason = updateReason;
        mExpectedResponse = expectedResponse;
    }

    /**
     * Tuner(s) for the update
     */
    public TunerSelect getTunerSelect()
    {
        return mTunerSelect;
    }

    /**
     * UpdateReason that is being updated
     */
    public UpdateReason getUpdateReason()
    {
        return mUpdateReason;
    }

    /**
     * Expected response to this update operation
     */
    public UpdateReason getExpectedResponse()
    {
        return mExpectedResponse;
    }

    /**
     * Flag to indicate if this update has been submitted and is currently awaiting results.
     */
    public boolean isSubmitted()
    {
        return mSubmitted;
    }

    /**
     * Sets flag to indicate that this update has been submitted.
     */
    public void setSubmitted(boolean submitted)
    {
        mSubmitted = submitted;
    }

    /**
     * Indicates if the completed async operation matches this submitted async update future
     * @param completedAsyncUpdate to compare
     * @return true if there is a match
     */
    public boolean matches(CompletedAsyncUpdate completedAsyncUpdate)
    {
        return getTunerSelect().equals(completedAsyncUpdate.getTunerSelect()) &&
                getExpectedResponse().equals(completedAsyncUpdate.getUpdateReason());
    }
}
