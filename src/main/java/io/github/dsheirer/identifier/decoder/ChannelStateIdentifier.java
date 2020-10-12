/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2019 Dennis Sheirer
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
 * *****************************************************************************
 */

package io.github.dsheirer.identifier.decoder;

import io.github.dsheirer.channel.state.State;
import io.github.dsheirer.identifier.Form;
import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.identifier.IdentifierClass;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.protocol.Protocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Channel state identifier.  Reflects the current state of the channel.
 */
public class ChannelStateIdentifier extends Identifier<State>
{
    private final static Logger mLog = LoggerFactory.getLogger(ChannelStateIdentifier.class);

    public static final ChannelStateIdentifier ACTIVE = new ChannelStateIdentifier(State.ACTIVE);
    public static final ChannelStateIdentifier CALL = new ChannelStateIdentifier(State.CALL);
    public static final ChannelStateIdentifier CONTROL = new ChannelStateIdentifier(State.CONTROL);
    public static final ChannelStateIdentifier DATA = new ChannelStateIdentifier(State.DATA);
    public static final ChannelStateIdentifier ENCRYPTED = new ChannelStateIdentifier(State.ENCRYPTED);
    public static final ChannelStateIdentifier FADE = new ChannelStateIdentifier(State.FADE);
    public static final ChannelStateIdentifier IDLE = new ChannelStateIdentifier(State.IDLE);
    public static final ChannelStateIdentifier TEARDOWN = new ChannelStateIdentifier(State.TEARDOWN);
    public static final ChannelStateIdentifier RESET = new ChannelStateIdentifier(State.RESET);

    public ChannelStateIdentifier(State value)
    {
        super(value, IdentifierClass.DECODER, Form.STATE, Role.ANY);
    }

    @Override
    public Protocol getProtocol()
    {
        return Protocol.UNKNOWN;
    }

    /**
     * Creates a decoder state identifier from the decoder state value
     */
    public static ChannelStateIdentifier get(State state)
    {
        switch(state)
        {
            case ACTIVE:
                return ACTIVE;
            case CALL:
                return CALL;
            case CONTROL:
                return CONTROL;
            case DATA:
                return DATA;
            case ENCRYPTED:
                return ENCRYPTED;
            case FADE:
                return FADE;
            case IDLE:
                return IDLE;
            case RESET:
                return RESET;
            case TEARDOWN:
                return TEARDOWN;
            default:
                mLog.warn("Creating new channel state identifier for unrecognized state [" + state + "]");
                return new ChannelStateIdentifier(state);
        }
    }

    @Override
    public String toString()
    {
        return getValue().getDisplayValue();
    }
}
