/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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
package io.github.dsheirer.spectrum;

import io.github.dsheirer.buffer.INativeBuffer;
import io.github.dsheirer.controller.NamingThreadFactory;
import io.github.dsheirer.dsp.window.WindowFactory;
import io.github.dsheirer.dsp.window.WindowType;
import io.github.dsheirer.properties.SystemProperties;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.spectrum.converter.DFTResultsConverter;
import java.io.IOException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jtransforms.fft.FloatFFT_1D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Processes both complex samples or float samples and dispatches a float array of DFT results, using configurable fft
 * size and output dispatch timelines.
 */
public class ComplexDftProcessor<T extends INativeBuffer> implements Listener<T>, IDFTWidthChangeProcessor
{
    private static final Logger mLog = LoggerFactory.getLogger(ComplexDftProcessor.class);
    private static final String FRAME_RATE_PROPERTY = "spectral.display.frame.rate";

    //The Cosine and Hann windows seem to offer the best spectral display with minimal bin leakage/smearing
    private volatile WindowType mWindowType = WindowType.BLACKMAN_HARRIS_7;
    private volatile boolean mWindowChanged = true;
    private volatile boolean mUnityGainWindow = false;
    private float[] mWindow;
    private DFTSize mDFTSize = DFTSize.FFT04096;
    private volatile DFTSize mNewDFTSize = DFTSize.FFT04096;
    private FloatFFT_1D mFFT = new FloatFFT_1D(mDFTSize.getSize());
    private int mFrameRate;
    private AtomicBoolean mRunning = new AtomicBoolean();
    private ScheduledFuture<?> mProcessorTaskHandle;
    private ScheduledExecutorService mExecutorService = Executors.newSingleThreadScheduledExecutor(new NamingThreadFactory("sdrtrunk dft processor"));
    private CopyOnWriteArrayList<DFTResultsConverter> mListeners = new CopyOnWriteArrayList<>();
    private NativeBufferManager mDftBufferManager = new NativeBufferManager(mDFTSize.getSize() * 2);
    private float[] mPreviousSamples = new float[mDFTSize.getSize() * 2];

    public ComplexDftProcessor()
    {
        mFrameRate = SystemProperties.getInstance().get(FRAME_RATE_PROPERTY, 20);
        mWindow = createWindow(mWindowType, mDFTSize.getSize(), mUnityGainWindow);
        start();
    }

    public void dispose()
    {
        stop();
        mExecutorService.shutdown();

        mListeners.clear();
        mWindow = null;
    }

    public WindowType getWindowType()
    {
        return mWindowType;
    }

    /**
     * Queues a window type change.  The processor applies the change before the next calculation.
     */
    public void setWindowType(WindowType windowType)
    {
        if(windowType != null)
        {
            mWindowType = windowType;
            mWindowChanged = true;
        }
    }

    /**
     * Scales the window to unity coherent gain, so that a full scale complex tone centered in a bin has a DFT
     * magnitude equal to the DFT size, regardless of the window type.  Use this with a full scale (dBFS) converter.
     * Defaults to false, which leaves the window coefficients unscaled.
     */
    public void setUnityGainWindow(boolean unityGain)
    {
        mUnityGainWindow = unityGain;
        mWindowChanged = true;
    }

    /**
     * Creates the window coefficients for interleaved complex samples, with the same coefficient applied to the I
     * and Q values of each sample.
     * @param type of window.  Kaiser windows need extra parameters, so Kaiser uses a Blackman-Harris 7 window.
     * @param size of the DFT in complex samples
     * @param unityGain to scale the window so the coefficients sum to the DFT size (unity coherent gain)
     * @return interleaved window coefficients, twice the DFT size
     */
    public static float[] createWindow(WindowType type, int size, boolean unityGain)
    {
        if(type == null || type == WindowType.KAISER)
        {
            type = WindowType.BLACKMAN_HARRIS_7;
        }

        float[] window = WindowFactory.getWindow(type, size);
        double sum = 0.0;

        for(float coefficient: window)
        {
            sum += coefficient;
        }

        float scale = (unityGain && sum > 0) ? (float)(size / sum) : 1.0f;
        float[] interleaved = new float[size * 2];

        for(int x = 0; x < size; x++)
        {
            interleaved[2 * x] = window[x] * scale;
            interleaved[2 * x + 1] = window[x] * scale;
        }

        return interleaved;
    }

