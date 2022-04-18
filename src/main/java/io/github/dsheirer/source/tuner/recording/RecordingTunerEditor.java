/*
 * *****************************************************************************
 * Copyright (C) 2014-2022 Dennis Sheirer
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
package io.github.dsheirer.source.tuner.recording;

import io.github.dsheirer.preference.UserPreferences;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.source.tuner.ui.TunerEditor;
import net.miginfocom.swing.MigLayout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JLabel;

/**
 * Recording tuner configuration editor
 */
public class RecordingTunerEditor extends TunerEditor<RecordingTuner,RecordingTunerConfiguration>
{
    private static final long serialVersionUID = 1L;
    private final static Logger mLog = LoggerFactory.getLogger(RecordingTunerEditor.class);
    private JLabel mRecordingPath;
    private boolean mLoading;

    private RecordingTunerController mController;

    /**
     * Constructs an instance
     * @param userPreferences for wide-band recordings
     * @param tunerManager for saving configurations
     * @param discoveredTuner for this configuration
     */
    public RecordingTunerEditor(UserPreferences userPreferences, TunerManager tunerManager, DiscoveredTuner discoveredTuner)
    {
        super(userPreferences, tunerManager, discoveredTuner);
//        mController = discoveredTuner.getTunerController();
//        init();
    }

    @Override
    public void setTunerLockState(boolean locked)
    {
        super.setTunerLockState(locked);
    }

    @Override
    protected void tunerStatusUpdated()
    {

    }

    private void init()
    {
        setLayout(new MigLayout("fill,wrap 4", "[right][grow,fill]",
            "[][][grow]"));
        add(new JLabel("Recording Tuner Configuration"), "span,align center");
        add(new JLabel("File:"));
        mRecordingPath = new JLabel();
        add(mRecordingPath);
    }

//    @Override
//    public void setItem(TunerConfiguration tunerConfiguration)
//    {
//        super.setItem(tunerConfiguration);
//
//        //Toggle loading so that we don't fire a change event and schedule a settings file save
//        mLoading = true;
//
//        if(hasItem())
//        {
//            RecordingTunerConfiguration config = getConfiguration();
//
//            mRecordingPath.setText(config.getPath());
//        }
//        else
//        {
//            mRecordingPath.setText("");
//        }
//
//        mLoading = false;
//    }

    @Override
    public void save()
    {
        if(hasConfiguration() && !mLoading)
        {
            RecordingTunerConfiguration config = getConfiguration();
            String path = mRecordingPath.getText();

            if(path != null && !path.isEmpty())
            {
                config.setPath(path);

                try
                {
                    mController.apply(config);
                }
                catch(SourceException se)
                {
                    mLog.error("Error while applying recording tuner configuration", se);
                }
            }

            saveConfiguration();
        }
    }
}