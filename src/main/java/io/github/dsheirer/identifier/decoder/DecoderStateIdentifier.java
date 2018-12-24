/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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

public class DecoderStateIdentifier extends Identifier<State>
{
    public static final DecoderStateIdentifier ACTIVE = DecoderStateIdentifier.create(State.ACTIVE);
    public static final DecoderStateIdentifier CALL = DecoderStateIdentifier.create(State.CALL);
    public static final DecoderStateIdentifier CONTROL = DecoderStateIdentifier.create(State.CONTROL);
    public static final DecoderStateIdentifier DATA = DecoderStateIdentifier.create(State.DATA);
    public static final DecoderStateIdentifier ENCRYPTED = DecoderStateIdentifier.create(State.ENCRYPTED);
    public static final DecoderStateIdentifier FADE = DecoderStateIdentifier.create(State.FADE);
    public static final DecoderStateIdentifier IDLE = DecoderStateIdentifier.create(State.IDLE);
    public static final DecoderStateIdentifier TEARDOWN = DecoderStateIdentifier.create(State.TEARDOWN);
    public static final DecoderStateIdentifier RESET = DecoderStateIdentifier.create(State.RESET);

    public DecoderStateIdentifier(State value)
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
    public static DecoderStateIdentifier create(State state)
    {
        return new DecoderStateIdentifier(state);
    }

    @Override
    public String toString()
    {
        return getValue().getDisplayValue();
    }
}
