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
package io.github.dsheirer.gui.preference.network;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.preference.network.ImbeStreamPreference;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.controlsfx.control.ToggleSwitch;

/**
 * Preference editor for the raw IMBE Audio Stream output feature.
 */
public class ImbeStreamPreferenceEditor extends HBox
{
    private final ImbeStreamPreference mPreference;

    public ImbeStreamPreferenceEditor(UserPreferences userPreferences)
    {
        mPreference = userPreferences.getImbeStreamPreference();
        setMaxWidth(Double.MAX_VALUE);

        ScrollPane scroll = new ScrollPane(buildContent());
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: transparent;");
        HBox.setHgrow(scroll, Priority.ALWAYS);
        getChildren().add(scroll);
    }

    private VBox buildContent()
    {
        VBox root = new VBox(14);
        root.setPadding(new Insets(18, 20, 18, 20));
        root.setMaxWidth(Double.MAX_VALUE);

        // ── Title ──────────────────────────────────────────────────────────
        Label title = new Label("IMBE Audio Stream");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

        Label subtitle = new Label(
            "Stream raw compressed voice frames from every active P25 Phase 1 voice channel " +
            "over TCP in real-time.  A separate application (on this machine or anywhere on " +
            "your LAN) connects to this port and decodes the frames into live audio using the " +
            "same open-source JMBE library that SDRTrunk uses internally.\n\n" +
            "This bypasses SDRTrunk's MP3 recording pipeline entirely, so audio is available " +
            "within ~20 ms of the radio transmitting — before any file is written to disk.");
        subtitle.setWrapText(true);
        subtitle.setStyle("-fx-text-fill: #555555;");

        root.getChildren().addAll(title, subtitle, new Separator());

        // ── Enable / port ─────────────────────────────────────────────────
        ToggleSwitch enableToggle = new ToggleSwitch();
        enableToggle.setSelected(mPreference.isEnabled());
        enableToggle.selectedProperty().addListener((obs, oldVal, enabled) ->
                mPreference.setEnabled(enabled));

        HBox enableRow = new HBox(10, enableToggle, bold("Enable IMBE Audio Stream"));
        enableRow.setAlignment(Pos.CENTER_LEFT);

        Label restartNote = new Label("⚠  Changes take effect after restarting SDRTrunk.");
        restartNote.setStyle("-fx-text-fill: #996600; -fx-font-size: 11px;");

        root.getChildren().addAll(enableRow, restartNote);

        // Port row
        Label portLabel = new Label("TCP port:");
        portLabel.setMinWidth(90);
        Spinner<Integer> portSpinner = new Spinner<>();
        portSpinner.setValueFactory(
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1024, 65535, mPreference.getPort()));
        portSpinner.setEditable(true);
        portSpinner.setPrefWidth(100);
        portSpinner.valueProperty().addListener((obs, oldVal, newVal) ->
        {
            if (newVal != null) mPreference.setPort(newVal);
        });
        HBox portRow = new HBox(10, portLabel, portSpinner);
        portRow.setAlignment(Pos.CENTER_LEFT);

        root.getChildren().addAll(portRow, new Separator());

        // ── What is an IMBE frame? ─────────────────────────────────────────
        root.getChildren().add(sectionHeader("WHAT IS AN IMBE FRAME?"));
        Label imbeDesc = new Label(
            "P25 Phase 1 voice is compressed using IMBE (Improved Multi-Band Excitation), a " +
            "vocoder codec.  The radio transmits groups of 9 compressed frames called an LDU " +
            "(Link Data Unit).  Each frame is 18 bytes (144 bits) and represents exactly 20 ms " +
            "of audio at 8 kHz.\n\n" +
            "SDRTrunk normally feeds each frame into the JMBE library, which decompresses it " +
            "into 160 PCM float samples, accumulates the samples into an AudioSegment, and " +
            "writes an MP3 file when the call ends.  This stream hands you the 18-byte frames " +
            "directly so your application can decode them in real-time using JMBE — or any " +
            "other IMBE-compatible codec — and play the audio immediately.");
        imbeDesc.setWrapText(true);
        root.getChildren().add(imbeDesc);
        root.getChildren().add(new Separator());

        // ── Message format ─────────────────────────────────────────────────
        root.getChildren().add(sectionHeader("MESSAGE FORMAT  ·  Newline-delimited JSON (NDJSON)"));
        Label fmtDesc = new Label(
            "Three message types flow on this port.  Each is a single JSON object terminated " +
            "by a newline (\\n).  Use the \"type\" field to distinguish them:");
        fmtDesc.setWrapText(true);
        root.getChildren().add(fmtDesc);

        String formatExample =
            "// 1. Call started — emitted once when squelch opens\n" +
            "{\n" +
            "  \"type\":      \"call_start\",\n" +
            "  \"callId\":    \"a3f9b2\",       // unique ID for this transmission\n" +
            "  \"system\":    \"MY_SYSTEM\",\n" +
            "  \"talkgroup\": \"1001\",\n" +
            "  \"from\":      \"1001234\",      // source radio unit ID\n" +
            "  \"timestamp\": \"2026-05-20 14:23:01\"\n" +
            "}\n\n" +
            "// 2. IMBE audio frame — 9 per LDU, one every ~20 ms per active channel\n" +
            "{\n" +
            "  \"type\":      \"frame\",\n" +
            "  \"callId\":    \"a3f9b2\",       // matches the call_start above\n" +
            "  \"system\":    \"MY_SYSTEM\",\n" +
            "  \"talkgroup\": \"1001\",\n" +
            "  \"from\":      \"1001234\",\n" +
            "  \"seq\":       4,               // frame counter within this call (0-based)\n" +
            "  \"imbe\":      \"Qx7f2mAbT...\"  // 18 bytes, Base64-encoded\n" +
            "}\n\n" +
            "// 3. Call ended — emitted once when squelch closes\n" +
            "{\n" +
            "  \"type\":      \"call_end\",\n" +
            "  \"callId\":    \"a3f9b2\",\n" +
            "  \"system\":    \"MY_SYSTEM\",\n" +
            "  \"talkgroup\": \"1001\",\n" +
            "  \"frames\":    47              // total frames sent for this call\n" +
            "}";

