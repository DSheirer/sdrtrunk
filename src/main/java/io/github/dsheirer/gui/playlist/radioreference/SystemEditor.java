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
import io.github.dsheirer.rrapi.RadioReferenceException;
import io.github.dsheirer.rrapi.type.CountyInfo;
import io.github.dsheirer.rrapi.type.Flavor;
import io.github.dsheirer.rrapi.type.Site;
import io.github.dsheirer.rrapi.type.System;
import io.github.dsheirer.rrapi.type.SystemInformation;
import io.github.dsheirer.rrapi.type.Tag;
import io.github.dsheirer.rrapi.type.Talkgroup;
import io.github.dsheirer.rrapi.type.TalkgroupCategory;
import io.github.dsheirer.rrapi.type.Type;
import io.github.dsheirer.rrapi.type.Voice;
import io.github.dsheirer.service.radioreference.RadioReference;
import io.github.dsheirer.util.ThreadPool;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.javafx.IconNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Radio Reference editor for trunked radio systems
 */
public class SystemEditor extends VBox
{
    private static final Logger mLog = LoggerFactory.getLogger(SystemEditor.class);

    private UserPreferences mUserPreferences;
    private RadioReference mRadioReference;
    private PlaylistManager mPlaylistManager;
    private Level mLevel;
    private ComboBox<System> mSystemComboBox;
    private IntegerProperty mSystemCountProperty = new SimpleIntegerProperty();
    private TabPane mTabPane;
    private Tab mSystemTab;
    private Tab mTalkgroupTab;
    private SystemSiteSelectionEditor mSystemSiteSelectionEditor;
    private SystemTalkgroupSelectionEditor mSystemTalkgroupSelectionEditor;
    private RadioReferenceDecoder mRadioReferenceDecoder;

    /**
     * Constructs an instance
     * @param userPreferences for preferences
     * @param radioReference to access radio reference
     * @param playlistManager
     * @param level STATE or COUNTY
     */
    public SystemEditor(UserPreferences userPreferences, RadioReference radioReference,
                        PlaylistManager playlistManager, Level level)
    {
        mUserPreferences = userPreferences;
        mRadioReference = radioReference;
        mPlaylistManager = playlistManager;
        mLevel = level;
        mSystemCountProperty.bind(Bindings.size(getSystemComboBox().getItems()));

        setPadding(new Insets(20,10,10,10));
        setSpacing(10);
        HBox systemBox = new HBox();
        HBox.setHgrow(getSystemComboBox(), Priority.ALWAYS);
        systemBox.setAlignment(Pos.CENTER_LEFT);
        systemBox.setSpacing(5);
        systemBox.setMaxWidth(Double.MAX_VALUE);
        systemBox.getChildren().addAll(new Label("System"), getSystemComboBox());
        VBox.setVgrow(getTabPane(), Priority.ALWAYS);
        getChildren().addAll(systemBox, getTabPane());
    }

    /**
     * Observable count of systems in the editor
     */
    public IntegerProperty systemCountProperty()
    {
        return mSystemCountProperty;
    }

    public void clear()
    {
        getSystemComboBox().getItems().clear();
        getSystemSiteSelectionEditor().clear();
        getSystemTalkgroupSelectionEditor().clear();
    }

    /**
     * Sets the list of displayed systems and clears out any existing systems.  Auto-selects a system if the user
     * has selected that system before.
     *
     * Note: this should only be invoked on the FX application thread
     *
     * @param systems to display
     */
    public void setSystems(List<System> systems)
    {
        clear();

        if(systems != null && !systems.isEmpty())
        {
            Collections.sort(systems, new SystemComparator());
            getSystemComboBox().getItems().addAll(systems);

            int preferredSystemId = mUserPreferences.getRadioReferencePreference().getPreferredSystemId(mLevel);

            for(System system: getSystemComboBox().getItems())
            {
                if(system.getSystemId() == preferredSystemId)
                {
                    getSystemComboBox().getSelectionModel().select(system);
                    return;
                }
            }
        }
    }

