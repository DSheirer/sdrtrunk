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
import dsp.filter.Window;
import dsp.filter.design.FilterDesignException;
import dsp.mixer.LowPhaseNoiseOscillator;
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
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ChannelizerViewer extends JFrame
{
    private final static Logger mLog = LoggerFactory.getLogger(ChannelizerViewer.class);

    private static final int CHANNEL_BANDWIDTH = 12500;
    private static final int CHANNEL_FFT_FRAME_RATE = 20;

    private SettingsManager mSettingsManager = new SettingsManager(new TunerConfigurationModel());
    private JPanel mPrimaryPanel;
    private JPanel mControlPanel;
    private ChannelPanel mPrimarySpectrumPanel;
    private ChannelArrayPanel mChannelPanel;
    private LowPhaseNoiseOscillator mOscillator;
    private int mChannelCount;
    private int mChannelsPerRow;
    private int mSampleRate;
    private int mToneFrequency;
    private DFTSize mMainPanelDFTSize = DFTSize.FFT32768;
    private DFTSize mChannelPanelDFTSize = DFTSize.FFT00512;

    /**
     * GUI Test utility for researching polyphase channelizers.
     *
     * @param channelCount
     * @param channelsPerRow
     */
    public ChannelizerViewer(int channelCount, int channelsPerRow)
    {
        mChannelCount = channelCount;
        mChannelsPerRow = channelsPerRow;
        mSampleRate = mChannelCount * CHANNEL_BANDWIDTH;
        mToneFrequency = (int)(CHANNEL_BANDWIDTH * 1); //Set to channel 1 as default

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

        mOscillator = new LowPhaseNoiseOscillator(mSampleRate, mToneFrequency);
    }

    public void start()
    {
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        scheduledExecutorService.scheduleAtFixedRate(new SampleGenerationTask(), 0, 50, TimeUnit.MILLISECONDS);
    }

    public void setToneFrequency(int frequency)
    {
        mOscillator.setFrequency(frequency);
        mToneFrequency = frequency;
    }

    private void generateSamples()
    {
        int size = mSampleRate / CHANNEL_FFT_FRAME_RATE;

        float[] samples = mOscillator.generate(size);

        ComplexBuffer buffer = new ComplexBuffer(samples);

        getChannelArrayPanel().receive(buffer);
        getSpectrumPanel().receive(buffer);
    }

    private JPanel getPrimaryPanel()
    {
        if(mPrimaryPanel == null)
        {
            mPrimaryPanel = new JPanel();
            mPrimaryPanel.setLayout(new MigLayout("insets 0 0 0 0", "[grow,fill]", "[][][grow,fill]"));
            mPrimaryPanel.add(getSpectrumPanel(), "wrap");
            mPrimaryPanel.add(getControlPanel(), "wrap");
            mPrimaryPanel.add(getChannelArrayPanel());
        }

        return mPrimaryPanel;
    }

    private ChannelPanel getSpectrumPanel()
    {
        if(mPrimarySpectrumPanel == null)
        {
            mPrimarySpectrumPanel = new ChannelPanel(mSettingsManager, mSampleRate);
            mPrimarySpectrumPanel.setPreferredSize(new Dimension(1000, 200));
            mPrimarySpectrumPanel.setDFTSize(mMainPanelDFTSize);
        }

        return mPrimarySpectrumPanel;
    }

    private JPanel getControlPanel()
    {
        if(mControlPanel == null)
        {
            mControlPanel = new JPanel();
            mControlPanel.setLayout(new MigLayout("insets 0 0 0 0", "", ""));

            mControlPanel.add(new JLabel("Tone:"), "align left");
            int maximumFrequency = mSampleRate / 2;

            SpinnerNumberModel model = new SpinnerNumberModel(mToneFrequency, -maximumFrequency, maximumFrequency,
                100 );

            model.addChangeListener(new ChangeListener()
            {
                @Override
                public void stateChanged(ChangeEvent e)
                {
                    int frequency = model.getNumber().intValue();
                    mOscillator.setFrequency(frequency);
                }
            });

            JSpinner spinner = new JSpinner(model);

            mControlPanel.add(spinner);
            mControlPanel.add(new JLabel("Hz"));

            mControlPanel.add(new JLabel("Sample Rate: " + mSampleRate + " Hz"), "push,align right");

            mControlPanel.add(new JLabel("Max: " + (mSampleRate / 2) + " Hz"), "push,align right");
            mControlPanel.add(new JLabel("Channels: " + mChannelCount), "push,align right");
        }

        return mControlPanel;
    }

    private ChannelArrayPanel getChannelArrayPanel()
    {
        if(mChannelPanel == null)
        {
            float[] taps = null;

            try
            {
                taps = getFilter();
            }
            catch(FilterDesignException fde)
            {
                mLog.debug("Couldn't design filter", fde);
            }

            mChannelPanel = new ChannelArrayPanel(taps);
        }

        return mChannelPanel;
    }

    private float[] getFilter() throws FilterDesignException
    {
        return FilterFactory.getChannelizer(CHANNEL_BANDWIDTH, mChannelCount, 19,
            Window.WindowType.BLACKMAN_HARRIS_7, true);
    }

    public class ChannelArrayPanel extends JPanel implements Listener<ComplexBuffer>
    {
        private PolyphaseChannelizer2 mPolyphaseChannelizer;
        private ChannelDistributor mChannelDistributor;

        public ChannelArrayPanel(float[] taps)
        {
            int bufferSize = CHANNEL_BANDWIDTH / CHANNEL_FFT_FRAME_RATE;
            if(bufferSize % 2 == 1)
            {
                bufferSize++;
            }

            mPolyphaseChannelizer = new PolyphaseChannelizer2(taps, mChannelCount);
            mChannelDistributor = new ChannelDistributor(bufferSize, mChannelCount);
            mPolyphaseChannelizer.setChannelDistributor(mChannelDistributor);

            init();
        }

        private void init()
        {
            setLayout(new MigLayout("insets 0 0 0 0", "fill", "fill"));

            for(int x = 0; x < mChannelCount; x++)
            {
                ChannelPanel channelPanel = new ChannelPanel(mSettingsManager, CHANNEL_BANDWIDTH * 2);
                channelPanel.setDFTSize(mChannelPanelDFTSize);

                if(x % mChannelsPerRow == mChannelsPerRow - 1)
                {
                    add(channelPanel, "grow,push,wrap 2px");
                }
                else
                {
                    add(channelPanel, "grow,push");
                }

                mChannelDistributor.addListener(x, channelPanel);
            }
        }

        @Override
        public void receive(ComplexBuffer complexBuffer)
        {
            mPolyphaseChannelizer.receive(complexBuffer);
        }
    }

    public class ChannelPanel extends JPanel implements Listener<ComplexBuffer>
    {
        private DFTProcessor mDFTProcessor = new DFTProcessor(SampleType.COMPLEX);
        private ComplexDecibelConverter mComplexDecibelConverter = new ComplexDecibelConverter();
        private SpectrumPanel mSpectrumPanel;

        public ChannelPanel(SettingsManager settingsManager, int sampleRate)
        {
            setLayout(new MigLayout("insets 0 0 0 0", "[grow,fill]", "[grow,fill]"));
            mSpectrumPanel = new SpectrumPanel(settingsManager);
            add(mSpectrumPanel);

            mDFTProcessor.addConverter(mComplexDecibelConverter);
            mDFTProcessor.frequencyChanged(new FrequencyChangeEvent(
                FrequencyChangeEvent.Event.NOTIFICATION_SAMPLE_RATE_CHANGE, sampleRate));
            mComplexDecibelConverter.addListener(mSpectrumPanel);
        }

        public void setDFTSize(DFTSize dftSize)
        {
            mDFTProcessor.setDFTSize(dftSize);
        }

        @Override
        public void receive(ComplexBuffer complexBuffer)
        {
            mDFTProcessor.receive(complexBuffer);
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
        int channelCount = 100;
        int channelsPerRow = 25;

        final ChannelizerViewer frame = new ChannelizerViewer(channelCount, channelsPerRow);

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
