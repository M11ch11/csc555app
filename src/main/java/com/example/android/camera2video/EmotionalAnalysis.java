package com.example.android.camera2video;

/**
 * Emotion API Calls
 * This class manages all API Calls for obtaining a overall emotion from kairos for a mp4 video
 */

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public final class EmotionalAnalysis {

    private final static String KAIROS_URL = "https://api.kairos.com/v2/media";
    private final static String APP_ID = "***";
    private final static String APP_KEY = "***";

    /**
     * Returns the overall emotion to a mp4 video
     *
     * @param filePath the absolute path to the video
     * @return the overall emotion
     * @throws JSONException
     * @throws InterruptedException
     * @throws IOException
     */
    public static String getEmotion(String filePath) throws JSONException, InterruptedException, IOException {
        String mediaId = getIDFromMedia(filePath);
        Log.d("media id  by kairos", String.valueOf(mediaId));
        String completeEmotionalResponse = pollEmotionAnalysis(mediaId);
        Log.d("emot. resp by kairos", String.valueOf(completeEmotionalResponse));
        return getOverallEmotion(completeEmotionalResponse);
    }

    /**
     * Uploads the media to kairos and creates the required id for the uploaded media
     *
     * @param filePath the media to upload
     * @return the id, which kairos creates for the uploaded media
     * @throws IOException          occurs when there is a error with reading the file to upload
     * @throws JSONException        occurs when there is an error with the response
     * @throws InterruptedException
     */
    private static String getIDFromMedia(String filePath) throws IOException, JSONException, InterruptedException {
        //thanks to http://stackoverflow.com/questions/19026256/how-to-upload-multipart-form-data-and-image-to-server-in-android
        final String LINE_FEED = "\r\n";
        List<String> response = new ArrayList<>();

        //uploading media is done via a POST form upload
        URL url = new URL(KAIROS_URL);
        HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        httpConn.setUseCaches(false);
        httpConn.setDoOutput(true); // indicates POST method
        httpConn.setDoInput(true);
        httpConn.setRequestProperty("Content-Type", "multipart/form-data; boundary=*****boundary*****");
        httpConn.setRequestProperty("app_id", APP_ID);
        httpConn.setRequestProperty("app_key", APP_KEY);

        OutputStream outputStream = httpConn.getOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, "UTF-8"), true);

        //add file
        writer.append("--*****boundary*****").append(LINE_FEED);
        writer.append("Content-Disposition: form-data; name=\"source\"; " +
                "filename=\"" + filePath + "\"").append(LINE_FEED);
        writer.append("Content-Type: " +
                URLConnection.guessContentTypeFromName(filePath)).append(LINE_FEED);
        writer.append("Content-Transfer-Encoding: binary").append(LINE_FEED);
        writer.append(LINE_FEED);
        writer.flush();

        FileInputStream inputStream = new FileInputStream(new File(filePath));
        byte[] buffer = new byte[4096];
        int bytesRead = -1;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        outputStream.flush();
        inputStream.close();

        writer.append(LINE_FEED);
        writer.flush();

        writer.append(LINE_FEED).flush();
        writer.append("--*****boundary*****--").append(LINE_FEED);
        writer.close();
        int status = httpConn.getResponseCode();
        if ((status == HttpURLConnection.HTTP_OK) || (status == HttpURLConnection.HTTP_ACCEPTED)) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    httpConn.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                response.add(line);
            }
            reader.close();
            httpConn.disconnect();
        } else {
            throw new IOException("status code: " + status);
        }

        //get the response and put it into a json array to extract the id
        String res = "";
        for (String line : response) {
            res += line;
        }


        //Log.d("Result_UP: ", res);    //whole response

        JSONObject obj = new JSONObject(res);
        return obj.getString("id");
    }

    /**
     * Asks the kairos server for the results of the emotional analyses. It polls every second.
     *
     * @param id the id of the media to be analyzed
     * @return the complete JSON response as a String
     * @throws IOException          Occurs when something with the URL or HTTP connection went wrong
     * @throws JSONException        Occurs when there's an error by receiving the emotional analysis response
     * @throws InterruptedException Occurs when the Thread is interrupted
     */
    private static String pollEmotionAnalysis(String id) throws IOException, JSONException, InterruptedException {
        List<String> response = new ArrayList<>();
        URL url = new URL(KAIROS_URL + "/" + id);   // needed / for GET
        int status_code_analysis = 0;

        //------------------------------------
        // Poll as long as http's status is ok or accepted and the kairos status code indicated that kairos is still working
        // see https://github.com/kairosinc/api-examples/blob/master/demo/emotion/js/emoDemoApp.js
        // POLL KAIROS API WITH MEDIA ID FOR JSON RESPONSE
        // RESPONSES:
        //      "code": 3002, "message": "Invalid API Call"
        //      "status_code": 1,  "status_message": "In_Progress"
        //      "status_code": 2,  "status_message": "Analyzing"
        //      "status_code": 3,  "status_message": "Error: Media record not found"
        //      "status_code": 4,  "status_message": "Complete"
        //------------------------------------
        while (!(status_code_analysis == 3 || status_code_analysis == 4)) {
            HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();

            //using GET and the obtained media id to obtain the emotional response
            httpConn.setUseCaches(false);
            httpConn.setDoInput(true);
            httpConn.setRequestMethod("GET");
            httpConn.setRequestProperty("app_id", APP_ID);
            httpConn.setRequestProperty("app_key", APP_KEY);
            httpConn.connect();

            int responseCode = httpConn.getResponseCode();
            if ((responseCode == HttpURLConnection.HTTP_OK) || (responseCode == HttpURLConnection.HTTP_ACCEPTED)) {
                response.clear();
                BufferedReader reader = new BufferedReader(new InputStreamReader(httpConn.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    response.add(line);
                }
                JSONObject obj = new JSONObject(response.toString().substring(1, response.toString().length() - 1));
                status_code_analysis = obj.getInt("status_code");
                reader.close();
                Thread.sleep(1000);
            }
        }
        return response.toString();
    }

    /**
     * Returns the overall emotion of one person in the uploaded media
     * !! We assume that there is only one person on the video, because kairos
     * sometimes makes mistakes and identifies the same person as another.
     * So using this app, there should be only one person on the recorded video
     *
     * @param completeEmotionalResponse the JSON response
     * @return the overall emotion of all people on media
     * @throws JSONException
     */
    private static String getOverallEmotion(String completeEmotionalResponse) throws JSONException {
        Person personOnVideo = new Person(0);

        JSONObject obj = new JSONObject(completeEmotionalResponse.substring(1, completeEmotionalResponse.length() - 1));
        //Console.d("",content);
        JSONArray framesArray = obj.getJSONArray("frames");
        //Console.d("","only frames: " + frames.toString());
        for (int i = 0; i < framesArray.length(); i++) {    // for each frame
            JSONObject framesObj = framesArray.getJSONObject(i);
            //Console.d("",framesObj.toString());
            Log.d("", "--Frame " + String.valueOf(framesObj.getInt("time")) + "--");
            JSONArray peopleArray = framesObj.getJSONArray("people");
            for (int p = 0; p < peopleArray.length(); p++) {    //for each person
                JSONObject peopleJSONObj = peopleArray.getJSONObject(p);
                Log.d("", peopleJSONObj.getString("person_id") + " app: " + peopleJSONObj.get("emotions"));
                JSONObject emotionsArray = peopleJSONObj.getJSONObject("emotions");
                Log.d("", "surprise =" + String.valueOf(emotionsArray.getDouble("surprise")));
                personOnVideo.addEmotions(emotionsArray.getDouble("surprise"),
                        emotionsArray.getDouble("joy"), emotionsArray.getDouble("sadness"), emotionsArray.getDouble("disgust"),
                        emotionsArray.getDouble("anger"), emotionsArray.getDouble("fear"));
            }
        }
        Log.d("Highest Emotion", "Person " + personOnVideo.toString() + "emotion: " + personOnVideo.getHighestEmotion());
        return personOnVideo.getHighestEmotion();
    }
}

