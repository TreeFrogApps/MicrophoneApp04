package com.treefrogapps.microphoneapp04;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements SoundRecorderFragment.FragmentCallBacks,
        View.OnClickListener {

    private SoundRecorderFragment mSoundRecorderFragment;
    private static int PERMISSION_REQUEST_CODE = 10;

    private Button startButton;
    private Button stopButton;
    private TextView frequencyBinTextView;
    private TextView frequencyBinValueTextView;

    private boolean isClickable;

    private FrameLayout frameLayout;
    private SpectrumView spectrumView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startButton = (Button) findViewById(R.id.startButton);
        startButton.setOnClickListener(this);

        stopButton = (Button) findViewById(R.id.stopButton);
        stopButton.setOnClickListener(this);

        if (savedInstanceState != null) {
            isClickable = savedInstanceState.getBoolean("isClickable", true);
            startButton.setClickable(isClickable);

            if (!isClickable) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        } else {
            isClickable = true;
        }

        frameLayout = (FrameLayout) findViewById(R.id.frameLayout);

        spectrumView = new SpectrumView(this);
        frameLayout.addView(spectrumView);

        frequencyBinTextView = (TextView) findViewById(R.id.frequencyBinTextView);
        frequencyBinValueTextView = (TextView) findViewById(R.id.frequencyBinValueTextView);


    }

    @Override
    protected void onResume() {
        super.onResume();

        mSoundRecorderFragment = (SoundRecorderFragment)
                getSupportFragmentManager().findFragmentByTag(SoundRecorderFragment.FRAGMENT_TAG);

        if (mSoundRecorderFragment == null){

            mSoundRecorderFragment = new SoundRecorderFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(mSoundRecorderFragment, SoundRecorderFragment.FRAGMENT_TAG).commit();
        }

    }

    // ----- CALLBACK METHOD ----------------------------
    @Override
    public void onPostExecuteCallBack(final double[] frequencyBins) {


        int peakBinPosition = 0;
        double peakBinValue = 0.0;


        for (int i = 0; i < frequencyBins.length; i++) {

            // detect highest frequency - for testing
            if (frequencyBins[i] > peakBinValue) {
                peakBinPosition = i;
                peakBinValue = frequencyBins[i];
            }
        }
        final String frequencyBin = String.valueOf(peakBinPosition * SoundRecorderFragment.mSampleRate / 1024
                + "Hz - " + (peakBinPosition + 1) * SoundRecorderFragment.mSampleRate / 1024) + "Hz";

        final double pBA = peakBinValue;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                frequencyBinTextView.setText(frequencyBin);
                frequencyBinValueTextView.setText(String.format("%.5f", pBA));

                spectrumView.redraw(frequencyBins);

            }
        });

    }
    // --------------------------------------------------

    @Override
    public void onClick(View v) {

        switch (v.getId()) {

            case R.id.startButton:

                if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){

                    ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.RECORD_AUDIO},
                            PERMISSION_REQUEST_CODE);

                } else {
                    startRecording();
                }

                break;

            case R.id.stopButton:

                if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED){

                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                    mSoundRecorderFragment.stopRecording();

                    isClickable = true;
                    startButton.setClickable(isClickable);
                    Toast.makeText(this, "STOPPED RECORDING", Toast.LENGTH_SHORT).show();
                }
                break;

            default:
                break;
        }

    }

    private void startRecording() {

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        isClickable = false;
        mSoundRecorderFragment.startRecording();
        startButton.setClickable(isClickable);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            startRecording();
        } else {
            Toast.makeText(this, "Cannot continue - Permission denied", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean("isClickable", isClickable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        if (!isClickable){
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            mSoundRecorderFragment.releaseAudioRecord();
        }

    }
}
