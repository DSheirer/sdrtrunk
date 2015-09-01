package controller;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread factory that applies custom names to threads
 */
public class NamingThreadFactory implements ThreadFactory 
{
    static final AtomicInteger poolNumber = new AtomicInteger(1);
    
    final ThreadGroup mThreadGroup;
    
    final AtomicInteger mThreadNumber = new AtomicInteger(1);
    
    final String mNamePrefix;

    public NamingThreadFactory( String prefix ) 
    {
    	SecurityManager s = System.getSecurityManager();
        
        mThreadGroup = (s != null)? s.getThreadGroup() :
                             Thread.currentThread().getThreadGroup();
        
        mNamePrefix = prefix + " pool-" +
                      poolNumber.getAndIncrement() +
                     "-thread-";
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
        
        return thread;
    }
}