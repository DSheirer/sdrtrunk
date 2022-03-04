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

package io.github.dsheirer.gui.playlist.radioreference;

import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.gui.playlist.channel.ViewChannelRequest;
import io.github.dsheirer.module.decode.DecoderFactory;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import io.github.dsheirer.module.decode.nbfm.DecodeConfigNBFM;
import io.github.dsheirer.module.decode.p25.phase1.DecodeConfigP25Phase1;
import io.github.dsheirer.module.decode.p25.phase1.P25P1Decoder;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.rrapi.type.Category;
import io.github.dsheirer.rrapi.type.Frequency;
import io.github.dsheirer.rrapi.type.Mode;
import io.github.dsheirer.rrapi.type.SubCategory;
import io.github.dsheirer.service.radioreference.RadioReference;
import io.github.dsheirer.source.config.SourceConfigTuner;
import io.github.dsheirer.util.ThreadPool;
import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;

public class FrequencyEditor extends VBox
{
    private static final Logger mLog = LoggerFactory.getLogger(FrequencyEditor.class);
    private final DecimalFormat FREQUENCY_FORMATTER = new DecimalFormat("0.00000");
    private UserPreferences mUserPreferences;
    private RadioReference mRadioReference;
    private PlaylistManager mPlaylistManager;
    private Level mLevel;
    private TextField mAlphaTagTextField;
    private TextField mFrequencyTextField;
    private TextField mModeTextField;
    private TextField mToneTextField;
    private TextField mSystemTextField;
    private TextField mSiteTextField;
    private TextField mNameTextField;
    private TextField mDecoderTextField;
    private Button mCreateButton;
    private Label mChannelCreatedLabel;
    private CheckBox mShowCreatedChannelCheckBox;
    private ModeDecoderType mModeDecoderType;
    private long mFrequency;

