/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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

import io.github.dsheirer.identifier.Identifier;
import io.github.dsheirer.message.IMessage;
import io.github.dsheirer.protocol.Protocol;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Verifies that message log files preserve millisecond timestamps, validity labels, and message text.
 */
class MessageEventLoggerTest
{
    @Test
    void logsMillisecondTimestampsForPassedAndFailedMessages(@TempDir Path logDirectory) throws IOException
    {
        long timestamp = Instant.parse("2026-10-15T12:34:56.007Z").toEpochMilli();
        List<String> lines = logMessages(logDirectory, MessageEventLogger.Type.DECODED,
                new TestMessage(timestamp, true, "Valid message"),
                new TestMessage(timestamp + 124, false, "Invalid message"));

        assertEquals(List.of("DECODED Message Logger", "",
                formatTimestamp(timestamp) + ",PASSED,Valid message",
                formatTimestamp(timestamp + 124) + ",FAILED,Invalid message"), lines);
    }

    @Test
    void logsTimestampsAcrossTheUnixEpoch(@TempDir Path logDirectory) throws IOException
    {
        List<String> lines = logMessages(logDirectory, MessageEventLogger.Type.BINARY,
                new TestMessage(-1, true, "Before epoch"),
                new TestMessage(0, true, "At epoch"),
                new TestMessage(1, true, "After epoch"));

        assertEquals(List.of("BINARY Message Logger", "",
                formatTimestamp(-1) + ",PASSED,Before epoch",
                formatTimestamp(0) + ",PASSED,At epoch",
                formatTimestamp(1) + ",PASSED,After epoch"), lines);
    }

    @Test
    void logsMinimumAndMaximumTimestamps(@TempDir Path logDirectory) throws IOException
    {
        List<String> lines = logMessages(logDirectory, MessageEventLogger.Type.BINARY,
                new TestMessage(Long.MIN_VALUE, true, "Minimum timestamp"),
                new TestMessage(Long.MAX_VALUE, false, "Maximum timestamp"));

        assertEquals(List.of("BINARY Message Logger", "",
                formatTimestamp(Long.MIN_VALUE) + ",PASSED,Minimum timestamp",
                formatTimestamp(Long.MAX_VALUE) + ",FAILED,Maximum timestamp"), lines);
    }

    @Test
    void preservesEmptyAndMultilineMessageText(@TempDir Path logDirectory) throws IOException
    {
        long timestamp = Instant.parse("2026-10-15T12:34:56.999Z").toEpochMilli();
        List<String> lines = logMessages(logDirectory, MessageEventLogger.Type.DECODED,
                new TestMessage(timestamp, false, ""),
                new TestMessage(timestamp + 1, true, "  alpha,beta\nsecond line  "));

        assertEquals(List.of("DECODED Message Logger", "",
                formatTimestamp(timestamp) + ",FAILED,",
                formatTimestamp(timestamp + 1) + ",PASSED,  alpha,beta",
                "second line  "), lines);
    }

    private static String formatTimestamp(long timestamp)
    {
        return new SimpleDateFormat("yyyyMMdd HHmmss.SSS").format(new Date(timestamp));
    }

    private static List<String> logMessages(Path logDirectory, MessageEventLogger.Type type, IMessage... messages)
            throws IOException
    {
        MessageEventLogger logger = new MessageEventLogger(logDirectory, "messages.log", type, 851_000_000);
        logger.start();
        try
        {
            for(IMessage message: messages)
            {
                logger.getMessageListener().receive(message);
            }
        }
        finally
        {
            logger.stop();
        }

        return Files.readAllLines(Path.of(logger.toString()));
    }

    private record TestMessage(long getTimestamp, boolean isValid, String text) implements IMessage
    {
        @Override
        public Protocol getProtocol()
        {
            return Protocol.UNKNOWN;
        }

        @Override
        public int getTimeslot()
        {
            return 0;
        }

        @Override
        public List<Identifier> getIdentifiers()
        {
            return List.of();
        }

        @Override
        public @NonNull String toString()
        {
            return text;
        }
    }
}
