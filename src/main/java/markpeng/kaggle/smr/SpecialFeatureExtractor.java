package markpeng.kaggle.smr;

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
import java.util.Hashtable;
import java.util.List;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
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

public class SpecialFeatureExtractor implements Runnable {

	private static final int BUFFER_LENGTH = 1000;
	private static final String newLine = System.getProperty("line.separator");

	private String htmlFolderPath = null;
	private Hashtable<String, String> trainFileList = null;
	private List<String> testFileList = null;
	private String outputTrain = null;
	private String outputTest = null;

	private String typeFilePath = null;
	private String attrFilePath = null;

	private String folderId = null;

	public SpecialFeatureExtractor(String htmlFolderPath, String typeFilePath,
			String attrFilePath, Hashtable<String, String> trainFileList,
			List<String> testFileList, String outputTrain, String outputTest,
			String folderId) {
		this.htmlFolderPath = htmlFolderPath;
		this.typeFilePath = typeFilePath;
		this.attrFilePath = attrFilePath;
		this.trainFileList = trainFileList;
		this.testFileList = testFileList;
		this.outputTrain = outputTrain;
		this.outputTest = outputTest;
		this.folderId = folderId;
	}

	public static List<String> readTagListFile(String filePath)
			throws Exception {
		List<String> result = new ArrayList<String>();
		BufferedReader in = new BufferedReader(new InputStreamReader(
				new FileInputStream(filePath), "UTF-8"));

		try {
			String aLine = null;
			while ((aLine = in.readLine()) != null) {
				String tmp = aLine.trim();
				if (tmp.length() > 0) {
					String[] tokens = tmp.split(":");
					if (tokens.length == 2) {
						String tag = tokens[0].trim().toLowerCase();
						if (!result.contains(tag))
							result.add(tag);
					} else {
						// skip
						// String tag = tmp.substring(0, tmp.lastIndexOf(":"));
						// if (!result.contains(tag))
						// result.add(tag);
					}
				}
			}
		} finally {
			in.close();
		}

		return result;
	}

	public static Hashtable<String, String> readTrainListFile(String filePath)
			throws Exception {
		Hashtable<String, String> result = new Hashtable<String, String>();
		BufferedReader in = new BufferedReader(new InputStreamReader(
				new FileInputStream(filePath), "UTF-8"));

		// skip header
		in.readLine();

		try {
			String aLine = null;
			while ((aLine = in.readLine()) != null) {
				String tmp = aLine.trim();
				if (tmp.length() > 0) {
					String[] tokens = tmp.split(",");
					result.put(tokens[0], tokens[1]);
				}
			}
		} finally {
			in.close();
		}

		return result;
	}

	public static List<String> readTestListFile(String filePath)
			throws Exception {
		List<String> result = new ArrayList<String>();
		BufferedReader in = new BufferedReader(new InputStreamReader(
				new FileInputStream(filePath), "UTF-8"));

		// skip header
		in.readLine();

		try {
			String aLine = null;
			while ((aLine = in.readLine()) != null) {
				String tmp = aLine.trim();
				if (tmp.length() > 0) {
					String[] tokens = tmp.split(",");
					result.add(tokens[0]);
				}
			}
		} finally {
			in.close();
		}

		return result;
	}

