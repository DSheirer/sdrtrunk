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

import io.github.dsheirer.bits.CorrectedBinaryMessage;

/**
 * No puncturing provider
 */
public class PunctureProviderNone extends PunctureProvider
{
    /**
     * Constructs an instance
     */
    public PunctureProviderNone()
    {
        super(0, 0);
    }

    @Override
    public CorrectedBinaryMessage puncture(CorrectedBinaryMessage original)
    {
        return original;
    }

    @Override
    public CorrectedBinaryMessage depuncture(CorrectedBinaryMessage punctured)
    {
        return punctured;
    }

    @Override
    public boolean isPreserved(int index)
    {
        return true;
    }

    @Override
    public boolean isPunctured(int index)
    {
        return false;
    }
}
