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

package io.github.dsheirer.source.tuner.sdrplay.api.device;

import io.github.dsheirer.source.tuner.sdrplay.api.SDRPlayException;
import io.github.dsheirer.source.tuner.sdrplay.api.SDRplay;
import io.github.dsheirer.source.tuner.sdrplay.api.Status;
import io.github.dsheirer.source.tuner.sdrplay.api.UpdateReason;
import io.github.dsheirer.source.tuner.sdrplay.api.async.AsyncUpdateFuture;
import io.github.dsheirer.source.tuner.sdrplay.api.async.CompletedAsyncUpdate;
import io.github.dsheirer.source.tuner.sdrplay.api.callback.IDeviceEventListener;
import io.github.dsheirer.source.tuner.sdrplay.api.callback.IStreamCallbackListener;
import io.github.dsheirer.source.tuner.sdrplay.api.callback.IStreamListener;
import io.github.dsheirer.source.tuner.sdrplay.api.callback.StreamCallbackParameters;
import io.github.dsheirer.source.tuner.sdrplay.api.error.DebugLevel;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.composite.CompositeParameters;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.tuner.IfMode;
import io.github.dsheirer.source.tuner.sdrplay.api.parameter.tuner.LoMode;
import java.lang.foreign.MemorySegment;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract device structure (sdrplay_api_DeviceT)
 */
public abstract class Device<T extends CompositeParameters<?,?>, R extends RspTuner<?,?>>
{
    private static final Logger mLog = LoggerFactory.getLogger(Device.class);

    private SDRplay mSDRplay;
    private final UpdateRequestManager mUpdateRequestManager = new UpdateRequestManager();
    private final IDeviceStruct mDeviceStruct;
    protected boolean mSelected = false;
    protected boolean mInitialized = false;
    private T mCompositeParameters;

    /**
     * Constructs an SDRPlay device from the foreign memory segment
     * @param sdrPlay api instance that created this device
     * @param deviceStruct to parse or access the fields of the device structure
     */
    public Device(SDRplay sdrPlay, IDeviceStruct deviceStruct)
    {
        mSDRplay = sdrPlay;
        mDeviceStruct = deviceStruct;
    }

    /**
     * Version specific device structure parser
     */
    protected IDeviceStruct getDeviceStruct()
    {
        return mDeviceStruct;
    }

    /**
     * API that owns this device
     */
    protected SDRplay getAPI()
    {
        return mSDRplay;
    }

    /**
     * Stream callback listener for parameter change events.
     */
    public IStreamCallbackListener getStreamCallbackListener()
    {
        return mUpdateRequestManager;
    }

    /**
     * Loads the device parameters for this device.  Subsequent calls once the parameters are created are ignored.
     * @throws SDRPlayException if the device is not selected or if there is an issue loading the parameters
     */
    private void loadDeviceParameters() throws SDRPlayException
    {
        if(selected())
        {
            mCompositeParameters = (T)getAPI().getCompositeParameters(getDeviceType(), getDeviceHandle());
        }
    }

    /**
     * Sets the debug logging level for this device.
     * @param debugLevel to set
     */
    public void setDebugLevel(DebugLevel debugLevel)
    {
        try
        {
            mSDRplay.setDebugLevel(getDeviceHandle(), debugLevel);
        }
        catch(SDRPlayException se)
        {
            mLog.info("Unable to set debug level [" + debugLevel + "] for device - not selected", se);
        }
    }

    /**
     * Selects this device for exclusive use and loads the device composite parameters.
     * @throws SDRPlayException if unable to select the device or if unable to load the composite parameters for use.
     */
    public void select() throws SDRPlayException
    {
        if(!selected())
        {
            getAPI().select(getDeviceMemorySegment());
            mSelected = true;
            loadDeviceParameters();
        }
    }

    /**
     * Indicates if this device has been selected via the SDRplay api
     */
    protected boolean selected()
    {
        return mSelected;
    }

    /**
     * Indicates if the device is valid and ready for use
     */
    public boolean isValid()
    {
        return getDeviceStruct().isValid();
    }

