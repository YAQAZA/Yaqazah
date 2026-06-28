package com.yaqazah.dashboard.util;

import com.yaqazah.detection.model.DetectionType;

/**
 * Maps detection_event.type to dashboard alert type ids agreed with the frontend:
 * 0 Asleep, 1 Drowsy, 2 Distracted, 3 Phone, 4 LookingAway, 5 EatingAndDrinking.
 */
public final class AlertTypeMapper {

    private AlertTypeMapper() {
    }

    public static Integer toTypeId(DetectionType type) {
        if (type == null) {
            return null;
        }
        return switch (type) {
            case SLEEPINESS -> 0;
            case DROWSINESS -> 1;
            case DISTRACTION -> 2;
            case PHONE -> 3;
            case LOOKING_AWAY -> 4;
            case EATING_AND_DRINKING -> 5;
            case NORMAL -> null;
        };
    }

    public static boolean isTrendType(int typeId) {
        return typeId >= 0 && typeId <= 2;
    }

    public static boolean isPieType(int typeId) {
        return typeId >= 3 && typeId <= 5;
    }
}
