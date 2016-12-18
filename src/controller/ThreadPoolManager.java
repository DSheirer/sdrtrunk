/*******************************************************************************
 *     SDR Trunk 
 *     Copyright (C) 2014 Dennis Sheirer
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>
 ******************************************************************************/
package controller;

import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThreadPoolManager
{
	private final static Logger mLog = LoggerFactory.getLogger( ThreadPoolManager.class );

	private int THREAD_POOL_SIZE = 2;

	private HashMap<ScheduledFuture<?>,ThreadType> mTasks = 
				new HashMap<ScheduledFuture<?>,ThreadType>();
	
	private ScheduledExecutorService mExecutor;
	
	public ThreadPoolManager()
	{
	}
	
	public ScheduledExecutorService getScheduledExecutorService()
	{
		if( mExecutor == null )
		{
			mExecutor = Executors.newScheduledThreadPool( THREAD_POOL_SIZE,
					new NamingThreadFactory( "sdrtrunk" ) );
		}

		return mExecutor;
	}

	public ScheduledFuture<?> scheduleFixedRate( ThreadType type, 
										Runnable command, 
										long period, 
										TimeUnit unit )
										throws RejectedExecutionException
	{
		ScheduledExecutorService executorService = getScheduledExecutorService();

		ScheduledFuture<?> task = 
				executorService.scheduleAtFixedRate( command, 0, period, unit );
		
		mTasks.put( task, type );
		
		return task;
	}
	
	public void scheduleOnce( Runnable command, long delay, TimeUnit unit )	
			throws RejectedExecutionException
	{
		ScheduledExecutorService executorService = getScheduledExecutorService();
		
		executorService.schedule( command, delay, unit );
	}

	public boolean cancel( ScheduledFuture<?> task )
	{
		boolean success = task.cancel( true );
		
		mTasks.remove( task );
		
		return success;
	}
	
	public int getTaskCount( ThreadType type )
	{
		int count = 0;
		
		for( ThreadType current: mTasks.values() )
		{
			if( current == type )
			{
				count++;
			}
		}
		
		return count;
	}

	public enum ThreadType
	{
		AUDIO_PROCESSING,
		AUDIO_RECORDING,
		BASEBAND_RECORDING,
		SOURCE_SAMPLE_PROCESSING,
		DECIMATION,
		DECODER;
	}
}