    public FrequencyEditor(UserPreferences userPreferences, RadioReference radioReference,
                           PlaylistManager playlistManager, Level level)
    {
        mUserPreferences = userPreferences;
        mRadioReference = radioReference;
        mPlaylistManager = playlistManager;
        mLevel = level;

        GridPane gridPane = new GridPane();
        gridPane.setVgap(5);
        gridPane.setHgap(5);

        int row = 0;

        Label detailsLabel = new Label("Radio Reference Details");
        GridPane.setConstraints(detailsLabel, 1, row);
        gridPane.getChildren().add(detailsLabel);

        Label alphaLabel = new Label("Alpha Tag");
        GridPane.setHalignment(alphaLabel, HPos.RIGHT);
        GridPane.setConstraints(alphaLabel, 0, ++row);
        gridPane.getChildren().add(alphaLabel);

        GridPane.setHgrow(getAlphaTagTextField(), Priority.ALWAYS);
        GridPane.setConstraints(getAlphaTagTextField(), 1, row);
        gridPane.getChildren().add(getAlphaTagTextField());

        Label toneLabel = new Label("Tone");
        GridPane.setHalignment(toneLabel, HPos.RIGHT);
        GridPane.setConstraints(toneLabel, 0, ++row);
        gridPane.getChildren().add(toneLabel);

        GridPane.setHgrow(getToneTextField(), Priority.ALWAYS);
        GridPane.setConstraints(getToneTextField(), 1, row);
        gridPane.getChildren().add(getToneTextField());

        Label modeLabel = new Label("Mode");
        GridPane.setHalignment(modeLabel, HPos.RIGHT);
        GridPane.setConstraints(modeLabel, 0, ++row);
        gridPane.getChildren().add(modeLabel);

        GridPane.setHgrow(getModeTextField(), Priority.ALWAYS);
        GridPane.setConstraints(getModeTextField(), 1, row);
        gridPane.getChildren().add(getModeTextField());

        Label frequencyLabel = new Label("Frequency");
        GridPane.setHalignment(frequencyLabel, HPos.RIGHT);
        GridPane.setConstraints(frequencyLabel, 0, ++row);
        gridPane.getChildren().add(frequencyLabel);

        GridPane.setHgrow(getFrequencyTextField(), Priority.ALWAYS);
        GridPane.setConstraints(getFrequencyTextField(), 1, row);
        gridPane.getChildren().add(getFrequencyTextField());

        Separator separator = new Separator(Orientation.HORIZONTAL);
        GridPane.setConstraints(separator, 0, ++row, 2, 1);
        gridPane.getChildren().add(separator);

        Label createLabel = new Label("Create Channel Configuration");
        GridPane.setConstraints(createLabel, 1, ++row);
        gridPane.getChildren().add(createLabel);

        Label systemLabel = new Label("System");
        GridPane.setHalignment(systemLabel, HPos.RIGHT);
        GridPane.setConstraints(systemLabel, 0, ++row);
        gridPane.getChildren().add(systemLabel);

        GridPane.setHgrow(getSystemTextField(), Priority.ALWAYS);
        GridPane.setConstraints(getSystemTextField(), 1, row);
        gridPane.getChildren().add(getSystemTextField());

        Label siteLabel = new Label("Site");
        GridPane.setHalignment(siteLabel, HPos.RIGHT);
        GridPane.setConstraints(siteLabel, 0, ++row);
        gridPane.getChildren().add(siteLabel);

        GridPane.setHgrow(getSiteTextField(), Priority.ALWAYS);
        GridPane.setConstraints(getSiteTextField(), 1, row);
        gridPane.getChildren().add(getSiteTextField());

        Label nameLabel = new Label("Name");
        GridPane.setHalignment(nameLabel, HPos.RIGHT);
        GridPane.setConstraints(nameLabel, 0, ++row);
        gridPane.getChildren().add(nameLabel);

        GridPane.setHgrow(getNameTextField(), Priority.ALWAYS);
        GridPane.setConstraints(getNameTextField(), 1, row);
        gridPane.getChildren().add(getNameTextField());

        Label decoderLabel = new Label("Decoder");
        GridPane.setHalignment(decoderLabel, HPos.RIGHT);
        GridPane.setConstraints(decoderLabel, 0, ++row);
        gridPane.getChildren().add(decoderLabel);

        GridPane.setHgrow(getDecoderTextField(), Priority.ALWAYS);
        GridPane.setConstraints(getDecoderTextField(), 1, row);
        gridPane.getChildren().add(getDecoderTextField());

        GridPane.setConstraints(getShowCreatedChannelCheckBox(), 1, ++row);
        gridPane.getChildren().add(getShowCreatedChannelCheckBox());

        GridPane.setHgrow(getCreateButton(), Priority.ALWAYS);
        GridPane.setMargin(getCreateButton(), new Insets(10,0,0,0));
        GridPane.setConstraints(getCreateButton(), 1, ++row);
        gridPane.getChildren().add(getCreateButton());

        GridPane.setConstraints(getChannelCreatedLabel(), 1, ++row);
        gridPane.getChildren().add(getChannelCreatedLabel());

        getChildren().add(gridPane);
    }

    public void setItem(Frequency item, Category category, SubCategory subCategory)
    {
        mModeDecoderType = ModeDecoderType.UNKNOWN;

        if(item != null)
        {
            mFrequency = (long)(item.getDownlink() * 1E6);
            getAlphaTagTextField().setText(item.getAlphaTag());
            getFrequencyTextField().setText(FREQUENCY_FORMATTER.format(item.getDownlink()));
            getToneTextField().setText(item.getTone());
            updateMode(item.getMode());
            getSystemTextField().setText(category != null ? category.getName() : null);
            getSiteTextField().setText(subCategory != null ? subCategory.getName() : null);
            getNameTextField().setText(item.getAlphaTag());
        }
        else
        {
            mFrequency = 0;
            getAlphaTagTextField().setText(null);
            getFrequencyTextField().setText(null);
            getModeTextField().setText(null);
            getToneTextField().setText(null);
            getSystemTextField().setText(null);
            getSiteTextField().setText(null);
            getNameTextField().setText(null);

            getSystemTextField().setDisable(true);
            getSiteTextField().setDisable(true);
            getNameTextField().setDisable(true);
            getCreateButton().setDisable(true);
            getShowCreatedChannelCheckBox().setDisable(true);
        }
    }

