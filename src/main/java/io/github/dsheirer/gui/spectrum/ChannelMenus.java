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

package io.github.dsheirer.gui.spectrum;

import io.github.dsheirer.controller.channel.Channel;
import io.github.dsheirer.eventbus.MyEventBus;
import io.github.dsheirer.gui.playlist.channel.ViewChannelRequest;
import io.github.dsheirer.source.tuner.channel.TunerChannel;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.WindowEvent;

/**
 * Context menu entries of the Channels section, shared by the playlist channel and radio reference layers.  Each
 * channel is a submenu showing its title and details, like the display's hover cards, with the actions for its state.
 * The entries have the display theme's colors: the card background, with the title colored by state - the theme's
 * active color (a green) for an enabled playlist channel, its inactive color (a blue) for a disabled one, and its
 * plain text color for a radio reference candidate that isn't in the playlist.
 */
public final class ChannelMenus
{
    /**
     * Context menu section of the channel entries
     */
    public static final String SECTION = "Channels";

    private static final DecimalFormat FREQUENCY_FORMAT = new DecimalFormat("0.00000");
    private static final String STYLE_ITEM = "channel-menu-item";
    private static final String STYLE_TITLE = "channel-menu-title";
    private static final String STYLE_DETAIL = "channel-menu-detail";
    private static final String STYLE_NOTE = "channel-menu-note";
    //Minimum contrast ratio of the colored text against the menu background
    private static final double MIN_CONTRAST = 3.0;

    /**
     * State of a channel entry, which sets its color
     */
    public enum State
    {
        ENABLED("channel-menu-enabled"),
        DISABLED("channel-menu-disabled"),
        CANDIDATE("channel-menu-candidate");

        private final String mStyleClass;

        State(String styleClass)
        {
            mStyleClass = styleClass;
        }

        public String getStyleClass()
        {
            return mStyleClass;
        }
    }

    private ChannelMenus()
    {
    }

    /**
     * Channel entry submenu showing the title and details
     * @param title of the channel
     * @param details line, or null
     * @param state of the channel
     * @param key identifying the channel, e.g. the playlist channel, so the display lists it once, or null
     */
    public static Menu createEntry(String title, String details, State state, Object key)
    {
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add(STYLE_TITLE);
        VBox box = new VBox(titleLabel);

        if(details != null)
        {
            Label detailLabel = new Label(details);
            detailLabel.getStyleClass().add(STYLE_DETAIL);
            box.getChildren().add(detailLabel);
        }

        Menu menu = new Menu(null, box);
        menu.getStyleClass().addAll(STYLE_ITEM, state.getStyleClass());
        menu.setUserData(key);
        return menu;
    }

    /**
     * Entry for a playlist channel: enable or disable it, and view or edit it in the playlist editor
     * @param channel in the playlist
     * @param tunerChannel of the channel to describe, or null for the channel's first
     * @param toggle that enables the channel when it's disabled or disables it when it's enabled, or null when the
     * channel can't be started or stopped from the menu, e.g. a traffic channel
     */
    public static Menu createChannelEntry(Channel channel, TunerChannel tunerChannel, Runnable toggle)
    {
        boolean processing = channel.isProcessing();
        State state = getState(channel);
        Menu menu = createEntry(getTitle(channel), String.join(" \u00B7 ", getDetails(channel, tunerChannel)), state,
            channel);

        if(toggle != null)
        {
            menu.getItems().add(createItem(processing ? "Disable" : "Enable", state, toggle));
        }

        menu.getItems().add(createItem("View/Edit in Playlist", state,
            () -> MyEventBus.getGlobalEventBus().post(new ViewChannelRequest(channel))));
        return menu;
    }

    /**
     * Hover card entry for a playlist channel
     * @param channel in the playlist
     * @param tunerChannel of the channel to describe, or null for the channel's first
     * @param prompts true when the entry is reason to show the card
     */
    public static ChannelCardEntry createCardEntry(Channel channel, TunerChannel tunerChannel, boolean prompts)
    {
        //One details line, as in the menu entry, so the card has room for more channels
        return new ChannelCardEntry(getTitle(channel), List.of(String.join(" \u00B7 ", getDetails(channel,
            tunerChannel))), getState(channel), channel, prompts);
    }

