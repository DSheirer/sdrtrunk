package io.github.dsheirer.gui.instrument;

import com.sun.glass.ui.Application;
import io.github.dsheirer.dsp.filter.design.FilterViewer;

public class DemodVFXWrapper {
    public static void main(String[] args) {
        DemodulatorViewerFX.main(args);
        //FilterViewer.main(args);
    }
}
