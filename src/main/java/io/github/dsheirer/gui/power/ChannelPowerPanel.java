/*
 * *****************************************************************************
 * Copyright (C) 2014-2025 Dennis Sheirer
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

package io.github.dsheirer.gui.power;

import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.dsp.filter.channelizer.PolyphaseChannelSource;
import io.github.dsheirer.dsp.squelch.ISquelchConfiguration;
import io.github.dsheirer.gui.control.DbPowerMeter;
import io.github.dsheirer.module.ProcessingChain;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.ComplexSamplesToNativeBufferModule;
import io.github.dsheirer.settings.SettingsManager;
import io.github.dsheirer.source.Source;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.tuner.channel.HalfBandTunerChannelSource;
import io.github.dsheirer.source.tuner.channel.TunerChannel;
import io.github.dsheirer.source.tuner.channel.TunerChannelSource;
import io.github.dsheirer.spectrum.ComplexDftProcessor;
import io.github.dsheirer.spectrum.FrequencyOverlayPanel;
import io.github.dsheirer.spectrum.SpectrumPanel;
import io.github.dsheirer.spectrum.converter.ComplexDecibelConverter;
import io.github.dsheirer.spectrum.converter.DFTResultsConverter;
import java.awt.Component;
import java.awt.EventQueue;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.List;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.swing.IconFontSwing;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.MouseInputAdapter;

/**
 * Display for channel power and squelch details
 */
public class ChannelPowerPanel extends JPanel implements Listener<ProcessingChain>
{
    private static final Logger mLog = LoggerFactory.getLogger(ChannelPowerPanel.class);
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("0.0");
    private static final DecimalFormat FREQUENCY_FORMAT = new DecimalFormat("0.00000");
    private static final String NOT_AVAILABLE = "Not Available";
    private final PlaylistManager mPlaylistManager;
    private ProcessingChain mProcessingChain;
    private final ComplexSamplesToNativeBufferModule mSampleStreamTapModule = new ComplexSamplesToNativeBufferModule();
    private final ComplexDftProcessor mComplexDftProcessor = new ComplexDftProcessor();
    private SpectrumPanel mSpectrumPanel;
    private final FrequencyOverlayPanel mFrequencyOverlayPanel;
    private final DbPowerMeter mPowerMeter = new DbPowerMeter();
    private final PeakMonitor mPeakMonitor = new PeakMonitor(DbPowerMeter.DEFAULT_MINIMUM_POWER);
    private final SourceEventProcessor mSourceEventProcessor = new SourceEventProcessor();
    private final SpinnerNumberModel mNoiseFloorSpinnerModel;
    private final JLabel mPowerLabel;
    private final JLabel mPeakLabel;
    private final JLabel mSquelchLabel;
    private final JLabel mSquelchValueLabel;
    private final JLabel mEstimatedCarrierOffsetFrequencyLabel;
    private final JLabel mEstimatedCarrierOffsetFrequencyValueLabel;
    private final JButton mSquelchUpButton;
    private final JButton mSquelchDownButton;
    private final JCheckBox mSquelchAutoTrackCheckBox;
    private double mSquelchThreshold;
    private boolean mPanelVisible = false;
    private boolean mDftProcessing = false;

