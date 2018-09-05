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

import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.channelizer.TwoChannelSynthesizerM2;
import io.github.dsheirer.dsp.filter.design.FilterDesignException;
import io.github.dsheirer.dsp.mixer.FS4DownConverter;
import io.github.dsheirer.dsp.mixer.IOscillator;
import io.github.dsheirer.dsp.mixer.LowPhaseNoiseOscillator;
import io.github.dsheirer.sample.IOverflowListener;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.SampleType;
import io.github.dsheirer.sample.buffer.ReusableComplexBuffer;
import io.github.dsheirer.sample.buffer.ReusableComplexBufferQueue;
import io.github.dsheirer.settings.SettingsManager;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.tuner.configuration.TunerConfigurationModel;
import io.github.dsheirer.spectrum.DFTProcessor;
import io.github.dsheirer.spectrum.DFTSize;
import io.github.dsheirer.spectrum.SpectrumPanel;
import io.github.dsheirer.spectrum.converter.ComplexDecibelConverter;
import io.github.dsheirer.util.ThreadPool;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.util.concurrent.TimeUnit;

public class SynthesizerViewer extends JFrame
{
    private final static Logger mLog = LoggerFactory.getLogger(SynthesizerViewer.class);

    private static final int CHANNEL_BANDWIDTH = 12500;
    private static final int CHANNEL_SAMPLE_RATE = 25000;
    private static final int CHANNEL_FFT_FRAME_RATE = 20; //frames per second
    private static final int DATA_GENERATOR_FRAME_RATE = 50; //frames per second

    private SettingsManager mSettingsManager = new SettingsManager(new TunerConfigurationModel());
    private JPanel mPrimaryPanel;
    private PrimarySpectrumPanel mPrimarySpectrumPanel;
    private ChannelPanel mChannel1Panel;
    private ChannelPanel mChannel2Panel;
    private ChannelControlPanel mChannel1ControlPanel;
    private ChannelControlPanel mChannel2ControlPanel;
    private DFTSize mMainPanelDFTSize = DFTSize.FFT08192;
    private DFTSize mChannelPanelDFTSize = DFTSize.FFT08192;

    /**
     * GUI Test utility for researching polyphase synthesizers.
     */
    public SynthesizerViewer()
    {
        init();
    }

    public void start()
    {
        ThreadPool.SCHEDULED.scheduleAtFixedRate(new DataGenerationManager(), 0, 1000 / DATA_GENERATOR_FRAME_RATE, TimeUnit.MILLISECONDS);
    }

    private void init()
    {
        setTitle("Polyphase Synthesizer Viewer");
        setSize(500, 400);
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
            mPrimaryPanel.setLayout(new MigLayout("insets 0 0 0 0", "[grow,fill][grow,fill]", "[grow,fill][grow,fill]"));
            mPrimaryPanel.add(getSpectrumPanel(), "span");
            mPrimaryPanel.add(getChannel1Panel());
            mPrimaryPanel.add(getChannel2Panel());
        }