/**
 * This class represents the person that is on the video
 * It's attributes are the id and a list of emotions
 * values can be added and the overall emotion of the person can be returned
 */
class Person {
    private int id;
    private LinkedList<Emotion> emotions;

    public Person(int id) {
        this.id = id;
        emotions = new LinkedList<>();
        this.emotions.add(new Emotion("surprise"));
        this.emotions.add(new Emotion("joy"));
        this.emotions.add(new Emotion("sadness"));
        this.emotions.add(new Emotion("disgust"));
        this.emotions.add(new Emotion("anger"));
        this.emotions.add(new Emotion("fear"));
    }

    /**
     * adds the specific emotional values to the belonging list
     *
     * @param surprise surprise value
     * @param joy      joy value
     * @param sadness  sadness value
     * @param disgust  disgust value
     * @param anger    anger value
     * @param fear     fear value
     */
    public void addEmotions(double surprise, double joy, double sadness, double disgust, double anger, double fear) {
        this.emotions.get(this.emotions.indexOf(new Emotion("surprise"))).addValue(surprise);
        this.emotions.get(this.emotions.indexOf(new Emotion("joy"))).addValue(joy);
        this.emotions.get(this.emotions.indexOf(new Emotion("sadness"))).addValue(sadness);
        this.emotions.get(this.emotions.indexOf(new Emotion("disgust"))).addValue(disgust);
        this.emotions.get(this.emotions.indexOf(new Emotion("anger"))).addValue(anger);
        this.emotions.get(this.emotions.indexOf(new Emotion("fear"))).addValue(fear);
    }

