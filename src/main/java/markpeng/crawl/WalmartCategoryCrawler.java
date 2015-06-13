package markpeng.crawl;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class WalmartCategoryCrawler {

	private List<String> apiKeys = null;

	private HttpClientFetcher fetcher = new HttpClientFetcher();

	public WalmartCategoryCrawler(List<String> apiKeys) {
		this.apiKeys = apiKeys;
	}

	public void fetchQueryCategory(List<String> queryList) throws Exception {
		for (String query : queryList) {
			String json = getJsonFromAPI(query, apiKeys.get(0));
			JsonElement jelement = new JsonParser().parse(json);
			JsonObject jobject = jelement.getAsJsonObject();
			JsonArray itemArray = jobject.getAsJsonArray("items");
			for (final JsonElement e : itemArray) {
				final JsonObject item = e.getAsJsonObject();
				String itemId = item.get("itemId").getAsString();
				String name = item.get("name").getAsString();
				String categoryPath = item.get("categoryPath").getAsString();

				System.out.println("itemId: " + itemId);
				System.out.println("name: " + name);
				System.out.println("categoryPath: " + categoryPath + "\n");
			}

		}
	}

	public String getJsonFromAPI(String query, String key) throws Exception {
		return fetcher.getHtml("http://api.walmartlabs.com/v1/search?apiKey="
				+ key + "&query=" + query, "UTF-8");
	}

	public static void main(String[] args) throws Exception {
		List<String> apiKeys = new ArrayList<String>();
		apiKeys.add("9wdtaxtf3vvbwuys3b3kkp48");
		apiKeys.add("bjaf45tux25jk3h9at845nc6");
		apiKeys.add("tmbq2u6u4pvecgxcbqkacwk7");

		List<String> queryList = new ArrayList<String>();
		queryList.add("neck pillow");

		WalmartCategoryCrawler worker = new WalmartCategoryCrawler(apiKeys);
		worker.fetchQueryCategory(queryList);

	}

}
