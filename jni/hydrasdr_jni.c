/*
 * *****************************************************************************
 * Copyright (C) 2024-2025 Benjamin VERNOUX
 *
 * JNI bridge between Java (sdrtrunk) and libhydrasdr C library.
 *
 * This file implements all native methods declared in HydraSdrNative.java.
 * It wraps the libhydrasdr public API to provide hardware-agnostic access
 * to HydraSDR devices from Java.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * ****************************************************************************
 */

#include <jni.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>
#include <hydrasdr.h>

#ifdef _WIN32
#include <windows.h>
static CRITICAL_SECTION g_table_cs;
#define TABLE_LOCK()   EnterCriticalSection(&g_table_cs)
#define TABLE_UNLOCK() LeaveCriticalSection(&g_table_cs)
#else
#include <pthread.h>
static pthread_mutex_t g_table_mtx = PTHREAD_MUTEX_INITIALIZER;
#define TABLE_LOCK()   pthread_mutex_lock(&g_table_mtx)
#define TABLE_UNLOCK() pthread_mutex_unlock(&g_table_mtx)
#endif

#define DEVICE_INFO_CLASS "io/github/dsheirer/source/tuner/hydrasdr/HydraSdrDeviceInfo"

/* Validate device handle macro - returns retval if NULL */
#define CHECK_DEV(handle, retval) \
	struct hydrasdr_device *dev = (struct hydrasdr_device *)(uintptr_t)(handle); \
	if (!dev) return (retval)

/*
 * Streaming callback context - holds JVM reference and Java callback object.
 *
 * Lifecycle:
 *   startRx:  allocate context, create global refs, start streaming
 *   callback: attach thread once, de-interleave, call Java, detach on stop
 *   stopRx:   hydrasdr_stop_rx() blocks until thread exits, then cleanup
 *
 * Pre-allocated Java arrays are reused across callbacks to eliminate
 * NewFloatArray/GC overhead (at 10 MSps: ~75 MB/sec of avoided GC pressure).
 */
typedef struct {
	JavaVM *jvm;
	jobject callback;
	jmethodID onSamplesMethod;
	JNIEnv *attached_env;
	volatile int thread_attached;
	volatile int stopping;         /* set by stopRx before hydrasdr_stop_rx */
	/* Double-buffered pre-allocated Java arrays (global refs).
	 * Two sets alternate so Java can consume one while native fills the other. */
	jfloatArray ji_buf[2];
	jfloatArray jq_buf[2];
	volatile int buf_index;
	volatile int buf_size;
	volatile int active;           /* 1 if streaming is active */
} jni_stream_ctx_t;

/*
 * Per-device stream context table.
 * Supports up to MAX_DEVICES concurrent HydraSDR devices.
 * Keyed by the native device handle pointer.
 */
/* Maximum concurrent HydraSDR devices supported */
#define MAX_DEVICES 8

/* Maximum samples per callback (4M samples = 32 MB of float arrays) */
#define MAX_SAMPLES_PER_CALLBACK (4 * 1024 * 1024)

static struct {
	uintptr_t handle;
	jni_stream_ctx_t ctx;
} g_devices[MAX_DEVICES];

/* Lookup or allocate a context slot for a device handle (thread-safe) */
static jni_stream_ctx_t *get_stream_ctx(uintptr_t handle, int allocate)
{
	jni_stream_ctx_t *result = NULL;

	TABLE_LOCK();

	/* Find existing */
	for (int i = 0; i < MAX_DEVICES; i++) {
		if (g_devices[i].handle == handle && handle != 0) {
			result = &g_devices[i].ctx;
			goto done;
		}
	}

	if (!allocate)
		goto done;

	/* Allocate new slot */
	for (int i = 0; i < MAX_DEVICES; i++) {
		if (g_devices[i].handle == 0) {
			g_devices[i].handle = handle;
			memset(&g_devices[i].ctx, 0, sizeof(jni_stream_ctx_t));
			result = &g_devices[i].ctx;
			goto done;
		}
	}

done:
	TABLE_UNLOCK();
	return result;
}

