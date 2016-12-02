import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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

// get response to picture given by url
// next step: use uploaded picture







public class Main {

	private final static String KAIROS_URL = "https://api.kairos.com/v2/media";
	private final static String APP_ID = "4985f625";
	private final static String APP_KEY = "4423301b832793e217d04bc44eb041d3";

	

	//deprecated
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
			ArrayList<Person> peopleOnVideo = new ArrayList<>();
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
					System.out.println("surprise =" + String.valueOf(emotionsArray.getDouble("surprise")));
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
			for (Person person : peopleOnVideo) {
				System.out.println("Person " + person.toString() + "emotion: " + person.getHighestEmotion());
			}
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