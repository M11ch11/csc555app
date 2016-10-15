import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

// get response to picture given by url
// next step: use uploaded picture


public class Main {
	private final static String MEDIA_URL = "http://www.elitereaders.com/wp-content/uploads/2015/09/selfie-deaths-3.jpg";

	public static void getReq() {

		//set data for POST, kairos id and key
		HttpPost httppost = new HttpPost("https://api.kairos.com/v2/media?source="+MEDIA_URL);
		httppost.setHeader("app_id", "4985f625");
		httppost.setHeader("app_key", "4423301b832793e217d04bc44eb041d3");

		//request and print response
		try {
			CloseableHttpClient httpcl = HttpClients.createDefault();
			CloseableHttpResponse response = httpcl.execute(httppost);
			System.out.println(response.getStatusLine());
			HttpEntity responseEntity = response.getEntity();
			System.out.println(responseEntity.getContentType().toString());
			String content = EntityUtils.toString(responseEntity);
			System.out.println(content);
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		getReq();
	}
}