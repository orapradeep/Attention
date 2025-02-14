package com.example.attention;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.provider.MediaStore;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.exifinterface.media.ExifInterface;
// ContentResolver dependency
import com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmark;
import com.google.mediapipe.solutioncore.CameraInput;
import com.google.mediapipe.solutioncore.SolutionGlSurfaceView;
import com.google.mediapipe.solutioncore.VideoInput;
import com.google.mediapipe.solutions.facemesh.FaceMesh;
import com.google.mediapipe.solutions.facemesh.FaceMeshOptions;
import com.google.mediapipe.solutions.facemesh.FaceMeshResult;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/** Main activity of MediaPipe Face Mesh app. */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private FaceMesh facemesh;
    // Run the pipeline and the model inference on GPU or CPU.
    private static final boolean RUN_ON_GPU = true;
    private enum InputSource {
        UNKNOWN,
        IMAGE,
        VIDEO,
        CAMERA,
    }
    private InputSource inputSource = InputSource.UNKNOWN;
    // Image demo UI and image loader components.
    private ActivityResultLauncher<Intent> imageGetter;
    private FaceMeshResultImageView imageView;
    // Video demo UI and video loader components.
    private VideoInput videoInput;
    private ActivityResultLauncher<Intent> videoGetter;
    // Live camera demo UI and camera components.
    private CameraInput cameraInput;

    private SolutionGlSurfaceView<FaceMeshResult> glSurfaceView;

    private MediaPlayer player;
    private MediaPlayer player_distraction;
    private boolean drowsinessAlertPlayed = false;
    private boolean distractionAlertPlayed = false;
    private boolean prevDrowsinessState;
    private boolean prevDistractionState;
    private UserStatusDetector userStatusDetector = new UserStatusDetector();
    private boolean userIsDrowsy = false;
    private boolean userIsDistracted = false;
    private boolean userFaceDetected = false;
    long curTime = 0;
    long prevTime = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // TODO: Add a toggle to switch between the original face mesh and attention mesh.
//        setupStaticImageDemoUiComponents();
//        setupVideoDemoUiComponents();
        setupLiveDemoUiComponents();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (inputSource == InputSource.CAMERA) {
            // Restarts the camera and the opengl surface rendering.
            cameraInput = new CameraInput(this);
            cameraInput.setNewFrameListener(textureFrame -> facemesh.send(textureFrame));
            glSurfaceView.post(this::startCamera);
            glSurfaceView.setVisibility(View.VISIBLE);
        } else if (inputSource == InputSource.VIDEO) {
            videoInput.resume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (inputSource == InputSource.CAMERA) {
            glSurfaceView.setVisibility(View.GONE);
            cameraInput.close();
        } else if (inputSource == InputSource.VIDEO) {
            videoInput.pause();
        }
    }

    private Bitmap downscaleBitmap(Bitmap originalBitmap) {
        double aspectRatio = (double) originalBitmap.getWidth() / originalBitmap.getHeight();
        int width = imageView.getWidth();
        int height = imageView.getHeight();
        if (((double) imageView.getWidth() / imageView.getHeight()) > aspectRatio) {
            width = (int) (height * aspectRatio);
        } else {
            height = (int) (width / aspectRatio);
        }
        return Bitmap.createScaledBitmap(originalBitmap, width, height, false);
    }

    private Bitmap rotateBitmap(Bitmap inputBitmap, InputStream imageData) throws IOException {
        int orientation =
                new ExifInterface(imageData)
                        .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        if (orientation == ExifInterface.ORIENTATION_NORMAL) {
            return inputBitmap;
        }
        Matrix matrix = new Matrix();
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                break;
            default:
                matrix.postRotate(0);
        }
        return Bitmap.createBitmap(
                inputBitmap, 0, 0, inputBitmap.getWidth(), inputBitmap.getHeight(), matrix, true);
    }

    /** Sets up the UI components for the static image demo. */
