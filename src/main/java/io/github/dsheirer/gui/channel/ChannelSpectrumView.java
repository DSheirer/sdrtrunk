/*
 * *****************************************************************************
 * Copyright (C) 2014-2026 Dennis Sheirer
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
import io.github.dsheirer.gui.power.SignalPowerView;
import io.github.dsheirer.gui.spectrum.ChannelSpectralDisplay;
import io.github.dsheirer.gui.spectrum.DftSpectralDisplay;
import io.github.dsheirer.gui.squelch.NoiseSquelchView;
import io.github.dsheirer.gui.symbol.SymbolView;
import io.github.dsheirer.module.ProcessingChain;
import io.github.dsheirer.module.decode.FeedbackDecoder;
import io.github.dsheirer.module.decode.PrimaryDecoder;
import io.github.dsheirer.module.decode.am.AMDecoder;
import io.github.dsheirer.module.decode.nbfm.NBFMDecoder;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.sample.complex.ComplexSamplesToNativeBufferModule;
import io.github.dsheirer.source.ChannelFrequencyCorrectionStatusNotification;
import io.github.dsheirer.source.Source;
import io.github.dsheirer.source.SourceEvent;
import io.github.dsheirer.source.tuner.channel.TunerChannel;
import io.github.dsheirer.source.tuner.channel.TunerChannelSource;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.List;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.SplitPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Display for channel spectrum, frequency corrections and the decoder squelch, power or symbol view.
 *
 * Use this class on the JavaFX application thread.
 */
