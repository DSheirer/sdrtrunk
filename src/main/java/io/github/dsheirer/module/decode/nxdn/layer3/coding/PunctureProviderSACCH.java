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

package io.github.dsheirer.module.decode.nxdn.layer3.coding;

/**
 * Puncture provider for SACCH messages
 */
public class PunctureProviderSACCH extends PunctureProvider
{
    private static final int BLOCK_SIZE = 12;
    private static final int PUNCTURE_BIT_1 = 5;
    private static final int PUNCTURE_BIT_2 = 11;

    /**
     * Constructs an instance
     */
    public PunctureProviderSACCH()
    {
        super(BLOCK_SIZE, 2);
    }

    @Override
    public boolean isPreserved(int index)
    {
        int mod = index % BLOCK_SIZE;
        return (mod != PUNCTURE_BIT_1) && (mod != PUNCTURE_BIT_2);
    }

    @Override
    public boolean isPunctured(int index)
    {
        int mod = index % BLOCK_SIZE;
        return (mod == PUNCTURE_BIT_1) || (mod == PUNCTURE_BIT_2);
    }
}
