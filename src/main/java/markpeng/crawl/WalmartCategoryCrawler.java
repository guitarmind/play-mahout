package markpeng.crawl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

public class WalmartCategoryCrawler {

	private static final String newLine = System.getProperty("line.separator");

	private List<String> apiKeys = null;

	private String currentKey = null;
	private TreeSet<String> availableKeys = new TreeSet<String>();
	private TreeSet<String> usedUpKeys = new TreeSet<String>();

	private HttpClientFetcher fetcher = new HttpClientFetcher();

	private Gson gson = new GsonBuilder().setPrettyPrinting().create();

	public WalmartCategoryCrawler(List<String> apiKeys) {
		this.apiKeys = apiKeys;

		availableKeys.addAll(apiKeys);
		currentKey = availableKeys.pollFirst();
	}

	public void fetchTitleCategory(List<String> titleList,
			String outputFolderPath, String missingTitleFilePath,
			String fetchedTitleFilePath) throws Exception {
		List<String> missing = new ArrayList<String>();
		BufferedWriter missingOut = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(missingTitleFilePath, true), "UTF-8"));

		List<String> fetched = new ArrayList<String>();

		// read previous fetched titles
		File checker = new File(fetchedTitleFilePath);
		if (checker.exists()) {
			BufferedReader in = new BufferedReader(new InputStreamReader(
					new FileInputStream(fetchedTitleFilePath), "UTF-8"));
			try {
				String aLine = null;
				while ((aLine = in.readLine()) != null) {
					fetched.add(aLine);
				}
			} finally {
				in.close();
			}
		}
		BufferedWriter fetchedOut = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(fetchedTitleFilePath, true), "UTF-8"));

		for (String title : titleList) {
			if (!fetched.contains(title)) {
				System.out.println("Querying " + title + " ......");

				String json = getJsonFromAPI(title);
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
					System.out.println("item counts: " + itemArray.size()
							+ "\n");

					// write to JSON file
					BufferedWriter out = new BufferedWriter(
							new OutputStreamWriter(new FileOutputStream(
									outputFolderPath + "/" + title + ".json",
									false), "UTF-8"));
					try {
						String prettyJsonString = gson.toJson(jelement);
						out.write(prettyJsonString);
					} finally {
						out.flush();
						out.close();
					}

				} else {
					missing.add(title);
					missingOut.write(title + newLine);
					missingOut.flush();
				}

				fetched.add(title);
				fetchedOut.write(title + newLine);
				fetchedOut.flush();

				Thread.sleep(1000 * 3);
			}
		} // end of for loop

		System.out.println("missing titles: " + missing.size() + "\n");
		missingOut.close();
		System.out.println("fetched titles: " + fetched.size() + "\n");
		fetchedOut.close();
	}

	public void fetchQueryCategory(List<String> queryList,
			String outputFolderPath, String missingQueryFilePath)
			throws Exception {
		List<String> missing = new ArrayList<String>();
		BufferedWriter missingOut = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(missingQueryFilePath, false), "UTF-8"));

		for (String query : queryList) {
			System.out.println("Querying " + query + " ......");

			String json = getJsonFromAPI(query);
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

	public static List<String> readTitlesFromCsv(String trainPath,
			String testPath) throws Exception {
		List<String> titleList = new ArrayList<String>();

		BufferedReader trainIn = new BufferedReader(new InputStreamReader(
				new FileInputStream(trainPath), "UTF-8"));

		BufferedReader testIn = new BufferedReader(new InputStreamReader(
				new FileInputStream(testPath), "UTF-8"));

		CsvParserSettings settings = new CsvParserSettings();
		settings.setParseUnescapedQuotes(false);
		settings.getFormat().setLineSeparator("\n");
		settings.getFormat().setDelimiter(',');
		settings.getFormat().setQuote('"');
		settings.setHeaderExtractionEnabled(true);
		settings.setEmptyValue("");
		settings.setMaxCharsPerColumn(40960);

		// -------------------------------------------------------------------------------------------
		// Train Data

		try {
			// creates a CSV parser
			CsvParser trainParser = new CsvParser(settings);

			// call beginParsing to read records one by one, iterator-style.
			trainParser.beginParsing(trainIn);

			int count = 0;
			String[] tokens;
			while ((tokens = trainParser.parseNext()) != null) {
				String id = tokens[0];
				String query = tokens[1].replace("\"", "").trim();
				String productTitle = tokens[2].replace("\"", "").trim();
				String productDesc = tokens[3].replace("\"", "").trim();
				int medianRelevance = Integer.parseInt(tokens[4]);
				double relevance_variance = Double.parseDouble(tokens[5]);

				titleList.add(productTitle);

				count++;
			}

			System.out.println("Total train records: " + count);
		} finally {
			trainIn.close();
		}

		// -------------------------------------------------------------------------------------------
		// Test Data

		try {
			// creates a CSV parser
			CsvParser testParser = new CsvParser(settings);

			// call beginParsing to read records one by one, iterator-style.
			testParser.beginParsing(testIn);

			int count = 0;
			String[] tokens;
			while ((tokens = testParser.parseNext()) != null) {
				String id = tokens[0];
				String query = tokens[1].replace("\"", "").trim();
				String productTitle = tokens[2].replace("\"", "").trim();
				String productDesc = tokens[3].replace("\"", "").trim();

				titleList.add(productTitle);

				count++;
			}

			System.out.println("Total test records: " + count);

		} finally {
			testIn.close();
		}

		return titleList;
	}

	public String getJsonFromAPI(String query) throws Exception {

		try {
			return fetcher.getHtml(
					"http://api.walmartlabs.com/v1/search?apiKey=" + currentKey
							+ "&query=" + query + "&numItems=25", "UTF-8");
		} catch (Exception e) {
			// change anonther key
			usedUpKeys.add(currentKey);
			currentKey = availableKeys.pollFirst();

			return fetcher.getHtml(
					"http://api.walmartlabs.com/v1/search?apiKey=" + currentKey
							+ "&query=" + query + "&numItems=25", "UTF-8");
		}
	}

	public static void main(String[] args) throws Exception {
		List<String> apiKeys = new ArrayList<String>();
		apiKeys.add("9wdtaxtf3vvbwuys3b3kkp48");
		apiKeys.add("bjaf45tux25jk3h9at845nc6");
		apiKeys.add("tmbq2u6u4pvecgxcbqkacwk7");

		String queryFilePath = "/home/markpeng/Share/Kaggle/Search Results Relevance/dintinct_query_origin_20150613.txt";
		// String trainPath =
		// "/home/markpeng/Share/Kaggle/Search Results Relevance/walmart/train_filterred_crawl_markpeng_20150615.csv";
		// String testPath =
		// "/home/markpeng/Share/Kaggle/Search Results Relevance/walmart/test_filterred_crawl_markpeng_20150615.csv";

		String trainPath = "walmart/train_filterred_crawl_markpeng_20150615.csv";
		String testPath = "walmart/test_filterred_crawl_markpeng_20150615.csv";

		// List<String> queryList = new ArrayList<String>();
		// queryList.add("neck pillow");
		// queryList.add("blue mini refrigerator");
		// queryList.add("van back pack");
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

		List<String> titleList = WalmartCategoryCrawler.readTitlesFromCsv(
				trainPath, testPath);

		WalmartCategoryCrawler worker = new WalmartCategoryCrawler(apiKeys);
		// worker.fetchQueryCategory(
		// queryList,
		// "/home/markpeng/Share/Kaggle/Search Results Relevance/walmart/query",
		// "/home/markpeng/Share/Kaggle/Search Results Relevance/walmart/walmart_missing_query_20150613.txt");

		// worker.fetchTitleCategory(
		// titleList,
		// "/home/markpeng/Share/Kaggle/Search Results Relevance/walmart/title",
		// "/home/markpeng/Share/Kaggle/Search Results Relevance/walmart/walmart_missing_title_20150616.txt",
		// "/home/markpeng/Share/Kaggle/Search Results Relevance/walmart/walmart_fetched_title_20150616.txt");

		worker.fetchTitleCategory(titleList, "walmart/title",
				"walmart/walmart_missing_title_20150616.txt",
				"walmart/walmart_fetched_title_20150616.txt");

	}
}
