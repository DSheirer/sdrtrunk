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

package io.github.dsheirer.module.decode.nxdn.layer1;

import io.github.dsheirer.dsp.symbol.Dibit;
import java.util.Arrays;

/**
 * Equalizes Inter-Symbol Interference (ISI) error in (C4FM/QPSK) dibit symbol sequences.
 *
 * The SYMBOL_LENGTH parameter determines the length of the tracked symbol sequence.  We track the error in the next
 * to last symbol recognizing that both preceding and following symbols contribute to the error in the tracked symbol.
 *
 * For each arriving symbol, we make a hard-symbol decision and then use the bit pair for that symbol decision to track
 * the sequence of bits over the symbol sequence and use the integer value of that bit sequence to select the tracked
 * error index in the mTrackedError array.  Recognizing that an errant soft symbol value could produce a sequence
 * error if it's mapped into the incorrect quadrant, we use min/max limits on how much impact an error like this can
 * have.  However, even when a symbol decision falls into an incorrect quadrant, the neighboring symbol sequence is
 * likely to have a similar tracked error and so the overall effect would seem to be minimal.  In practice, this
 * approach seems to be effective in muting the inherent ISI induced from the RRC pulse shaping filter and the channel.
 *
 * At four tracked symbol length, we maintain an array of 256x tracked float values (1K memory) and we incur a minimal
 * CPU cost per-symbol for correcting the error and tracking out the residual error.
 */
public class SymbolEqualizerC4FM
{
    private static final float ERROR_LOOP_GAIN = 0.25f;
    private static final float ERROR_MAXIMUM = 0.8f;
    private static final float ERROR_MINIMUM = -ERROR_MAXIMUM;
    private static final float RESIDUAL_ERROR_MAXIMUM = 0.2f;
    private static final float RESIDUAL_ERROR_MINIMUM = -RESIDUAL_ERROR_MAXIMUM;
    private static final int SYMBOL_SEQUENCE_LENGTH = 4;
    private static final int TRACKED_SEQUENCE_COUNT = (int)Math.pow(4, SYMBOL_SEQUENCE_LENGTH);
    private static final int SYMBOL_SEQUENCE_MASK = (TRACKED_SEQUENCE_COUNT - 1) & 0x7FFFFFFC;
    private final float[] mTrackedError = new float[TRACKED_SEQUENCE_COUNT];
    private Dibit mDelayedDecision = Dibit.D00_PLUS_1;
    private float mDelayedSymbol = Dibit.D00_PLUS_1.getIdealPhase();
    private boolean mEnabled = false;
    private int mSymbolSequence = 0;

    /**
     * Constructs an instance and prefills the tracker map with every 3-dibit sequence pattern.
     */
    public SymbolEqualizerC4FM()
    {
    }

    /**
     * Enables the equalizer.  Disable the equalizer until you have a confirmed sync detection and then
     * enable until you have a sync loss so that you don't contaminate the symbol sequence trackers with bad data.
     */
    public void enable()
    {
        mEnabled = true;
    }

    /**
     * Disables the equalizer.
     */
    public void disable()
    {
        mEnabled = false;
    }

    /**
     * Processes the soft symbol and corresponding symbol decision and return a corrected, 1-symbol delayed soft
     * symbol value.
     * @param softSymbol for the current symbol period
     * @param decision from the soft wymbol
     * @return delayed (1-period) and corrected soft symbol value
     */
    public float process(float softSymbol, Dibit decision)
    {
        if(mEnabled)
        {
            mSymbolSequence <<= 2;
            mSymbolSequence &= SYMBOL_SEQUENCE_MASK; //Mask the oldest symbol and clear for the newly arriving symbol
            mSymbolSequence += decision.getValue(); //Add newly arrived symbol dibit bit value
            float error = mTrackedError[mSymbolSequence];
            float processed = mDelayedSymbol - error;
            float residualError = mDelayedDecision.getIdealPhase() - processed;
            residualError = Math.clamp(residualError, RESIDUAL_ERROR_MINIMUM, RESIDUAL_ERROR_MAXIMUM);
            error -= (residualError * ERROR_LOOP_GAIN);
            error = Math.clamp(error, ERROR_MINIMUM, ERROR_MAXIMUM);
            mTrackedError[mSymbolSequence] = error;
            mDelayedSymbol = softSymbol;
            mDelayedDecision = decision;
            return processed;
        }
        else
        {
            float passThrough = mDelayedSymbol;
            mDelayedSymbol = softSymbol;
            return passThrough;
        }
    }

    /**
     * Resets all trackers
     */
    public void reset()
    {
        Arrays.fill(mTrackedError, 0f);
        disable();
    }
}
