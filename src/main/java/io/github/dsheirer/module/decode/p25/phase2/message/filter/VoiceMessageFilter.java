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

package io.github.dsheirer.module.decode.p25.phase2.message.filter;

import io.github.dsheirer.filter.Filter;
import io.github.dsheirer.filter.FilterElement;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.p25.phase2.enumeration.DataUnitID;
import io.github.dsheirer.module.decode.p25.phase2.timeslot.AbstractVoiceTimeslot;
import java.util.function.Function;

/**
 * Filter for P25 Phase 2 Voice (2/4) messages
 */
public class VoiceMessageFilter extends Filter<IMessage, DataUnitID>
{
    private KeyExtractor mKeyExtractor = new KeyExtractor();

    /**
     * Constructs an instance
     *
     * @param name of this filter
     */
    public VoiceMessageFilter()
    {
        super("Voice Timeslot Messages");
        add(new FilterElement<>(DataUnitID.VOICE_2));
        add(new FilterElement<>(DataUnitID.VOICE_4));
    }

    @Override
    public Function<IMessage, DataUnitID> getKeyExtractor()
    {
        return mKeyExtractor;
    }

    @Override
    public boolean canProcess(IMessage message)
    {
        return message instanceof AbstractVoiceTimeslot && super.canProcess(message);
    }

    /**
     * Key extractor
     */
    private class KeyExtractor implements Function<IMessage,DataUnitID>
    {
        @Override
        public DataUnitID apply(IMessage message)
        {
            if(message instanceof AbstractVoiceTimeslot voice)
            {
                return voice.getDataUnitID();
            }

            return null;
        }
    }
}
