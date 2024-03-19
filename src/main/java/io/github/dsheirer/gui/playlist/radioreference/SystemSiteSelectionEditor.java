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

import com.google.common.collect.Ordering;
import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.rrapi.type.Flavor;
import io.github.dsheirer.rrapi.type.System;
import io.github.dsheirer.rrapi.type.SystemInformation;
import io.github.dsheirer.rrapi.type.Type;
import io.github.dsheirer.rrapi.type.Voice;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;


/**
 * View for a radio reference trunked mobile radio system
 */
public class SystemSiteSelectionEditor extends GridPane
{
    private static final Logger mLog = LoggerFactory.getLogger(SystemSiteSelectionEditor.class);

    private UserPreferences mUserPreferences;
    private PlaylistManager mPlaylistManager;
    private ProgressIndicator mProgressIndicator;
    private Label mPlaceholderLabel;
    private Label mProtocolLabel;
    private Label mFlavorLabel;
    private Label mVoiceLabel;
    private TableView<EnrichedSite> mSiteTableView;
    private SiteEditor mSiteEditor;
    private RadioReferenceDecoder mRadioReferenceDecoder;
    private System mCurrentSystem;
    private SystemInformation mCurrentSystemInformation;

    /**
     * Constructs an instance
     * @param userPreferences for settings
     * @param playlistManager to access radio reference
     */
    public SystemSiteSelectionEditor(UserPreferences userPreferences, PlaylistManager playlistManager)
    {
        mUserPreferences = userPreferences;
        mPlaylistManager = playlistManager;

        setPadding(new Insets(10,0,0,0));
        setHgap(10);
        setVgap(10);

        int row = 0;

        Label typeLabel = new Label("Protocol:");
        GridPane.setConstraints(typeLabel, 0, row);
        GridPane.setHalignment(typeLabel, HPos.RIGHT);
        getChildren().add(typeLabel);

        GridPane.setConstraints(getProtocolLabel(), 1, row);
        GridPane.setHgrow(getProtocolLabel(), Priority.ALWAYS);
        getChildren().add(getProtocolLabel());

        Label voiceLabel = new Label("Voice:");
        GridPane.setConstraints(voiceLabel, 2, row);
        GridPane.setHalignment(voiceLabel, HPos.RIGHT);
        getChildren().add(voiceLabel);

        GridPane.setConstraints(getVoiceLabel(), 3, row);
        GridPane.setHgrow(getVoiceLabel(), Priority.ALWAYS);
        getChildren().add(getVoiceLabel());

        GridPane.setConstraints(getSiteTableView(), 0, ++row, 4, 1);
        getChildren().add(getSiteTableView());

        GridPane.setConstraints(getSiteEditor(), 0, ++row, 4, 1);
        GridPane.setHgrow(getSiteEditor(), Priority.ALWAYS);
        GridPane.setVgrow(getSiteEditor(), Priority.ALWAYS);
        getChildren().add(getSiteEditor());
    }

    /**
     * Updates this view with the specified system
     * @param system to view
     */
    public void setSystem(System system, List<EnrichedSite> sites, RadioReferenceDecoder decoder,
                          SystemInformation systemInformation)
    {
        mCurrentSystem = system;
        mCurrentSystemInformation = systemInformation;
        mRadioReferenceDecoder = decoder;

        Flavor flavor = decoder.getFlavor(system);
        Type type = decoder.getType(system);
        String protocol = (type != null ? type.getName() : "Unknown") + (flavor != null ? " " + flavor.getName() : "");
        getProtocolLabel().setText(protocol);
        getFlavorLabel().setText(flavor != null ? flavor.getName() : null);
        Voice voice = decoder.getVoice(system);
        getVoiceLabel().setText(voice != null ? voice.getName() : null);
        Collections.sort(sites, Ordering.natural());
        getSiteTableView().getItems().addAll(sites);
        setLoading(false);
    }

    public void clear()
    {
        getProtocolLabel().setText(null);
        getFlavorLabel().setText(null);
        getVoiceLabel().setText(null);
        getSiteTableView().getItems().clear();
    }

    public void clearAndSetLoading()
    {
        clear();
        setLoading(true);
    }

    private SiteEditor getSiteEditor()
    {
        if(mSiteEditor == null)
        {
            mSiteEditor = new SiteEditor(mUserPreferences, mPlaylistManager);
        }

        return mSiteEditor;
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
            mPlaceholderLabel = new Label("Select a system, or there are no sites available for currently selected system");
        }

        return mPlaceholderLabel;
    }

    private TableView<EnrichedSite> getSiteTableView()
    {
        if(mSiteTableView == null)
        {
            mSiteTableView = new TableView<>();
            mSiteTableView.setPlaceholder(getPlaceholderLabel());

            TableColumn systemColumn = new TableColumn();
            systemColumn.setText("System");
            systemColumn.setCellValueFactory(new PropertyValueFactory<>("systemFormatted"));
            systemColumn.setPrefWidth(60);

            TableColumn rfssColumn = new TableColumn();
            rfssColumn.setText("RFSS");
            rfssColumn.setCellValueFactory(new PropertyValueFactory<>("rfssFormatted"));
            rfssColumn.setPrefWidth(60);

            TableColumn siteColumn = new TableColumn();
            siteColumn.setText("Site");
            siteColumn.setCellValueFactory(new PropertyValueFactory<>("siteFormatted"));
            siteColumn.setPrefWidth(75);

            TableColumn countyNameColumn = new TableColumn();
            countyNameColumn.setText("County");
            countyNameColumn.setCellValueFactory(new PropertyValueFactory<>("countyName"));
            countyNameColumn.setPrefWidth(125);

            TableColumn descriptionColumn = new TableColumn();
            descriptionColumn.setText("Name");
            descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
            descriptionColumn.setPrefWidth(400);

            mSiteTableView.getColumns().addAll(systemColumn, rfssColumn, siteColumn, countyNameColumn,
                    descriptionColumn);
            mSiteTableView.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
            mSiteTableView.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, selected) ->
                {
                    getSiteEditor().setSite(selected, mCurrentSystem, mCurrentSystemInformation,
                        mRadioReferenceDecoder);
                });
        }

        return mSiteTableView;
    }

    private void setLoading(boolean loading)
    {
        getSiteTableView().setPlaceholder(loading ? getProgressIndicator() : getPlaceholderLabel());
    }
}
