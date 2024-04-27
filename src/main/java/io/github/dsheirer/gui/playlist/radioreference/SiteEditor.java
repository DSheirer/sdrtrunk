/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
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

import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.gui.control.MaxLengthUnaryOperator;
import io.github.dsheirer.gui.playlist.channel.ViewChannelRequest;
import io.github.dsheirer.module.decode.DecoderFactory;
import io.github.dsheirer.module.decode.DecoderType;
import io.github.dsheirer.module.decode.config.DecodeConfiguration;
import io.github.dsheirer.module.decode.dmr.DecodeConfigDMR;
import io.github.dsheirer.module.decode.dmr.channel.TimeslotFrequency;
import io.github.dsheirer.module.decode.p25.phase1.DecodeConfigP25Phase1;
import io.github.dsheirer.module.decode.p25.phase1.P25P1Decoder;
import io.github.dsheirer.module.decode.p25.phase2.DecodeConfigP25Phase2;
import io.github.dsheirer.module.decode.p25.phase2.enumeration.ScrambleParameters;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.rrapi.type.Flavor;
import io.github.dsheirer.rrapi.type.RadioNetwork;
import io.github.dsheirer.rrapi.type.Site;
import io.github.dsheirer.rrapi.type.SiteFrequency;
import io.github.dsheirer.rrapi.type.System;
import io.github.dsheirer.rrapi.type.SystemInformation;
import io.github.dsheirer.source.config.SourceConfigTuner;
import io.github.dsheirer.source.config.SourceConfigTunerMultipleFrequency;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import javafx.animation.RotateTransition;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.HPos;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.Callback;
import javafx.util.Duration;
import org.controlsfx.control.SegmentedButton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SiteEditor extends GridPane
{
    private static final Logger mLog = LoggerFactory.getLogger(SiteEditor.class);

    private static final String ALTERNATE_CONTROL_CHANNEL = "a";
    private static final String PRIMARY_CONTROL_CHANNEL = "d";
    private static final String TOGGLE_BUTTON_CONTROL = "Control";
    private static final String TOGGLE_BUTTON_P25_VOICE = "All P25 Voice";
    private static final String PHASE_2_TDMA_MODULATION = "TDMA";
    private static final String PHASE_2_FLAVOR = "Phase II";

    private UserPreferences mUserPreferences;
    private PlaylistManager mPlaylistManager;
    private TableView<SiteFrequency> mSiteFrequencyTableView;
    private TableColumn<SiteFrequency,String> mTypeColumn;
    private SegmentedButton mFrequenciesSegmentedButton;
    private ToggleButton mControlToggleButton;
    private ToggleButton mControlAndAltToggleButton;
    private ToggleButton mSelectedToggleButton;
    private ToggleButton mAllToggleButton;
    private SegmentedButton mConfigurationsSegmentedButton;
    private ToggleButton mSingleToggleButton;
    private ToggleButton mForEachToggleButton;
    private Button mCreateChannelConfigurationButton;
    private TextField mSystemTextField;
    private TextField mSiteTextField;
    private TextField mNameTextField;
    private CheckBox mGoToChannelEditorCheckBox;
    private Label mProtocolNotSupportedLabel;
    private RadioReferenceDecoder mRadioReferenceDecoder;
    private EnrichedSite mCurrentSite;
    private System mCurrentSystem;
    private SystemInformation mCurrentSystemInformation;
    private ComboBox mAliasListNameComboBox;
    private Button mNewAliasListButton;
    private Label mP25ControlLabel;
    private SegmentedButton mP25ControlSegmentedButton;
    private ToggleButton mFdmaControlToggleButton;
    private ToggleButton mTdmaControlToggleButton;

    public SiteEditor(UserPreferences userPreferences, PlaylistManager playlistManager)
    {
        mUserPreferences = userPreferences;
        mPlaylistManager = playlistManager;

        setMaxHeight(Double.MAX_VALUE);
        setHgap(5.0);
        setVgap(5.0);

        ColumnConstraints column1 = new ColumnConstraints();
        column1.setPercentWidth(40);
        ColumnConstraints column2 = new ColumnConstraints();
        ColumnConstraints column3 = new ColumnConstraints();
        getColumnConstraints().addAll(column1, column2, column3);

        int row = 0;

        Label siteFrequencies = new Label("Frequencies");
        GridPane.setConstraints(siteFrequencies, 0, row);
        getChildren().add(siteFrequencies);

        Label createChannelLabel = new Label("Create Channel Configuration(s)");
        GridPane.setConstraints(createChannelLabel, 2, row);
        GridPane.setHalignment(createChannelLabel, HPos.LEFT);
        getChildren().add(createChannelLabel);

        GridPane.setConstraints(getSiteFrequencyTableView(), 0, ++row, 1, 10);
        GridPane.setHgrow(getSiteFrequencyTableView(), Priority.ALWAYS);
        GridPane.setVgrow(getSiteFrequencyTableView(), Priority.ALWAYS);
        getChildren().add(getSiteFrequencyTableView());

        Label frequenciesLabel = new Label("Frequencies:");
        GridPane.setConstraints(frequenciesLabel, 1, row);
        GridPane.setHalignment(frequenciesLabel, HPos.RIGHT);
        getChildren().add(frequenciesLabel);

        GridPane.setConstraints(getFrequenciesSegmentedButton(), 2, row);
        getChildren().add(getFrequenciesSegmentedButton());

        Label configurationsLabel = new Label("Configurations:");
        GridPane.setConstraints(configurationsLabel, 1, ++row);
        GridPane.setHalignment(configurationsLabel, HPos.RIGHT);
        getChildren().add(configurationsLabel);

        GridPane.setConstraints(getConfigurationsSegmentedButton(), 2, row);
        getChildren().add(getConfigurationsSegmentedButton());

        GridPane.setConstraints(getP25ControlLabel(), 1, ++row);
        GridPane.setHalignment(getP25ControlLabel(), HPos.RIGHT);
        getChildren().add(getP25ControlLabel());

        GridPane.setConstraints(getP25ControlSegmentedButton(), 2, row);
        getChildren().add(getP25ControlSegmentedButton());

        Label systemLabel = new Label("System");
        GridPane.setConstraints(systemLabel, 1, ++row);
        GridPane.setHalignment(systemLabel, HPos.RIGHT);
        getChildren().add(systemLabel);

        GridPane.setConstraints(getSystemTextField(), 2, row);
        GridPane.setHgrow(getSystemTextField(), Priority.ALWAYS);
        getChildren().add(getSystemTextField());

        Label siteLabel = new Label("Site");
        GridPane.setConstraints(siteLabel, 1, ++row);
        GridPane.setHalignment(siteLabel, HPos.RIGHT);
        getChildren().add(siteLabel);

        GridPane.setConstraints(getSiteTextField(), 2, row);
        GridPane.setHgrow(getSiteTextField(), Priority.ALWAYS);
        getChildren().add(getSiteTextField());

        Label nameLabel = new Label("Name");
        GridPane.setConstraints(nameLabel, 1, ++row);
        GridPane.setHalignment(nameLabel, HPos.RIGHT);
        getChildren().add(nameLabel);

        GridPane.setConstraints(getNameTextField(), 2, row);
        GridPane.setHgrow(getNameTextField(), Priority.ALWAYS);
        getChildren().add(getNameTextField());

        Label aliasListLabel = new Label("Alias List");
        GridPane.setConstraints(aliasListLabel, 1, ++row);
        GridPane.setHalignment(aliasListLabel, HPos.RIGHT);
        getChildren().add(aliasListLabel);

        GridPane.setConstraints(getAliasListNameComboBox(), 2, row);
        GridPane.setHgrow(getAliasListNameComboBox(), Priority.ALWAYS);
        getChildren().add(getAliasListNameComboBox());

        GridPane.setConstraints(getNewAliasListButton(), 2, ++row);
        getChildren().add(getNewAliasListButton());

        HBox createBox = new HBox();
        createBox.setAlignment(Pos.CENTER_LEFT);
        createBox.setSpacing(10);
        createBox.getChildren().addAll(getCreateChannelConfigurationButton(), getGoToChannelEditorCheckBox());
        GridPane.setConstraints(createBox, 2, ++row);
        getChildren().addAll(createBox);

        //Note: the following label node is added to the same location as the buttons and visibility is toggled
        //according to if the protocol is supported
        GridPane.setConstraints(getProtocolNotSupportedLabel(), 2, row);
        getChildren().add(getProtocolNotSupportedLabel());
    }

    /**
     * Creates a channel decode configuration for the specified site.
     * @param decoderType to create
     * @param site information and frequencies
     * @param systemInformation with frequency mapping
     * @return decode configuration
     */
    private DecodeConfiguration getDecodeConfiguration(DecoderType decoderType, Site site, SystemInformation systemInformation)
    {
        if(decoderType == null)
        {
            return null;
        }

        switch(decoderType)
        {
            case DMR:
                DecodeConfigDMR dmr = new DecodeConfigDMR();
                List<TimeslotFrequency> timeslotFrequencies = mRadioReferenceDecoder
                    .getTimeslotFrequencies(systemInformation, site);
                dmr.setTimeslotMap(timeslotFrequencies);
                return dmr;
            case P25_PHASE1:
                DecodeConfiguration p1config = DecoderFactory.getDecodeConfiguration(decoderType);

                if(mRadioReferenceDecoder.isLSM(site))
                {
                    if(p1config instanceof DecodeConfigP25Phase1)
                    {
                        ((DecodeConfigP25Phase1)p1config).setModulation(P25P1Decoder.Modulation.CQPSK);
                    }
                }
                return p1config;
            case P25_PHASE2:
                DecodeConfigP25Phase2 config = new DecodeConfigP25Phase2();

                int nac = 0;

                if(site != null && site.getNac() != null)
                {
                    try
                    {
                        nac = Integer.parseInt(site.getNac(), 16);
                    }
                    catch(Exception e)
                    {
                        //Do nothing
                    }
                }

                int wacn = 0;
                int system = 0;

                if(systemInformation != null && systemInformation.getRadioNetworks() != null &&
                    !systemInformation.getRadioNetworks().isEmpty())
                {
                    RadioNetwork radioNetwork = systemInformation.getRadioNetworks().get(0);

                    if(radioNetwork.getWacn() != null)
                    {
                        try
                        {
                            wacn = Integer.parseInt(radioNetwork.getWacn(), 16);
                        }
                        catch(Exception e)
                        {
                            //Do nothing
                        }
                    }

                    if(radioNetwork.getSystemId() != null)
                    {
                        try
                        {
                            system = Integer.parseInt(radioNetwork.getSystemId(), 16);
                        }
                        catch(Exception e)
                        {
                            //Do nothing
                        }
                    }
                }

                config.setScrambleParameters(new ScrambleParameters(wacn, system, nac));
                return config;
            default:
                return DecoderFactory.getDecodeConfiguration(decoderType);
        }
    }

    /**
     * Loads the user selected site info into this editor.
     * @param site to load
     * @param system for the site
     * @param systemInformation for the site
     * @param decoder to lookup supplemental information
     */
    public void setSite(EnrichedSite site, System system, SystemInformation systemInformation, RadioReferenceDecoder decoder)
    {
        mCurrentSite = site;
        mCurrentSystem = system;
        mCurrentSystemInformation = systemInformation;
        mRadioReferenceDecoder = decoder;

        getSiteFrequencyTableView().getItems().clear();

        boolean disable = site == null || site.getSite().getSiteFrequencies().isEmpty();
        boolean supported = decoder.hasSupportedProtocol(system);

        getFrequenciesSegmentedButton().setDisable(disable || !supported);
        getConfigurationsSegmentedButton().setDisable(disable || !supported);
        getSystemTextField().setDisable(disable || !supported);
        getSiteTextField().setDisable(disable || !supported);
        getNameTextField().setDisable(disable || !supported);
        getCreateChannelConfigurationButton().setDisable(disable || !supported);
        getGoToChannelEditorCheckBox().setDisable(disable || !supported);
        getAliasListNameComboBox().setDisable(disable || !supported);
        getNewAliasListButton().setDisable(disable || !supported);

        getCreateChannelConfigurationButton().setVisible(false);
        getGoToChannelEditorCheckBox().setVisible(false);
        getProtocolNotSupportedLabel().setVisible(false);

        if(site != null)
        {
            List<SiteFrequency> siteFrequencies = site.getSite().getSiteFrequencies();
            for(SiteFrequency siteFrequency: siteFrequencies)
            {
                if(siteFrequency.getFrequency() > 0.01)
                {
                    getSiteFrequencyTableView().getItems().add(siteFrequency);
                }
            }
            mSiteFrequencyTableView.getSortOrder().clear();
            mTypeColumn.setSortType(TableColumn.SortType.DESCENDING);
            mSiteFrequencyTableView.getSortOrder().addAll(mTypeColumn);
            getSiteFrequencyTableView().sort();

            if(!supported)
            {
                getProtocolNotSupportedLabel().setVisible(true);
            }
            else
            {
                Flavor flavor = decoder.getFlavor(system);

                if(flavor != null && flavor.getName() != null && flavor.getName().equals(PHASE_2_FLAVOR))
                {
                    getP25ControlLabel().setVisible(true);
                    getTdmaControlToggleButton().setVisible(true);
                    getFdmaControlToggleButton().setVisible(true);

                    if(site.getSite().getTdmaControlChannel() > 0) //Value is 0 for FDMA or 1 for TDMA
                    {
                        getTdmaControlToggleButton().setSelected(true);
                    }
                    else
                    {
                        getFdmaControlToggleButton().setSelected(true);
                    }
                }
                else
                {
                    getP25ControlLabel().setVisible(false);
                    getTdmaControlToggleButton().setVisible(false);
                    getFdmaControlToggleButton().setVisible(false);
                }

                getCreateChannelConfigurationButton().setVisible(true);
                getGoToChannelEditorCheckBox().setVisible(true);
                getSystemTextField().setText(system.getName());
                getSiteTextField().setText(mCurrentSite.getCountyName());
                getNameTextField().setText("Control");

                if(!siteFrequencies.isEmpty())
                {
                    boolean hasControl = hasControl(siteFrequencies);
                    boolean hasAlternate = hasAlternate(siteFrequencies);
                    boolean p25Hybrid = decoder.isHybridMotorolaP25(system);
                    boolean ltr = mRadioReferenceDecoder.isLTR(system);

                    //Set visibility for control, control & alt, and p25 traffic in frequencies toggle group
                    //For Motorola Type II with P25 Voice channel hybrid systems, we customize the first toggle button

                    if(ltr)
                    {
                        getControlToggleButton().setDisable(true);
                        getControlToggleButton().setText(TOGGLE_BUTTON_CONTROL);
                        getControlToggleButton().setSelected(false);
                        getControlAndAltToggleButton().setDisable(true);
                        getControlAndAltToggleButton().setSelected(false);
                        getSelectedToggleButton().setDisable(false);
                        getSelectedToggleButton().setSelected(false);
                        getAllToggleButton().setDisable(false);
                        getAllToggleButton().setSelected(true);

                        getSingleToggleButton().setDisable(true);
                        getSingleToggleButton().setSelected(false);
                        getForEachToggleButton().setDisable(false);
                        getForEachToggleButton().setSelected(true);
                    }
                    else if(p25Hybrid)
                    {
                        getControlToggleButton().setDisable(false);
                        getControlToggleButton().setText(TOGGLE_BUTTON_P25_VOICE);
                        getControlToggleButton().setSelected(true);
                        getControlAndAltToggleButton().setDisable(true);
                        getControlAndAltToggleButton().setSelected(false);
                        getSelectedToggleButton().setDisable(false);
                        getSelectedToggleButton().setSelected(false);
                        getAllToggleButton().setDisable(true);
                        getAllToggleButton().setSelected(false);
                    }
                    else
                    {
                        getControlToggleButton().setDisable(!hasControl);
                        getControlToggleButton().setText(TOGGLE_BUTTON_CONTROL);
                        getControlAndAltToggleButton().setDisable(!hasAlternate);
                        getSelectedToggleButton().setDisable(false);
                        getAllToggleButton().setDisable(false);

                        if(hasControl)
                        {
                            getControlToggleButton().setSelected(true);
                            getControlAndAltToggleButton().setSelected(false);
                            getSelectedToggleButton().setSelected(false);
                            getAllToggleButton().setSelected(false);
                        }
                        else if(hasAlternate)
                        {
                            getControlToggleButton().setSelected(false);
                            getControlAndAltToggleButton().setSelected(true);
                            getSelectedToggleButton().setSelected(false);
                            getAllToggleButton().setSelected(false);
                        }
                        else
                        {
                            getControlToggleButton().setSelected(false);
                            getControlAndAltToggleButton().setSelected(false);
                            getSelectedToggleButton().setSelected(false);
                            getAllToggleButton().setSelected(true);
                        }

                        getSingleToggleButton().setDisable(false);
                        getSingleToggleButton().setSelected(true);
                        getForEachToggleButton().setDisable(false);
                        getForEachToggleButton().setSelected(false);
                    }
                }
            }
        }
        else
        {
            getSystemTextField().setText(null);
            getSiteTextField().setText(null);
            getNameTextField().setText(null);
        }
    }

    /**
     * Creates an channel with the system, site and name values filled in from the editor control contents.
     */
    private Channel getChannelTemplate()
    {
        Channel channel = new Channel();
        channel.setName(getNameTextField().getText());
        channel.setSite(getSiteTextField().getText());
        channel.setSystem(getSystemTextField().getText());
        channel.setAliasListName(getAliasListNameComboBox().getSelectionModel().getSelectedItem());
        return channel;
    }

    /**
     * Creates a P25 Phase 1 C4FM channel configuration for each non-control site frequency
     */
    private void createHybridP25VoiceChannels()
    {
        Channel gotoChannel = null;

        for(SiteFrequency siteFrequency: getSiteFrequencyTableView().getItems())
        {
            //Don't create a channel configuration for the Motorola Type II control channel - not currently supported
            if(!isControl(siteFrequency) && siteFrequency.getFrequency() > 0.01)
            {
                Channel channel = getChannelTemplate();
                channel.setName("LCN " + siteFrequency.getLogicalChannelNumber());
                DecoderType decoderType = mRadioReferenceDecoder.getDecoderType(mCurrentSystem);
                channel.setDecodeConfiguration(getDecodeConfiguration(decoderType, mCurrentSite.getSite(),
                    mCurrentSystemInformation));
                SourceConfigTuner sourceConfigTuner = new SourceConfigTuner();
                sourceConfigTuner.setFrequency(getFrequency(siteFrequency));
                channel.setSourceConfiguration(sourceConfigTuner);

                if(gotoChannel == null)
                {
                    gotoChannel = channel;
                }

                mPlaylistManager.getChannelModel().addChannel(channel);
            }
        }

        if(getGoToChannelEditorCheckBox().isSelected() && gotoChannel != null)
        {
            MyEventBus.getGlobalEventBus().post(new ViewChannelRequest(gotoChannel));
        }
    }

    private void createControlChannel()
    {
        List<SiteFrequency> controls = new ArrayList<>();

        for(SiteFrequency siteFrequency: getSiteFrequencyTableView().getItems())
        {
            if(isControl(siteFrequency) && siteFrequency.getFrequency() > 0.01)
            {
                controls.add(siteFrequency);
            }
        }

        if(controls.isEmpty())
        {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Site Has No Control Channel(s)", ButtonType.OK);
            alert.setTitle("Create Channel Configuration");
            alert.setHeaderText("Can't Create Channel Configuration");
            alert.initOwner((getCreateChannelConfigurationButton()).getScene().getWindow());
            alert.showAndWait();
            return;
        }

        if(mRadioReferenceDecoder.hasSupportedProtocol(mCurrentSystem))
        {
            Channel channel = getChannelTemplate();

            DecoderType decoderType = mRadioReferenceDecoder.getDecoderType(mCurrentSystem);

            //Phase 2 - inspect the site modulation and use Phase 2 for TDMA control channel, otherwise Phase 1
            if(decoderType == DecoderType.P25_PHASE2)
            {
                if(getFdmaControlToggleButton().isSelected())
                {
                    decoderType = DecoderType.P25_PHASE1;
                }
            }

            channel.setDecodeConfiguration(getDecodeConfiguration(decoderType, mCurrentSite.getSite(), mCurrentSystemInformation));

            if(controls.size() == 1)
            {
                SourceConfigTuner sourceConfigTuner = new SourceConfigTuner();
                sourceConfigTuner.setFrequency((long)(controls.get(0).getFrequency() * 1E6));
                channel.setSourceConfiguration(sourceConfigTuner);
            }
            else
            {
                SourceConfigTunerMultipleFrequency sourceConfig = new SourceConfigTunerMultipleFrequency();

                for(SiteFrequency control: controls)
                {
                    sourceConfig.addFrequency((long)(control.getFrequency() * 1E6));
                }

                channel.setSourceConfiguration(sourceConfig);
            }

            mPlaylistManager.getChannelModel().addChannel(channel);

            if(getGoToChannelEditorCheckBox().isSelected())
            {
                MyEventBus.getGlobalEventBus().post(new ViewChannelRequest(channel));
            }
        }
    }

    private void createControlAndAlternatesChannel()
    {
        List<SiteFrequency> siteFrequencies = new ArrayList<>();

        for(SiteFrequency siteFrequency: getSiteFrequencyTableView().getItems())
        {
            if(isControl(siteFrequency) && siteFrequency.getFrequency() > 0.01)
            {
                siteFrequencies.add(siteFrequency);
            }
        }

        for(SiteFrequency siteFrequency: getSiteFrequencyTableView().getItems())
        {
            if(isAlternate(siteFrequency) && siteFrequency.getFrequency() > 0.01)
            {
                siteFrequencies.add(siteFrequency);
            }
        }

        if(siteFrequencies.isEmpty())
        {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Site Has No Control or Alternate Channel(s)", ButtonType.OK);
            alert.setTitle("Create Channel Configuration");
            alert.setHeaderText("Can't Create Channel Configuration");
            alert.initOwner((getCreateChannelConfigurationButton()).getScene().getWindow());
            alert.showAndWait();
            return;
        }

        if(mRadioReferenceDecoder.hasSupportedProtocol(mCurrentSystem))
        {
            if(getSingleToggleButton().isSelected())
            {
                Channel channel = getChannelTemplate();

                DecoderType decoderType = mRadioReferenceDecoder.getDecoderType(mCurrentSystem);

                //Phase 2 - inspect the site modulation and use Phase 2 for TDMA control channel, otherwise Phase 1
                if(decoderType == DecoderType.P25_PHASE2)
                {
                    if(getFdmaControlToggleButton().isSelected())
                    {
                        decoderType = DecoderType.P25_PHASE1;
                    }
                }

                channel.setDecodeConfiguration(getDecodeConfiguration(decoderType, mCurrentSite.getSite(),
                    mCurrentSystemInformation));

                if(siteFrequencies.size() == 1)
                {
                    SourceConfigTuner sourceConfigTuner = new SourceConfigTuner();
                    sourceConfigTuner.setFrequency(getFrequency(siteFrequencies.get(0)));
                    channel.setSourceConfiguration(sourceConfigTuner);
                }
                else
                {
                    SourceConfigTunerMultipleFrequency sourceConfig = new SourceConfigTunerMultipleFrequency();
                    List<Long> frequencies = new ArrayList<>();
                    for(SiteFrequency siteFrequency: siteFrequencies)
                    {
                        frequencies.add(getFrequency(siteFrequency));
                    }
                    sourceConfig.setFrequencies(frequencies);
                    channel.setSourceConfiguration(sourceConfig);
                }

                mPlaylistManager.getChannelModel().addChannel(channel);

                if(getGoToChannelEditorCheckBox().isSelected())
                {
                    MyEventBus.getGlobalEventBus().post(new ViewChannelRequest(channel));
                }
            }
            else
            {
                Channel gotoChannel = null;

                for(SiteFrequency siteFrequency: siteFrequencies)
                {
                    Channel channel = getChannelTemplate();
                    channel.setName("LCN " + siteFrequency.getLogicalChannelNumber());
                    DecoderType decoderType = mRadioReferenceDecoder.getDecoderType(mCurrentSystem);
                    channel.setDecodeConfiguration(getDecodeConfiguration(decoderType, mCurrentSite.getSite(),
                        mCurrentSystemInformation));
                    SourceConfigTuner sourceConfigTuner = new SourceConfigTuner();
                    sourceConfigTuner.setFrequency(getFrequency(siteFrequency));
                    channel.setSourceConfiguration(sourceConfigTuner);

                    if(gotoChannel == null)
                    {
                        gotoChannel = channel;
                    }

                    mPlaylistManager.getChannelModel().addChannel(channel);
                }

                if(getGoToChannelEditorCheckBox().isSelected() && gotoChannel != null)
                {
                    MyEventBus.getGlobalEventBus().post(new ViewChannelRequest(gotoChannel));
                }
            }
        }
    }

    private void createChannels(boolean selectedOnly)
    {
        List<SiteFrequency> siteFrequencies = new ArrayList<>((selectedOnly ? getSiteFrequencyTableView()
            .getSelectionModel().getSelectedItems() : getSiteFrequencyTableView().getItems()));

        //Remove 0 frequency values
        Iterator<SiteFrequency> it = siteFrequencies.iterator();
        while(it.hasNext())
        {
            if(it.next().getFrequency() < 0.01)
            {
                it.remove();
            }
        }

        if(siteFrequencies.isEmpty())
        {
            String context = (selectedOnly ? "Please select frequencies" : "Site has no channel frequencies");
            Alert alert = new Alert(Alert.AlertType.ERROR, context, ButtonType.OK);
            alert.setTitle("Create Channel Configuration");
            alert.setHeaderText("Can't Create Channel Configuration");
            alert.initOwner((getCreateChannelConfigurationButton()).getScene().getWindow());
            alert.showAndWait();
            return;
        }

        if(mRadioReferenceDecoder.hasSupportedProtocol(mCurrentSystem))
        {
            if(getSingleToggleButton().isSelected())
            {
                Channel channel = getChannelTemplate();

                DecoderType decoderType = mRadioReferenceDecoder.getDecoderType(mCurrentSystem);

                //Phase 2 - inspect the site modulation and use Phase 2 for TDMA control channel, otherwise Phase 1
                if(decoderType == DecoderType.P25_PHASE2)
                {
                    if(getFdmaControlToggleButton().isSelected())
                    {
                        decoderType = DecoderType.P25_PHASE1;
                    }
                }

                channel.setDecodeConfiguration(getDecodeConfiguration(decoderType, mCurrentSite.getSite(),
                    mCurrentSystemInformation));

                if(siteFrequencies.size() == 1)
                {
                    SourceConfigTuner sourceConfigTuner = new SourceConfigTuner();
                    sourceConfigTuner.setFrequency(getFrequency(siteFrequencies.get(0)));
                    channel.setSourceConfiguration(sourceConfigTuner);
                }
                else
                {
                    SourceConfigTunerMultipleFrequency sourceConfig = new SourceConfigTunerMultipleFrequency();
                    List<Long> frequencies = new ArrayList<>();
                    for(SiteFrequency siteFrequency: siteFrequencies)
                    {
                        frequencies.add(getFrequency(siteFrequency));
                    }
                    sourceConfig.setFrequencies(frequencies);
                    channel.setSourceConfiguration(sourceConfig);
                }

                mPlaylistManager.getChannelModel().addChannel(channel);

                if(getGoToChannelEditorCheckBox().isSelected())
                {
                    MyEventBus.getGlobalEventBus().post(new ViewChannelRequest(channel));
                }
            }
            else
            {
                Channel gotoChannel = null;

                for(SiteFrequency siteFrequency: siteFrequencies)
                {
                    Channel channel = getChannelTemplate();
                    channel.setName("LCN " + siteFrequency.getLogicalChannelNumber());
                    DecoderType decoderType = mRadioReferenceDecoder.getDecoderType(mCurrentSystem);
                    channel.setDecodeConfiguration(getDecodeConfiguration(decoderType, mCurrentSite.getSite(),
                        mCurrentSystemInformation));

                    SourceConfigTuner sourceConfigTuner = new SourceConfigTuner();
                    sourceConfigTuner.setFrequency(getFrequency(siteFrequency));
                    channel.setSourceConfiguration(sourceConfigTuner);

                    if(gotoChannel == null)
                    {
                        gotoChannel = channel;
                    }

                    mPlaylistManager.getChannelModel().addChannel(channel);
                }

                if(getGoToChannelEditorCheckBox().isSelected() && gotoChannel != null)
                {
                    MyEventBus.getGlobalEventBus().post(new ViewChannelRequest(gotoChannel));
                }
            }
        }
    }

    private static long getFrequency(SiteFrequency siteFrequency)
    {
        return (long)(siteFrequency.getFrequency() * 1E6);
    }

    /**
     * P25 Phase 2 control channel label.
     */
    private Label getP25ControlLabel()
    {
        if(mP25ControlLabel == null)
        {
            mP25ControlLabel = new Label("Control");
            mP25ControlLabel.setVisible(false);
        }

        return mP25ControlLabel;
    }

    /**
     * P25 Phase 2 control channel type (TDMA vs FDMA) selection buttons.
     */
    private SegmentedButton getP25ControlSegmentedButton()
    {
        if(mP25ControlSegmentedButton == null)
        {
            mP25ControlSegmentedButton = new SegmentedButton(getFdmaControlToggleButton(), getTdmaControlToggleButton());
            mP25ControlSegmentedButton.getStyleClass().add(SegmentedButton.STYLE_CLASS_DARK);
            mP25ControlSegmentedButton.getToggleGroup().selectedToggleProperty()
                    .addListener((observable, oldValue, newValue) -> {
                        //Ensure that one button is always selected
                        if(newValue == null)
                        {
                            oldValue.setSelected(true);
                        }
                    });
        }

        return mP25ControlSegmentedButton;
    }

    /**
     * P25 Phase 2 with FDMA (phase 1) control channel selection button.
     */
    private ToggleButton getFdmaControlToggleButton()
    {
        if(mFdmaControlToggleButton == null)
        {
            mFdmaControlToggleButton = new ToggleButton("FDMA Phase 1");
            mFdmaControlToggleButton.setVisible(false);
        }

        return mFdmaControlToggleButton;
    }

    /**
     * P25 Phase 2 with TDMA (phase 2) control channel selection button.
     */
    private ToggleButton getTdmaControlToggleButton()
    {
        if(mTdmaControlToggleButton == null)
        {
            mTdmaControlToggleButton = new ToggleButton("TDMA Phase 2");
            mTdmaControlToggleButton.setVisible(false);
        }

        return mTdmaControlToggleButton;
    }

    /**
     * Flashes the alias list combobox to let the user know that they must select an alias list
     */
    private void flashAliasListComboBox()
    {
        RotateTransition rt = new RotateTransition(Duration.millis(150), getAliasListNameComboBox());
        rt.setByAngle(5);
        rt.setCycleCount(4);
        rt.setAutoReverse(true);
        rt.play();
    }

    private ComboBox<String> getAliasListNameComboBox()
    {
        if(mAliasListNameComboBox == null)
        {
            Predicate<String> filterPredicate = s -> !s.contentEquals(AliasModel.NO_ALIAS_LIST);
            FilteredList<String> filteredChannelList =
                new FilteredList<>(mPlaylistManager.getAliasModel().aliasListNames(), filterPredicate);
            mAliasListNameComboBox = new ComboBox<>(filteredChannelList);
            mAliasListNameComboBox.setDisable(true);
            mAliasListNameComboBox.setMaxWidth(Double.MAX_VALUE);

            if(mAliasListNameComboBox.getItems().size() > 0)
            {
                mAliasListNameComboBox.getSelectionModel().select(0);
            }
            else
            {
                mAliasListNameComboBox.getSelectionModel().select(null);
            }
        }

        return mAliasListNameComboBox;
    }

    private Button getNewAliasListButton()
    {
        if(mNewAliasListButton == null)
        {
            mNewAliasListButton = new Button("New Alias List");
            mNewAliasListButton.setDisable(true);
            mNewAliasListButton.setOnAction(event -> {
                TextInputDialog dialog = new TextInputDialog();
                dialog.setTitle("Create New Alias List");
                dialog.setHeaderText("Please enter an alias list name (max 25 chars).");
                dialog.setContentText("Name:");
                dialog.getEditor().setTextFormatter(new TextFormatter<String>(new MaxLengthUnaryOperator(25)));
                Optional<String> result = dialog.showAndWait();

                result.ifPresent(s -> {
                    String name = result.get();

                    if(name != null && !name.isEmpty())
                    {
                        name = name.trim();
                        mPlaylistManager.getAliasModel().addAliasList(name);
                        getAliasListNameComboBox().getSelectionModel().select(name);
                    }
                });
            });
        }

        return mNewAliasListButton;
    }

    private Label getProtocolNotSupportedLabel()
    {
        if(mProtocolNotSupportedLabel == null)
        {
            mProtocolNotSupportedLabel = new Label("Protocol Not Supported");
            mProtocolNotSupportedLabel.setVisible(false);
        }

        return mProtocolNotSupportedLabel;
    }

    private SegmentedButton getConfigurationsSegmentedButton()
    {
        if(mConfigurationsSegmentedButton == null)
        {
            mConfigurationsSegmentedButton = new SegmentedButton(getSingleToggleButton(), getForEachToggleButton());
            mConfigurationsSegmentedButton.getStyleClass().add(SegmentedButton.STYLE_CLASS_DARK);
            mConfigurationsSegmentedButton.setDisable(true);
            mConfigurationsSegmentedButton.getToggleGroup().selectedToggleProperty()
                .addListener((observable, oldValue, newValue) -> {
                //Ensure that one button is always selected
                if(newValue == null)
                {
                    oldValue.setSelected(true);
                }
                else
                {
                    getNameTextField().setDisable(getForEachToggleButton().isSelected());
                }
            });
        }

        return mConfigurationsSegmentedButton;
    }

    private ToggleButton getSingleToggleButton()
    {
        if(mSingleToggleButton == null)
        {
            mSingleToggleButton = new ToggleButton("Single");
        }

        return mSingleToggleButton;
    }

    private ToggleButton getForEachToggleButton()
    {
        if(mForEachToggleButton == null)
        {
            mForEachToggleButton = new ToggleButton("For Each Frequency");
        }

        return mForEachToggleButton;
    }

    private SegmentedButton getFrequenciesSegmentedButton()
    {
        if(mFrequenciesSegmentedButton == null)
        {
            mFrequenciesSegmentedButton = new SegmentedButton(getControlToggleButton(), getControlAndAltToggleButton(),
                getSelectedToggleButton(), getAllToggleButton());
            mFrequenciesSegmentedButton.getStyleClass().add(SegmentedButton.STYLE_CLASS_DARK);
            mFrequenciesSegmentedButton.setDisable(true);
            mFrequenciesSegmentedButton.getToggleGroup().selectedToggleProperty()
                .addListener((observable, oldValue, newValue) -> {
                //Ensure that at least one of the toggles is selected at all times
                if(newValue == null)
                {
                    oldValue.setSelected(true);
                }
            });
        }

        return mFrequenciesSegmentedButton;
    }

    private ToggleButton getControlToggleButton()
    {
        if(mControlToggleButton == null)
        {
            mControlToggleButton = new ToggleButton("Control");
        }

        return mControlToggleButton;
    }

    private ToggleButton getControlAndAltToggleButton()
    {
        if(mControlAndAltToggleButton == null)
        {
            mControlAndAltToggleButton = new ToggleButton("Control & Alternates");
        }

        return mControlAndAltToggleButton;
    }

    private ToggleButton getSelectedToggleButton()
    {
        if(mSelectedToggleButton == null)
        {
            mSelectedToggleButton = new ToggleButton("Selected");
        }

        return mSelectedToggleButton;
    }

    private ToggleButton getAllToggleButton()
    {
        if(mAllToggleButton == null)
        {
            mAllToggleButton = new ToggleButton("All");
        }

        return mAllToggleButton;
    }

    private TextField getSystemTextField()
    {
        if(mSystemTextField == null)
        {
            mSystemTextField = new TextField();
            mSystemTextField.setDisable(true);
            mSystemTextField.setMaxWidth(Double.MAX_VALUE);
        }

        return mSystemTextField;
    }

    private TextField getSiteTextField()
    {
        if(mSiteTextField == null)
        {
            mSiteTextField = new TextField();
            mSiteTextField.setDisable(true);
            mSiteTextField.setMaxWidth(Double.MAX_VALUE);
        }

        return mSiteTextField;
    }

    private TextField getNameTextField()
    {
        if(mNameTextField == null)
        {
            mNameTextField = new TextField();
            mNameTextField.setDisable(true);
            mNameTextField.setMaxWidth(Double.MAX_VALUE);
        }

        return mNameTextField;
    }

    private TableView<SiteFrequency> getSiteFrequencyTableView()
    {
        if(mSiteFrequencyTableView == null)
        {
            mSiteFrequencyTableView = new TableView<>();
            mSiteFrequencyTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
            mSiteFrequencyTableView.setPlaceholder(new Label("Please select a site to view frequencies"));

            mTypeColumn = new TableColumn("Type");
            mTypeColumn.setPrefWidth(100);
            mTypeColumn.setCellValueFactory(new TypeCellValueFactory());

            TableColumn<SiteFrequency,Integer> lcnColumn = new TableColumn("LCN");
            lcnColumn.setCellValueFactory(new PropertyValueFactory<>("logicalChannelNumber"));

            TableColumn<SiteFrequency,String> frequencyColumn = new TableColumn("Frequency");
            frequencyColumn.setPrefWidth(100);
            frequencyColumn.setCellValueFactory(new FrequencyCellValueFactory());

            mSiteFrequencyTableView.getColumns().addAll(mTypeColumn, lcnColumn, frequencyColumn);
        }

        return mSiteFrequencyTableView;
    }

    private Button getCreateChannelConfigurationButton()
    {
        if(mCreateChannelConfigurationButton == null)
        {
            mCreateChannelConfigurationButton = new Button("Create Channel Configuration");
            mCreateChannelConfigurationButton.setVisible(false);
            mCreateChannelConfigurationButton.setDisable(true);
            mCreateChannelConfigurationButton.setOnAction(event -> {
                if(mRadioReferenceDecoder == null)
                {
                    throw new IllegalStateException("Can't create channel configuration - radio reference decoder is null");
                }

                String aliasList = getAliasListNameComboBox().getSelectionModel().getSelectedItem();

                if(aliasList == null)
                {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "Please select an Alias List",
                        ButtonType.OK);
                    alert.setTitle("Alias List Required");
                    alert.setHeaderText("Channel configuration requires an alias list");
                    alert.initOwner((getCreateChannelConfigurationButton()).getScene().getWindow());
                    alert.showAndWait();
                    flashAliasListComboBox();
                }
                else
                {
                    if(getControlToggleButton().isSelected())
                    {
                        if(mRadioReferenceDecoder.isHybridMotorolaP25(mCurrentSystem))
                        {
                            createHybridP25VoiceChannels();
                        }
                        else
                        {
                            createControlChannel();
                        }
                    }
                    else if(getControlAndAltToggleButton().isSelected())
                    {
                        createControlAndAlternatesChannel();
                    }
                    else if(getSelectedToggleButton().isSelected())
                    {
                        createChannels(true);
                    }
                    else
                    {
                        createChannels(false);
                    }
                }
            });
        }

        return mCreateChannelConfigurationButton;
    }

    private CheckBox getGoToChannelEditorCheckBox()
    {
        if(mGoToChannelEditorCheckBox == null)
        {
            mGoToChannelEditorCheckBox = new CheckBox("Go To Channel Editor");
            mGoToChannelEditorCheckBox.setDisable(true);
            mGoToChannelEditorCheckBox.setVisible(false);
            mGoToChannelEditorCheckBox.setSelected(mUserPreferences.getRadioReferencePreference()
                .isCreateAndShowChannelEditor());
            mGoToChannelEditorCheckBox.selectedProperty()
                .addListener((observable, oldValue, newValue) -> mUserPreferences.getRadioReferencePreference()
                    .setCreateAndShowChannelEditor(newValue));
        }

        return mGoToChannelEditorCheckBox;
    }

    public class SiteFrequencyListCell extends ListCell<SiteFrequency>
    {
        private final DecimalFormat FREQUENCY_FORMATTER = new DecimalFormat("0.00000");
        private HBox mHBox;
        private Label mFrequency;
        private Label mType;

        public SiteFrequencyListCell()
        {
            mHBox = new HBox();
            mHBox.setMaxWidth(Double.MAX_VALUE);
            mFrequency = new Label();
            mFrequency.setMaxWidth(Double.MAX_VALUE);
            mFrequency.setAlignment(Pos.CENTER_LEFT);
            mType = new Label();
            mType.setMaxWidth(Double.MAX_VALUE);
            mType.setAlignment(Pos.CENTER_RIGHT);
            HBox.setHgrow(mFrequency, Priority.ALWAYS);
            HBox.setHgrow(mType, Priority.ALWAYS);
            mHBox.getChildren().addAll(mFrequency, mType);
        }

        @Override
        protected void updateItem(SiteFrequency item, boolean empty)
        {
            super.updateItem(item, empty);

            setText(null);

            if(empty || item == null)
            {
                setGraphic(null);
            }
            else
            {
                String text = item.getLogicalChannelNumber() + ": " + FREQUENCY_FORMATTER.format(item.getFrequency());
                mFrequency.setText(text);

                String use = item.getUse();

                if(use == null || use.isEmpty())
                {
                    mType.setText(null);
                }
                else if(use.equalsIgnoreCase(ALTERNATE_CONTROL_CHANNEL))
                {
                    mType.setText("Alt Control");
                }
                else if(use.equalsIgnoreCase(PRIMARY_CONTROL_CHANNEL))
                {
                    mType.setText("Control");
                }
                else
                {
                    mType.setText(use);
                }

                setGraphic(mHBox);
            }
        }
    }

    /**
     * Cell value factory to display formatted frequency values
     */
    public class FrequencyCellValueFactory implements Callback<TableColumn.CellDataFeatures<SiteFrequency, String>,
            ObservableValue<String>>
    {
        private final DecimalFormat FREQUENCY_FORMATTER = new DecimalFormat("0.00000");
        private SimpleStringProperty mFrequencyFormatted = new SimpleStringProperty();

        @Override
        public ObservableValue<String> call(TableColumn.CellDataFeatures<SiteFrequency, String> param)
        {
            mFrequencyFormatted.set(FREQUENCY_FORMATTER.format(param.getValue().getFrequency()));
            return mFrequencyFormatted;
        }
    }

    /**
     * Cell value factory to display SiteFrequency usage: control or alternate control
     */
    public class TypeCellValueFactory implements Callback<TableColumn.CellDataFeatures<SiteFrequency,String>,
        ObservableValue<String>>
    {

        @Override
        public ObservableValue<String> call(TableColumn.CellDataFeatures<SiteFrequency,String> param)
        {
            String use = param.getValue().getUse();

            if(use != null)
            {
                switch(use)
                {
                    case PRIMARY_CONTROL_CHANNEL:
                        return new ReadOnlyObjectWrapper<>("Control");
                    case ALTERNATE_CONTROL_CHANNEL:
                        return new ReadOnlyObjectWrapper<>("Alt Control");
                }
            }

            return null;
        }
    }

    public class TypeComparator implements Comparator<SiteFrequency>
    {
        @Override
        public int compare(SiteFrequency o1, SiteFrequency o2)
        {
            if(o1.getUse() != null && o2.getUse() != null)
            {
                return o1.getUse().compareTo(o2.getUse());
            }
            else if(o1.getUse() != null)
            {
                return -1;
            }
            else if(o2.getUse() != null)
            {
                return 1;
            }

            return 0;
        }
    }

    /**
     * Indicates if the use category for the site frequency is 'd' for control channel
     */
    public static boolean isControl(SiteFrequency siteFrequency)
    {
        return siteFrequency.getUse() != null && siteFrequency.getUse().contentEquals("d");
    }

    /**
     * Indicates if the use category for the site frequency is 'a' for alternate control channel
     */
    public static boolean isAlternate(SiteFrequency siteFrequency)
    {
        return siteFrequency.getUse() != null && siteFrequency.getUse().contentEquals("a");
    }

    public static boolean hasControl(List<SiteFrequency> siteFrequencies)
    {
        for(SiteFrequency siteFrequency: siteFrequencies)
        {
            if(isControl(siteFrequency))
            {
                return true;
            }
        }

        return false;
    }

    public static boolean hasAlternate(List<SiteFrequency> siteFrequencies)
    {
        for(SiteFrequency siteFrequency: siteFrequencies)
        {
            if(isAlternate(siteFrequency))
            {
                return true;
            }
        }

        return false;
    }
}