    private TabPane getTabPane()
    {
        if(mTabPane == null)
        {
            mTabPane = new TabPane();
            mTabPane.setMaxHeight(Double.MAX_VALUE);
            mTabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
            mTabPane.getTabs().addAll(getSystemTab(), getTalkgroupTab());
        }

        return mTabPane;
    }

    private SystemSiteSelectionEditor getSystemSiteSelectionEditor()
    {
        if(mSystemSiteSelectionEditor == null)
        {
            mSystemSiteSelectionEditor = new SystemSiteSelectionEditor(mUserPreferences, mPlaylistManager);
        }

        return mSystemSiteSelectionEditor;
    }

    private Tab getSystemTab()
    {
        if(mSystemTab == null)
        {
            mSystemTab = new Tab("System View");
            mSystemTab.setContent(getSystemSiteSelectionEditor());
        }

        return mSystemTab;
    }

    private SystemTalkgroupSelectionEditor getSystemTalkgroupSelectionEditor()
    {
        if(mSystemTalkgroupSelectionEditor == null)
        {
            mSystemTalkgroupSelectionEditor = new SystemTalkgroupSelectionEditor(mUserPreferences, mPlaylistManager);
        }

        return mSystemTalkgroupSelectionEditor;
    }

    private Tab getTalkgroupTab()
    {
        if(mTalkgroupTab == null)
        {
            mTalkgroupTab = new Tab("Talkgroup View");
            mTalkgroupTab.setContent(getSystemTalkgroupSelectionEditor());
        }

        return mTalkgroupTab;
    }

    private ComboBox<System> getSystemComboBox()
    {
        if(mSystemComboBox == null)
        {
            mSystemComboBox = new ComboBox<>();
            mSystemComboBox.setMaxWidth(Double.MAX_VALUE);
            mSystemComboBox.setCellFactory(param -> new SystemListCell());
            mSystemComboBox.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> setSystem(newValue));
        }

