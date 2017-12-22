package io.github.dsheirer.alias.action.script;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dsheirer.alias.Alias;
import io.github.dsheirer.alias.action.AliasActionType;
import io.github.dsheirer.alias.action.RecurringAction;
import io.github.dsheirer.message.Message;
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
    }

    @Override
    public void performAction(Alias alias, Message message)
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

            BufferedReader reader = new BufferedReader(
                new InputStreamReader(p.getInputStream()));

            String line = "";

            StringBuilder sb = new StringBuilder();

            while((line = reader.readLine()) != null)
            {
                sb.append(line + "\n");
            }

            if(exitCode != 0)
            {
                throw new RuntimeException("Exit Code: " + exitCode +
                    " Console:" + sb.toString());
            }
        }
    }

    @Override
    public String toString()
    {
        if(mScript == null)
        {
            return "Run Script";
        }
        else
        {
            return "Run Script: " + mScript;
        }
    }
}