        return mPrimaryPanel;
    }

    private PrimarySpectrumPanel getSpectrumPanel()
    {
        if(mPrimarySpectrumPanel == null)
        {
            mPrimarySpectrumPanel = new PrimarySpectrumPanel(mSettingsManager);
            mPrimarySpectrumPanel.setPreferredSize(new Dimension(500, 200));
            mPrimarySpectrumPanel.setDFTSize(mMainPanelDFTSize);
        }

        return mPrimarySpectrumPanel;
    }

    private ChannelPanel getChannel1Panel()
    {
        if(mChannel1Panel == null)
        {
            mChannel1Panel = new ChannelPanel(mSettingsManager, getChannel1ControlPanel());
            mChannel1Panel.setPreferredSize(new Dimension(250, 200));
            mChannel1Panel.setDFTSize(mChannelPanelDFTSize);
        }

        return mChannel1Panel;
    }

    private ChannelPanel getChannel2Panel()
    {
        if(mChannel2Panel == null)
        {
            mChannel2Panel = new ChannelPanel(mSettingsManager, getChannel2ControlPanel());
            mChannel2Panel.setPreferredSize(new Dimension(250, 200));
            mChannel2Panel.setDFTSize(mChannelPanelDFTSize);
        }

        return mChannel2Panel;
    }

    public class PrimarySpectrumPanel extends JPanel implements Listener<ReusableComplexBuffer>
    {
        private DFTProcessor mDFTProcessor = new DFTProcessor(SampleType.COMPLEX);
        private ComplexDecibelConverter mComplexDecibelConverter = new ComplexDecibelConverter();
        private SpectrumPanel mSpectrumPanel;

        public PrimarySpectrumPanel(SettingsManager settingsManager)
        {
            setLayout(new MigLayout("insets 0 0 0 0", "[grow,fill]", "[grow,fill]"));
            mSpectrumPanel = new SpectrumPanel(settingsManager);
            mSpectrumPanel.setSampleSize(16);
            add(mSpectrumPanel);

            mDFTProcessor.addConverter(mComplexDecibelConverter);
            mDFTProcessor.process(SourceEvent.sampleRateChange(CHANNEL_SAMPLE_RATE));
            mDFTProcessor.setFrameRate(CHANNEL_FFT_FRAME_RATE);
            mComplexDecibelConverter.addListener(mSpectrumPanel);

            mDFTProcessor.setOverflowListener(new IOverflowListener()
            {
                @Override
                public void sourceOverflow(boolean overflow)
                {
                    mLog.debug("Buffer " + (overflow ? "overflow" : "reset"));
                }
            });
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
    }

    public class ChannelPanel extends JPanel implements Listener<ReusableComplexBuffer>
    {
        private DFTProcessor mDFTProcessor = new DFTProcessor(SampleType.COMPLEX);
        private ComplexDecibelConverter mComplexDecibelConverter = new ComplexDecibelConverter();
        private SpectrumPanel mSpectrumPanel;
        private boolean mLoggingEnabled = false;

        public ChannelPanel(SettingsManager settingsManager, ChannelControlPanel channelControlPanel)
        {
            setLayout(new MigLayout("insets 0 0 0 0", "[grow,fill]", "[grow,fill][]"));
            mSpectrumPanel = new SpectrumPanel(settingsManager);
            mSpectrumPanel.setSampleSize(16);
            add(mSpectrumPanel, "wrap");
            add(channelControlPanel);

            mDFTProcessor.addConverter(mComplexDecibelConverter);
            mDFTProcessor.process(SourceEvent.sampleRateChange(CHANNEL_SAMPLE_RATE));
            mDFTProcessor.setFrameRate(CHANNEL_FFT_FRAME_RATE);

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
    }

    private ChannelControlPanel getChannel1ControlPanel()
    {
        if(mChannel1ControlPanel == null)
        {
            mChannel1ControlPanel = new ChannelControlPanel();
        }

        return mChannel1ControlPanel;
    }

    private ChannelControlPanel getChannel2ControlPanel()
    {
        if(mChannel2ControlPanel == null)
        {
            mChannel2ControlPanel = new ChannelControlPanel();
        }

        return mChannel2ControlPanel;
    }

    public class ChannelControlPanel extends JPanel
    {
        private static final int MIN_FREQUENCY = -6250;
        private static final int MAX_FREQUENCY = 6250;
        private static final int DEFAULT_FREQUENCY = 50;

        private IOscillator mOscillator = new LowPhaseNoiseOscillator(DEFAULT_FREQUENCY, CHANNEL_SAMPLE_RATE);

        public ChannelControlPanel()
        {
            setLayout(new MigLayout("insets 0 0 0 0", "[grow,fill]", "[grow,fill]"));
            add(new JLabel("Tone:"), "align right");

            SpinnerNumberModel model = new SpinnerNumberModel(DEFAULT_FREQUENCY, MIN_FREQUENCY, MAX_FREQUENCY, 100);
            model.addChangeListener(new ChangeListener()
            {
                @Override
                public void stateChanged(ChangeEvent e)
                {
                    long toneFrequency = model.getNumber().longValue();
                    mOscillator.setFrequency(toneFrequency);
                }
            });

            JSpinner spinner = new JSpinner(model);
            add(spinner);
            add(new JLabel("Hz"));
        }

        public IOscillator getOscillator()
        {
            return mOscillator;
        }
    }

    public class DataGenerationManager implements Runnable
    {
        private TwoChannelSynthesizerM2 mSynthesizer;
        private FS4DownConverter mFS4DownConverter = new FS4DownConverter();
        private int mSamplesPerCycle = CHANNEL_SAMPLE_RATE / DATA_GENERATOR_FRAME_RATE;
        private ReusableComplexBufferQueue mReusableComplexBufferQueue = new ReusableComplexBufferQueue("SynthesizerViewer");

        public DataGenerationManager()
        {
            try
            {
                float[] taps = FilterFactory.getSincM2Synthesizer( 25000.0, 12500.0, 2, 12);
                mSynthesizer = new TwoChannelSynthesizerM2(taps);
            }
            catch(FilterDesignException fde)
            {
                mLog.error("Filter design error", fde);
            }
        }

        @Override
        public void run()
        {
            ReusableComplexBuffer channel1Buffer = mReusableComplexBufferQueue.getBuffer(mSamplesPerCycle);
            getChannel1ControlPanel().getOscillator().generateComplex(channel1Buffer);

            ReusableComplexBuffer channel2Buffer = mReusableComplexBufferQueue.getBuffer(mSamplesPerCycle);
            getChannel2ControlPanel().getOscillator().generateComplex(channel2Buffer);

            channel1Buffer.incrementUserCount();
            channel2Buffer.incrementUserCount();

            ReusableComplexBuffer synthesizedBuffer = mSynthesizer.process(channel1Buffer, channel2Buffer);

            getChannel1Panel().receive(channel1Buffer);

            getChannel2Panel().receive(channel2Buffer);

            getSpectrumPanel().receive(synthesizedBuffer);
        }
    }

    public static void main(String[] args)
    {
        final SynthesizerViewer frame = new SynthesizerViewer();

        EventQueue.invokeLater(() -> {
            frame.setVisible(true);
            frame.start();
        });
    }
}