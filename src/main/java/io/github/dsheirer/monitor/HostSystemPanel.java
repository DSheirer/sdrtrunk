/*
 * ****************************************************************************
 * Copyright (C) 2026 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * ****************************************************************************
 */
package io.github.dsheirer.monitor;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.Timer;

/** Desktop host resource monitor displayed beside the tuner and playlist tabs. */
public class HostSystemPanel extends JPanel
{
    private final JProgressBar mCpu = meter();
    private final JProgressBar mMemory = meter();
    private final JProgressBar mJvmMemory = meter();
    private final JLabel mDetails = new JLabel();

    public HostSystemPanel()
    {
        setLayout(new BorderLayout(18, 18));
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));
        JLabel title = new JLabel("Host System");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 20f));
        add(title, BorderLayout.NORTH);

        JPanel meters = new JPanel(new GridLayout(3, 1, 12, 12));
        meters.add(row("CPU", mCpu));
        meters.add(row("Host memory", mMemory));
        meters.add(row("sdrtrunk JVM memory", mJvmMemory));
        add(meters, BorderLayout.CENTER);
        add(mDetails, BorderLayout.SOUTH);

        refresh();
        Timer timer = new Timer(1000, event -> refresh());
        timer.start();
    }

    private JPanel row(String label, JProgressBar meter)
    {
        JPanel panel = new JPanel(new BorderLayout(12, 4));
        panel.add(new JLabel(label), BorderLayout.NORTH);
        panel.add(meter, BorderLayout.CENTER);
        return panel;
    }

    private static JProgressBar meter()
    {
        JProgressBar meter = new JProgressBar(0, 100);
        meter.setStringPainted(true);
        return meter;
    }

    private void refresh()
    {
        HostSystemMetrics.Snapshot snapshot = HostSystemMetrics.snapshot();
        setMeter(mCpu, snapshot.hasCpuLoad() ? snapshot.cpuLoad() : -1,
            snapshot.hasCpuLoad() ? String.format(Locale.US, "%.1f%%", snapshot.cpuLoad() * 100) : "Unavailable");
        setMeter(mMemory, snapshot.hasHostMemory() ? (double)snapshot.usedMemory() / snapshot.totalMemory() : -1,
            snapshot.hasHostMemory() ? bytes(snapshot.usedMemory()) + " / " + bytes(snapshot.totalMemory()) : "Unavailable");
        setMeter(mJvmMemory, snapshot.jvmMaximumMemory() > 0 ?
                (double)snapshot.jvmUsedMemory() / snapshot.jvmMaximumMemory() : -1,
            bytes(snapshot.jvmUsedMemory()) + " / " + bytes(snapshot.jvmMaximumMemory()));
        mDetails.setText(snapshot.operatingSystem() + "  •  " + snapshot.processors() + " logical processors  •  JVM uptime " +
            duration(snapshot.jvmUptimeMs()));
    }

    private static void setMeter(JProgressBar meter, double ratio, String label)
    {
        meter.setValue(ratio >= 0 ? (int)Math.round(Math.min(1, ratio) * 100) : 0);
        meter.setString(label);
    }

    private static String bytes(long value)
    {
        if(value < 0) { return "Unavailable"; }
        double gib = value / 1_073_741_824.0;
        return String.format(Locale.US, "%.1f GiB", gib);
    }

    private static String duration(long milliseconds)
    {
        long seconds = milliseconds / 1000;
        return String.format(Locale.US, "%dd %02d:%02d:%02d", seconds / 86400, (seconds / 3600) % 24,
            (seconds / 60) % 60, seconds % 60);
    }
}
