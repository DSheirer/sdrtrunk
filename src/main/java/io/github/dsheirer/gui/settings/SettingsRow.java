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
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Labeled;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.javafx.IconNode;
import org.controlsfx.control.ToggleSwitch;

/**
 * One setting in a settings section: a label on the left, the controls on the right, and optional help text and a
 * restart required badge.
 *
 * A row can track whether its setting differs from the default with {@link #defaultValue(ObservableValue, Object,
 * Consumer)} or {@link #track(ObservableBooleanValue, Runnable)}.  A changed row shows a marker in the margin that
 * restores the default when clicked, and the page's reset button restores every tracked row.
 */
public class SettingsRow extends VBox
{
    private static final PseudoClass SEARCH_MATCH = PseudoClass.getPseudoClass("search-match");
    private static final double MARKER_SIZE = 10;
    private final HBox mLine = new HBox();
    private final Label mLabel;
    private final List<Label> mHelp = new ArrayList<>();
    private final List<Tracker> mTrackers = new ArrayList<>();
    private final ReadOnlyBooleanWrapper mChanged = new ReadOnlyBooleanWrapper();

    /**
     * Constructs an instance
     * @param label for the setting
     * @param controls that edit the setting
     */
    public SettingsRow(String label, Node... controls)
    {
        getStyleClass().add("settings-row");

        mLabel = new Label(label);
        mLabel.getStyleClass().add("settings-row-label");
        mLabel.setWrapText(true);
        mLabel.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(mLabel, Priority.ALWAYS);

        if(controls.length > 0)
        {
            mLabel.setLabelFor(controls[0]);
        }

        //Buttons, switches and value labels keep their natural width; combo and choice boxes shrink instead
        for(Node control: controls)
        {
            if(control instanceof Labeled || control instanceof ToggleSwitch)
            {
                ((Region)control).setMinWidth(Region.USE_PREF_SIZE);
            }
        }

        HBox controlsBox = new HBox(controls);
        controlsBox.getStyleClass().add("settings-row-controls");
        controlsBox.setAlignment(Pos.CENTER_RIGHT);

        mLine.getStyleClass().add("settings-row-line");
        mLine.setAlignment(Pos.CENTER_LEFT);
        mLine.getChildren().addAll(mLabel, controlsBox);
        getChildren().add(mLine);
    }

    /**
     * Label for the setting
     */
    public Label getLabel()
    {
        return mLabel;
    }

    /**
     * Adds help text under the row.
     * @param text to show
     * @return this row
     */
    public SettingsRow help(String text)
    {
        Label help = new Label(text);
        help.getStyleClass().add("settings-help");
        help.setWrapText(true);
        mHelp.add(help);
        getChildren().add(help);
        return this;
    }

    /**
     * Adds a node under the row, across its full width.
     * @param node to add
     * @return this row
     */
    public SettingsRow content(Node node)
    {
        getChildren().add(node);
        return this;
    }

    /**
     * Adds a restart required badge after the row label, for settings that take effect after a restart.
     * @return this row
     */
    public SettingsRow requiresRestart()
    {
        mLine.getChildren().add(1, createRestartBadge());
        return this;
    }

    /**
     * Tracks a value of this row against its default.
     * @param value of the control that edits the setting
     * @param defaultValue of the setting
     * @param apply to set the control, and with it the setting, to the default
     * @return this row
     */
    public <T> SettingsRow defaultValue(ObservableValue<T> value, T defaultValue, Consumer<T> apply)
    {
        return track(Bindings.createBooleanBinding(() -> !Objects.equals(value.getValue(), defaultValue), value),
            () -> apply.accept(defaultValue));
    }

    /**
     * Tracks whether this row's setting differs from its default.
     * @param changed is true when the setting differs from the default
     * @param reset restores the default
     * @return this row
     */
    public SettingsRow track(ObservableBooleanValue changed, Runnable reset)
    {
        if(mTrackers.isEmpty())
        {
            createMarker();
        }

        mTrackers.add(new Tracker(changed, reset));
        changed.addListener((observable, oldValue, newValue) -> updateChanged());
        updateChanged();
        return this;
    }

    /**
     * Indicates if this row tracks its setting against a default
     */
    public boolean isResettable()
    {
        return !mTrackers.isEmpty();
    }

    /**
     * True when the row's setting differs from its default
     */
    public ReadOnlyBooleanProperty changedProperty()
    {
        return mChanged.getReadOnlyProperty();
    }

    /**
     * Restores the default for each tracked value that differs from it
     */
    public void resetToDefault()
    {
        for(Tracker tracker: List.copyOf(mTrackers))
        {
            if(tracker.changed().get())
            {
                tracker.reset().run();
            }
        }
    }

    /**
     * Indicates if the row is visible and its label or help text contains the query.
     * @param query in lower case
     */
    public boolean matches(String query)
    {
        //A hidden row, such as the key row of a tile server that doesn't need a key, can't be shown as a match
        if(!isVisible())
        {
            return false;
        }

        if(contains(mLabel.getText(), query))
        {
            return true;
        }

        for(Label help: mHelp)
        {
            if(contains(help.getText(), query))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Highlights the row when it matches the query.
     * @param query in lower case, or empty to clear the highlight
     * @return true if the row matches
     */
    public boolean highlight(String query)
    {
        boolean match = !query.isEmpty() && matches(query);
        pseudoClassStateChanged(SEARCH_MATCH, match);
        return match;
    }

    static boolean contains(String text, String query)
    {
        return text != null && text.toLowerCase(Locale.ROOT).contains(query);
    }

    private void updateChanged()
    {
        mChanged.set(mTrackers.stream().anyMatch(tracker -> tracker.changed().get()));
    }

    /**
     * Marker in the left margin of the section, shown while the row differs from its default.  Clicking it restores
     * the default.
     */
    private void createMarker()
    {
        IconNode icon = new IconNode(FontAwesome.CIRCLE);
        icon.setIconSize(8);
        icon.getStyleClass().add("settings-changed-icon");

        StackPane marker = new StackPane(icon);
        marker.getStyleClass().add("settings-changed-marker");
        marker.setManaged(false);
        marker.resize(MARKER_SIZE, MARKER_SIZE);
        marker.setTranslateX(-MARKER_SIZE - 2);
        marker.layoutYProperty().bind(mLine.heightProperty().subtract(MARKER_SIZE).divide(2));
        marker.visibleProperty().bind(mChanged);
        marker.setCursor(Cursor.HAND);
        Tooltip.install(marker, new Tooltip("Changed from the default.  Click to restore the default."));
        marker.setOnMouseClicked(event -> resetToDefault());
        mLine.getChildren().add(marker);
    }

    /**
     * Creates a restart required badge.
     */
    public static Labeled createRestartBadge()
    {
        IconNode icon = new IconNode(FontAwesome.EXCLAMATION_TRIANGLE);
        icon.setIconSize(10);
        icon.getStyleClass().add("settings-badge-icon");
        Label badge = new Label("Restart required", icon);
        badge.getStyleClass().add("settings-badge-restart");
        badge.setMinWidth(Label.USE_PREF_SIZE);
        return badge;
    }

    private record Tracker(ObservableBooleanValue changed, Runnable reset)
    {
    }
}
