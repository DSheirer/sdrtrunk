/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2019 Dennis Sheirer
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
 * *****************************************************************************
 */
package io.github.dsheirer.spectrum;

import io.github.dsheirer.dsp.filter.Window;
import io.github.dsheirer.dsp.filter.Window.WindowType;
import io.github.dsheirer.properties.SystemProperties;
import io.github.dsheirer.sample.IOverflowListener;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.SampleType;
import io.github.dsheirer.sample.buffer.ReusableComplexBuffer;
import io.github.dsheirer.source.ISourceEventProcessor;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.spectrum.converter.DFTResultsConverter;
import io.github.dsheirer.util.ThreadPool;
import org.jtransforms.fft.FloatFFT_1D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Processes both complex samples or float samples and dispatches a float array of DFT results, using configurable fft
 * size and output dispatch timelines.
 */
public class DFTProcessor implements Listener<ReusableComplexBuffer>, ISourceEventProcessor, IDFTWidthChangeProcessor
{
    private static final Logger mLog = LoggerFactory.getLogger(DFTProcessor.class);
    private static final int BUFFER_QUEUE_MAX_SIZE = 20;
    private static final int BUFFER_QUEUE_OVERFLOW_RESET_THRESHOLD = 6;
    private static final String FRAME_RATE_PROPERTY = "spectral.display.frame.rate";

    //The Cosine and Hann windows seem to offer the best spectral display with minimal bin leakage/smearing
    private WindowType mWindowType = WindowType.HANN;
    private double[] mWindow;
    private DFTSize mDFTSize = DFTSize.FFT04096;
    private DFTSize mNewDFTSize = DFTSize.FFT04096;
    private FloatFFT_1D mFFT = new FloatFFT_1D(mDFTSize.getSize());
    private int mFrameRate;
    private int mSampleRate = 2400000; //Initial high value until we receive update from tuner
    private int mFrameSize;
    private int mFrameFlushCount;
    private int mFrameOverlapCount;
    private SampleType mSampleType;
    private AtomicBoolean mRunning = new AtomicBoolean();
    private ScheduledFuture<?> mProcessorTaskHandle;
    private CopyOnWriteArrayList<DFTResultsConverter> mListeners = new CopyOnWriteArrayList<DFTResultsConverter>();
    private OverflowableBufferStream mOverflowableBufferStream = new OverflowableBufferStream(BUFFER_QUEUE_MAX_SIZE,
        BUFFER_QUEUE_OVERFLOW_RESET_THRESHOLD, mDFTSize.getSize());
    private float[] mPreviousSamples;

    public DFTProcessor(SampleType sampleType)
    {
        setSampleType(sampleType);
        mFrameRate = SystemProperties.getInstance().get(FRAME_RATE_PROPERTY, 20);
        calculateConsumptionRate();
        start();
    }

    public void dispose()
    {
        stop();

        mListeners.clear();
        mOverflowableBufferStream.clear();
        mWindow = null;
    }

    /**
     * Sets the listener to receive buffer overflow/reset indications
     * @param listener
     */
    public void setOverflowListener(IOverflowListener listener)
    {
        mOverflowableBufferStream.setOverflowListener(listener);
    }

    public WindowType getWindowType()
    {
        return mWindowType;
    }

    public void setWindowType(WindowType windowType)
    {
        mWindowType = windowType;

        if(mSampleType == SampleType.COMPLEX)
        {
            mWindow = Window.getWindow(mWindowType, mDFTSize.getSize() * 2);
        }
        else
        {
            mWindow = Window.getWindow(mWindowType, mDFTSize.getSize());
        }
    }

    /**
     * Sets the processor mode to Float or Complex, depending on the sample
     * types that will be delivered for processing
     */
    public void setSampleType(SampleType type)
    {
        mSampleType = type;
        setWindowType(mWindowType);
    }

    public SampleType getSampleType()
    {
        return mSampleType;
    }

    /**
     * Queues an FFT size change request.  The scheduled executor will apply
     * the change when it runs.
     */
    public void setDFTSize(DFTSize size)
    {
        mNewDFTSize = size;
    }

    public DFTSize getDFTSize()
    {
        return mDFTSize;
    }

    public int getFrameRate()
    {
        return mFrameRate;
    }

    public void setFrameRate(int framesPerSecond)
    {
        if(framesPerSecond < 1 || framesPerSecond > 1000)
        {
            throw new IllegalArgumentException("DFTProcessor cannot run more than 1000 times per second -- requested " +
                "setting:" + framesPerSecond);
        }

        mFrameRate = framesPerSecond;

        SystemProperties.getInstance().set(FRAME_RATE_PROPERTY, mFrameRate);

        calculateConsumptionRate();

        restart();
    }

    public void start()
    {
        if(mProcessorTaskHandle == null)
        {
            //Schedule the DFT to run calculations at a fixed rate
            int initialDelay = 0;
            int period = (int) (1000 / mFrameRate);

            mProcessorTaskHandle = ThreadPool.SCHEDULED.scheduleAtFixedRate(new DFTCalculationTask(), initialDelay, period,
                TimeUnit.MILLISECONDS);
        }
    }

    public void stop()
    {
        //Cancel running DFT calculation task
        if(mProcessorTaskHandle != null)
        {
            mProcessorTaskHandle.cancel(true);
            mProcessorTaskHandle = null;
        }
    }