    /**
     * Constructs an instance.
     */
    public ChannelPowerPanel(PlaylistManager playlistManager, SettingsManager settingsManager)
    {
        mPlaylistManager = playlistManager;

        setLayout(new MigLayout("", "[][][][grow,fill]", "[grow,fill]"));
        mPowerMeter.setPeakVisible(true);
        mPowerMeter.setSquelchThresholdVisible(true);
        add(mPowerMeter);

        JPanel valuePanel = new JPanel();
        valuePanel.setLayout(new MigLayout("", "[right][left][][]", ""));

        mPeakLabel = new JLabel("0");
        mPeakLabel.setToolTipText("Current peak power level in decibels.");
        valuePanel.add(new JLabel("Peak:"));
        valuePanel.add(mPeakLabel, "wrap");

        mPowerLabel = new JLabel("0");
        mPowerLabel.setToolTipText("Current Power level in decibels");
        valuePanel.add(new JLabel("Power:"));
        valuePanel.add(mPowerLabel, "wrap");

        mSquelchLabel = new JLabel("Squelch:");
        mSquelchLabel.setEnabled(false);
        valuePanel.add(mSquelchLabel);
        mSquelchValueLabel = new JLabel(NOT_AVAILABLE);
        mSquelchValueLabel.setToolTipText("Squelch threshold value in decibels");
        mSquelchValueLabel.setEnabled(false);
        valuePanel.add(mSquelchValueLabel, "wrap");

        IconFontSwing.register(FontAwesome.getIconFont());
        Icon iconUp = IconFontSwing.buildIcon(FontAwesome.ANGLE_UP, 12);
        mSquelchUpButton = new JButton(iconUp);
        mSquelchUpButton.setToolTipText("Increases the squelch threshold value");
        mSquelchUpButton.setEnabled(false);
        mSquelchUpButton.addActionListener(e -> broadcast(SourceEvent.requestSquelchThreshold(null, mSquelchThreshold + 1)));
        valuePanel.add(mSquelchUpButton);

        Icon iconDown = IconFontSwing.buildIcon(FontAwesome.ANGLE_DOWN, 12);
        mSquelchDownButton = new JButton(iconDown);
        mSquelchDownButton.setToolTipText("Decreases the squelch threshold value.");
        mSquelchDownButton.setEnabled(false);
        mSquelchDownButton.addActionListener(e -> broadcast(SourceEvent.requestSquelchThreshold(null, mSquelchThreshold - 1)));
        valuePanel.add(mSquelchDownButton, "wrap");

        mSquelchAutoTrackCheckBox = new JCheckBox("Auto Track");
        mSquelchAutoTrackCheckBox.setToolTipText("Enable or disable monitoring of the noise floor to auto-adjust the " +
                "squelch threshold value maintaining a consistent level/buffer above the noise floor");
        mSquelchAutoTrackCheckBox.setEnabled(false);
        mSquelchAutoTrackCheckBox.addActionListener(e ->
        {
            broadcast(SourceEvent.requestSquelchAutoTrack(mSquelchAutoTrackCheckBox.isSelected()));
        });
        valuePanel.add(mSquelchAutoTrackCheckBox, "span,left");

        add(valuePanel);
        add(new JSeparator(JSeparator.VERTICAL));

        JPanel fftPanel = new JPanel();
        fftPanel.setLayout(new MigLayout("insets 0", "[grow,fill]", "[][grow,fill]"));

        JPanel labelPanel = new JPanel();
        labelPanel.setLayout(new MigLayout("insets 0", "[grow,fill][right][grow,fill][right][][]", ""));
        labelPanel.add(new JLabel("Channel Spectrum"));

        mEstimatedCarrierOffsetFrequencyLabel = new JLabel("Estimated Carrier Offset:");
        mEstimatedCarrierOffsetFrequencyLabel.setEnabled(false);
        labelPanel.add(mEstimatedCarrierOffsetFrequencyLabel);
        mEstimatedCarrierOffsetFrequencyValueLabel = new JLabel("0 Hz");
        mEstimatedCarrierOffsetFrequencyValueLabel.setEnabled(false);
        labelPanel.add(mEstimatedCarrierOffsetFrequencyValueLabel);

        mNoiseFloorSpinnerModel = new SpinnerNumberModel(18, 8, 36, 1);
        mNoiseFloorSpinnerModel.addChangeListener(e -> {
            Number number = mNoiseFloorSpinnerModel.getNumber();
            mSpectrumPanel.setSampleSize(number.doubleValue());
        });
        JSpinner noiseFloorSpinner = new JSpinner(mNoiseFloorSpinnerModel);
        labelPanel.add(noiseFloorSpinner);
        labelPanel.add(new JLabel("Spectral Display Noise Floor"));

        JButton logIndexesButton = new JButton("Log Settings");
        logIndexesButton.addActionListener(e -> {
            if(mProcessingChain != null)
            {
                Source source = mProcessingChain.getSource();

                if(source instanceof PolyphaseChannelSource pcs)
                {
                    List<Integer> indexes = pcs.getOutputProcessorIndexes();
                    double sampleRate = pcs.getSampleRate();
                    long indexCenterFrequency = pcs.getIndexCenterFrequency();
                    long appliedFrequencyOffset = pcs.getFrequencyOffset();
                    long requestedCenterFrequency = pcs.getFrequency();

                    StringBuilder sb = new StringBuilder();
                    sb.append("Polyphase Channel - BW: ").append(FREQUENCY_FORMAT.format(sampleRate / 1E6d));
                    sb.append(" Center/Requested/Mixer: ").append(FREQUENCY_FORMAT.format(indexCenterFrequency / 1E6d));
                    sb.append("/").append(FREQUENCY_FORMAT.format(requestedCenterFrequency / 1E6d));
                    sb.append("/").append(FREQUENCY_FORMAT.format(appliedFrequencyOffset / 1E6d));
                    sb.append(" Polyphase Indexes: ").append(indexes);
                    sb.append(" Tuner SR:").append(FREQUENCY_FORMAT.format(pcs.getTunerSampleRate() / 1E6d));
                    sb.append(" CF:").append(FREQUENCY_FORMAT.format(pcs.getTunerCenterFrequency() / 1E6d));
                    mLog.info(sb.toString());
                    mLog.info("Output Processor: " + pcs.getStateDescription());
                }
                else if(source instanceof HalfBandTunerChannelSource<?> hbtcs)
                {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Heterodyne Channel - CF:").append(FREQUENCY_FORMAT.format(hbtcs.getFrequency() / 1E6d));
                    sb.append(" SR:").append(FREQUENCY_FORMAT.format(hbtcs.getSampleRate() / 1E6d));
                    sb.append(" Mixer:").append(FREQUENCY_FORMAT.format(hbtcs.getMixerFrequency() / 1E6d));
                    mLog.info(sb.toString());
                }
                else
                {
                    mLog.info("Unsupported channel type: " + (source != null ? source.getClass() : " null"));
                }
            }
        });
        //This is a debug button to log the current settings to the app log.
//        labelPanel.add(mLogIndexesButton);

        fftPanel.add(labelPanel, "wrap");

        mFrequencyOverlayPanel = new FrequencyOverlayPanel(settingsManager);
        mSpectrumPanel = new SpectrumPanel(settingsManager);
        mSpectrumPanel.setSampleSize(18.0);

        /**
         * The layered pane holds the overlapping spectrum and channel panels
         * and manages the sizing of each panel with the resize listener
         */
        JLayeredPane layeredPanel = new JLayeredPane();
        layeredPanel.addComponentListener(new ResizeListener());

        /**
         * Create a mouse adapter to handle mouse events over the spectrum
         * and waterfall panels
         */
        MouseEventProcessor mouser = new MouseEventProcessor();

        mFrequencyOverlayPanel.addMouseListener(mouser);
        mFrequencyOverlayPanel.addMouseMotionListener(mouser);
        mFrequencyOverlayPanel.addMouseWheelListener(mouser);

        //Add the spectrum and channel panels to the layered panel
        layeredPanel.add(mSpectrumPanel, 0, 0);
        layeredPanel.add(mFrequencyOverlayPanel, 1, 0);

        fftPanel.add(layeredPanel);
        add(fftPanel);

        mSampleStreamTapModule.setListener(mComplexDftProcessor);
        DFTResultsConverter DFTResultsConverter = new ComplexDecibelConverter();
        mComplexDftProcessor.addConverter(DFTResultsConverter);
        DFTResultsConverter.addListener(mSpectrumPanel);
        mSpectrumPanel.clearSpectrum();
    }

