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
package io.github.dsheirer.source.tuner.ui;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.tuner.TunerFactory;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.IDiscoveredTunerStatusListener;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.manager.TunerStatus;
import javafx.application.Platform;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hosts the tuner editor for the selected discovered tuner, and replaces the editor when the tuner's status changes,
 * since the tuner (and therefore the editor's controls) can change with the status.
 */
public class DiscoveredTunerEditor extends StackPane implements IDiscoveredTunerStatusListener
{
    private static final Logger mLog = LoggerFactory.getLogger(DiscoveredTunerEditor.class);
    private final UserPreferences mUserPreferences;
    private final TunerManager mTunerManager;
    private final EmptyTunerEditor mEmptyEditor = new EmptyTunerEditor();
    private final ScrollPane mScrollPane = new ScrollPane();
    private DiscoveredTuner mDiscoveredTuner;
    private TunerEditor<?,?> mEditor;

    /**
     * Constructs an instance
     * @param userPreferences to use for starting wideband recordings sourced by a tuner
     * @param tunerManager for creating tuner editors
     */
    public DiscoveredTunerEditor(UserPreferences userPreferences, TunerManager tunerManager)
    {
        mUserPreferences = userPreferences;
        mTunerManager = tunerManager;
        getStyleClass().add("discovered-tuner-editor");
        mScrollPane.setFitToWidth(true);
        mScrollPane.getStyleClass().add("tuner-editor-scroll");
        mScrollPane.setContent(mEmptyEditor);
        getChildren().add(mScrollPane);
    }

    /**
     * Discovered tuner shown in the editor, or null.
     */
    public DiscoveredTuner getItem()
    {
        return mDiscoveredTuner;
    }

    /**
     * Indicates if the editor shows a tuner.
     */
    public boolean hasItem()
    {
        return mDiscoveredTuner != null;
    }

    /**
     * Current tuner editor, or null.
     */
    public TunerEditor<?,?> getEditor()
    {
        return mEditor;
    }

    /**
     * Shows the editor for the discovered tuner, or the empty editor when the tuner is null.
     */
    public void setItem(DiscoveredTuner discoveredTuner)
    {
        if(mDiscoveredTuner != null)
        {
            mDiscoveredTuner.removeTunerStatusListener(this);
        }

        if(mEditor != null)
        {
            mEditor.dispose();
            mEditor = null;
        }

        mDiscoveredTuner = discoveredTuner;

        if(discoveredTuner != null)
        {
            try
            {
                mEditor = TunerFactory.getEditor(mUserPreferences, discoveredTuner, mTunerManager);
            }
            catch(Exception e)
            {
                mLog.error("Error creating tuner editor for " + discoveredTuner.getId(), e);
            }

            discoveredTuner.addTunerStatusListener(this);
        }

        mScrollPane.setContent(mEditor != null ? mEditor : mEmptyEditor);
    }

    @Override
    public void tunerStatusUpdated(DiscoveredTuner discoveredTuner, TunerStatus previous, TunerStatus current)
    {
        Platform.runLater(() -> {
            //If this is the currently displayed tuner, set it again to re-create the editor
            if(mDiscoveredTuner != null && mDiscoveredTuner.equals(discoveredTuner))
            {
                setItem(current == TunerStatus.REMOVED ? null : discoveredTuner);
            }
        });
    }
}