/* Release a context slot (thread-safe) */
static void release_stream_ctx(uintptr_t handle)
{
	TABLE_LOCK();

	for (int i = 0; i < MAX_DEVICES; i++) {
		if (g_devices[i].handle == handle) {
			g_devices[i].handle = 0;
			memset(&g_devices[i].ctx, 0, sizeof(jni_stream_ctx_t));
			break;
		}
	}

	TABLE_UNLOCK();
}

/* Helper: free all double-buffer global refs */
static void free_stream_buffers(JNIEnv *env, jni_stream_ctx_t *ctx)
{
	for (int b = 0; b < 2; b++) {
		if (ctx->ji_buf[b]) {
			(*env)->DeleteGlobalRef(env, ctx->ji_buf[b]);
			ctx->ji_buf[b] = NULL;
		}
		if (ctx->jq_buf[b]) {
			(*env)->DeleteGlobalRef(env, ctx->jq_buf[b]);
			ctx->jq_buf[b] = NULL;
		}
	}
	ctx->buf_size = 0;
}

/* Helper: free all global refs in context */
static void cleanup_stream_ctx(JNIEnv *env, jni_stream_ctx_t *ctx)
{
	free_stream_buffers(env, ctx);
	if (ctx->callback) {
		(*env)->DeleteGlobalRef(env, ctx->callback);
		ctx->callback = NULL;
	}
	ctx->thread_attached = 0;
	ctx->attached_env = NULL;
	ctx->stopping = 0;
	ctx->active = 0;
}

/* ==================== Device Management ==================== */

JNIEXPORT jintArray JNICALL
Java_io_github_dsheirer_source_tuner_hydrasdr_HydraSdrNative_getLibVersion(
	JNIEnv *env, jclass cls)
{
	hydrasdr_lib_version_t ver;

	hydrasdr_lib_version(&ver);

	jintArray result = (*env)->NewIntArray(env, 3);
	if (!result)
		return NULL;

	jint buf[3];
	buf[0] = ver.major_version;
	buf[1] = ver.minor_version;
	buf[2] = ver.revision;
	(*env)->SetIntArrayRegion(env, result, 0, 3, buf);

	return result;
}

JNIEXPORT jlongArray JNICALL
Java_io_github_dsheirer_source_tuner_hydrasdr_HydraSdrNative_listDevices(
	JNIEnv *env, jclass cls)
{
	int count = hydrasdr_list_devices(NULL, 0);
	if (count <= 0)
		return (*env)->NewLongArray(env, 0);

	uint64_t *serials = calloc(count, sizeof(uint64_t));
	if (!serials)
		return (*env)->NewLongArray(env, 0);

	int found = hydrasdr_list_devices(serials, count);
	if (found <= 0) {
		free(serials);
		return (*env)->NewLongArray(env, 0);
	}

	jlongArray result = (*env)->NewLongArray(env, found);
	if (!result) {
		free(serials);
		return NULL;
	}

	/* Use stack buffer for small counts, heap for large */
	jlong stack_buf[16];
	jlong *buf = (found <= 16) ? stack_buf : calloc(found, sizeof(jlong));
	if (!buf) {
		free(serials);
		return NULL;
	}

	for (int i = 0; i < found; i++)
		buf[i] = (jlong)serials[i]; /* opaque 64-bit value */

	(*env)->SetLongArrayRegion(env, result, 0, found, buf);
	if (buf != stack_buf)
		free(buf);
	free(serials);

	return result;
}

JNIEXPORT jlong JNICALL
Java_io_github_dsheirer_source_tuner_hydrasdr_HydraSdrNative_open(
	JNIEnv *env, jclass cls, jlong serialNumber)
{
	struct hydrasdr_device *dev = NULL;
	int ret = hydrasdr_open_sn(&dev, (uint64_t)serialNumber);

	if (ret != HYDRASDR_SUCCESS || !dev)
		return 0;

	return (jlong)(uintptr_t)dev;
}