    /**
     * Updates the mode text field and related controls to indicate if a channel configuration can be created.
     */
    private void updateMode(String modeId)
    {
        if(modeId != null)
        {
            ThreadPool.CACHED.execute(() -> {
                Integer parsed = null;

                try
                {
                    parsed = Integer.parseInt(modeId);
                }
                catch(Exception e)
                {
                    //Do nothing, we couldn't parse the value
                }

                if(parsed != null)
                {
                    try
                    {
                        Mode mode = mRadioReference.getService().getMode(parsed);
                        mModeDecoderType = ModeDecoderType.get(mode);

                        Platform.runLater(() -> {
                            getModeTextField().setText(mode.getName());

                            boolean disable = !mModeDecoderType.hasDecoderType();

                            getSystemTextField().setDisable(disable);
                            getSiteTextField().setDisable(disable);
                            getNameTextField().setDisable(disable);
                            getCreateButton().setDisable(disable);
                            getShowCreatedChannelCheckBox().setDisable(disable);

                            if(mModeDecoderType.hasDecoderType())
                            {
                                getDecoderTextField().setText(mModeDecoderType.getDecoderType().getDisplayString());
                            }
                            else
                            {
                                getDecoderTextField().setText(mode.getName() + " - Not Supported");
                            }
                        });
                    }
                    catch(Throwable t)
                    {
                        mLog.error("Error retrieving mode from radio reference", t);
                    }
                }
            });
        }
        else
        {
            getModeTextField().setText(null);
        }
    }

    private TextField getAlphaTagTextField()
    {
        if(mAlphaTagTextField == null)
        {
            mAlphaTagTextField = new TextField();
            mAlphaTagTextField.setMaxWidth(Double.MAX_VALUE);
            mAlphaTagTextField.setDisable(true);
        }

        return mAlphaTagTextField;
    }

    private TextField getFrequencyTextField()
    {
        if(mFrequencyTextField == null)
        {
            mFrequencyTextField = new TextField();
            mFrequencyTextField.setMaxWidth(Double.MAX_VALUE);
            mFrequencyTextField.setDisable(true);
        }

        return mFrequencyTextField;
    }

    private TextField getModeTextField()
    {
        if(mModeTextField == null)
        {
            mModeTextField = new TextField();
            mModeTextField.setMaxWidth(Double.MAX_VALUE);
            mModeTextField.setDisable(true);
        }

        return mModeTextField;
    }

    private TextField getToneTextField()
    {
        if(mToneTextField == null)
        {
            mToneTextField = new TextField();
            mToneTextField.setMaxWidth(Double.MAX_VALUE);
            mToneTextField.setDisable(true);
        }

        return mToneTextField;
    }

    private CheckBox getShowCreatedChannelCheckBox()
    {
        if(mShowCreatedChannelCheckBox == null)
        {
            boolean show = mUserPreferences.getRadioReferencePreference().getShowChannelEditor(mLevel);
            mShowCreatedChannelCheckBox = new CheckBox("View Channel Editor After Create");
            mShowCreatedChannelCheckBox.setDisable(true);
            mShowCreatedChannelCheckBox.selectedProperty().set(show);
            mShowCreatedChannelCheckBox.selectedProperty()
                .addListener((observable, oldValue, newValue) -> mUserPreferences.getRadioReferencePreference()
                    .setShowChannelEditor(newValue, mLevel));
        }

        return mShowCreatedChannelCheckBox;
    }

