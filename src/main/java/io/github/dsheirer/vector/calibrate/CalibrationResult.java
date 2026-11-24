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

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Measurements from a calibration run.
 * @param selected implementation
 * @param speedups throughput of each measured implementation relative to scalar, in measurement order
 * @param rejected implementations whose output didn't match scalar
 * @param timestamp of the calibration, in milliseconds since the epoch
 */
public record CalibrationResult(Implementation selected, Map<Implementation,Double> speedups,
                                List<Implementation> rejected, long timestamp)
{
    private static final String SELECTED = "selected";
    private static final String TIME = "time";
    private static final String SPEEDUP = "speedup";
    private static final String REJECTED = "rejected";

    public CalibrationResult
    {
        speedups = Collections.unmodifiableMap(new LinkedHashMap<>(speedups));
        rejected = List.copyOf(rejected);
    }

    /**
     * Throughput of the selected implementation relative to scalar.
     */
    public double getSelectedSpeedup()
    {
        return speedups.getOrDefault(selected, 1.0);
    }

    /**
     * Formats the result for storage.
     */
    public String format()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(SELECTED).append("=").append(selected.name());
        sb.append(";").append(TIME).append("=").append(timestamp);
        sb.append(";").append(SPEEDUP).append("=");
        List<String> speedupValues = new ArrayList<>();

        for(Map.Entry<Implementation,Double> entry : speedups.entrySet())
        {
            speedupValues.add(entry.getKey().name() + ":" + String.format(Locale.ROOT, "%.3f", entry.getValue()));
        }

        sb.append(String.join(",", speedupValues));
        sb.append(";").append(REJECTED).append("=");
        sb.append(String.join(",", rejected.stream().map(Implementation::name).toList()));
        return sb.toString();
    }

    /**
     * Parses a stored result.
     * @param value to parse
     * @return result, or empty if the value is missing or malformed
     */
    public static Optional<CalibrationResult> parse(String value)
    {
        if(value == null || value.isBlank())
        {
            return Optional.empty();
        }

        try
        {
            Implementation selected = null;
            long timestamp = 0;
            Map<Implementation,Double> speedups = new LinkedHashMap<>();
            List<Implementation> rejected = new ArrayList<>();

            for(String field : value.split(";"))
            {
                int separator = field.indexOf('=');

                if(separator < 0)
                {
                    continue;
                }

                String name = field.substring(0, separator);
                String content = field.substring(separator + 1);

                switch(name)
                {
                    case SELECTED -> selected = Implementation.valueOf(content);
                    case TIME -> timestamp = Long.parseLong(content);
                    case SPEEDUP ->
                    {
                        for(String speedup : content.split(","))
                        {
                            if(!speedup.isBlank())
                            {
                                String[] parts = speedup.split(":");
                                speedups.put(Implementation.valueOf(parts[0]), Double.parseDouble(parts[1]));
                            }
                        }
                    }
                    case REJECTED ->
                    {
                        for(String implementation : content.split(","))
                        {
                            if(!implementation.isBlank())
                            {
                                rejected.add(Implementation.valueOf(implementation));
                            }
                        }
                    }
                    default -> {}
                }
            }

            return selected != null ? Optional.of(new CalibrationResult(selected, speedups, rejected, timestamp)) :
                Optional.empty();
        }
        catch(RuntimeException e)
        {
            return Optional.empty();
        }
    }
}
