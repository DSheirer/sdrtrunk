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

package io.github.dsheirer.dsp.squelch;

/**
 * Noise Squelch operating status record.  Captures a snapshot in time of the squelch status.
 * @param squelch state, on (true) or off (false)
 * @param squelchOverride on (true) or off (false)
 * @param noise - current noise variance
 * @param noiseOpenThreshold for determining un-squelch state
 * @param noiseCloseThreshold for determining squelch state
 * @param hysteresis in units of 10-milliseconds
 * @param hysteresisOpenThreshold for toggling the squelch state.
 * @param hysteresisCloseThreshold for toggling the squelch state.
 */
public record NoiseSquelchState(boolean squelch, boolean squelchOverride, float noise, float noiseOpenThreshold,
                                float noiseCloseThreshold, int hysteresis, int hysteresisOpenThreshold,
                                int hysteresisCloseThreshold){}