    /**
     * Releases this device from exclusive use.
     */
    public void release() throws SDRPlayException
    {
        if(selected())
        {
            mSelected = false;
            getAPI().release(getDeviceMemorySegment());
        }
    }

    /**
     * Indicates if this device is initialized for use
     */
    public boolean isInitialized()
    {
        return mInitialized;
    }

    /**
     * Initializes this device for single-tuner use and starts the tuner providing raw signal samples to the stream
     * listener and device events to the event listener.
     *
     * Note: invoke select() to select this device for exclusive use before invoking this method.  If this device has
     * previously been initialized, an exception is thrown.  Use uninit() to uninitialize this device and stop the
     * sample stream and event notifications.
     *
     * @param eventListener to receive device event notifications
     * @param streamListener to receive samples from stream.  Stream is from either Tuner 1 or Tuner 2 when
     * the device is selected for single-tuner mode.
     * @throws SDRPlayException if there is an error
     */
    public void initStreamA(IDeviceEventListener eventListener, IStreamListener streamListener) throws SDRPlayException
    {
        if(!isSelected())
        {
            throw new SDRPlayException("Device must be selected before it can be initialized");
        }

        if(isInitialized())
        {
            throw new SDRPlayException("Device has already been initialized with listeners");
        }

        getAPI().initA(Device.this, getDeviceHandle(), eventListener, streamListener);
        mInitialized = true;
    }

    /**
     * Initializes this device for dual-tuner stream B use and starts the tuner providing raw signal samples to the stream
     * listener and device events to the event listener.
     *
     * Note: invoke select() to select this device for exclusive use before invoking this method.  If this device has
     * previously been initialized, an exception is thrown.  Use uninit() to uninitialize this device and stop the
     * sample stream and event notifications.
     *
     * @param eventListener to receive device event notifications
     * @param streamListener to receive samples from stream.  Stream is from either Tuner 1 or Tuner 2 when
     * the device is selected for single-tuner mode.
     * @throws SDRPlayException if there is an error
     */
    public void initStreamB(IDeviceEventListener eventListener, IStreamListener streamListener) throws SDRPlayException
    {
        if(!isSelected())
        {
            throw new SDRPlayException("Device must be selected before it can be initialized");
        }

        if(isInitialized())
        {
            throw new SDRPlayException("Device has already been initialized with listeners");
        }

        getAPI().initB(Device.this, getDeviceHandle(), eventListener, streamListener);
        mInitialized = true;
    }

    /**
     * Uninitializes this device from use.  Note: call is ignored if this device hasn't been initialized
     * @throws SDRPlayException if there is an error
     */
    public void uninitialize() throws SDRPlayException
    {
        if(isInitialized())
        {
            getAPI().uninit(getDeviceHandle());
            mInitialized = false;
        }
        else
        {
            throw new SDRPlayException("Attempt to uninit a device that has not been initialized previously");
        }
    }

    /**
     * Updates this device after parameter change, only when the device is initialized.  If the device is not yet
     * initialized, the update request is ignored.
     *
     * @throws SDRPlayException if unable to apply updates
     */
    public void update(TunerSelect tunerSelect, UpdateReason ... updateReasons) throws SDRPlayException
    {
        if(isInitialized())
        {
            submitUpdate(tunerSelect, updateReasons);
        }
    }

    private String toString(UpdateReason ... updateReasons)
    {
        StringBuilder sb = new StringBuilder();

        boolean first = true;

        for(UpdateReason updateReason: updateReasons)
        {
            if(first)
            {
                sb.append("[");
            }
            else
            {
                sb.append(", ");
            }

            sb.append(updateReason.name());

            first = false;
        }

        sb.append("]");

        return sb.toString();
    }

