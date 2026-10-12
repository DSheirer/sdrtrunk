package io.github.dsheirer.bits;

import io.github.dsheirer.dsp.symbol.Dibit;
import io.github.dsheirer.module.decode.nxdn.layer1.sync.ReferenceSyncWaveform;
import java.util.Map;
import java.util.TreeMap;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

/**
 * Base sync detector for C4FM/QPSK dibit implementations.
 */
public class DibitSyncDetector
{
    private final long mPattern;
    private final long mMask;
    private final int mDibitLength;
    private Dibit[] mDibits;
    private float[] mSymbols;
    private final Map<Integer,ReferenceSyncWaveform> mReferenceWaveformMap = new TreeMap<>();

    /*
     * Constructs an instance.
     * @param pattern for the sync
     * @param dibitLength of the sync pattern
     */
    public DibitSyncDetector(long pattern, int dibitLength)
    {
        mPattern = pattern;
        mMask = (long)(Math.pow(2, dibitLength * 2) - 1);
        mDibitLength = dibitLength;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("SYNC: ").append(Long.toHexString(mPattern).toUpperCase());
        sb.append(" MASK: ").append(Long.toHexString(mMask).toUpperCase());
        sb.append(" LENGTH: ").append(mDibitLength);
        return sb.toString();
    }

    /**
     * Sync pattern
     * @return pattern
     */
    public long getPattern()
    {
        return mPattern;
    }

    /**
     * Sync mask
     */
    public long getMask()
    {
        return mMask;
    }

    /**
     * Pattern length in dibits
     */
    public int getDibitLength()
    {
        return mDibitLength;
    }

    /**
     * Sync pattern as an array of dibits.
     * @return dibit array.
     */
    public Dibit[] getDibits()
    {
        if(mDibits == null)
        {
            mDibits = toDibits(mPattern, mDibitLength);
        }

        return mDibits;
    }

    /**
     * Sync pattern as an array of soft symbols
     * @return symbols array in radians.
     */
    public float[] getSymbols()
    {
        if(mSymbols == null)
        {
            mSymbols = toSymbols(getDibits());
        }

        return mSymbols;
    }

    /**
     * Converts the sync pattern to dibits.
     * @param pattern to convert
     * @param dibitLength in dibits
     * @return array of dibits for the sync pattern.
     */
    public static Dibit[] toDibits(long pattern, int dibitLength)
    {
        Dibit[] dibits = new Dibit[dibitLength];
        long mask = 3;
        int dibitValue;

        for(int x = 0; x < dibitLength; x++)
        {
            dibitValue = (int)((pattern & mask) >> (2 * x));

            dibits[dibitLength - x - 1] = switch(dibitValue)
            {
                case 0 -> Dibit.D00_PLUS_1;
                case 1 -> Dibit.D01_PLUS_3;
                case 2 -> Dibit.D10_MINUS_1;
                default -> Dibit.D11_MINUS_3;
            };

            mask = mask << 2;
        }

        return dibits;
    }

    /**
     * Utility method to convert a dibit array into an array of ideal soft symbol values.
     * @param dibits for the sync pattern
     * @return ideal soft symbol array.
     */
    public static float[] toSymbols(Dibit[] dibits)
    {
        float[] symbols = new float[dibits.length];

        for(int x = 0; x < dibits.length; x++)
        {
            symbols[x] = dibits[x].getIdealPhase();
        }

        return symbols;
    }

    /**
     * Creates a reference waveform representation of the sync pattern that can be used to correlate against a
     * candidate detected sync waveform to discriminate between a valid and invalid sync detection.  The generated
     * waveform will be aligned where the final sample is time-aligned to the final symbol.
     *
     * @param samplesPerSymbol count
     * @return a reference waveform and ideal correlation score.
     */
    public ReferenceSyncWaveform getReferenceWaveform(double samplesPerSymbol)
    {
        int key = (int)(samplesPerSymbol * 1000.0);

        if(mReferenceWaveformMap.containsKey(key))
        {
            return mReferenceWaveformMap.get(key);
        }

        float[] symbols = getSymbols();
        int sampleCount = (int)Math.ceil(samplesPerSymbol * (symbols.length - 1)) + 1;
        float[] samples = new float[sampleCount];
        double sampleInterval = 1.0 / samplesPerSymbol;

        double[] x = new double[symbols.length + 2];
        double[] y = new double[symbols.length + 2];

        for(int i = -1; i < x.length - 1; i++)
        {
            x[i + 1] = i;

            if(0 <= i && i < symbols.length)
            {
                y[i + 1] = symbols[i];
            }
        }

        double start = symbols.length - 1;
        int samplePointer = sampleCount - 1;
        SplineInterpolator interpolator = new SplineInterpolator();
        PolynomialSplineFunction function = interpolator.interpolate(x, y);

        float accumulator = 0;
        float sample;
        float max = 0;
        while(samplePointer >= 0)
        {
            sample = (float)function.value(start);

            if(Math.abs(sample) > max)
            {
                max = Math.abs(sample);
            }
            samples[samplePointer--] = sample;
            accumulator += sample * sample;
            start -= sampleInterval;
        }

        ReferenceSyncWaveform referenceWaveform = new ReferenceSyncWaveform(samples, accumulator, max);
        mReferenceWaveformMap.put(key, referenceWaveform);
        return referenceWaveform;
    }
}
