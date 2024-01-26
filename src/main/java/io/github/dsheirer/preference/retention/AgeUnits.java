/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

package io.github.dsheirer.preference.retention;

import java.time.Duration;

/**
 * Age units
 */
public enum AgeUnits
{
    DAYS,
    WEEKS,
    MONTHS,
    YEARS;

    /**
     * Returns a duration value in milliseconds that represents the value in the current units.
     * @param value to calculate
     * @return milliseconds of duration representing the age unit and value.
     */
    public long getDuration(int value)
    {
        return switch(this)
        {
            case DAYS -> Duration.ofDays(value).toMillis();
            case WEEKS -> Duration.ofDays(7).toMillis() * value;
            case MONTHS -> Duration.ofDays(31).toMillis() * value;
            case YEARS -> Duration.ofDays(365).toMillis() * value;
        };
    }
}