    /**
     * Signals this panel to indicate if this panel is visible to turn on the FFT processor when the panel is visible
     * and turn off the FFT processor when it's not.
     *
     * Note: this method is intended to be called by the Swing event thread to ensure that only a single thread is
     * invoking either this method, or the receive() method, since there is no thread synchronization between these
     * two methods and they each depend on stable access to the mPanelVisible variable.
     *
     * @param visible true to indicate that this panel is showing/visible.
     */
    public void setPanelVisible(boolean visible)
    {
        mPanelVisible = visible;
        updateFFTProcessing();
    }

    /**
     * Updates processing state for the DFT processor.  Turns on DFT processing when we have a processing chain and
     * when the user has this tab selected and visible.  Otherwise, turns off DFT processing.
     */
    private void updateFFTProcessing()
    {
        if(mPanelVisible && mProcessingChain != null)
        {
            startDftProcessing();
        }
        else
        {
            stopDftProcessing();
        }
    }

    /**
     * Starts DFT processing
     */
    private void startDftProcessing()
    {
        if(!mDftProcessing)
        {
            mDftProcessing = true;
            mSampleStreamTapModule.setListener(mComplexDftProcessor);
            mComplexDftProcessor.start();
        }
    }

    /**
     * Stops DFT processing
     */
    private void stopDftProcessing()
    {
        if(mDftProcessing)
        {
            mSampleStreamTapModule.removeListener();
            mComplexDftProcessor.stop();
            mSpectrumPanel.clearSpectrum();
            mDftProcessing = false;
        }
    }

