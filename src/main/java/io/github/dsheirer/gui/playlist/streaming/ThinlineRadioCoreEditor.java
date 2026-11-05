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

package io.github.dsheirer.gui.playlist.streaming;

import io.github.dsheirer.audio.broadcast.BroadcastServerType;
import io.github.dsheirer.playlist.PlaylistManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Polygon;

/**
 * Editor for a Thinline Radio Core stream. The credential is the site key.
 */
public class ThinlineRadioCoreEditor extends RdioScannerEditor
{
    private static final String SUMMARY = "This site keeps each recording and tells ThinLine the system and talkgroup. " +
        "ThinLine asks one connected site to upload that transmission. The site key is the one in User Preferences, " +
        "under Thinline Radio. This stream does not have its own key. Turn off the Thinline Radio upload stream for " +
        "this same system, or that upload arrives before ThinLine has chosen.";
    private static final String MISSING_KEY = "Add the site key in User Preferences, under Thinline Radio, before this " +
        "stream can connect. Thinline Radio Core uses that key.";

    private Label mDescription;

    private static final String THEME_TEXT = "-fx-text-fill: -fx-text-background-color;";
    private static final String SITE_BOX = "-fx-border-color: -fx-accent; -fx-border-radius: 6; " +
        "-fx-background-color: -fx-control-inner-background; -fx-background-radius: 6;";
    private static final String HUB_BOX = "-fx-border-color: -fx-accent; -fx-border-width: 2; -fx-border-radius: 6; " +
        "-fx-background-color: -fx-control-inner-background; -fx-background-radius: 6;";

    public ThinlineRadioCoreEditor(PlaylistManager playlistManager)
    {
        super(playlistManager);
    }

    @Override
    public BroadcastServerType getBroadcastServerType()
    {
        return BroadcastServerType.THINLINE_RADIO_CORE;
    }

    @Override
    protected String getHostLabel()
    {
        return "Thinline Radio URL";
    }

    @Override
    protected boolean showsApiKey()
    {
        return false;
    }

    @Override
    public void setItem(io.github.dsheirer.audio.broadcast.rdioscanner.RdioScannerConfiguration item)
    {
        super.setItem(item);
        boolean ready = hasSiteKey();
        if(mDescription != null)
        {
            mDescription.setText(ready ? SUMMARY : MISSING_KEY);
        }
        if(item != null)
        {
            getEnabledSwitch().setDisable(!ready);
            String preferenceUrl = getPlaylistManager().getUserPreferences().getThinlineRadioPreference().getUrl();
            String host = getHostTextField().getText();
            if(preferenceUrl != null && !preferenceUrl.isBlank() &&
                (host == null || host.isBlank() || host.startsWith("http://localhost")))
            {
                String shown = preferenceUrl;
                String uploadPath = "/api/call-upload";
                if(shown.endsWith(uploadPath))
                {
                    shown = shown.substring(0, shown.length() - uploadPath.length());
                }
                getHostTextField().setText(shown);
                modifiedProperty().set(false);
            }
        }
    }

    @Override
    protected Node getTrailingNode()
    {
        mDescription = themedLabel(hasSiteKey() ? SUMMARY : MISSING_KEY);
        mDescription.setWrapText(true);
        mDescription.setPrefWidth(480);

        Button info = new Button("How Core works");
        info.setOnAction(event -> showDiagram(info));

        Button upload = new Button("Switch to Thinline Radio");
        upload.setTooltip(new Tooltip("Upload each finished call immediately with an upload API key"));
        upload.setOnAction(event -> switchTo(BroadcastServerType.THINLINE_RADIO, upload));

        HBox actions = new HBox(8, info, upload);
        actions.setAlignment(Pos.CENTER_LEFT);
        return new VBox(8, mDescription, actions);
    }

    private void showDiagram(Node owner)
    {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Thinline Radio Core");
        dialog.setHeaderText("One recording for each transmission");
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        if(owner.getScene() != null)
        {
            dialog.initOwner(owner.getScene().getWindow());
        }

        HBox pictures = new HBox(18);
        pictures.setAlignment(Pos.CENTER);
        pictures.setPadding(new Insets(8, 0, 12, 0));
        pictures.getChildren().addAll(siteBox("Site A"), arrow(), hubBox(), arrowBack(), siteBox("Site B"));

        VBox content = new VBox(10,
            pictures,
            paragraph("Each physical site uses its own site key. Another system on the same computer is another " +
                "Core stream with that same site key and a different system ID. Those streams share one connection."),
            paragraph("The site key is the one in User Preferences, under Thinline Radio. A Core stream does not " +
                "have its own key. An upload key is not used."),
            paragraph("When at least two sites are connected for the same system, ThinLine waits until each of " +
                "those sites has finished the talkgroup, then asks one site to upload. A site waits while another " +
                "connected site is still receiving that talkgroup."),
            paragraph("ThinLine keeps the longer recording when it is longer by more than half a second. When the " +
                "lengths match, it keeps one of them. A different radio ID stays a separate call. A patched call " +
                "matches when either side shares a talkgroup."),
            paragraph("Leave the regular Thinline Radio upload stream off for this system. To convert a stream you " +
                "already have, open it and choose Switch to Thinline Radio Core. The name, system ID, and URL stay. " +
                "Replace the key with the site key, then save the playlist."));
        content.setPadding(new Insets(4, 8, 4, 8));
        content.setPrefWidth(520);
        dialog.getDialogPane().setContent(content);
        dialog.setResizable(true);
        dialog.showAndWait();
    }

    private Label paragraph(String text)
    {
        Label label = themedLabel(text);
        label.setWrapText(true);
        label.setMaxWidth(500);
        return label;
    }

    private Label themedLabel(String text)
    {
        Label label = new Label(text);
        label.setStyle(THEME_TEXT);
        return label;
    }

    private VBox siteBox(String name)
    {
        Label title = themedLabel(name);
        Label detail = themedLabel("system + talkgroup");
        VBox box = new VBox(4, title, detail);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(10, 12, 10, 12));
        box.setStyle(SITE_BOX);
        return box;
    }

    private VBox hubBox()
    {
        Label title = themedLabel("ThinLine");
        Label detail = themedLabel("picks one");
        VBox box = new VBox(4, title, detail);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(10, 12, 10, 12));
        box.setStyle(HUB_BOX);
        return box;
    }

    private Polygon arrow()
    {
        Polygon arrow = new Polygon(0, 6, 28, 6, 28, 0, 42, 10, 28, 20, 28, 14, 0, 14);
        arrow.setStyle("-fx-fill: -fx-accent;");
        return arrow;
    }

    private Polygon arrowBack()
    {
        Polygon arrow = new Polygon(42, 6, 14, 6, 14, 0, 0, 10, 14, 20, 14, 14, 42, 14);
        arrow.setStyle("-fx-fill: -fx-accent;");
        return arrow;
    }
}
