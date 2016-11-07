import java.io.File;
import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;
import org.json.JSONException;

// get response to picture given by url
// next step: use uploaded picture


public class Main {

	private final static String KAIROS_URL = "https://api.kairos.com/v2/media";
	private final static String APP_ID = "4985f625";
	private final static String APP_KEY = "4423301b832793e217d04bc44eb041d3";

	//	//deprecated
	//	public static void getReq(String mediaId) {
	//		//set data for POST, kairos id and key
	//		HttpGet httpget = new HttpGet(KAIROS_URL+mediaId);
	//		httpget.setHeader("app_id", APP_ID);
	//		httpget.setHeader("app_key", APP_KEY);
	//
	//		System.out.println("-- obtaining emotional analysis --");
	//		
	//		//request and print response
	//		try {
	//			CloseableHttpClient httpcl = HttpClients.createDefault();
	//			CloseableHttpResponse response = httpcl.execute(httpget);
	//			System.out.println(response.getStatusLine());
	//			HttpEntity responseEntity = response.getEntity();
	//			String content = EntityUtils.toString(responseEntity);
	//			System.out.println(content);
	//		} catch (ClientProtocolException e) {
	//			e.printStackTrace();
	//		} catch (IOException e) {
	//			e.printStackTrace();
	//		}
	//	}

	private static void getEmotions(String filePath) throws IOException, JSONException {
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
			//JSONObject obj = new JSONObject(content); 
			//System.out.println("id of uploaded file = " + obj.getString("id"));
		} finally {
			httpcl.close();
		}
	}

	public static void main(String[] args) {
		String pathToFile = "./src/selfie.jpg";
		try {
			getEmotions(pathToFile);
		} catch (IOException | JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}