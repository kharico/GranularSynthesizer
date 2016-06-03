package kharico.granularsynthesizer;

import android.content.Context;
import android.media.AudioManager;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;

public class MainActivity extends AppCompatActivity {

    Button oscPower;
    boolean pwrOn = false;
    SeekBar freqControl;
    double sliderVal;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createEngine();
        int sampleRate = 0;
        int bufSize = 0;
        /*
         * retrieve fast audio path sample rate and buf size; if we have it, we pass to native
         * side to create a player with fast audio enabled [ fast audio == low latency audio ];
         * IF we do not have a fast audio path, we pass 0 for sampleRate, which will force native
         * side to pick up the 8Khz sample rate.
         */
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            AudioManager myAudioMgr = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            String nativeParam = myAudioMgr.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);
            sampleRate = Integer.parseInt(nativeParam);
            nativeParam = myAudioMgr.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER);
            bufSize = Integer.parseInt(nativeParam);
        }

        createBufferQueueAudioPlayer(sampleRate, bufSize);

       oscPower = (Button)findViewById(R.id.pwr_switch);
       freqControl = (SeekBar) findViewById(R.id.freq);
       freqControl.setOnSeekBarChangeListener(listener);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy()
    {
        shutdown();
        super.onDestroy();
    }

    SeekBar.OnSeekBarChangeListener listener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
                sliderVal = progress / (double) seekBar.getMax();
                freqChange(sliderVal);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {

        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {

        }
    };

    public void switchPower (View view) {
        if (pwrOn) {
            oscillatorOn(false);
            pwrOn = false;
        }
        else {
            oscillatorOn(true);
            pwrOn = true;
        }
    }

    public static native void createEngine();
    public static native void createBufferQueueAudioPlayer(int sampleRate, int samplesPerBuf);
    public static native void shutdown();
    public static native void oscillatorOn(boolean On);
    public static native void freqChange(double sliderVal);

    /** Load jni .so on initialization */
    static {
        System.loadLibrary("native-audio");
    }
}
