//
// Created by Khari on 11/8/2016.
//

#ifndef GRANULARSYNTHESIZER_OSCTEST_H
#define GRANULARSYNTHESIZER_OSCTEST_H

#define myFloat double      /* float or double, to set the resolution of the FFT, etc. (the resulting wavetables are always float) */

// tests
void testPWM(double*, WaveTableOsc*);
void testThreeOsc(double*);
void testSawSweep(double*, WaveTableOsc*);
void updateSawSynth(float*, WaveTableOsc*, double freq);

void fft(int N, myFloat *ar, myFloat *ai);
void defineSawtooth(int len, int numHarmonics, myFloat *ar, myFloat *ai);
float makeWaveTable(WaveTableOsc *osc, int len, myFloat *ar, myFloat *ai, myFloat scale, double topFreq);
void setSawtoothOsc(WaveTableOsc *osc, float baseFreq);



#endif //GRANULARSYNTHESIZER_OSCTEST_H
