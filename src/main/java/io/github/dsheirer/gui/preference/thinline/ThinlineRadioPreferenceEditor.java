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

package io.github.dsheirer.gui.preference.thinline;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.thinline.ThinlineRadioPreference;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

/**
 * Thinline Radio site URL and site key used by Thinline Radio Core streams.
 */
public class ThinlineRadioPreferenceEditor extends HBox
{
    private final ThinlineRadioPreference mPreference;

    public ThinlineRadioPreferenceEditor(UserPreferences userPreferences)
    {
        mPreference = userPreferences.getThinlineRadioPreference();
        GridPane grid = new GridPane();
        grid.setPadding(new Insets(10));
        grid.setHgap(8);
        grid.setVgap(10);

        int row = 0;
        Label intro = new Label("Site URL and site key for Thinline Radio Core. Core streams use this key instead of a per-stream upload key.");
        intro.setWrapText(true);
        intro.setPrefWidth(480);
        grid.add(intro, 0, row, 2, 1);

        grid.add(new Label("Site URL"), 0, ++row);
        TextField url = new TextField(nullToEmpty(mPreference.getUrl()));
        url.setPrefWidth(360);
        url.setPromptText("https://example.com");
        url.textProperty().addListener((o, old, value) -> mPreference.setUrl(value));
        grid.add(url, 1, row);

        grid.add(new Label("Site key"), 0, ++row);
        TextField apiKey = new TextField(nullToEmpty(mPreference.getApiKey()));
        apiKey.setPrefWidth(360);
        apiKey.setPromptText("Site key");
        apiKey.textProperty().addListener((o, old, value) -> mPreference.setApiKey(value));
        grid.add(apiKey, 1, row);

        getChildren().add(grid);
    }

    private static String nullToEmpty(String value)
    {
        return value == null ? "" : value;
    }
}
