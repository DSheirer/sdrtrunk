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

package io.github.dsheirer.gui.status;

import io.github.dsheirer.settings.Setting;
import io.github.dsheirer.source.tuner.manager.DiscoveredTuner;
import io.github.dsheirer.source.tuner.manager.TunerStatus;
import io.github.dsheirer.source.tuner.ui.DiscoveredTunerModel;
import java.util.LinkedHashMap;
import java.util.Map;
import javafx.application.Platform;

/**
 * Tuner status: tuners in use (with channels) out of the usable tuners, with an error dot when a tuner has an error
 * or was unplugged while sdrtrunk was using it.  A click opens the Tuners tab and clears the unplugged tuners.
 */
public class TunerStatusItem extends ServiceStatusItem implements DiscoveredTunerModel.ModelListener
{
    private final DiscoveredTunerModel mDiscoveredTunerModel;
    //Tuner ID to name, for tuners removed from the system while sdrtrunk ran.  JavaFX thread only.
    private final Map<String,String> mRemovedTuners = new LinkedHashMap<>();
    private Runnable mOpenTunersAction;

    /**
     * Constructs an instance.  JavaFX thread only.
     */
    public TunerStatusItem(DiscoveredTunerModel discoveredTunerModel)
    {
        super("Tuners", Setting.MAIN_WINDOW_STATUS_ITEM_TUNERS);
        mDiscoveredTunerModel = discoveredTunerModel;
        mDiscoveredTunerModel.addModelListener(this);
        update();
    }

    /**
     * Sets the action that opens the Tuners tab.  A click also clears the unplugged tuners.
     */
    public void setOpenTunersAction(Runnable action)
    {
        mOpenTunersAction = action;
        setAction(() -> {
            mRemovedTuners.clear();
            update();

            if(mOpenTunersAction != null)
            {
                mOpenTunersAction.run();
            }
        });
        update();
    }

    @Override
    public void tunerAdded(DiscoveredTuner discoveredTuner)
    {
        String id = discoveredTuner.getId();
        //Updates in the same task, since a coalesced update could already be queued ahead of the change
        Platform.runLater(() -> {
            mRemovedTuners.remove(id);
            update();
        });
    }

    @Override
    public void tunerRemoved(DiscoveredTuner discoveredTuner)
    {
        //The model also removes tuners at shutdown, so only an unplugged tuner counts
        if(discoveredTuner.getTunerStatus() == TunerStatus.REMOVED)
        {
            String id = discoveredTuner.getId();
            String name = getName(discoveredTuner);
            Platform.runLater(() -> {
                mRemovedTuners.put(id, name);
                update();
            });
        }
        else
        {
            scheduleUpdate();
        }
    }

    @Override
    public void tunerUpdated(DiscoveredTuner discoveredTuner)
    {
        scheduleUpdate();
    }

    @Override
    protected void update()
    {
        int usable = 0;
        int inUse = 0;
        int errors = 0;
        StringBuilder details = new StringBuilder();

        for(DiscoveredTuner tuner: mDiscoveredTunerModel.getDiscoveredTuners())
        {
            TunerStatus status = tuner.getTunerStatus();
            details.append("\n").append(getName(tuner)).append(": ").append(status);

            if(status == TunerStatus.ENABLED)
            {
                usable++;
                int channels = getChannelCount(tuner);

                if(channels > 0)
                {
                    inUse++;
                    details.append(", ").append(channels).append(channels == 1 ? " channel" : " channels");
                }
            }
            else if(status == TunerStatus.ERROR)
            {
                errors++;

                if(tuner.hasErrorMessage())
                {
                    details.append(" - ").append(tuner.getErrorMessage());
                }
            }
        }

        for(String name: mRemovedTuners.values())
        {
            details.append("\n").append(name).append(": unplugged while in use");
        }

        StatusSeverity severity;
        String text;
        String summary;

        if(usable == 0 && errors == 0 && mRemovedTuners.isEmpty())
        {
            severity = StatusSeverity.NEUTRAL;
            text = "No tuners";
            summary = "No usable tuners.  Connect a tuner, or enable one in the Tuners tab.";
        }
        else
        {
            severity = errors > 0 || !mRemovedTuners.isEmpty() ? StatusSeverity.ERROR :
                usable > 0 ? StatusSeverity.OK : StatusSeverity.NEUTRAL;
            text = "Tuners " + inUse + "/" + usable;
            summary = inUse + " of " + usable + " usable tuners in use";

            if(errors > 0)
            {
                summary += ", " + errors + (errors == 1 ? " tuner" : " tuners") + " with an error";
            }

            if(!mRemovedTuners.isEmpty())
            {
                summary += ", " + mRemovedTuners.size() + " unplugged";
            }

            summary += ".";
        }

        String hint = mOpenTunersAction == null ? "" : !mRemovedTuners.isEmpty() ?
            "\n\nClick to open the Tuners tab and clear the unplugged tuners." : "\n\nClick to open the Tuners tab.";
        show(severity, text, summary + (details.length() > 0 ? "\n" + details : "") + hint);
    }

    private static int getChannelCount(DiscoveredTuner tuner)
    {
        try
        {
            return tuner.hasTuner() ? tuner.getTuner().getChannelSourceManager().getTunerChannelCount() : 0;
        }
        catch(Exception e)
        {
            return 0;
        }
    }

    private static String getName(DiscoveredTuner tuner)
    {
        if(tuner.hasTuner())
        {
            try
            {
                return tuner.getTuner().getPreferredName();
            }
            catch(Exception e)
            {
                //Fall through to the tuner class
            }
        }

        return tuner.getTunerClass().toString();
    }

    @Override
    public void dispose()
    {
        mDiscoveredTunerModel.removeModelListener(this);
    }
}