public class ChannelSpectrumView extends SplitPane implements Listener<ProcessingChain>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ChannelSpectrumView.class);
    private static final DecimalFormat PPM_FORMAT = new DecimalFormat("0.00000");
    private static final String LABEL_PREFIX_PPM = "Tuner PPM: ";
    private static final String LABEL_PREFIX_TUNER = "Tuner:";
    private static final String LABEL_PREFIX_CHANNEL = "Channel:";
    private static final String LABEL_PREFIX_DECODER = "Decoder:";
    private static final double DIVIDER_POSITION = 0.4;
    private final PlaylistManager mPlaylistManager;
    private final ComplexSamplesToNativeBufferModule mSampleStreamTapModule = new ComplexSamplesToNativeBufferModule();
    private final ChannelSpectralDisplay mChannelSpectralDisplay = new ChannelSpectralDisplay();
    private final SourceEventProcessor mSourceEventProcessor = new SourceEventProcessor();
    private final Label mTunerPPMLabel = new Label(LABEL_PREFIX_PPM);
    private final Label mTunerCorrectionLabel = new Label(LABEL_PREFIX_TUNER);
    private final Label mChannelCorrectionLabel = new Label(LABEL_PREFIX_CHANNEL);
    private final Label mDecoderCorrectionLabel = new Label(LABEL_PREFIX_DECODER);
    private final NoiseSquelchView mNoiseSquelchView;
    private final SignalPowerView mSignalPowerView;
    private final SymbolView mSymbolView = new SymbolView();
    private ProcessingChain mProcessingChain;
    private boolean mPanelVisible = false;

    /**
     * Constructs an instance.  Construct on the JavaFX application thread.
     * @param playlistManager for the channel configurations
     */
    public ChannelSpectrumView(PlaylistManager playlistManager)
    {
        mPlaylistManager = playlistManager;
        mNoiseSquelchView = new NoiseSquelchView(mPlaylistManager);
        mSignalPowerView = new SignalPowerView(mPlaylistManager);

        URL resource = getClass().getResource("/sdrtrunk_style.css");

        if(resource != null)
        {
            mSymbolView.getStylesheets().add(resource.toExternalForm());
        }
        else
        {
            LOGGER.warn("Can't find stylesheet resource for sdrtrunk");
        }

        //Use the same color theme as the channel spectral display
        mSymbolView.followTheme(DftSpectralDisplay.getSharedTheme());

        Region leftSpacer = new Region();
        Region rightSpacer = new Region();
        HBox.setHgrow(leftSpacer, Priority.ALWAYS);
        HBox.setHgrow(rightSpacer, Priority.ALWAYS);
        HBox labels = new HBox(10, leftSpacer, mTunerPPMLabel, new Label("Corrections (Hz) "), mTunerCorrectionLabel,
            mChannelCorrectionLabel, mDecoderCorrectionLabel, rightSpacer);
        labels.setAlignment(Pos.CENTER);
        labels.setPadding(new Insets(2));

        VBox.setVgrow(mChannelSpectralDisplay, Priority.ALWAYS);
        VBox spectrumBox = new VBox(labels, mChannelSpectralDisplay);
        spectrumBox.setMinWidth(0);

        getItems().addAll(spectrumBox, mNoiseSquelchView);
        reset();
    }

    /**
     * Pads the value to a fixed width and returns the prefix and padded value.
     */
    private static String getPaddedLabel(String prefix, Number value)
    {
        return prefix + StringUtils.leftPad(String.valueOf(value), 7);
    }

    /**
     * Signals this view to indicate if it is visible, to turn on the DFT processor when the view is visible and turn
     * it off when it's not.
     * @param visible true to indicate that this view is showing/visible.
     */
    public void setPanelVisible(boolean visible)
    {
        mPanelVisible = visible;
        updateFFTProcessing();
        mNoiseSquelchView.setShowing(visible);
        mSymbolView.setShowing(visible);
    }

    /**
     * Turns on DFT processing when there is a processing chain and the user has this view selected and visible.
     * Otherwise, turns off DFT processing.
     */
    private void updateFFTProcessing()
    {
        boolean active = mPanelVisible && mProcessingChain != null;

        if(active)
        {
            mSampleStreamTapModule.setListener(mChannelSpectralDisplay.getSampleListener());
        }
        else
        {
            mSampleStreamTapModule.removeListener();
        }

        mChannelSpectralDisplay.setActive(active);
    }

    /**
     * Resets controls when changing processing chain source.
     */
    private void reset()
    {
        mTunerPPMLabel.setText(LABEL_PREFIX_PPM);
        mTunerPPMLabel.setDisable(true);
        mTunerCorrectionLabel.setText(LABEL_PREFIX_TUNER);
        mTunerCorrectionLabel.setDisable(true);
        mChannelCorrectionLabel.setText(LABEL_PREFIX_CHANNEL);
        mChannelCorrectionLabel.setDisable(true);
        mDecoderCorrectionLabel.setText(LABEL_PREFIX_DECODER);
        mDecoderCorrectionLabel.setDisable(true);
        mChannelSpectralDisplay.setChannel(0, 0, 0);
    }

    /**
     * Shows the processing chain details.  Call on the JavaFX application thread.
     * @param processingChain to show, or null to clear the view
     */
    @Override
    public void receive(ProcessingChain processingChain)
    {
        //Disconnect the previous processing chain.
        if(mProcessingChain != null)
        {
            mNoiseSquelchView.setController(null);
            mSignalPowerView.setProcessingChain(null);
            mSymbolView.removeSymbolProvider();
            mSymbolView.setProtocol("");
            mProcessingChain.removeSourceEventListener(mSourceEventProcessor);
            mProcessingChain.removeModule(mSampleStreamTapModule);
        }

        reset();

        mProcessingChain = processingChain;

        if(mProcessingChain != null)
        {
            mProcessingChain.addSourceEventListener(mSourceEventProcessor);

            PrimaryDecoder primaryDecoder = mProcessingChain.getPrimaryDecoder();

            if(primaryDecoder instanceof NBFMDecoder nbfmDecoder)
            {
                setRightComponent(mNoiseSquelchView);
                mNoiseSquelchView.setController(nbfmDecoder);
            }
            else if(primaryDecoder instanceof AMDecoder)
            {
                setRightComponent(mSignalPowerView);
                mSignalPowerView.setProcessingChain(mProcessingChain);
            }
            else if(primaryDecoder instanceof FeedbackDecoder feedbackDecoder)
            {
                setRightComponent(mSymbolView);
                mSymbolView.setSymbolProvider(feedbackDecoder);
                mSymbolView.setProtocol(feedbackDecoder.getProtocolDescription());
            }

            mProcessingChain.addModule(mSampleStreamTapModule);
            Source source = mProcessingChain.getSource();

            int bandwidth = 0;
            Channel channel = mPlaylistManager.getChannelProcessingManager().getChannel(mProcessingChain);

            if(channel != null)
            {
                List<TunerChannel> tunerChannels = channel.getTunerChannels();

                if(!tunerChannels.isEmpty())
                {
                    bandwidth = tunerChannels.getFirst().getBandwidth();
                }
            }

            if(source instanceof TunerChannelSource tcs)
            {
                mChannelSpectralDisplay.setChannel(tcs.getFrequency(), (int)tcs.getSampleRate(), bandwidth,
                    tcs::getNoiseDensity);
            }
        }

        updateFFTProcessing();
    }

    /**
     * Shows the node on the right side of the split pane.
     */
    private void setRightComponent(Node node)
    {
        if(getItems().size() < 2 || getItems().get(1) != node)
        {
            if(getItems().size() > 1)
            {
                getItems().remove(1);
            }

            getItems().add(node);
            setDividerPositions(DIVIDER_POSITION);
        }
    }

    /**
     * Processor for source event stream to capture power level and squelch related source events.
     */
    private class SourceEventProcessor implements Listener<SourceEvent>
    {
        @Override
        public void receive(SourceEvent sourceEvent)
        {
            if(sourceEvent.getEvent() == SourceEvent.Event.NOTIFICATION_CHANNEL_FREQUENCY_CORRECTION_STATUS &&
                sourceEvent instanceof ChannelFrequencyCorrectionStatusNotification status)
            {
                Platform.runLater(() -> {
                    mTunerPPMLabel.setText(LABEL_PREFIX_PPM + PPM_FORMAT.format(status.getTunerPPM()));
                    mTunerPPMLabel.setDisable(false);

                    if(status.isAutoPPM())
                    {
                        mTunerCorrectionLabel.setText(getPaddedLabel(LABEL_PREFIX_TUNER, status.getTunerCorrection()));
                    }
                    else
                    {
                        mTunerCorrectionLabel.setText(LABEL_PREFIX_TUNER + " disabled");
                    }

                    mTunerCorrectionLabel.setDisable(false);
                    mChannelCorrectionLabel.setText(getPaddedLabel(LABEL_PREFIX_CHANNEL, status.getChannelCorrection()));
                    mChannelCorrectionLabel.setDisable(false);
                    mDecoderCorrectionLabel.setText(getPaddedLabel(LABEL_PREFIX_DECODER, status.getDecoderCorrection()));
                    mDecoderCorrectionLabel.setDisable(false);
                });

                //The offset cursor line is negative so that it aligns with where the signal is located instead of the
                //correction that's needed to bring the signal to zero.
                mChannelSpectralDisplay.setCarrierOffset(-status.getDecoderCorrection());
            }

            mSignalPowerView.receive(sourceEvent);
        }
    }
}
