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

package io.github.dsheirer.module.decode.dmr.message.filter;

import io.github.dsheirer.filter.Filter;
import io.github.dsheirer.filter.FilterElement;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.module.decode.dmr.sync.DMRSyncPattern;
import io.github.dsheirer.module.decode.dmr.message.voice.VoiceMessage;
import java.util.function.Function;

/**
 * Filter for voice messages
 */
public class VoiceMessageFilter extends Filter<IMessage, DMRSyncPattern>
{
    private KeyExtractor mKeyExtractor = new KeyExtractor();

    /**
     * Constructs an instance
     */
    public VoiceMessageFilter()
    {
        super("Voice Messages");
        add(new FilterElement<>(DMRSyncPattern.BASE_STATION_VOICE));
        add(new FilterElement<>(DMRSyncPattern.BS_VOICE_FRAME_B));
        add(new FilterElement<>(DMRSyncPattern.BS_VOICE_FRAME_C));
        add(new FilterElement<>(DMRSyncPattern.BS_VOICE_FRAME_D));
        add(new FilterElement<>(DMRSyncPattern.BS_VOICE_FRAME_E));
        add(new FilterElement<>(DMRSyncPattern.BS_VOICE_FRAME_F));
        add(new FilterElement<>(DMRSyncPattern.MOBILE_STATION_VOICE));
        add(new FilterElement<>(DMRSyncPattern.MS_VOICE_FRAME_B));
        add(new FilterElement<>(DMRSyncPattern.MS_VOICE_FRAME_C));
        add(new FilterElement<>(DMRSyncPattern.MS_VOICE_FRAME_D));
        add(new FilterElement<>(DMRSyncPattern.MS_VOICE_FRAME_E));
        add(new FilterElement<>(DMRSyncPattern.MS_VOICE_FRAME_F));
        add(new FilterElement<>(DMRSyncPattern.DIRECT_VOICE_TIMESLOT_1));
        add(new FilterElement<>(DMRSyncPattern.DIRECT_VOICE_TIMESLOT_2));
    }

    @Override
    public boolean canProcess(IMessage message)
    {
        return message instanceof VoiceMessage && super.canProcess(message);
    }

    @Override
    public Function<IMessage,DMRSyncPattern> getKeyExtractor()
    {
        return mKeyExtractor;
    }

    /**
     * Key extractor
     */
    private class KeyExtractor implements Function<IMessage,DMRSyncPattern>
    {
        @Override
        public DMRSyncPattern apply(IMessage message)
        {
            if(message instanceof VoiceMessage voice)
            {
                return voice.getSyncPattern();
            }

            return null;
        }
    }
}