    /**
     * Asynchronous update request.  This method should only be used for Frequency, Gain and Sample Rate updates.
     * @param tunerSelect tuner being updated
     * @param updateReason for the parameter that is being updated
     * @param expectedResponse that is one of Gain, Frequency, or Sample Rate.
     * @return a future that has already been completed, or if initialized a future that will be completed.
     */
    protected AsyncUpdateFuture updateAsync(TunerSelect tunerSelect, UpdateReason updateReason, UpdateReason expectedResponse)
    {
        if(!expectedResponse.isAsyncUpdateResponse())
        {
            throw new IllegalArgumentException("Invalid expected response: " + expectedResponse +
                    ". Valid values are: " + UpdateReason.ASYNC_UPDATE_RESPONSES);
        }

        if(isInitialized())
        {
            return mUpdateRequestManager.update(tunerSelect, updateReason, expectedResponse);
        }

        //If not initialized, return success.
        AsyncUpdateFuture future = new AsyncUpdateFuture(tunerSelect, updateReason, expectedResponse);
        future.setResult(Status.SUCCESS);
        return future;
    }

    /**
     * Submits an update request to the API.  This method is used/managed by the update request manager.
     * @param tunerSelect for the update
     * @param updateReasons to apply
     * @throws SDRPlayException if there is an issue.
     */
    private void submitUpdate(TunerSelect tunerSelect, UpdateReason ... updateReasons) throws SDRPlayException
    {
        getAPI().update(Device.this, getDeviceHandle(), tunerSelect, updateReasons);
    }

    /**
     * Acknowledge tuner power overload events
     * @param tunerSelect identifying which tuner(s)
     * @throws SDRPlayException on error
     */
    public void acknowledgePowerOverload(TunerSelect tunerSelect) throws SDRPlayException
    {
//        mLog.info("Acknowledging power overload message for tuner [" + tunerSelect + "]");

        //There's a bug (feature?) in the API ... when you un-initialize the device, it causes a power overload event
        // and if you acknowledge it, you get an error that the device is not initialized.
        if(isInitialized())
        {
            try
            {
                update(tunerSelect, UpdateReason.CONTROL_OVERLOAD_MESSAGE_ACK);
            }
            catch(SDRPlayException se)
            {
                //Ignore the not initialized exception that can happen when shutting down the receiver, otherwise
                //rethrow the exception.
                if(se.getStatus() != Status.NOT_INITIALIZED)
                {
                    throw se;
                }
            }
        }
    }

    /**
     * Tuner selection.
     * @return tuner select.  Defaults to TUNER_1 for all but the RSPduo where it is overridden for tuner 2.
     */
    public TunerSelect getTunerSelect()
    {
        return TunerSelect.TUNER_1;
    }

    /**
     * Foreign memory segment representing this device.
     */
    protected MemorySegment getDeviceMemorySegment()
    {
        return getDeviceStruct().getDeviceMemorySegment();
    }

    /**
     * Handle to this device.
     *
     * Note: this device must be selected for exclusive use before you can access this handle.
     *
     * @throws SDRPlayException if this method is accessed before the device has been successfully selected
     */
    MemorySegment getDeviceHandle() throws SDRPlayException
    {
        if(!selected())
        {
            throw new SDRPlayException("This device must be selected for exclusive use before accessing/using the " +
                    "device handle");
        }

        return getDeviceStruct().getDeviceHandle();
    }

    /**
     * Tuner for this device
     * @return tuner appropriate for the device type
     * @throws SDRPlayException for various reasons include device not selected or API unavailable
     */
    public abstract R getTuner() throws SDRPlayException;

    /**
     * Composite parameters for this device
     */
    public T getCompositeParameters()
    {
        return mCompositeParameters;
    }

    /**
     * Indicates if this device has composite parameters
     */
    public boolean hasCompositeParameters()
    {
        return mCompositeParameters != null;
    }

    /**
     * Indicates if this device is available and has been selected for exclusive use.
     */
    public boolean isSelected()
    {
        return mSelected;
    }

    /**
     * Device type
     */
    public DeviceType getDeviceType()
    {
        return getDeviceStruct().getDeviceType();
    }

    /**
     * Serial number
     */
    public String getSerialNumber()
    {
        return getDeviceStruct().getSerialNumber();
    }