	@Override
	public void run() {
		BufferedWriter out = null;
		StringBuffer resultStr = new StringBuffer();

		try {
			// read token list
			List<String> typeList = readTagListFile(typeFilePath);
			List<String> attrList = readTagListFile(attrFilePath);

			// special features names
			String[] spFeatureNames = { "title_tokenCount",
					"content_tokenCount", "weird_title_unigram",
					"weird_content_unigram", "comment_start_count" };

			if (folderId.equals("5")) {
				out = new BufferedWriter(new OutputStreamWriter(
						new FileOutputStream(outputTest + "_" + folderId
								+ ".csv", false), "UTF-8"));

				// create headers
				resultStr.append("\"file\",");
				int tindex = 0;
				for (String t : typeList) {
					if (tindex != typeList.size() - 1)
						resultStr.append("\"" + t + "_typeCount\",");
					else
						resultStr.append("\"" + t + "_typeCount\",");

					tindex++;
				}
				for (String t : attrList) {
					if (tindex != attrList.size() - 1)
						resultStr.append("\"" + t + "_attrCount\",");
					else
						resultStr.append("\"" + t + "_attrCount\",");

					tindex++;
				}
				for (String t : spFeatureNames) {
					if (tindex != spFeatureNames.length - 1)
						resultStr.append("\"" + t + "_count\",");
					else
						resultStr.append("\"" + t + "_count\"");

					tindex++;
				}
				resultStr.append(newLine);
			} else {
				out = new BufferedWriter(new OutputStreamWriter(
						new FileOutputStream(outputTrain + "_" + folderId
								+ ".csv", false), "UTF-8"));
				// create headers
				resultStr.append("\"file\",");
				int tindex = 0;
				for (String t : typeList) {
					if (tindex != typeList.size() - 1)
						resultStr.append("\"" + t + "_typeCount\",");
					else
						resultStr.append("\"" + t + "_typeCount\",");

					tindex++;
				}
				for (String t : attrList) {
					if (tindex != attrList.size() - 1)
						resultStr.append("\"" + t + "_attrCount\",");
					else
						resultStr.append("\"" + t + "_attrCount\",");

					tindex++;
				}
				for (String t : spFeatureNames) {
					if (tindex != spFeatureNames.length - 1)
						resultStr.append("\"" + t + "_count\",");
					else
						resultStr.append("\"" + t + "_count\"");

					tindex++;
				}
				resultStr.append(newLine);
			}

			File checker = new File(htmlFolderPath + "/" + folderId);
			if (checker.exists()) {
				System.out.println("Folder " + folderId + ": "
						+ checker.listFiles().length + " files.");

				List<String> files = new ArrayList<String>();
				for (final File fileEntry : checker.listFiles()) {
					if (fileEntry.getName().contains(".txt")) {
						String tmp = fileEntry.getAbsolutePath();
						files.add(tmp);
					}
				}

				for (String file : files) {

					String fileName = file.substring(file.lastIndexOf("/") + 1);
					resultStr.append("\"" + fileName + "\",");

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

						Document doc = Jsoup.parse(htmlStr.toString());

						// get file type counts
						List<String> tokens = processTextByLucene(
								htmlStr.toString(), true, true);

						Hashtable<String, Integer> foundTypes = new Hashtable<String, Integer>();
						for (String token : tokens) {
							if (typeList.contains(token)) {
								if (!foundTypes.containsKey(token))
									foundTypes.put(token, 1);
								else
									foundTypes.put(token,
											foundTypes.get(token) + 1);

							}
						}

						// get attribute counts
						TreeMap<String, Integer> attrCounts = new TreeMap<String, Integer>();
						Elements allNodes = doc.getAllElements();
						for (Element e : allNodes) {
							Attributes attrs = e.attributes();

							for (Attribute a : attrs) {
								String name = a.getKey();
								if (attrList.contains(name)) {
									if (!attrCounts.containsKey(name)) {
										attrCounts.put(name, 1);
									} else {
										attrCounts.put(name,
												attrCounts.get(name) + 1);
									}
								}
							}

						}

						// get all raw text
						String title = doc.title();
						// System.out.println(title);
						String allText = "";
						if (doc.body() != null && doc.body().hasText()) {
							allText = doc.body().text().trim();
							// System.out.println(allText.length());
						}

						int titleCount = 0;
						int contentCount = 0;
						List<String> titleTokens = processTextByLucene(title,
								true, true);
						List<String> contentTokens = processTextByLucene(
								allText, true, true);
						titleCount = titleTokens.size();
						contentCount = contentTokens.size();

						// weird unigram
						String[] titleGrams = title.split(" ");
						String[] contentGrams = allText.split(" ");
						int weirdTitleUnigram = 0;
						int weirdContentUnigram = 0;
						int tCount = 0;
						for (String t : titleGrams) {
							if (t.length() == 1)
								tCount++;
						}
						if (tCount >= 0.9 * titleGrams.length)
							weirdTitleUnigram = 1;

						int cCount = 0;
						for (String c : contentGrams) {
							if (c.length() == 1)
								cCount++;
						}
						if (cCount >= 0.9 * contentGrams.length)
							weirdContentUnigram = 1;

						// comment count
						int comment_count = StringUtils.countMatches(
								htmlStr.toString(), "<!--");

						// record values
						for (String t : typeList) {
							if (foundTypes.containsKey(t))
								resultStr.append(foundTypes.get(t) + ",");
							else
								resultStr.append("0,");
						}
						for (String t : attrList) {
							if (attrCounts.containsKey(t))
								resultStr.append(attrCounts.get(t) + ",");
							else
								resultStr.append("0,");
						}
						resultStr.append(titleCount + ",");
						resultStr.append(contentCount + ",");
						resultStr.append(weirdTitleUnigram + ",");
						resultStr.append(weirdContentUnigram + ",");
						resultStr.append(comment_count);
						resultStr.append(newLine);

					} finally {
						in.close();
					}

					if (resultStr.length() >= BUFFER_LENGTH) {
						out.write(resultStr.toString());
						out.flush();
						resultStr.setLength(0);
					}

					System.out.println("Scanned html file in folder "
							+ folderId + ": " + f.getName());

				} // end of for loop
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				out.write(resultStr.toString());
				out.flush();
				out.close();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
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

	public static void main(String[] args) throws Exception {
		String htmlFolderPath = args[0];
		String trainFileListPath = args[1];
		String testFileListPath = args[2];
		String outputTrain = args[3];
		String outputTest = args[4];
		String typeFilePath = args[5];
		String attrFilePath = args[6];

		Hashtable<String, String> trainFileList = readTrainListFile(trainFileListPath);
		List<String> testFileList = readTestListFile(testFileListPath);

		Thread[] threads = new Thread[6];
		for (int i = 0; i < 6; i++) {
			System.out.println("Running for folder " + i + " ...");
			SpecialFeatureExtractor worker = new SpecialFeatureExtractor(
					htmlFolderPath, typeFilePath, attrFilePath, trainFileList,
					testFileList, outputTrain, outputTest, Integer.toString(i));
			threads[i] = new Thread(worker);
			threads[i].start();
			// worker.run();

			System.out.println("Running thread for folder " + i + " ...");
			Thread.sleep(2000);
		}

		for (Thread t : threads)
			t.join();
	}

}
