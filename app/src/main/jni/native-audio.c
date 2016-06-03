//
// Created by fxpa72 on 5/31/2016.
//

#include "native-audio.h"

// engine interfaces
static SLObjectItf engineObject = NULL;
static SLEngineItf engineEngine;

// output mix interfaces
static SLObjectItf outputMixObject = NULL;

// buffer queue player interfaces
static SLObjectItf bqPlayerObject = NULL;
static SLPlayItf bqPlayerPlay;
static SLAndroidSimpleBufferQueueItf bqPlayerBufferQueue;

static SLVolumeItf bqPlayerVolume;
static jint   bqPlayerBufSize = 0;

// Oscillator player interfaces
static SLObjectItf oscPlayerObject = NULL;
static SLPlayItf oscPlayerPlay;
static SLAndroidSimpleBufferQueueItf recorderBufferQueue;

// buffer data
#define BUFFERFRAMES 480
#define SAMPLINGRATE SL_SAMPLINGRATE_44_1
short outputBuffer[BUFFERFRAMES];
short inputBuffer[BUFFERFRAMES];

// oscillator data
int amp = 10000;
const double twopi = 2.0f * M_PI;
double freq = 440.0f;
//double sliderVal = 0.0f;
double phase = 0.0f;
jboolean pwr = JNI_FALSE;

// thread locks
void* inlock;
void* outlock;

void Java_kharico_granularsynthesizer_MainActivity_oscillatorOn (JNIEnv* env, jclass clazz, jboolean On)
{
    pwr = On;
    if (pwr) {
        __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG,"Power ON");
    }
    else {
        __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG,"Power OFF");
    }
};

void Java_kharico_granularsynthesizer_MainActivity_freqChange (JNIEnv* env, jclass clazz, jdouble sliderVal)
{
    freq = 440.0f + 440.0f * sliderVal;
    __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG,"freq: %f", freq);
}

// this callback handler is called every time a buffer finishes recording
void bqPlayerCallback(SLAndroidSimpleBufferQueueItf bq, void *context)
{
    notifyThreadLock(inlock);
    notifyThreadLock(outlock);

    assert(bq == bqPlayerBufferQueue);
    assert(NULL == context);

    waitThreadLock(inlock);

    // signal processing
    unsigned int j;
    //phase = 0.0f;
    for (j = 0; j < BUFFERFRAMES; j++) {
        if (pwr) {
            double w = twopi * freq / SL_SAMPLINGRATE_44_1;

            //inputBuffer[j] = (short) (amp * sin(phase));
            inputBuffer[j] = (short) sin(w * j + phase);
            //phase += twopi * freq / SL_SAMPLINGRATE_44_1;

            outputBuffer[j] = inputBuffer[j];
        }
        else {
            outputBuffer[j] = 0;
        }
    }

    SLresult result;

    waitThreadLock(outlock);

    result = (*bqPlayerBufferQueue)->Enqueue(bqPlayerBufferQueue, outputBuffer, BUFFERFRAMES);
    assert(SL_RESULT_SUCCESS == result);
    (void)result;
}

// create the engine and output mix objects
void Java_kharico_granularsynthesizer_MainActivity_createEngine(JNIEnv* env, jclass clazz)
{
    __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG,"Engine");
    inlock = createThreadLock();
    outlock = createThreadLock();

    SLresult result;

    // create engine
    result = slCreateEngine(&engineObject, 0, NULL, 0, NULL, NULL);
    assert(SL_RESULT_SUCCESS == result);
    (void)result;

    // realize the engine
    result = (*engineObject)->Realize(engineObject, SL_BOOLEAN_FALSE);
    assert(SL_RESULT_SUCCESS == result);
    (void)result;

    // get the engine interface, which is needed in order to create other objects
    result = (*engineObject)->GetInterface(engineObject, SL_IID_ENGINE, &engineEngine);
    assert(SL_RESULT_SUCCESS == result);
    (void)result;

    // create output mix, with environmental reverb specified as a non-required interface
    const SLInterfaceID ids[1] = {SL_IID_VOLUME};
    const SLboolean req[1] = {SL_BOOLEAN_FALSE};
    result = (*engineEngine)->CreateOutputMix(engineEngine, &outputMixObject, 0, NULL, NULL);
    assert(SL_RESULT_SUCCESS == result);
    (void)result;

    // realize the output mix
    result = (*outputMixObject)->Realize(outputMixObject, SL_BOOLEAN_FALSE);
    assert(SL_RESULT_SUCCESS == result);
    (void)result;
}

