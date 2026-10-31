/*
 * *****************************************************************************
 * Copyright (C) 2026 Jared Szechy
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

package io.github.dsheirer.source.tuner.network;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import io.github.dsheirer.source.tuner.ITunerErrorListener;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for the rtl_tcp wire protocol helpers in {@link NetworkTunerController}: command
 * encoding and handshake header validation.
 */
public class NetworkTunerControllerTest {
    @Test
    public void testEncodeCommandProducesFiveByteBigEndianPacket() {
        //Command 0x01 (set frequency) with parameter 100_000_000 (100 MHz)
        byte[] expected = ByteBuffer.allocate(5).put((byte) 0x01).putInt(100_000_000).array();
        byte[] actual = NetworkTunerController.encodeCommand(0x01, 100_000_000);
        assertEquals(5, actual.length);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void testEncodeCommandNegativeParameterEncodesAsUnsignedBigEndian() {
        //rtl_tcp treats the 4-byte parameter as an unsigned 32-bit value - verify negative Java
        //ints (used to represent values > Integer.MAX_VALUE) round-trip through the same bytes
        //as ByteBuffer.putInt() would produce.
        byte[] expected = ByteBuffer.allocate(5).put((byte) 0x02).putInt(-1).array();
        byte[] actual = NetworkTunerController.encodeCommand(0x02, -1);
        assertArrayEquals(expected, actual);
    }

    @Test
    public void testReadAndValidateHandshakeAcceptsValidMagic() throws IOException {
        byte[] header = new byte[]{'R', 'T', 'L', '0', 0, 0, 0, 0, 0, 0, 0, 1};
        NetworkTunerController controller = createController();
        //Should not throw
        controller.readAndValidateHandshake(new ByteArrayInputStream(header));
    }

    @Test
    public void testReadAndValidateHandshakeRejectsInvalidMagic() {
        byte[] header = new byte[]{'X', 'X', 'X', 'X', 0, 0, 0, 0, 0, 0, 0, 1};
        NetworkTunerController controller = createController();
        InputStream in = new ByteArrayInputStream(header);
        assertThrows(IOException.class, () -> controller.readAndValidateHandshake(in));
    }

    @Test
    public void testReadAndValidateHandshakeRejectsTruncatedHeader() {
        byte[] header = new byte[]{'R', 'T', 'L', '0'};
        NetworkTunerController controller = createController();
        InputStream in = new ByteArrayInputStream(header);
        assertThrows(IOException.class, () -> controller.readAndValidateHandshake(in));
    }

    private static NetworkTunerController createController() {
        NetworkTunerConfiguration config = NetworkTunerConfiguration.create();
        config.setHost("localhost");
        config.setPort((short) 1234);
        return new NetworkTunerController(new ITunerErrorListener() {
            @Override
            public void setErrorMessage(String errorMessage) {
            }

            @Override
            public void tunerRemoved() {
            }
        }, config);
    }
}
