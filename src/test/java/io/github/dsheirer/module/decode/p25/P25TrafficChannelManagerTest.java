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

package io.github.dsheirer.module.decode.p25;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.module.decode.config.DecodeConfigJMBE;
import io.github.dsheirer.module.decode.p25.phase1.DecodeConfigP25Phase1;
import io.github.dsheirer.module.decode.p25.phase2.DecodeConfigP25Phase2;
import java.lang.reflect.Field;
import java.util.List;
import org.junit.jupiter.api.Test;

class P25TrafficChannelManagerTest
{
    @Test
    void phase2TrafficChannelsInheritPhase1ParentJmbeAudioSettings() throws Exception
    {
        DecodeConfigP25Phase1 parentConfiguration = configured(new DecodeConfigP25Phase1());
        P25TrafficChannelManager manager = createManager(parentConfiguration);
        DecodeConfigJMBE trafficConfiguration = assertInstanceOf(DecodeConfigP25Phase2.class,
                firstManagedChannel(manager, "mManagedPhase2TrafficChannels").getDecodeConfiguration());

        assertAudioSettings(trafficConfiguration);
    }

    @Test
    void phase1TrafficChannelsInheritPhase2ParentJmbeAudioSettings() throws Exception
    {
        DecodeConfigP25Phase2 parentConfiguration = configured(new DecodeConfigP25Phase2());
        P25TrafficChannelManager manager = createManager(parentConfiguration);
        DecodeConfigJMBE trafficConfiguration = assertInstanceOf(DecodeConfigP25Phase1.class,
                firstManagedChannel(manager, "mManagedPhase1TrafficChannels").getDecodeConfiguration());

        assertAudioSettings(trafficConfiguration);
    }

    private static <T extends DecodeConfigJMBE> T configured(T configuration)
    {
        configuration.setAGC(false);
        configuration.setToneGain(0.25f);
        configuration.setNoiseGain(0.4f);

        if(configuration instanceof DecodeConfigP25Phase1 phase1)
        {
            phase1.setTrafficChannelPoolSize(1);
        }
        else if(configuration instanceof DecodeConfigP25Phase2 phase2)
        {
            phase2.setTrafficChannelPoolSize(1);
        }

        return configuration;
    }

    private static P25TrafficChannelManager createManager(DecodeConfigJMBE configuration)
    {
        Channel parent = new Channel("P25 Test", Channel.ChannelType.STANDARD);
        parent.setDecodeConfiguration(configuration);
        return new P25TrafficChannelManager(parent);
    }

    @SuppressWarnings("unchecked")
    private static Channel firstManagedChannel(P25TrafficChannelManager manager, String fieldName) throws Exception
    {
        Field field = P25TrafficChannelManager.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return ((List<Channel>)field.get(manager)).get(0);
    }

    private static void assertAudioSettings(DecodeConfigJMBE configuration)
    {
        assertFalse(configuration.isAGC());
        assertEquals(0.25f, configuration.getToneGain());
        assertEquals(0.4f, configuration.getNoiseGain());
    }
}
