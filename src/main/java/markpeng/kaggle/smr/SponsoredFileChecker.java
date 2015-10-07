package markpeng.kaggle.smr;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class SponsoredFileChecker {

	private static final int BUFFER_LENGTH = 1000;
	private static final String newLine = System.getProperty("line.separator");

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
		worker.extractTagList(targetFolder);
	}

}
