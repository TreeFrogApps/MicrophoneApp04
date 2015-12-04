package com.treefrogapps.microphoneapp04;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.treefrogapps.microphoneapp04.fftpack.Complex1D;
import com.treefrogapps.microphoneapp04.fftpack.ComplexDoubleFFT;


public class SoundRecorderFragment extends Fragment {

    public static final String FRAGMENT_TAG = "com.treefrogapps.microphoneapp02.asynctaskfragment_tag";

    private static final String AUDIO_TAG = "AUDIO_RECORD_TAG";

    private FragmentCallBacks mFragmentCallBacks;

    private AudioRecord mAudioRecord;
    private Thread mAudioRecordThread;
    protected static int mSampleRate = 44100;
    private int mAudioRecordBufferSize;
    // 16-bit signed array
    private short[] mAudioRecordBuffer;
    private volatile boolean isRecording;

    private long mSystemTime;
    private int mCounter = 0;
    private long updateRate = 100L; // 10th of a second


    // 1024 'Bins' - each bin roughly 44hz (44,100(sample rate) / 1024 = 44)
    // 1024 bins equals 512 frequency ranges
    // bin fill time for 1024 bins : 1024 / 44,100 = 0.0232199 seconds
    // so updates per second could be 43/44 (1 / 0.0232199 = 43.07 bin fills in a second)
    // more FFTBins, the longer the fill time, but smaller frequency bin size - more accurate
    public static int FFTBuffer = 1024;


    public interface FragmentCallBacks {
        void onPostExecuteCallBack(double[] frequencyBins);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof SoundRecorderFragment.FragmentCallBacks) {
            mFragmentCallBacks = (SoundRecorderFragment.FragmentCallBacks) context;
        } else {
            throw new ClassCastException(context.toString() + " must implement callback methods");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mFragmentCallBacks = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);

        // get min buffer size
        mAudioRecordBufferSize = AudioRecord.getMinBufferSize(mSampleRate, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT);

        Log.e("MIN AUDIO SHORT ARRAY SIZE", String.valueOf(mAudioRecordBufferSize));

        // create audio record object
        mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, mSampleRate, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, mAudioRecordBufferSize);


    }

    public void startRecording() {


        isRecording = true;

        mAudioRecordThread = new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    captureAudio();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        mAudioRecordThread.setPriority(Thread.MAX_PRIORITY);
        mAudioRecordThread.start();

        mSystemTime = System.currentTimeMillis();

    }

    private void captureAudio() throws InterruptedException {

        // byte array to hold audio buffer
        mAudioRecordBuffer = new short[FFTBuffer];

        if (mAudioRecord == null || mAudioRecord.getState() != AudioRecord.STATE_INITIALIZED){
            mAudioRecord = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, mSampleRate, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, mAudioRecordBufferSize);
        }

            mAudioRecord.startRecording();

            // while loop whilst record state is 'recording'
            while (isRecording) {

                if(Thread.interrupted()){
                    throw new InterruptedException();
                }

                // read audio data into mAudioRecordBuffer, and return status
                int status = mAudioRecord.read(mAudioRecordBuffer, 0, FFTBuffer);

                if (status == AudioRecord.ERROR_INVALID_OPERATION || status == AudioRecord.ERROR_BAD_VALUE) {
                    Log.e(AUDIO_TAG, "Error reading audio data!");
                    return;
                }

                mCounter++;

                if (mFragmentCallBacks != null && (mSystemTime + updateRate) <= System.currentTimeMillis()) {
                    // calculate FFT
                    mFragmentCallBacks.onPostExecuteCallBack(calculateFFT(mAudioRecordBuffer));

                    mCounter = 0;
                    mSystemTime = System.currentTimeMillis();
                }
            }

            // if isRecording returns false then it will be because 'stop' has been pressed so code
            // will come out of while loop - close buffered stream

            mAudioRecord.stop();

    }

    private double[] calculateFFT(short[] audioBufferInput) {

        /** Frequency Bins
         *
         *  0:   0 * 44100 / 1024 =     0.0 Hz
         *  1:   1 * 44100 / 1024 =    43.1 Hz
         *  2:   2 * 44100 / 1024 =    86.1 Hz
         *  3:   3 * 44100 / 1024 =   129.2 Hz
         *  4: ...
         *  5: ...
         *  ...
         *  511: 511 * 44100 / 1024 = 22006.9 Hz
         *
         */

        ComplexDoubleFFT complexDoubleFFT = new ComplexDoubleFFT(FFTBuffer);

        Complex1D complex1D = new Complex1D();
        complex1D.x = new double[FFTBuffer];
        complex1D.y = new double[FFTBuffer];

        // Complex1D array to hold return values
        // each value should return between 0 and 1

        for (int i = 0; i < FFTBuffer; i++) {
            // max signed 16 bit value to get a vale between 0 and 1 - will get 1024 samples (512 FFT Bins)
            complex1D.x[i] = (double) ((audioBufferInput[i]) / 32768.0F); // real
            complex1D.y[i] = 0.0; // imaginary
        }

        complexDoubleFFT.ft(complex1D);

        double[] frequencyMagnitudes = new double[FFTBuffer];

        int peakBinPosition = 0;
        double peakBinValue = 0.0;

        for (int i = 0; i < FFTBuffer / 2; i++) {
            // work out the square root of the sum of powers of the complex numbers (real + imaginary)
            double R = Math.pow(complex1D.x[i], 2);
            double I = Math.pow(complex1D.y[i], 2);

            frequencyMagnitudes[i] = Math.sqrt(R + I);

            // detect highest frequency - for testing
            if (frequencyMagnitudes[i] > peakBinValue) {
                peakBinValue = frequencyMagnitudes[i];
                peakBinPosition = i;
            }
        }

        Log.i("PEAK VALUE " + String.valueOf(peakBinPosition),
                String.valueOf(peakBinPosition * mSampleRate / FFTBuffer) + " - " + String.valueOf((peakBinPosition + 1) * mSampleRate / FFTBuffer)
                        + " : " + String.valueOf(peakBinValue));

        return frequencyMagnitudes;
    }

    public void stopRecording() {
        isRecording = false;
    }

    public void releaseAudioRecord() {

        if (mAudioRecord != null) {
            mAudioRecord.stop();
            mAudioRecord.release();
        }
    }
}