    public boolean isRunning()
    {
        return mProcessorTaskHandle != null;
    }

    public void restart()
    {
        stop();
        start();
    }

    /**
     * Places the sample into a transfer queue for future processing.
     */
    @Override
    public void receive(ReusableComplexBuffer sampleBuffer)
    {
        mOverflowableBufferStream.offer(sampleBuffer);
    }

    private void calculate()
    {
        //We always send the previous calculated samples - this should improve the screen rendering since the frame
        //rate will always occur on an even rhythm.  Any delays caused by processing will be absorbed and not impact
        //the screen rendering.
        dispatch(mPreviousSamples);

        try
        {
            if(mFrameFlushCount > 0)
            {
                mOverflowableBufferStream.flush(mFrameFlushCount);
            }

            //If this throws an IO exception, the buffer queue is (temporarily) empty and we return from the method
            float[] samples = mOverflowableBufferStream.get(mFrameSize, mFrameOverlapCount);

            Window.apply(mWindow, samples);

            if(mSampleType == SampleType.REAL)
            {
                mFFT.realForward(samples);
            }
            else
            {
                mFFT.complexForward(samples);
            }

            mPreviousSamples = samples;
        }
        catch(IOException ioe)
        {
            //No new data, dispatch the previous samples again
            //no-op
        }
        catch(Exception e)
        {
            if(e instanceof InterruptedException)
            {
                mLog.info("FFT Library interrupted exception - this is normal during application shutdown");
            }
            else
            {
                mLog.error("Error while calculating FFT results", e);
            }
        }
    }

    /**
     * Takes a calculated DFT results set, reformats the data, and sends it
     * out to all registered listeners.
     */
    private void dispatch(float[] results)
    {

        for (DFTResultsConverter mListener : mListeners) {
            mListener.receive(results);
        }
    }

    public void addConverter(DFTResultsConverter listener)
    {
        mListeners.add(listener);
    }

    private class DFTCalculationTask implements Runnable
    {
        @Override
        public void run()
        {
            try
            {
				/* Only run if we're not currently running */
                if(mRunning.compareAndSet(false, true))
                {
                    checkFFTSize();

                    calculate();

                    mRunning.set(false);
                }
            }
            catch(Exception e)
            {
                mLog.error("error during dft processor calculation task", e);
            }
        }
    }

    /**
     * Checks for a queued FFT width change request and applies it.  This
     * method will only be accessed by the scheduled executor that gains
     * access to run a calculate method, thus providing thread safety.
     */
    private void checkFFTSize()
    {
        if(mNewDFTSize.getSize() != mDFTSize.getSize())
        {
            mDFTSize = mNewDFTSize;

            calculateConsumptionRate();

            setWindowType(mWindowType);

            mFFT = new FloatFFT_1D(mDFTSize.getSize());
        }
    }

    public void clearBuffer()
    {
        mOverflowableBufferStream.clear();
    }

    @Override
    public void process(SourceEvent event)
    {
        switch(event.getEvent())
        {
            case NOTIFICATION_SAMPLE_RATE_CHANGE:
                mSampleRate = event.getValue().intValue();
                calculateConsumptionRate();
                break;
            default:
                break;
        }
    }

    /**
     * Calculates the frame size, flush count and overlap count to use for each calculation cycle.
     */
    private void calculateConsumptionRate()
    {
        int floatsPerSample = mSampleType == SampleType.COMPLEX ? 2 : 1;

        mFrameSize = mDFTSize.getSize() * floatsPerSample;
        mPreviousSamples = new float[mFrameSize];

        int productionRate = mSampleRate * floatsPerSample;
        int consumptionRate = mFrameRate * mFrameSize;
        int residual = productionRate - consumptionRate;

        if(residual < 0)
        {
            mFrameFlushCount = 0;
            mFrameOverlapCount = -residual / mFrameRate;

            if(mFrameOverlapCount * mFrameRate < -residual)
            {
                mFrameOverlapCount ++;
            }
        }
        else if(residual > 0)
        {
            mFrameOverlapCount = 0;
            mFrameFlushCount = residual / mFrameRate;

            if(mFrameFlushCount * mFrameRate < residual)
            {
                mFrameFlushCount++;
            }
        }

        //Ensure we're flushing or overlapping by even multiples of the sample size
        if(mFrameOverlapCount % floatsPerSample != 0)
        {
            mFrameOverlapCount++;
        }

        if(mFrameFlushCount % floatsPerSample != 0)
        {
            mFrameFlushCount++;
        }

        //If the overlap size is greater than the frame size, we can't do that.  Automatically decrease the frame rate
        //until we reach a legitimate overlap value.
        if(mFrameOverlapCount >= mFrameSize)
        {
            mLog.warn("Unable to provide frame rate [" + mFrameRate + "] for current DFT size [" + mDFTSize.getSize() +
                "] - reducing frame rate");

            mFrameRate--;

            if(mFrameRate <= 0)
            {
                //Consider reducing the FFT size as a possible alternative corrective measure
                throw new IllegalStateException("Unable to determine a viable frame rate based on incoming sample " +
                    "rate, DFT size and DFT frame rate");
            }

            //Recursively call this method to test the new frame rate
            calculateConsumptionRate();
        }
    }
}
