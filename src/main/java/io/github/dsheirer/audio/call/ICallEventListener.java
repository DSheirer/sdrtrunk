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

package io.github.dsheirer.audio.call;

/**
 * Call event listener interface
 */
public interface ICallEventListener
{
    /**
     * Call added event.
     * @param call that was added
     */
    void added(Call call);

    /**
     * Call updated event.
     * @param call that was updated
     */
    void updated(Call call);

    /**
     * Call completed event.  This indicates that the call was completed and no further updates will be available.
     * @param call that was completed.
     * @param audioSegment for the call
     */
    void completed(Call call, AudioSegment audioSegment);

    /**
     * Call that was deleted (age-off) from the database.
     * @param call that was deleted.
     */
    void deleted(Call call);
}