JNIEXPORT jlong JNICALL
Java_io_github_dsheirer_source_tuner_hydrasdr_HydraSdrNative_openAny(
	JNIEnv *env, jclass cls)
{
	struct hydrasdr_device *dev = NULL;
	int ret = hydrasdr_open(&dev);

	if (ret != HYDRASDR_SUCCESS || !dev)
		return 0;

	return (jlong)(uintptr_t)dev;
}

JNIEXPORT void JNICALL
Java_io_github_dsheirer_source_tuner_hydrasdr_HydraSdrNative_close(
	JNIEnv *env, jclass cls, jlong handle)
{
	struct hydrasdr_device *dev = (struct hydrasdr_device *)(uintptr_t)handle;
	if (dev)
		hydrasdr_close(dev);
}

/* ==================== Configuration ==================== */

JNIEXPORT jint JNICALL
Java_io_github_dsheirer_source_tuner_hydrasdr_HydraSdrNative_setFrequency(
	JNIEnv *env, jclass cls, jlong handle, jlong freqHz)
{
	CHECK_DEV(handle, HYDRASDR_ERROR_INVALID_PARAM);
	return hydrasdr_set_freq(dev, (uint64_t)freqHz);
}

JNIEXPORT jint JNICALL
Java_io_github_dsheirer_source_tuner_hydrasdr_HydraSdrNative_setSampleRate(
	JNIEnv *env, jclass cls, jlong handle, jint rateHz)
{
	CHECK_DEV(handle, HYDRASDR_ERROR_INVALID_PARAM);
	return hydrasdr_set_samplerate(dev, (uint32_t)rateHz);
}

JNIEXPORT jint JNICALL
Java_io_github_dsheirer_source_tuner_hydrasdr_HydraSdrNative_setBandwidth(
	JNIEnv *env, jclass cls, jlong handle, jint bwHz)
{
	CHECK_DEV(handle, HYDRASDR_ERROR_INVALID_PARAM);
	return hydrasdr_set_bandwidth(dev, (uint32_t)bwHz);
}

JNIEXPORT jint JNICALL
Java_io_github_dsheirer_source_tuner_hydrasdr_HydraSdrNative_setSampleType(
	JNIEnv *env, jclass cls, jlong handle, jint type)
{
	CHECK_DEV(handle, HYDRASDR_ERROR_INVALID_PARAM);
	return hydrasdr_set_sample_type(dev, (enum hydrasdr_sample_type)type);
}

JNIEXPORT jint JNICALL
Java_io_github_dsheirer_source_tuner_hydrasdr_HydraSdrNative_setDecimationMode(
	JNIEnv *env, jclass cls, jlong handle, jint mode)
{
	CHECK_DEV(handle, HYDRASDR_ERROR_INVALID_PARAM);
	return hydrasdr_set_decimation_mode(dev, (enum hydrasdr_decimation_mode)mode);
}

static jintArray uint32_array_to_jintArray(JNIEnv *env, uint32_t *data, uint32_t count)
{
	jintArray result = (*env)->NewIntArray(env, count);
	if (!result)
		return NULL;

	/* Use SetIntArrayRegion with stack cast for small arrays */
	jint stack_buf[64];
	jint *buf = (count <= 64) ? stack_buf : calloc(count, sizeof(jint));
	if (!buf)
		return NULL;

	for (uint32_t i = 0; i < count; i++)
		buf[i] = (jint)data[i];

	(*env)->SetIntArrayRegion(env, result, 0, count, buf);
	if (buf != stack_buf)
		free(buf);

	return result;
}

JNIEXPORT jintArray JNICALL
Java_io_github_dsheirer_source_tuner_hydrasdr_HydraSdrNative_getSampleRates(
	JNIEnv *env, jclass cls, jlong handle)
{
	CHECK_DEV(handle, NULL);

	uint32_t count = 0;
	int ret = hydrasdr_get_samplerates(dev, &count, 0);
	if (ret != HYDRASDR_SUCCESS || count == 0)
		return NULL;

	uint32_t *rates = calloc(count, sizeof(uint32_t));
	if (!rates)
		return NULL;

	ret = hydrasdr_get_samplerates(dev, rates, count);
	if (ret != HYDRASDR_SUCCESS) {
		free(rates);
		return NULL;
	}

	jintArray result = uint32_array_to_jintArray(env, rates, count);
	free(rates);
	return result;
}

