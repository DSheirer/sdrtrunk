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

import io.github.dsheirer.bits.CorrectedBinaryMessage;
import io.github.dsheirer.bits.IntField;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.radio.FullyQualifiedRadioIdentifier;
import io.github.dsheirer.module.decode.p25.identifier.radio.APCO25FullyQualifiedRadioIdentifier;
import io.github.dsheirer.module.decode.p25.phase1.message.P25P1Message;
import io.github.dsheirer.module.decode.p25.phase1.message.lc.standard.LCSourceIDExtension;
import io.github.dsheirer.protocol.Protocol;
import java.util.List;

/**
 * Extended link control word.  Base class for adding in the extension word for messages that require two link control
 * fragments to fit the content.
 */
public abstract class ExtendedSourceLinkControlWord extends LinkControlWord implements IExtendedSourceMessage
{
    private static final IntField SOURCE_ADDRESS = IntField.length24(OCTET_6_BIT_48);
    private LCSourceIDExtension mSourceIDExtension;
    private FullyQualifiedRadioIdentifier mSourceAddress;
    private long mTimestamp;
    private boolean mTerminator;
    protected List<Identifier> mIdentifiers;

    /**
     * Constructs a Link Control Word from the binary message sequence.
     *
     * @param message
     * @param timestamp of this message.
     * @param isTerminator to indicate if message is carried by a TDULC terminator message
     */
    public ExtendedSourceLinkControlWord(CorrectedBinaryMessage message, long timestamp, boolean isTerminator)
    {
        super(message);
        mTimestamp = timestamp;
        mTerminator = isTerminator;
    }

    @Override
    public boolean isTerminator()
    {
        return mTerminator;
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.APCO25;
    }

    @Override
    public long getTimestamp()
    {
        return mTimestamp;
    }

    @Override
    public int getTimeslot()
    {
        return P25P1Message.TIMESLOT_0;
    }

    /**
     * Optional source ID extension message for a fully qualified radio source.
     */
    protected LCSourceIDExtension getSourceIDExtension()
    {
        return mSourceIDExtension;
    }

    /**
     * Sets the extension message.
     */
    public void setSourceIDExtension(LCSourceIDExtension extension)
    {
        mSourceIDExtension = extension;
        mSourceAddress = null;
        mIdentifiers = null;
    }

    /**
     * Indicates that this LCW always requires an extended source.
     */
    @Override
    public boolean isExtensionRequired()
    {
        return true;
    }

    /**
     * Indicates if this message has the source ID extension message appended.
     */
    @Override
    public boolean isFullyExtended()
    {
        return mSourceIDExtension != null;
    }

    /**
     * Indicates if this message is carrying a source ID extension message.
     */
    protected boolean hasSourceIDExtension()
    {
        return mSourceIDExtension != null;
    }

    /**
     * Source address
     */
    public FullyQualifiedRadioIdentifier getSourceAddress()
    {
        if(mSourceAddress == null && hasSourceIDExtension())
        {
            int radio = getInt(SOURCE_ADDRESS);
            int wacn = getSourceIDExtension().getWACN();
            int system = getSourceIDExtension().getSystem();
            int id = getSourceIDExtension().getId();
            mSourceAddress = APCO25FullyQualifiedRadioIdentifier.createFrom(radio, wacn, system, id);
        }

        return mSourceAddress;
    }
}
