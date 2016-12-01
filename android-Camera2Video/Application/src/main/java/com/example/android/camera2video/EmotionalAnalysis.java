package com.example.android.camera2video;

/**
 * Emotion API Calls
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
    private final static String APP_ID = "4985f625";
    private final static String APP_KEY = "4423301b832793e217d04bc44eb041d3";

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
     * @param id the id of the media to be analyzed
     * @return the complete JSON response as a String
     * @throws IOException Occurs when something with the URL or HTTP connection went wrong
     * @throws JSONException Occurs when there's an error by receiving the emotional analysis response
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
     * Returns the overall emotion of all people in the uploaded media
     * The calculation is done as follows:
     * For each time frame every single person's emotions are saved
     * When all emotions are saved, the median of each emotion (anger,...) is calculated and
     * the emotion with the highest median value is obtained as the overall emotion
     * @param completeEmotionalResponse the JSON response
     * @return the overall emotion of all people on media
     * @throws JSONException
     */
    private static String getOverallEmotion(String completeEmotionalResponse) throws JSONException {
        ArrayList<Person> peopleOnVideo = new ArrayList<>();

        JSONObject obj = new JSONObject(completeEmotionalResponse.substring(1,completeEmotionalResponse.length()-1));
        //Console.d("",content);
        JSONArray framesArray = obj.getJSONArray("frames");
        //Console.d("","only frames: " + frames.toString());
        for (int i = 0; i < framesArray.length(); i++) {	// for each frame
            JSONObject framesObj = framesArray.getJSONObject(i);
            //Console.d("",framesObj.toString());
            Log.d("","--Frame " + String.valueOf(framesObj.getInt("time")) + "--");
            JSONArray peopleArray = framesObj.getJSONArray("people");
            for (int p = 0; p < peopleArray.length(); p++) {	//for each person
                JSONObject peopleJSONObj = peopleArray.getJSONObject(p);
                Log.d("",peopleJSONObj.getString("person_id") + " app: " + peopleJSONObj.get("emotions"));
                JSONObject emotionsArray = peopleJSONObj.getJSONObject("emotions");
                Log.d("","surprise =" + String.valueOf(emotionsArray.getDouble("surprise")));
                //if person already exists, add emotion info for current time frame,
                //if not, create instance for the new person
                Person currentPerson = new Person(peopleJSONObj.getInt("person_id"));
                if (!peopleOnVideo.contains(currentPerson)) {
                    peopleOnVideo.add(currentPerson);
                }
                peopleOnVideo.get(peopleOnVideo.indexOf(currentPerson)).addEmotions(emotionsArray.getDouble("surprise"),
                        emotionsArray.getDouble("joy"), emotionsArray.getDouble("sadness"), emotionsArray.getDouble("disgust"),
                        emotionsArray.getDouble("anger"), emotionsArray.getDouble("fear"));
            }
        }
        Log.d("allPeople", peopleOnVideo.toString());
        String result = "";
        for (Person person : peopleOnVideo) {
            result += (person.toString() + " emotion: " + person.getHighestEmotion());
        }

        return result;
    }
}

class Person {
    private int id;
    private LinkedList<Double> surprise;
    private LinkedList<Double> joy;
    private LinkedList<Double> sadness;
    private LinkedList<Double> disgust;
    private LinkedList<Double> anger;
    private LinkedList<Double> fear;

    public Person(int id) {
        this.id = id;
        this.surprise = new LinkedList<>();
        this.joy = new LinkedList<>();
        this.sadness = new LinkedList<>();
        this.disgust = new LinkedList<>();
        this.anger = new LinkedList<>();
        this.fear = new LinkedList<>();
    }

    public void addEmotions(double d, double e, double f, double g, double h, double i) {
        this.surprise.add(d);
        this.joy.add(e);
        this.sadness.add(f);
        this.disgust.add(g);
        this.anger.add(h);
        this.fear.add(i);
    }

    private Double getMedian(LinkedList<Double> emotion) {
        Collections.sort(emotion);

        int indexMiddle = emotion.size() / 2;
        if (emotion.size()%2 == 1) {
            return emotion.get(indexMiddle);
        } else {
            return (emotion.get(indexMiddle-1) + emotion.get(indexMiddle)) / 2.0;
        }

    }

    public String getHighestEmotion() {
        String highestEmotion = null;
        double highestValue = -1;
        if (getMedian(surprise) > highestValue) {
            highestEmotion = "surprise";
            highestValue = getMedian(surprise);
        }
        if (getMedian(joy) > highestValue) {
            highestEmotion = "joy";
            highestValue = getMedian(joy);
        }
        if (getMedian(sadness) > highestValue) {
            highestEmotion = "sadness";
            highestValue = getMedian(sadness);
        }
        if (getMedian(disgust) > highestValue) {
            highestEmotion = "disgust";
            highestValue = getMedian(disgust);
        }
        if (getMedian(anger) > highestValue) {
            highestEmotion = "anger";
            highestValue = getMedian(anger);
        }
        if (getMedian(fear) > highestValue) {
            highestEmotion = "fear";
            highestValue = getMedian(fear);
        }
        return highestEmotion;
    }

    @Override
    public String toString() {
        return "Person [id=" + id + "]";
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