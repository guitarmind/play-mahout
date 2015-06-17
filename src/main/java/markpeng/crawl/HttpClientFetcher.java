package markpeng.crawl;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;

import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.CookieStore;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

public class HttpClientFetcher {
	private static final String newLine = System.getProperty("line.separator");

	private static final int REQUEST_TIMEOUT_SECS = 30;

	private CloseableHttpClient httpclient = null;
	private CookieStore cookieStore = null;
	private HttpClientContext localContext = null;

	private Logger logger = null;

	public HttpClientFetcher() {

		logger = Logger.getLogger(HttpClientFetcher.class.getName());
		ConsoleAppender ca = new ConsoleAppender();
		ca.setWriter(new OutputStreamWriter(System.out));
		ca.setLayout(new PatternLayout("%-5p [%t]: %m%n"));
		logger.addAppender(ca);

		// get HTTP client instance
		httpclient = HttpClients.createDefault();

		// cookie and session
		this.cookieStore = new BasicCookieStore();
		this.localContext = HttpClientContext.create();
		this.localContext.setCookieStore(this.cookieStore);

		RequestConfig.Builder requestConfigBuilder = RequestConfig.custom();
		requestConfigBuilder.setConnectTimeout(REQUEST_TIMEOUT_SECS * 1000);
		requestConfigBuilder
				.setConnectionRequestTimeout(REQUEST_TIMEOUT_SECS * 1000);
		requestConfigBuilder.setSocketTimeout(REQUEST_TIMEOUT_SECS * 1000);
		requestConfigBuilder.setRedirectsEnabled(true);
		// requestConfigBuilder.setRedirectsEnabled(false);
		this.localContext.setRequestConfig(requestConfigBuilder.build());
	}

	public String getHtml(String url, String charsetEncoding) throws Exception {
		HttpGet httpGet = new HttpGet(toASCIIUrl(url));

		logger.info("HttpClient: fetching url: " + url);

		CloseableHttpResponse responseGet = httpclient.execute(httpGet,
				localContext);
		responseGet
				.setHeader("Accept",
						"text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
		responseGet.setHeader("Accept-Language",
				"zh-TW,zh;q=0.8,en-US;q=0.6,en;q=0.4,ja;q=0.2,zh-CN;q=0.2");
		responseGet
				.setHeader(
						"User-Agent",
						"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/31.0.1650.57 Safari/537.36");

		try {
			StatusLine status = responseGet.getStatusLine();
			logger.info(status.toString());

			if (status.getStatusCode() != 200)
				throw new Exception("Bad status code: "
						+ status.getStatusCode());

			HttpEntity entity = responseGet.getEntity();
			if (entity != null) {
				Charset charset = null;
				ContentType contentType = ContentType.get(entity);
				if (contentType != null)
					charset = contentType.getCharset();
				if (charset == null)
					charset = Charset.forName(charsetEncoding);

				InputStream is = entity.getContent();

				StringBuffer buffer = new StringBuffer();
				BufferedReader br = new BufferedReader(new InputStreamReader(
						is, charset));
				String line = null;
				while ((line = br.readLine()) != null) {
					buffer.append(line + newLine);
				}
				br.close();
				is.close();

				logger.info("Content Length: " + buffer.length());

				// return EntityUtils.toString(entity);
				return buffer.toString();
			}
		} finally {
			responseGet.close();
		}

		return "";
	}

	public void close() throws Exception {
		if (httpclient != null)
			httpclient.close();
	}

	/**
	 * Convert any possible Non-ASCII url into valid ASCII url format.
	 * 
	 * @param url
	 * @return
	 * @throws Exception
	 */
	public String toASCIIUrl(String url) throws Exception {
		URL currentUrlObj = new URL(url);
		URI currentUri = new URI(currentUrlObj.getProtocol(),
				currentUrlObj.getHost(), currentUrlObj.getPath(),
				currentUrlObj.getQuery(), null);
		return currentUri.toASCIIString();
	}

	public static void main(String[] args) throws Exception {
		HttpClientFetcher fetcher = new HttpClientFetcher();

		String url = "http://api.walmartlabs.com/v1/search?apiKey=tmbq2u6u4pvecgxcbqkacwk7&query=neck pillow";

		String encoding = "UTF-8";
		String result = fetcher.getHtml(url, encoding);
		System.out.println("----------------------------------------");
		System.out.println(result);
		System.out.println("----------------------------------------");

		fetcher.close();
	}
}
