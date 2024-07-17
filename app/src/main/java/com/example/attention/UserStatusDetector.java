package com.example.attention;

import android.opengl.GLES20;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.common.collect.ImmutableSet;
import com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmark;
import com.google.mediapipe.solutions.facemesh.FaceMesh;
import com.google.mediapipe.solutions.facemesh.FaceMeshConnections;
import com.google.mediapipe.solutions.facemesh.FaceMeshResult;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.List;
import java.lang.Math;
public class UserStatusDetector extends AppCompatActivity {
    private float distanceLeftEye; // betw the top and the bottom
    private float distanceRightEye; // betw the top and the bottom
    private float drowsyDistanceThreshold;
    private long elapsedTime;  // milliseconds
    private long elapsedTimeTurnLeft;  // milliseconds
    private long elapsedTimeTurnRight;  // milliseconds
    private int drowsinessDurationThreshold; // milliseconds
    private int distractionDurationThreshold; // milliseconds
    private float diffInXOfFaceSides;
    private float diffInXOfFacesSidesThreshold;
    private float smallConstant;
    private float get_distance(float x1, float y1, float x2, float y2) {
        return (float) Math.sqrt(Math.pow((x1 - x2), 2) + Math.pow((y1 - y2), 2));
    }

    private float getDistanceOneDimension(float x1, float x2) {
        return x1 - x2;
    }

    public UserStatusDetector() {
        this.distanceLeftEye = 0;
        this.drowsyDistanceThreshold = 0.15F;
        this.elapsedTime = 0;
        this.drowsinessDurationThreshold = 5000;
        this.diffInXOfFaceSides = 0;
        this.diffInXOfFacesSidesThreshold = 0.8F;
        this.distractionDurationThreshold = 3000;
        this.smallConstant = 0.00001f;
    }

    public boolean isDrowsy(FaceMeshResult result, long curTime, long prevTime) {
        try {
            // detecting drowsiness for the first detected face
            List<NormalizedLandmark> lmList = result.multiFaceLandmarks().get(0).getLandmarkList();
            //        Log.i("Msg", String.valueOf(lmList.size())); //478
            NormalizedLandmark leftEyeTop = lmList.get(159);
            NormalizedLandmark leftEyeBottom = lmList.get(145);
            NormalizedLandmark leftEyeLeft = lmList.get(33);
            NormalizedLandmark leftEyeRight = lmList.get(133);
            NormalizedLandmark rightEyeTop = lmList.get(386);
            NormalizedLandmark rightEyeBottom = lmList.get(374);
            NormalizedLandmark rightEyeLeft = lmList.get(362);
            NormalizedLandmark rightEyeRight = lmList.get(263);
            for (NormalizedLandmark coordinates : lmList) {
                this.distanceLeftEye = get_distance(leftEyeTop.getX(), leftEyeTop.getY(), leftEyeBottom.getX(), leftEyeBottom.getY()) / get_distance(leftEyeLeft.getX(), leftEyeLeft.getY(), leftEyeRight.getX(), leftEyeRight.getY());
                this.distanceRightEye = get_distance(rightEyeTop.getX(), rightEyeTop.getY(), rightEyeBottom.getX(), rightEyeBottom.getY()) / get_distance(rightEyeLeft.getX(), rightEyeLeft.getY(), rightEyeRight.getX(), rightEyeRight.getY());
            }
            if (this.distanceLeftEye <= this.drowsyDistanceThreshold && this.distanceRightEye <= this.drowsyDistanceThreshold) {
                this.elapsedTime = this.elapsedTime + (curTime - prevTime);
                if (this.elapsedTime >= this.drowsinessDurationThreshold) {
                    return true;
                } else {
                    return false;
                }

            } else {
                this.elapsedTime = 0;
                return false;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

    public boolean headFacingLeft(FaceMeshResult result, long curTime, long prevTime) {
        try {
            // the first detected face
            List<NormalizedLandmark> lmList = result.multiFaceLandmarks().get(0).getLandmarkList();
            NormalizedLandmark leftSideFace = lmList.get(234);
            NormalizedLandmark rightSideFace = lmList.get(454);
            NormalizedLandmark topFace = lmList.get(10);
            NormalizedLandmark bottomFace = lmList.get(152);
            for (NormalizedLandmark coordinates : lmList) {
                float normalizedFactor = getDistanceOneDimension(bottomFace.getY(), topFace.getY());
                if (normalizedFactor == 0) {
                    normalizedFactor = smallConstant;
                }
                this.diffInXOfFaceSides = Math.abs(getDistanceOneDimension(leftSideFace.getX(), rightSideFace.getX())) / normalizedFactor;
            }
            if (this.diffInXOfFaceSides <= this.diffInXOfFacesSidesThreshold && leftSideFace.getZ() > rightSideFace.getZ()) {
                this.elapsedTimeTurnLeft = this.elapsedTimeTurnLeft + (curTime - prevTime);
                if (this.elapsedTimeTurnLeft >= this.distractionDurationThreshold) {
                    return true;
                } else {
                    return false;
                }

            } else {
                this.elapsedTimeTurnLeft = 0;
                return false;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

    public boolean headFacingRight(FaceMeshResult result, long curTime, long prevTime) {
        try {
            // the first detected face
            List<NormalizedLandmark> lmList = result.multiFaceLandmarks().get(0).getLandmarkList();
            NormalizedLandmark leftSideFace = lmList.get(234);
            NormalizedLandmark rightSideFace = lmList.get(454);
            NormalizedLandmark topFace = lmList.get(10);
            NormalizedLandmark bottomFace = lmList.get(152);
            for (NormalizedLandmark coordinates : lmList) {
                float normalizedFactor = getDistanceOneDimension(bottomFace.getY(), topFace.getY());
                if (normalizedFactor == 0) {
                    normalizedFactor = smallConstant;
                }
                this.diffInXOfFaceSides = Math.abs(getDistanceOneDimension(leftSideFace.getX(), rightSideFace.getX())) / normalizedFactor;
            }
            if (this.diffInXOfFaceSides <= this.diffInXOfFacesSidesThreshold && leftSideFace.getZ() < rightSideFace.getZ()) {
                this.elapsedTimeTurnRight = this.elapsedTimeTurnRight + (curTime - prevTime);
                if (this.elapsedTimeTurnRight >= this.distractionDurationThreshold) {
                    return true;
                } else {
                    return false;
                }

            } else {
                this.elapsedTimeTurnRight = 0;
                return false;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return false;
    }

    public float getDistanceLeftEye() {
        return this.distanceLeftEye;
    }
    public float getDistanceRightEye() {
        return this.distanceRightEye;
    }

    public float getDiffInXOfFaceSides() {
        return this.diffInXOfFaceSides;
    }
}
