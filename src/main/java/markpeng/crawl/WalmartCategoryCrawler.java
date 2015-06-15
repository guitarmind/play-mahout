package markpeng.crawl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class WalmartCategoryCrawler {

	private static final String newLine = System.getProperty("line.separator");

	private List<String> apiKeys = null;

	private HttpClientFetcher fetcher = new HttpClientFetcher();

	private Gson gson = new GsonBuilder().setPrettyPrinting().create();

	public WalmartCategoryCrawler(List<String> apiKeys) {
		this.apiKeys = apiKeys;
	}

	public void fetchQueryCategory(List<String> queryList,
			String outputFolderPath, String missingQueryFilePath)
			throws Exception {
		List<String> missing = new ArrayList<String>();
		BufferedWriter missingOut = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(missingQueryFilePath, false), "UTF-8"));

		for (String query : queryList) {
			System.out.println("Querying " + query + " ......");

			String json = getJsonFromAPI(query, apiKeys.get(0));
			JsonElement jelement = new JsonParser().parse(json);
			JsonObject jobject = jelement.getAsJsonObject();

			if (jobject.has("items")) {
				JsonArray itemArray = jobject.getAsJsonArray("items");
				for (final JsonElement e : itemArray) {
					final JsonObject item = e.getAsJsonObject();
					String itemId = item.get("itemId").getAsString();
					String name = item.get("name").getAsString();
					String categoryPath = item.get("categoryPath")
							.getAsString();

					System.out.println("itemId: " + itemId);
					System.out.println("name: " + name);
					System.out.println("categoryPath: " + categoryPath);
				}
				System.out.println("item counts: " + itemArray.size() + "\n");

				// write to JSON file
				BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
						new FileOutputStream(outputFolderPath + "/" + query
								+ ".json", false), "UTF-8"));
				try {
					String prettyJsonString = gson.toJson(jelement);
					out.write(prettyJsonString);
				} finally {
					out.flush();
					out.close();
				}
			} else {
				missing.add(query);
				missingOut.write(query + newLine);
				missingOut.flush();
			}

			Thread.sleep(1000 * 3);
		} // end of for loop

		System.out.println("missing queries: " + missing.size() + "\n");
		missingOut.close();
	}

	public String getJsonFromAPI(String query, String key) throws Exception {
		return fetcher.getHtml("http://api.walmartlabs.com/v1/search?apiKey="
				+ key + "&query=" + query + "&numItems=25", "UTF-8");
	}

	public static void main(String[] args) throws Exception {
		List<String> apiKeys = new ArrayList<String>();
		apiKeys.add("9wdtaxtf3vvbwuys3b3kkp48");
		apiKeys.add("bjaf45tux25jk3h9at845nc6");
		apiKeys.add("tmbq2u6u4pvecgxcbqkacwk7");

		String queryFilePath = "/home/markpeng/Share/Kaggle/Search Results Relevance/dintinct_query_origin_20150613.txt";

		List<String> queryList = new ArrayList<String>();
		// queryList.add("neck pillow");
		// queryList.add("blue mini refrigerator");
		queryList.add("van back pack");
		// BufferedReader in = new BufferedReader(new InputStreamReader(
		// new FileInputStream(queryFilePath), "UTF-8"));
		// try {
		// String aLine = null;
		// while ((aLine = in.readLine()) != null) {
		// String tmp = aLine.toLowerCase().trim();
		// queryList.add(tmp);
		// }
		// } finally {
		// in.close();
		// }

		WalmartCategoryCrawler worker = new WalmartCategoryCrawler(apiKeys);
		worker.fetchQueryCategory(
				queryList,
				"/home/markpeng/Share/Kaggle/Search Results Relevance/walmart/query",
				"/home/markpeng/Share/Kaggle/Search Results Relevance/walmart/walmart_missing_query_20150613.txt");

	}
}