//    private void setupStaticImageDemoUiComponents() {
//        // The Intent to access gallery and read images as bitmap.
//        imageGetter =
//                registerForActivityResult(
//                        new ActivityResultContracts.StartActivityForResult(),
//                        result -> {
//                            Intent resultIntent = result.getData();
//                            if (resultIntent != null) {
//                                if (result.getResultCode() == RESULT_OK) {
//                                    Bitmap bitmap = null;
//                                    try {
//                                        bitmap =
//                                                downscaleBitmap(
//                                                        MediaStore.Images.Media.getBitmap(
//                                                                this.getContentResolver(), resultIntent.getData()));
//                                    } catch (IOException e) {
//                                        Log.e(TAG, "Bitmap reading error:" + e);
//                                    }
//                                    try {
//                                        InputStream imageData =
//                                                this.getContentResolver().openInputStream(resultIntent.getData());
//                                        bitmap = rotateBitmap(bitmap, imageData);
//                                    } catch (IOException e) {
//                                        Log.e(TAG, "Bitmap rotation error:" + e);
//                                    }
//                                    if (bitmap != null) {
//                                        facemesh.send(bitmap);
//                                    }
//                                }
//                            }
//                        });
//        Button loadImageButton = findViewById(R.id.button_load_picture);
//        loadImageButton.setOnClickListener(
//                v -> {
//                    if (inputSource != InputSource.IMAGE) {
//                        stopCurrentPipeline();
//                        setupStaticImageModePipeline();
//                    }
//                    // Reads images from gallery.
//                    Intent pickImageIntent = new Intent(Intent.ACTION_PICK);
//                    pickImageIntent.setDataAndType(MediaStore.Images.Media.INTERNAL_CONTENT_URI, "image/*");
//                    imageGetter.launch(pickImageIntent);
//                });
//        imageView = new FaceMeshResultImageView(this);
//    }

    /** Sets up core workflow for static image mode. */
    private void setupStaticImageModePipeline() {
        this.inputSource = InputSource.IMAGE;
        // Initializes a new MediaPipe Face Mesh solution instance in the static image mode.
        facemesh =
                new FaceMesh(
                        this,
                        FaceMeshOptions.builder()
                                .setStaticImageMode(true)
                                .setRefineLandmarks(true)
                                .setRunOnGpu(RUN_ON_GPU)
                                .build());

        // Connects MediaPipe Face Mesh solution to the user-defined FaceMeshResultImageView.
        facemesh.setResultListener(
                faceMeshResult -> {
                    logNoseLandmark(faceMeshResult, /*showPixelValues=*/ true);
                    imageView.setFaceMeshResult(faceMeshResult);
                    runOnUiThread(() -> imageView.update());
                });
        facemesh.setErrorListener((message, e) -> Log.e(TAG, "MediaPipe Face Mesh error:" + message));

        // Updates the preview layout.
        FrameLayout frameLayout = findViewById(R.id.preview_display_layout);
        frameLayout.removeAllViewsInLayout();
        imageView.setImageDrawable(null);
        frameLayout.addView(imageView);
        imageView.setVisibility(View.VISIBLE);
    }

    /** Sets up the UI components for the video demo. */
