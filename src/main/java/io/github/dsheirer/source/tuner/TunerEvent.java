package io.github.dsheirer.source.tuner;

public class TunerEvent
{
    private Tuner mTuner;
    private Event mEvent;

    public TunerEvent(Tuner tuner, Event event)
    {
        mTuner = tuner;
        mEvent = event;
    }

    public Tuner getTuner()
    {
        return mTuner;
    }

    public Event getEvent()
    {
        return mEvent;
    }

    public enum Event
    {
        CHANNEL_COUNT,
        FREQUENCY_UPDATED,
        FREQUENCY_ERROR_UPDATED,
        LOCK_STATE_CHANGE,
        MEASURED_FREQUENCY_ERROR_UPDATED,
        SAMPLE_RATE_UPDATED,

        CLEAR_MAIN_SPECTRAL_DISPLAY,
        REQUEST_MAIN_SPECTRAL_DISPLAY,
        REQUEST_NEW_SPECTRAL_DISPLAY;
    }
}
