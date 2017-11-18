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

import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sample.Listener;
import sample.SampleType;
import sample.complex.ComplexBuffer;
import settings.SettingsManager;
import source.ISourceEventProcessor;
import source.SourceEvent;
import source.SourceException;
import source.tuner.TunerChannel;
import source.tuner.configuration.TunerConfigurationModel;
import source.tuner.test.TestTuner;
import spectrum.DFTProcessor;
import spectrum.DFTSize;
import spectrum.SpectrumPanel;
import spectrum.converter.ComplexDecibelConverter;
import util.ThreadPool;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;

public class ChannelizerViewer extends JFrame
{
    private final static Logger mLog = LoggerFactory.getLogger(ChannelizerViewer.class);

    private static final int CHANNEL_BANDWIDTH = 12500;
    private static final int CHANNEL_FFT_FRAME_RATE = 20;

    private SettingsManager mSettingsManager = new SettingsManager(new TunerConfigurationModel());
    private JPanel mPrimaryPanel;
    private JPanel mControlPanel;
    private PrimarySpectrumPanel mPrimarySpectrumPanel;
    private ChannelArrayPanel mChannelPanel;
    private int mChannelCount;
    private int mChannelsPerRow;
    private long mToneFrequency;
    private DFTSize mMainPanelDFTSize = DFTSize.FFT32768;
    private DFTSize mChannelPanelDFTSize = DFTSize.FFT00512;
    private TestTuner mTestTuner;

    /**
     * GUI Test utility for researching polyphase channelizers.
     *
     * @param channelsPerRow
     */
    public ChannelizerViewer(int channelsPerRow)
    {
        mTestTuner = new TestTuner();

        mChannelCount = mTestTuner.getSampleRate() / CHANNEL_BANDWIDTH;
        mChannelsPerRow = channelsPerRow;

        mToneFrequency = mTestTuner.getTunerController().getFrequency() +
            (int)(CHANNEL_BANDWIDTH * 1); //Set to channel 1 as default

        mTestTuner.setToneFrequency(mToneFrequency);

        init();
    }

