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

import io.github.dsheirer.playlist.PlaylistManager;
import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.rrapi.type.Agency;
import io.github.dsheirer.rrapi.type.AgencyInfo;
import io.github.dsheirer.rrapi.type.CountyInfo;
import io.github.dsheirer.service.radioreference.RadioReference;
import io.github.dsheirer.util.ThreadPool;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Editor for displaying a list of agencies and viewing frequency lists for each agency
 */
public class AgencyEditor extends VBox
{
    private static final Logger mLog = LoggerFactory.getLogger(AgencyEditor.class);
    private UserPreferences mUserPreferences;
    private RadioReference mRadioReference;
    private PlaylistManager mPlaylistManager;
    private Level mLevel;
    private ListView<Agency> mAgencyListView;
    private AgencyFrequencyEditor mAgencyFrequencyEditor;
    private IntegerProperty mAgencyCountProperty = new SimpleIntegerProperty();

    /**
     * Constructs an instance
     * @param userPreferences
     * @param radioReference
     * @param level
     */
    public AgencyEditor(UserPreferences userPreferences, RadioReference radioReference,
                        PlaylistManager playlistManager, Level level)
    {
        mUserPreferences = userPreferences;
        mRadioReference = radioReference;
        mPlaylistManager = playlistManager;
        mLevel = level;
        mAgencyCountProperty.bind(Bindings.size(getAgencyListView().getItems()));

        setPadding(new Insets(10,10,10,10));
        setSpacing(10);
        VBox.setVgrow(getAgencyFrequencyEditor(), Priority.ALWAYS);
        getChildren().addAll(getAgencyListView(), getAgencyFrequencyEditor());
    }

    /**
     * Sets the list of displayed agencies and clears out any existing agencies.  Auto-selects an agency if the user
     * has selected that agency before.
     *
     * Note: this should only be invoked on the FX application thread
     *
     * @param agencies to display
     */
    public void setAgencies(List<Agency> agencies)
    {
        clear();

        if(agencies != null && !agencies.isEmpty())
        {
            Collections.sort(agencies, new AgencyComparator());
            getAgencyListView().getItems().addAll(agencies);

            int preferredAgencyId = mUserPreferences.getRadioReferencePreference().getPreferredAgencyId(mLevel);

            for(Agency agency: getAgencyListView().getItems())
            {
                if(agency.getAgencyId() == preferredAgencyId)
                {
                    getAgencyListView().getSelectionModel().select(agency);
                    getAgencyListView().scrollTo(agency);
                    return;
                }
            }
        }
    }

    public void clear()
    {
        getAgencyListView().getItems().clear();
        getAgencyFrequencyEditor().setCategories(null);
    }

    /**
     * Observable count of agencies in the editor
     */
    public IntegerProperty agencyCountProperty()
    {
        return mAgencyCountProperty;
    }

    private AgencyFrequencyEditor getAgencyFrequencyEditor()
    {
        if(mAgencyFrequencyEditor == null)
        {
            mAgencyFrequencyEditor = new AgencyFrequencyEditor(mUserPreferences, mRadioReference, mPlaylistManager, mLevel);
        }

        return mAgencyFrequencyEditor;
    }

    private ListView<Agency> getAgencyListView()
    {
        if(mAgencyListView == null)
        {
            mAgencyListView = new ListView<>();
            mAgencyListView.setPrefHeight(200);
            mAgencyListView.setCellFactory(param -> new AgencyListCell());
            mAgencyListView.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> setAgency(newValue));
            mAgencyListView.setPlaceholder(new Label("No agencies available"));
        }

        return mAgencyListView;
    }

    private void setAgency(Agency agency)
    {
        if(agency != null)
        {
            getAgencyFrequencyEditor().clearAndSetLoading();

            mUserPreferences.getRadioReferencePreference().setPreferredAgencyId(agency.getAgencyId(), mLevel);

            ThreadPool.CACHED.submit(() -> {
                try
                {
                    if (mLevel == Level.COUNTY && agency instanceof CountyAgency) {
                        final CountyInfo countyInfo = mRadioReference.getService().getCountyInfo(-agency.getAgencyId());
                        Platform.runLater(() -> getAgencyFrequencyEditor().setCategories(countyInfo.getCategories()));
                    } else {
                        final AgencyInfo agencyInfo = mRadioReference.getService().getAgencyInfo(agency);
                        Platform.runLater(() -> getAgencyFrequencyEditor().setCategories(agencyInfo.getCategories()));
                    }
                }
                catch(Throwable t)
                {
                    mLog.error("Error retrieving agency info", t);
                    Platform.runLater(() -> {
                        getAgencyFrequencyEditor().setLoading(false);
                        new RadioReferenceUnavailableAlert(getAgencyListView()).showAndWait();
                    });
                }
            });
        }
        else
        {
            getAgencyFrequencyEditor().clear();
        }
    }

    public class AgencyListCell extends ListCell<Agency>
    {
        @Override
        protected void updateItem(Agency item, boolean empty)
        {
            super.updateItem(item, empty);
            setText((empty || item == null) ? null : item.getName());
        }
    }

    public class AgencyComparator implements Comparator<Agency>
    {
        @Override
        public int compare(Agency o1, Agency o2)
        {
            if(o1.getName() == null && o2.getName() == null)
            {
                return 0;
            }
            else if(o1.getName() == null || o2 instanceof CountyAgency)
            {
                return 1;
            }
            else if(o2.getName() == null || o1 instanceof CountyAgency)
            {
                return -1;
            }
            else
            {
                return o1.getName().compareTo(o2.getName());
            }
        }
    }
}
