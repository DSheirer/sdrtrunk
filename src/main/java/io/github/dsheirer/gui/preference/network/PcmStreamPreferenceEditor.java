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
import io.github.dsheirer.preference.network.PcmStreamPreference;
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
 * Preference editor for the decoded PCM Audio Stream output feature (port 9503).
 */
public class PcmStreamPreferenceEditor extends HBox
{
    private final PcmStreamPreference mPreference;

    public PcmStreamPreferenceEditor(UserPreferences userPreferences)
    {
        mPreference = userPreferences.getPcmStreamPreference();
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
        Label title = new Label("PCM Audio Stream");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

        Label subtitle = new Label(
            "Stream decoded 16-bit PCM audio from every active voice channel over TCP in " +
            "real-time.  A separate application (on this machine or anywhere on your LAN) " +
            "connects to this port and plays or processes the audio immediately.\n\n" +
            "Unlike the IMBE stream on port 9502, no JMBE library is needed on the client " +
            "— SDRTrunk has already decoded the audio.  Samples are 16-bit signed " +
            "little-endian at 8000 Hz mono, Base64-encoded in each JSON message.\n\n" +
            "This stream also emits a fast voice_id message the moment a radio unit ID is " +
            "decoded from the Link Control Word (~180 ms after squelch opens), well before " +
            "audio frames arrive.  This lets dispatch or alerting apps identify who is " +
            "talking almost instantly without waiting for the audio pipeline to start.\n\n" +
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

        HBox enableRow = new HBox(10, enableToggle, bold("Enable PCM Audio Stream"));
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

        // ── Message format ─────────────────────────────────────────────────
        root.getChildren().add(sectionHeader("MESSAGE FORMAT  ·  Newline-delimited JSON (NDJSON)"));
        Label fmtDesc = new Label(
            "Four message types flow on this port.  Each is a single JSON object terminated " +
            "by a newline (\\n).  Use the \"type\" field to distinguish them.  The \"system\" " +
            "and \"site\" fields reflect the names you configured in SDRTrunk for that channel " +
            "— filter client-side on these to isolate specific systems:");
        fmtDesc.setWrapText(true);
        root.getChildren().add(fmtDesc);

        String formatExample =
            "// 1. Fast unit ID -- fires ~180ms after squelch opens (before audio starts)\n" +
            "//    Repeats on every LDU1 throughout the call -- deduplicate by talkgroup+from\n" +
            "{\"type\":\"voice_id\",\"system\":\"MySystem\",\"site\":\"MySite\",\"talkgroup\":\"12345\",\"from\":\"1234567\",\"timestamp\":\"2026-01-01 12:00:00\"}\n\n" +
            "// 2. Call started -- emitted once when the audio pipeline opens (~360ms after squelch)\n" +
            "{\"type\":\"call_start\",\"callId\":\"a1b2c3d\",\"system\":\"MySystem\",\"site\":\"MySite\",\"talkgroup\":\"12345\",\"from\":\"1234567\",\"timestamp\":\"2026-01-01 12:00:00\"}\n\n" +
            "// 3. PCM audio chunk -- one per decoded audio buffer (~20 ms per active channel)\n" +
            "{\"type\":\"pcm\",\"callId\":\"a1b2c3d\",\"system\":\"MySystem\",\"site\":\"MySite\",\"talkgroup\":\"12345\",\"from\":\"1234567\",\"seq\":0,\"samples\":\"BASE64_ENCODED_PCM\"}\n" +
            "// samples = Base64-encoded 16-bit signed little-endian PCM at 8000 Hz mono\n\n" +
            "// 4. Call ended -- emitted once when squelch closes\n" +
            "{\"type\":\"call_end\",\"callId\":\"a1b2c3d\",\"system\":\"MySystem\",\"site\":\"MySite\",\"talkgroup\":\"12345\",\"frames\":90}";

        root.getChildren().add(codeBox(formatExample, 200));
        root.getChildren().add(new Separator());

        // ── Python playback example ────────────────────────────────────────
        root.getChildren().add(sectionHeader("QUICK START  ·  Playback with Python"));
        Label pyDesc = new Label(
            "This snippet connects and plays audio in real-time.  Requires numpy and " +
            "sounddevice (pip install numpy sounddevice).  Replace the IP address with the " +
            "address of the machine running SDRTrunk:");
        pyDesc.setWrapText(true);
        root.getChildren().add(pyDesc);

        String pythonCode =
            "import socket, json, base64, numpy as np, sounddevice as sd\n\n" +
            "SDRTRUNK_HOST = '192.168.1.100'   # <- your SDRTrunk machine\n" +
            "PCM_PORT      = 9503\n\n" +
            "s = socket.create_connection((SDRTRUNK_HOST, PCM_PORT))\n" +
            "print(f'Connected to PCM stream on {SDRTRUNK_HOST}:{PCM_PORT}')\n\n" +
            "for raw_line in s.makefile():\n" +
            "    msg = json.loads(raw_line)\n" +
            "    t = msg['type']\n\n" +
            "    if t == 'voice_id':\n" +
            "        # Fires ~180ms after squelch -- fastest possible unit ID\n" +
            "        print(f'[ID]    TG {msg[\"talkgroup\"]} | FROM {msg[\"from\"]} | {msg[\"system\"]} / {msg[\"site\"]}')\\n" +
            "    elif t == 'call_start':\n" +
            "        print(f'[START] TG {msg[\"talkgroup\"]} | FROM {msg[\"from\"]} | call {msg[\"callId\"]}')\n" +
            "    elif t == 'pcm':\n" +
            "        samples = np.frombuffer(base64.b64decode(msg['samples']), dtype='<i2').astype(np.float32) / 32767\n" +
            "        sd.play(samples, 8000)\n" +
            "    elif t == 'call_end':\n" +
            "        print(f'[END]   TG {msg[\"talkgroup\"]} | {msg[\"frames\"]} frames | call {msg[\"callId\"]}')";

        root.getChildren().add(codeBox(pythonCode, 300));
        root.getChildren().add(new Separator());

        // ── Audio format details ───────────────────────────────────────────
        root.getChildren().add(sectionHeader("AUDIO FORMAT DETAILS"));
        Label audioDesc = new Label(
            "The \"samples\" field in each pcm message contains Base64-encoded raw audio:\n\n" +
            "  • Encoding:    16-bit signed integer (int16)\n" +
            "  • Byte order:  little-endian\n" +
            "  • Sample rate: 8000 Hz\n" +
            "  • Channels:    mono\n\n" +
            "Conversion from the internal float representation uses:\n" +
            "    (short) Math.max(-32768, Math.min(32767, Math.round(sample * 32767f)))\n\n" +
            "To decode in Python:  np.frombuffer(base64.b64decode(msg['samples']), dtype='<i2')\n" +
            "To decode in Java:    ByteBuffer.wrap(Base64.getDecoder().decode(b64)).order(ByteOrder.LITTLE_ENDIAN)\n\n" +
            "Each chunk typically contains 160 samples (20 ms at 8 kHz), matching one IMBE " +
            "frame worth of audio, though the exact count may vary by decoder.");
        audioDesc.setWrapText(true);
        root.getChildren().add(audioDesc);

        Label tip = new Label(
            "💡  Multiple clients can connect simultaneously — each receives its own copy of " +
            "the full stream.  Filter client-side by \"system\", \"site\", or \"talkgroup\" to isolate " +
            "specific channels.  The \"seq\" field increments per call (not globally), so a " +
            "gap in seq numbers within the same callId indicates dropped chunks.");
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
