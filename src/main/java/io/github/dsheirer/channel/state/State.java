/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014-2016 Dennis Sheirer
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package io.github.dsheirer.channel.state;

import java.util.EnumSet;

/**
 * Details the set of states for a channel and the allowable transition states
 */
public enum State
{
    /**
     * Active state indicates that valid messages are being decoded that do not indicate any call or data
     * activity.  For example, this is used for P25 when a channel is actively transmitting Terminator Data
     * Units in between data packets or at the beginning or end of a call.
     */
    ACTIVE("ACTIVE")
        {
            @Override
            public boolean canChangeTo(State state)
            {
                return state == CALL ||
                       state == CONTROL ||
                       state == DATA ||
                       state == ENCRYPTED ||
                       state == FADE ||
                       state == TEARDOWN;
            }
        },
    /**
     * Indicates that call messages are being decoded, or a call is in progress (ie producing audio).
     */
    CALL("CALL")
        {
            @Override
            public boolean canChangeTo(State state)
            {
                return state == ACTIVE ||
                       state == CONTROL ||
                       state == DATA ||
                       state == ENCRYPTED ||
                       state == FADE ||
                       state == TEARDOWN;
            }
        },
    /**
     * Indicates a trunking control channel is being decoded.
     */
    CONTROL("CONTROL")
        {
            @Override
            public boolean canChangeTo(State state)
            {
                return state == IDLE ||
                       state == FADE;
            }
        },
    /**
     * Indicates data packets are being decoded
     */
    DATA("DATA")
        {
            @Override
            public boolean canChangeTo(State state)
            {
                return state == ACTIVE ||
                       state == CALL ||
                       state == CONTROL ||
                       state == ENCRYPTED ||
                       state == FADE ||
                       state == TEARDOWN;
            }
        },
    /**
     * Indicates encrypted audio is detected.
     */
    ENCRYPTED("ENCRYPTED")
        {
            @Override
            public boolean canChangeTo(State state)
            {
                return state == FADE ||
                       state == TEARDOWN;
            }
        },
    /**
     * Fade state occurs when the channel is no longer actively decoding an messages.  This is a transitory
     * state before transition to IDLE or back to an active state.
     */
    FADE("FADE")
        {
            @Override
            public boolean canChangeTo(State state)
            {
                return state != FADE &&
                       state != RESET;
            }
        },
    /**
     * Indicates the channel is idle and not actively decoding any messages
     */
    IDLE("IDLE")
        {
            @Override
            public boolean canChangeTo(State state)
            {
                return state != TEARDOWN &&
                       state != RESET;
            }
        },

    /**
     * Indicates that the channel has been reset and is ready to use for a new decoding session.
     */
    RESET("RESET")
        {
            @Override
            public boolean canChangeTo(State state)
            {
                return state == IDLE;
            }
        },
    /**
     * Call or data teardown.  This state is used by traffic channels to indicate that a call or data session
     * has concluded and the channel is ready for tear down and reuse.
     */
    TEARDOWN("TEARDOWN")
        {
            @Override
            public boolean canChangeTo(State state)
            {
                return state == RESET;
            }
        };

    private String mDisplayValue;

    public static final EnumSet<State> CALL_STATES = EnumSet.of(ACTIVE, CALL, CONTROL, DATA, ENCRYPTED);
    public static final EnumSet<State> IDLE_STATES = EnumSet.of(IDLE, FADE);

    private State(String displayValue)
    {
        mDisplayValue = displayValue;
    }

    public abstract boolean canChangeTo(State state);

    /**
     * Indicates that this state is an active (ie decoding) state and is one of the states
     * enumerated in the CALL_STATES enumeration set.
     *
     * @return true if this state is an active state
     */
    public boolean isActiveState()
    {
        return CALL_STATES.contains(this);
    }

    public String getDisplayValue()
    {
        return mDisplayValue;
    }

    @Override
    public String toString()
    {
        return mDisplayValue;
    }
}