JNIEXPORT jintArray JNICALL
Java_io_github_dsheirer_source_tuner_hydrasdr_HydraSdrNative_getBandwidths(
	JNIEnv *env, jclass cls, jlong handle)
{
	CHECK_DEV(handle, NULL);

	uint32_t count = 0;
	int ret = hydrasdr_get_bandwidths(dev, &count, 0);
	if (ret != HYDRASDR_SUCCESS || count == 0)
		return NULL;

	uint32_t *bws = calloc(count, sizeof(uint32_t));
	if (!bws)
		return NULL;

	ret = hydrasdr_get_bandwidths(dev, bws, count);
	if (ret != HYDRASDR_SUCCESS) {
		free(bws);
		return NULL;
	}

	jintArray result = uint32_array_to_jintArray(env, bws, count);
	free(bws);
	return result;
}

/* ==================== Gain Control ==================== */

JNIEXPORT jint JNICALL
Java_io_github_dsheirer_source_tuner_hydrasdr_HydraSdrNative_setGain(
	JNIEnv *env, jclass cls, jlong handle, jint type, jint value)
{
	CHECK_DEV(handle, HYDRASDR_ERROR_INVALID_PARAM);
	return hydrasdr_set_gain(dev, (hydrasdr_gain_type_t)type, (uint8_t)value);
}

JNIEXPORT jintArray JNICALL
Java_io_github_dsheirer_source_tuner_hydrasdr_HydraSdrNative_getGainInfo(
	JNIEnv *env, jclass cls, jlong handle, jint type)
{
	CHECK_DEV(handle, NULL);
	hydrasdr_gain_info_t info;

	int ret = hydrasdr_get_gain(dev, (hydrasdr_gain_type_t)type, &info);
	if (ret != HYDRASDR_SUCCESS)
		return NULL;

	jintArray result = (*env)->NewIntArray(env, 6);
	if (!result)
		return NULL;

	jint buf[6];
	buf[0] = info.value;
	buf[1] = info.min_value;
	buf[2] = info.max_value;
	buf[3] = info.step_value;
	buf[4] = info.default_value;
	buf[5] = info.flags;
	(*env)->SetIntArrayRegion(env, result, 0, 6, buf);

	return result;
}

JNIEXPORT jint JNICALL
Java_io_github_dsheirer_source_tuner_hydrasdr_HydraSdrNative_getCapabilities(
	JNIEnv *env, jclass cls, jlong handle)
{
	CHECK_DEV(handle, 0);
	hydrasdr_device_info_t info;

	int ret = hydrasdr_get_device_info(dev, &info);
	if (ret != HYDRASDR_SUCCESS)
		return 0;

	return (jint)info.features;
}

/* ==================== Streaming ==================== */

/*
 * De-interleave float IQ samples into separate I and Q arrays.
 * Processes 4 samples at a time for better pipelining.
 */
static void deinterleave_iq(const float *iq, float *i_out, float *q_out, int sample_count)
{
	int x = 0;

	/* Unrolled loop: process 4 complex samples per iteration */
	for (; x + 3 < sample_count; x += 4) {
		const float *src = iq + x * 2;
		i_out[x]     = src[0];
		q_out[x]     = src[1];
		i_out[x + 1] = src[2];
		q_out[x + 1] = src[3];
		i_out[x + 2] = src[4];
		q_out[x + 2] = src[5];
		i_out[x + 3] = src[6];
		q_out[x + 3] = src[7];
	}

	/* Remainder */
	for (; x < sample_count; x++) {
		i_out[x] = iq[x * 2];
		q_out[x] = iq[x * 2 + 1];
	}
}

/*
 * Native streaming callback - called on libhydrasdr's USB thread.
 *
 * - Attaches to JVM once on first call
 * - De-interleaves IQ in native C (unrolled loop) into pre-split I/Q arrays
 * - Double-buffered arrays: no GC allocation per callback
 * - Detaches from JVM when streaming stops (preventing thread leak)
 */
