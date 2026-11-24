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

import io.github.dsheirer.settings.Settings;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.value.ObservableBooleanValue;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.javafx.IconNode;

/**
 * Base class for a settings dialog page.  The page shows a header with the page icon, title and description from its
 * {@link SettingsPageType}, followed by the sections that the page adds with {@link #addSection(String)}, and a button
 * that restores the defaults of the page's rows that track a default.  The page scrolls vertically when its content
 * is taller than the dialog.
 */
public abstract class SettingsPage extends ScrollPane
{
    private final SettingsPageType mPageType;
    private final Settings mSettings;
    private final VBox mContent = new VBox();
    private final List<SettingsSection> mSections = new ArrayList<>();
    private BooleanBinding mAnyChanged;

    /**
     * Constructs an instance
     * @param pageType for the page header
     * @param settings to edit
     */
    protected SettingsPage(SettingsPageType pageType, Settings settings)
    {
        mPageType = pageType;
        mSettings = settings;

        getStyleClass().add("settings-page");
        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);

        mContent.getStyleClass().add("settings-page-content");
        mContent.getChildren().add(createHeader());
        setContent(mContent);
    }

    /**
     * Type of this page
     */
    public SettingsPageType getPageType()
    {
        return mPageType;
    }

    /**
     * Settings edited by this page
     */
    protected Settings getSettings()
    {
        return mSettings;
    }

    /**
     * Adds a titled section to the end of the page.
     * @param title of the section, or null for a section without a title.
     * @return section, for adding rows.
     */
    protected SettingsSection addSection(String title)
    {
        SettingsSection section = new SettingsSection(title);
        mSections.add(section);
        mContent.getChildren().add(section);
        return section;
    }

    /**
     * Adds a node to the end of the page, outside of any section.
     */
    protected void addContent(Node node)
    {
        mContent.getChildren().add(node);
    }

    /**
     * Rows on this page that track their setting against a default
     */
    public List<SettingsRow> getResettableRows()
    {
        List<SettingsRow> rows = new ArrayList<>();

        for(SettingsSection section: mSections)
        {
            for(SettingsRow row: section.getRows())
            {
                if(row.isResettable())
                {
                    rows.add(row);
                }
            }
        }

        return rows;
    }

    /**
     * Restores the default of every row that tracks one.
     */
    public void resetToDefaults()
    {
        getResettableRows().forEach(SettingsRow::resetToDefault);
    }

    /**
     * Indicates if the page title, description or any section contains the query.
     * @param query in lower case
     */
    public boolean matches(String query)
    {
        return SettingsRow.contains(mPageType.getTitle(), query) ||
            SettingsRow.contains(mPageType.getDescription(), query) ||
            mSections.stream().anyMatch(section -> section.matches(query));
    }

    /**
     * Highlights the sections and rows that match the query.
     * @param query in lower case, or empty to clear the highlights
     */
    public void highlight(String query)
    {
        mSections.forEach(section -> section.highlight(query));
    }

    /**
     * Handles a request that opened this page, for requests that also ask the page to do something.  The default
     * does nothing.
     * @param request that opened this page
     */
    public void process(ViewSettingsEditorRequest request)
    {
    }

    /**
     * Completes the page after the subclass constructor has added its sections: adds the reset button when the page
     * has rows that track a default.  Called once by the settings dialog.
     */
    void complete()
    {
        List<SettingsRow> rows = getResettableRows();

        if(rows.isEmpty())
        {
            return;
        }

        ObservableBooleanValue[] changed = rows.stream().map(SettingsRow::changedProperty)
            .toArray(ObservableBooleanValue[]::new);
        mAnyChanged = Bindings.createBooleanBinding(() -> rows.stream().anyMatch(row -> row.changedProperty().get()),
            changed);

        IconNode icon = new IconNode(FontAwesome.UNDO);
        icon.setIconSize(12);
        icon.getStyleClass().add("settings-reset-icon");
        Button reset = new Button("Reset Page to Defaults", icon);
        reset.getStyleClass().add("settings-reset-button");
        reset.disableProperty().bind(mAnyChanged.not());
        reset.setOnAction(event -> {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Restore the default for each setting on the " +
                mPageType.getTitle() + " page?", ButtonType.YES, ButtonType.NO);
            alert.setTitle("Reset Page to Defaults");
            alert.setHeaderText("Reset " + mPageType.getTitle() + " settings");
            alert.initOwner(getScene().getWindow());
            alert.showAndWait().filter(button -> button == ButtonType.YES).ifPresent(button -> resetToDefaults());
        });

        Label note = new Label("A dot beside a setting marks a change from its default.");
        note.getStyleClass().add("settings-help");

        HBox footer = new HBox(12, note, reset);
        footer.getStyleClass().add("settings-page-footer");
        footer.setAlignment(Pos.CENTER_RIGHT);
        mContent.getChildren().add(footer);
    }

    private Node createHeader()
    {
        IconNode icon = new IconNode(mPageType.getIcon());
        icon.setIconSize(20);
        icon.getStyleClass().add("settings-page-icon");

        Label title = new Label(mPageType.getTitle(), icon);
        title.getStyleClass().add("settings-page-title");

        Label description = new Label(mPageType.getDescription());
        description.getStyleClass().add("settings-page-description");
        description.setWrapText(true);

        VBox header = new VBox(title, description);
        header.getStyleClass().add("settings-page-header");
        return header;
    }
}
