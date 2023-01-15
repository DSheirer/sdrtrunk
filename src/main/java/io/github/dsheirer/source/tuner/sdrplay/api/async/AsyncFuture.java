/*
 * *****************************************************************************
 * Copyright (C) 2014-2023 Dennis Sheirer
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

package io.github.dsheirer.source.tuner.sdrplay.api.async;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Asynchronous operation that implements the Future interface with utility methods for callback on completion, or
 * converting to a synchronous (ie blocking) operation.
 * @param <R> expected return type upon completion of the operation
 */
public class AsyncFuture<R> implements Future<R>
{
    private R mResult;
    private Throwable mError;
    private boolean mCancelled;
    private boolean mCompleted = false;
    private ReentrantLock mLock = new ReentrantLock();
    private IAsyncCallback mCallback;

    /**
     * Sets the result and completes this future.
     * @param result to apply.
     */
    public synchronized void setResult(R result)
    {
        if(mCompleted)
        {
            throw new IllegalStateException("Future is already completed.");
        }

        mResult = result;
        finish();
    }

    /**
     * Sets an error resulting from the operation and completes this future.
     * @param error produced from the operation
     */
    public synchronized void setError(Throwable error)
    {
        if(mCompleted)
        {
            throw new IllegalStateException("Future is already completed.");
        }

        mError = error;
        finish();
    }

    /**
     * (Optional) error produced from the operation.
     */
    public Throwable getError()
    {
        return mError;
    }

    /**
     * Indicates if this future had an error during the operation
     */
    public boolean hasError()
    {
        return mError != null;
    }

    /**
     * Cleanup task that completes the future and notifies an optional registered callback.
     */
    private void finish()
    {
        mLock.lock();

        try
        {
            mCompleted = true;

            if(mCallback != null)
            {
                mCallback.complete(this);
                mCallback = null;
            }
        }
        finally
        {
            mLock.unlock();
        }

        notifyAll();
    }

    /**
     * Signals a desire to cancel this future operation.
     *
     * Note: it is up to the implementer to monitor this cancel state.
     * @param mayInterruptIfRunning not implemented.
     * @return true always.
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning)
    {
        mCancelled = true;
        return true;
    }

    /**
     * Indicates if this future has been cancelled.
     */
    @Override
    public boolean isCancelled()
    {
        return mCancelled;
    }

    /**
     * Indicates if this future has completed.
     */
    @Override
    public boolean isDone()
    {
        return mCompleted;
    }

    /**
     * Blocking call to wait indefinitely for the result of the operation.
     * @return result of the operation
     * @throws InterruptedException if the calling thread is interrupted while waiting
     * @throws ExecutionException if there is an error produced from the operation
     */
    @Override
    public synchronized R get() throws InterruptedException, ExecutionException
    {
        while(!mCompleted)
        {
            wait();
        }

        if(mError != null)
        {
            throw new ExecutionException(mError);
        }

        return mResult;
    }

    /**
     * Blocking call to synchronously get the result of the operation.
     * @param timeout to wait
     * @param unit for the timeout
     * @return result once completed
     * @throws InterruptedException if the calling thread is interrupted while waiting
     * @throws ExecutionException if there is an error produced from the operation
     * @throws TimeoutException if the timeout is exceeded while waiting.
     */
    @Override
    public synchronized R get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException
    {
        if(!mCompleted)
        {
            wait(unit.toMillis(timeout));
        }

        if(!mCompleted)
        {
            throw new TimeoutException("Get timeout [" + timeout + " " + unit + "] exceeded without completion");
        }

        if(mError != null)
        {
            throw new ExecutionException(mError);
        }

        return mResult;
    }

    /**
     * Registers the callback to receive the result once the operation completes.  If the operation is completed, the
     * callback is immediately notified, otherwise the callback will be notified later.
     * @param callback to be notified once the operation is completed.
     */
    public synchronized void callback(IAsyncCallback callback)
    {
        if(mCallback != null)
        {
            throw new IllegalArgumentException("Callback has been registered - only one callback is supported.");
        }

        mLock.lock();

        try
        {
            if(mCompleted)
            {
                callback.complete(this);
            }
            else
            {
                mCallback = callback;
            }
        }
        finally
        {
            mLock.unlock();
        }
    }
}
