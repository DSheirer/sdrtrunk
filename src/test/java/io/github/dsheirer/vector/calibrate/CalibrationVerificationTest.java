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

package io.github.dsheirer.vector.calibrate;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

/**
 * Verifies every candidate implementation of every calibration against its scalar implementation, including vector
 * widths that this CPU emulates, without measuring or storing anything.
 */
public class CalibrationVerificationTest
{
    @TestFactory
    Stream<DynamicTest> candidatesMatchScalar()
    {
        return CalibrationManager.createCalibrations().stream().map(calibration -> DynamicTest.dynamicTest(calibration.getType().name(), () -> {
            List<Calibration.Verification> verifications = calibration.verify(true);
            String report = verifications.stream().map(Calibration.Verification::toString)
                .collect(Collectors.joining("\n\t", calibration.getType() + ":\n\t", ""));
            System.out.println(report);
            assertTrue(verifications.stream().allMatch(Calibration.Verification::passed), report);
        }));
    }
}