//    private void setupVideoDemoUiComponents() {
//        // The Intent to access gallery and read a video file.
//        videoGetter =
//                registerForActivityResult(
//                        new ActivityResultContracts.StartActivityForResult(),
//                        result -> {
//                            Intent resultIntent = result.getData();
//                            if (resultIntent != null) {
//                                if (result.getResultCode() == RESULT_OK) {
//                                    glSurfaceView.post(
//                                            () ->
//                                                    videoInput.start(
//                                                            this,
//                                                            resultIntent.getData(),
//                                                            facemesh.getGlContext(),
//                                                            glSurfaceView.getWidth(),
//                                                            glSurfaceView.getHeight()));
//                                }
//                            }
//                        });
//        Button loadVideoButton = findViewById(R.id.button_load_video);
//        loadVideoButton.setOnClickListener(
//                v -> {
//                    stopCurrentPipeline();
//                    setupStreamingModePipeline(InputSource.VIDEO);
//                    // Reads video from gallery.
//                    Intent pickVideoIntent = new Intent(Intent.ACTION_PICK);
//                    pickVideoIntent.setDataAndType(MediaStore.Video.Media.INTERNAL_CONTENT_URI, "video/*");
//                    videoGetter.launch(pickVideoIntent);
//                });
//    }

    /** Sets up the UI components for the live demo with camera input. */
    private void setupLiveDemoUiComponents() {
        Button startCameraButton = findViewById(R.id.button_start_camera);
        Log.i("Attention","Initializing button for starting cam!");
        startCameraButton.setOnClickListener(
                v -> {
                    Log.i("Attention","Clicking button to start cam!");
                    if (inputSource == InputSource.CAMERA) {
                        return;
                    }
                    stopCurrentPipeline();
                    setupStreamingModePipeline(InputSource.CAMERA);
                });
    }

    /** Sets up core workflow for streaming mode. */
    private void setupStreamingModePipeline(InputSource inputSource) {
        this.inputSource = inputSource;
        // Initializes a new MediaPipe Face Mesh solution instance in the streaming mode.
        facemesh =
                new FaceMesh(
                        this,
                        FaceMeshOptions.builder()
                                .setStaticImageMode(false)
                                .setRefineLandmarks(true)
                                .setRunOnGpu(RUN_ON_GPU)
                                .build());
        facemesh.setErrorListener((message, e) -> Log.e(TAG, "MediaPipe Face Mesh error:" + message));

        if (inputSource == InputSource.CAMERA) {
            cameraInput = new CameraInput(this);
            cameraInput.setNewFrameListener(textureFrame -> facemesh.send(textureFrame));
        } else if (inputSource == InputSource.VIDEO) {
            videoInput = new VideoInput(this);
            videoInput.setNewFrameListener(textureFrame -> facemesh.send(textureFrame));
        }

        // Initializes a new Gl surface view with a user-defined FaceMeshResultGlRenderer.
        glSurfaceView =
                new SolutionGlSurfaceView<>(this, facemesh.getGlContext(), facemesh.getGlMajorVersion());
        glSurfaceView.setSolutionResultRenderer(new FaceMeshResultGlRenderer());
        glSurfaceView.setRenderInputImage(true);
        facemesh.setResultListener(
                faceMeshResult -> {
                    curTime = System.currentTimeMillis();
                    try {
                        // to see if there is a face detected
                        List<NormalizedLandmark> lmList = faceMeshResult.multiFaceLandmarks().get(0).getLandmarkList();
                        this.userFaceDetected = true;
                        prevDrowsinessState = this.userIsDrowsy;
                        prevDistractionState = this.userIsDistracted;
                        this.userIsDrowsy = userStatusDetector.isDrowsy(faceMeshResult, curTime, prevTime);
                        if (userStatusDetector.headFacingLeft(faceMeshResult, curTime, prevTime)) {
                            this.userIsDistracted = true;
                            TextView distractedTV = findViewById(R.id.distracted_status);
                            distractedTV.setText("Head turning left" +  ". Diff X: " + String.valueOf(userStatusDetector.getDiffInXOfFaceSides()));
                        } else if (userStatusDetector.headFacingRight(faceMeshResult, curTime, prevTime)) {
                            this.userIsDistracted = true;
                            TextView distractedTV = findViewById(R.id.distracted_status);
                            distractedTV.setText("Head turning right" +  ". Diff X: " + String.valueOf(userStatusDetector.getDiffInXOfFaceSides()));
                        } else {
                            this.userIsDistracted = false;
                            TextView distractedTV = findViewById(R.id.distracted_status);
                            distractedTV.setText(getResources().getString(R.string.no_distraction) +  ". Diff X: " + String.valueOf(userStatusDetector.getDiffInXOfFaceSides()));
                        }

                        if (!prevDrowsinessState && this.userIsDrowsy) {
                            drowsinessAlertPlayed = false;
                        }
                        if (!prevDistractionState && this.userIsDistracted) {
                            distractionAlertPlayed = false;
                        }
                        if (this.userIsDrowsy) {
                            TextView drowsinessTV = findViewById(R.id.drowsiness_status);
                            drowsinessTV.setText(getResources().getString(R.string.drowsy) + ". L: " + String.valueOf(userStatusDetector.getDistanceLeftEye()) + ", R: " + String.valueOf(userStatusDetector.getDistanceRightEye()));
//                        drowsinessTV.setText(getResources().getString(R.string.drowsy))
                            if (player == null) {
                                player = MediaPlayer.create(this, R.raw.drowsiness_alert);
                            }
                            if (!drowsinessAlertPlayed) {
                                player.start();
                                drowsinessAlertPlayed = true;
                            }
                        } else {
                            TextView drowsinessTV = findViewById(R.id.drowsiness_status);
                            drowsinessTV.setText(getResources().getString(R.string.no_drowsy) + ". L: " + String.valueOf(userStatusDetector.getDistanceLeftEye()) + ", R: " + String.valueOf(userStatusDetector.getDistanceRightEye()));
//                        drowsinessTV.setText(getResources().getString(R.string.no_drowsy)
                        }

                        if (this.userIsDistracted) {
                            if (player_distraction == null) {
                                player_distraction = MediaPlayer.create(this, R.raw.distraction_alert);
                            }
                            if (!distractionAlertPlayed) {
                                player_distraction.start();
                                distractionAlertPlayed = true;
                            }
                        }
                    } catch (Exception ex) {
                        // no face detected
                        prevDrowsinessState = false;
                        prevDistractionState = false;
                        this.userIsDrowsy = false;
                        this.userIsDistracted = false;
                        this.userFaceDetected = false;
                        ex.printStackTrace();
                    }

                    if (!userFaceDetected) {
                        TextView drowsinessTV = findViewById(R.id.drowsiness_status);
                        TextView distractionTV = findViewById(R.id.distracted_status);
                        drowsinessTV.setText("NO FACE DETECTED!");
                        distractionTV.setText("");
                    }
                    logNoseLandmark(faceMeshResult, /*showPixelValues=*/ false);
                    glSurfaceView.setRenderData(faceMeshResult);
                    glSurfaceView.requestRender();
                    prevTime = curTime;
                });

        // The runnable to start camera after the gl surface view is attached.
        // For video input source, videoInput.start() will be called when the video uri is available.
        if (inputSource == InputSource.CAMERA) {
            glSurfaceView.post(this::startCamera);
        }

        // Updates the preview layout.
        FrameLayout frameLayout = findViewById(R.id.preview_display_layout);
        //imageView.setVisibility(View.GONE);
        //frameLayout.removeAllViewsInLayout();
        frameLayout.addView(glSurfaceView);
        glSurfaceView.setVisibility(View.VISIBLE);

        frameLayout.requestLayout();
    }

    private void startCamera() {
        cameraInput.start(
                this,
                facemesh.getGlContext(),
                CameraInput.CameraFacing.FRONT,
                glSurfaceView.getWidth(),
                glSurfaceView.getHeight());
    }

    private void stopCurrentPipeline() {
        if (cameraInput != null) {
            cameraInput.setNewFrameListener(null);
            cameraInput.close();
        }
        if (videoInput != null) {
            videoInput.setNewFrameListener(null);
            videoInput.close();
        }
        if (glSurfaceView != null) {
            glSurfaceView.setVisibility(View.GONE);
        }
        if (facemesh != null) {
            facemesh.close();
        }
    }

    private void logNoseLandmark(FaceMeshResult result, boolean showPixelValues) {
        if (result == null || result.multiFaceLandmarks().isEmpty()) {
            return;
        }
        NormalizedLandmark noseLandmark = result.multiFaceLandmarks().get(0).getLandmarkList().get(1);
        // For Bitmaps, show the pixel values. For texture inputs, show the normalized coordinates.
        if (showPixelValues) {
            int width = result.inputBitmap().getWidth();
            int height = result.inputBitmap().getHeight();
            Log.i(
                    TAG,
                    String.format(
                            "MediaPipe Face Mesh nose coordinates (pixel values): x=%f, y=%f",
                            noseLandmark.getX() * width, noseLandmark.getY() * height));
        } else {
            Log.i(
                    TAG,
                    String.format(
                            "MediaPipe Face Mesh nose normalized coordinates (value range: [0, 1]): x=%f, y=%f",
                            noseLandmark.getX(), noseLandmark.getY()));
        }
    }
}
