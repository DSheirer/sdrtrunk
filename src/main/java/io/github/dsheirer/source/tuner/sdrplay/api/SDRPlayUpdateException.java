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

package io.github.dsheirer.source.tuner.sdrplay.api;

import java.util.ArrayList;
import java.util.List;

/**
 * SDRplay failed update request checked exception.
 */
public class SDRPlayUpdateException extends SDRPlayException
{
    private List<UpdateReason> mUpdateReasons = new ArrayList<>();

    /**
     * Creates an operation exception
     * @param status of the operation
     * @param updateReasons that were submitted
     */
    public SDRPlayUpdateException(Status status, List<UpdateReason> updateReasons)
    {
        super("Unable to update device parameters: " + updateReasons, status);
    }

    /**
     * List of update reasons that failed.
     */
    public List<UpdateReason> getUpdateReasons()
    {
        return mUpdateReasons;
    }
}
