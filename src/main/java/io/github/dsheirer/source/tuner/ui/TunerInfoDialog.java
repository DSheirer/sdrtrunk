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

import java.util.ArrayList;
import java.util.List;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * Shows tuner information that the tuner editors describe with a small subset of HTML: an optional h3 heading
 * followed by lines separated by br, where a line is either "<b>Key: </b>value" or plain text.
 */
public class TunerInfoDialog
{
    private TunerInfoDialog()
    {
    }

    /**
     * A parsed line of tuner information.
     * @param key bold key text, or null for a plain line
     * @param value text
     */
    public record Line(String key, String value)
    {
    }

    /**
     * Parsed tuner information.
     * @param heading text or null
     * @param lines of information
     */
    public record Info(String heading, List<Line> lines)
    {
        /**
         * Plain text version for copying to the clipboard.
         */
        public String toText()
        {
            StringBuilder sb = new StringBuilder();

            if(heading != null)
            {
                sb.append(heading).append("\n");
            }

            for(Line line: lines)
            {
                if(line.key() != null)
                {
                    sb.append(line.key()).append(": ");
                }
                sb.append(line.value()).append("\n");
            }

            return sb.toString();
        }
    }

    /**
     * Parses the tuner information HTML.
     */
    public static Info parse(String html)
    {
        String text = html == null ? "" : html.replaceAll("(?i)</?html>", "");
        String heading = null;

        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(?is)<h3>(.*?)</h3>").matcher(text);

        if(matcher.find())
        {
            heading = strip(matcher.group(1));
            text = text.substring(0, matcher.start()) + text.substring(matcher.end());
        }

        List<Line> lines = new ArrayList<>();

        for(String part: text.split("(?i)<br\\s*/?>|\\n"))
        {
            java.util.regex.Matcher keyed = java.util.regex.Pattern.compile("(?is)^\\s*<b>(.*?)</b>(.*)$").matcher(part);

            if(keyed.matches())
            {
                String key = strip(keyed.group(1)).trim();

                if(key.endsWith(":"))
                {
                    key = key.substring(0, key.length() - 1).trim();
                }

                lines.add(new Line(key, strip(keyed.group(2)).trim()));
            }
            else
            {
                String plain = strip(part).trim();

                if(!plain.isEmpty())
                {
                    lines.add(new Line(null, plain));
                }
            }
        }

        return new Info(heading, lines);
    }

    private static String strip(String text)
    {
        return text.replaceAll("<[^>]+>", "").replace("&nbsp;", " ").replace("&amp;", "&")
                .replace("&lt;", "<").replace("&gt;", ">");
    }

    /**
     * Creates the content node for the information.
     */
    public static Node createContent(Info info)
    {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("tuner-info-grid");
        grid.setHgap(12);
        grid.setVgap(4);
        int row = 0;

        for(Line line: info.lines())
        {
            Label value = new Label(line.value());
            value.setWrapText(true);

            if(line.key() != null)
            {
                Label key = new Label(line.key());
                key.setStyle("-fx-font-weight: bold;");
                grid.add(key, 0, row);
                grid.add(value, 1, row);
            }
            else
            {
                grid.add(value, 0, row, 2, 1);
            }

            row++;
        }

        VBox box = new VBox(8, grid);
        box.setMinWidth(320);
        return box;
    }

    /**
     * Shows the tuner information dialog.
     * @param owner window or null
     * @param html tuner information
     */
    public static void show(Window owner, String html)
    {
        Info info = parse(html);
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.initOwner(owner);
        alert.setTitle("Tuner Info");
        alert.setHeaderText(info.heading() != null ? info.heading() : "Tuner Info");
        alert.getDialogPane().setContent(createContent(info));

        ButtonType copy = new ButtonType("Copy", ButtonBar.ButtonData.LEFT);
        alert.getButtonTypes().add(copy);
        alert.getDialogPane().lookupButton(copy).addEventFilter(javafx.event.ActionEvent.ACTION, e -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(info.toText());
            Clipboard.getSystemClipboard().setContent(content);
            e.consume();
        });

        alert.show();
    }
}
