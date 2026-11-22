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

package io.github.dsheirer.module.decode.p25.phase1;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * JUnit tests for the P25 Phase 1 LSM symbol processor.
 */
public class P25P1SymbolProcessorLSMTest
{
    private static final float SAMPLES_PER_SYMBOL = 5.0f;
    private static final double TOLERANCE = 1e-5;

    private static P25P1SymbolProcessorLSM getProcessor()
    {
        P25P1SymbolProcessorLSM processor = new P25P1SymbolProcessorLSM(new P25P1DecoderLSM(), new P25P1MessageFramer());
        processor.setSamplesPerSymbol(SAMPLES_PER_SYMBOL);
        return processor;
    }

    /**
     * Converts radians per symbol to the radians per sample domain of the demodulated sample stream.
     */
    private static float toSample(double radiansPerSymbol)
    {
        return (float)(radiansPerSymbol / SAMPLES_PER_SYMBOL);
    }

    /**
     * Test: samples that don't straddle the phase wrap are linearly interpolated.
     */
    @Test
    void testLinearInterpolation()
    {
        P25P1SymbolProcessorLSM processor = getProcessor();
        assertEquals(toSample(1.5), processor.interpolate(toSample(1.0), toSample(2.0), 0.5), TOLERANCE);
        assertEquals(toSample(-1.25), processor.interpolate(toSample(-1.0), toSample(-2.0), 0.25), TOLERANCE);
    }

    /**
     * Test: samples that straddle the phase wrap near an outer symbol interpolate across the wrap instead of through
     * zero, and the result is wrapped to +/- PI radians per symbol.
     */
    @Test
    void testInterpolationAcrossPhaseWrap()
    {
        P25P1SymbolProcessorLSM processor = getProcessor();

        //+3.0 and -3.0 are 0.283 radians apart across the wrap at +/- PI.
        assertEquals(toSample(Math.PI), Math.abs(processor.interpolate(toSample(3.0), toSample(-3.0), 0.5)), TOLERANCE);

        //A quarter of the way from +3.0 toward -3.0 (across the wrap) is still on the positive side of the wrap.
        double expected = 3.0 + 0.25 * (2.0 * Math.PI - 6.0);
        assertEquals(toSample(expected), processor.interpolate(toSample(3.0), toSample(-3.0), 0.25), TOLERANCE);

        //Three quarters of the way crosses the wrap to the negative side.
        expected = 3.0 + 0.75 * (2.0 * Math.PI - 6.0) - 2.0 * Math.PI;
        assertEquals(toSample(expected), processor.interpolate(toSample(3.0), toSample(-3.0), 0.75), TOLERANCE);
    }
}