        root.getChildren().add(codeBox(formatExample, 380));
        root.getChildren().add(new Separator());

        // ── Decoding with JMBE ─────────────────────────────────────────────
        root.getChildren().add(sectionHeader("DECODING WITH JMBE  ·  Java"));
        Label jmbeDesc = new Label(
            "JMBE is open-source (Apache 2.0) and available at github.com/DSheirer/jmbe.  " +
            "Add the jmbe-1.0.9.jar to your project.  One codec instance must be maintained " +
            "per active call — do NOT reset() between frames within the same call, only at " +
            "call_end.  The codec preserves vocoder state across frames; resetting mid-call " +
            "causes audio artifacts.\n\n" +
            "getAudio() returns 160 float samples at 8 kHz (20 ms of audio).  Feed them " +
            "directly to javax.sound.sampled for playback, or encode with a library like " +
            "LAME or FFmpeg for MP3/Opus output.");
        jmbeDesc.setWrapText(true);
        root.getChildren().add(jmbeDesc);

        String javaCode =
            "import jmbe.iface.*;\nimport java.util.Base64;\n\n" +
            "// One-time setup — load the library\n" +
            "IAudioCodecLibrary lib = new jmbe.JMBEAudioLibrary();\n" +
            "Map<String, IAudioCodec> codecs = new ConcurrentHashMap<>();\n\n" +
            "// On call_start: create a codec instance for this callId\n" +
            "codecs.put(callId, lib.getAudioConverter(\"IMBE\"));\n\n" +
            "// On each frame message:\n" +
            "IAudioCodec codec = codecs.get(callId);\n" +
            "byte[] imbeBytes = Base64.getDecoder().decode(msg.get(\"imbe\").getAsString());\n" +
            "float[] pcm = codec.getAudio(imbeBytes);  // 160 samples @ 8 kHz\n" +
            "// → write pcm to a javax.sound.sampled.SourceDataLine for live playback\n\n" +
            "// On call_end: flush and release\n" +
            "IAudioCodec done = codecs.remove(callId);\n" +
            "if (done != null) done.reset();";

        root.getChildren().add(codeBox(javaCode, 220));
        root.getChildren().add(new Separator());

        // ── Quick start Python ─────────────────────────────────────────────
        root.getChildren().add(sectionHeader("QUICK START  ·  Inspect the stream with Python"));
        Label pyDesc = new Label(
            "This snippet connects and prints every message.  Replace the IP address with the " +
            "address of the machine running SDRTrunk:");
        pyDesc.setWrapText(true);
        root.getChildren().add(pyDesc);

        String pythonCode =
            "import socket, json\n\n" +
            "SDRTRUNK_HOST = '192.168.1.100'   # <- your SDRTrunk machine\n" +
            "IMBE_PORT     = 9502\n\n" +
            "s = socket.create_connection((SDRTRUNK_HOST, IMBE_PORT))\n" +
            "print(f'Connected to IMBE stream on {SDRTRUNK_HOST}:{IMBE_PORT}')\n\n" +
            "for raw_line in s.makefile():\n" +
            "    msg = json.loads(raw_line)\n" +
            "    t = msg['type']\n\n" +
            "    if t == 'call_start':\n" +
            "        print(f'[START] TG {msg[\"talkgroup\"]} | FROM {msg[\"from\"]} | call {msg[\"callId\"]}')\n" +
            "    elif t == 'frame':\n" +
            "        import base64\n" +
            "        raw = base64.b64decode(msg['imbe'])   # 18 bytes — feed to IMBE decoder\n" +
            "        print(f'  frame #{msg[\"seq\"]:04d}  {raw.hex()}')\n" +
            "    elif t == 'call_end':\n" +
            "        print(f'[END]   TG {msg[\"talkgroup\"]} | {msg[\"frames\"]} frames | call {msg[\"callId\"]}')";

        root.getChildren().add(codeBox(pythonCode, 255));

        Label tip = new Label(
            "💡  Multiple clients can connect simultaneously — each receives its own copy of " +
            "the full stream.  Filter client-side by \"talkgroup\" or \"system\" to isolate " +
            "specific channels.  The \"seq\" field increments per call (not globally), so a " +
            "gap in seq numbers within the same callId indicates lost frames.");
        tip.setWrapText(true);
        tip.setStyle("-fx-font-size: 11px; -fx-background-color: #f0f4f0; " +
                     "-fx-padding: 8px; -fx-background-radius: 4px;");
        root.getChildren().add(tip);

        return root;
    }

    private Label sectionHeader(String text)
    {
        Label l = new Label(text);
        l.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1a5276;");
        return l;
    }

    private Label bold(String text)
    {
        Label l = new Label(text);
        l.setStyle("-fx-font-weight: bold;");
        return l;
    }

    private TextArea codeBox(String code, double prefHeight)
    {
        TextArea ta = new TextArea(code);
        ta.setEditable(false);
        ta.setWrapText(false);
        ta.setPrefHeight(prefHeight);
        ta.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 11px; " +
                    "-fx-control-inner-background: #1e2329; -fx-text-fill: #abb2bf; " +
                    "-fx-border-color: #444; -fx-border-radius: 4px; -fx-background-radius: 4px;");
        VBox.setVgrow(ta, Priority.NEVER);
        return ta;
    }
}
