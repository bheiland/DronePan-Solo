package com.unmannedairlines.dronepan;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.o3dr.android.client.ControlTower;
import com.o3dr.android.client.Drone;
import com.o3dr.android.client.apis.ControlApi;
import com.o3dr.android.client.apis.GimbalApi;
import com.o3dr.android.client.apis.VehicleApi;
import com.o3dr.android.client.apis.solo.SoloApi;
import com.o3dr.android.client.apis.solo.SoloCameraApi;
import com.o3dr.android.client.apis.solo.SoloMessageApi;
import com.o3dr.android.client.interfaces.DroneListener;
import com.o3dr.android.client.interfaces.TowerListener;
import com.o3dr.services.android.lib.coordinate.LatLong;
import com.o3dr.services.android.lib.drone.attribute.AttributeEvent;
import com.o3dr.services.android.lib.drone.attribute.AttributeType;
import com.o3dr.services.android.lib.drone.companion.solo.SoloAttributes;
import com.o3dr.services.android.lib.drone.companion.solo.SoloState;
import com.o3dr.services.android.lib.drone.connection.ConnectionParameter;
import com.o3dr.services.android.lib.drone.connection.ConnectionResult;
import com.o3dr.services.android.lib.drone.connection.ConnectionType;
import com.o3dr.services.android.lib.drone.property.Altitude;
import com.o3dr.services.android.lib.drone.property.Battery;
import com.o3dr.services.android.lib.drone.property.Gps;
import com.o3dr.services.android.lib.drone.property.State;
import com.o3dr.services.android.lib.drone.property.Type;
import com.o3dr.services.android.lib.drone.property.VehicleMode;
import com.o3dr.services.android.lib.model.AbstractCommandListener;
import com.o3dr.services.android.lib.model.SimpleCommandListener;

public class MainActivity extends AppCompatActivity implements TowerListener, DroneListener {

    private Drone drone;
    private int droneType = Type.TYPE_UNKNOWN;
    private ControlTower controlTower;
    private final Handler handler = new Handler();
    private boolean panoInProgress = false;

    private TextureView cameraView;
    private Button panoButton;
    private Button columnsButton;
    private Button pitchesButton;
    private Button columnReductionButton;

    // Progress bar
    private ProgressBar panoProgressBar;
    private int totalPhotoCount = 0;
    private int calculatedPhotoCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        hideStatusAndActionBars();

        // Initialize the drone objects
        final Context context = getApplicationContext();
        this.controlTower = new ControlTower(context);
        this.drone = new Drone(context);

