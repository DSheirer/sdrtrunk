/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.gui.playlist.alias;

import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.AliasModel;
import io.github.dsheirer.alias.id.AliasID;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import javafx.beans.InvalidationListener;
import javafx.geometry.Insets;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

/**
 * Primary alias editor with tabbed panes for view-by alias editing support
 */
public class AliasEditor extends TabPane
{
    @Resource
    private AliasConfigurationEditor mAliasConfigurationEditor;
    @Resource
    private AliasModel mAliasModel;
    @Resource
    private AliasViewByIdentifierEditor mAliasViewByIdentifierEditor;
    @Resource
    private AliasViewByRecordingEditor mAliasViewByRecordingEditor;
    private Tab mAliasConfigurationTab;
    private Tab mAliasIdentifierTab;
    private Tab mAliasRecordingTab;
    private boolean mAliasListInvalidated = false;


    /**
     * Constructs an instance
     * @param playlistManager for alias model access
     */
    public AliasEditor()
    {
    }

    @PostConstruct
    public void postConstruct()
    {
        setPadding(new Insets(4,0,0,0));
        setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        Tab viewByTab = new Tab("View By:");
        viewByTab.setDisable(true);
        getTabs().addAll(viewByTab, getAliasConfigurationTab(), getAliasIdentifierTab(), getAliasRecordingTab());

        //Note if aliases change since we're using a static snapshot
        mAliasModel.aliasList().addListener((InvalidationListener)observable -> mAliasListInvalidated = true);

        //Only update the alias list when this tab is selected to avoid many slow updates in the background
        getAliasIdentifierTab().selectedProperty().addListener(observable ->
        {
            if (mAliasListInvalidated)
            {
                getAliasViewByIdentifierEditor().updateList();
                mAliasListInvalidated = false;
            }
        });
    }

    /**
     * Processes the alias view request.
     *
     * Note: this method must be invoked on the JavaFX platform thread
     *
     * @param aliasTabRequest to process
     */
    public void process(AliasTabRequest aliasTabRequest)
    {
        if(aliasTabRequest instanceof ViewAliasRequest)
        {
            Alias alias = ((ViewAliasRequest)aliasTabRequest).getAlias();

            if(alias != null)
            {
                getSelectionModel().select(getAliasConfigurationTab());
                getAliasConfigurationEditor().show(alias);
            }
        }
        else if(aliasTabRequest instanceof ViewAliasIdentifierRequest)
        {
            AliasID aliasID = ((ViewAliasIdentifierRequest)aliasTabRequest).getAliasId();

            if(aliasID != null)
            {
                getSelectionModel().select(getAliasIdentifierTab());
                getAliasViewByIdentifierEditor().show(aliasID);
            }
        }
    }

    private Tab getAliasConfigurationTab()
    {
        if(mAliasConfigurationTab == null)
        {
            mAliasConfigurationTab = new Tab("Alias");
            mAliasConfigurationTab.setContent(getAliasConfigurationEditor());
        }

        return mAliasConfigurationTab;
    }

    private AliasConfigurationEditor getAliasConfigurationEditor()
    {
        return mAliasConfigurationEditor;
    }

    private Tab getAliasIdentifierTab()
    {
        if(mAliasIdentifierTab == null)
        {
            mAliasIdentifierTab = new Tab("Identifier");
            mAliasIdentifierTab.setContent(getAliasViewByIdentifierEditor());
        }

        return mAliasIdentifierTab;
    }

    private AliasViewByIdentifierEditor getAliasViewByIdentifierEditor()
    {
        return mAliasViewByIdentifierEditor;
    }

    private AliasViewByRecordingEditor getAliasViewByRecordingEditor()
    {
        return mAliasViewByRecordingEditor;
    }

    private Tab getAliasRecordingTab()
    {
        if(mAliasRecordingTab == null)
        {
            mAliasRecordingTab = new Tab("Record");
            mAliasRecordingTab.setContent(getAliasViewByRecordingEditor());
        }

        return mAliasRecordingTab;
    }
}
