/**
 * Utility class to calculate some metrics
 */
package com.example.android.camera2video;

import java.util.Collections;
import java.util.LinkedList;

public final class Utils {

    public static Double getLowerQuartile(LinkedList<Emotion> emotion) {
        return getQuartileEmotion(emotion, 0.25);
    }

    public static Double getHigherQuartile(LinkedList<Emotion> emotion) {
        return getQuartileEmotion(emotion, 0.75);
    }

    public static Double getQuartile(LinkedList<Double> values, double which) {
        Collections.sort(values);
        int index = (int) (values.size() * which);
        if (values.size() % 2 == 1) {
            return values.get(index);
        } else {
            return (values.get(index - 1) + values.get(index)) / 2.0;
        }
    }

    public static Double getQuartileEmotion(LinkedList<Emotion> values, double which) {
        Collections.sort(values);
        int index = (int) (values.size() * which);
        if (values.size() % 2 == 1) {
            return values.get(index).getMedian();
        } else {
            return (values.get(index - 1).getMedian() + values.get(index).getMedian()) / 2.0;
        }
    }

}
