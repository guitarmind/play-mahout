package markpeng.kaggle.smr;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.miscellaneous.WordDelimiterFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;
import org.jsoup.Jsoup;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class SponsoredFileChecker {

	private static final int BUFFER_LENGTH = 1000;
	private static final String newLine = System.getProperty("line.separator");

	public void extractScriptTokenList(String targetFolder) {
		try {
			File checker = new File(targetFolder);
			if (checker.exists()) {

				List<String> files = new ArrayList<String>();
				for (final File fileEntry : checker.listFiles()) {
					if (fileEntry.getName().contains(".txt")) {
						String tmp = fileEntry.getAbsolutePath();
						files.add(tmp);
					}
				}

				TreeMap<String, Integer> tokenCount = new TreeMap<String, Integer>();
				for (String file : files) {

					BufferedReader in = new BufferedReader(
							new InputStreamReader(new FileInputStream(file),
									"UTF-8"));
					File f = new File(file);
					try {
						StringBuffer htmlStr = new StringBuffer();
						String aLine = null;
						while ((aLine = in.readLine()) != null) {
							htmlStr.append(aLine + newLine);
						}

						List<String> codeTokens = null;
						StringBuffer tmpStr = new StringBuffer();
						Document doc = Jsoup.parse(htmlStr.toString());

						// get all script content
						Elements scriptElements = doc
								.getElementsByTag("script");
						if (scriptElements != null) {
							for (Element element : scriptElements) {
								for (DataNode node : element.dataNodes()) {
									tmpStr.append(node.getWholeData() + " ");
								}
							}

							codeTokens = processTextByLucene(tmpStr.toString());
						}

						// only count one time for each tag
						for (String t : codeTokens) {
							if (!tokenCount.containsKey(t)) {
								tokenCount.put(t, 1);
							} else {
								tokenCount.put(t, tokenCount.get(t) + 1);
							}
						}
					} finally {
						in.close();
					}

					System.out.println("Scanned html file: " + file);
				} // end of for loop

				// for (String tag : tagCount.keySet()) {
				// System.out.println(tag + ": " + tagCount.get(tag));
				// }

				int minDF = 3;
				SortedSet<Map.Entry<String, Integer>> sortedFeatures = entriesSortedByValues(tokenCount);
				int validN = 0;
				for (Map.Entry<String, Integer> m : sortedFeatures) {
					String feature = m.getKey();
					int df = m.getValue();
					if (df >= minDF) {
						System.out.println(feature + ": " + df);
						validN++;
					}
				} // end of feature loop

				System.out.println("Total # of features (DF >= " + minDF
						+ "): " + validN);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void extractTagList(String targetFolder) {
		try {
			File checker = new File(targetFolder);
			if (checker.exists()) {

				List<String> files = new ArrayList<String>();
				for (final File fileEntry : checker.listFiles()) {
					if (fileEntry.getName().contains(".txt")) {
						String tmp = fileEntry.getAbsolutePath();
						files.add(tmp);
					}
				}

				TreeMap<String, Integer> tagCount = new TreeMap<String, Integer>();
				for (String file : files) {

					BufferedReader in = new BufferedReader(
							new InputStreamReader(new FileInputStream(file),
									"UTF-8"));
					File f = new File(file);
					try {
						StringBuffer htmlStr = new StringBuffer();
						String aLine = null;
						while ((aLine = in.readLine()) != null) {
							htmlStr.append(aLine + newLine);
						}

						String processedJS = "";
						StringBuffer tmpStr = new StringBuffer();
						Document doc = Jsoup.parse(htmlStr.toString());

						TreeMap<String, Integer> innerCount = new TreeMap<String, Integer>();
						Elements allNodes = doc.getAllElements();
						for (Element e : allNodes) {
							String tagName = e.tagName();

							if (!innerCount.containsKey(tagName)) {
								innerCount.put(tagName, 1);
							} else {
								innerCount.put(tagName,
										innerCount.get(tagName) + 1);
							}
						}

						// only count one time for each tag
						for (String tagName : innerCount.keySet()) {
							if (!tagCount.containsKey(tagName)) {
								tagCount.put(tagName, 1);
							} else {
								tagCount.put(tagName, tagCount.get(tagName) + 1);
							}
						}
					} finally {
						in.close();
					}

					System.out.println("Scanned html file: " + file);
				} // end of for loop

				// for (String tag : tagCount.keySet()) {
				// System.out.println(tag + ": " + tagCount.get(tag));
				// }

				int minDF = 3;
				SortedSet<Map.Entry<String, Integer>> sortedFeatures = entriesSortedByValues(tagCount);
				int validN = 0;
				for (Map.Entry<String, Integer> m : sortedFeatures) {
					String feature = m.getKey();
					int df = m.getValue();
					if (df >= minDF) {
						System.out.println(feature + ": " + df);
						validN++;
					}
				} // end of feature loop

				System.out.println("Total # of features (DF >= " + minDF
						+ "): " + validN);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public List<String> processTextByLucene(String text) throws IOException {
		List<String> result = new ArrayList<String>();

		// Set stopWords = new StandardAnalyzer(Version.LUCENE_46)
		// .getStopwordSet();
		TokenStream ts = new StandardTokenizer(Version.LUCENE_46,
				new StringReader(text));
		// ts = new StopFilter(Version.LUCENE_46, ts, (CharArraySet) stopWords);
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
		// ts = new KStemFilter(ts);

		try {
			CharTermAttribute termAtt = ts
					.addAttribute(CharTermAttribute.class);
			ts.reset();
			// int wordCount = 0;
			while (ts.incrementToken()) {
				if (termAtt.length() > 0) {
					String word = termAtt.toString();

					if (word.length() > 1 && isAllEnglish(word)
							&& !result.contains(word))
						result.add(word);
					// System.out.println(word);

					// wordCount++;
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

		String[] tokens = text.split("\\s");

		for (String token : tokens) {
			for (int i = 0; i < token.length(); i++) {
				char c = token.charAt(i);
				if (!Character.isAlphabetic(c) && c != '-') {
					result = false;
					break;
				}
			}
		}

		return result;
	}

	public static <K, V extends Comparable<? super V>> SortedSet<Map.Entry<K, V>> entriesSortedByValues(
			Map<K, V> map) {
		SortedSet<Map.Entry<K, V>> sortedEntries = new TreeSet<Map.Entry<K, V>>(
				new Comparator<Map.Entry<K, V>>() {
					@Override
					public int compare(Map.Entry<K, V> e1, Map.Entry<K, V> e2) {
						int res = e1.getValue().compareTo(e2.getValue());
						if (res > 0)
							return -1;
						if (res < 0)
							return 1;
						else
							return res;
					}
				});
		sortedEntries.addAll(map.entrySet());
		return sortedEntries;
	}

	public static void main(String[] args) {
		String targetFolder = "/home/markpeng/Share/Kaggle/tnative/sponsored";

		SponsoredFileChecker worker = new SponsoredFileChecker();
		// worker.extractTagList(targetFolder);
		worker.extractScriptTokenList(targetFolder);
	}

}
