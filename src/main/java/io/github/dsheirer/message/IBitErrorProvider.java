/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.message;

/**
 * Interface for elements to provide a number of bits processed and the number of errors that were
 * corrected.
 */
public interface IBitErrorProvider
{
    /**
     * Indicates the number of bits that have been processed.
     */
    int getBitsProcessedCount();

    /**
     * Indicates the number of errors contained in the bits that have been processed.
     */
    int getBitErrorsCount();
}
