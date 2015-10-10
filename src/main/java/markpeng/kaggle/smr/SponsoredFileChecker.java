package markpeng.kaggle.smr;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.miscellaneous.WordDelimiterFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class SponsoredFileChecker {

	private static final int BUFFER_LENGTH = 1000;
	private static final String newLine = System.getProperty("line.separator");

	public void extractAttributeList(String targetFolder, String attrListPath)
			throws Exception {
		try {
			List<String> fileTypes = new ArrayList<String>();
			// extract attributes lists
			BufferedReader in = new BufferedReader(new InputStreamReader(
					new FileInputStream(attrListPath), "UTF-8"));
			String aLine = null;
			while ((aLine = in.readLine()) != null) {
				String[] tokens = aLine.split("\t");
				if (tokens.length >= 2) {
					String type = tokens[0].trim().toLowerCase()
							.replace("*", "");
					if (!fileTypes.contains(type))
						fileTypes.add(type);
				}
			}
			in.close();

			System.out.println(fileTypes);
			System.out.println("Attribute count: " + fileTypes.size());

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

					System.out.println("Scanning html file: " + file);

					in = new BufferedReader(new InputStreamReader(
							new FileInputStream(file), "UTF-8"));
					File f = new File(file);
					try {
						StringBuffer htmlStr = new StringBuffer();
						aLine = null;
						while ((aLine = in.readLine()) != null) {
							htmlStr.append(aLine + newLine);
						}

						Document doc = Jsoup.parse(htmlStr.toString());

						TreeMap<String, Integer> innerCount = new TreeMap<String, Integer>();
						Elements allNodes = doc.getAllElements();
						for (Element e : allNodes) {
							Attributes attrs = e.attributes();

							for (Attribute a : attrs) {
								String name = a.getKey();
								if (!innerCount.containsKey(name)) {
									innerCount.put(name, 1);
								} else {
									innerCount.put(name,
											innerCount.get(name) + 1);
								}
							}

						}

						// only count one time for each tag
						for (String t : innerCount.keySet()) {
							if (!tokenCount.containsKey(t)) {
								tokenCount.put(t, 1);
								// System.out.println(t);
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

	public void extractFileTypeList(String targetFolder,
			String webFileListPath, String commonFileListPath) throws Exception {
		try {
			List<String> fileTypes = new ArrayList<String>();
			// extract web file lists
			BufferedReader in = new BufferedReader(new InputStreamReader(
					new FileInputStream(webFileListPath), "UTF-8"));
			String aLine = null;
			while ((aLine = in.readLine()) != null) {
				String[] tokens = aLine.split("\t");
				if (tokens.length >= 2) {
					String type = tokens[0].trim().toLowerCase()
							.replace(".", "");
					if (!fileTypes.contains(type))
						fileTypes.add(type);
				}
			}
			in.close();
			// extract common file lists
			in = new BufferedReader(new InputStreamReader(new FileInputStream(
					commonFileListPath), "UTF-8"));
			aLine = null;
			while ((aLine = in.readLine()) != null) {
				if (aLine.startsWith(".")) {
					String[] tokens = aLine.split("\t");
					if (tokens.length >= 2) {
						String type = tokens[0].trim().toLowerCase()
								.replace(".", "");
						if (!fileTypes.contains(type))
							fileTypes.add(type);
					}
				}
			}
			in.close();

			System.out.println(fileTypes);
			System.out.println("Types count: " + fileTypes.size());

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

					System.out.println("Scanning html file: " + file);

					in = new BufferedReader(new InputStreamReader(
							new FileInputStream(file), "UTF-8"));
					File f = new File(file);
					try {
						StringBuffer htmlStr = new StringBuffer();
						aLine = null;
						while ((aLine = in.readLine()) != null) {
							htmlStr.append(aLine + newLine);
						}

						List<String> tokens = processTextByLucene(
								htmlStr.toString(), true, true);

						List<String> foundTypes = new ArrayList<String>();
						for (String token : tokens) {
							if (fileTypes.contains(token)) {
								if (!foundTypes.contains(token))
									foundTypes.add(token);
							}
						}

						// only count one time for each tag
						for (String t : foundTypes) {
							if (!tokenCount.containsKey(t)) {
								tokenCount.put(t, 1);
								// System.out.println(t);
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

	public void extractDomainNameList(String targetFolder) throws Exception {
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

					System.out.println("Scanning html file: " + file);

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

						String ulrPattern = "\\b(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]";
						Pattern p = Pattern.compile(ulrPattern);
						Matcher matcher = p.matcher(htmlStr.toString());
						List<String> domainNames = new ArrayList<String>();
						while (matcher.find()) {
							String raw = matcher.group();
							String url = raw.replaceAll("&lt;", "");
							url = url.replaceAll("&gt;", "");

							if (url.contains("?"))
								url = url.substring(0, url.indexOf("?"));
							if (url.contains(";"))
								url = url.substring(0, url.indexOf(";"));
							if (url.contains("&"))
								url = url.substring(0, url.indexOf("&"));
							if (url.contains("#"))
								url = url.substring(0, url.indexOf("#"));
							if (url.contains("|"))
								url = url.substring(0, url.indexOf("|"));
							if (url.contains("@"))
								url = url.substring(0, url.indexOf("@"));
							if (url.contains("%"))
								url = url.substring(0, url.indexOf("%"));

							String domain = null;
							if (!url.contains("file:///") && url.length() > 7) {
								String[] tmpArr = url.split("/");
								// if (tmpArr.length > 3)
								// url = url
								// .substring(0, url.lastIndexOf("/"));
								if (tmpArr.length > 3)
									url = "http://" + tmpArr[2];
								try {
									if (!url.trim().equals("http://")
											&& !url.trim().equals("https://"))
										domain = getDomainName(url);
								} catch (Exception e) {
									System.out.println("Error: " + url);
									e.printStackTrace();
									throw e;
								}
							} else
								domain = url;

							if (domain != null && domain.length() < 2)
								System.err.println("Too short: " + domain + "("
										+ raw + ")");
							if (domain != null && !domainNames.contains(domain))
								domainNames.add(domain);
						}

						// only count one time for each tag
						for (String t : domainNames) {
							if (!tokenCount.containsKey(t)) {
								tokenCount.put(t, 1);
								// System.out.println(t);
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

							codeTokens = processTextByLucene(tmpStr.toString(),
									true, false);
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

	public List<String> processTextByLucene(String text, boolean english,
			boolean digits) throws IOException {
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

					boolean valid = false;

					if (english && !digits)
						valid = isAllEnglish(word);
					else if (!english && digits)
						valid = isAllDigits(word);
					else if (english && digits)
						valid = isAllEnglishAndDigits(word);

					if (valid) {
						if (word.length() > 1 && !result.contains(word))
							result.add(word);
					}

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

	public boolean isAllDigits(String text) {
		boolean result = true;

		String[] tokens = text.split("\\s");

		for (String token : tokens) {
			for (int i = 0; i < token.length(); i++) {
				char c = token.charAt(i);
				if (!Character.isDigit(c)) {
					result = false;
					break;
				}
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

	public static String getDomainName(String url) throws URISyntaxException {
		URI uri = new URI(url);
		String domain = uri.getHost();
		if (domain != null)
			return domain.startsWith("www.") ? domain.substring(4) : domain;
		else
			return url;
	}

	public static void main(String[] args) throws Exception {
		String targetFolder = "/home/markpeng/Share/Kaggle/tnative/sponsored";

		SponsoredFileChecker worker = new SponsoredFileChecker();
		// worker.extractTagList(targetFolder);
		// worker.extractScriptTokenList(targetFolder);
		// worker.extractDomainNameList(targetFolder);
		// worker.extractDomainNameList(targetFolder);
		// worker.extractFileTypeList(targetFolder,
		// "/home/markpeng/Share/Kaggle/tnative/web_files.txt",
		// "/home/markpeng/Share/Kaggle/tnative/common_file_types.txt");
		worker.extractAttributeList(targetFolder,
				"/home/markpeng/Share/Kaggle/tnative/html_attributes.txt");
	}

}
