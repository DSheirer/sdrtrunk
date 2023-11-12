/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.monitor;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;

/**
 * JavaFX status panel box.
 */
public class StatusBox extends HBox
{
    private ResourceMonitor mResourceMonitor;

    /**
     * Constructs an instance.
     * @param resourceMonitor for accessing resource usage statistics.
     */
    public StatusBox(ResourceMonitor resourceMonitor)
    {
        mResourceMonitor = resourceMonitor;
        setPadding(new Insets(0, 0, 2, 0));
        setSpacing(6);
        Label cpuLabel = new Label("CPU:");
        cpuLabel.setPadding(new Insets(0, 0, 0, 10));
        cpuLabel.setAlignment(Pos.CENTER_RIGHT);
        getChildren().add(cpuLabel);

        ProgressBar cpuIndicator = new ProgressBar();
        cpuIndicator.progressProperty().bind(mResourceMonitor.cpuPercentageProperty());
        getChildren().add(cpuIndicator);

        Label memoryLabel = new Label("Allocated Memory:");
        memoryLabel.setAlignment(Pos.CENTER_RIGHT);
        getChildren().add(memoryLabel);

        ProgressBar memoryBar = new ProgressBar();
        memoryBar.progressProperty().bind(mResourceMonitor.systemMemoryUsedPercentageProperty());
        getChildren().add(memoryBar);

        Label javaMemoryLabel = new Label("Used Memory:");
        javaMemoryLabel.setAlignment(Pos.CENTER_RIGHT);
        getChildren().add(javaMemoryLabel);

        ProgressBar javaMemoryBar = new ProgressBar();
        javaMemoryBar.progressProperty().bind(mResourceMonitor.javaMemoryUsedPercentageProperty());
        getChildren().add(javaMemoryBar);

        Label eventLogsLabel = new Label("Event Logs:");
        eventLogsLabel.setAlignment(Pos.CENTER_RIGHT);
        getChildren().add(eventLogsLabel);

        ProgressBar eventLogsBar = new ProgressBar();
        eventLogsBar.progressProperty().bind(mResourceMonitor.directoryUsePercentEventLogsProperty());
        getChildren().add(eventLogsBar);

        Label eventLogsSizeLabel = new Label();
        eventLogsSizeLabel.textProperty().bind(mResourceMonitor.fileSizeEventLogsProperty());
        eventLogsSizeLabel.setAlignment(Pos.CENTER_RIGHT);
        getChildren().add(eventLogsSizeLabel);

        Label recordingsLabel = new Label("Recordings:");
        recordingsLabel.setPadding(new Insets(0, 0, 0, 10));
        recordingsLabel.setAlignment(Pos.CENTER_RIGHT);
        getChildren().add(recordingsLabel);

        ProgressBar recordingsBar = new ProgressBar();
        recordingsBar.progressProperty().bind(mResourceMonitor.directoryUsePercentRecordingsProperty());
        getChildren().add(recordingsBar);

        Label recordingsSizeLabel = new Label();
        recordingsSizeLabel.textProperty().bind(mResourceMonitor.fileSizeRecordingsProperty());
        recordingsSizeLabel.setAlignment(Pos.CENTER_RIGHT);
        getChildren().add(recordingsSizeLabel);
    }
}
