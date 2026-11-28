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

import io.github.dsheirer.controller.channel.ChannelProcessingManager;
import io.github.dsheirer.database.DatabaseManager;
import io.github.dsheirer.service.radioreference.RadioReference;
import io.github.dsheirer.settings.Setting;
import io.github.dsheirer.settings.Settings;
import io.github.dsheirer.web.WebServer;
import java.net.URL;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.javafx.IconNode;

/**
 * Settings dialog: a search box and a sidebar of settings pages grouped under headers, and the selected page.
 * Settings apply as soon as they are changed, so the dialog has no OK or Apply button.  Escape clears the search, or
 * closes the dialog when there is no search.  The dialog reopens on the page that it last showed.
 */
public class SettingsEditor extends BorderPane
{
    private static final SettingsPageType DEFAULT_PAGE = SettingsPageType.GENERAL;
    private static final KeyCombination FIND = new KeyCodeCombination(KeyCode.F, KeyCombination.SHORTCUT_DOWN);
    private final SettingsContext mContext;
    private final Map<SettingsPageType,SettingsPage> mPages = new EnumMap<>(SettingsPageType.class);
    private final Map<SettingsPageType,ToggleButton> mNavigationButtons = new EnumMap<>(SettingsPageType.class);
    private final Map<SettingsPageType.Group,Label> mGroupHeaders = new EnumMap<>(SettingsPageType.Group.class);
    private final ToggleGroup mNavigationGroup = new ToggleGroup();
    private final TextField mSearchField = new TextField();
    private final Label mNoMatchesLabel = new Label("No settings match");
    private SettingsPageType mCurrentPage;
    private String mQuery = "";