        // Pano button manages starting and stopping panos
        panoButton = (Button) findViewById(R.id.panoButton);
        panoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Start the pano
                if(!panoInProgress) {

                    showToast("Starting panorama.");
                    setupPano();
                    columnsButton.setEnabled(false);
                    pitchesButton.setEnabled(false);
                    // Stop the pano
                } else {

                    showToast("Stopping panorama. Please stand by.");
                    panoInProgress = false;

                    // Let's disable the button so it doesn't get double-clicked
                    panoButton.setEnabled(false);
                    columnsButton.setEnabled(true);
                    pitchesButton.setEnabled(true);

                }
            }
        });
        columnReductionButton = (Button) findViewById(R.id.columnReductionButton);
        columnReductionButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View colRed) {
                columnReduction = (!columnReduction);
                if (columnReduction == true){
                    columnReductionButton.setText("Column Reduction: YES");

                }else{
                    columnReductionButton.setText("Column Reduction: NO" );
                }
                calculatedPhotos = calculatedPhotoCount(NUM_COLUMNS,NUM_PITCHES,columnReduction);
                showToast("Number of columns =  " + NUM_COLUMNS + "  Number of photos = " + (calculatedPhotos));
                TextView photoCount = (TextView) findViewById(R.id.photoCount);
                photoCount.setText(String.format("Photos: 0 of %d", (calculatedPhotos)));
                panoProgressBar.setMax(calculatedPhotoCount); // auto adjust progress bar max limit to photo count
                totalPhotoCount = 0;
                panoProgressBar.setProgress(totalPhotoCount);
            }
        });
        columnsButton = (Button) findViewById(R.id.columnsButton);
        columnsButton.setOnClickListener(new View.OnClickListener(){
            @Override
        public void onClick(View col){

                switch (NUM_COLUMNS){
                    case 3: NUM_COLUMNS = 4;
                        SHOT_DELAY = 3000;  // larger turn of drone requires more time
                        break;
                    case 4: NUM_COLUMNS = 6;
                        SHOT_DELAY = 3000;
                        break;
                    case 6: NUM_COLUMNS = 8;
                        SHOT_DELAY = 3000;
                        break;
                    case 8: NUM_COLUMNS = 12;
                        SHOT_DELAY = 3000;
                        break;
                    case 12: NUM_COLUMNS = 3;
                        SHOT_DELAY = 3000;
                        break;
                    default: NUM_COLUMNS = 6;
                        break;
                }
                UI_NUM_COLUMNS = NUM_COLUMNS;

                columnsButton.setText("Columns:"+ NUM_COLUMNS + " at " + (360/NUM_COLUMNS) +" degrees" );
                calculatedPhotos = calculatedPhotoCount(NUM_COLUMNS,NUM_PITCHES,columnReduction);
                showToast("Number of columns =  " + NUM_COLUMNS + "  Number of photos = " + (calculatedPhotos));
                TextView photoCount = (TextView) findViewById(R.id.photoCount);
                photoCount.setText(String.format("Photos: 0 of %d", (calculatedPhotos)));
                panoProgressBar.setMax(calculatedPhotoCount); // auto adjust progress bar max limit to photo count
                totalPhotoCount = 0;
                panoProgressBar.setProgress(totalPhotoCount);
                //showToast("Number of photos = " +  (NUM_COLUMNS * NUM_PITCHES +1));
            }

        });
        pitchesButton = (Button) findViewById(R.id.pitchesButton);
        pitchesButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View pit){
                switch (pitches.length){
                    case 3: pitches = new int[] {0, -30, -60, -90};
                        SHOT_DELAY = 3000;
                        NUM_PITCHES = pitches.length -1;
                        pitchesButton.setText("Pitches: 0, -30, -60, -90" );
                        break;
                    case 4: pitches = new int[] {0,-23, -45, -67, -90};
                        SHOT_DELAY = 3000;
                        NUM_PITCHES = pitches.length -1;
                        pitchesButton.setText("Pitches: 0, -23, -45, -67, -90" );
                        break;
                    case 5: pitches = new int[] {0, -15, -30, -45, -60, -90};
                        SHOT_DELAY = 3000;
                        NUM_PITCHES = pitches.length -1;
                        pitchesButton.setText("Pitches: 0, -15, -30, -45, -60, -90" );
                        break;
                    case 6: pitches = new int[] {0, -45, -90};
                        SHOT_DELAY = 3000;
                        NUM_PITCHES = pitches.length -1;
                        pitchesButton.setText("Pitches: 0, -45, -90" );
                        break;
                    default: pitches = new int[] {0, -30, -60, -90};
                        SHOT_DELAY = 3000;
                        NUM_PITCHES = pitches.length -1;
                        pitchesButton.setText("Pitches: 0, -30, -60, -90" );
                        break;
                }
                System.gc(); // garbage collect

                totalPhotoCount = 0;
                calculatedPhotos = calculatedPhotoCount(NUM_COLUMNS,NUM_PITCHES,columnReduction);
                panoProgressBar.setMax(calculatedPhotoCount); // auto adjust progress bar max limit to photo count
                TextView photoCount = (TextView) findViewById(R.id.photoCount);
                photoCount.setText(String.format("Photos: 0 of %d", (calculatedPhotos)));
                panoProgressBar.setProgress(totalPhotoCount);
                showToast("Number of columns =  " + NUM_COLUMNS + "  Number of photos = " +  (calculatedPhotos));

            }

        });

        // Camera preview
        cameraView = (TextureView) findViewById(R.id.cameraView);
        cameraView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                return true;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {

            }
        });

        // Progress bar for showing pano progress
        panoProgressBar = (ProgressBar) findViewById(R.id.progressBar);

    }

    private void hideStatusAndActionBars() {
        // Hide the status bar
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        // Hide the action bar
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();
    }

    // When waking up let's keep the status and action bars hidden
    @Override
    public void onResume() {
        super.onResume();
        hideStatusAndActionBars();
    }

    @Override
    public void onStart() {
        super.onStart();
        this.controlTower.connect(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (this.drone.isConnected()) {
            this.drone.disconnect();
            //updateConnectedButton(false);
        }
        this.controlTower.unregisterDrone(this.drone);
        this.controlTower.disconnect();
    }

    @Override
    public void onTowerConnected() {
        this.controlTower.registerDrone(this.drone, this.handler);
        this.drone.registerDroneListener(this);

        // Connect to drone
        Bundle extraParams = new Bundle();
        extraParams.putInt(ConnectionType.EXTRA_UDP_SERVER_PORT, 14550);
        ConnectionParameter connectionParams = new ConnectionParameter(ConnectionType.TYPE_UDP, extraParams, null);
        this.drone.connect(connectionParams);
    }

    @Override
    public void onTowerDisconnected() {

    }

    @Override
    public void onDroneConnectionFailed(ConnectionResult result) {

    }

    @Override
    public void onDroneEvent(String event, Bundle extras) {
        switch (event) {
            case AttributeEvent.STATE_CONNECTED:
                TextView connectionStatusTextView = (TextView)findViewById(R.id.connectionStatus);
                connectionStatusTextView.setText("Connected");
                startVideoFeed();
                break;

            case AttributeEvent.STATE_DISCONNECTED:
                connectionStatusTextView = (TextView)findViewById(R.id.connectionStatus);
                connectionStatusTextView.setText("Disconnected");
                stopVideoFeed();
                break;

            case AttributeEvent.ALTITUDE_UPDATED:
                TextView altitudeTextView = (TextView)findViewById(R.id.altitudeTextView);
                Altitude droneAltitude = this.drone.getAttribute(AttributeType.ALTITUDE);
                altitudeTextView.setText(String.format("Alt: %3.1f", droneAltitude.getAltitude()) + "m");
                break;

            case AttributeEvent.BATTERY_UPDATED:
                TextView batteryTextView = (TextView)findViewById(R.id.batteryTextView);
                Battery batt = this.drone.getAttribute(AttributeType.BATTERY);
                batteryTextView.setText(String.format("Batt: %3.1f", batt.getBatteryRemain()) + "%");
                break;

            case AttributeEvent.GPS_COUNT:
                TextView satelliteTextView = (TextView)findViewById(R.id.satelliteTextView);
                Gps gps = this.drone.getAttribute(AttributeType.GPS);
                satelliteTextView.setText(String.format("Sats: %3.1f", gps.getSatellitesCount()));
                break;

            case AttributeEvent.STATE_VEHICLE_MODE:
                State vehicleState = this.drone.getAttribute(AttributeType.STATE);
                VehicleMode vehicleMode = vehicleState.getVehicleMode();
                TextView flightModeTextView = (TextView)findViewById(R.id.flightModeTextView);
                flightModeTextView.setText("Mode: " + vehicleMode.getLabel());

                // If the mode is switched and the pano is in progress let's release gimbal control
                if(vehicleMode.getLabel() != "Guided" && panoInProgress) {
                    panoInProgress = false;
                    GimbalApi.getApi(this.drone).stopGimbalControl(gimbalListener);
                }
                break;

            default:
                break;
        }
    }

    @Override
    public void onDroneServiceInterrupted(String errorMsg) {

    }

    public void showToast(String toast) {
        Toast.makeText(getApplicationContext(), toast, Toast.LENGTH_SHORT).show();
    }

    private void startVideoFeed() {
        SoloCameraApi.getApi(drone).startVideoStream(new Surface(cameraView.getSurfaceTexture()), "", true, new AbstractCommandListener() {
            @Override
            public void onSuccess() {
            }

            @Override
            public void onError(int executionError) {

            }

            @Override
            public void onTimeout() {
            }
        });
    }

    private void stopVideoFeed() {
        SoloCameraApi.getApi(drone).stopVideoStream(new SimpleCommandListener() {
            @Override
            public void onSuccess() {

            }
        });
    }

    public void armDrone() {

        // Arm the drone
        VehicleApi.getApi(this.drone).arm(true, false, new SimpleCommandListener() {

            // On successful arming let's takeoff
            @Override
            public void onSuccess() {
                takeOff();
            }

            @Override
            public void onError(int executionError) {
                showToast("Error arming");
            }

            @Override
            public void onTimeout() {
                showToast("Arming timed out");
            }

        });

    }

    public void takeOff() {

        ControlApi.getApi(this.drone).takeoff(10, new AbstractCommandListener() {

            @Override
            public void onSuccess() {
                showToast("Taking off to 10m");
                //gotoWaypoint();
                //ControlApi.getApi(this.drone).goTo();

            }

            @Override
            public void onError(int executionError) {

            }

            @Override
            public void onTimeout() {

            }
        });
    }

    private int NUM_COLUMNS = 6;  // 4 = 90 degrees 6 = 60 degree, 8 = 45 degree, 12 = 30 degree columns
    private int UI_NUM_COLUMNS = 6;  // retain num columns setting from UI button
    private int loopCount = 0;
    private int[] pitches = {0, -30, -60, -90};
    private int NUM_PITCHES = pitches.length-1;  // Set NUM_PITCHES to pitches array count minus one
    private int photoCount = 0;
    private int SHOT_DELAY = 2000;  // added a variable for SHOT DELAY to remove hard coding and allow more flexibility
    private boolean columnReduction = true;
    private int calculatedPhotos =0;
    private int decreaseColumns(int startingColumns){
        int columns = 0;
        switch (startingColumns){
            case 12: columns = 8;
                break;
            case 8: columns = 6;
                break;
            case 6: columns = 4;
                break;
            case 4: columns = 3;
                break;
            case 3: columns = 2;
                break;
            case 2: columns = 2;
                break;
            default: columns = 4; // default to 4 on invalid
                break;
        }
        return columns;
    }
    private void autoDecreaseColumns(){
        switch( NUM_COLUMNS){
            case 12: NUM_COLUMNS = 8;
                break;
            case 8: NUM_COLUMNS = 6;
                break;
            case 6: NUM_COLUMNS = 4;
                break;
            case 4: NUM_COLUMNS = 3;
                break;
            case 3: NUM_COLUMNS = 2;
                break;
            case 2: NUM_COLUMNS = 2;
                break;
            default: NUM_COLUMNS = 4; // default to 4 on invalid
                break;
    }
        showToast("Columns auto reduced to:" + NUM_COLUMNS);
    }
    // Change to guided mode, start gimbal control, reset the gimbal to 0, and set mode to guided

    private int calculatedPhotoCount(int columns, int numPitches, boolean reduction){
        int numPhotos =0;
        if (reduction) {
            numPhotos = columns;
            for (int i = 0; i < (numPitches-1); i++) {
                columns = decreaseColumns(columns);
                numPhotos += columns;
            }


        }else{
            numPhotos = (columns * numPitches );
        }
        numPhotos +=1;
        return numPhotos;
    }

    private void setupPano() {

        totalPhotoCount = 0;
        calculatedPhotos = calculatedPhotoCount(NUM_COLUMNS,NUM_PITCHES,columnReduction);
        panoProgressBar.setMax(calculatedPhotos); // set progress bar to the photos column reduction count

        panoProgressBar.setProgress(totalPhotoCount);
        panoInProgress = true;

        // Change start button to stop button
        panoButton.setText("Stop");

        // Set copter to guided
        VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_GUIDED, new SimpleCommandListener() {
            @Override
            public void onSuccess() {
            }
        });

        // Enable gimbal control
        GimbalApi.getApi(this.drone).startGimbalControl(gimbalListener);

        // Reset gimbal to 0
        pitchGimbal(0);

        // Give the gimbal a couple seconds to reset and begin pano
        final Handler h = new Handler();

        final Runnable begin = new Runnable() {

            @Override
            public void run() {
                loopAndShoot(); // Let's begin the pano process
            }
        };

        h.postDelayed(begin, SHOT_DELAY);

    }

    private void loopAndShoot() {

        // The pano stop button was clicked so let's reset a bunch of stuff
        if (!panoInProgress) {

            panoButton.setText("Start");

            // Reset gimbal and give control back to user

            // Changing vehicle mode back to FLY
            VehicleApi.getApi(this.drone).setVehicleMode(VehicleMode.COPTER_LOITER, new SimpleCommandListener() {
                @Override
                public void onSuccess() {
                }
            });

            // Reset the progress bar
            panoProgressBar.setProgress(0);

            // Enable the start button again
            panoButton.setEnabled(true);


            showToast("Your panorama has been stopped successfully.");

            return;
        }

        final Handler h = new Handler();

        // Pitch the gimbal
        final Runnable pitch = new Runnable() {

            // Pitch gimbal and either continue with loop or take nadir
            @Override
            public void run() {

                if(Build.MODEL.contains("SDK")) {

                    showToast("Pitching gimbal to: " + pitches[loopCount]);

                } else {

                    pitchGimbal(pitches[loopCount]);

                }

                photoCount = 0;

                // Take nadir shot
                if(loopCount == NUM_PITCHES) {

                    loopCount = 0;

                    takeNadirPhotoAndFinishPano();

                    // Set the progress bar to 100%
                    panoProgressBar.setProgress(calculatedPhotoCount);

                    // Update the stop button
                    panoButton.setText("Start");
                    columnsButton.setEnabled(true);
                    pitchesButton.setEnabled(true);


                    // Continue with photo loop
                } else {

                    loopAndShoot();

                }
            }
        }; // End pitch gimbal

        // Yaw the drone
        final Runnable yaw = new Runnable() {

            @Override
            public void run() {

                if(Build.MODEL.contains("SDK")) {

                    showToast("Yaw drone: " + 360/NUM_COLUMNS);

                } else {

                    yawDrone(360/NUM_COLUMNS);

                }

                if(photoCount == NUM_COLUMNS) {
                    loopCount++;

                    // Now let's pitch gimbal and then we'll begin the next loop
                    h.postDelayed(pitch, SHOT_DELAY);
                    if (columnReduction == true) {
                        autoDecreaseColumns();
                    }
                } else {

                    loopAndShoot();

                }
            }
        }; // End yaw drone

        // Take a photo
        final Runnable photo = new Runnable() {

            @Override
            public void run() {

                if(Build.MODEL.contains("SDK")) {

                    showToast("Take photo");
                    // Update progress bar
                    totalPhotoCount++;
                    panoProgressBar.setProgress(totalPhotoCount);


                } else {

                    takePhoto();

                    // Update progress bar
                    totalPhotoCount++;
                    panoProgressBar.setProgress(totalPhotoCount);


                }
                TextView photoCount = (TextView) findViewById(R.id.photoCount);
                photoCount.setText(String.format("Photos: %d of %d", totalPhotoCount,calculatedPhotos));
                h.postDelayed(yaw, SHOT_DELAY);

            }
        }; // End take photo

        h.postDelayed(photo, SHOT_DELAY);

        photoCount++;

    }

    private void yawDrone(int angle) {

        ControlApi.getApi(this.drone).turnTo(angle, 1, true, new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                showToast("Yawing to"+ (360/NUM_COLUMNS) +" degrees...");
            }

            @Override
            public void onError(int executionError) {
                showToast("Error yawing...");
            }

            @Override
            public void onTimeout() {

            }
        });

        //showToast("Yaw drone..."); // remarked out to unclutter messages not needed if onSuccess happened.

    }

    private void gotoWaypoint() {
        //LatLongAlt vehicle3DPosition = new LatLongAlt(vehiclePosition.getLatitude(), vehiclePosition.getLongitude(), vehicleAltitude);
        //LatLong point = new LatLong();
        //MissionApi.getApi(this.drone).


        ControlApi.getApi(this.drone).goTo(new LatLong(1, 1), false, new AbstractCommandListener() {
            @Override
            public void onSuccess() {

            }

            @Override
            public void onError(int executionError) {

            }

            @Override
            public void onTimeout() {

            }
        });
    }

    private void takePhoto() {

        SoloCameraApi.getApi(this.drone).takePhoto(new AbstractCommandListener() {
            @Override
            public void onSuccess() {
                //showToast("Photo taken.");
            }

            @Override
            public void onError(int executionError) {
                showToast("Error while trying to take the photo: " + executionError);
            }

            @Override
            public void onTimeout() {
                showToast("Timeout while trying to take the photo.");
            }
        });
    }

    // Take the nadir shot, release drone and gimbal control back to the user
    private void takeNadirPhotoAndFinishPano() {

        final Handler h = new Handler();

        final Runnable pitch = new Runnable() {

            @Override
            public void run() {

                if(Build.MODEL.contains("SDK")) {

                    showToast("Panorama complete!!!");
                    panoInProgress = (false);//needed to add this when in sdk mode to end properly
                    NUM_COLUMNS = UI_NUM_COLUMNS;
                } else {

                    pitchGimbal(0);

                    // Change mode back to FLY mode, which is Loiter
                    VehicleApi.getApi(drone).setVehicleMode(VehicleMode.COPTER_LOITER, new SimpleCommandListener() {
                        @Override
                        public void onSuccess() {
                            showToast("Panorama complete! You can now take control of Solo.");
                        }
                    });
                    NUM_COLUMNS = UI_NUM_COLUMNS;

                }
            }

        };

        final Runnable finish = new Runnable() {

            @Override
            public void run() {

                if(Build.MODEL.contains("SDK")) {

                    showToast("Taking nadir shot");
                    TextView photoCount = (TextView) findViewById(R.id.photoCount);
                    photoCount.setText(String.format("Photos: %d of %d", calculatedPhotos, calculatedPhotos));
                } else {

                    takePhoto();
                    NUM_COLUMNS = UI_NUM_COLUMNS;

                }

                h.postDelayed(pitch, SHOT_DELAY);

            }
        };

        h.postDelayed(finish, SHOT_DELAY);

    }

    // Listener for when we take control of the gimbal
    GimbalApi.GimbalOrientationListener gimbalListener = new GimbalApi.GimbalOrientationListener() {

        @Override
        public void onGimbalOrientationUpdate(GimbalApi.GimbalOrientation orientation) {

        }

        @Override
        public void onGimbalOrientationCommandError(int error) {
            showToast("Gimbal error");
        }
    };

    private void pitchGimbal(int angle) {

        GimbalApi.getApi(this.drone).updateGimbalOrientation(angle, 0, 0, gimbalListener);
    }

    // This is called from the webappinterface
    public void parseMission(String missionStr) {
        showToast(missionStr);
    }
}