        return mSystemComboBox;
    }

    private boolean isSupported(System system)
    {
        return mRadioReferenceDecoder != null && mRadioReferenceDecoder.hasSupportedProtocol(system);
    }

    private String getType(System system)
    {
        if(mRadioReferenceDecoder == null)
        {
            try
            {
                initRadioReferenceDecoder();
            }
            catch (Throwable t)
            {
                mLog.error("Error retrieving system information", t);
            }
        }

        if(mRadioReferenceDecoder != null)
        {
            Type type = mRadioReferenceDecoder.getType(system);
            Flavor flavor = mRadioReferenceDecoder.getFlavor(system);

            if(type != null)
            {
                if(flavor != null)
                {
                    return type.getName() + " " + flavor.getName();
                }
                else
                {
                    return type.getName();
                }
            }
        }

        return "Unknown";
    }

    /**
     * Sets the system to be displayed in the editor and updates the system and talkgroup view editors
     */
    private void setSystem(System system)
    {
        if(system != null)
        {
            getSystemSiteSelectionEditor().clearAndSetLoading();
            getSystemTalkgroupSelectionEditor().clearAndSetLoading();

            mUserPreferences.getRadioReferencePreference().setPreferredSystemId(system.getSystemId(), mLevel);

            //Retrieve the radio reference data on a separate thread and then load the editors on the FX thread
            ThreadPool.CACHED.execute(() -> {
                try
                {
                    if(mRadioReferenceDecoder == null)
                    {
                        initRadioReferenceDecoder();
                    }

                    //Query and load the system view editor first
                    SystemInformation systemInformation = mRadioReference.getService().getSystemInformation(system.getSystemId());

                    List<Site> sites = mRadioReference.getService().getSites(system.getSystemId());

                    //The service api doesn't provide the county name, so we run a separate query to update each value
                    List<EnrichedSite> enrichedSites = new ArrayList<>();
                    for(Site site: sites)
                    {
                        CountyInfo countyInfo = null;

                        int countyId = site.getCountyId();

                        //Temporary sites that have been added to a system where the county/location is unknown, can
                        //have a county ID of 99,999 as observed from API (undocumented).
                        if(countyId > 0 && countyId != 99999)
                        {
                            countyInfo = mRadioReference.getService().getCountyInfo(site.getCountyId());
                        }

                        enrichedSites.add(new EnrichedSite(site, countyInfo));
                    }

                    Platform.runLater(() -> getSystemSiteSelectionEditor().setSystem(system, enrichedSites, mRadioReferenceDecoder,
                        systemInformation));

                    //Query and load the talkgroup view second
                    List<Talkgroup> talkgroups = mRadioReference.getService().getTalkgroups(system.getSystemId());
                    List<TalkgroupCategory> categories = mRadioReference.getService().getTalkgroupCategories(system.getSystemId());
                    Platform.runLater(() -> getSystemTalkgroupSelectionEditor()
                        .setSystem(system, talkgroups, categories, mRadioReferenceDecoder));
                }
                catch(Throwable t)
                {
                    mLog.error("Error retrieving system information", t);

                    //We have to call setSystem() on both editors to clear the loading status spinny icon
                    Platform.runLater(() -> {
                        getSystemTalkgroupSelectionEditor().setSystem(null, Collections.emptyList(),
                            Collections.emptyList(), mRadioReferenceDecoder);
                        getSystemSiteSelectionEditor().setSystem(null, Collections.emptyList(),
                            mRadioReferenceDecoder, null);
                        new RadioReferenceUnavailableAlert(getSystemComboBox()).showAndWait();
                    });
                }
            });
        }
        else
        {
            getSystemSiteSelectionEditor().clear();
            getSystemTalkgroupSelectionEditor().clear();
        }
    }

    /**
     * Initializes the Radio Reference Decoder
     * @throws RadioReferenceException
     */
    private void initRadioReferenceDecoder() throws RadioReferenceException
    {
        Map<Integer, Type> typeMap = mRadioReference.getService().getTypesMap();
        Map<Integer, Flavor> flavorMap = mRadioReference.getService().getFlavorsMap();
        Map<Integer, Voice> voiceMap = mRadioReference.getService().getVoicesMap();
        Map<Integer, Tag> tagMap = mRadioReference.getService().getTagsMap();
        mRadioReferenceDecoder = new RadioReferenceDecoder(mUserPreferences, typeMap, flavorMap,
                voiceMap, tagMap);
    }

    public class SystemListCell extends ListCell<System>
    {
        private HBox mHBox;
        private Label mName;
        private Label mProtocol;
        private IconNode mIconNode;

        public SystemListCell()
        {
            mHBox = new HBox();
            mHBox.setSpacing(15);
            mHBox.setPadding(new Insets(0,15,0,0));
            mHBox.setMaxWidth(Double.MAX_VALUE);
            mName = new Label();
            mName.setMaxWidth(Double.MAX_VALUE);
            mName.setAlignment(Pos.CENTER_LEFT);
            mProtocol = new Label();
            mProtocol.setMaxWidth(Double.MAX_VALUE);
            mProtocol.setAlignment(Pos.CENTER_RIGHT);
            mProtocol.setContentDisplay(ContentDisplay.RIGHT);
            HBox.setHgrow(mName, Priority.ALWAYS);
            HBox.setHgrow(mProtocol, Priority.ALWAYS);
            mHBox.getChildren().addAll(mName, mProtocol);
        }

        @Override
        protected void updateItem(System item, boolean empty)
        {
            super.updateItem(item, empty);

            setText(null);

            if(empty || item == null)
            {
                setGraphic(null);
                mName.setText(null);
                mProtocol.setText(null);
            }
            else
            {
                mName.setText(item.getName());
                mProtocol.setText(getType(item));

                if(isSupported(item))
                {
                    mIconNode = new IconNode(FontAwesome.CHECK);
                    mIconNode.setFill(Color.GREEN);
                }
                else
                {
                    mIconNode = new IconNode(FontAwesome.BAN);
                    mIconNode.setFill(Color.RED);
                }

                mProtocol.setGraphic(mIconNode);
                setGraphic(mHBox);
            }
        }
    }

    public class SystemComparator implements Comparator<System>
    {
        @Override
        public int compare(System o1, System o2)
        {
            if(o1.getName() == null && o2.getName() == null)
            {
                return 0;
            }
            else if(o1.getName() == null)
            {
                return 1;
            }
            else if(o2.getName() == null)
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