    /**
     * Current sample rate
     */
    public double getSampleRate()
    {
        return getCompositeParameters().getDeviceParameters().getSamplingFrequency().getSampleRate();
    }

    public void setIfMode(IfMode ifMode)
    {
        getCompositeParameters().getTunerAParameters().setIfMode(ifMode);
    }

    public void setLoMode(LoMode loMode)
    {
        getCompositeParameters().getTunerAParameters().setLoMode(loMode);
    }

    /**
     * Enables or disables wideband signal mode.  This should be set according to the IFMode, where IFMode.ZERO is
     * wideband and all others are not wideband.
     * @param enable
     */
    public void setWidebandSignal(boolean enable) throws SDRPlayException
    {
        getCompositeParameters().getControlAParameters().getDecimation().setWideBandSignal(enable);

        if(isInitialized())
        {
            update(getTunerSelect(), UpdateReason.CONTROL_DECIMATION);
        }
    }

    /**
     * Sets the software-based decimation factor for the final sample rate.
     * @param decimate value where X1 is no decimation and all others are decimation enabled values.
     * @throws SDRPlayException if there is an error while setting decimation
     */
    public void setDecimation(Decimate decimate) throws SDRPlayException
    {
        getCompositeParameters().getControlAParameters().getDecimation().setDecimationFactor(decimate.getValue());
        getCompositeParameters().getControlAParameters().getDecimation().setEnabled(decimate.isEnabled());

        if(isInitialized())
        {
            update(getTunerSelect(), UpdateReason.CONTROL_DECIMATION);
        }
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append("SDRPplay Device").append("\n");
        sb.append("\tType: ").append(getDeviceType()).append("\n");
        sb.append("\tSerial Number: ").append(getSerialNumber()).append("\n");
        sb.append("\tSelected: ").append(isSelected());
        if(hasCompositeParameters())
        {
            sb.append("\t").append(getCompositeParameters());
        }

        return sb.toString();
    }

    /**
     * Thread-safe manager for asynchronous update request queue processing for an initialized RSP device.
     *
     * Once a device has been initialized, any changes to frequency, gain or sample rate require submitting an update
     * request to the device to apply the parameter change(s).  However, since the API supports non-blocking operation,
     * the operation executes asynchronously. The API supports only a single update request operation
     * to be in-progress at a time and any overlapping update requests are met with an unsuccessful status code return
     * from the update method.
     */
    class UpdateRequestManager implements IStreamCallbackListener
    {
        private static final long UPDATE_QUEUE_PROCESSING_INTERVAL_MS = 75;
        private final ScheduledExecutorService mExecutorService = Executors.newSingleThreadScheduledExecutor();
        private final Queue<AsyncUpdateFuture> mUpdateQueue = new ConcurrentLinkedQueue();
        private final Queue<CompletedAsyncUpdate> mCompletedUpdateQueue = new ConcurrentLinkedQueue();
        private final ReentrantLock mLock = new ReentrantLock();

        /**
         * Submits an update request for the specified tuner and update reason for queued processing.  This is a
         * non-blocking operation and the update is performed by a thread from the thread pool.
         * @param tunerSelect to apply the update
         * @param updateReason to update
         * @return an asynchronous future to monitor the progress of the update request
         */
        public AsyncUpdateFuture update(TunerSelect tunerSelect, UpdateReason updateReason, UpdateReason expectedResponse)
        {
            AsyncUpdateFuture future = new AsyncUpdateFuture(tunerSelect, updateReason, expectedResponse);
            mUpdateQueue.add(future);
            processQueuesImmediately();
            return future;
        }

        /**
         * Schedules a process queues task for immediate execution.  Non-blocking.
         */
        private void processQueuesImmediately()
        {
            processQueuesAfterDelay(0);
        }

        /**
         * Schedules a process queues task after the delay.  Non-blocking
         * @param delay in milliseconds, or zero for immediate.
         */
        private void processQueuesAfterDelay(long delay)
        {
            mExecutorService.schedule(() -> processQueues(), delay, TimeUnit.MILLISECONDS);
        }