static int jni_stream_callback(hydrasdr_transfer_t *transfer)
{
	jni_stream_ctx_t *ctx = (jni_stream_ctx_t *)transfer->ctx;
	if (!ctx || !ctx->callback || ctx->stopping)
		goto detach_and_exit;

	JNIEnv *env = ctx->attached_env;

	/* Attach streaming thread to JVM on first callback */
	if (!ctx->thread_attached) {
		jint status = (*ctx->jvm)->GetEnv(ctx->jvm, (void **)&env, JNI_VERSION_1_6);
		if (status == JNI_EDETACHED) {
			if ((*ctx->jvm)->AttachCurrentThread(ctx->jvm, (void **)&env, NULL) != 0)
				return -1;
		} else if (status != JNI_OK) {
			return -1;
		}
		ctx->attached_env = env;
		ctx->thread_attached = 1;
	}

	int sample_count = transfer->sample_count;
	float *samples = (float *)transfer->samples;

	/* Sanity check: reject absurd sample counts */
	if (sample_count <= 0 || sample_count > MAX_SAMPLES_PER_CALLBACK)
		return 0;

	/*
	 * Double-buffer: reuse pre-allocated Java arrays when size matches.
	 * On size change, free old buffers and allocate new ones.
	 */
	if (ctx->buf_size != sample_count) {
		free_stream_buffers(env, ctx);

		int alloc_ok = 1;
		for (int b = 0; b < 2 && alloc_ok; b++) {
			jfloatArray ji_local = (*env)->NewFloatArray(env, sample_count);
			jfloatArray jq_local = (*env)->NewFloatArray(env, sample_count);
			if (!ji_local || !jq_local) {
				if (ji_local) (*env)->DeleteLocalRef(env, ji_local);
				if (jq_local) (*env)->DeleteLocalRef(env, jq_local);
				/* Clean up any already-allocated buffers from previous iterations */
				free_stream_buffers(env, ctx);
				alloc_ok = 0;
			} else {
				ctx->ji_buf[b] = (*env)->NewGlobalRef(env, ji_local);
				ctx->jq_buf[b] = (*env)->NewGlobalRef(env, jq_local);
				(*env)->DeleteLocalRef(env, ji_local);
				(*env)->DeleteLocalRef(env, jq_local);
				if (!ctx->ji_buf[b] || !ctx->jq_buf[b]) {
					/* Global ref pool exhausted */
					if ((*env)->ExceptionCheck(env))
						(*env)->ExceptionClear(env);
					free_stream_buffers(env, ctx);
					alloc_ok = 0;
				}
			}
		}

		if (!alloc_ok)
			return 0; /* OOM -- skip this buffer, try again next callback */

		ctx->buf_size = sample_count;
		ctx->buf_index = 0;
	}

	/* Select current buffer pair and advance for next callback */
	int idx = ctx->buf_index;
	ctx->buf_index = 1 - idx;

	/*
	 * GetPrimitiveArrayCritical: fastest JNI array access path.
	 * May return direct pointer to Java heap (no copy on most JVMs).
	 * Must not call other JNI functions while held.
	 */
	jfloat *i_ptr = (*env)->GetPrimitiveArrayCritical(env, ctx->ji_buf[idx], NULL);
	jfloat *q_ptr = (*env)->GetPrimitiveArrayCritical(env, ctx->jq_buf[idx], NULL);

	if (i_ptr && q_ptr) {
		deinterleave_iq(samples, i_ptr, q_ptr, sample_count);
	}

	if (q_ptr) (*env)->ReleasePrimitiveArrayCritical(env, ctx->jq_buf[idx], q_ptr, 0);
	if (i_ptr) (*env)->ReleasePrimitiveArrayCritical(env, ctx->ji_buf[idx], i_ptr, 0);

	(*env)->CallVoidMethod(env, ctx->callback, ctx->onSamplesMethod,
		ctx->ji_buf[idx], ctx->jq_buf[idx], (jint)sample_count,
		(jlong)transfer->dropped_samples);

	if ((*env)->ExceptionCheck(env)) {
		(*env)->ExceptionClear(env);
		return -1;
	}

	return 0;

detach_and_exit:
	/* Streaming is ending -- detach this thread from JVM before it exits */
	if (ctx && ctx->thread_attached && ctx->jvm) {
		(*ctx->jvm)->DetachCurrentThread(ctx->jvm);
		ctx->thread_attached = 0;
		ctx->attached_env = NULL;
	}
	return -1;
}

