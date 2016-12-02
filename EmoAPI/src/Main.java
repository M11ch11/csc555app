import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

public class Main {

	private final static String KAIROS_URL = "https://api.kairos.com/v2/media";
	private final static String APP_ID = "4985f625";
	private final static String APP_KEY = "4423301b832793e217d04bc44eb041d3";

	/**
	 * !! We assume that there is only one person on the video, because kairos sometimes makes mistakes and identifies the same person as another.
	 * So using this app, there should be only one person on the recorded video
	 * @param mediaId
	 */

	public static void getReq(String mediaId) {
		//set data for POST, kairos id and key
		HttpGet httpget = new HttpGet(KAIROS_URL+"/"+mediaId);
		httpget.setHeader("app_id", APP_ID);
		httpget.setHeader("app_key", APP_KEY);

		System.out.println("-- obtaining emotional analysis --");

		//request and print response
		try {
			int status_code = 0;
			JSONObject obj = null;
			String content = null;
			while (!(status_code == 3 || status_code == 4)) {
				CloseableHttpClient httpcl = HttpClients.createDefault();
				CloseableHttpResponse response = httpcl.execute(httpget);
				System.out.println(response.getStatusLine());
				HttpEntity responseEntity = response.getEntity();
				content = EntityUtils.toString(responseEntity);				
				obj = new JSONObject(content);
				status_code = obj.getInt("status_code");
				System.out.println(status_code);
				Thread.sleep(1000);
			}
			System.out.println(content);
			Person personOnVideo = new Person(0);
			//System.out.println(content);
			JSONArray framesArray = obj.getJSONArray("frames");
			//System.out.println("only frames: " + frames.toString());
			for (int i = 0; i < framesArray.length(); i++) {	// for each frame
				JSONObject framesObj = framesArray.getJSONObject(i);
				//System.out.println(framesObj.toString());
				System.out.println("--Frame " + String.valueOf(framesObj.getInt("time")) + "--");
				JSONArray peopleArray = framesObj.getJSONArray("people");
				for (int p = 0; p < peopleArray.length(); p++) {	//for each person
					JSONObject peopleJSONObj = peopleArray.getJSONObject(p);
					System.out.println(peopleJSONObj.getString("person_id") + " app: " + peopleJSONObj.get("emotions"));
					JSONObject emotionsArray = peopleJSONObj.getJSONObject("emotions");
					//System.out.println("surprise =" + String.valueOf(emotionsArray.getDouble("surprise")));

					personOnVideo.addEmotions(emotionsArray.getDouble("surprise"),
							emotionsArray.getDouble("joy"), emotionsArray.getDouble("sadness"), emotionsArray.getDouble("disgust"),
							emotionsArray.getDouble("anger"), emotionsArray.getDouble("fear"));
				}
			}
			System.out.println("Person " + personOnVideo.toString() + "emotion: " + personOnVideo.getHighestEmotion());
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static void getEmotions(String filePath) throws IOException, JSONException, InterruptedException {
		System.out.println("-- uploading file --");
		CloseableHttpClient httpcl = HttpClients.createDefault();
		try {
			//set up
			HttpPost httppost = new HttpPost(KAIROS_URL);
			httppost.setHeader("app_id", APP_ID);
			httppost.setHeader("app_key", APP_KEY);

			//add file content body
			MultipartEntityBuilder builder = MultipartEntityBuilder.create();
			builder.addBinaryBody("source", new File(filePath));
			HttpEntity entity = builder.build();
			httppost.setEntity(entity);

			//execute command

			CloseableHttpResponse response = httpcl.execute(httppost);
			//System.out.println(httppost.getRequestLine());
			System.out.println(response.getStatusLine());
			HttpEntity responseEntity = response.getEntity();
			String content = EntityUtils.toString(responseEntity);
			System.out.println(content);


			// //in case you need id
			JSONObject obj = new JSONObject(content);
			System.out.println(obj.getString("status_code"));
			System.out.println("id of uploaded file = " + obj.getString("id"));
			getReq(obj.getString("id"));

		} finally {
			httpcl.close();
		}
	}

	public static void main(String[] args) {
		String pathToFile = "./src/VID-20161128-WA0038.mp4";
		try {
			getEmotions(pathToFile);
		} catch (IOException | JSONException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}



class Person {
	private int id;
	LinkedList<Emotion> emotions;


	public Person(int id) {
		this.id = id;
		emotions = new LinkedList<Emotion>();
		this.emotions.add(new Emotion("surprise"));
		this.emotions.add(new Emotion("joy"));
		this.emotions.add(new Emotion("sadness"));
		this.emotions.add(new Emotion("disgust"));
		this.emotions.add(new Emotion("anger"));
		this.emotions.add(new Emotion("fear"));
	}

	public void addEmotions(double surprise, double joy, double sadness, double disgust, double anger, double fear) {
		this.emotions.get(this.emotions.indexOf(new Emotion("surprise"))).addValue(surprise);
		this.emotions.get(this.emotions.indexOf(new Emotion("joy"))).addValue(joy);
		this.emotions.get(this.emotions.indexOf(new Emotion("sadness"))).addValue(sadness);
		this.emotions.get(this.emotions.indexOf(new Emotion("disgust"))).addValue(disgust);
		this.emotions.get(this.emotions.indexOf(new Emotion("anger"))).addValue(anger);
		this.emotions.get(this.emotions.indexOf(new Emotion("fear"))).addValue(fear);
	}


	public String getHighestEmotionByMedian() {
		Collections.sort(this.emotions);
		return this.emotions.getLast().getType();
	}

	public String getHighestEmotion() {
		// check for outlier
		// based on Tukey, John W (1977). Exploratory Data Analysis:
		// An outlier is a data point, which has a value outside the range [1.5*0.25quartile, 1.5*0.75quartile]

		// calculate range
		Collections.sort(this.emotions);
		double lowerQuartile = Utils.getLowerQuartile(this.emotions);
		double higherQuartile = Utils.getHigherQuartile(this.emotions);
		double iqr = higherQuartile - lowerQuartile;					//interquartile ranges
		double lowerBound = lowerQuartile - 1.5*iqr;
		double higherBound = higherQuartile + 1.5*iqr;

		System.out.println("outlier < " + lowerBound + " or > " + higherBound);

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

		// if there is none, add the pos, add the neg, compare => positive, negative resonse
		// assume that surprise and joy are positive, sadness, disgust, anger and fear are negative emotions
		// compare if the means of the positive and the negative emotions are about equal or not
		// according to https://github.com/kairosinc/api-examples/blob/master/demo/emotion/js/highchartsApp.js:100, the maximum value of an emotion is 100
		// we say, if they are in the not more than 10 points away from each other => neutral
		double meanPositive = (this.emotions.get(this.emotions.indexOf(new Emotion("surprise"))).getMedian() + 
				this.emotions.get(this.emotions.indexOf(new Emotion("joy"))).getMedian()) / 2.0;

		double meanNegative = (this.emotions.get(this.emotions.indexOf(new Emotion("sadness"))).getMedian() + 
				this.emotions.get(this.emotions.indexOf(new Emotion("disgust"))).getMedian() + 
				this.emotions.get(this.emotions.indexOf(new Emotion("anger"))).getMedian() + 
				this.emotions.get(this.emotions.indexOf(new Emotion("fear"))).getMedian() / 4.0);

		System.out.println("meanPositive = " + meanPositive + ", meanNegative = " + meanNegative);

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
		} else if  (median > o.median) {
			return 1;
		} else {
			return -1;
		}
	}

}



