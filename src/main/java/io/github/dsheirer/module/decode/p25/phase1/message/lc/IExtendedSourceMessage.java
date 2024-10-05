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

package io.github.dsheirer.module.decode.p25.phase1.message.lc;

import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCSourceIDExtension;

/**
 * Interface to identify Link Control messages that can optionally be extended by appending source ID extension message.
 */
public interface IExtendedSourceMessage extends IMessage
{
    /**
     * Sets or assigns the optional source ID extension message.
     * @param sourceIDExtension to append.
     */
    void setSourceIDExtension(LCSourceIDExtension sourceIDExtension);

    /**
     * Indicates if this message requires an optional source extension message.
     * @return true if required.
     */
    boolean isExtensionRequired();

    /**
     * Indicates if this message requires an extended source and if that extended source is appended.
     * @return true if fully extended.
     */
    boolean isFullyExtended();

    /**
     * Indicates if this link control message was carried by a TDULC terminator message.
     * @return true if terminator.
     */
    boolean isTerminator();
}
