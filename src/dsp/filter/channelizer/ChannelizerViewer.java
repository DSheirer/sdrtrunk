/*******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2017 Dennis Sheirer
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
 *
 ******************************************************************************/
package dsp.filter.channelizer;

import dsp.filter.FilterFactory;
import dsp.mixer.Oscillator;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sample.Listener;
import sample.SampleType;
import sample.complex.ComplexBuffer;
import settings.SettingsManager;
import source.tuner.configuration.TunerConfigurationModel;
import source.tuner.frequency.FrequencyChangeEvent;
import spectrum.DFTProcessor;
import spectrum.DFTSize;
import spectrum.SpectrumPanel;
import spectrum.converter.ComplexDecibelConverter;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ChannelizerViewer extends JFrame
{
    private final static Logger mLog = LoggerFactory.getLogger(ChannelizerViewer.class);

    private static final int CHANNEL_BANDWIDTH = 12500;

    private SettingsManager mSettingsManager = new SettingsManager(new TunerConfigurationModel());
    private JPanel mPrimaryPanel;
    private SpectrumPanel mSpectrumPanel;
    private ComplexDecibelConverter mComplexDecibelConverter = new ComplexDecibelConverter();
    private DFTProcessor mDFTProcessor = new DFTProcessor(SampleType.COMPLEX);
    private ChannelPanel mChannelPanel;
    private Oscillator mOscillator;
    private int mChannelCount;
    private int mSampleRate;
    private int mToneFrequency;
    private DFTSize mDFTSize = DFTSize.FFT04096;

    public ChannelizerViewer(int channelCount)
    {
        mChannelCount = channelCount;
        mSampleRate = mChannelCount * CHANNEL_BANDWIDTH;
        mToneFrequency = 37500; //Set to channel 1 as default

        init();
    }

    private void init()
    {
        setTitle("Channelizer Viewer");
        setSize(1000, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new MigLayout("insets 0 0 0 0", "[grow,fill]", "[grow,fill]"));
        setLocationRelativeTo(null);
        add(getPrimaryPanel());

        mOscillator = new Oscillator(mToneFrequency, mSampleRate);
    }

    public void start()
    {
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        scheduledExecutorService.scheduleAtFixedRate(new SampleGenerationTask(), 0, 100, TimeUnit.MILLISECONDS);
    }

    public void setToneFrequency(int frequency)
    {
        mOscillator.setFrequency(frequency);
        mToneFrequency = frequency;
    }

    private void generateSamples()
    {
        int size = mDFTSize.getSize();

        float[] samples = new float[size * 2];

        for(int x = 0; x < size; x += 2)
        {
            samples[x] = mOscillator.getComplex().inphase();
            samples[x + 1] = mOscillator.getComplex().quadrature();

            mOscillator.rotate();
        }

        ComplexBuffer buffer = new ComplexBuffer(samples);

        getChannelPanel().receive(buffer);
        mDFTProcessor.receive(buffer);
    }

    private JPanel getPrimaryPanel()
    {
        if(mPrimaryPanel == null)
        {
            mPrimaryPanel = new JPanel();
            mPrimaryPanel.setLayout(new MigLayout("insets 0 0 0 0", "[grow,fill]", "[][grow,fill]"));
            mPrimaryPanel.add(getSpectrumPanel(), "wrap");
            mPrimaryPanel.add(getChannelPanel());
        }

        return mPrimaryPanel;
    }

    private SpectrumPanel getSpectrumPanel()
    {
        if(mSpectrumPanel == null)
        {
            mSpectrumPanel = new SpectrumPanel(mSettingsManager);
            mSpectrumPanel.setPreferredSize(new Dimension(1000, 200));
            mComplexDecibelConverter.addListener(mSpectrumPanel);
            mDFTProcessor.addConverter(mComplexDecibelConverter);
            mDFTProcessor.frequencyChanged(new FrequencyChangeEvent(
                FrequencyChangeEvent.Event.NOTIFICATION_SAMPLE_RATE_CHANGE, mSampleRate));
        }

        return mSpectrumPanel;
    }

    private ChannelPanel getChannelPanel()
    {
        if(mChannelPanel == null)
        {
            mChannelPanel = new ChannelPanel(getFilter());
        }

        return mChannelPanel;
    }

    private float[] getFilter()
    {
        int symbolRate = 4800;
        int samplesPerSymbol = 2;
        int symbolCount = 8;

        //Alpha is the residual channel bandwidth left over from the symbol rate and samples per symbol
        float alpha = ((float)CHANNEL_BANDWIDTH / (float)(symbolRate * samplesPerSymbol)) - 1.0f;

        float[] taps = FilterFactory.getRootRaisedCosine(samplesPerSymbol * mChannelCount, symbolCount, alpha);

        StringBuilder sb = new StringBuilder();
        sb.append("\nPolyphase Channelizer\n");
        sb.append("Sample Rate:" + mSampleRate + " Channels:" + mChannelCount + " Channel Rate:" + CHANNEL_BANDWIDTH + "\n");
        sb.append("Alpha: " + alpha + " Tap Count:" + taps.length + "\n");
        sb.append("Channel:" + mToneFrequency);
        mLog.debug(sb.toString());

        return taps;
    }

    public class ChannelPanel extends JPanel implements Listener<ComplexBuffer>
    {
        private PolyphaseChannelizer mPolyphaseChannelizer;

        public ChannelPanel(float[] taps)
        {
            mPolyphaseChannelizer = new PolyphaseChannelizer(taps, mChannelCount);
        }

        @Override
        public void receive(ComplexBuffer complexBuffer)
        {
            mPolyphaseChannelizer.receive(complexBuffer);
        }
    }

    public class SampleGenerator
    {
        public SampleGenerator()
        {
            mLog.debug("Starting ...");


            PolyphaseChannelizer channelizer = new PolyphaseChannelizer(getFilter(), mChannelCount);

            Oscillator oscillator = new Oscillator(mToneFrequency, mSampleRate);

            for(int x = 0; x < 2000; x++)
            {
                channelizer.filter(oscillator.getComplex().inphase(), oscillator.getComplex().quadrature());
                oscillator.rotate();
            }

            mLog.debug("Finished!");
        }
    }

    public class SampleGenerationTask implements Runnable
    {
        @Override
        public void run()
        {
            generateSamples();
        }
    }

    public static void main(String[] args)
    {
        final ChannelizerViewer frame = new ChannelizerViewer(16);

        EventQueue.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                frame.setVisible(true);
                frame.start();
            }
        });
    }

}
