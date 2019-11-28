/*
 *
 *  * ******************************************************************************
 *  * Copyright (C) 2014-2019 Dennis Sheirer
 *  *
 *  * This program is free software: you can redistribute it and/or modify
 *  * it under the terms of the GNU General Public License as published by
 *  * the Free Software Foundation, either version 3 of the License, or
 *  * (at your option) any later version.
 *  *
 *  * This program is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  * GNU General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU General Public License
 *  * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *  * *****************************************************************************
 *
 *
 */

package io.github.dsheirer.gui.playlist.radioreference;

import io.github.dsheirer.rrapi.RadioReferenceException;
import io.github.dsheirer.rrapi.type.CountyInfo;
import io.github.dsheirer.rrapi.type.Flavor;
import io.github.dsheirer.rrapi.type.Site;
import io.github.dsheirer.rrapi.type.SiteFrequency;
import io.github.dsheirer.rrapi.type.System;
import io.github.dsheirer.rrapi.type.SystemInformation;
import io.github.dsheirer.rrapi.type.Type;
import io.github.dsheirer.rrapi.type.Voice;
import io.github.dsheirer.service.radioreference.RadioReference;
import io.github.dsheirer.util.ThreadPool;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.Callback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.EnumSet;
import java.util.List;


/**
 * View for a radio reference trunked mobile radio system
 */
public class TrunkedSystemView extends GridPane
{
    private static final Logger mLog = LoggerFactory.getLogger(TrunkedSystemView.class);
    private static final String ALTERNATE_CONTROL_CHANNEL = "a";
    private static final String PRIMARY_CONTROL_CHANNEL = "d";

    private RadioReference mRadioReference;
    private ProgressIndicator mProgressIndicator;
    private Label mPlaceholderLabel;
    private Label mNameLabel;
    private Label mProtocolLabel;
    private Label mFlavorLabel;
    private Label mVoiceLabel;
    private TableView<Site> mSiteTableView;
    private ListView<SiteFrequency> mSiteFrequencyListView;
    private Button mAddSiteButton;
    private ComboBox<SiteChannel> mSiteChannelsComboBox;

    /**
     * Constructs an instance
     * @param radioReference service
     */
    public TrunkedSystemView(RadioReference radioReference)
    {
        mRadioReference = radioReference;

        setPadding(new Insets(5, 5, 5,5));
        setHgap(5.0);
        setVgap(5.0);

        Label systemLabel = new Label("Trunked System:");
        GridPane.setConstraints(systemLabel, 0, 0);
        GridPane.setHalignment(systemLabel, HPos.RIGHT);
        getChildren().add(systemLabel);

        GridPane.setConstraints(getNameLabel(), 1, 0, 3, 1);
        GridPane.setHgrow(getNameLabel(), Priority.ALWAYS);
        getChildren().add(getNameLabel());

        Label typeLabel = new Label("Protocol:");
        GridPane.setConstraints(typeLabel, 0, 1);
        GridPane.setHalignment(typeLabel, HPos.RIGHT);
        getChildren().add(typeLabel);

        GridPane.setConstraints(getProtocolLabel(), 1, 1);
        GridPane.setHgrow(getProtocolLabel(), Priority.ALWAYS);
        getChildren().add(getProtocolLabel());

        Label voiceLabel = new Label("Voice:");
        GridPane.setConstraints(voiceLabel, 2, 1);
        GridPane.setHalignment(voiceLabel, HPos.RIGHT);
        getChildren().add(voiceLabel);

        GridPane.setConstraints(getVoiceLabel(), 3, 1);
        GridPane.setHgrow(getVoiceLabel(), Priority.ALWAYS);
        getChildren().add(getVoiceLabel());

        GridPane.setConstraints(getSiteTableView(), 0, 2, 4, 4);
        GridPane.setHgrow(getSiteTableView(), Priority.ALWAYS);
        getChildren().add(getSiteTableView());

        Label siteFrequencies = new Label("Site Frequencies");
        GridPane.setConstraints(siteFrequencies, 0, 6, 2, 1);
        GridPane.setHgrow(siteFrequencies, Priority.ALWAYS);
        getChildren().add(siteFrequencies);

        GridPane.setConstraints(getSiteFrequencyListView(), 0, 7, 2, 3);
        GridPane.setHgrow(getSiteFrequencyListView(), Priority.ALWAYS);
        getChildren().add(getSiteFrequencyListView());

        Label playlist = new Label("Create Channel Configuration");
        GridPane.setConstraints(playlist, 2, 6, 2, 1);
        GridPane.setHalignment(playlist, HPos.LEFT);
        GridPane.setHgrow(playlist, Priority.ALWAYS);
        getChildren().add(playlist);

        Label include = new Label("Include Frequencies:");
        GridPane.setConstraints(include, 2, 7, 1, 1);
        GridPane.setHalignment(include, HPos.RIGHT);
        getChildren().add(include);

        GridPane.setConstraints(getSiteChannelsComboBox(), 3, 7, 1, 1);
        GridPane.setHgrow(getSiteChannelsComboBox(), Priority.ALWAYS);
        getChildren().addAll(getSiteChannelsComboBox());

        GridPane.setConstraints(getAddSiteButton(), 3, 8, 1, 1);
        GridPane.setHgrow(getAddSiteButton(), Priority.ALWAYS);
        getChildren().addAll(getAddSiteButton());
    }

