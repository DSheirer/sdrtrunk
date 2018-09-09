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

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
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

public class ChannelizerViewer2 extends JFrame
{
    private final static Logger mLog = LoggerFactory.getLogger(ChannelizerViewer2.class);

    private static final int CHANNEL_BANDWIDTH = 12500;

    private SettingsManager mSettingsManager = new SettingsManager(new TunerConfigurationModel());
    private JPanel mPrimaryPanel;
    private JPanel mControlPanel;
    private JButton mTopFrameAddChannelButton;
    private JButton mBottomFrameAddChannelButton;
    private JLabel mToneFrequencyLabel;
    private JLabel mCenterFrequencyLabel;
    private PrimarySpectrumPanel mPrimarySpectrumPanel;
    private ChannelArrayPanel mTopChannelArrayPanel;
    private ChannelArrayPanel mBottomChannelArrayPanel;
    private DFTSize mMainPanelDFTSize = DFTSize.FFT32768;
    private DFTSize mChannelPanelDFTSize = DFTSize.FFT04096;
    private TestTuner mTestTuner = new TestTuner();

    /**
     * GUI Test utility for researching polyphase channelizers.
     */
    public ChannelizerViewer2()
    {
        init();
    }

    private void init()
    {
        setTitle("Polyphase Channelizer Viewer");
        setSize(1200, 700);
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
            mPrimaryPanel.add(getTopChannelArrayPanel(), "wrap");
            mPrimaryPanel.add(getBottomChannelArrayPanel());
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
                    mCenterFrequencyLabel.setText(String.valueOf(getCenterFrequency()));
                }
            });

            JSpinner spinner = new JSpinner(model);

            mControlPanel.add(spinner);
            mControlPanel.add(new JLabel("Hz"));

            mControlPanel.add(new JLabel("Tone Frequency:"), "push,align right");
            mToneFrequencyLabel = new JLabel(String.valueOf(getToneFrequency()));
            mControlPanel.add(mToneFrequencyLabel, "push,align left");

            mControlPanel.add(new JLabel("Center Frequency:"), "push,align right");
            mCenterFrequencyLabel = new JLabel(String.valueOf(getCenterFrequency()));
            mControlPanel.add(mCenterFrequencyLabel, "push,align left");

            mControlPanel.add(getBottomFrameAddChannelButton(), "push,align right");
            mControlPanel.add(getTopFrameAddChannelButton(), "push,align right");
        }

        return mControlPanel;
    }

    private JButton getTopFrameAddChannelButton()
    {
        if(mTopFrameAddChannelButton == null)
        {
            mTopFrameAddChannelButton = new JButton("Top - Add Channel");

            mTopFrameAddChannelButton.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    String value = JOptionPane.showInputDialog(ChannelizerViewer2.this, "Frequency?");

                    if(value != null && !value.isEmpty())
                    {
                        try
                        {
                            int frequency = Integer.parseInt(value);

                            TunerChannel tunerChannel = new TunerChannel(frequency, CHANNEL_BANDWIDTH);

                            getTopChannelArrayPanel().addChannel(tunerChannel);
                        }
                        catch(Exception exception)
                        {
                            mLog.error("Can't parse frequency from value: " + value);
                        }
                    }
                }
            });
        }

        return mTopFrameAddChannelButton;
    }

    private JButton getBottomFrameAddChannelButton()
    {
        if(mBottomFrameAddChannelButton == null)
        {
            mBottomFrameAddChannelButton = new JButton("Bottom - Add Channel");

            mBottomFrameAddChannelButton.addActionListener(new ActionListener()
            {
                @Override
                public void actionPerformed(ActionEvent e)
                {
                    String value = JOptionPane.showInputDialog(ChannelizerViewer2.this, "Frequency?");

                    if(value != null && !value.isEmpty())
                    {
                        try
                        {
                            int frequency = Integer.parseInt(value);

                            TunerChannel tunerChannel = new TunerChannel(frequency, CHANNEL_BANDWIDTH);

                            getBottomChannelArrayPanel().addChannel(tunerChannel);
                        }
                        catch(Exception exception)
                        {
                            mLog.error("Can't parse frequency from value: " + value);
                        }
                    }
                }
            });
        }

        return mBottomFrameAddChannelButton;
    }

    private long getToneFrequency()
    {
        return mTestTuner.getTunerController().getFrequency() + mTestTuner.getTunerController().getToneFrequency();
    }

    private long getCenterFrequency()
    {
        return mTestTuner.getTunerController().getFrequency();
    }

    private ChannelArrayPanel getTopChannelArrayPanel()
    {
        if(mTopChannelArrayPanel == null)
        {
            mTopChannelArrayPanel = new ChannelArrayPanel();
        }

        return mTopChannelArrayPanel;
    }

    private ChannelArrayPanel getBottomChannelArrayPanel()
    {
        if(mBottomChannelArrayPanel == null)
        {
            mBottomChannelArrayPanel = new ChannelArrayPanel();
        }

        return mBottomChannelArrayPanel;
    }

    public class ChannelArrayPanel extends JPanel
    {
        public ChannelArrayPanel()
        {
            init();
        }

        private void init()
        {
            setLayout(new MigLayout("insets 0 0 0 0", "fill", "fill"));
        }

        public void addChannel(TunerChannel tunerChannel)
        {
            ChannelPanel channelPanel = new ChannelPanel(mSettingsManager, CHANNEL_BANDWIDTH * 2,
                tunerChannel.getFrequency(), CHANNEL_BANDWIDTH);
            channelPanel.setDFTSize(mChannelPanelDFTSize);

            add(channelPanel, "grow,push");

            validate();
            repaint();
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
            mSpectrumPanel.setSampleSize(16);
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

        public ChannelPanel(SettingsManager settingsManager, double sampleRate, long frequency, int bandwidth)
        {
            setLayout(new MigLayout("insets 0 0 0 0", "[grow,fill][grow,fill][grow,fill]", "[grow,fill][]"));
            mSpectrumPanel = new SpectrumPanel(settingsManager);
            mSpectrumPanel.setSampleSize(16);
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
                int half = (int)(sampleRate / 2.0f);
                add(new JLabel("Min:" + (frequency - half)), "align left");
                add(new JLabel("Center:" + frequency));
                add(new JLabel("Max:" + (frequency + half)), "align right");
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
        final ChannelizerViewer2 frame = new ChannelizerViewer2();

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