    /**
     * Equivalent noise bandwidth of the window type, in DFT bins.  This is how much wider the window's noise
     * bandwidth is than one bin: 1.0 for no window and about 2 to 4 for the low leakage windows.
     */
    public static double getNoiseBandwidth(WindowType type)
    {
        if(type == null || type == WindowType.KAISER)
        {
            type = WindowType.BLACKMAN_HARRIS_7;
        }

        int size = 4096;
        float[] window = WindowFactory.getWindow(type, size);
        double sum = 0.0;
        double sumSquares = 0.0;

        for(float coefficient: window)
        {
            sum += coefficient;
            sumSquares += coefficient * coefficient;
        }

        return sum > 0 ? size * sumSquares / (sum * sum) : 1.0;
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
        setFrameRateNoSave(framesPerSecond);
        SystemProperties.getInstance().set(FRAME_RATE_PROPERTY, mFrameRate);
    }

    /**
     * Sets the frame rate without saving it as the default frame rate setting.
     */
    public void setFrameRateNoSave(int framesPerSecond)
    {
        if(framesPerSecond < 1 || framesPerSecond > 1000)
        {
            throw new IllegalArgumentException("DFTProcessor cannot run more than 1000 times per second -- requested " +
                "setting:" + framesPerSecond);
        }

        mFrameRate = framesPerSecond;
        restart();
    }

    public void start()
    {
        if(mProcessorTaskHandle == null)
        {
            //Schedule the DFT to run calculations at a fixed rate
            int initialDelay = 0;
            int period = (int) (1000 / mFrameRate);

            mProcessorTaskHandle = mExecutorService.scheduleAtFixedRate(new DFTCalculationTask(), initialDelay, period,
                TimeUnit.MILLISECONDS);
        }
    }

    public void stop()
    {
        //Cancel running DFT calculation task
        if(mProcessorTaskHandle != null)
        {
            mProcessorTaskHandle.cancel(false);
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
    public void receive(T buffer)
    {
        mDftBufferManager.add(buffer);
    }

    private void calculate()
    {
        //We always send the previous calculated samples - this should improve the screen rendering since the frame
        //rate will always occur on an even rhythm.  Any delays caused by processing will be absorbed and not impact
        //the screen rendering.
        dispatch(mPreviousSamples);

        try
        {
            //If this throws an IO exception, the buffer queue is (temporarily) empty and we return from the method
            float[] samples = mDftBufferManager.get(mDFTSize.getSize());
            if(mWindow == null || mWindow.length != samples.length)
            {
                mWindow = createWindow(mWindowType, mDFTSize.getSize(), mUnityGainWindow);
            }

            WindowFactory.apply(mWindow, samples);
            mFFT.complexForward(samples);
            mPreviousSamples = samples;
        }
        catch(IOException ioe)
        {
            //Not enough samples available, dispatch the previous samples again
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
        for (DFTResultsConverter mListener : mListeners)
        {
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
     * Checks for a queued FFT width or window change request and applies it.  This
     * method will only be accessed by the scheduled executor that gains
     * access to run a calculate method, thus providing thread safety.
     */
    private void checkFFTSize()
    {
        if(mNewDFTSize.getSize() != mDFTSize.getSize())
        {
            mDFTSize = mNewDFTSize;
            mWindowChanged = true;
            mFFT = new FloatFFT_1D(mDFTSize.getSize());
        }

        if(mWindowChanged)
        {
            mWindowChanged = false;
            mWindow = createWindow(mWindowType, mDFTSize.getSize(), mUnityGainWindow);
        }
    }

    public void clearBuffer()
    {
        mDftBufferManager.clear();
    }
}
