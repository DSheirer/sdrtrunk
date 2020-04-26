/*
 * ******************************************************************************
 * sdrtrunk
 * Copyright (C) 2014-2018 Dennis Sheirer
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
 * *****************************************************************************
 */

package io.github.dsheirer.alias.action.script;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.action.AliasActionType;
import io.github.dsheirer.alias.action.RecurringAction;
import io.github.dsheirer.message.IMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class ScriptAction extends RecurringAction
{
    private final static Logger mLog = LoggerFactory.getLogger(ScriptAction.class);

    private String mScript;

    public ScriptAction()
    {
        updateValueProperty();
    }

    @JacksonXmlProperty(isAttribute = true, localName = "type", namespace = "http://www.w3.org/2001/XMLSchema-instance")
    @Override
    public AliasActionType getType()
    {
        return AliasActionType.SCRIPT;
    }

    public String getScript()
    {
        return mScript;
    }

    public void setScript(String script)
    {
        mScript = script;
        updateValueProperty();
    }

    @Override
    public void performAction(Alias alias, IMessage message)
    {
        try
        {
            play();
        }
        catch(Exception e)
        {
            mLog.error("Couldn't execute script [" + mScript + "]", e);
        }
    }

    public void play() throws Exception
    {
        if(mScript != null)
        {
            ProcessBuilder pb = new ProcessBuilder(mScript);

            pb.redirectErrorStream(true);

            Process p = pb.start();

            int exitCode = p.waitFor();

            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

            String line = "";

            StringBuilder sb = new StringBuilder();

            while((line = reader.readLine()) != null)
            {
                sb.append(line + "\n");
            }

            if(exitCode != 0)
            {
                throw new RuntimeException("Exit Code: " + exitCode + " Console:" + sb.toString());
            }
        }
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Run Script");

        if(getInterval() != null)
        {
            switch(getInterval())
            {
                case ONCE:
                    sb.append(" Once");
                    break;
                case DELAYED_RESET:
                    sb.append(" Once, Reset After ").append(getPeriod()).append(" Seconds");
                    break;
                case UNTIL_DISMISSED:
                    sb.append(" Every ").append(getPeriod()).append(" Seconds Until Dismissed");
                    break;
            }
        }

        if(getScript() == null)
        {
            sb.append(" - (script file empty)");
        }

        return sb.toString();
    }
}
