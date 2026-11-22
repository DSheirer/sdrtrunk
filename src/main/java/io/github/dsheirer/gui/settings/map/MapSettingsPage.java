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

package io.github.dsheirer.gui.settings.map;

import com.google.common.eventbus.Subscribe;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.gui.map.MapView;
import io.github.dsheirer.gui.map.TileChoice;
import io.github.dsheirer.gui.map.TileServer;
import io.github.dsheirer.gui.settings.SettingsPage;
import io.github.dsheirer.gui.settings.SettingsPageType;
import io.github.dsheirer.gui.settings.SettingsRow;
import io.github.dsheirer.gui.settings.SettingsSection;
import io.github.dsheirer.service.radioreference.RadioReferenceFrequencyIndex;
import io.github.dsheirer.settings.Settings;
import io.github.dsheirer.settings.SettingsType;
import io.github.dsheirer.settings.map.MapSettings;
import io.github.dsheirer.settings.radioreference.RadioReferenceSettings;
import javafx.application.Platform;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.controlsfx.control.ToggleSwitch;

/**
 * Map settings: the tile server and its API key, and the layers shown on the Map tab.  The Map tab's own Settings
 * pane edits the same settings.
 */
public class MapSettingsPage extends SettingsPage
{
    private final MapSettings mMapSettings;
    private final ComboBox<TileChoice> mTileChoiceComboBox = new ComboBox<>();
    private final TextField mTileServerKeyField = new TextField();
    private final Hyperlink mTileServerKeyLink = new Hyperlink("Get a free key");
    private final ToggleSwitch mShowSitesSwitch = new ToggleSwitch();
    private final Label mLocationLabel = new Label();
    private final SettingsRow mKeyRow;

    public MapSettingsPage(Settings settings)
    {
        super(SettingsPageType.MAP, settings);
        mMapSettings = settings.getMapSettings();

        mTileChoiceComboBox.getItems().setAll(TileChoice.CHOICES);
        mTileChoiceComboBox.setVisibleRowCount(TileChoice.CHOICES.size());
        mTileChoiceComboBox.setValue(getStoredTileChoice());
        mTileChoiceComboBox.setOnAction(event -> {
            TileChoice choice = mTileChoiceComboBox.getValue();

            if(choice != null && !choice.equals(getStoredTileChoice()))
            {
                mMapSettings.setTileServer(choice.server().id());
                mMapSettings.setTileStyle(choice.style().name());
            }

            updateKeyField();
        });

        mTileServerKeyField.setPromptText("API key");
        mTileServerKeyField.setPrefColumnCount(22);
        mTileServerKeyField.setOnAction(event -> saveTileServerKey());
        mTileServerKeyField.focusedProperty().addListener((observable, oldValue, focused) -> {
            if(!focused)
            {
                saveTileServerKey();
            }
        });
        mTileServerKeyLink.setOnAction(event -> {
            TileServer server = getTileServer();

            if(server.keyUrl() != null)
            {
                MapView.browse(server.keyUrl());
            }
        });

        SettingsSection tiles = addSection("Tiles");
        tiles.addRow("Tile server", mTileChoiceComboBox)
            .defaultValue(mTileChoiceComboBox.valueProperty(), TileChoice.get(null, null), mTileChoiceComboBox::setValue)
            .help("Auto follows the theme, Light and Dark always use that style.  Servers without a dark style are " +
                "recolored for dark tiles.");
        mKeyRow = tiles.addRow("API key", mTileServerKeyField, mTileServerKeyLink)
            .help("This tile server needs a free API key from the link.");
        updateKeyField();

        mShowSitesSwitch.setSelected(mMapSettings.isShowRadioReferenceSites());
        mShowSitesSwitch.selectedProperty().addListener((observable, oldValue, show) -> {
            if(show != mMapSettings.isShowRadioReferenceSites())
            {
                mMapSettings.setShowRadioReferenceSites(show);
            }
        });

        SettingsSection layers = addSection("Layers");
        layers.addRow("Show Radio Reference sites", mShowSitesSwitch)
            .defaultValue(mShowSitesSwitch.selectedProperty(), MapSettings.DEFAULT_SHOW_RADIO_REFERENCE_SITES,
                mShowSitesSwitch::setSelected)
            .help("Shows the trunked radio system sites within " + (int)RadioReferenceFrequencyIndex.RADIUS_KILOMETERS +
                " km of your location.  Requires a premium Radio Reference login and your location.");

        SettingsSection location = addSection("Location");
        mLocationLabel.setWrapText(true);
        updateLocation();
        location.addRow("My location", mLocationLabel)
            .help("Set your location with Set My Location on the Map tab, or in the playlist editor's Radio " +
                "Reference tab.");

        MyEventBus.getGlobalEventBus().register(this);
    }

    public void dispose()
    {
        MyEventBus.getGlobalEventBus().unregister(this);
    }

    private TileChoice getStoredTileChoice()
    {
        return TileChoice.get(mMapSettings.getTileServer(), mMapSettings.getTileStyle());
    }

    private TileServer getTileServer()
    {
        TileChoice choice = mTileChoiceComboBox.getValue();
        return choice != null ? choice.server() : TileServer.OPEN_STREET_MAP;
    }

    /**
     * Stores the key the user entered for the selected server
     */
    private void saveTileServerKey()
    {
        TileServer server = getTileServer();

        if(server.requiresKey())
        {
            String key = mTileServerKeyField.getText();
            String stored = mMapSettings.getTileServerKey(server.id());

            if(!key.equals(stored == null ? "" : stored))
            {
                mMapSettings.setTileServerKey(server.id(), key);
            }
        }
    }

    /**
     * Shows the key row only for a server that needs a key, with that server's stored key
     */
    private void updateKeyField()
    {
        TileServer server = getTileServer();
        mKeyRow.setVisible(server.requiresKey());
        mKeyRow.setManaged(server.requiresKey());

        if(server.requiresKey() && !mTileServerKeyField.isFocused())
        {
            String key = mMapSettings.getTileServerKey(server.id());
            mTileServerKeyField.setText(key != null ? key : "");
        }
    }

    private void updateLocation()
    {
        RadioReferenceSettings.Location location = getSettings().getRadioReferenceSettings().getLocation();
        mLocationLabel.setText(location != null ? location.describe() : "Not set");
    }

    /**
     * Follows changes made on the Map tab
     */
    @Subscribe
    public void settingsUpdated(SettingsType settingsType)
    {
        if(settingsType == SettingsType.MAP)
        {
            Platform.runLater(() -> {
                TileChoice stored = getStoredTileChoice();

                if(!stored.equals(mTileChoiceComboBox.getValue()))
                {
                    mTileChoiceComboBox.setValue(stored);
                }

                updateKeyField();

                if(mShowSitesSwitch.isSelected() != mMapSettings.isShowRadioReferenceSites())
                {
                    mShowSitesSwitch.setSelected(mMapSettings.isShowRadioReferenceSites());
                }
            });
        }
        else if(settingsType == SettingsType.RADIO_REFERENCE)
        {
            Platform.runLater(this::updateLocation);
        }
    }
}