    /**
     * Updates this view with the specified system
     * @param system to view
     */
    public void setSystem(final System system)
    {
        clear();

        if(system != null)
        {
            setLoading(true);

            ThreadPool.SCHEDULED.submit(new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        SystemInformation systemInformation = mRadioReference.getService().getSystemInformation(system.getSystemId());
                        Type type = mRadioReference.getService().getType(systemInformation.getTypeId());
                        Flavor flavor = mRadioReference.getService().getFlavor(systemInformation.getFlavorId());
                        Voice voice = mRadioReference.getService().getVoice(systemInformation.getVoiceId());
                        List<Site> sites = mRadioReference.getService().getSites(system.getSystemId());

                        //The service api doesn't provide the county name, so we run a separate query and update the value
                        for(Site site: sites)
                        {
                            CountyInfo countyInfo = mRadioReference.getService().getCountyInfo(site.getCountyId());
                            site.setCountyName(countyInfo.getName());
                        }

                        Platform.runLater(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                getNameLabel().setText(systemInformation.getName());

                                String protocol = (type != null ? type.getName() : "Unknown") + (flavor != null ? " " + flavor.getName() : "");
                                getProtocolLabel().setText(protocol);
                                getFlavorLabel().setText(flavor != null ? flavor.getName() : null);
                                getVoiceLabel().setText(voice != null ? voice.getName() : null);
                                getSiteTableView().getItems().addAll(sites);
                                setLoading(false);
                            }
                        });
                    }
                    catch(RadioReferenceException rre)
                    {
                        setLoading(false);
                        mLog.error("Error", rre);
                    }
                }
            });
        }
    }

    private void clear()
    {
        Runnable runnable = new Runnable()
        {
            @Override
            public void run()
            {
                getNameLabel().setText(null);
                getProtocolLabel().setText(null);
                getFlavorLabel().setText(null);
                getVoiceLabel().setText(null);
                getSiteTableView().getItems().clear();
            }
        };

        if(Platform.isFxApplicationThread())
        {
            runnable.run();
        }
        else
        {
            Platform.runLater(runnable);
        }
    }

    private Label getNameLabel()
    {
        if(mNameLabel == null)
        {
            mNameLabel = new Label();
        }

        return mNameLabel;
    }

    private Label getProtocolLabel()
    {
        if(mProtocolLabel == null)
        {
            mProtocolLabel = new Label();
        }

        return mProtocolLabel;
    }

    private Label getFlavorLabel()
    {
        if(mFlavorLabel == null)
        {
            mFlavorLabel = new Label();
        }

        return mFlavorLabel;
    }

    private Label getVoiceLabel()
    {
        if(mVoiceLabel == null)
        {
            mVoiceLabel = new Label();
        }

        return mVoiceLabel;
    }



    private ProgressIndicator getProgressIndicator()
    {
        if(mProgressIndicator == null)
        {
            mProgressIndicator = new ProgressIndicator();
            mProgressIndicator.setProgress(-1);
        }

        return mProgressIndicator;
    }

    private Label getPlaceholderLabel()
    {
        if(mPlaceholderLabel == null)
        {
            mPlaceholderLabel = new Label("No Frequencies Available");
        }

        return mPlaceholderLabel;
    }

    private TableView<Site> getSiteTableView()
    {
        if(mSiteTableView == null)
        {
            mSiteTableView = new TableView<>();
            mSiteTableView.setPlaceholder(getPlaceholderLabel());

            TableColumn rfssColumn = new TableColumn();
            rfssColumn.setText("RFSS");
            rfssColumn.setCellValueFactory(new PropertyValueFactory<>("rfss"));
            rfssColumn.setPrefWidth(60);

            TableColumn numberColumn = new TableColumn();
            numberColumn.setText("Site");
            numberColumn.setCellValueFactory(new PropertyValueFactory<>("siteNumber"));
            numberColumn.setPrefWidth(60);

            TableColumn countyColumn = new TableColumn();
            countyColumn.setText("County");
            countyColumn.setCellValueFactory(new PropertyValueFactory<>("countyName"));
            countyColumn.setPrefWidth(125);

            TableColumn descriptionColumn = new TableColumn();
            descriptionColumn.setText("Name");
            descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
            descriptionColumn.setPrefWidth(525);

            mSiteTableView.getColumns().addAll(numberColumn, rfssColumn, countyColumn, descriptionColumn);
            mSiteTableView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
            mSiteTableView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Site>()
            {
                @Override
                public void changed(ObservableValue<? extends Site> observable, Site oldValue, Site selected)
                {
                    getSiteFrequencyListView().getItems().clear();

                    if(selected != null)
                    {
                        getSiteFrequencyListView().getItems().addAll(selected.getSiteFrequencies());

                        EnumSet<SiteChannel> siteChannels = SiteChannel.fromSiteFrequencies(selected.getSiteFrequencies());

                        getSiteChannelsComboBox().getItems().clear();
                        getSiteChannelsComboBox().getItems().addAll(siteChannels);

                        if(siteChannels.contains(SiteChannel.CONTROL))
                        {
                            getSiteChannelsComboBox().getSelectionModel().select(SiteChannel.CONTROL);
                        }
                        else
                        {
                            getSiteChannelsComboBox().getSelectionModel().select(SiteChannel.ALL);
                        }
                    }
                }
            });
        }

        return mSiteTableView;
    }

    private ListView<SiteFrequency> getSiteFrequencyListView()
    {
        if(mSiteFrequencyListView == null)
        {
            mSiteFrequencyListView = new ListView<>();
            mSiteFrequencyListView.setCellFactory(new Callback<ListView<SiteFrequency>,ListCell<SiteFrequency>>()
            {
                @Override
                public ListCell<SiteFrequency> call(ListView<SiteFrequency> param)
                {
                    return new SiteFrequencyListCell();
                }
            });
        }

        return mSiteFrequencyListView;
    }

    private void setLoading(boolean loading)
    {
        if(Platform.isFxApplicationThread())
        {
            getSiteTableView().setPlaceholder(loading ? getProgressIndicator() : getPlaceholderLabel());
        }
        else
        {
            Platform.runLater(new Runnable()
            {
                @Override
                public void run()
                {
                    getSiteTableView().setPlaceholder(loading ? getProgressIndicator() : getPlaceholderLabel());
                }
            });
        }
    }

    private ComboBox<SiteChannel> getSiteChannelsComboBox()
    {
        if(mSiteChannelsComboBox == null)
        {
            mSiteChannelsComboBox = new ComboBox<>();
            mSiteChannelsComboBox.getItems().addAll(SiteChannel.values());
        }

        return mSiteChannelsComboBox;
    }

    private Button getAddSiteButton()
    {
        if(mAddSiteButton == null)
        {
            mAddSiteButton = new Button("Add To Playlist");
        }

        return mAddSiteButton;
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
}