JNIEXPORT jint JNICALL
Java_io_github_dsheirer_source_tuner_hydrasdr_HydraSdrNative_startRx(
	JNIEnv *env, jclass cls, jlong handle, jobject callback)
{
	CHECK_DEV(handle, HYDRASDR_ERROR_INVALID_PARAM);

	/* Get or allocate a per-device context slot */
	jni_stream_ctx_t *ctx = get_stream_ctx((uintptr_t)handle, 1);
	if (!ctx)
		return HYDRASDR_ERROR_NO_MEM;

	if (ctx->active)
		return HYDRASDR_ERROR_BUSY;

	/* Clean up any leftover state from a previous session */
	cleanup_stream_ctx(env, ctx);

	/* Initialize context */
	(*env)->GetJavaVM(env, &ctx->jvm);
	ctx->callback = (*env)->NewGlobalRef(env, callback);
	if (!ctx->callback) {
		if ((*env)->ExceptionCheck(env))
			(*env)->ExceptionClear(env);
		release_stream_ctx((uintptr_t)handle);
		return HYDRASDR_ERROR_NO_MEM;
	}

	jclass cbClass = (*env)->GetObjectClass(env, callback);
	ctx->onSamplesMethod = (*env)->GetMethodID(env, cbClass,
		"onSamples", "([F[FIJ)V");
	(*env)->DeleteLocalRef(env, cbClass);

	if (!ctx->onSamplesMethod) {
		if ((*env)->ExceptionCheck(env))
			(*env)->ExceptionClear(env);
		cleanup_stream_ctx(env, ctx);
		release_stream_ctx((uintptr_t)handle);
		return HYDRASDR_ERROR_OTHER;
	}

	ctx->active = 1;

	int ret = hydrasdr_start_rx(dev, jni_stream_callback, ctx);
	if (ret != HYDRASDR_SUCCESS) {
		cleanup_stream_ctx(env, ctx);
		release_stream_ctx((uintptr_t)handle);
	}

	return ret;
}

JNIEXPORT jint JNICALL
Java_io_github_dsheirer_source_tuner_hydrasdr_HydraSdrNative_stopRx(
	JNIEnv *env, jclass cls, jlong handle)
{
	CHECK_DEV(handle, HYDRASDR_ERROR_INVALID_PARAM);

	jni_stream_ctx_t *ctx = get_stream_ctx((uintptr_t)handle, 0);
	if (!ctx || !ctx->active)
		return HYDRASDR_SUCCESS;

	/*
	 * Signal the callback to detach its thread and stop processing.
	 *
	 * CRITICAL CONTRACT: hydrasdr_stop_rx() MUST block until the streaming
	 * thread has fully exited and will never call the callback again.
	 * The callback checks 'stopping' and calls DetachCurrentThread before
	 * the thread terminates. If hydrasdr_stop_rx() returned before the
	 * thread exits, cleanup_stream_ctx() below would free resources still
	 * in use by the callback — causing a use-after-free.
	 */
	ctx->stopping = 1;

	int ret = hydrasdr_stop_rx(dev);

	/* hydrasdr_stop_rx() has returned — streaming thread has exited.
	 * Safe to clean up JNI global refs and context. */
	cleanup_stream_ctx(env, ctx);
	release_stream_ctx((uintptr_t)handle);

	return ret;
}

JNIEXPORT jboolean JNICALL
Java_io_github_dsheirer_source_tuner_hydrasdr_HydraSdrNative_isStreaming(
	JNIEnv *env, jclass cls, jlong handle)
{
	CHECK_DEV(handle, JNI_FALSE);
	return hydrasdr_is_streaming(dev) == HYDRASDR_TRUE ? JNI_TRUE : JNI_FALSE;
}

/* ==================== Device Information ==================== */

