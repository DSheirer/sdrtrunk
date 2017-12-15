/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014 Dennis Sheirer
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package dsp.fsk;

import buffer.BooleanAveragingBuffer;
import dsp.symbol.SymbolEvent;
import dsp.symbol.SymbolEvent.Shift;
import instrument.Instrumentable;
import instrument.tap.Tap;
import instrument.tap.TapGroup;
import instrument.tap.stream.SymbolEventTap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sample.Listener;
import sample.real.RealBuffer;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * Binary Frequency Shift Keying (FSK) decoder.  Implements a BFSK correlation
 * decoder.  Provides normal or inverted decoded output.  Requires the symbol
 * rate to be an integer multiple of the sample rate.
 *
 * Implements a correlation decoder that correlates a modulated FSK signal upon
 * against a one-baud delayed version of itself, converting all float samples to
 * binary (0 or 1), and uses XOR to produce the correlation value.  Low pass
 * filtering smoothes the correlated output.
 *
 * Automatically aligns to symbol timing during symbol transitions (0 to 1, or
 * 1 to 0) by inspecting the samples at the symbol edges and advancing or
 * retarding symbol window to maintain continuous symbol alignment.
 *
 * Use a DC-removal filter prior to this decoder to ensure samples don't have
 * a DC component.
 *
 * Implements instrumentable interface, so that slice events can be received
 * externally to analyze decoder performance.
 */
public class FSK2Decoder implements Instrumentable, Listener<RealBuffer>
{
    private final static Logger mLog = LoggerFactory.getLogger(FSK2Decoder.class);

    public enum Output
    {
        NORMAL, INVERTED
    }

    /* Instrumentation taps */
    private static final String INSTRUMENT_DECISION =
        "Tap Point: FSK2 Symbol Decision";
    private List<TapGroup> mAvailableTaps;
    private ArrayList<SymbolEventTap> mTaps = new ArrayList<SymbolEventTap>();

    private Listener<Boolean> mListener;
    private BooleanAveragingBuffer mDelayBuffer;
    private BooleanAveragingBuffer mLowPassFilter;
    private Slicer mSlicer;
    private boolean mNormalOutput;
    private int mSamplesPerSymbol;
    private int mSymbolRate;

    public FSK2Decoder(int sampleRate, int symbolRate, Output output)
    {
        /* Ensure we're using an integral symbolRate of the sampleRate */
        assert (sampleRate % symbolRate == 0);

        mSamplesPerSymbol = (int) (sampleRate / symbolRate);
        mNormalOutput = (output == Output.NORMAL);
        mSymbolRate = symbolRate;

        mDelayBuffer = new BooleanAveragingBuffer(mSamplesPerSymbol);
        mLowPassFilter = new BooleanAveragingBuffer(mSamplesPerSymbol);
        mSlicer = new Slicer(mSamplesPerSymbol);
    }

    /**
     * Disposes of all references to prepare for garbage collection
     */
    public void dispose()
    {
        mListener = null;
    }

    /**
     * Primary sample input
     */
    @Override
    public void receive(RealBuffer buffer)
    {
        for(float sample : buffer.getSamples())
        {
			/* Square the sample.  Greater than zero is a 1 (true) and less than 
			 * zero is a 0 (false) */
            boolean bitSample = (sample >= 0.0f);

			/* Feed the delay buffer and fetch the one-baud delayed sample */
            boolean delayedBitSample = mDelayBuffer.get(bitSample);

			/* Correlation: xor current bit with delayed bit */
            boolean softBit = bitSample ^ delayedBitSample;

			/* Low pass filter to smooth the correlated values */
            boolean filteredSoftBit = mLowPassFilter.getAverage(softBit);

			/* Send the filtered correlated bit to the slicer */
            mSlicer.receive(filteredSoftBit);
        }
    }

    /**
     * Registers a listener to receive the decoded FSK bits
     */
    public void setListener(Listener<Boolean> listener)
    {
        mListener = listener;
    }

