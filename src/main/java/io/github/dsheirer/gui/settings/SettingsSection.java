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

package io.github.dsheirer.gui.settings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Titled group of related settings on a settings page.  Add one row per setting with
 * {@link #addRow(String, Node...)}, explanatory text with {@link #addHelp(String)}, and anything that doesn't fit the
 * row layout (tables, consoles) with {@link #addContent(Node)}.
 */
public class SettingsSection extends VBox
{
    private static final PseudoClass SEARCH_MATCH = PseudoClass.getPseudoClass("search-match");
    private final Label mTitle;
    private final List<SettingsRow> mRows = new ArrayList<>();
    private final List<Label> mHelp = new ArrayList<>();

    /**
     * Constructs an instance
     * @param title of the section, or null for a section without a title.
     */
    public SettingsSection(String title)
    {
        getStyleClass().add("settings-section");

        if(title != null)
        {
            mTitle = new Label(title);
            mTitle.getStyleClass().add("settings-section-title");
            getChildren().add(mTitle);
        }
        else
        {
            mTitle = null;
        }
    }

    /**
     * Adds a row with a label on the left and the controls on the right.
     * @param label for the setting
     * @param controls that edit the setting
     * @return row, for adding help text, a default value or a restart required badge.
     */
    public SettingsRow addRow(String label, Node... controls)
    {
        SettingsRow row = new SettingsRow(label, controls);
        mRows.add(row);
        getChildren().add(row);
        return row;
    }

    /**
     * Adds a paragraph of explanatory text for the section.
     * @param text to show
     * @return label, for further styling.
     */
    public Label addHelp(String text)
    {
        Label label = new Label(text);
        label.getStyleClass().add("settings-help");
        label.setWrapText(true);
        mHelp.add(label);
        getChildren().add(label);
        return label;
    }

    /**
     * Adds a node across the full width of the section.
     */
    public void addContent(Node node)
    {
        getChildren().add(node);
    }

    /**
     * Rows in this section
     */
    public List<SettingsRow> getRows()
    {
        return Collections.unmodifiableList(mRows);
    }

    /**
     * Indicates if the section title, help text or any row contains the query.
     * @param query in lower case
     */
    public boolean matches(String query)
    {
        return (mTitle != null && SettingsRow.contains(mTitle.getText(), query)) ||
            mHelp.stream().anyMatch(help -> SettingsRow.contains(help.getText(), query)) ||
            mRows.stream().anyMatch(row -> row.matches(query));
    }

    /**
     * Highlights the section title and the rows that match the query.
     * @param query in lower case, or empty to clear the highlights
     */
    public void highlight(String query)
    {
        if(mTitle != null)
        {
            mTitle.pseudoClassStateChanged(SEARCH_MATCH, !query.isEmpty() &&
                SettingsRow.contains(mTitle.getText(), query));
        }

        mRows.forEach(row -> row.highlight(query));
    }
}
