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
package io.github.dsheirer.gui.table;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.Pane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests placing the column editor window next to the Columns button.
 */
public class ColumnConfigurationEditorTest
{
    @BeforeAll
    public static void startToolkit() throws Exception
    {
        //Run without a display
        System.setProperty("glass.platform", "Headless");
        System.setProperty("prism.order", "sw");
        CompletableFuture<Void> started = new CompletableFuture<>();

        try
        {
            Platform.startup(() -> started.complete(null));
        }
        catch(IllegalStateException e)
        {
            started.complete(null); //Already started
        }

        started.get(10, TimeUnit.SECONDS);
        //Keep the toolkit running for other tests after this test closes its windows
        Platform.setImplicitExit(false);
    }

    @Test
    public void editorStaysOnScreenForButtonAtBottomRight() throws Exception
    {
        Preferences preferences = Preferences.userRoot().node("sdrtrunk-test/" + UUID.randomUUID());
        CompletableFuture<Boolean> onScreen = new CompletableFuture<>();

        Platform.runLater(() -> {
            Stage owner = new Stage();

            try
            {
                TableView<String> table = new TableView<>();
                TableColumn<String,String> column = new TableColumn<>("Column");
                column.setId("column");
                table.getColumns().add(column);
                TableColumnMonitor<String> monitor = new TableColumnMonitor<>(table, "test.table", preferences,
                    null, null);

                Rectangle2D screen = Screen.getPrimary().getVisualBounds();
                Button button = new Button("Columns");
                button.relocate(screen.getWidth() - 100, screen.getHeight() - 40);
                owner.setScene(new Scene(new Pane(button), screen.getWidth(), screen.getHeight()));
                owner.setX(screen.getMinX());
                owner.setY(screen.getMinY());
                owner.show();

                new ColumnConfigurationEditor(monitor, "Test Columns").show(button);
                Stage editor = (Stage)Window.getWindows().stream()
                    .filter(window -> window instanceof Stage stage && "Test Columns".equals(stage.getTitle()))
                    .findFirst().orElseThrow();
                onScreen.complete(screen.contains(editor.getX(), editor.getY(), editor.getWidth(),
                    editor.getHeight()));
                editor.hide();
            }
            catch(Throwable t)
            {
                onScreen.completeExceptionally(t);
            }
            finally
            {
                owner.hide();
            }
        });

        try
        {
            assertTrue(onScreen.get(10, TimeUnit.SECONDS));
        }
        finally
        {
            preferences.removeNode();
        }
    }
}
