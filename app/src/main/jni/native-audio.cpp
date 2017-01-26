//
// Created by fxpa72 on 5/31/2016.
//

#include "native-audio.h"
#include "WaveTableOsc.h"
#include "oscTest.h"
#include "StochasticDelayLineGranulator.h"
#include <vector>

// engine interfaces
static SLObjectItf engineObject = NULL;
static SLEngineItf engineEngine;

// output mix interfaces
static SLObjectItf outputMixObject = NULL;

// buffer queue player interfaces
static SLObjectItf bqPlayerObject = NULL;
static SLPlayItf bqPlayerPlay;
static SLAndroidSimpleBufferQueueItf bqPlayerBufferQueue;
static SLPlaybackRateItf playRate;

static SLVolumeItf bqPlayerVolume;
static jint   bqPlayerBufSize = 0;
static int bqSampRate = 0;

// recorder interfaces
static SLObjectItf recorderObject = NULL;
static SLRecordItf recorderRecord;
static SLAndroidSimpleBufferQueueItf recorderBufferQueue;


// buffer data
#define BUFFERFRAMES 441
#define SAMPLINGRATE SL_SAMPLINGRATE_44_1
short outputBuffer[BUFFERFRAMES];
short inputBuffer[BUFFERFRAMES];


// oscillator data
int amp = 32768;
const double twopi = 2.0f * M_PI;
double carrierFreq = 440.0f;
double lowFreq = 0.0f;
double phase = 0.0f;
double a = 0.0f;
double cut_freq = 0.0f;
jboolean pwr = JNI_FALSE;
int note_num = 0;
int cycles = 0;
// thread locks
void* inlock;
void* outlock;

// synthesized sawtooth clip
#define SAWTOOTH_FRAMES 441
//static float sawtoothBuffer[SAWTOOTH_FRAMES];

//CHANGES
#define numSecs (30)      /* length of sound file to generate (seconds) */
#define sweepSamples numSecs * 44100
#define synthSamples 441
#define grainSamples 64
//#define recordSamples 44100
//#define outSamples 44100
double sawSweepBuffer[sweepSamples];
float sawSynthBuffer[synthSamples];
float* outBuffer;
static float* recorderBuffer;
static volatile int  bqPlayerRecorderBusy = 0;

float sawtoothBuffer[synthSamples];

//GRAIN
StochasticDelayLineGranulator *granulator;
int maxGrains = 100;
double maxDelaySeconds = 4; //allocation error at > 5 sec
float* grainBuffer;

//OSCILLATOR
WaveTableOsc *osc;

#define constantRatioLimit (99999)    /* set to a large number (greater than or equal to the length
                                      of the lowest octave table) for constant table size; set to
                                      0 for a constant oversampling ratio */



void mallocTest () {
    int i;
    for (i = 1; i <= 10; i++) {
        maxDelaySeconds = i;
        __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG,"maxDelaySeconds: %d\n", i);
        granulator = new StochasticDelayLineGranulator(maxGrains, maxDelaySeconds, SAMPLINGRATE);

        //if allocation exception, print error
        //print size of allocation
        //__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG,"granulator size: %d\n\n", sizeof(granulator));
        delete granulator;
        granulator = NULL;
    }

    return;
}

// synthesize a mono sawtooth wave and place it into a buffer (called automatically on load)
__attribute__((constructor)) static void onDlOpen(void)
{
    unsigned i;
    for (i = 0; i < SAWTOOTH_FRAMES; ++i) {
        sawtoothBuffer[i] = 32768 - ((i % 100) * 660);
    }

    float startFreq = 55.0;
    osc = new WaveTableOsc();
    //testSawSweep(sawSweepBuffer, osc);
    //testPWM(sawSweepBuffer, osc);
    //testThreeOsc(sawSweepBuffer);
    updateSawSynth(sawSynthBuffer, osc, startFreq);
    for (int i = 0; i < sweepSamples; i++) {
        //sawSweepBuffer[i] = (short)(sawSweepBuffer[i]*32768) ;
    }

    for (int i = 0; i < grainSamples; i++) {
        //outBuffer[i] = (short)(sawSynthBuffer[i]*32768) ;
    }

    //__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "bufSize open: %d", bqPlayerBufSize);
    //outBuffer = new float[bqPlayerBufSize];
    //recorderBuffer = new float[bqPlayerBufSize];
    //granulator = new StochasticDelayLineGranulator(maxGrains, maxDelaySeconds, bqSampRate);

    granulator = new StochasticDelayLineGranulator(maxGrains, maxDelaySeconds, SL_SAMPLINGRATE_48);
/*
    granulator->interonsetTime(.0001, 2.);
    granulator->grainDuration(.001, 1.);
    granulator->delayTime(0, 0);
    granulator->playbackRate(.1, 10.);
    granulator->amplitude(0., 1.);
    granulator->sustain(0., 1.);
    granulator->skew(-1., 1.);
    granulator->feedback(.95);
*/


    granulator->interonsetTime(.01, .011);
    granulator->grainDuration(.048, .05);
    granulator->delayTime(0, 0); //0.1, 0.1
    granulator->playbackRate(1, 1);
    granulator->amplitude(.3, .3);
    granulator->sustain(.5, .5);
    granulator->skew(0, 0);
    granulator->feedback(0);

/*
    granulator->interonsetTime(.01, .011);
    granulator->grainDuration(.048, .05);
    granulator->delayTime(0, 0);
    granulator->playbackRate(1, 1);
    granulator->amplitude(.9, .9);
    granulator->sustain(.9, .9);
    granulator->skew(0, 0);
    granulator->feedback(0);
*/
    //mallocTest();
}



