/*
 * *****************************************************************************
 * Copyright (C) 2014-2021 Dennis Sheirer
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

package io.github.dsheirer.module.log;

import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.channel.IChannelDescriptor;
import io.github.dsheirer.identifier.IdentifierCollection;
import io.github.dsheirer.identifier.Role;
import io.github.dsheirer.identifier.configuration.AliasListConfigurationIdentifier;
import io.github.dsheirer.identifier.configuration.DecoderTypeConfigurationIdentifier;
import io.github.dsheirer.identifier.configuration.FrequencyConfigurationIdentifier;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.event.DecodeEvent;
import io.github.dsheirer.module.decode.p25.identifier.channel.APCO25Channel;
import io.github.dsheirer.module.decode.p25.identifier.talkgroup.APCO25Talkgroup;
import io.github.dsheirer.protocol.Protocol;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

class DecodeEventLoggerTest {
    IChannelDescriptor channelDescriptor = APCO25Channel.create(98765, 1);

    APCO25Talkgroup fromIdentifier = new APCO25Talkgroup(123, Role.FROM);
    APCO25Talkgroup toIdentifier = new APCO25Talkgroup(456, Role.TO);
    FrequencyConfigurationIdentifier freqIdentifier = new FrequencyConfigurationIdentifier(859562500L);
    DecoderTypeConfigurationIdentifier decoderIdentifier = new DecoderTypeConfigurationIdentifier(DecoderType.P25_PHASE1);
    AliasListConfigurationIdentifier aliasListIdentifier = new AliasListConfigurationIdentifier("My Alias List");
    AliasModel aliasModel = new AliasModel();

    Path logDirectory = Paths.get(System.getProperty("java.io.tmpdir"), "sdr_trunk_tests");

    @BeforeEach
    void setUp() {
        aliasModel.addAliasList(aliasListIdentifier.getValue());
    }

    @AfterEach
    void tearDown() {
    }

    IdentifierCollection buildIdentifierCollection() {
        return new IdentifierCollection(Arrays.asList(
                fromIdentifier,
                toIdentifier,
                decoderIdentifier,
                freqIdentifier,
                aliasListIdentifier
        ));
    }

    DecodeEvent.DecodeEventBuilder decodeEventBuilder() {
        return DecodeEvent.builder(1634428994000L)
                .channel(channelDescriptor)
                .identifiers(buildIdentifierCollection())
                .protocol(Protocol.APCO25)
                .eventDescription("DATA_PACKET")
                .timeslot(2)
                .details("Some details");
    }

//    @Test
//    void test_receive_writesToCsv() {
//        IDecodeEvent decodeEvent = decodeEventBuilder().build();
//
//        DecodeEventLogger decodeEventLogger = new DecodeEventLogger(aliasModel, logDirectory, "_foo.txt", 859562500);
//        DecodeEventLogger spy = spy(decodeEventLogger);
//
//        doNothing().when(spy).write(anyString());
//
//        spy.receive(decodeEvent);
//
//        String expectedToCsvString =
//                "\"2021:10:16:20:03:14\",\"111\",\"APCO-25\",\"DATA_PACKET\",\"123\",\" (456)\",\"98765-1\",\"859.562500\",\"TS:2\",\"Some details\"";
//        verify(spy).write(expectedToCsvString);
//    }

//    @Test
//    void test_receive_withQuotesInDetails_writesToCsv() {
//        IDecodeEvent decodeEvent = decodeEventBuilder()
//                .details("Some details now with \"quotes\"!")
//                .build();
//
//        DecodeEventLogger decodeEventLogger = new DecodeEventLogger(aliasModel, logDirectory, "_foo.txt", 859562500);
//        DecodeEventLogger spy = spy(decodeEventLogger);
//
//        doNothing().when(spy).write(anyString());
//
//        spy.receive(decodeEvent);
//
//        String expectedToCsvString =
//                "\"2021:10:16:20:03:14\",\"111\",\"APCO-25\",\"DATA_PACKET\",\"123\",\" (456)\",\"98765-1\",\"859.562500\",\"TS:2\",\"Some details now with \"\"quotes\"\"!\"";
//        verify(spy).write(expectedToCsvString);
//    }

//    @Test
//    void test_receive_withCommasInDetails_writesToCsv() {
//        IDecodeEvent decodeEvent = decodeEventBuilder()
//                .details("Some details now with, to an extent, commas!")
//                .build();
//
//        DecodeEventLogger decodeEventLogger = new DecodeEventLogger(aliasModel, logDirectory, "_foo.txt", 859562500);
//        DecodeEventLogger spy = spy(decodeEventLogger);
//
//        doNothing().when(spy).write(anyString());
//
//        spy.receive(decodeEvent);
//
//        String expectedToCsvString =
//                "\"2021:10:16:20:03:14\",\"111\",\"APCO-25\",\"DATA_PACKET\",\"123\",\" (456)\",\"98765-1\",\"859.562500\",\"TS:2\",\"Some details now with, to an extent, commas!\"";
//        verify(spy).write(expectedToCsvString);
//    }
}