JNIEXPORT jobject JNICALL
Java_io_github_dsheirer_source_tuner_hydrasdr_HydraSdrNative_getDeviceInfo(
	JNIEnv *env, jclass cls, jlong handle)
{
	CHECK_DEV(handle, NULL);
	hydrasdr_device_info_t info;

	int ret = hydrasdr_get_device_info(dev, &info);
	if (ret != HYDRASDR_SUCCESS)
		return NULL;

	jclass infoClass = (*env)->FindClass(env, DEVICE_INFO_CLASS);
	if (!infoClass)
		return NULL;

	jmethodID ctor = (*env)->GetMethodID(env, infoClass, "<init>", "()V");
	if (!ctor)
		return NULL;

	jobject jinfo = (*env)->NewObject(env, infoClass, ctor);
	if (!jinfo)
		return NULL;

	/* Lookup all setters - validate first one as canary */
	jmethodID setBoardId = (*env)->GetMethodID(env, infoClass, "setBoardId", "(I)V");
	if (!setBoardId)
		return NULL; /* Java/JNI class mismatch */

	jmethodID setBoardName = (*env)->GetMethodID(env, infoClass, "setBoardName", "(Ljava/lang/String;)V");
	jmethodID setFirmwareVersion = (*env)->GetMethodID(env, infoClass, "setFirmwareVersion", "(Ljava/lang/String;)V");
	jmethodID setSerialNumber = (*env)->GetMethodID(env, infoClass, "setSerialNumber", "(Ljava/lang/String;)V");
	jmethodID setPartNumber = (*env)->GetMethodID(env, infoClass, "setPartNumber", "(Ljava/lang/String;)V");
	jmethodID setCapabilities = (*env)->GetMethodID(env, infoClass, "setCapabilities", "(I)V");
	jmethodID setMinFrequency = (*env)->GetMethodID(env, infoClass, "setMinFrequency", "(J)V");
	jmethodID setMaxFrequency = (*env)->GetMethodID(env, infoClass, "setMaxFrequency", "(J)V");
	jmethodID setRfPortCount = (*env)->GetMethodID(env, infoClass, "setRfPortCount", "(I)V");
	jmethodID setSampleTypes = (*env)->GetMethodID(env, infoClass, "setSampleTypes", "(I)V");
	jmethodID setGpioCount = (*env)->GetMethodID(env, infoClass, "setGpioCount", "(I)V");
	jmethodID setMaxSafeTemp = (*env)->GetMethodID(env, infoClass, "setMaxSafeTemperature", "(F)V");
	jmethodID setCurrentSampleRate = (*env)->GetMethodID(env, infoClass, "setCurrentSampleRate", "(I)V");
	jmethodID setCurrentBandwidth = (*env)->GetMethodID(env, infoClass, "setCurrentBandwidth", "(I)V");
	jmethodID setCurrentHwSampleRate = (*env)->GetMethodID(env, infoClass, "setCurrentHwSampleRate", "(I)V");
	jmethodID setCurrentDecimationFactor = (*env)->GetMethodID(env, infoClass, "setCurrentDecimationFactor", "(I)V");

	(*env)->CallVoidMethod(env, jinfo, setBoardId, (jint)info.board_id);

	/* Helper: set a string field, safely handling NewStringUTF OOM */
#define SET_STRING_FIELD(setter, cstr) do {                           \
		jstring _js = (*env)->NewStringUTF(env, (cstr));              \
		if (_js) {                                                    \
			(*env)->CallVoidMethod(env, jinfo, (setter), _js);        \
			(*env)->DeleteLocalRef(env, _js);                         \
		}                                                             \
	} while (0)

	SET_STRING_FIELD(setBoardName, info.board_name);
	SET_STRING_FIELD(setFirmwareVersion, info.firmware_version);

	/* Format serial number - 64-bit like hydrasdr host tools: 0xMSB32LSB32 */
	char serial_str[64];
	uint32_t sn_msb = info.part_serial.serial_no[2];
	uint32_t sn_lsb = info.part_serial.serial_no[3];
	snprintf(serial_str, sizeof(serial_str), "0x%08X%08X", sn_msb, sn_lsb);
	SET_STRING_FIELD(setSerialNumber, serial_str);

	char part_str[32];
	snprintf(part_str, sizeof(part_str), "%08X%08X",
		info.part_serial.part_id[1],
		info.part_serial.part_id[0]);
	SET_STRING_FIELD(setPartNumber, part_str);

