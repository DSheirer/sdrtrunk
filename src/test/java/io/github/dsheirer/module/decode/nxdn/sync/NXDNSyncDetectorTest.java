/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

package io.github.dsheirer.module.decode.nxdn.sync;

import io.github.dsheirer.module.decode.nxdn.layer1.sync.control.NXDNControlSoftSyncDetector;
import io.github.dsheirer.module.decode.nxdn.layer1.sync.control.NXDNControlSoftSyncDetectorScalar;
import io.github.dsheirer.module.decode.nxdn.layer1.sync.control.NXDNControlSoftSyncDetectorVector128;
import io.github.dsheirer.module.decode.nxdn.layer1.sync.control.NXDNControlSoftSyncDetectorVector256;
import io.github.dsheirer.module.decode.nxdn.layer1.sync.control.NXDNControlSoftSyncDetectorVector512;
import io.github.dsheirer.module.decode.nxdn.layer1.sync.control.NXDNControlSoftSyncDetectorVector64;
import io.github.dsheirer.module.decode.nxdn.layer1.sync.standard.NXDNStandardSoftSyncDetector;
import io.github.dsheirer.module.decode.nxdn.layer1.sync.standard.NXDNStandardSoftSyncDetectorScalar;
import io.github.dsheirer.module.decode.nxdn.layer1.sync.standard.NXDNStandardSoftSyncDetectorVector128;
import io.github.dsheirer.module.decode.nxdn.layer1.sync.standard.NXDNStandardSoftSyncDetectorVector256;
import io.github.dsheirer.module.decode.nxdn.layer1.sync.standard.NXDNStandardSoftSyncDetectorVector512;
import io.github.dsheirer.module.decode.nxdn.layer1.sync.standard.NXDNStandardSoftSyncDetectorVector64;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests for NXDN soft sync detector implementations for both the Standard 10-dibit length and the Control 22-dibit
 * length sync patterns.
 */
public class NXDNSyncDetectorTest
{
    private static final float[] STANDARD_SYMBOL_TEST_SEQUENCE = {-2.1f, 1.3f, -2.2f, 2.3f, -2.05f, -2.15f, 2.25f,
            2.35f, -1.4f, 2.45f};
    private static final float STANDARD_EXPECTED_TEST_SCORE = 44.178642f;
    private static final float[] CONTROL_SYMBOL_TEST_SEQUENCE = {1.0f, 2.0f, -1.1f, -2.1f, 1.0f, 2.0f, -1.1f, -2.1f,
            1.0f, 2.0f, -1.1f, -2.1f, -2.1f, 1.3f, -2.2f, 2.3f, -2.05f, -2.15f, 2.25f, 2.35f, -1.4f, 2.45f};
    private static final float CONTROL_EXPECTED_TEST_SCORE = 34.28263f;

    /**
     * Feeds the standard sequence of test soft symbols into the detector
     * @param detector under test
     */
    private void loadStandardSequence(NXDNStandardSoftSyncDetector detector)
    {
        for(float value : STANDARD_SYMBOL_TEST_SEQUENCE)
        {
            detector.process(value);
        }
    }

    /**
     * Feeds the control sequence of test soft symbols into the detector
     * @param detector under test
     */
    private void loadControlSequence(NXDNControlSoftSyncDetector detector)
    {
        for(float value : CONTROL_SYMBOL_TEST_SEQUENCE)
        {
            detector.process(value);
        }
    }

    @Test
    public void testControlScalar()
    {
        NXDNControlSoftSyncDetector detector = new NXDNControlSoftSyncDetectorScalar();
        loadControlSequence(detector);
        float score = detector.calculate();
        Assertions.assertTrue(Math.abs(CONTROL_EXPECTED_TEST_SCORE - score) < 0.00001);
    }

    @Test
    public void testControlVector64()
    {
        NXDNControlSoftSyncDetector detector = new NXDNControlSoftSyncDetectorVector64();
        loadControlSequence(detector);
        float score = detector.calculate();
        Assertions.assertTrue(Math.abs(CONTROL_EXPECTED_TEST_SCORE - score) < 0.00001);
    }

    @Test
    public void testControlVector128()
    {
        NXDNControlSoftSyncDetector detector = new NXDNControlSoftSyncDetectorVector128();
        loadControlSequence(detector);
        float score = detector.calculate();
        Assertions.assertTrue(Math.abs(CONTROL_EXPECTED_TEST_SCORE - score) < 0.00001);
    }

    @Test
    public void testControlVector256()
    {
        NXDNControlSoftSyncDetector detector = new NXDNControlSoftSyncDetectorVector256();
        loadControlSequence(detector);
        float score = detector.calculate();
        Assertions.assertTrue(Math.abs(CONTROL_EXPECTED_TEST_SCORE - score) < 0.00001);
    }

    @Test
    public void testControlVector512()
    {
        NXDNControlSoftSyncDetector detector = new NXDNControlSoftSyncDetectorVector512();
        loadControlSequence(detector);
        float score = detector.calculate();
        Assertions.assertTrue(Math.abs(CONTROL_EXPECTED_TEST_SCORE - score) < 0.00001);
    }

    @Test
    public void testStandardScalar()
    {
        NXDNStandardSoftSyncDetector detector = new NXDNStandardSoftSyncDetectorScalar();
        loadStandardSequence(detector);
        float score = detector.calculate();
        Assertions.assertTrue(Math.abs(STANDARD_EXPECTED_TEST_SCORE - score) < 0.00001);
    }

    @Test
    public void testStandardVector64()
    {
        NXDNStandardSoftSyncDetector detector = new NXDNStandardSoftSyncDetectorVector64();
        loadStandardSequence(detector);
        float score = detector.calculate();
        Assertions.assertTrue(Math.abs(STANDARD_EXPECTED_TEST_SCORE - score) < 0.00001);
    }

    @Test
    public void testStandardVector128()
    {
        NXDNStandardSoftSyncDetector detector = new NXDNStandardSoftSyncDetectorVector128();
        loadStandardSequence(detector);
        float score = detector.calculate();
        Assertions.assertTrue(Math.abs(STANDARD_EXPECTED_TEST_SCORE - score) < 0.00001);
    }

    @Test
    public void testStandardVector256()
    {
        NXDNStandardSoftSyncDetector detector = new NXDNStandardSoftSyncDetectorVector256();
        loadStandardSequence(detector);
        float score = detector.calculate();
        Assertions.assertTrue(Math.abs(STANDARD_EXPECTED_TEST_SCORE - score) < 0.00001);
    }

    @Test
    public void testStandardVector512()
    {
        NXDNStandardSoftSyncDetector detector = new NXDNStandardSoftSyncDetectorVector512();
        loadStandardSequence(detector);
        float score = detector.calculate();
        Assertions.assertTrue(Math.abs(STANDARD_EXPECTED_TEST_SCORE - score) < 0.00001);
    }
}
