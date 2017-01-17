package kharico.granularsynthesizer;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.TextureView;
import android.view.View;
import android.webkit.PermissionRequest;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Toast;

import java.io.IOException;

import kharico.granularsynthesizer.gl.VideoTextureRenderer;

public class MainActivity extends AppCompatActivity implements TextureView.SurfaceTextureListener {

    private static final String TAG = "MainActivity";

    Button oscPower;
    boolean pwrOn = false;
    SeekBar freqControl;
    double sliderVal;
    private final int REQUEST_AUDIO = 1;
    private final int REQUEST_CAMERA = 2;

    private Camera mCamera;
    private TextureView mTextureView;
    private VideoTextureRenderer mRenderer;

    private int surfaceWidth;
    private int surfaceHeight;

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

        if (!hasPermission(Manifest.permission.RECORD_AUDIO)) {
            requestPermissions(Manifest.permission.RECORD_AUDIO, REQUEST_AUDIO);
        }

        createAudioRecorder();
        startRecording();

        if (!hasPermission(Manifest.permission.CAMERA)) {
            requestPermissions(Manifest.permission.CAMERA, REQUEST_CAMERA);
        }

       //oscPower = (Button)findViewById(R.id.pwr_switch);
       oscPower = (Button)findViewById(R.id.filter);
       freqControl = (SeekBar) findViewById(R.id.freq);
       freqControl.setOnSeekBarChangeListener(listener);


        mTextureView = (TextureView) findViewById(R.id.surface);
        mTextureView.setSurfaceTextureListener(this);

        ((Button) findViewById(R.id.filter)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                sepiaFilter();
                switchPower();
            }
        });

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
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume ");

    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause ");
        if (mCamera != null) {
            mCamera.release();
            mCamera = null;
        }
        if (mRenderer != null)
            mRenderer.onPause();
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

    public void switchPower () {

        pwrOn = !pwrOn;
        oscillatorOn(pwrOn);
        /*
        if (pwrOn) {
            oscillatorOn(false);
            pwrOn = false;
        }
        else {
            oscillatorOn(true);
            pwrOn = true;
        }
        */
    }

    private boolean hasPermission(String permission){
        boolean hasPermission = (ContextCompat.checkSelfPermission(this,
                permission) == PackageManager.PERMISSION_GRANTED);

        Log.d(TAG, "Has " + permission + " permission? " + hasPermission);
        return hasPermission;
    }

    private void requestPermissions(String requiredPermission, int reqCode){

        // If the user previously denied this permission then show a message explaining why
        // this permission is needed
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                requiredPermission)) {

            Toast.makeText(this, "This app needs to access" + requiredPermission, Toast.LENGTH_SHORT).show();
        }

        // request the permission.
        ActivityCompat.requestPermissions(this,
                new String[]{requiredPermission},
                reqCode);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "grantedddd ");
            if (requestCode == REQUEST_AUDIO) {
                Log.d(TAG, "creating Recorder ");
                //createAudioRecorder();
                //startRecording();
            }
        }
        else {
            Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
        }
        return;
        // This method is called when the user responds to the permissions dialog
    }

    private void startPlaying()
    {

        mRenderer = new VideoTextureRenderer(this, mTextureView.getSurfaceTexture(), surfaceWidth, surfaceHeight);
        mRenderer.setVideoSize(surfaceWidth, surfaceHeight);


        Log.d(TAG, "startPlaying ");
        try {
            if (mCamera != null)  {
                mCamera.release();
                mCamera = null;
            }

            mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
            mCamera.setDisplayOrientation(90);
            //set camera to continually auto-focus
            Camera.Parameters params = mCamera.getParameters();
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            mCamera.setParameters(params);
            mCamera.setPreviewTexture(mRenderer.getVideoTexture());
            mCamera.startPreview();

        } catch (IOException ioe) {
            mCamera.release();
            mCamera = null;
            throw new RuntimeException("Could not open camera!");
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height)
    {
        Log.d(TAG, "onSurfaceTextureAvailable ");
        surfaceWidth = width;
        surfaceHeight = height;
        startPlaying();
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height)
    {
        // Ignore
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        Log.d(TAG, "onSurfaceTextureDestroyed ");
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface)
    {
        //Invoked every time there's a new Camera preview frame

    }

    public void sepiaFilter() {

        mRenderer.sepiaFilterOn = !(mRenderer.sepiaFilterOn);

    }

    public static native void createEngine();
    public static native void createBufferQueueAudioPlayer(int sampleRate, int samplesPerBuf);
    public static native void createAudioRecorder();
    public static native void startRecording();
    public static native void shutdown();
    public static native void oscillatorOn(boolean On);
    public static native void freqChange(double sliderVal);

    /** Load jni .so on initialization */
    static {
        System.loadLibrary("native-audio");
    }
}