extern "C" void Java_kharico_granularsynthesizer_MainActivity_oscillatorOn (JNIEnv* env, jclass clazz, jboolean On)
{
    pwr = On;
    if (pwr) {
        __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG,"Power ON");
    }
    else {
        __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG,"Power OFF");
    }
};

extern "C" void Java_kharico_granularsynthesizer_MainActivity_freqChange (JNIEnv* env, jclass clazz, jdouble sliderVal)
{
    //lowFreq = 0.0f + 440.0f * sliderVal;
    //carrierFreq = 440.0f + 440.0f * sliderVal;
    //a = 0.0f + 0.99f * sliderVal;
    //cut_freq = 0.0f + 22050.0f * sliderVal;
    //_android_log_print(ANDROID_LOG_DEBUG, LOG_TAG,"freq: %f", cut_freq);
    carrierFreq = 55.0f + 825.0f * sliderVal;
    //updateSawSynth(sawSynthBuffer, osc, carrierFreq);

    granulator->amplitude(0.f, 1.0*sliderVal);
    __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG,"amp: %f", sliderVal);

    for (int i = 0; i < grainSamples; i++) {
        //outBuffer[i] = (short)(sawSynthBuffer[i]*32768) ;
    }
}


float* filterAudio( StochasticDelayLineGranulator* filter, float in[]){
    static const int length = grainSamples;
    float input [length], output[length];

    for (int i = 0; i < length; i++) {
        __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "grain: %f", in[i]);
    }

    std::fill_n( output, length, 0.f);  // zero output
    //for (int i = 0; i < synthSamples; i++) {
    //    output[i] = input[i];
    //}
    filter->synthesize( output, in, length );

    float *grains = output;
    return grains;
}

/* freq = fundamental frequency
 * j = index
 * k = harmonic
 */
double sinWaveGenerator(double freq, uint j, uint k) {
    phase += twopi * j * k * freq / SAMPLINGRATE;
    if (phase > twopi) {
        phase -= twopi;
    }
    return sin(phase);
}

// Discrete Summation Formula
double DSF (double x,   // input
            double a,   // a<1.0
            double N,   // N<SmpleFQ/2
            double fi ) // phase
{
    double s1 = pow(a, N-1.0) * sin((N-1.0)*x+fi);
    double s2 = pow(a,N) * sin(N*x+fi);
    double s3 = a * sin(x+fi);
    double s4 = 1.0 - (2*a*cos(x)) + (a*a);
    if (s4 == 0)
        return 0;
    else
        return (sin(fi) - s3 - s2 + s1)/s4;
}

//Low Pass Filter
short LPF(short *in, short *out, double cutoff) {
    double resonance = 0.1;

    double c = 1.0 / tan(M_PI * cutoff / SAMPLINGRATE);
    double a1 = 1.0 / ( 1.0 + resonance * c + c * c);
    double a2 = 2* a1;
    double a3 = a1;
    double b1 = 2.0 * ( 1.0 - c*c) * a1;
    double b2 = ( 1.0 - resonance * c + c * c) * a1;

    short filterOut = (short) (a1 * (*(in)) + a2 * (*(in-1)) + a3 * (*(in-2)) - b1 * (*(out-1)) - b2 * (*(out-2)));
    return filterOut;
}

// this callback handler is called every time a buffer finishes playing
void bqPlayerCallback(SLAndroidSimpleBufferQueueItf bq, void *context)
{
    assert(bq == bqPlayerBufferQueue);
    assert(NULL == context);

    notifyThreadLock(inlock);
    notifyThreadLock(outlock);
}