    /**
     * State of a playlist channel
     */
    public static State getState(Channel channel)
    {
        return channel.isProcessing() ? State.ENABLED : State.DISABLED;
    }

    private static String getTitle(Channel channel)
    {
        return channel.getName() != null && !channel.getName().isEmpty() ? channel.getName() : "(unnamed)";
    }

    /**
     * Detail lines of a playlist channel: the system and site, then the decoder, frequency and state
     * @param tunerChannel of the channel to describe, or null for the channel's first
     */
    public static List<String> getDetails(Channel channel, TunerChannel tunerChannel)
    {
        if(tunerChannel == null)
        {
            List<TunerChannel> tunerChannels = channel.getTunerChannels();
            tunerChannel = tunerChannels.isEmpty() ? null : tunerChannels.get(0);
        }

        List<String> details = new ArrayList<>();
        String systemSite = join(channel.hasSystem() ? channel.getSystem() : null,
            channel.hasSite() ? channel.getSite() : null);

        if(systemSite != null)
        {
            details.add(systemSite);
        }

        details.add(join(channel.getDecodeConfiguration().getDecoderType().getShortDisplayString(),
            tunerChannel != null ? FREQUENCY_FORMAT.format(tunerChannel.getFrequency() / 1E6) + " MHz" : null,
            channel.isProcessing() ? "Enabled" : "Disabled"));
        return details;
    }

    /**
     * Action item of an entry's submenu
     */
    public static MenuItem createItem(String text, State state, Runnable action)
    {
        MenuItem item = new MenuItem(text);
        item.getStyleClass().addAll(STYLE_ITEM, state.getStyleClass());
        item.setOnAction(event -> action.run());
        return item;
    }

    /**
     * Inactive item of an entry's submenu that explains something, e.g. why a candidate can't be added
     */
    public static MenuItem createNote(String text)
    {
        MenuItem item = new MenuItem(text);
        item.getStyleClass().addAll(STYLE_ITEM, STYLE_NOTE);
        item.setDisable(true);
        return item;
    }

    /**
     * Separator of an entry's submenu
     */
    public static SeparatorMenuItem createSeparator()
    {
        SeparatorMenuItem separator = new SeparatorMenuItem();
        separator.getStyleClass().add(STYLE_ITEM);
        return separator;
    }

    /**
     * Gives the context menu's channel entries the theme's colors.  The stylesheet goes into the scenes of the menu
     * and the entry submenus, since a popup takes its owner's stylesheets when it's shown and the application theme's
     * stylesheets are added to each popup's scene: the more specific rules win in the same scene.
     */
    public static void style(ContextMenu menu, SpectralDisplayTheme theme)
    {
        String stylesheet = createStylesheet(theme);
        menu.addEventHandler(WindowEvent.WINDOW_SHOWN, event -> addStylesheet(menu.getScene(), stylesheet));

        for(MenuItem item: menu.getItems())
        {
            if(item instanceof Menu entry && entry.getStyleClass().contains(STYLE_ITEM))
            {
                //The submenu's popup isn't exposed, but its items' nodes are in its scene once it's showing
                EventHandler<Event> handler = event -> {
                    for(MenuItem child: entry.getItems())
                    {
                        Node node = child.getStyleableNode();

                        if(node != null && node.getScene() != null)
                        {
                            addStylesheet(node.getScene(), stylesheet);
                            return;
                        }
                    }
                };
                entry.setOnShowing(handler);
                entry.setOnShown(handler);
            }
        }
    }

    private static void addStylesheet(Scene scene, String stylesheet)
    {
        if(scene != null && !scene.getStylesheets().contains(stylesheet))
        {
            scene.getStylesheets().add(stylesheet);
        }
    }

