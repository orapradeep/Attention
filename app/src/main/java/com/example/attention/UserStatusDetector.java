package com.example.attention;

import android.util.Log;

import com.google.mediapipe.formats.proto.LandmarkProto.NormalizedLandmark;
import com.google.mediapipe.solutions.facemesh.FaceMeshResult;

import java.util.List;

public class UserStatusDetector {
    // detecting drowsiness for the first detected face
    public void isDrowsy(FaceMeshResult result) {
        List<NormalizedLandmark> lmList = result.multiFaceLandmarks().get(0).getLandmarkList();
//        Log.i("Msg", String.valueOf(lmList.size())); //478
        for (NormalizedLandmark coordinates: lmList) {
            Log.i("coor", String.valueOf(coordinates.getX()) + ", " + String.valueOf(coordinates.getY()));
        }
    }
}
