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
import io.github.dsheirer.preference.network.NetworkStreamPreference;
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
 * Preference editor for the TCP Network Stream Output feature.
 */
public class NetworkStreamPreferenceEditor extends HBox
{
    private final NetworkStreamPreference mPreference;

    public NetworkStreamPreferenceEditor(UserPreferences userPreferences)
    {
        mPreference = userPreferences.getNetworkStreamPreference();
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

        Label title = new Label("Network Stream Output");
        title.setStyle("-fx-font-size: 15px; -fx-font-weight: bold;");

        Label subtitle = new Label(
            "Stream live SDRTrunk decode data over TCP to any application on your network. " +
            "Multiple clients can connect simultaneously.  This feature is independent of " +
            "CSV logging — both can be enabled together, or used on their own.");
        subtitle.setWrapText(true);
        subtitle.setStyle("-fx-text-fill: #555555;");

        root.getChildren().addAll(title, subtitle, new Separator());

        ToggleSwitch enableToggle = new ToggleSwitch();
        enableToggle.setSelected(mPreference.isEnabled());
        enableToggle.selectedProperty().addListener((obs, oldVal, enabled) ->
                mPreference.setEnabled(enabled));

        HBox enableRow = new HBox(10, enableToggle, bold("Enable Network Streaming"));
        enableRow.setAlignment(Pos.CENTER_LEFT);

        Label restartNote = new Label("⚠  Changes take effect after restarting SDRTrunk.");
        restartNote.setStyle("-fx-text-fill: #996600; -fx-font-size: 11px;");

        root.getChildren().addAll(enableRow, restartNote, new Separator());

        root.getChildren().add(sectionHeader("EVENT STREAM  ·  Port"));

        Label eventDesc = new Label(
            "Every call event from all monitored P25 Phase 1 and DMR channels — group calls, " +
            "individual calls, affiliations, registrations, data calls.  This is the same data " +
            "that appears in the SDRTrunk Events tab and the daily CSV event log files, delivered " +
            "in real-time as one JSON object per line.\n\n" +
            "Each message is tagged with the system name so a single subscriber can receive " +
            "all monitored systems on one connection and filter client-side by the \"system\" field.");
        eventDesc.setWrapText(true);

        root.getChildren().add(eventDesc);
        root.getChildren().add(portRow("Event port:", mPreference.getEventPort(),
                v -> mPreference.setEventPort(v)));

        root.getChildren().add(new Separator());

        root.getChildren().add(sectionHeader("RAW CONTROL-CHANNEL STREAM  ·  Port"));

        Label rawDesc = new Label(
            "Every valid decoded control-channel message, delivered before SDRTrunk processes " +
            "it into a higher-level event:\n\n" +
            "  • P25 Phase 1 — TSBKs (Trunked System Bursts) and AMBTCs such as " +
            "GROUP_VOICE_GRANT, RFSS_STATUS_BCST, ADJACENT_STATUS_BCST, etc.\n\n" +
            "  • DMR — CSBKs (Control Signaling Blocks) and Link Control messages such as " +
            "CSBK ALOHA, CSBK GRANT, LC GROUP VOICE CHANNEL USER, etc.  Voice audio frames " +
            "are filtered out — only signaling messages flow through.\n\n" +
            "This stream is finer-grained than the event stream.  Use it when you need to react " +
            "to specific message types, extract fields SDRTrunk doesn't surface in events, or " +
            "build your own monitoring logic on top of raw protocol data.");
        rawDesc.setWrapText(true);

        root.getChildren().add(rawDesc);
        root.getChildren().add(portRow("Raw port:", mPreference.getRawPort(),
                v -> mPreference.setRawPort(v)));

        root.getChildren().add(new Separator());

        root.getChildren().add(sectionHeader("MESSAGE FORMAT  ·  Newline-delimited JSON (NDJSON)"));

        Label formatDesc = new Label("One complete JSON object per line on both streams:");
        formatDesc.setWrapText(true);

        String eventExample =
            "// Event stream (port 9500)\n" +
            "{\n" +
            "  \"pipe\":       \"events\",\n" +
            "  \"system\":     \"MY_SYSTEM\",\n" +
            "  \"timestamp\":  \"2026-05-10 12:17:56\",\n" +
            "  \"durationMs\": 4200,\n" +
            "  \"protocol\":   \"APCO25\",\n" +
            "  \"event\":      \"GROUP_CALL\",\n" +
            "  \"from\":       \"1001234\",\n" +
            "  \"to\":         \"1001\",\n" +
            "  \"frequency\":  \"857.112500\",\n" +
            "  \"details\":    \"ENCRYPTED\",\n" +
            "  \"eventId\":    -1234567890\n" +
            "}\n\n" +
            "// Raw CC stream (port 9501) — P25\n" +
            "{\"pipe\":\"raw_cc\",\"system\":\"MY_SYSTEM\",\"timestamp\":\"2026-05-10 12:17:56\"," +
            "\"message\":\"TSBK OSP GROUP VOICE GRANT  LCN:5  TGP:1001  SU:1001234 ...\"}\n\n" +
            "// Raw CC stream (port 9501) — DMR\n" +
            "{\"pipe\":\"raw\",\"protocol\":\"DMR\",\"system\":\"MY_DMR_SYSTEM\",\"timeslot\":1," +
            "\"timestamp\":\"2026-05-10 12:17:56\",\"message\":\"CSBK ALOHA  ...\"}";

        TextArea formatBox = codeBox(eventExample, 220);

        root.getChildren().addAll(formatDesc, formatBox, new Separator());

        root.getChildren().add(sectionHeader("QUICK START  ·  Connect with Python"));

        Label quickDesc = new Label(
            "Run this on any machine that can reach the SDRTrunk host over the network.  " +
            "Replace the IP address with the IP of the machine running SDRTrunk:");
        quickDesc.setWrapText(true);

        String pythonCode =
            "import socket, json, threading\n\n" +
            "SDRTRUNK_HOST = '192.168.1.100'   # <- change to your SDRTrunk machine's IP\n\n" +
            "def tap(port, label):\n" +
            "    s = socket.create_connection((SDRTRUNK_HOST, port))\n" +
            "    print(f'[{label}] connected')\n" +
            "    for line in s.makefile():\n" +
            "        obj = json.loads(line)\n" +
            "        print(f'[{label}] {obj[\"system\"]} | {obj}')\n\n" +
            "# Tap both pipes at once\n" +
            "threading.Thread(target=tap, args=(9500, 'EVENTS'), daemon=True).start()\n" +
            "threading.Thread(target=tap, args=(9501, 'RAW CC'), daemon=True).start()\n\n" +
            "input('Press Enter to quit\\n')";

        TextArea pythonBox = codeBox(pythonCode, 230);

        Label testTip = new Label(
            "💡  You can also test with netcat:   nc 192.168.1.100 9500\n" +
            "    or Telnet:                        telnet 192.168.1.100 9500\n" +
            "    JSON objects will scroll by in real-time as activity is decoded.");
        testTip.setWrapText(true);
        testTip.setStyle("-fx-font-family: monospace; -fx-font-size: 11px; " +
                         "-fx-background-color: #f0f4f0; -fx-padding: 8px; " +
                         "-fx-background-radius: 4px;");

        root.getChildren().addAll(quickDesc, pythonBox, testTip);

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

    private HBox portRow(String labelText, int currentValue, PortSetter setter)
    {
        Label lbl = new Label(labelText);
        lbl.setMinWidth(90);

        Spinner<Integer> spinner = new Spinner<>();
        spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1024, 65535, currentValue));
        spinner.setEditable(true);
        spinner.setPrefWidth(100);
        spinner.valueProperty().addListener((obs, oldVal, newVal) ->
        {
            if(newVal != null) setter.set(newVal);
        });

        HBox row = new HBox(10, lbl, spinner);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
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

    @FunctionalInterface
    private interface PortSetter { void set(int port); }
}
