/*******************************************************************************
 * sdr-trunk
 * Copyright (C) 2014-2018 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by  the Free Software Foundation, either version 3 of the License, or  (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful,  but WITHOUT ANY WARRANTY; without even the implied
 * warranty of  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License  along with this program.
 * If not, see <http://www.gnu.org/licenses/>
 *
 ******************************************************************************/
package io.github.dsheirer.gui.channelizer;

import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.SampleType;
import io.github.dsheirer.sample.buffer.ReusableComplexBuffer;
import io.github.dsheirer.settings.SettingsManager;
import io.github.dsheirer.source.ISourceEventProcessor;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.Tuner;
import io.github.dsheirer.source.tuner.channel.TunerChannel;
import io.github.dsheirer.source.tuner.channel.TunerChannelSource;
import io.github.dsheirer.source.tuner.configuration.TunerConfigurationModel;
import io.github.dsheirer.source.tuner.test.TestTuner;
import io.github.dsheirer.spectrum.DFTProcessor;
import io.github.dsheirer.spectrum.DFTSize;
import io.github.dsheirer.spectrum.SpectrumPanel;
import io.github.dsheirer.spectrum.converter.ComplexDecibelConverter;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ChannelizerViewer extends JFrame
{
    private final static Logger mLog = LoggerFactory.getLogger(ChannelizerViewer.class);

    private static final int CHANNEL_BANDWIDTH = 12500;
    private static final int CHANNEL_FFT_FRAME_RATE = 20;

    private SettingsManager mSettingsManager = new SettingsManager(new TunerConfigurationModel());
    private JPanel mPrimaryPanel;
    private JPanel mControlPanel;
    private JLabel mToneFrequencyLabel;
    private PrimarySpectrumPanel mPrimarySpectrumPanel;
    private ChannelArrayPanel mChannelPanel;
    private DiscreteIndexChannelArrayPanel mDiscreteIndexChannelPanel;
    private int mChannelCount;
    private int mChannelsPerRow;
    private long mBaseFrequency = 100000000;  //100 MHz
    private DFTSize mMainPanelDFTSize = DFTSize.FFT32768;
    private DFTSize mChannelPanelDFTSize = DFTSize.FFT04096;
    private TestTuner mTestTuner;

    /**
     * GUI Test utility for researching polyphase channelizers.
     *
     * @param channelsPerRow
     */
    public ChannelizerViewer(int channelsPerRow)
    {
        mTestTuner = new TestTuner();

//        mChannelCount = (int)(mTestTuner.getTunerController().getUsableBandwidth() / CHANNEL_BANDWIDTH);
        mChannelCount = (int)(mTestTuner.getTunerController().getBandwidth() / CHANNEL_BANDWIDTH);
        mChannelsPerRow = channelsPerRow;

        init();
    }

    private void init()
    {
        setTitle("Polyphase Channelizer Viewer");
        setSize(1200, 800);
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
            mPrimaryPanel.setLayout(new MigLayout("insets 0 0 0 0", "[grow,fill]", "[][][grow,fill][grow,fill]"));
            mPrimaryPanel.add(getSpectrumPanel(), "wrap");
            mPrimaryPanel.add(getControlPanel(), "wrap");
            mPrimaryPanel.add(getChannelArrayPanel(), "wrap");
            mPrimaryPanel.add(getDiscreteIndexChannelPanel());
        }

        return mPrimaryPanel;
    }

    private PrimarySpectrumPanel getSpectrumPanel()
    {
        if(mPrimarySpectrumPanel == null)
        {
            mPrimarySpectrumPanel = new PrimarySpectrumPanel(mSettingsManager,
                mTestTuner.getTunerController().getSampleRate());
            mPrimarySpectrumPanel.setPreferredSize(new Dimension(1200, 200));
            mPrimarySpectrumPanel.setDFTSize(mMainPanelDFTSize);
            mTestTuner.getTunerController().addBufferListener(mPrimarySpectrumPanel);
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
            long minimumFrequency = -(long)mTestTuner.getTunerController().getSampleRate() / 2 + 1;
            long maximumFrequency = (long)mTestTuner.getTunerController().getSampleRate() / 2 - 1;
            long toneFrequency = 0;

            SpinnerNumberModel model = new SpinnerNumberModel(toneFrequency, minimumFrequency, maximumFrequency,
                100);

            model.addChangeListener(new ChangeListener()
            {
                @Override
                public void stateChanged(ChangeEvent e)
                {
                    long toneFrequency = model.getNumber().longValue();
                    mTestTuner.getTunerController().setToneFrequency(toneFrequency);
                    mToneFrequencyLabel.setText(String.valueOf(getToneFrequency()));
                }
            });

            JSpinner spinner = new JSpinner(model);

            mControlPanel.add(spinner);
            mControlPanel.add(new JLabel("Hz"));

            mControlPanel.add(new JLabel("Frequency:"), "push,align right");
            mToneFrequencyLabel = new JLabel(String.valueOf(getToneFrequency()));
            mControlPanel.add(mToneFrequencyLabel, "push,align left");

            mControlPanel.add(new JLabel("Channels: " + mChannelCount), "push,align right");
        }

        return mControlPanel;
    }

    private long getToneFrequency()
    {
        return mTestTuner.getTunerController().getFrequency() + mTestTuner.getTunerController().getToneFrequency();
    }

    private ChannelArrayPanel getChannelArrayPanel()
    {
        if(mChannelPanel == null)
        {
            mChannelPanel = new ChannelArrayPanel();
        }

        return mChannelPanel;
    }

    private DiscreteIndexChannelArrayPanel getDiscreteIndexChannelPanel()
    {
        if(mDiscreteIndexChannelPanel == null)
        {
            mDiscreteIndexChannelPanel = new DiscreteIndexChannelArrayPanel();
        }

        return mDiscreteIndexChannelPanel;
    }

    public class ChannelArrayPanel extends JPanel
    {
        private final Logger mLog = LoggerFactory.getLogger(ChannelArrayPanel.class);

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

            double spectralBandwidth = mTestTuner.getTunerController().getSampleRate();
            double halfSpectralBandwidth = spectralBandwidth / 2.0;

            int channelToLog = -1;

            long baseFrequency = mBaseFrequency + (CHANNEL_BANDWIDTH / 2);

            for(int x = 0; x < mChannelCount; x++)
            {
                long frequency = baseFrequency + (x * CHANNEL_BANDWIDTH);

                mLog.debug("Channel " + x + "/" + mChannelCount + " Frequency: " + frequency);

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

    public class DiscreteIndexChannelArrayPanel extends JPanel
    {
        public DiscreteIndexChannelArrayPanel()
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

            for(int x = 0; x < mChannelCount; x++)
            {
                TunerChannel tunerChannel = new TunerChannel(100000000, 12500);
                TunerChannelSource source = mTestTuner.getChannelSourceManager().getSource(tunerChannel, null);
                DiscreteChannelPanel channelPanel = new DiscreteChannelPanel(mSettingsManager, source, x);
                channelPanel.setDFTSize(mChannelPanelDFTSize);

                mLog.debug("Testing Channel [" + x + "] is set to [" + source.getTunerChannel().getFrequency() + "]");

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

    /**
     * Returns a list of tuner channels that will fit within the tuner's bandwidth, minus a half channel each at the
     * lower and upper ends of the spectrum.
     *
     * @param tuner to create channels for
     * @return list of contiguous channels filling the tuner bandwidth
     */
    public static List<TunerChannel> getTunerChannels(Tuner tuner)
    {
        List<TunerChannel> tunerChannels = new ArrayList<>();

        long baseFrequency = tuner.getTunerController().getFrequency();
        baseFrequency -= tuner.getTunerController().getSampleRate() / 2;
        baseFrequency += (CHANNEL_BANDWIDTH / 2);

        int channelCount = (int)(tuner.getTunerController().getSampleRate() / CHANNEL_BANDWIDTH) - 1;

        for(int x = 0; x < channelCount; x++)
        {
            long frequency = baseFrequency + (x * CHANNEL_BANDWIDTH);
            TunerChannel tunerChannel = new TunerChannel(frequency, CHANNEL_BANDWIDTH);
            tunerChannels.add(tunerChannel);
        }

        return tunerChannels;
    }

    public class PrimarySpectrumPanel extends JPanel implements Listener<ReusableComplexBuffer>, ISourceEventProcessor
    {
        private DFTProcessor mDFTProcessor = new DFTProcessor(SampleType.COMPLEX);
        private ComplexDecibelConverter mComplexDecibelConverter = new ComplexDecibelConverter();
        private SpectrumPanel mSpectrumPanel;

        public PrimarySpectrumPanel(SettingsManager settingsManager, double sampleRate)
        {
            setLayout(new MigLayout("insets 0 0 0 0", "[grow,fill]", "[grow,fill]"));
            mSpectrumPanel = new SpectrumPanel(settingsManager);
            mSpectrumPanel.setSampleSize(28);
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
        public void receive(ReusableComplexBuffer reusableComplexBuffer)
        {
            mDFTProcessor.receive(reusableComplexBuffer);
        }

        @Override
        public void process(SourceEvent event) throws SourceException
        {
            mLog.debug("Source Event!  Add handler support for this to channelizer viewer");
        }
    }

    public class ChannelPanel extends JPanel implements Listener<ReusableComplexBuffer>, ISourceEventProcessor
    {
        private TunerChannelSource mSource;
        private DFTProcessor mDFTProcessor = new DFTProcessor(SampleType.COMPLEX);
        private ComplexDecibelConverter mComplexDecibelConverter = new ComplexDecibelConverter();
        private SpectrumPanel mSpectrumPanel;
        private JToggleButton mLoggingButton;
        private boolean mLoggingEnabled;

        public ChannelPanel(SettingsManager settingsManager, double sampleRate, long frequency, int bandwidth, boolean enableLogging)
        {
            setLayout(new MigLayout("insets 0 0 0 0", "[center,grow,fill][]", "[grow,fill][]"));
            mSpectrumPanel = new SpectrumPanel(settingsManager);
            mSpectrumPanel.setSampleSize(32);
            add(mSpectrumPanel, "span");

            mDFTProcessor.addConverter(mComplexDecibelConverter);
            mDFTProcessor.process(SourceEvent.sampleRateChange(sampleRate));
            mComplexDecibelConverter.addListener(mSpectrumPanel);

            TunerChannel tunerChannel = new TunerChannel(frequency, bandwidth);
            mSource = mTestTuner.getChannelSourceManager().getSource(tunerChannel, null);

            if(mSource != null)
            {
                mSource.setListener(new Listener<ReusableComplexBuffer>()
                {
                    @Override
                    public void receive(ReusableComplexBuffer complexBuffer)
                    {
                        if(mLoggingEnabled)
                        {
                            mLog.debug("Samples:" + Arrays.toString(complexBuffer.getSamples()));
                        }

                        complexBuffer.incrementUserCount();
                        mDFTProcessor.receive(complexBuffer);
                        complexBuffer.decrementUserCount();
                    }
                });

                mSource.start();
            }
            else
            {
                mLog.error("Couldn't get a source from the tuner for frequency: " + frequency);
            }

            if(mSource != null)
            {
                add(new JLabel("Center:" + frequency));
            }
            else
            {
                add(new JLabel("NO SRC:" + frequency));
            }

            mLoggingButton = new JToggleButton("Logging");
            mLoggingButton.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    mLoggingEnabled = mLoggingButton.isSelected();
                }
            });
//            add(mLoggingButton);

        }

        public TunerChannelSource getSource()
        {
            return mSource;
        }

        public void setDFTSize(DFTSize dftSize)
        {
            mDFTProcessor.setDFTSize(dftSize);
        }

        @Override
        public void receive(ReusableComplexBuffer reusableComplexBuffer)
        {
            reusableComplexBuffer.incrementUserCount();
            mDFTProcessor.receive(reusableComplexBuffer);
            reusableComplexBuffer.decrementUserCount();
        }

        @Override
        public void process(SourceEvent event) throws SourceException
        {
            mLog.debug("Source Event!  Add handler support for this to channelizer viewer");
        }
    }

    public class DiscreteChannelPanel extends JPanel implements Listener<ReusableComplexBuffer>, ISourceEventProcessor
    {
        private final Logger mLog = LoggerFactory.getLogger(DiscreteChannelPanel.class);

        private TunerChannelSource mSource;
        private DFTProcessor mDFTProcessor = new DFTProcessor(SampleType.COMPLEX);
        private ComplexDecibelConverter mComplexDecibelConverter = new ComplexDecibelConverter();
        private SpectrumPanel mSpectrumPanel;
        private JToggleButton mLoggingButton;
        private boolean mLoggingEnabled;

        public DiscreteChannelPanel(SettingsManager settingsManager, TunerChannelSource source, int index)
        {
            mSource = source;
            setLayout(new MigLayout("insets 0 0 0 0", "[center,grow,fill][]", "[grow,fill][]"));
            mSpectrumPanel = new SpectrumPanel(settingsManager);
            mSpectrumPanel.setSampleSize(32);
            add(mSpectrumPanel, "span");
            add(new JLabel("Index:" + index));

            mLoggingButton = new JToggleButton("Logging");
            mLoggingButton.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    mLoggingEnabled = mLoggingButton.isSelected();
                }
            });
//            add(mLoggingButton);

            mDFTProcessor.addConverter(mComplexDecibelConverter);
            mDFTProcessor.process(SourceEvent.sampleRateChange(25000.0));
            mComplexDecibelConverter.addListener(mSpectrumPanel);

            if(mSource != null)
            {
                mSource.setListener(new Listener<ReusableComplexBuffer>()
                {
                    @Override
                    public void receive(ReusableComplexBuffer complexBuffer)
                    {
                        if(mLoggingEnabled)
                        {
                            mLog.debug("Samples:" + Arrays.toString(complexBuffer.getSamples()));
                        }

                        complexBuffer.incrementUserCount();
                        mDFTProcessor.receive(complexBuffer);
                        complexBuffer.decrementUserCount();
                    }
                });

                mSource.start();
            }
            else
            {
                mLog.error("Couldn't get a source from the tuner for index: " + index);
            }
        }

        public TunerChannelSource getSource()
        {
            return mSource;
        }

        public void setDFTSize(DFTSize dftSize)
        {
            mDFTProcessor.setDFTSize(dftSize);
        }

        @Override
        public void receive(ReusableComplexBuffer reusableComplexBuffer)
        {
            reusableComplexBuffer.incrementUserCount();
            mDFTProcessor.receive(reusableComplexBuffer);
            reusableComplexBuffer.decrementUserCount();
        }

        @Override
        public void process(SourceEvent event) throws SourceException
        {
            mLog.debug("Source Event!  Add handler support for this to channelizer viewer");
        }
    }


    public static void main(String[] args)
    {
        boolean useGUI = true;

        if(useGUI)
        {
            int channelsPerRow = 16;

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
        else
        {
            TestTuner tuner = new TestTuner();

            List<TunerChannel> tunerChannels = getTunerChannels(tuner);

            List<TunerChannelSource> sources = new ArrayList<>();

            int maxSourceCount = 30;
            int sourceCount = 0;
            for(TunerChannel tunerChannel : tunerChannels)
            {
                if(sourceCount < maxSourceCount)
                {
                    TunerChannelSource source = tuner.getChannelSourceManager().getSource(tunerChannel, null);

                    if(source != null)
                    {
                        sources.add(source);
                        sourceCount++;
                    }
                    else
                    {
                        mLog.debug("Couldn't get source for: " + tunerChannel);
                    }
                }
            }

            mLog.debug("Starting [" + sources.size() + "] tuner channel sources");

            for(TunerChannelSource tunerChannelSource : sources)
            {
                tunerChannelSource.setListener(new Listener<ReusableComplexBuffer>()
                {
                    @Override
                    public void receive(ReusableComplexBuffer reusableComplexBuffer)
                    {
                        reusableComplexBuffer.decrementUserCount();
                    }
                });

                tunerChannelSource.start();
            }

            while(true)
            {
                ;
            }
        }
    }
}