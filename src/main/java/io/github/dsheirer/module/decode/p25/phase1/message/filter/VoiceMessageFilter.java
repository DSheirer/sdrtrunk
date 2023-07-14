/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.module.decode.p25.phase1.message.filter;

import io.github.dsheirer.filter.Filter;
import io.github.dsheirer.filter.FilterElement;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.p25.phase1.message.ldu.LDU1Message;
import io.github.dsheirer.module.decode.p25.phase1.message.ldu.LDU2Message;
import io.github.dsheirer.module.decode.p25.phase1.message.ldu.LDUMessage;
import java.util.function.Function;

/**
 * Filter for LDU1 and LDU2 voice messages
 */
public class VoiceMessageFilter extends Filter<IMessage,String>
{
    private static final String LDU1_KEY = "LDU1 Voice Message";
    private static final String LDU2_KEY = "LDU2 Voice Message";
    private KeyExtractor mKeyExtractor = new KeyExtractor();

    /**
     * Constructs an instance
     *
     * @param name of this filter
     */
    public VoiceMessageFilter()
    {
        super("Voice Messages");
        add(new FilterElement<>(LDU1_KEY));
        add(new FilterElement<>(LDU2_KEY));
    }

    @Override
    public Function<IMessage, String> getKeyExtractor()
    {
        return mKeyExtractor;
    }

    @Override
    public boolean canProcess(IMessage message)
    {
        return message instanceof LDUMessage && super.canProcess(message);
    }

    /**
     * Key extractor
     */
    private class KeyExtractor implements Function<IMessage,String>
    {
        @Override
        public String apply(IMessage message)
        {
            if(message instanceof LDU1Message)
            {
                return LDU1_KEY;
            }
            else if(message instanceof LDU2Message)
            {
                return LDU2_KEY;
            }

            return null;
        }
    }
}
