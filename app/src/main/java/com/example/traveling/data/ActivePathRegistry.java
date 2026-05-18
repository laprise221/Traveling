package com.example.traveling.data;

import com.example.traveling.model.TravelPath;
import java.util.List;

public class ActivePathRegistry {
    private static TravelPath activePath;
    private static int currentStepIndex = 0;
    private static List<double[]> cachedRoutePoints = null;

    public static void start(TravelPath path) {
        activePath = path;
        currentStepIndex = 0;
        cachedRoutePoints = null;
    }

    public static void stop() {
        activePath = null;
        currentStepIndex = 0;
        cachedRoutePoints = null;
    }

    public static void cacheRoute(List<double[]> points) {
        cachedRoutePoints = points;
    }

    public static List<double[]> getCachedRoute() {
        return cachedRoutePoints;
    }

    public static TravelPath getActivePath() {
        return activePath;
    }

    public static boolean isActive() {
        return activePath != null;
    }

    public static int getCurrentStepIndex() {
        return currentStepIndex;
    }

    public static void advanceStep() {
        if (activePath != null && activePath.getSteps() != null
                && currentStepIndex < activePath.getSteps().size() - 1) {
            currentStepIndex++;
        }
    }
}
