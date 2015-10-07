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
import java.util.Set;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.miscellaneous.WordDelimiterFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.util.Version;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class TagWithDfFeatureExtractor implements Runnable {

	private static final int BUFFER_LENGTH = 1000;
	private static final String newLine = System.getProperty("line.separator");

	private String htmlFolderPath = null;
	private Hashtable<String, String> trainFileList = null;
	private List<String> testFileList = null;
	private String outputTrain = null;
	private String outputTest = null;

	private String tagFilePath = null;

	private String folderId = null;

	private final String[] TAGS = { "meta", "title", "body", "p", "a", "img",
			"base", "link", "script", "style", "div", "ul", "li", "span", "i",
			"nav", "button", "form", "iframe", "h1", "h2", "h3", "h4", "h5",
			"br", "input", "b", "em", "table", "tr", "td", "hr", "svg",
			"symbol", "path", "g", "use", "label", "section", "noscript",
			"article", "footer" };

	public TagWithDfFeatureExtractor(String htmlFolderPath, String tagFilePath,
			Hashtable<String, String> trainFileList, List<String> testFileList,
			String outputTrain, String outputTest, String folderId) {
		this.htmlFolderPath = htmlFolderPath;
		this.tagFilePath = tagFilePath;
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
					result.add(tokens[0].trim());
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
			// read tag list
			List<String> tagList = readTagListFile(tagFilePath);
			for (String t : TAGS) {
				if (!tagList.contains(t))
					tagList.add(t);
			}

			if (folderId.equals("5")) {
				out = new BufferedWriter(new OutputStreamWriter(
						new FileOutputStream(outputTest + "_" + folderId
								+ ".csv", false), "UTF-8"));

				// create headers
				resultStr.append("\"file\",");
				int tindex = 0;
				for (String t : tagList) {
					if (tindex != tagList.size() - 1)
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
				for (String t : tagList) {
					if (tindex != tagList.size() - 1)
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
						// get all raw text
						// String title = doc.title();
						// System.out.println(title);
						// String allText = doc.body().text().trim();
						// System.out.println(allText.length());
						//
						// String processedTitle =
						// processTextByLucene(title);
						// String processedText =
						// processTextByLucene(allText);

						// get tag counts
						int tindex = 0;
						for (String t : tagList) {
							int count = doc.select(t).size();
							if (tindex != tagList.size() - 1)
								resultStr.append(count + ",");
							else
								resultStr.append(count);

							tindex++;
						}
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

	public String processTextByLucene(String text) throws IOException {
		String result = text;

		StringBuffer postText = new StringBuffer();
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
		ts = new PorterStemFilter(ts);
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

					postText.append(word + " ");
					// System.out.println(word);

					// wordCount++;
				}
			}

			String finalText = postText.toString().trim().replace("'", "")
					.replace("\"", "");
			if (finalText.length() > 1)
				result = finalText;

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
		String tagFilePath = args[5];

		Hashtable<String, String> trainFileList = readTrainListFile(trainFileListPath);
		List<String> testFileList = readTestListFile(testFileListPath);

		Thread[] threads = new Thread[6];
		for (int i = 0; i < 6; i++) {
			System.out.println("Running for folder " + i + " ...");
			TagWithDfFeatureExtractor worker = new TagWithDfFeatureExtractor(
					htmlFolderPath, tagFilePath, trainFileList, testFileList,
					outputTrain, outputTest, Integer.toString(i));
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