    private void init()
    {
        setTitle("TunerChannelizer Viewer");
        setSize(1000, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new MigLayout("insets 0 0 0 0", "[grow,fill]", "[grow,fill]"));
        setLocationRelativeTo(null);
        add(getPrimaryPanel());
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

    private PrimarySpectrumPanel getSpectrumPanel()
    {
        if(mPrimarySpectrumPanel == null)
        {
            mPrimarySpectrumPanel = new PrimarySpectrumPanel(mSettingsManager, mTestTuner.getSampleRate());
            mPrimarySpectrumPanel.setPreferredSize(new Dimension(1000, 200));
            mPrimarySpectrumPanel.setDFTSize(mMainPanelDFTSize);
            mTestTuner.addListener(mPrimarySpectrumPanel);
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
            int maximumFrequencyOffset = mTestTuner.getSampleRate() / 2;
            long minimumFrequency = mTestTuner.getTunerController().getFrequency() - maximumFrequencyOffset;
            long maximumFrequency = mTestTuner.getTunerController().getFrequency() + maximumFrequencyOffset;

            SpinnerNumberModel model = new SpinnerNumberModel(mToneFrequency, minimumFrequency, maximumFrequency,
                100 );

            model.addChangeListener(new ChangeListener()
            {
                @Override
                public void stateChanged(ChangeEvent e)
                {
                    int toneFrequency = model.getNumber().intValue();
                    mTestTuner.setToneFrequency(toneFrequency);
                }
            });

            JSpinner spinner = new JSpinner(model);

            mControlPanel.add(spinner);
            mControlPanel.add(new JLabel("Hz"));

            mControlPanel.add(new JLabel("Sample Rate: " + mTestTuner.getSampleRate() + " Hz"), "push,align right");

            mControlPanel.add(new JLabel("Max: " + (mTestTuner.getSampleRate() / 2) + " Hz"), "push,align right");
            mControlPanel.add(new JLabel("Channels: " + mChannelCount), "push,align right");
        }

        return mControlPanel;
    }

    private ChannelArrayPanel getChannelArrayPanel()
    {
        if(mChannelPanel == null)
        {
            mChannelPanel = new ChannelArrayPanel();
        }

        return mChannelPanel;
    }

    public class ChannelArrayPanel extends JPanel
    {
        public ChannelArrayPanel()
        {
            int bufferSize = CHANNEL_BANDWIDTH / CHANNEL_FFT_FRAME_RATE;
            if(bufferSize % 2 == 1)
            {
                bufferSize++;
            }

            init();
        }

        private void init()
        {
            setLayout(new MigLayout("insets 0 0 0 0", "fill", "fill"));

            long spectralBandwidth = mTestTuner.getSampleRate();
            long halfSpectralBandwidth = spectralBandwidth / 2;

            int channelToLog = -1;

            for(int x = 0; x < mChannelCount; x++)
            {
                //place the channels left and right of 10.0 MHz
                long frequency = mTestTuner.getTunerController().getFrequency() +
                    ((x * CHANNEL_BANDWIDTH) - halfSpectralBandwidth) + (CHANNEL_BANDWIDTH / 2);

                mLog.debug("Channel " + x + " Frequency: " + frequency);

                ChannelPanel channelPanel = new ChannelPanel(mSettingsManager, CHANNEL_BANDWIDTH * 2, frequency, CHANNEL_BANDWIDTH, (x == channelToLog));
                channelPanel.setDFTSize(mChannelPanelDFTSize);

                if(x % mChannelsPerRow == mChannelsPerRow - 1)
                {
                    add(channelPanel, "grow,push,wrap 2px");
                }
                else
                {
                    add(channelPanel, "grow,push");
                }
            }
        }
    }

    public class PrimarySpectrumPanel extends JPanel implements Listener<ComplexBuffer>, ISourceEventProcessor
    {
        private DFTProcessor mDFTProcessor = new DFTProcessor(SampleType.COMPLEX);
        private ComplexDecibelConverter mComplexDecibelConverter = new ComplexDecibelConverter();
        private SpectrumPanel mSpectrumPanel;

        public PrimarySpectrumPanel(SettingsManager settingsManager, int sampleRate)
        {
            setLayout(new MigLayout("insets 0 0 0 0", "[grow,fill]", "[grow,fill]"));
            mSpectrumPanel = new SpectrumPanel(settingsManager);
            add(mSpectrumPanel);

            mDFTProcessor.addConverter(mComplexDecibelConverter);
            mDFTProcessor.process(SourceEvent.sampleRateChange(sampleRate));
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

        @Override
        public void process(SourceEvent event) throws SourceException
        {
            mLog.debug("Source Event!  Add handler support for this to channelizer viewer");
        }
    }

    public class ChannelPanel extends JPanel implements Listener<ComplexBuffer>, ISourceEventProcessor
    {
        private PolyphaseChannelSource mSource;
        private DFTProcessor mDFTProcessor = new DFTProcessor(SampleType.COMPLEX);
        private ComplexDecibelConverter mComplexDecibelConverter = new ComplexDecibelConverter();
        private SpectrumPanel mSpectrumPanel;
        private long mFrequency;

        public ChannelPanel(SettingsManager settingsManager, int sampleRate, long frequency, int bandwidth, boolean enableLogging)
        {
            setLayout(new MigLayout("insets 0 0 0 0", "[grow,fill]", "[grow,fill]"));
            mSpectrumPanel = new SpectrumPanel(settingsManager);
            add(mSpectrumPanel);

            mDFTProcessor.addConverter(mComplexDecibelConverter);
            mDFTProcessor.process(SourceEvent.sampleRateChange(sampleRate));

            mComplexDecibelConverter.addListener(mSpectrumPanel);

            mFrequency = frequency;

            TunerChannel tunerChannel = new TunerChannel(frequency, bandwidth);
            mSource = (PolyphaseChannelSource)mTestTuner.getChannelSource(tunerChannel);

            if(mSource != null)
            {
                if(enableLogging)
                {
                    mSource.setListener(new Listener<ComplexBuffer>()
                    {
                        @Override
                        public void receive(ComplexBuffer complexBuffer)
                        {
                            mLog.debug("Sending samples to DFT processor: " + complexBuffer.getSamples().length);
                            mDFTProcessor.receive(complexBuffer);
                        }
                    });
                }
                else
                {
                    mSource.setListener(mDFTProcessor);
                }
                mSource.start(ThreadPool.SCHEDULED);
            }
            else
            {
                mLog.error("Couldn't get a source from the tuner for frequency: " + frequency);
            }
        }

        public PolyphaseChannelSource getSource()
        {
            return mSource;
        }

        public void setDFTSize(DFTSize dftSize)
        {
            mDFTProcessor.setDFTSize(dftSize);
        }

        @Override
        public void receive(ComplexBuffer complexBuffer)
        {
            mLog.debug("Got a buffer for the channel dft processor");
            mDFTProcessor.receive(complexBuffer);
        }

        @Override
        public void process(SourceEvent event) throws SourceException
        {
            mLog.debug("Source Event!  Add handler support for this to channelizer viewer");
        }
    }

    public static void main(String[] args)
    {
        int channelsPerRow = 4;

        final ChannelizerViewer frame = new ChannelizerViewer(channelsPerRow);

        EventQueue.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                frame.setVisible(true);
            }
        });
    }

}
