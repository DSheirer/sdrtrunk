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

package io.github.dsheirer.gui.channel;

import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.dsp.filter.channelizer.PolyphaseChannelSource;
import io.github.dsheirer.gui.power.SignalPowerView;
import io.github.dsheirer.gui.preference.colortheme.ColorThemeManager;
import io.github.dsheirer.gui.squelch.NoiseSquelchView;
import io.github.dsheirer.module.ProcessingChain;
import io.github.dsheirer.module.decode.PrimaryDecoder;
import io.github.dsheirer.module.decode.am.AMDecoder;
import io.github.dsheirer.module.decode.nbfm.NBFMDecoder;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.preference.UserPreferences;
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
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.MouseInputAdapter;

/**
 * Display for channel FFT and squelch details
 */
public class ChannelSpectrumPanel extends JPanel implements Listener<ProcessingChain>
{
    private static final Logger mLog = LoggerFactory.getLogger(ChannelSpectrumPanel.class);
    private static final DecimalFormat FREQUENCY_FORMAT = new DecimalFormat("0.00000");
    private final PlaylistManager mPlaylistManager;
    private ProcessingChain mProcessingChain;
    private final ComplexSamplesToNativeBufferModule mSampleStreamTapModule = new ComplexSamplesToNativeBufferModule();
    private final ComplexDftProcessor mComplexDftProcessor = new ComplexDftProcessor();
    private SpectrumPanel mSpectrumPanel;
    private final FrequencyOverlayPanel mFrequencyOverlayPanel;
    private final SourceEventProcessor mSourceEventProcessor = new SourceEventProcessor();
    private final SpinnerNumberModel mNoiseFloorSpinnerModel;
    private final JLabel mEstimatedCarrierOffsetFrequencyLabel;
    private final JLabel mEstimatedCarrierOffsetFrequencyValueLabel;
    private boolean mPanelVisible = false;
    private boolean mDftProcessing = false;
    private final NoiseSquelchView mNoiseSquelchView;
    private final SignalPowerView mSignalPowerView;
    private final JFXPanel mNoiseSquelchPanel;
    private JSplitPane mSplitPane;

    /**
     * Constructs an instance.
     */
    public ChannelSpectrumPanel(PlaylistManager playlistManager, SettingsManager settingsManager, UserPreferences userPreferences)
    {
        mPlaylistManager = playlistManager;
        mNoiseSquelchView = new NoiseSquelchView(mPlaylistManager);
        mSignalPowerView = new SignalPowerView(mPlaylistManager);
        setLayout(new MigLayout("insets 0", "[grow,fill]", "[grow,fill]"));

        JPanel fftPanel = new JPanel();
        fftPanel.setLayout(new MigLayout("insets 0", "[grow,fill]", "[][grow,fill]"));

        JPanel labelPanel = new JPanel();
        labelPanel.setLayout(new MigLayout("insets 0", "[grow,fill][right][grow,fill][right][][]", ""));
        labelPanel.add(new JLabel("Channel Spectrum"));

        mEstimatedCarrierOffsetFrequencyLabel = new JLabel("Carrier Offset:");
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

        mNoiseSquelchPanel = new JFXPanel();

        //Spin noise squelch panel construction off onto the JavafX UI thread.
        Platform.runLater(() -> {
            Scene scene = new Scene(mNoiseSquelchView);
            ColorThemeManager.applyThemeToScene(scene, userPreferences);
            mNoiseSquelchPanel.setScene(scene);
        });

        mSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mSplitPane.add(fftPanel, JSplitPane.LEFT);
        mSplitPane.add(mNoiseSquelchPanel, JSplitPane.RIGHT);
        mSplitPane.setDividerLocation(0.5);
        add(mSplitPane);

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
        mNoiseSquelchView.setShowing(visible);
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
        mEstimatedCarrierOffsetFrequencyLabel.setEnabled(false);
        mEstimatedCarrierOffsetFrequencyValueLabel.setText("0 Hz");
        mEstimatedCarrierOffsetFrequencyValueLabel.setEnabled(false);
        mFrequencyOverlayPanel.process(SourceEvent.frequencyChange(null, 0));
        mFrequencyOverlayPanel.process(SourceEvent.sampleRateChange(0));
        mFrequencyOverlayPanel.setEstimatedCarrierOffsetFrequency(0);
        mFrequencyOverlayPanel.setChannelBandwidth(0);
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
            mNoiseSquelchView.setController(null);
            mProcessingChain.removeSourceEventListener(mSourceEventProcessor);
            mProcessingChain.removeModule(mSampleStreamTapModule);
        }

        //Invoking reset - we're on the Swing dispatch thread here
        reset();

        mProcessingChain = processingChain;

        if(mProcessingChain != null)
        {
            mProcessingChain.addSourceEventListener(mSourceEventProcessor);

            PrimaryDecoder primaryDecoder = mProcessingChain.getPrimaryDecoder();
            if(primaryDecoder instanceof NBFMDecoder nbfmDecoder)
            {
                Component rightComponent = mSplitPane.getRightComponent();

                if(rightComponent != mNoiseSquelchPanel)
                {
                    mSplitPane.remove(rightComponent);
                    mSplitPane.setRightComponent(mNoiseSquelchPanel);
                }

                mNoiseSquelchView.setController(nbfmDecoder);
                mSignalPowerView.setProcessingChain(null);
            }
            else if(primaryDecoder instanceof AMDecoder)
            {
                Component rightComponent = mSplitPane.getRightComponent();

                if(rightComponent != mSignalPowerView)
                {
                    mSplitPane.remove(rightComponent);
                    mSplitPane.setRightComponent(mSignalPowerView);
                }

                mNoiseSquelchView.setController(null);
                mSignalPowerView.setProcessingChain(mProcessingChain);
            }
            else
            {
                mNoiseSquelchView.setController(null);
                mSignalPowerView.setProcessingChain(null);
            }

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
        else
        {
            mNoiseSquelchView.setController(null);
            mSignalPowerView.setProcessingChain(null);
        }

        updateFFTProcessing();
    }


    /**
     * Processor for source event stream to capture power level and squelch related source events.
     */
    private class SourceEventProcessor implements Listener<SourceEvent>
    {
        @Override
        public void receive(SourceEvent sourceEvent)
        {
            if(sourceEvent.getEvent() == SourceEvent.Event.NOTIFICATION_CARRIER_OFFSET_FREQUENCY)
            {
                updateEstimatedCarrierOffsetFrequency(sourceEvent.getValue().longValue());
            }

            mSignalPowerView.receive(sourceEvent);
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
