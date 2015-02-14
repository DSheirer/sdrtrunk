package alias.action;

import java.awt.EventQueue;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JOptionPane;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;

import message.Message;
import alias.Alias;
import controller.ThreadPoolManager;
import controller.ThreadPoolManager.ThreadType;

public abstract class RecurringAction extends AliasAction
{
	@XmlTransient
	protected AtomicBoolean mRunning = new AtomicBoolean( false );
	@XmlTransient
	private ScheduledFuture<?> mPerpetualAction;
	
	protected Interval mInterval = Interval.ONCE;
	protected int mPeriod = 5;
	
	protected ThreadPoolManager mThreadPoolManager;
	
	public abstract void performAction( Alias alias, Message message );

	@Override
	public void execute( ThreadPoolManager threadPoolManager, 
						 Alias alias,
						 Message message )
	{
		mThreadPoolManager = threadPoolManager;

		if( mRunning.compareAndSet( false, true ) )
		{
			switch( mInterval )
			{
				case ONCE:
					performThreadedAction( alias, message );
					/* Don't reset */
					break;
				case DELAYED_RESET:
					performThreadedAction( alias, message );
					mThreadPoolManager.scheduleOnce( new ResetTask(), 
							mPeriod, TimeUnit.SECONDS );
					break;
				case UNTIL_DISMISSED:
					mPerpetualAction = mThreadPoolManager
						.scheduleFixedRate( ThreadType.DECODER, 
							new PerformActionTask( alias, message ), 
							mPeriod, TimeUnit.SECONDS );

					StringBuilder sb = new StringBuilder();
					sb.append( "<html><div width='250'>Alias [" );
					sb.append( alias.getName() );
					sb.append( "] is active in message [" );
					sb.append( message.toString() );
					sb.append( "]</div></html>" );
					
					final String text = sb.toString();
					
					EventQueue.invokeLater( new Runnable() 
					{
						@Override
						public void run()
						{
							JOptionPane.showMessageDialog( null, text, 
								"Alias Alert", JOptionPane.INFORMATION_MESSAGE );
							
							dismiss( false );
							
							mThreadPoolManager.scheduleOnce( new ResetTask(), 
									15, TimeUnit.SECONDS );
						}
					} );
					break;
				default:
			}
		}
	}

	/**
	 * Spawns the performAction() event into a new thread so that it doesn't
	 * delay any decoder actions.
	 */
	private void performThreadedAction( final Alias alias, final Message message )
	{
		mThreadPoolManager.scheduleOnce( new Runnable() {

			@Override
			public void run()
			{
				performAction( alias, message );
			}}, 0, TimeUnit.SECONDS );
	}
	
	@Override
	public void dismiss( boolean reset )
	{
		if( mPerpetualAction != null )
		{
			mPerpetualAction.cancel( true );
			
			mPerpetualAction = null;
		}
				
		if( reset )
		{
			mRunning.set( false );
		}
	}
	
	@XmlAttribute
	public int getPeriod()
	{
		return mPeriod;
	}
	
	public void setPeriod( int period )
	{
		mPeriod = period;
	}
	
	@XmlAttribute
	public Interval getInterval()
	{
		return mInterval;
	}
	
	public void setInterval( Interval interval )
	{
		mInterval = interval;
		
		mRunning.set( false );
	}
	
	public enum Interval
	{
		ONCE( "Once" ),
		DELAYED_RESET( "Once, Reset After Delay" ),
		UNTIL_DISMISSED( "Until Dismissed" );
		
		private String mLabel;
		
		private Interval( String label )
		{
			mLabel = label;
		}
		
		public String toString()
		{
			return mLabel;
		}
	}
	
	public class PerformActionTask implements Runnable
	{
		private Alias mAlias;
		private Message mMessage;

		public PerformActionTask( Alias alias, Message message )
		{
			mAlias = alias;
			mMessage = message;
		}
		
		@Override
		public void run()
		{
			/* Don't use the performThreadedAction() method, since this is
			 * already running in a separate thread */
			performAction( mAlias, mMessage );
		}
	}
	
	public class ResetTask implements Runnable
	{
		@Override
		public void run()
		{
			mRunning.set( false );
		}
	}
}