// this callback handler is called every time a buffer finishes recording
void bqRecorderCallback(SLAndroidSimpleBufferQueueItf bq, void *context)
{

    assert(bq == recorderBufferQueue);
    assert(NULL == context);

    waitThreadLock(inlock);

    // signal processing
    unsigned int j;
    //phase = 0.0f;
    unsigned int nanCount = 0;

    if (pwr) {
        //grainBuffer = filterAudio(granulator, sawSynthBuffer);
        grainBuffer = filterAudio(granulator, recorderBuffer);
        for (int i = 0; i < grainSamples; i++) {
            //outBuffer[i] = (short)(grainBuffer[i]*32768);
            outBuffer[i] = grainBuffer[i];
            //__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "Grain: %f", outBuffer[i]);
            /*
            if (__isnanf(outBuffer[i])) {
                nanCount++;
            }
            */
        }
        //__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "GrainNaNCount: %d", nanCount);
    }
    else {
        for (int i = 0; i < bqPlayerBufSize; i++) {
            //outBuffer[i] = (short)(32768*recorderBuffer[i]);
            outBuffer[i] = recorderBuffer[i];
            //__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "Record: %f", outBuffer[i]);
            /*
            if (__isnanf(recorderBuffer[i])) {
                nanCount++;
            }
            */
        }
        //__android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "NaNCount: %d", nanCount);
    }

    //for (j = 0; j < BUFFERFRAMES; j++) {
        //if (pwr) {

            /*
            inputBuffer[j] = (short) ((amp/2 * sinWaveGenerator(261.626)) +
                    (amp/4 * sinWaveGenerator(329.628)) + (amp/8 * sinWaveGenerator(391.995)) +
                    (amp/8 * sinWaveGenerator(523.251)));
            */
            //inputBuffer[j] = (short) ((amp * sinWaveGenerator(carrierFreq, j)) * sinWaveGenerator(lowFreq, j));
            //inputBuffer[j] = (short) ((amp * sinWaveGenerator(carrierFreq * sinWaveGenerator(lowFreq, j), j)));

            //double x0 = sinWaveGenerator(carrierFreq, j, 1);
            //double x1 = sinWaveGenerator(carrierFreq, j, 2);
            //double x2 = sinWaveGenerator(carrierFreq, j, 3);
            //double x3 = sinWaveGenerator(carrierFreq, j, 4);

            /*
            double d0 = DSF(x0, a, carrierFreq, phase);
            double d1 = DSF(x1, a, carrierFreq, phase);
            double d2 = DSF(x2, a, carrierFreq, phase);
            double d3 = DSF(x3, a, carrierFreq, phase);
            inputBuffer[j] = (short) (((3*amp)/4 * d0) + (amp/8 * d1) + (amp/12 * d2) + (amp/16 * d3));
            */

            //inputBuffer[j] = (short) (x0 * 32768);

            //No Effects
            //outputBuffer[j] = inputBuffer[j];

            //Low Pass Filter
            //short *in = &inputBuffer[j];
            //short *out = &outputBuffer[j];
            //outputBuffer[j] = LPF(in, out, cut_freq);
        //}
        //else {
            //outputBuffer[j] = 0;
        //}
    //}

    SLresult result;

    waitThreadLock(outlock);

    //result = (*bqPlayerBufferQueue)->Enqueue(bqPlayerBufferQueue, outputBuffer, BUFFERFRAMES);
    //result = (*bqPlayerBufferQueue)->Enqueue(bqPlayerBufferQueue, sawSweepBuffer, SAWTOOTH_FRAMES);

    result = (*recorderBufferQueue)->Enqueue(recorderBufferQueue, recorderBuffer, bqPlayerBufSize);
    //assert(SL_RESULT_SUCCESS == result);

/*
    for (int i = 0; i < grainSamples; i++) {
        if (recorderBuffer[i] != 0) {
            __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "sample: %d", recorderBuffer[i]);
        }
    }
*/  //CHANGE PLAYBACK RATE TO 1/2
    result = (*bqPlayerBufferQueue)->Enqueue(bqPlayerBufferQueue, outBuffer, bqPlayerBufSize);
    //assert(SL_RESULT_SUCCESS == result);

    // WORKING SYNTH
    // result = (*bqPlayerBufferQueue)->Enqueue(bqPlayerBufferQueue, outBuffer, grainSamples);



    //result = (*bqPlayerBufferQueue)->Enqueue(bqPlayerBufferQueue, sawSynthBuffer, synthSamples);
    bqPlayerRecorderBusy = 0;


    (void)result;
}

