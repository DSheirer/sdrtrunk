package ua.in.smartjava.alias.action.script;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import ua.in.smartjava.message.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ua.in.smartjava.alias.Alias;
import ua.in.smartjava.alias.action.AliasActionType;
import ua.in.smartjava.alias.action.RecurringAction;

public class ScriptAction extends RecurringAction
{
	private final static Logger mLog = LoggerFactory.getLogger( ScriptAction.class );

	private String mScript;

	public ScriptAction()
	{
	}
	
	@Override
	public AliasActionType getType()
	{
		return AliasActionType.SCRIPT;
	}

	public String getScript()
	{
		return mScript;
	}
	
	public void setScript( String script )
	{
		mScript = script;
	}
	
	@Override
	public void performAction( Alias alias, Message message )
	{
		try
		{
			play();
		}
		catch( Exception e )
		{
			mLog.error( "Couldn't execute script [" + mScript + "]", e );
		}
	}
	
	public void play() throws Exception
	{
		if( mScript != null )
		{
			ProcessBuilder pb = new ProcessBuilder( mScript );
			
			pb.redirectErrorStream( true );

			Process p = pb.start();
			
			int exitCode = p.waitFor();
			
			BufferedReader reader = new BufferedReader( 
					new InputStreamReader( p.getInputStream() ) );

            String line = "";			

            StringBuilder sb = new StringBuilder();
            
            while( ( line = reader.readLine() ) != null ) 
            {
            	sb.append(line + "\n");
            }
            
            if( exitCode != 0 )
            {
            	throw new RuntimeException( "Exit Code: " + exitCode + 
            			" Console:" + sb.toString() );
            }
		}
	}
	
	@Override
	public String toString()
	{
		if( mScript == null )
		{
			return "Run Script";
		}
		else
		{
			return "Run Script: " + mScript;
		}
	}
}