    @Deprecated
    public String getHighestEmotionByMedian() {
        Collections.sort(this.emotions);
        return this.emotions.getLast().getType();
    }

    /**
     * Returns the overall emotion of the person. It can be a specific emotion like anger,.. if
     * it is distinctive, otherwise it returns positive, negative or neutral
     * See comments for further information
     *
     * @return the overall emotion
     */
    public String getHighestEmotion() {
        // check for outlier
        // based on Tukey, John W (1977). Exploratory Data Analysis:
        // An outlier is a data point, which has a value outside the range [1.5*0.25quartile, 1.5*0.75quartile]

        // calculate range
        Collections.sort(this.emotions);
        double lowerQuartile = Utils.getLowerQuartile(this.emotions);
        double higherQuartile = Utils.getHigherQuartile(this.emotions);
        double iqr = higherQuartile - lowerQuartile;                    //interquartile ranges
        double lowerBound = lowerQuartile - 1.5 * iqr;
        double higherBound = higherQuartile + 1.5 * iqr;

        Log.d("outlier range", "outlier < " + lowerBound + " or > " + higherBound);

        // which is the outlier? (if there is exactly one)
        int countOutlier = 0;
        int indexOfOutlier = -1;
        for (Emotion e : this.emotions) {
            if ((countOutlier == 0) && ((e.getMedian() < lowerBound) || (e.getMedian() > higherBound))) {
                countOutlier++;
                indexOfOutlier = this.emotions.indexOf(e);
                System.out.println("outlier found: " + e.getType() + " = " + e.getMedian());
            }
        }
        if (countOutlier == 1) {
            return this.emotions.get(indexOfOutlier).getType();
        }

        // if there is none or more than one obtain a coarser emotion
        // assume that surprise and joy are positive, sadness, disgust, anger and fear are negative emotions
        // compare if the means of the positive and the negative emotions are about equal or not
        // according to https://github.com/kairosinc/api-examples/blob/master/demo/emotion/js/highchartsApp.js:100,
        // the maximum value of an emotion is 100, but looking at results by using the api, the range is almost never used completely.
        // So we say, if they are not more than 10 points away from each other => neutral
        double meanPositive = (this.emotions.get(this.emotions.indexOf(new Emotion("surprise"))).getMedian() +
                this.emotions.get(this.emotions.indexOf(new Emotion("joy"))).getMedian()) / 2.0;

        double meanNegative = (this.emotions.get(this.emotions.indexOf(new Emotion("sadness"))).getMedian() +
                this.emotions.get(this.emotions.indexOf(new Emotion("disgust"))).getMedian() +
                this.emotions.get(this.emotions.indexOf(new Emotion("anger"))).getMedian() +
                this.emotions.get(this.emotions.indexOf(new Emotion("fear"))).getMedian() / 4.0);

        Log.d("means pos/neg", "meanPositive = " + meanPositive + ", meanNegative = " + meanNegative);

        if (Math.abs(meanPositive - meanNegative) < 10) {
            return "neutral";
        } else if (meanPositive > meanNegative) {
            return "positive";
        } else {
            return "negative";
        }
    }

    @Override
    public String toString() {
        return "Person [id = " + id + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + id;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Person other = (Person) obj;
        if (id != other.id)
            return false;
        return true;
    }


}

/**
 * This class represents one of the emotions surprise,...
 * It contains a list of all its values and there median
 */
class Emotion implements Comparable<Emotion> {
    private String type;
    LinkedList<Double> allValues;
    private double median;

    public Emotion(String type) {
        this.type = type;
        allValues = new LinkedList<>();
    }

    public void addValue(double v) {
        allValues.add(v);
        calculateMedian();
    }

    private void calculateMedian() {
        median = Utils.getQuartile(allValues, 0.5);
    }

    public double getMedian() {
        return median;
    }

    public String getType() {
        return type;
    }

    public boolean equals(Object anObject) {
        return type.equals(((Emotion) anObject).type);
    }

    public String toString() {
        return type.toString();
    }

    @Override
    public int compareTo(Emotion o) {
        if (median == o.median) {
            return 0;
        } else if (median > o.median) {
            return 1;
        } else {
            return -1;
        }
    }

}
