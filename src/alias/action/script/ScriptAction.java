package alias.action.script;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import message.Message;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import alias.Alias;
import alias.action.RecurringAction;

public class ScriptAction extends RecurringAction
{
	private final static Logger mLog = LoggerFactory.getLogger( ScriptAction.class );

	private String mScript;

	public ScriptAction()
	{
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
			mLog.debug( "Couldn't execute script [" + mScript + "]", e );
		}
	}
	
	public void play() throws Exception
	{
		if( mScript != null )
		{
			mLog.debug( "Running Script ..." );
			
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
}
