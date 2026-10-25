package io.github.dsheirer.dsp.c4fm;

import io.github.dsheirer.dsp.symbol.Dibit;

/**
 * Interface for a soft sync detector for C4FM/QPSK dibits.
 */
public interface ISoftC4FMSyncDetector
{
    /**
     * Process the symbol and detect sync pattern
     * @param softSymbol to process
     * @return sync detection score
     */
    float process(float softSymbol);

    /**
     * Soft symbols that represent the sync pattern.  For detectors that monitor multiple sync patterns (ie DMR), this
     * is the sync pattern that produced the score from the most recent call to process().
     * @return soft symbols array
     */
    float[] getPatternSymbols();

    /**
     * Array of dibits for the sync pattern (or most recently detected sync pattern for multi-pattern detectors).
     * @return sync pattern dibit array
     */
    Dibit[] getPatternDibits();

    /**
     * Length of the sync pattern in dibits
     */
    int getDibitLength();

    /**
     * Provides a reference waveform that represents the sync pattern (or most recently detected sync pattern for
     * multi-pattern detectors) for sync detection validation
     * @param samplesPerSymbol for each symbol
     * @return reference waveform
     */
    ReferenceSyncWaveform getReferenceWaveform(double samplesPerSymbol);
}