    /**
     * Stylesheet with the theme's colors
     * @return data URL of the stylesheet
     */
    static String createStylesheet(SpectralDisplayTheme theme)
    {
        Color background = opaque(theme.getBackground());
        String detail = css(readable(opaque(theme.getLine()), background));
        String item = ".menu-item." + STYLE_ITEM;
        StringBuilder sb = new StringBuilder();
        sb.append(item).append(" { -fx-background-color: ").append(css(background)).append("; }\n");
        sb.append(item).append(":focused, ").append(item).append(":hover { -fx-background-color: ")
            .append(css(background)).append(", ").append(css(theme.getLine(0.35))).append("; }\n");
        sb.append(item).append(":disabled, ").append(item).append(":disabled .label { -fx-opacity: 1; }\n");

        for(State state: State.values())
        {
            String color = css(readable(getColor(theme, state), background));
            String stateItem = item + "." + state.getStyleClass();
            //The item labels, and the title of an entry
            sb.append(textRules(color, stateItem + " .label", stateItem + ":focused .label",
                stateItem + ":hover .label"));
        }

        //More specific than the state rules, so the details keep their color on hover
        sb.append(textRules(detail, item + " .label." + STYLE_DETAIL, item + ":focused .label." + STYLE_DETAIL,
            item + ":hover .label." + STYLE_DETAIL, item + "." + STYLE_NOTE + " .label"));
        sb.append(item).append(" .label.").append(STYLE_TITLE).append(" { -fx-font-weight: bold; }\n");
        sb.append(".menu.").append(STYLE_ITEM).append(" > .right-container > .arrow { -fx-background-color: ")
            .append(detail).append("; }\n");
        sb.append(".separator-menu-item.").append(STYLE_ITEM).append(" { -fx-padding: 0.25em 0; }\n");
        sb.append(".separator-menu-item.").append(STYLE_ITEM).append(" > .line { -fx-border-color: ")
            .append(css(theme.getLine(0.5)))
            .append(" transparent transparent transparent; -fx-border-insets: 0; -fx-padding: 0; }\n");
        return "data:text/css;base64," +
            Base64.getEncoder().encodeToString(sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Title color of the state: the theme's active color (a green) for an enabled playlist channel, its inactive
     * color (a blue) for a disabled one, and its plain text color for a radio reference candidate
     */
    public static Color getColor(SpectralDisplayTheme theme, State state)
    {
        return switch(state)
        {
            case ENABLED -> theme.getChannelActive();
            case DISABLED -> theme.getChannelInactive();
            case CANDIDATE -> theme.getText();
        };
    }

    /**
     * Color moved toward white on a dark background, or black on a light one, until it's readable on the background
     */
    public static Color readable(Color color, Color background)
    {
        Color target = luminance(background) < 0.5 ? Color.WHITE : Color.BLACK;
        Color adjusted = color;

        for(int step = 1; step <= 10 && contrast(adjusted, background) < MIN_CONTRAST; step++)
        {
            adjusted = color.interpolate(target, step / 10.0);
        }

        return adjusted;
    }

    static double contrast(Color a, Color b)
    {
        double la = luminance(a);
        double lb = luminance(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    private static double luminance(Color color)
    {
        return (0.2126 * linear(color.getRed())) + (0.7152 * linear(color.getGreen())) +
            (0.0722 * linear(color.getBlue()));
    }

    private static double linear(double channel)
    {
        return channel <= 0.03928 ? channel / 12.92 : Math.pow((channel + 0.055) / 1.055, 2.4);
    }

    /**
     * Rules that set the text color of the labels and of their text nodes.  The dark application theme fills each
     * label's text node, so both are set.
     */
    private static String textRules(String color, String... labelSelectors)
    {
        String labels = String.join(", ", labelSelectors);
        String texts = String.join(" .text, ", labelSelectors) + " .text";
        return labels + " { -fx-text-fill: " + color + "; }\n" + texts + " { -fx-fill: " + color + "; }\n";
    }

    private static Color opaque(Color color)
    {
        return Color.color(color.getRed(), color.getGreen(), color.getBlue());
    }

    private static String css(Color color)
    {
        return String.format(Locale.ROOT, "rgba(%d,%d,%d,%.3f)", Math.round(color.getRed() * 255),
            Math.round(color.getGreen() * 255), Math.round(color.getBlue() * 255), color.getOpacity());
    }

    /**
     * Values joined with a middle dot, skipping null and empty values
     */
    public static String join(String... values)
    {
        StringBuilder sb = new StringBuilder();

        for(String value: values)
        {
            if(value != null && !value.isEmpty())
            {
                if(!sb.isEmpty())
                {
                    sb.append(" · ");
                }

                sb.append(value);
            }
        }

        return sb.isEmpty() ? null : sb.toString();
    }
}