    /**
     * Removes the listener
     */
    public void removeListener(Listener<Boolean> listener)
    {
        mListener = null;
    }

    /**
     * Symbol slicer with auto-aligning baud timing
     */
    public class Slicer
    {
        private BitSet mBitSet = new BitSet();
        private int mSymbolLength;
        private int mDecisionThreshold;
        private int mSampleCounter;

        public Slicer(int samplesPerSymbol)
        {
            mSymbolLength = samplesPerSymbol;

            mDecisionThreshold = (int) (mSymbolLength / 2);

			/* Adjust for an odd number of samples per baud */
            if(mSymbolLength % 2 == 1)
            {
                mDecisionThreshold++;
            }
        }

        public void receive(boolean softBit)
        {
            if(mSampleCounter >= 0)
            {
                if(softBit)
                {
                    mBitSet.set(mSampleCounter);
                }
                else
                {
                    mBitSet.clear(mSampleCounter);
                }
            }

            mSampleCounter++;

            if(mSampleCounter >= mSymbolLength)
            {
                boolean decision = mBitSet.cardinality() >= mDecisionThreshold;

                send(decision);

				/* Shift timing left if the left bit in the bitset is opposite 
				 * the decision and the right bit is the same */
                if((mBitSet.get(0) ^ decision) &&
                    (!(mBitSet.get(mSymbolLength - 1) ^ decision)))
                {
                    sendTapEvent(mBitSet, Shift.LEFT, decision);

                    reset();

                    mSampleCounter--;
                }
				/* Shift timing right if the left bit is the same as the 
				 * decision and the right bit is opposite */
                else if((!(mBitSet.get(0) ^ decision)) &&
                    (mBitSet.get(mSymbolLength - 1) ^ decision))
                {
                    sendTapEvent(mBitSet, Shift.RIGHT, decision);
					
					/* Last bit from previous symbol to pre-fill next symbol */
                    boolean previousSoftBit = mBitSet.get(mSymbolLength - 1);

                    reset();

                    if(previousSoftBit)
                    {
                        mBitSet.set(0);
                    }

                    mSampleCounter++;
                }
				/* No shift */
                else
                {
                    sendTapEvent(mBitSet, Shift.NONE, decision);

                    reset();
                }
            }
        }

        /**
         * Sends the bit decision to the listener
         */
        private void send(boolean decision)
        {
            if(mListener != null)
            {
                mListener.receive(mNormalOutput ? decision : !decision);
            }
        }

        private void reset()
        {
            mBitSet.clear();
            mSampleCounter = 0;
        }

        /**
         * Sends instrumentation tap event to all registered listeners
         */
        private void sendTapEvent(BitSet bitset, Shift shift, boolean decision)
        {
            for(SymbolEventTap tap : mTaps)
            {
                SymbolEvent event =
                    new SymbolEvent(bitset.get(0, mSymbolLength),
                        mSymbolLength,
                        decision,
                        shift);

                tap.receive(event);
            }
        }
    }

    /**
     * Get instrumentation taps
     */
    @Override
    public List<TapGroup> getTapGroups()
    {
        if(mAvailableTaps == null)
        {
            mAvailableTaps = new ArrayList<TapGroup>();

            TapGroup tapGroup = new TapGroup("FSK2 Decoder");
            tapGroup.add(new SymbolEventTap(INSTRUMENT_DECISION, 0, .025f));

            mAvailableTaps.add(tapGroup);
        }

        return mAvailableTaps;
    }

    /**
     * Add instrumentation tap
     */
    @Override
    public void registerTap(Tap tap)
    {
        if(tap instanceof SymbolEventTap && !mTaps.contains(tap))
        {
            mTaps.add((SymbolEventTap) tap);
        }
    }

    /**
     * Remove instrumentation tap
     */
    @Override
    public void unregisterTap(Tap tap)
    {
        mTaps.remove(tap);
    }
}