    private Label getChannelCreatedLabel()
    {
        if(mChannelCreatedLabel == null)
        {
            mChannelCreatedLabel = new Label("Channel Created Successfully");
            mChannelCreatedLabel.setDisable(true);
            mChannelCreatedLabel.setOpacity(0);
        }

        return mChannelCreatedLabel;
    }

    private Button getCreateButton()
    {
        if(mCreateButton == null)
        {
            mCreateButton = new Button("Create");
            mCreateButton.setDisable(true);
            mCreateButton.setOnAction(event -> {
                Channel channel = createChannel(mModeDecoderType, mFrequency, getSystemTextField().getText(),
                    getSiteTextField().getText(), getNameTextField().getText());

                if(channel != null)
                {
                    getCreateButton().setDisable(true);
                    mPlaylistManager.getChannelModel().addChannel(channel);

                    if(getShowCreatedChannelCheckBox().selectedProperty().get())
                    {
                        MyEventBus.getGlobalEventBus().post(new ViewChannelRequest(channel));
                    }
                    else
                    {
                        getChannelCreatedLabel().setOpacity(1.0);
                        FadeTransition transition = new FadeTransition(Duration.seconds(2), getChannelCreatedLabel());
                        transition.setDelay(Duration.seconds(1));
                        transition.setToValue(0.0);
                        transition.play();
                    }
                }
            });
        }

        return mCreateButton;
    }

    /**
     * Creates a channel configuration from the supplied values
     * @param modeDecoderType indicating the decoder type
     * @param frequency value
     * @param system value
     * @param site value
     * @param name value
     * @return configured channel or null
     */
    private Channel createChannel(ModeDecoderType modeDecoderType, long frequency, String system, String site, String name)
    {
        if(modeDecoderType.hasDecoderType())
        {
            Channel channel = new Channel();
            channel.setSystem(system);
            channel.setSite(site);
            channel.setName(name);
            SourceConfigTuner sourceConfigTuner = new SourceConfigTuner();
            sourceConfigTuner.setFrequency(frequency);
            channel.setSourceConfiguration(sourceConfigTuner);
            DecodeConfiguration decodeConfiguration = DecoderFactory.getDecodeConfiguration(modeDecoderType.getDecoderType());

            if(decodeConfiguration instanceof DecodeConfigNBFM && modeDecoderType == ModeDecoderType.FM)
            {
                ((DecodeConfigNBFM)decodeConfiguration).setBandwidth(DecodeConfigNBFM.Bandwidth.BW_25_0);
            }
            else if(decodeConfiguration instanceof DecodeConfigP25Phase1)
            {
                ((DecodeConfigP25Phase1)decodeConfiguration).setModulation(P25P1Decoder.Modulation.C4FM);
            }
            channel.setDecodeConfiguration(decodeConfiguration);
            return channel;
        }
        else
        {
            mLog.warn("Can't create channel configuration for [" + modeDecoderType.name() + "] no supported decoder type");
        }

        return null;
    }

    private TextField getSystemTextField()
    {
        if(mSystemTextField == null)
        {
            mSystemTextField = new TextField();
            mSystemTextField.setMaxWidth(Double.MAX_VALUE);
            mSystemTextField.setDisable(true);
        }

        return mSystemTextField;
    }

    private TextField getSiteTextField()
    {
        if(mSiteTextField == null)
        {
            mSiteTextField = new TextField();
            mSiteTextField.setMaxWidth(Double.MAX_VALUE);
            mSiteTextField.setDisable(true);
        }

        return mSiteTextField;
    }

    private TextField getNameTextField()
    {
        if(mNameTextField == null)
        {
            mNameTextField = new TextField();
            mNameTextField.setMaxWidth(Double.MAX_VALUE);
            mNameTextField.setDisable(true);
        }

        return mNameTextField;
    }

    private TextField getDecoderTextField()
    {
        if(mDecoderTextField == null)
        {
            mDecoderTextField = new TextField();
            mDecoderTextField.setMaxWidth(Double.MAX_VALUE);
            mDecoderTextField.setDisable(true);
        }

        return mDecoderTextField;
    }
}