    /**
     * Updates the CarrierOffsetProcessor's current carrier offset tracking frequency
     * @param carrierOffsetFrequency that is currently measured/estimated.
     */
    private void updateEstimatedCarrierOffsetFrequency(long carrierOffsetFrequency)
    {
        //Note: we flip the sign on the error measurement because the value represents the amount of offset the PLL
        //has to apply to move the signal to center/baseband
        EventQueue.invokeLater(() -> {
            String formattedValue = NumberFormat.getInstance().format(carrierOffsetFrequency);
            mEstimatedCarrierOffsetFrequencyValueLabel.setText(formattedValue + " Hz");
            mEstimatedCarrierOffsetFrequencyValueLabel.setEnabled(true);
            mEstimatedCarrierOffsetFrequencyLabel.setEnabled(true);
        });

        mFrequencyOverlayPanel.setEstimatedCarrierOffsetFrequency(carrierOffsetFrequency);
    }

    /**
     * Updates the channel's decode configuration with a new squelch threshold value
     */
    private void setConfigSquelchThreshold(int threshold)
    {
        if(mProcessingChain != null)
        {
            Channel channel = mPlaylistManager.getChannelProcessingManager().getChannel(mProcessingChain);

            if(channel != null && channel.getDecodeConfiguration() instanceof ISquelchConfiguration configuration)
            {
                configuration.setSquelchThreshold(threshold);
                mPlaylistManager.schedulePlaylistSave();
            }
        }
    }

    /**
     * Updates the channel configuration squelch auto-track feature setting.
     * @param autoTrack true to enable.
     */
    private void setConfigSquelchAutoTrack(boolean autoTrack)
    {
        Channel channel = mPlaylistManager.getChannelProcessingManager().getChannel(mProcessingChain);

        if(channel != null && channel.getDecodeConfiguration() instanceof ISquelchConfiguration configuration)
        {
            configuration.setSquelchAutoTrack(autoTrack);
            mPlaylistManager.schedulePlaylistSave();
        }
    }

    private void broadcast(SourceEvent sourceEvent)
    {
        if(mProcessingChain != null)
        {
            mProcessingChain.broadcast(sourceEvent);
        }
    }

    /**
     * Resets controls when changing processing chain source.  Note: this must be called on the Swing
     * dispatch thread because it directly invokes swing components.
     */
    private void reset()
    {
        mPeakMonitor.reset();
        mPowerMeter.reset();

        mPeakLabel.setText("0");
        mPowerLabel.setText("0");
        mEstimatedCarrierOffsetFrequencyLabel.setEnabled(false);
        mEstimatedCarrierOffsetFrequencyValueLabel.setText("0 Hz");
        mEstimatedCarrierOffsetFrequencyValueLabel.setEnabled(false);
        mFrequencyOverlayPanel.process(SourceEvent.frequencyChange(null, 0));
        mFrequencyOverlayPanel.process(SourceEvent.sampleRateChange(0));
        mFrequencyOverlayPanel.setEstimatedCarrierOffsetFrequency(0);
        mFrequencyOverlayPanel.setChannelBandwidth(0);

        mSquelchLabel.setEnabled(false);
        mSquelchValueLabel.setText("Not Available");
        mSquelchValueLabel.setEnabled(false);
        mSquelchUpButton.setEnabled(false);
        mSquelchDownButton.setEnabled(false);
        mSquelchAutoTrackCheckBox.setEnabled(false);
        mSquelchAutoTrackCheckBox.setSelected(false);
    }

