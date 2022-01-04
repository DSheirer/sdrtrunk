package io.github.dsheirer.dsp.fm;

import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.SourceEvent;

/**
 * Interface for FM demodulator that provides squelch control.
 */
public interface ISquelchingFmDemodulator extends IFmDemodulator
{
    /**
     * Indicates if the squelch state has changed.
     */
    boolean isSquelchChanged();

    /**
     * Indicates if the squelch state is currently muted
     */
    boolean isMuted();

    /**
     * Sets the threshold to use for power squelch
     * @param threshold in decibels
     */
    void setSquelchThreshold(double threshold);

    /**
     * Resets the demodulator
     */
    void reset();

    /**
     * Registers the listener to receive notifications of squelch change events from the power squelch.
     */
    void setSourceEventListener(Listener<SourceEvent> listener);

    /**
     * Receives a source event
     */
    void receive(SourceEvent sourceEvent);
}

