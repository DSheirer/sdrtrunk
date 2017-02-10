package controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.ThreadPool;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread factory that applies custom names to threads
 */
public class NamingThreadFactory implements ThreadFactory 
{
    private final static Logger mLog = LoggerFactory.getLogger(ThreadPool.class);

    private static final AtomicInteger mPoolNumber = new AtomicInteger(1);
    
    private final ThreadGroup mThreadGroup;
    
    private final AtomicInteger mThreadNumber = new AtomicInteger(1);
    
    private final String mNamePrefix;

    public NamingThreadFactory( String prefix ) 
    {
    	SecurityManager s = System.getSecurityManager();
        
        mThreadGroup = (s != null)? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
        
        mNamePrefix = prefix + " pool-" + mPoolNumber.getAndIncrement() + "-thread-";
    }

    public Thread newThread( Runnable runnable ) 
    {
        Thread thread = new Thread(mThreadGroup, runnable,
              mNamePrefix + mThreadNumber.getAndIncrement(), 0 );
        
        if( thread.isDaemon() )
        {
            thread.setDaemon( false );
        }
        
        if( thread.getPriority() != Thread.NORM_PRIORITY )
        {
            thread.setPriority( Thread.NORM_PRIORITY );
        }

        thread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler()
        {
            @Override
            public void uncaughtException(Thread t, Throwable e)
            {
                mLog.error("Error while executing runnable in scheduled thread pool [" + t.getName() + "]", e);
            }
        });

        return thread;
    }
}