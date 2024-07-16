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

public class UserStatusDetector extends AppCompatActivity {
    private float distanceLeftEye; // betw the top and the bottom
    private float drowsyDistanceThreshold;
    private float get_distance(float x1, float y1, float x2, float y2) {
        return (float) Math.sqrt(Math.pow((x1 - x2), 2) + Math.pow((y1 - y2), 2));
    }

    public UserStatusDetector() {
        this.distanceLeftEye = 0;
        this.drowsyDistanceThreshold = 0.01F;
    }

    public boolean isDrowsy(FaceMeshResult result) {
        try {
            ImmutableSet<FaceMeshConnections.Connection> leftEyeConnections = FaceMeshConnections.FACEMESH_LEFT_EYE;
            // detecting drowsiness for the first detected face
            List<NormalizedLandmark> lmList = result.multiFaceLandmarks().get(0).getLandmarkList();
            //        Log.i("Msg", String.valueOf(lmList.size())); //478
            for (NormalizedLandmark coordinates : lmList) {
                NormalizedLandmark leftEyeTop = lmList.get(159);
                NormalizedLandmark leftEyeBottom = lmList.get(145);
                this.distanceLeftEye = get_distance(leftEyeTop.getX(), leftEyeTop.getY(), leftEyeBottom.getX(), leftEyeBottom.getY());

            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        if (this.distanceLeftEye <= this.drowsyDistanceThreshold) {
            return true;
        } else {
            return false;
        }
    }

    public float getDistanceLeftEye() {
        return this.distanceLeftEye;
    }
}
