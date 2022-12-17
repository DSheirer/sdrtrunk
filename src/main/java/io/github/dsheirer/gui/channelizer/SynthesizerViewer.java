/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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
package io.github.dsheirer.gui.channelizer;

import io.github.dsheirer.buffer.FloatNativeBuffer;
import io.github.dsheirer.buffer.INativeBuffer;
import io.github.dsheirer.dsp.filter.FilterFactory;
import io.github.dsheirer.dsp.filter.channelizer.TwoChannelSynthesizerM2;
import io.github.dsheirer.dsp.filter.design.FilterDesignException;
import io.github.dsheirer.dsp.oscillator.FS4DownConverter;
import io.github.dsheirer.dsp.oscillator.IComplexOscillator;
import io.github.dsheirer.dsp.oscillator.OscillatorFactory;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.ComplexSamples;
import io.github.dsheirer.settings.SettingsManager;
import io.github.dsheirer.spectrum.ComplexDftProcessor;
import io.github.dsheirer.spectrum.DFTSize;
import io.github.dsheirer.spectrum.SpectrumPanel;
import io.github.dsheirer.spectrum.converter.ComplexDecibelConverter;
import io.github.dsheirer.util.ThreadPool;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.util.concurrent.TimeUnit;
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

public class SynthesizerViewer extends JFrame
{
    private final static Logger mLog = LoggerFactory.getLogger(SynthesizerViewer.class);

    private static final int CHANNEL_BANDWIDTH = 12500;
    private static final int CHANNEL_SAMPLE_RATE = 25000;
    private static final int CHANNEL_FFT_FRAME_RATE = 20; //frames per second
    private static final int DATA_GENERATOR_FRAME_RATE = 50; //frames per second

    private SettingsManager mSettingsManager = new SettingsManager();
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

    public class PrimarySpectrumPanel extends JPanel implements Listener<INativeBuffer>
    {
        private ComplexDftProcessor mComplexDftProcessor = new ComplexDftProcessor();
        private ComplexDecibelConverter mComplexDecibelConverter = new ComplexDecibelConverter();
        private SpectrumPanel mSpectrumPanel;

        public PrimarySpectrumPanel(SettingsManager settingsManager)
        {
            setLayout(new MigLayout("insets 0 0 0 0", "[grow,fill]", "[grow,fill]"));
            mSpectrumPanel = new SpectrumPanel(settingsManager);
            mSpectrumPanel.setSampleSize(16);
            add(mSpectrumPanel);

            mComplexDftProcessor.addConverter(mComplexDecibelConverter);
            mComplexDftProcessor.setFrameRate(CHANNEL_FFT_FRAME_RATE);
            mComplexDecibelConverter.addListener(mSpectrumPanel);
        }

        public void setDFTSize(DFTSize dftSize)
        {
            mComplexDftProcessor.setDFTSize(dftSize);
        }

        @Override
        public void receive(INativeBuffer nativeBuffer)
        {
            mComplexDftProcessor.receive(nativeBuffer);
        }
    }

    public class ChannelPanel extends JPanel implements Listener<INativeBuffer>
    {
        private ComplexDftProcessor mComplexDftProcessor = new ComplexDftProcessor();
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

            mComplexDftProcessor.addConverter(mComplexDecibelConverter);
            mComplexDftProcessor.setFrameRate(CHANNEL_FFT_FRAME_RATE);

            mComplexDecibelConverter.addListener(mSpectrumPanel);
        }

        public void setDFTSize(DFTSize dftSize)
        {
            mComplexDftProcessor.setDFTSize(dftSize);
        }

        @Override
        public void receive(INativeBuffer nativeBuffer)
        {
            mComplexDftProcessor.receive(nativeBuffer);
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

        private IComplexOscillator mOscillator = OscillatorFactory.getComplexOscillator(DEFAULT_FREQUENCY, CHANNEL_SAMPLE_RATE);

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

        public IComplexOscillator getOscillator()
        {
            return mOscillator;
        }
    }

    public class DataGenerationManager implements Runnable
    {
        private TwoChannelSynthesizerM2 mSynthesizer;
        private FS4DownConverter mFS4DownConverter = new FS4DownConverter();
        private int mSamplesPerCycle = CHANNEL_SAMPLE_RATE / DATA_GENERATOR_FRAME_RATE;

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
            ComplexSamples channel1Buffer = getChannel1ControlPanel().getOscillator().generateComplexSamples(mSamplesPerCycle, 0l);
            ComplexSamples channel2Buffer = getChannel2ControlPanel().getOscillator().generateComplexSamples(mSamplesPerCycle, 0l);

            ComplexSamples synthesizedBuffer = mSynthesizer.process(channel1Buffer, channel2Buffer);
            getChannel1Panel().receive(new FloatNativeBuffer(channel1Buffer));

            getChannel2Panel().receive(new FloatNativeBuffer(channel2Buffer));

            getSpectrumPanel().receive(new FloatNativeBuffer(synthesizedBuffer));
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