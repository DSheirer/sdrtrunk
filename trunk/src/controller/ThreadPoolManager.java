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

public class ThreadPoolManager
{
	private int THREAD_POOL_SIZE = 10;
	private int MAX_TASK_COUNT = 24;

	private HashMap<ScheduledFuture<?>,ThreadType> mTasks = 
				new HashMap<ScheduledFuture<?>,ThreadType>();
	
	private ScheduledExecutorService mExecutor;
	
	public ThreadPoolManager()
	{
	}

	public ScheduledFuture<?> schedule( ThreadType type, 
										Runnable command, 
										long period, 
										TimeUnit unit )
										throws RejectedExecutionException
	{
		if( mExecutor == null )
		{
			mExecutor = Executors.newScheduledThreadPool( THREAD_POOL_SIZE );
		}

		if( mTasks.size() <= MAX_TASK_COUNT )
		{
			ScheduledFuture<?> task = 
					mExecutor.scheduleAtFixedRate( command, 0, period, unit );
			
			mTasks.put( task, type );
			
			return task;
		}
		else
		{
			throw new RejectedExecutionException( "Cannot schedule task - at "
					+ "max task count [" + mTasks.size() + "]" );
		}
		
	}
	
	public void cancel( ScheduledFuture<?> task )
	{
		task.cancel( true );
		
		ThreadType type = mTasks.remove( task );
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
		SOURCE_SAMPLE_PROCESSING,
		DECIMATION,
		DECODER;
	}
}