// create the engine and output mix objects
extern "C" void Java_kharico_granularsynthesizer_MainActivity_createEngine(JNIEnv* env, jclass clazz)
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
extern "C" void Java_kharico_granularsynthesizer_MainActivity_createBufferQueueAudioPlayer(JNIEnv* env,
                                                                      jclass clazz, jint sampleRate, jint bufSize)
{
    __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG,"BufferQueue");
    SLresult result;
    if (sampleRate >= 0 && bufSize >= 0 ) {
        bqPlayerBufSize = 64;
        __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "bufSize: %d", bqPlayerBufSize);
        __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "sampleRate: %d", sampleRate);
        bqSampRate = sampleRate;
    }
    outBuffer = new float[bqPlayerBufSize];
    recorderBuffer = new float[bqPlayerBufSize];
    // configure audio source
    SLDataLocator_AndroidSimpleBufferQueue loc_bufq = {SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE, 2};
    SLDataFormat_PCM format_pcm = {SL_DATAFORMAT_PCM, 1, SL_SAMPLINGRATE_48,
                                   SL_PCMSAMPLEFORMAT_FIXED_16, SL_PCMSAMPLEFORMAT_FIXED_16,
                                   SL_SPEAKER_FRONT_CENTER, SL_BYTEORDER_LITTLEENDIAN};

    //format_pcm.samplesPerSec = (SLuint32)sampleRate;
    SLDataSource audioSrc = {&loc_bufq, &format_pcm};

    // configure audio sink
    SLDataLocator_OutputMix loc_outmix = {SL_DATALOCATOR_OUTPUTMIX, outputMixObject};
    SLDataSink audioSnk = {&loc_outmix, NULL};

    // create audio player
    const SLInterfaceID ids[3] = {SL_IID_ANDROIDSIMPLEBUFFERQUEUE, SL_IID_VOLUME, SL_IID_PLAYBACKRATE};
    const SLboolean req[3] = {SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE, SL_BOOLEAN_TRUE};
    result = (*engineEngine)->CreateAudioPlayer(engineEngine, &bqPlayerObject, &audioSrc, &audioSnk,
                                                3, ids, req);
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

    // get playback rate interface
    result = (*bqPlayerObject)->GetInterface(bqPlayerObject,
                                             SL_IID_PLAYBACKRATE, &playRate);
    assert(SL_RESULT_SUCCESS == result);
    (void)result;
    // set playback rate
    //SLpermille  pRate;
    //SLpermille  newRate = (SLpermille) 1000;
    //(*playRate)->SetRate(playRate, newRate);
    //(*playRate)->GetRate(playRate, &pRate);

    // get the volume interface
    result = (*bqPlayerObject)->GetInterface(bqPlayerObject, SL_IID_VOLUME, &bqPlayerVolume);
    assert(SL_RESULT_SUCCESS == result);
    (void)result;

    // set the player's state to playing
    result = (*bqPlayerPlay)->SetPlayState(bqPlayerPlay, SL_PLAYSTATE_PLAYING);
    assert(SL_RESULT_SUCCESS == result);
    (void)result;

    /*
    unsigned i;
    for (i = 0; i < BUFFERFRAMES; i++) {
        inputBuffer[i] = (short) (amp * sinWaveGenerator(carrierFreq, i, 1));
        //
    }

    i = 0, phase = 0.0f;
    for (i = 0; i < BUFFERFRAMES; i++) {
        outputBuffer[i] = (short) (amp * sinWaveGenerator(carrierFreq, i, 1));
        //phase += twopi * freq / SAMPLINGRATE;
    }
    */
    result = (*bqPlayerBufferQueue)->Enqueue(bqPlayerBufferQueue, outBuffer, bqPlayerBufSize);
    assert(SL_RESULT_SUCCESS == result);
    (void)result;

}

