package markpeng.crawl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.KStemFilter;
import org.apache.lucene.analysis.miscellaneous.WordDelimiterFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.search.spell.NGramDistance;
import org.apache.lucene.util.Version;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class WalmartCategoryParser {

	private static final String newLine = System.getProperty("line.separator");

	private static final double MIN_SIMILARITY = 0.75;

	public String[] extractMostMatchedCategory(String targetFolder,
			String jsonFile, String compoundPath) throws Exception {
		String[] outputCategory = new String[2];

		NGramDistance similarityTool = new NGramDistance(2);
		List<String> compounds = readFile(compoundPath);

		String json = readJson(targetFolder + "/" + jsonFile);
		List<Item> items = parseItems(json);
		if (items.size() > 0) {
			Map<String, Integer> wordMap = new HashMap<String, Integer>();
			Map<String, Integer> mostMatchedWordMap = new HashMap<String, Integer>();

			String cleanQuery = jsonFile.replace(".json", "");

			for (Item item : items) {

				// preprocessing
				String cleanProductTitle = processTextByLuceneWithKStem(item
						.getTitle().toLowerCase());

				// replace compounds
				for (String c : compounds) {
					String tmp = c.replace(" ", "");
					if (cleanProductTitle.contains(tmp))
						cleanProductTitle = cleanProductTitle.replace(tmp, c);
				}

				Hashtable<String, Integer> qTokens = getTermFreqByLucene(
						cleanQuery, true, true);
				Hashtable<String, Integer> titleTokens = getTermFreqByLucene(
						cleanProductTitle, true, true);

				int titleMatched = 0;
				for (String q : qTokens.keySet()) {
					if (titleTokens.containsKey(q)) {
						titleMatched++;
					} else {
						for (String t : titleTokens.keySet()) {
							float sim = similarityTool.getDistance(q, t);
							if (sim >= MIN_SIMILARITY) {
								titleMatched++;
								break;
							}
						}
					}

				}

				// qInTitle
				double qInTitle = (double) titleMatched / qTokens.size();

				if (qInTitle >= 0.7) {
					if (!mostMatchedWordMap.containsKey(item.getCategoryPath()))
						mostMatchedWordMap.put(item.getCategoryPath(), 1);
					else
						mostMatchedWordMap
								.put(item.getCategoryPath(), mostMatchedWordMap
										.get(item.getCategoryPath()) + 1);
				}

				if (!wordMap.containsKey(item.getCategoryPath()))
					wordMap.put(item.getCategoryPath(), 1);
				else
					wordMap.put(item.getCategoryPath(),
							wordMap.get(item.getCategoryPath()) + 1);
			}

			// get Top-1 category path
			List<Entry<String, Integer>> categories = sortByValue(wordMap);
			List<Entry<String, Integer>> mostMatchedCategories = sortByValue(mostMatchedWordMap);

			outputCategory[0] = cleanQuery;
			if (mostMatchedCategories.size() > 0) {
				outputCategory[1] = mostMatchedCategories.get(0).getKey();
				System.out.println("Most-matched category for " + jsonFile
						+ ": " + mostMatchedCategories.get(0).getKey());
			} else {
				// System.out.println("Majority category for " + jsonFile + ": "
				// + categories.get(0).getKey());
				// outputCategory[1] = categories.get(0).getKey();
				outputCategory[1] = "Unknown";
			}

		} else {
			System.out.println("Unknown category for " + jsonFile);
			outputCategory[0] = jsonFile.replace(".json", "");
			outputCategory[1] = "Unknown";
		}

		return outputCategory;
	}

	/**
	 * Not precise.
	 * 
	 * @param targetFolder
	 * @param jsonFile
	 * @return
	 * @throws Exception
	 */
	public String[] extractMajorityCategory(String targetFolder, String jsonFile)
			throws Exception {
		String[] outputCategory = new String[2];

		String json = readJson(targetFolder + "/" + jsonFile);
		List<Item> items = parseItems(json);
		if (items.size() > 0) {
			Map<String, Integer> wordMap = new HashMap<String, Integer>();
			for (Item item : items) {
				if (!wordMap.containsKey(item.getCategoryPath()))
					wordMap.put(item.getCategoryPath(), 1);
				else
					wordMap.put(item.getCategoryPath(),
							wordMap.get(item.getCategoryPath()) + 1);
			}

			// get Top-1 category path
			List<Entry<String, Integer>> categories = sortByValue(wordMap);
			System.out.println("Majority category for " + jsonFile + ": "
					+ categories.get(0).getKey());

			outputCategory[0] = jsonFile.replace(".json", "");
			outputCategory[1] = categories.get(0).getKey();

		} else {
			System.out.println("Unknown category for " + jsonFile);
		}

		return outputCategory;
	}

	public List<Item> parseItems(String json) {
		List<Item> items = new ArrayList<Item>();

		JsonElement jelement = new JsonParser().parse(json);
		if (jelement != null && jelement.isJsonObject()) {
			JsonObject jobject = jelement.getAsJsonObject();

			if (jobject != null && jobject.has("items")) {
				JsonArray itemArray = jobject.getAsJsonArray("items");
				for (final JsonElement e : itemArray) {
					final JsonObject itemObj = e.getAsJsonObject();
					String itemId = itemObj.get("itemId").getAsString();
					String name = itemObj.get("name").getAsString();
					String categoryPath = "Unknown";
					if (itemObj.has("categoryPath"))
						categoryPath = itemObj.get("categoryPath")
								.getAsString();

					System.out.println("itemId: " + itemId);
					System.out.println("name: " + name);
					System.out.println("categoryPath: " + categoryPath + "\n");

					Item item = new Item(itemId, name, categoryPath);
					items.add(item);
				}
				System.out.println("item counts: " + itemArray.size() + "\n");

			}
		} else
			System.out.println("wrong json: " + json);
		return items;
	}

	public String readJson(String filePath) throws Exception {
		StringBuffer output = new StringBuffer();

		BufferedReader in = new BufferedReader(new InputStreamReader(
				new FileInputStream(filePath), "UTF-8"));

		try {
			String aLine = null;
			while ((aLine = in.readLine()) != null) {
				output.append(aLine + newLine);
			}
		} finally {
			in.close();
		}

		return output.toString();
	}

	public List<Entry<String, Integer>> sortByValue(Map<String, Integer> wordMap) {

		Set<Entry<String, Integer>> set = wordMap.entrySet();
		List<Entry<String, Integer>> list = new ArrayList<Entry<String, Integer>>(
				set);
		Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
			public int compare(Map.Entry<String, Integer> o1,
					Map.Entry<String, Integer> o2) {
				return (o2.getValue()).compareTo(o1.getValue());
			}
		});
		return list;
	}

	public List<String> readFile(String filePath) throws Exception {
		List<String> result = new ArrayList<String>();
		BufferedReader in = new BufferedReader(new InputStreamReader(
				new FileInputStream(filePath), "UTF-8"));

		try {
			String aLine = null;
			while ((aLine = in.readLine()) != null) {
				String tmp = aLine.toLowerCase().trim();
				if (tmp.length() > 0 && !result.contains(tmp))
					result.add(tmp);
			}
		} finally {
			in.close();
		}

		return result;
	}

	public String processTextByLuceneWithKStem(String text) throws IOException {
		String result = text;

		StringBuffer postText = new StringBuffer();
		Set stopWords = new StandardAnalyzer(Version.LUCENE_46)
				.getStopwordSet();
		TokenStream ts = new StandardTokenizer(Version.LUCENE_46,
				new StringReader(text));
		ts = new StopFilter(Version.LUCENE_46, ts, (CharArraySet) stopWords);
		int flags = WordDelimiterFilter.SPLIT_ON_NUMERICS
				| WordDelimiterFilter.SPLIT_ON_CASE_CHANGE
				| WordDelimiterFilter.GENERATE_NUMBER_PARTS
				| WordDelimiterFilter.GENERATE_WORD_PARTS;
		ts = new WordDelimiterFilter(ts, flags, null);
		ts = new LowerCaseFilter(Version.LUCENE_46, ts);
		// ts = new PorterStemFilter(ts);
		// ts = new DictionaryCompoundWordTokenFilter(Version.LUCENE_46, ts,
		// new CharArraySet(Version.LUCENE_46, dictionary, true), 6, 4,
		// 10, false);

		// less aggressive, better than PorterStemFilter!
		ts = new KStemFilter(ts);

		try {
			CharTermAttribute termAtt = ts
					.addAttribute(CharTermAttribute.class);
			ts.reset();
			int wordCount = 0;
			while (ts.incrementToken()) {
				if (termAtt.length() > 0) {
					String word = termAtt.toString();

					postText.append(word + " ");
					// System.out.println(word);

					wordCount++;
				}
			}

			String finalText = postText.toString().trim().replace("'", "");
			if (finalText.length() > 1)
				result = finalText;

		} finally {
			// Fixed error : close ts:TokenStream
			ts.end();
			ts.close();
		}

		return result;
	}

	private Hashtable<String, Integer> getTermFreqByLucene(String text)
			throws IOException {
		return getTermFreqByLucene(text, true, false);
	}

	private Hashtable<String, Integer> getTermFreqByLucene(String text,
			boolean english, boolean digits) throws IOException {
		Hashtable<String, Integer> result = new Hashtable<String, Integer>();

		Set stopWords = new StandardAnalyzer(Version.LUCENE_46)
				.getStopwordSet();
		TokenStream ts = new StandardTokenizer(Version.LUCENE_46,
				new StringReader(text));
		ts = new StopFilter(Version.LUCENE_46, ts, (CharArraySet) stopWords);
		// ts = new PorterStemFilter(ts);
		try {
			CharTermAttribute termAtt = ts
					.addAttribute(CharTermAttribute.class);
			ts.reset();
			int wordCount = 0;
			while (ts.incrementToken()) {
				if (termAtt.length() > 0) {
					String word = termAtt.toString();

					boolean valid = false;
					if (english && !digits)
						valid = isAllEnglish(word);
					else if (!english && digits)
						valid = isAllDigits(word);
					else if (english && digits)
						valid = isAllEnglishAndDigits(word);

					if (valid) {
						if (result.get(word) == null)
							result.put(word, 1);
						else {
							result.put(word, result.get(word) + 1);
						}
					}

					wordCount++;
				}
			}

		} finally {
			// Fixed error : close ts:TokenStream
			ts.end();
			ts.close();
		}

		return result;
	}

	public boolean isAllEnglish(String text) {
		boolean result = true;

		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (!Character.isAlphabetic(c) && c != '-') {
				result = false;
				break;
			}
		}

		return result;
	}

	public boolean isAllDigits(String text) {
		boolean result = true;

		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			if (!Character.isDigit(c)) {
				result = false;
				break;
			}
		}

		return result;
	}

	public boolean isAllEnglishAndDigits(String text) {
		boolean result = true;

		String[] tokens = text.split("\\s");

		for (String token : tokens) {
			for (int i = 0; i < token.length(); i++) {
				char c = token.charAt(i);
				if (!Character.isAlphabetic(c) && c != '-'
						&& !Character.isDigit(c)) {
					result = false;
					break;
				}
			}
		}

		return result;
	}

	private class Item {
		private String id = "";
		private String title = "";
		private String categoryPath = "";

		public Item(String id, String title, String categoryPath) {
			this.id = id;
			this.title = title;
			this.categoryPath = categoryPath;
		}

		public String getId() {
			return id;
		}

		public void setId(String id) {
			this.id = id;
		}

		public String getTitle() {
			return title;
		}

		public void setTitle(String title) {
			this.title = title;
		}

		public String getCategoryPath() {
			return categoryPath;
		}

		public void setCategoryPath(String categoryPath) {
			this.categoryPath = categoryPath;
		}

	}

	public static void main(String[] args) throws Exception {
		WalmartCategoryParser parser = new WalmartCategoryParser();

		String folderPath = args[0];
		String compoundPath = args[1];
		String outputFile = args[2];
		// String folderPath =
		// "/home/markpeng/Share/Kaggle/Search Results Relevance/walmart/query";
		// String compoundPath =
		// "/home/markpeng/Share/Kaggle/Search Results Relevance/english-compound-words.txt";
		// String outputFile =
		// "/../query_walmart_categories_mostMatched_20150616.txt";
		// String outputFile =
		// "/../query_walmart_categories_mostMatched_majority_20150616.txt";

		final File folder = new File(folderPath);

		// write to text file
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(outputFile, false), "UTF-8"));
		try {
			int unknown = 0;
			for (final File fileEntry : folder.listFiles()) {
				// String[] outputCategory = parser.extractMajorityCategory(
				// folderPath, fileEntry.getName());
				String[] outputCategory = parser.extractMostMatchedCategory(
						folderPath, fileEntry.getName(), compoundPath);

				out.write("\"" + outputCategory[0] + "\",\""
						+ outputCategory[1] + "\"" + newLine);

				if (outputCategory[1].equals("Unknown"))
					unknown++;
			}

			System.out.println("Total unknown: " + unknown);

		} finally {
			out.flush();
			out.close();
		}
		// parser.extractMajorityCategory(
		// "/home/markpeng/Share/Kaggle/Search Results Relevance/walmart/query",
		// "8 ounce mason jar.json");
	}
}