    /**
     * Constructs an instance
     *
     * @param settings to edit
     * @param radioReference service for the Radio Reference account controls, or null
     * @param channelProcessingManager for the processing state of channels, or null
     * @param databaseManager for the database page, or null
     * @param webServer for the web server page, or null
     */
    public SettingsEditor(Settings settings, RadioReference radioReference,
                          ChannelProcessingManager channelProcessingManager, DatabaseManager databaseManager,
                          WebServer webServer)
    {
        mContext = new SettingsContext(settings, radioReference, channelProcessingManager, databaseManager, webServer);

        getStyleClass().add("settings-editor");
        URL stylesheet = SettingsEditor.class.getResource("/settings_editor.css");

        if(stylesheet != null)
        {
            getStylesheets().add(stylesheet.toExternalForm());
        }

        setLeft(createSidebar());
        setBottom(createFooter());
        showPage(getLastPage());

        addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if(FIND.match(event))
            {
                mSearchField.requestFocus();
                mSearchField.selectAll();
                event.consume();
            }
            else if(event.getCode() == KeyCode.ESCAPE)
            {
                if(!mSearchField.getText().isEmpty())
                {
                    mSearchField.clear();
                }
                else if(getScene() != null && getScene().getWindow() != null)
                {
                    getScene().getWindow().hide();
                }

                event.consume();
            }
        });
    }

    /**
     * Shows the page requested, or keeps the current page when the request doesn't specify one.
     */
    public void process(ViewSettingsEditorRequest request)
    {
        if(request.getPageType() != null)
        {
            //A requested page might be hidden by a search, so clear the search first
            mSearchField.clear();
            showPage(request.getPageType());
            getPage(request.getPageType()).process(request);
        }
    }

    /**
     * Shows the page, creating it the first time it is shown.
     */
    public void showPage(SettingsPageType pageType)
    {
        mCurrentPage = pageType;
        setCenter(getPage(pageType));
        Setting.SETTINGS_EDITOR_LAST_PAGE.put(pageType.name());

        ToggleButton button = mNavigationButtons.get(pageType);

        if(button != null && !button.isSelected())
        {
            button.setSelected(true);
        }
    }

    /**
     * Page for the type, created and completed the first time it is requested.
     */
    private SettingsPage getPage(SettingsPageType pageType)
    {
        SettingsPage page = mPages.get(pageType);

        if(page == null)
        {
            page = pageType.createPage(mContext);
            page.complete();
            page.highlight(mQuery);
            mPages.put(pageType, page);
        }

        return page;
    }

    /**
     * Page that the dialog last showed, or the default page.
     */
    private static SettingsPageType getLastPage()
    {
        String stored = Setting.SETTINGS_EDITOR_LAST_PAGE.get(null);

        if(stored != null)
        {
            try
            {
                return SettingsPageType.valueOf(stored);
            }
            catch(IllegalArgumentException e)
            {
                //A page that no longer exists
            }
        }

        return DEFAULT_PAGE;
    }

    /**
     * Shows only the pages that match the query, and highlights the matching settings on each page.
     */
    private void search(String text)
    {
        mQuery = text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
        List<SettingsPageType> matches = new ArrayList<>();

        for(SettingsPageType pageType: SettingsPageType.values())
        {
            //Searching looks inside each page, so every page is created on the first search
            SettingsPage page = getPage(pageType);
            page.highlight(mQuery);
            boolean match = mQuery.isEmpty() || page.matches(mQuery);
            ToggleButton button = mNavigationButtons.get(pageType);
            button.setVisible(match);
            button.setManaged(match);

            if(match)
            {
                matches.add(pageType);
            }
        }

        for(Map.Entry<SettingsPageType.Group,Label> entry: mGroupHeaders.entrySet())
        {
            boolean visible = matches.stream().anyMatch(type -> type.getGroup() == entry.getKey());
            entry.getValue().setVisible(visible);
            entry.getValue().setManaged(visible);
        }

        mNoMatchesLabel.setVisible(matches.isEmpty());
        mNoMatchesLabel.setManaged(matches.isEmpty());

        if(!matches.isEmpty() && !matches.contains(mCurrentPage))
        {
            showPage(matches.getFirst());
        }
    }

    /**
     * Search box above the sidebar with one navigation button per page, grouped under group headers.
     */
    private VBox createSidebar()
    {
        IconNode searchIcon = new IconNode(FontAwesome.SEARCH);
        searchIcon.setIconSize(12);
        searchIcon.getStyleClass().add("settings-search-icon");
        mSearchField.setPromptText("Search settings");
        mSearchField.getStyleClass().add("settings-search-field");
        mSearchField.textProperty().addListener((observable, oldValue, newValue) -> search(newValue));
        HBox.setHgrow(mSearchField, Priority.ALWAYS);
        HBox searchBox = new HBox(8, searchIcon, mSearchField);
        searchBox.getStyleClass().add("settings-search");
        searchBox.setAlignment(Pos.CENTER_LEFT);

        VBox navigation = new VBox();
        navigation.getStyleClass().add("settings-navigation");
        SettingsPageType.Group group = null;

        for(SettingsPageType pageType: SettingsPageType.values())
        {
            if(pageType.getGroup() != group)
            {
                group = pageType.getGroup();
                Label header = new Label(group.toString().toUpperCase());
                header.getStyleClass().add("settings-group-header");
                mGroupHeaders.put(group, header);
                navigation.getChildren().add(header);
            }

            IconNode icon = new IconNode(pageType.getIcon());
            icon.setIconSize(14);
            icon.getStyleClass().add("settings-nav-icon");

            //Fixed width icon cell so that the titles line up whatever the icon's width
            StackPane iconCell = new StackPane(icon);
            iconCell.getStyleClass().add("settings-nav-icon-cell");

            ToggleButton button = new ToggleButton(pageType.getTitle(), iconCell);
            button.getStyleClass().setAll("settings-nav-item");
            button.setToggleGroup(mNavigationGroup);
            button.setMaxWidth(Double.MAX_VALUE);
            button.setAlignment(Pos.CENTER_LEFT);
            button.setOnAction(event -> showPage(pageType));
            mNavigationButtons.put(pageType, button);
            navigation.getChildren().add(button);
        }

        mNoMatchesLabel.getStyleClass().add("settings-no-matches");
        mNoMatchesLabel.setVisible(false);
        mNoMatchesLabel.setManaged(false);
        navigation.getChildren().add(mNoMatchesLabel);

        //Keep one page selected: clicking the selected page's button would otherwise deselect it
        mNavigationGroup.selectedToggleProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue == null && oldValue != null)
            {
                oldValue.setSelected(true);
            }
        });

        ScrollPane scrollPane = new ScrollPane(navigation);
        scrollPane.getStyleClass().add("settings-sidebar-scroll");
        scrollPane.setFitToWidth(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        VBox sidebar = new VBox(searchBox, scrollPane);
        sidebar.getStyleClass().add("settings-sidebar");
        return sidebar;
    }

    private HBox createFooter()
    {
        Label note = new Label("Changes are saved automatically.");
        note.getStyleClass().add("settings-footer-note");
        HBox footer = new HBox(note);
        footer.getStyleClass().add("settings-footer");
        footer.setAlignment(Pos.CENTER_LEFT);
        return footer;
    }
}
