package io.github.dsheirer.dsp.filter.design;

import io.github.dsheirer.gui.FxToolkit;
import javafx.application.Platform;
import javafx.stage.Stage;

public class FilterViewerLauncher
{
    public FilterViewerLauncher()
    {
        FxToolkit.ensureStarted();

        Platform.runLater(() ->
        {
            FilterViewer filterViewer = new FilterViewer();
            Stage stage = new Stage();

            try
            {
                filterViewer.start(stage);
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        });
    }

    public static void main(String[] args)
    {
        FilterViewerLauncher filterViewerLauncher = new FilterViewerLauncher();
    }
}
