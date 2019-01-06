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

public class ChannelStateIdentifier extends Identifier<State>
{
    public static final ChannelStateIdentifier ACTIVE = ChannelStateIdentifier.create(State.ACTIVE);
    public static final ChannelStateIdentifier CALL = ChannelStateIdentifier.create(State.CALL);
    public static final ChannelStateIdentifier CONTROL = ChannelStateIdentifier.create(State.CONTROL);
    public static final ChannelStateIdentifier DATA = ChannelStateIdentifier.create(State.DATA);
    public static final ChannelStateIdentifier ENCRYPTED = ChannelStateIdentifier.create(State.ENCRYPTED);
    public static final ChannelStateIdentifier FADE = ChannelStateIdentifier.create(State.FADE);
    public static final ChannelStateIdentifier IDLE = ChannelStateIdentifier.create(State.IDLE);
    public static final ChannelStateIdentifier TEARDOWN = ChannelStateIdentifier.create(State.TEARDOWN);
    public static final ChannelStateIdentifier RESET = ChannelStateIdentifier.create(State.RESET);

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
    public static ChannelStateIdentifier create(State state)
    {
        return new ChannelStateIdentifier(state);
    }

    @Override
    public String toString()
    {
        return getValue().getDisplayValue();
    }
}
