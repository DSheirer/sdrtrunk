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
        ADD,
        LOCK_STATE_CHANGE,
        REMOVE,
        CHANNEL_COUNT,
        FREQUENCY,
        SAMPLE_RATE,
        REQUEST_MAIN_SPECTRAL_DISPLAY,
        REQUEST_NEW_SPECTRAL_DISPLAY;
    }
}