#undef SET_STRING_FIELD

	(*env)->CallVoidMethod(env, jinfo, setCapabilities, (jint)info.features);
	(*env)->CallVoidMethod(env, jinfo, setMinFrequency, (jlong)info.min_frequency);
	(*env)->CallVoidMethod(env, jinfo, setMaxFrequency, (jlong)info.max_frequency);
	(*env)->CallVoidMethod(env, jinfo, setRfPortCount, (jint)info.rf_port_count);
	(*env)->CallVoidMethod(env, jinfo, setSampleTypes, (jint)info.sample_types);
	(*env)->CallVoidMethod(env, jinfo, setGpioCount, (jint)info.gpio_count);
	(*env)->CallVoidMethod(env, jinfo, setMaxSafeTemp, (jfloat)info.max_safe_temp_celsius);
	(*env)->CallVoidMethod(env, jinfo, setCurrentSampleRate, (jint)info.current_samplerate);
	(*env)->CallVoidMethod(env, jinfo, setCurrentBandwidth, (jint)info.current_bandwidth);
	(*env)->CallVoidMethod(env, jinfo, setCurrentHwSampleRate, (jint)info.current_hw_samplerate);
	(*env)->CallVoidMethod(env, jinfo, setCurrentDecimationFactor, (jint)info.current_decimation_factor);

	return jinfo;
}

/* ==================== RF Control ==================== */

JNIEXPORT jint JNICALL
Java_io_github_dsheirer_source_tuner_hydrasdr_HydraSdrNative_setBiasT(
	JNIEnv *env, jclass cls, jlong handle, jboolean enable)
{
	CHECK_DEV(handle, HYDRASDR_ERROR_INVALID_PARAM);
	return hydrasdr_set_rf_bias(dev, enable ? 1 : 0);
}

JNIEXPORT jint JNICALL
Java_io_github_dsheirer_source_tuner_hydrasdr_HydraSdrNative_setRfPort(
	JNIEnv *env, jclass cls, jlong handle, jint port)
{
	CHECK_DEV(handle, HYDRASDR_ERROR_INVALID_PARAM);
	return hydrasdr_set_rf_port(dev, (hydrasdr_rf_port_t)port);
}

JNIEXPORT jfloat JNICALL
Java_io_github_dsheirer_source_tuner_hydrasdr_HydraSdrNative_getTemperature(
	JNIEnv *env, jclass cls, jlong handle)
{
	CHECK_DEV(handle, (jfloat)NAN);
	hydrasdr_temperature_t temp;

	int ret = hydrasdr_get_temperature(dev, &temp);
	if (ret != HYDRASDR_SUCCESS)
		return (jfloat)NAN;

	return (jfloat)temp.temperature_celsius;
}

JNIEXPORT jint JNICALL
Java_io_github_dsheirer_source_tuner_hydrasdr_HydraSdrNative_reset(
	JNIEnv *env, jclass cls, jlong handle)
{
	CHECK_DEV(handle, HYDRASDR_ERROR_INVALID_PARAM);
	return hydrasdr_reset(dev);
}

/* ==================== Utility ==================== */

JNIEXPORT jstring JNICALL
Java_io_github_dsheirer_source_tuner_hydrasdr_HydraSdrNative_errorName(
	JNIEnv *env, jclass cls, jint errorCode)
{
	const char *name = hydrasdr_error_name((enum hydrasdr_error)errorCode);
	if (!name)
		name = "UNKNOWN";
	return (*env)->NewStringUTF(env, name);
}

/* ==================== JNI Lifecycle ==================== */

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved)
{
	(void)vm;
	(void)reserved;
#ifdef _WIN32
	InitializeCriticalSection(&g_table_cs);
#endif
	return JNI_VERSION_1_6;
}