// create audio recorder
extern "C" void Java_kharico_granularsynthesizer_MainActivity_createAudioRecorder(JNIEnv* env, jclass clazz)
{
    SLresult result;

    // configure audio source
    SLDataLocator_IODevice loc_dev = {SL_DATALOCATOR_IODEVICE, SL_IODEVICE_AUDIOINPUT,
                                      SL_DEFAULTDEVICEID_AUDIOINPUT, NULL};
    SLDataSource audioSrc = {&loc_dev, NULL};

    // configure audio sink
    SLDataLocator_AndroidSimpleBufferQueue loc_bq = {SL_DATALOCATOR_ANDROIDSIMPLEBUFFERQUEUE, 2};
    SLDataFormat_PCM format_pcm = {SL_DATAFORMAT_PCM, 1, SL_SAMPLINGRATE_48,
                                   SL_PCMSAMPLEFORMAT_FIXED_16, SL_PCMSAMPLEFORMAT_FIXED_16,
                                   SL_SPEAKER_FRONT_CENTER, SL_BYTEORDER_LITTLEENDIAN};

    SLDataSink audioSnk = {&loc_bq, &format_pcm};

    // create audio recorder
    // (requires the RECORD_AUDIO permission)
    const SLInterfaceID id[1] = {SL_IID_ANDROIDSIMPLEBUFFERQUEUE};
    const SLboolean req[1] = {SL_BOOLEAN_TRUE};
    result = (*engineEngine)->CreateAudioRecorder(engineEngine, &recorderObject, &audioSrc,
                                                  &audioSnk, 1, id, req);

    // realize the audio recorder
    result = (*recorderObject)->Realize(recorderObject, SL_BOOLEAN_FALSE);

    // get the record interface
    result = (*recorderObject)->GetInterface(recorderObject, SL_IID_RECORD, &recorderRecord);
    assert(SL_RESULT_SUCCESS == result);
    (void)result;

    // get the buffer queue interface
    result = (*recorderObject)->GetInterface(recorderObject, SL_IID_ANDROIDSIMPLEBUFFERQUEUE,
                                             &recorderBufferQueue);
    assert(SL_RESULT_SUCCESS == result);
    (void)result;

    // register callback on the buffer queue
    result = (*recorderBufferQueue)->RegisterCallback(recorderBufferQueue, bqRecorderCallback,
                                                      NULL);
    assert(SL_RESULT_SUCCESS == result);
    (void)result;
}

// set the recording state for the audio recorder
extern "C" void Java_kharico_granularsynthesizer_MainActivity_startRecording(JNIEnv* env, jclass clazz)
{
    SLresult result;

    if( bqPlayerRecorderBusy) {
        return;
    }
    // in case already recording, stop recording and clear buffer queue
    result = (*recorderRecord)->SetRecordState(recorderRecord, SL_RECORDSTATE_STOPPED);
    assert(SL_RESULT_SUCCESS == result);
    (void)result;
    result = (*recorderBufferQueue)->Clear(recorderBufferQueue);
    assert(SL_RESULT_SUCCESS == result);
    (void)result;

    // enqueue an empty buffer to be filled by the recorder
    // (for streaming recording, we would enqueue at least 2 empty buffers to start things off)
    result = (*recorderBufferQueue)->Enqueue(recorderBufferQueue, recorderBuffer, bqPlayerBufSize);
    result = (*recorderBufferQueue)->Enqueue(recorderBufferQueue, recorderBuffer, bqPlayerBufSize);
    // the most likely other result is SL_RESULT_BUFFER_INSUFFICIENT,
    // which for this code example would indicate a programming error
    assert(SL_RESULT_SUCCESS == result);
    (void)result;

    // start recording
    result = (*recorderRecord)->SetRecordState(recorderRecord, SL_RECORDSTATE_RECORDING);
    assert(SL_RESULT_SUCCESS == result);
    (void)result;
    bqPlayerRecorderBusy = 1;
}

extern "C" void Java_kharico_granularsynthesizer_MainActivity_stopRecording(JNIEnv* env, jclass clazz)
{
    SLresult result;

    if( bqPlayerRecorderBusy) {
        return;
    }
    // in case already recording, stop recording and clear buffer queue
    result = (*recorderRecord)->SetRecordState(recorderRecord, SL_RECORDSTATE_STOPPED);
    assert(SL_RESULT_SUCCESS == result);
    (void)result;
    result = (*recorderBufferQueue)->Clear(recorderBufferQueue);
    assert(SL_RESULT_SUCCESS == result);
    (void)result;

    // enqueue an empty buffer to be filled by the recorder
    // (for streaming recording, we would enqueue at least 2 empty buffers to start things off)
    result = (*recorderBufferQueue)->Enqueue(recorderBufferQueue, recorderBuffer, bqPlayerBufSize);
    // the most likely other result is SL_RESULT_BUFFER_INSUFFICIENT,
    // which for this code example would indicate a programming error
    assert(SL_RESULT_SUCCESS == result);
    (void)result;

    // start recording
    result = (*recorderRecord)->SetRecordState(recorderRecord, SL_RECORDSTATE_RECORDING);
    assert(SL_RESULT_SUCCESS == result);
    (void)result;
    bqPlayerRecorderBusy = 1;
}

// shut down the native audio system
extern "C" void Java_kharico_granularsynthesizer_MainActivity_shutdown(JNIEnv* env, jclass clazz)
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