    /**
     * Receive notifications of request to provide display of processing chain details.
     */
    @Override
    public void receive(ProcessingChain processingChain)
    {
        //Disconnect the FFT panel
        if(mProcessingChain != null)
        {
            mProcessingChain.removeSourceEventListener(mSourceEventProcessor);
            mProcessingChain.removeModule(mSampleStreamTapModule);
        }

        //Invoking reset - we're on the Swing dispatch thread here
        reset();

        mProcessingChain = processingChain;

        if(mProcessingChain != null)
        {
            mProcessingChain.addSourceEventListener(mSourceEventProcessor);
            mProcessingChain.addModule(mSampleStreamTapModule);

            Source source = mProcessingChain.getSource();

            if(source instanceof TunerChannelSource tcs)
            {
                mFrequencyOverlayPanel.process(SourceEvent.frequencyChange(null, tcs.getFrequency()));
                mFrequencyOverlayPanel.process(SourceEvent.sampleRateChange(tcs.getSampleRate()));
            }

            Channel channel = mPlaylistManager.getChannelProcessingManager().getChannel(mProcessingChain);

            if(channel != null)
            {
                List<TunerChannel> tunerChannels = channel.getTunerChannels();

                if(!tunerChannels.isEmpty())
                {
                    mFrequencyOverlayPanel.setChannelBandwidth(tunerChannels.getFirst().getBandwidth());
                }
            }
        }

        updateFFTProcessing();

        broadcast(SourceEvent.requestCurrentSquelchAutoTrack());
        broadcast(SourceEvent.requestCurrentSquelchThreshold());
    }


    /**
     * Processor for source event stream to capture power level and squelch related source events.
     */
    private class SourceEventProcessor implements Listener<SourceEvent>
    {
        @Override
        public void receive(SourceEvent sourceEvent)
        {
            switch(sourceEvent.getEvent())
            {
                case NOTIFICATION_CHANNEL_POWER ->
                    {
                        final double power = sourceEvent.getValue().doubleValue();
                        final double peak = mPeakMonitor.process(power);

                        EventQueue.invokeLater(() -> {
                            mPowerMeter.setPower(power);
                            mPowerLabel.setText(DECIMAL_FORMAT.format(power));

                            mPowerMeter.setPeak(peak);
                            mPeakLabel.setText(DECIMAL_FORMAT.format(peak));
                        });
                    }
                case NOTIFICATION_SQUELCH_THRESHOLD ->
                        {
                            final double threshold = sourceEvent.getValue().doubleValue();
                            mSquelchThreshold = threshold;
                            setConfigSquelchThreshold((int)threshold);

                            EventQueue.invokeLater(() -> {
                                mPowerMeter.setSquelchThreshold(threshold);
                                mSquelchLabel.setEnabled(true);
                                mSquelchValueLabel.setEnabled(true);
                                mSquelchValueLabel.setText(DECIMAL_FORMAT.format(threshold));
                                mSquelchDownButton.setEnabled(true);
                                mSquelchUpButton.setEnabled(true);
                            });
                        }
                case NOTIFICATION_CARRIER_OFFSET_FREQUENCY ->
                {
                    updateEstimatedCarrierOffsetFrequency(sourceEvent.getValue().longValue());
                }
                case NOTIFICATION_SQUELCH_AUTO_TRACK ->
                {
                    boolean autoTrack = sourceEvent.getValue().intValue() == 1;
                    setConfigSquelchAutoTrack(autoTrack);
                    EventQueue.invokeLater(() -> {
                        mSquelchAutoTrackCheckBox.setSelected(autoTrack);
                        mSquelchAutoTrackCheckBox.setEnabled(true);
                    });
                }
            }
        }
    }

    /**
     * Monitors the sizing of the layered pane and resizes the spectrum and
     * channel panels whenever the layered pane is resized
     */
    public class ResizeListener implements ComponentListener
    {
        @Override public void componentResized(ComponentEvent e)
        {
            Component c = e.getComponent();

            mSpectrumPanel.setBounds(0, 0, c.getWidth(), c.getHeight());
            mFrequencyOverlayPanel.setBounds(0, 0, c.getWidth(), c.getHeight());
        }

        @Override public void componentHidden(ComponentEvent arg0) {}
        @Override public void componentMoved(ComponentEvent arg0) {}
        @Override public void componentShown(ComponentEvent arg0) {}
    }

    /**
     * Mouse event handler for the spectral display panel.
     */
    public class MouseEventProcessor extends MouseInputAdapter
    {
        @Override public void mouseMoved(MouseEvent event)
        {
            update(event);
        }

        /**
         * Updates the cursor display while the mouse is performing actions
         */
        private void update(MouseEvent event)
        {
            mFrequencyOverlayPanel.setCursorLocation(event.getPoint());
        }

        @Override public void mouseEntered(MouseEvent e)
        {
            mFrequencyOverlayPanel.setCursorVisible(true);
        }

        @Override public void mouseExited(MouseEvent e)
        {
            mFrequencyOverlayPanel.setCursorVisible(false);
        }
    }
}