        /**
         * Processes the pending and completed update operation queues.  Submits new update requests one at a time and
         * awaits a stream callback notification that the matching update reason has been applied/updated.  Ensures
         * that only a single update operation is in-progress at any given time.
         */
        private synchronized void processQueues()
        {
            mLock.lock();

            try
            {
                if(mUpdateQueue.isEmpty())
                {
                    //If we have no pending updates, we don't care about any completed update results
                    mCompletedUpdateQueue.clear();
                }
                else
                {
                    boolean processing = true;

                    while(processing)
                    {
                        processing = false;

                        AsyncUpdateFuture futureUpdate = mUpdateQueue.peek();

                        if(futureUpdate != null)
                        {
                            if(futureUpdate.isSubmitted())
                            {
                                //Process the completion queue
                                while(!mCompletedUpdateQueue.isEmpty())
                                {
                                    CompletedAsyncUpdate completedUpdate = mCompletedUpdateQueue.poll();

                                    if(completedUpdate != null)
                                    {
                                        if(futureUpdate.matches(completedUpdate))
                                        {
                                            //Clear the remaining completed updates
                                            mCompletedUpdateQueue.clear();

                                            //Remove and (successfully) complete the current future
                                            mUpdateQueue.remove();
                                            futureUpdate.setResult(Status.SUCCESS);

                                            //Signal to immediately reprocess the queue
                                            processing = true;

                                            //Break out of the completed update queue processing
                                            break;
                                        }
                                    }
                                }
                            }
                            else
                            {
                                //Clear the completed queue and submit the update
                                mCompletedUpdateQueue.clear();

                                try
                                {
                                    submitUpdate(futureUpdate.getTunerSelect(), futureUpdate.getUpdateReason());
                                    futureUpdate.setSubmitted(true);
                                }
                                catch(SDRPlayException se)
                                {
                                    futureUpdate = mUpdateQueue.poll();
                                    futureUpdate.setError(se);
                                    //Set continuous to true to immediately reprocess the next update
                                    processing = true;
                                }
                            }
                        }
                    }
                }
            }
            finally
            {
                mLock.unlock();
            }

            if(!mUpdateQueue.isEmpty())
            {
                processQueuesAfterDelay(UPDATE_QUEUE_PROCESSING_INTERVAL_MS);
            }
        }

        /**
         * Receives an update completion event.  This is a non-blocking operation since this method will be invoked
         * by the stream callback thread, and we don't want to impact the delivery of streaming samples or events.
         *
         * @param tunerSelect tuner that was updated
         * @param updateReason for what was updated
         */
        public void completed(TunerSelect tunerSelect, UpdateReason updateReason)
        {
            mCompletedUpdateQueue.add(new CompletedAsyncUpdate(tunerSelect, updateReason));
            mExecutorService.schedule(this::processQueues, 0, TimeUnit.MILLISECONDS);
        }

        /**
         * Resets the update queue.
         */
        public void reset()
        {
            mLock.lock();

            try
            {
                while(!mUpdateQueue.isEmpty())
                {
                    AsyncUpdateFuture future = mUpdateQueue.poll();
                    future.setResult(Status.UNKNOWN);
                }
            }
            finally
            {
                mLock.unlock();
            }
        }

        /**
         * Implements the IStreamCallbackListener interface to receive change notifications from update requests.
         * @param parameters to process
         * @param reset value with flags
         */
        @Override
        public void process(TunerSelect tunerSelect, StreamCallbackParameters parameters, int reset)
        {
            if(parameters.isSampleRateChanged())
            {
                completed(tunerSelect, UpdateReason.DEVICE_SAMPLE_RATE);
            }
            if(parameters.isRfFrequencyChanged())
            {
                completed(tunerSelect, UpdateReason.TUNER_FREQUENCY_RF);
            }
            if(parameters.isGainReductionChanged())
            {
                completed(tunerSelect, UpdateReason.TUNER_GAIN_REDUCTION);
            }
        }
    }
}
