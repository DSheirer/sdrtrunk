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
package io.github.dsheirer.module.decode.config;

import io.github.dsheirer.message.IMessage;

/**
 * Runtime gate that applies an access code filter (NAC, color code, RAN) to a decoded message stream.
 *
 * Not every message carries a trustworthy access code: DMR voice bursts A and F have no EMB field, and NXDN audio and
 * FACCH1 messages inherit the RAN from a SACCH that may have failed its CRC.  Messages without a trusted access code
 * follow the decision made for the most recent message that had one, so that a call that was accepted (or rejected)
 * stays accepted (or rejected) across those messages.  The decision defaults to pass, and returns to pass on reset().
 *
 * Each consumer of the message stream (decoder state, audio module) uses its own gate instance, so that the gates
 * hold independent state for each timeslot.  The gate copies the filter at construction so that later edits to the
 * channel configuration don't change a running gate.
 */
public abstract class AccessCodeGate
{
    private final AccessCodeFilter mFilter;
    private final boolean mActive;
    private boolean mLastDecision = true;

    /**
     * Constructs an instance
     * @param filter to apply, or null for a gate that passes all messages
     */
    protected AccessCodeGate(AccessCodeFilter filter)
    {
        mFilter = filter != null ? filter.copy() : null;
        mActive = mFilter != null && mFilter.isActive();
    }

    /**
     * Indicates if the gate has an active filter.
     */
    public boolean isActive()
    {
        return mActive;
    }

    /**
     * Indicates if the message passes the gate.
     * @param message to test
     * @return true if the message should be processed
     */
    public boolean passes(IMessage message)
    {
        if(!mActive)
        {
            return true;
        }

        Integer accessCode = getTrustedAccessCode(message);

        if(accessCode != null)
        {
            mLastDecision = mFilter.passes(accessCode);
        }

        return mLastDecision;
    }

    /**
     * Resets the gate decision to pass.
     */
    public void reset()
    {
        mLastDecision = true;
    }

    /**
     * Extracts the access code from the message, when the message carries one that passed error detection.
     * @param message to inspect
     * @return access code, or null when the message has no trusted access code.
     */
    protected abstract Integer getTrustedAccessCode(IMessage message);
}
