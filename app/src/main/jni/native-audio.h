//
// Created by fxpa72 on 5/31/2016.
//

#ifndef GRANULARSYNTHESIZER_NATIVE_AUDIO_H
#define GRANULARSYNTHESIZER_NATIVE_AUDIO_H

#include <assert.h>
#include <jni.h>
#include <string.h>
#include <android/log.h>
#define LOG_TAG "buffer queue"
#include <pthread.h>
#include <unistd.h>
#include <time.h>
#include <math.h>
#include <stdlib.h>

// for native audio
#include <SLES/OpenSLES.h>
#include <SLES/OpenSLES_Android.h>

typedef struct threadLock_{
    pthread_mutex_t m;
    pthread_cond_t c;
    unsigned char s;
} threadLock;

void* createThreadLock(void) {

    threadLock *pp;
    pp = (threadLock*) malloc(sizeof(threadLock));

    if (pp == NULL) return NULL;

    memset(pp, 0, sizeof(threadLock));

    if (pthread_mutex_init(&(pp->m), (pthread_mutexattr_t*) NULL) != 0) {
        free((void*) pp);
        return NULL;
    }

    if (pthread_cond_init(&(pp->c), (pthread_condattr_t*) NULL ) != 0) {
        pthread_mutex_destroy(&(pp->m));
        free((void*) pp);
        return NULL;
    }

    pp->s = (unsigned char) 1;
    return pp;
}

int waitThreadLock(void *lock) {

    threadLock *pp;
    int retval = 0;
    pp = (threadLock*) lock;

    pthread_mutex_lock(&(pp->m));

    while (!pp->s) {
        pthread_cond_wait(&(pp->c), &(pp->m));
    }

    pp->s = (unsigned char) 0;
    pthread_mutex_unlock(&(pp->m));
}

void notifyThreadLock(void *lock) {

    threadLock *pp;
    pp = (threadLock*) lock;

    pthread_mutex_lock(&(pp->m));

    pp->s = (unsigned char) 1;
    pthread_cond_signal(&(pp->c));
    pthread_mutex_unlock(&(pp->m));
}

void destroyThreadLock(void *lock) {

    threadLock *pp;
    pp = (threadLock*) lock;

    if (pp == NULL) return;

    notifyThreadLock(pp);

    pthread_cond_destroy(&(pp->c));
    pthread_mutex_destroy(&(pp->m));
    free(pp);
}

#endif //GRANULARSYNTHESIZER_NATIVE_AUDIO_H