// create buffer queue audio player
void Java_kharico_granularsynthesizer_MainActivity_createBufferQueueAudioPlayer(JNIEnv* env,
                                                                      jclass clazz, jint sampleRate, jint bufSize)
{
    __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG,"BufferQueue");
    SLresult result;
    if (sampleRate >= 0 && bufSize >= 0 ) {
        bqPlayerBufSize = bufSize;
    }

    // configure audio source
    SLDataLocator_AndroidSimpleBufferQueue loc_bufq = {SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE, 2};
    SLDataFormat_PCM format_pcm = {SL_DATAFORMAT_PCM, 1, SAMPLINGRATE,
                                   SL_PCMSAMPLEFORMAT_FIXED_16, SL_PCMSAMPLEFORMAT_FIXED_16,
                                   SL_SPEAKER_FRONT_CENTER, SL_BYTEORDER_LITTLEENDIAN};

    format_pcm.samplesPerSec = SL_SAMPLINGRATE_44_1;
    SLDataSource audioSrc = {&loc_bufq, &format_pcm};

    // configure audio sink
    SLDataLocator_OutputMix loc_outmix = {SL_DATALOCATOR_OUTPUTMIX, outputMixObject};
    SLDataSink audioSnk = {&loc_outmix, NULL};

    // create audio player
    const SLInterfaceID ids[2] = {SL_IID_ANDROIDSIMPLEBUFFERQUEUE, SL_IID_VOLUME};
    const SLboolean req[2] = {SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE};
    result = (*engineEngine)->CreateAudioPlayer(engineEngine, &bqPlayerObject, &audioSrc, &audioSnk,
                                                2, ids, req);
    assert(SL_RESULT_SUCCESS == result);
    (void)result;

    // realize the player
    result = (*bqPlayerObject)->Realize(bqPlayerObject, SL_BOOLEAN_FALSE);
    assert(SL_RESULT_SUCCESS == result);
    (void)result;

    // get the play interface
    result = (*bqPlayerObject)->GetInterface(bqPlayerObject, SL_IID_PLAY, &bqPlayerPlay);
    assert(SL_RESULT_SUCCESS == result);
    (void)result;

    // get the buffer queue interface
    result = (*bqPlayerObject)->GetInterface(bqPlayerObject, SL_IID_BUFFERQUEUE,
                                             &bqPlayerBufferQueue);
    assert(SL_RESULT_SUCCESS == result);
    (void)result;

    // register callback on the buffer queue
    result = (*bqPlayerBufferQueue)->RegisterCallback(bqPlayerBufferQueue, bqPlayerCallback, NULL);
    assert(SL_RESULT_SUCCESS == result);
    (void)result;

    // get the volume interface
    result = (*bqPlayerObject)->GetInterface(bqPlayerObject, SL_IID_VOLUME, &bqPlayerVolume);
    assert(SL_RESULT_SUCCESS == result);
    (void)result;

    // set the player's state to playing
    result = (*bqPlayerPlay)->SetPlayState(bqPlayerPlay, SL_PLAYSTATE_PLAYING);
    assert(SL_RESULT_SUCCESS == result);
    (void)result;

    unsigned i;
    for (i = 0; i < BUFFERFRAMES; ++i) {
        inputBuffer[i] = (short) (amp * sin(phase));
        phase += twopi * freq / SAMPLINGRATE;
    }

    i = 0, phase = 0.0f;
    for (i = 0; i < BUFFERFRAMES; ++i) {
        outputBuffer[i] = (short) (amp * sin(phase));
        phase += twopi * freq / SAMPLINGRATE;
    }

    result = (*bqPlayerBufferQueue)->Enqueue(bqPlayerBufferQueue, outputBuffer, BUFFERFRAMES);
    assert(SL_RESULT_SUCCESS == result);
    (void)result;
}

// shut down the native audio system
void Java_kharico_granularsynthesizer_MainActivity_shutdown(JNIEnv* env, jclass clazz)
{
    __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG,"Shutdown");
    // destroy buffer queue audio player object, and invalidate all associated interfaces
    if (bqPlayerObject != NULL) {
        (*bqPlayerObject)->Destroy(bqPlayerObject);
        bqPlayerObject = NULL;
        bqPlayerPlay = NULL;
        bqPlayerBufferQueue = NULL;
        bqPlayerVolume = NULL;
    }

    // destroy output mix object, and invalidate all associated interfaces
    if (outputMixObject != NULL) {
        (*outputMixObject)->Destroy(outputMixObject);
        outputMixObject = NULL;
    }

    // destroy engine object, and invalidate all associated interfaces
    if (engineObject != NULL) {
        (*engineObject)->Destroy(engineObject);
        engineObject = NULL;
        engineEngine = NULL;
    }

}