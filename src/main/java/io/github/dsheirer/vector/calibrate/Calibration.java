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

import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.settings.Setting;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Calibration base class.  Determines the optimal (scalar vs vector) implementation of an operation for this computer.
 *
 * Subclasses provide the candidate implementations, each as a trial that processes one buffer of calibration input.
 * Calibration then:
 * <ol>
 *     <li>Skips vector widths that the CPU doesn't support natively, since the Vector API emulates those slowly.</li>
 *     <li>Verifies that each candidate's output matches the scalar implementation, using fresh instances and
 *     several consecutive buffers so that state carried between buffers is also checked.  Candidates that don't
 *     match, or that throw an exception, are rejected.</li>
 *     <li>Warms up each remaining candidate and then measures them in interleaved rounds, rotating the order each
 *     round, and scores each candidate by its median throughput.  Interleaving spreads CPU frequency, thermal and
 *     background load changes evenly across the candidates.</li>
 *     <li>Selects the fastest vector candidate if it beats scalar by the minimum speedup, otherwise scalar, and only
 *     then stores the result, so an interrupted calibration stays uncalibrated.</li>
 * </ol>
 */
public abstract class Calibration
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Calibration.class);
    private static final DecimalFormat RATE_FORMAT = new DecimalFormat("0");
    private static final DecimalFormat SPEEDUP_FORMAT = new DecimalFormat("0.00");
    private static final long RANDOM_SEED = 2766L;
    /**
     * Number of consecutive buffers processed by each candidate during verification.
     */
    private static final int VERIFICATION_RUNS = 4;
    private static final long WARMUP_NANOS = TimeUnit.MILLISECONDS.toNanos(500);
    private static final long ROUND_NANOS = TimeUnit.MILLISECONDS.toNanos(100);
    private static final int ROUNDS = 9;
    /**
     * Minimum throughput ratio over scalar that a vector candidate needs to be selected, so that measurement noise
     * doesn't select a vector implementation that is no faster.
     */
    private static final double MINIMUM_SPEEDUP = 1.05;
    /**
     * Vector candidates within this throughput ratio of the fastest vector candidate are treated as equally fast, and
     * the narrowest of them is selected.  This keeps the selection stable when widths measure within noise of each
     * other, and avoids wider vectors when they don't help, since on some CPUs wide (512-bit) vector instructions
     * lower the clock speed for everything running on that core.
     */
    private static final double EQUIVALENT_SPEEDUP = 1.05;
    /**
     * Default maximum error, relative to the larger of 1.0 or the scalar value.
     */
    private static final double DEFAULT_TOLERANCE = 1e-5;
    /**
     * Receives each measured trial's final output so the JIT compiler can't eliminate the trial as unused.
     */
    private static volatile Object sSink;

    private final CalibrationType mType;
    private final Random mRandom = new Random(RANDOM_SEED);
    private Implementation mImplementation;
    private CalibrationResult mResult;

    /**
     * Processes one buffer of calibration input with a single instance of a candidate implementation.  Each call
     * continues from the state that the previous call left in the instance.
     */
    @FunctionalInterface
    public interface Trial
    {
        /**
         * Processes one buffer.
         * @return output as a float[], float[][], ComplexSamples or Float.  The output may be a buffer that the
         * trial reuses on the next call.
         */
        Object run();
    }

    /**
     * Candidate implementation.
     * @param implementation that the factory creates for this candidate
     * @param trial creates a trial with a new instance of the candidate implementation
     */
    public record Candidate(Implementation implementation, Supplier<Trial> trial) {}

    /**
     * Result of verifying a candidate against the scalar implementation.
     * @param implementation verified
     * @param error maximum error measured, or infinity when the output couldn't be compared
     * @param tolerance maximum allowed error
     * @param detail describing the largest error, or the exception that the candidate threw
     */
    public record Verification(Implementation implementation, double error, double tolerance, String detail)
    {
        public boolean passed()
        {
            return error <= tolerance;
        }

        @Override public String toString()
        {
            return implementation + (passed() ? " passed" : " FAILED") + " - max error " + error +
                " (tolerance " + tolerance + ")" + (detail != null ? " " + detail : "");
        }
    }

    /**
     * Constructs an instance
     * @param type of calibration
     */
    public Calibration(CalibrationType type)
    {
        mType = type;
    }

    /**
     * Type of calibration plugin
     */
    public CalibrationType getType()
    {
        return mType;
    }

    /**
     * Candidate implementations.  The list must contain a SCALAR candidate, which is the reference implementation,
     * and each candidate must be one that the operation's factory creates for that implementation.
     */
    protected abstract List<Candidate> getCandidates();

    /**
     * Creates a candidate.
     */
    protected static Candidate candidate(Implementation implementation, Supplier<Trial> trial)
    {
        return new Candidate(implementation, trial);
    }

    /**
     * Maximum error allowed between a candidate's output and the scalar output, relative to the larger of 1.0 or the
     * scalar value.  Override for operations where vector arithmetic legitimately differs more from scalar.
     */
    protected double getTolerance()
    {
        return DEFAULT_TOLERANCE;
    }

    /**
     * Maximum constant delay, in output samples, allowed between a candidate's output and the scalar output.  Vector
     * filters that pad their coefficients to a multiple of the vector lane count can have a slightly different delay
     * than the scalar filter.  Defaults to zero, which requires sample-aligned output.
     */
    protected int getMaximumDelay()
    {
        return 0;
    }

    /**
     * Indicates if this plugin is calibrated.
     */
    public boolean isCalibrated()
    {
        return getImplementation() != Implementation.UNCALIBRATED;
    }

    /**
     * Resets calibration status to uncalibrated for the plugin
     */
    public void reset()
    {
        setImplementation(Implementation.UNCALIBRATED);
        mResult = null;
        Setting.VECTOR_CALIBRATION_RESULT.with(getType().getPreferenceKey()).remove();
    }

    /**
     * Measurements from the most recent calibration.
     * @return result, or empty if uncalibrated or calibrated by an earlier version that didn't store results
     */
    public Optional<CalibrationResult> getResult()
    {
        if(mResult == null && isCalibrated())
        {
            mResult = CalibrationResult.parse(Setting.VECTOR_CALIBRATION_RESULT.with(getType().getPreferenceKey())
                .get(null)).orElse(null);
        }

        return Optional.ofNullable(mResult);
    }

    /**
     * Optimal implementation for this plugin
     * @return implementation or uncalibrated if this plugin has not yet been calibrated
     */
    public Implementation getImplementation()
    {
        if(mImplementation == null)
        {
            String implementation = Setting.VECTOR_CALIBRATION.with(getType().getPreferenceKey())
                .get(Implementation.UNCALIBRATED.name());

            try
            {
                mImplementation = Implementation.valueOf(implementation);
            }
            catch(IllegalArgumentException iae)
            {
                mImplementation = Implementation.UNCALIBRATED;
            }
        }

        return mImplementation;
    }

    /**
     * Sets and stores the optimal implementation.
     * @param implementation to set
     */
    private void setImplementation(Implementation implementation)
    {
        mImplementation = implementation;
        Setting.VECTOR_CALIBRATION.with(getType().getPreferenceKey()).put(implementation.name());
    }

    /**
     * Verifies, measures and selects the optimal implementation for this computer, and stores the result.
     * @throws CalibrationException if the scalar candidate is missing or fails
     */
    public final void calibrate() throws CalibrationException
    {
        List<Candidate> candidates = new ArrayList<>();

        for(Candidate candidate : getCandidates())
        {
            if(candidate.implementation().isNativelySupported())
            {
                candidates.add(candidate);
            }
        }

        Candidate scalar = getScalar(candidates);
        List<Candidate> verified = new ArrayList<>();
        List<Implementation> rejected = new ArrayList<>();

        for(Verification verification : verify(candidates))
        {
            if(verification.passed())
            {
                verified.add(getCandidate(candidates, verification.implementation()));
            }
            else if(verification.implementation() == Implementation.SCALAR)
            {
                //Scalar is the fallback, so keep it but flag the failed reference check
                LOGGER.error(getType() + " - scalar implementation failed its reference check: " + verification);
                verified.add(scalar);
            }
            else
            {
                LOGGER.warn(getType() + " - rejecting " + verification);
                rejected.add(verification.implementation());
            }
        }

        double[] rates;

        try
        {
            rates = measure(verified);
        }
        catch(Exception e)
        {
            throw new CalibrationException("Error while measuring " + getType() + " implementations", e);
        }

        double scalarRate = rates[verified.indexOf(scalar)];
        Map<Implementation,Double> speedups = new LinkedHashMap<>();
        StringBuilder sb = new StringBuilder();
        sb.append(getType()).append(" - throughput (runs/second):");

        for(int x = 0; x < verified.size(); x++)
        {
            Implementation implementation = verified.get(x).implementation();
            speedups.put(implementation, rates[x] / scalarRate);
            sb.append(" ").append(implementation).append("=").append(RATE_FORMAT.format(rates[x]));

            if(implementation != Implementation.SCALAR)
            {
                sb.append(" (").append(SPEEDUP_FORMAT.format(rates[x] / scalarRate)).append("x)");
            }
        }

        Implementation selected = select(speedups);
        LOGGER.info(sb.toString());
        LOGGER.info(getType() + " - selected " + selected);
        CalibrationResult result = new CalibrationResult(selected, speedups, rejected, System.currentTimeMillis());
        Setting.VECTOR_CALIBRATION_RESULT.with(getType().getPreferenceKey()).put(result.format());
        mResult = result;
        setImplementation(selected);
    }

    /**
     * Selects the implementation from the measured speedups.  Scalar is selected unless the fastest vector
     * implementation beats it by the minimum speedup.  Otherwise the narrowest vector implementation that is within
     * the equivalent speedup of the fastest, and that also beats scalar by the minimum speedup, is selected, and the
     * faster of two with the same width.
     * @param speedups throughput of each measured implementation relative to scalar
     * @return selected implementation
     */
    static Implementation select(Map<Implementation,Double> speedups)
    {
        double fastest = 0.0;

        for(Map.Entry<Implementation,Double> entry : speedups.entrySet())
        {
            if(entry.getKey() != Implementation.SCALAR)
            {
                fastest = Math.max(fastest, entry.getValue());
            }
        }

        if(fastest < MINIMUM_SPEEDUP)
        {
            return Implementation.SCALAR;
        }

        Implementation selected = Implementation.SCALAR;
        double selectedSpeedup = 0.0;

        for(Map.Entry<Implementation,Double> entry : speedups.entrySet())
        {
            Implementation implementation = entry.getKey();
            double speedup = entry.getValue();

            if(implementation == Implementation.SCALAR || speedup < MINIMUM_SPEEDUP ||
                speedup * EQUIVALENT_SPEEDUP < fastest)
            {
                continue;
            }

            int width = implementation.getEffectiveVectorBitSize();
            int selectedWidth = selected.getEffectiveVectorBitSize();

            if(selected == Implementation.SCALAR || width < selectedWidth ||
                (width == selectedWidth && speedup > selectedSpeedup))
            {
                selected = implementation;
                selectedSpeedup = speedup;
            }
        }

        return selected;
    }

    /**
     * Verifies the candidates against the scalar candidate.
     * @param includeEmulated to also verify vector widths that the CPU doesn't support natively, which the Vector API
     * emulates.  Calibration never selects these, but verifying them checks the implementations on narrower CPUs.
     * @return verification results, in candidate order
     * @throws CalibrationException if there is no scalar candidate or the scalar candidate can't be run
     */
    public List<Verification> verify(boolean includeEmulated) throws CalibrationException
    {
        return verify(getCandidates().stream()
            .filter(c -> includeEmulated || c.implementation().isNativelySupported()).toList());
    }

    /**
     * Verifies each candidate against the scalar candidate.
     * @param candidates to verify, including the scalar candidate
     * @return verification results, in candidate order
     * @throws CalibrationException if there is no scalar candidate or the scalar candidate can't be run
     */
    private List<Verification> verify(List<Candidate> candidates) throws CalibrationException
    {
        float[][] reference;

        try
        {
            reference = collect(getScalar(candidates));
        }
        catch(CalibrationException ce)
        {
            throw ce;
        }
        catch(Exception e)
        {
            throw new CalibrationException("Scalar implementation failed for " + getType(), e);
        }

        List<Verification> verifications = new ArrayList<>();

        for(Candidate candidate : candidates)
        {
            try
            {
                verifications.add(verify(candidate.implementation(), reference, collect(candidate)));
            }
            catch(Exception e)
            {
                verifications.add(new Verification(candidate.implementation(), Double.POSITIVE_INFINITY,
                    getTolerance(), "exception: " + e));
            }
        }

        return verifications;
    }

    /**
     * Compares a candidate's output to the scalar output, at the constant delay (up to the maximum delay) that fits
     * best.  Override for operations where the scalar output isn't a direct reference.
     * @param implementation being verified
     * @param reference output of the scalar candidate, as channels of consecutive runs
     * @param output of the candidate, as channels of consecutive runs
     * @return verification
     */
    protected Verification verify(Implementation implementation, float[][] reference, float[][] output)
    {
        if(reference.length != output.length)
        {
            return new Verification(implementation, Double.POSITIVE_INFINITY, getTolerance(),
                "output has " + output.length + " channels, expected " + reference.length);
        }

        for(int channel = 0; channel < reference.length; channel++)
        {
            if(reference[channel].length != output[channel].length)
            {
                return new Verification(implementation, Double.POSITIVE_INFINITY, getTolerance(),
                    "channel " + channel + " has " + output[channel].length + " samples, expected " +
                        reference[channel].length);
            }
        }

        int maximumDelay = getMaximumDelay();
        Verification best = null;

        for(int delay = -maximumDelay; delay <= maximumDelay; delay++)
        {
            Verification verification = verify(implementation, reference, output, delay, maximumDelay);

            if(best == null || verification.error() < best.error())
            {
                best = verification;
            }
        }

        return best;
    }

    /**
     * Compares the candidate output to the scalar output delayed by the number of samples, skipping the margin at
     * each end of the output so that every delay compares the same number of samples.
     */
    private Verification verify(Implementation implementation, float[][] reference, float[][] output, int delay,
                                int margin)
    {
        double maxError = 0.0;
        String detail = null;

        for(int channel = 0; channel < reference.length; channel++)
        {
            for(int x = margin; x < output[channel].length - margin; x++)
            {
                double error = error(reference[channel][x - delay], output[channel][x]);

                if(error > maxError)
                {
                    maxError = error;
                    detail = "at channel " + channel + " index " + x + " (expected " + reference[channel][x - delay] +
                        ", was " + output[channel][x] + ")";
                }
            }
        }

        if(delay != 0)
        {
            detail = "at delay " + delay + (detail != null ? " " + detail : "");
        }

        return new Verification(implementation, maxError, getTolerance(), detail);
    }

    /**
     * Error between an expected and an actual value, relative to the larger of 1.0 or the expected value.
     * @return error, or infinity when only one of the values is NaN or infinite
     */
    protected static double error(double expected, double actual)
    {
        if(Double.isNaN(expected) || Double.isNaN(actual))
        {
            return Double.isNaN(expected) && Double.isNaN(actual) ? 0.0 : Double.POSITIVE_INFINITY;
        }

        if(Double.isInfinite(expected) || Double.isInfinite(actual))
        {
            return expected == actual ? 0.0 : Double.POSITIVE_INFINITY;
        }

        return Math.abs(expected - actual) / Math.max(1.0, Math.abs(expected));
    }

    /**
     * Runs a new trial of the candidate for the verification runs and collects its output.
     * @return output channels, each the concatenated output of consecutive runs
     */
    private static float[][] collect(Candidate candidate)
    {
        Trial trial = candidate.trial().get();
        List<float[][]> runs = new ArrayList<>();

        for(int run = 0; run < VERIFICATION_RUNS; run++)
        {
            float[][] output = toChannels(trial.run());
            float[][] copy = new float[output.length][];

            for(int channel = 0; channel < output.length; channel++)
            {
                copy[channel] = output[channel].clone();
            }

            runs.add(copy);
        }

        int channels = runs.getFirst().length;
        float[][] collected = new float[channels][];

        for(int channel = 0; channel < channels; channel++)
        {
            int length = 0;

            for(float[][] run : runs)
            {
                if(run.length != channels)
                {
                    throw new IllegalStateException("Inconsistent output channel count between runs");
                }

                length += run[channel].length;
            }

            collected[channel] = new float[length];
            int offset = 0;

            for(float[][] run : runs)
            {
                System.arraycopy(run[channel], 0, collected[channel], offset, run[channel].length);
                offset += run[channel].length;
            }
        }

        return collected;
    }

    /**
     * Converts a trial's output to output channels.
     */
    private static float[][] toChannels(Object output)
    {
        return switch(output)
        {
            case float[] samples -> new float[][]{samples};
            case float[][] channels -> channels;
            case ComplexSamples complex -> new float[][]{complex.i(), complex.q()};
            case Float value -> new float[][]{{value}};
            case null -> throw new IllegalStateException("Trial produced no output");
            default -> throw new IllegalStateException("Unsupported trial output type: " + output.getClass());
        };
    }

    /**
     * Warms up each candidate, then measures each in interleaved rounds.
     * @return median throughput (runs per second) for each candidate, in candidate order
     */
    private static double[] measure(List<Candidate> candidates)
    {
        List<Trial> trials = new ArrayList<>();

        for(Candidate candidate : candidates)
        {
            Trial trial = candidate.trial().get();
            trials.add(trial);
            run(trial, WARMUP_NANOS);
        }

        double[][] rates = new double[trials.size()][ROUNDS];

        for(int round = 0; round < ROUNDS; round++)
        {
            for(int x = 0; x < trials.size(); x++)
            {
                int index = (round + x) % trials.size();
                rates[index][round] = run(trials.get(index), ROUND_NANOS);
            }
        }

        double[] medians = new double[trials.size()];

        for(int x = 0; x < trials.size(); x++)
        {
            Arrays.sort(rates[x]);
            medians[x] = rates[x][ROUNDS / 2];
        }

        return medians;
    }

    /**
     * Repeatedly runs the trial for the duration.
     * @return throughput in runs per second
     */
    private static double run(Trial trial, long durationNanos)
    {
        Object output;
        long count = 0;
        long start = System.nanoTime();
        long elapsed;

        do
        {
            output = trial.run();
            count++;
            elapsed = System.nanoTime() - start;
        }
        while(elapsed < durationNanos);

        sSink = output;
        return count * 1E9 / elapsed;
    }

    private static Candidate getScalar(List<Candidate> candidates) throws CalibrationException
    {
        for(Candidate candidate : candidates)
        {
            if(candidate.implementation() == Implementation.SCALAR)
            {
                return candidate;
            }
        }

        throw new CalibrationException("No scalar candidate");
    }

    private static Candidate getCandidate(List<Candidate> candidates, Implementation implementation)
    {
        for(Candidate candidate : candidates)
        {
            if(candidate.implementation() == implementation)
            {
                return candidate;
            }
        }

        throw new IllegalArgumentException("No candidate for " + implementation);
    }

    /**
     * Generates repeatable floating point samples in the range -1.0 - 1.0
     * @param size of array
     * @return generated samples
     */
    protected float[] getFloatSamples(int size)
    {
        float[] samples = new float[size];

        for(int x = 0; x < samples.length; x++)
        {
            samples[x] = mRandom.nextFloat() * 2.0f - 1.0f;
        }

        return samples;
    }

    /**
     * Generates repeatable floating point samples in the range 0.0 - 1.0
     * @param size of array
     * @return generated samples
     */
    protected float[] getPositiveFloatSamples(int size)
    {
        float[] samples = new float[size];

        for(int x = 0; x < samples.length; x++)
        {
            samples[x] = mRandom.nextFloat();
        }

        return samples;
    }
}
