package markpeng.kaggle.tae;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.util.Version;

public class EntityFeatureGenerator {

	private static final int BUFFER_LENGTH = 1000;
	private static final String newLine = System.getProperty("line.separator");

	public List<String> readFeature(String... featureFiles) throws Exception {
		List<String> features = new ArrayList<String>();

		for (String featureFile : featureFiles) {
			BufferedReader in = new BufferedReader(new InputStreamReader(
					new FileInputStream(featureFile), "UTF-8"));

			try {
				String aLine = null;
				while ((aLine = in.readLine()) != null) {
					String tmp = aLine.toLowerCase().trim();
					String[] tmpArray = tmp.split("\\s");
					String feature = tmpArray[0].trim();
					if (!features.contains(feature))
						features.add(feature);
				}
			} finally {
				in.close();
			}
		}

		return features;
	}

	public void generate(String trainFile, String testFile, String outputTrain,
			String outputTest, String... featureFiles) throws Exception {
		List<String> entities = readFeature(featureFiles);
		List<String> finalEntities = processEntityByLucene(entities);

		StringBuffer resultStr = new StringBuffer();

		BufferedReader trainIn = new BufferedReader(new InputStreamReader(
				new FileInputStream(trainFile), "UTF-8"));

		BufferedReader testIn = new BufferedReader(new InputStreamReader(
				new FileInputStream(testFile), "UTF-8"));

		BufferedWriter trainOut = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(outputTrain, false), "UTF-8"));

		BufferedWriter testOut = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(outputTest, false), "UTF-8"));

		// topic words
		List<String> topicWords = topicWordExtraction(trainFile, testFile);

		// create headers
		int i = 0;
		for (String f : finalEntities) {
			resultStr.append("Entity_" + (i + 1) + ",");
			i++;
		}
		i = 0;
		for (String f : topicWords) {
			if (i != topicWords.size() - 1)
				resultStr.append("Topic_" + (i + 1) + ",");
			else
				resultStr.append("Topic_" + (i + 1));
			i++;
		}
		resultStr.append(newLine);

		try {

			String aLine = null;
			// skip header
			trainIn.readLine();
			while ((aLine = trainIn.readLine()) != null) {
				int[] rowResult = new int[finalEntities.size()
						+ topicWords.size()];

				String tmp = aLine.toLowerCase().trim();
				String[] tokens = tmp.split(",");
				String headline = tokens[3].replace("\"", "").trim();
				String abst = tokens[5].replace("\"", "").trim();

				Hashtable<String, Integer> htokens = getTermFreqByLucene(headline);
				Hashtable<String, Integer> abtokens = getTermFreqByLucene(abst);

				String postHeader = processTextByLucene(headline);
				String postAbst = processTextByLucene(abst);

				int sIndex = 0;
				for (String s : finalEntities) {
					if (!s.contains(" ")) {
						if (htokens.containsKey(s) || abtokens.containsKey(s)) {
							rowResult[sIndex] = 1;
							System.out.println(s + " ==> entity: " + postHeader
									+ "," + postAbst);
						}
					} else {
						if (postHeader.contains(s) || postAbst.contains(s)) {
							rowResult[sIndex] = 1;
							System.out.println(s + " ==> entity: " + postHeader
									+ "," + postAbst);
						}
					}

					sIndex++;
				}

				int tIndex = finalEntities.size();
				for (String t : topicWords) {
					if (htokens.containsKey(t) || abtokens.containsKey(t)) {
						rowResult[tIndex] = 1;
						System.out.println(t + " ==> topic: " + postHeader
								+ "," + postAbst);
					}
					tIndex++;
				}

				// append output row
				int vIndex = 0;
				for (Integer v : rowResult) {
					if (vIndex < rowResult.length - 1)
						resultStr.append(v + ",");
					else
						resultStr.append(v + newLine);
					vIndex++;
				}

				if (resultStr.length() >= BUFFER_LENGTH) {
					trainOut.write(resultStr.toString());
					trainOut.flush();
					resultStr.setLength(0);
				}

			} // end of train file loop

		} finally {
			trainIn.close();

			trainOut.write(resultStr.toString());
			trainOut.flush();
			trainOut.close();
			resultStr.setLength(0);
		}

		//
		// ---------------------------------------------------------------------------------------------------------------
		// test dataset

		try {
			resultStr.setLength(0);
			// create headers
			i = 0;
			for (String f : finalEntities) {
				resultStr.append("Entity_" + (i + 1) + ",");
				i++;
			}
			i = 0;
			for (String f : topicWords) {
				if (i != topicWords.size() - 1)
					resultStr.append("Topic_" + (i + 1) + ",");
				else
					resultStr.append("Topic_" + (i + 1));
				i++;
			}
			resultStr.append(newLine);

			String aLine = null;
			// skip header
			testIn.readLine();
			while ((aLine = testIn.readLine()) != null) {
				int[] rowResult = new int[finalEntities.size()
						+ topicWords.size()];

				String tmp = aLine.toLowerCase().trim();
				String[] tokens = tmp.split(",");
				String headline = tokens[3].replace("\"", "").trim();
				String abst = tokens[5].replace("\"", "").trim();

				Hashtable<String, Integer> htokens = getTermFreqByLucene(headline);
				Hashtable<String, Integer> abtokens = getTermFreqByLucene(abst);

				String postHeader = processTextByLucene(headline);
				String postAbst = processTextByLucene(abst);

				int sIndex = 0;
				for (String s : finalEntities) {
					if (!s.contains(" ")) {
						if (htokens.containsKey(s) || abtokens.containsKey(s)) {
							rowResult[sIndex] = 1;
							System.out.println(s + " ==> entity: " + postHeader
									+ "," + postAbst);
						}
					} else {
						if (postHeader.contains(s) || postAbst.contains(s)) {
							rowResult[sIndex] = 1;
							System.out.println(s + " ==> entity: " + postHeader
									+ "," + postAbst);
						}
					}

					sIndex++;
				}

				int tIndex = finalEntities.size();
				for (String t : topicWords) {
					if (htokens.containsKey(t) || abtokens.containsKey(t)) {
						rowResult[tIndex] = 1;
						System.out.println(t + " ==> topic: " + postHeader
								+ "," + postAbst);
					}
					tIndex++;
				}

				// append output row
				int vIndex = 0;
				for (Integer v : rowResult) {
					if (vIndex < rowResult.length - 1)
						resultStr.append(v + ",");
					else
						resultStr.append(v + newLine);
					vIndex++;
				}

				if (resultStr.length() >= BUFFER_LENGTH) {
					testOut.write(resultStr.toString());
					testOut.flush();
					resultStr.setLength(0);
				}

			} // end of test file loop

		} finally {
			testIn.close();

			testOut.write(resultStr.toString());
			testOut.flush();
			testOut.close();
			resultStr.setLength(0);
		}
	}

	public List<String> topicWordExtraction(String trainFile, String testFile)
			throws Exception {
		List<String> newFeatures = new ArrayList<String>();

		BufferedReader in = new BufferedReader(new InputStreamReader(
				new FileInputStream(trainFile), "UTF-8"));

		BufferedReader testIn = new BufferedReader(new InputStreamReader(
				new FileInputStream(testFile), "UTF-8"));

		try {
			String aLine = null;
			// skip header
			in.readLine();
			Hashtable<String, Integer> table = new Hashtable<String, Integer>();
			while ((aLine = in.readLine()) != null) {
				String tmp = aLine.toLowerCase().trim();
				String[] tokens = tmp.split(",");
				String headline = tokens[3].replace("\"", "").trim();
				headline = headline.replace(".", "");
				String abst = tokens[5].replace("\"", "").trim();
				abst = abst.replace(".", "");

				// List<String> htokens = Arrays.asList(headline.split("\\s"));
				Hashtable<String, Integer> htokens = getTermFreqByLucene(headline);
				Hashtable<String, Integer> abtokens = getTermFreqByLucene(abst);

				for (String h : htokens.keySet()) {
					if (h.length() >= 2 && abtokens.containsKey(h)) {
						if (!table.containsKey(h))
							table.put(h, 1);
						else
							table.put(h, table.get(h) + 1);
					}
				}

			}

			int sentiCount = 0;
			for (String t : table.keySet()) {
				int freq = table.get(t);
				System.out.println(t + " ==> " + freq);
				sentiCount++;
			}

			System.out.println("Total detected train topic features: "
					+ sentiCount);

			// make sure that test csv contains them
			aLine = null;
			// skip header
			in.readLine();
			while ((aLine = testIn.readLine()) != null) {
				String tmp = aLine.toLowerCase().trim();
				String[] tokens = tmp.split(",");
				String headline = tokens[3].replace("\"", "").trim();
				headline = headline.replace(".", "");
				String abst = tokens[5].replace("\"", "").trim();
				abst = abst.replace(".", "");

				// List<String> htokens = Arrays.asList(headline.split("\\s"));
				// List<String> abtokens = Arrays.asList(abst.split("\\s"));
				Hashtable<String, Integer> htokens = getTermFreqByLucene(headline);
				Hashtable<String, Integer> abtokens = getTermFreqByLucene(abst);

				for (String t : table.keySet()) {
					// if (htokens.contains(t)) {
					if (htokens.containsKey(t) || abtokens.containsKey(t)) {
						if (!newFeatures.contains(t))
							newFeatures.add(t);
					}
				}

			}

			for (String t : newFeatures) {
				System.out.println(t);
			}
			System.out.println("Final feature count: " + newFeatures.size());
		} finally {
			in.close();
			testIn.close();
		}

		return newFeatures;
	}

	private Hashtable<String, Integer> getTermFreqByLucene(String text)
			throws IOException {
		Hashtable<String, Integer> result = new Hashtable<String, Integer>();

		Set stopWords = new StandardAnalyzer(Version.LUCENE_46)
				.getStopwordSet();
		TokenStream ts = new StandardTokenizer(Version.LUCENE_46,
				new StringReader(text));
		ts = new StopFilter(Version.LUCENE_46, ts, (CharArraySet) stopWords);
		ts = new PorterStemFilter(ts);
		try {
			CharTermAttribute termAtt = ts
					.addAttribute(CharTermAttribute.class);
			ts.reset();
			int wordCount = 0;
			while (ts.incrementToken()) {
				if (termAtt.length() > 0) {
					String word = termAtt.toString();

					if (result.get(word) == null)
						result.put(word, 1);
					else {
						result.put(word, result.get(word) + 1);
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

	public List<String> processEntityByLucene(List<String> entities)
			throws IOException {
		List<String> result = new ArrayList<String>();

		for (String text : entities) {
			StringBuffer postText = new StringBuffer();
			Set stopWords = new StandardAnalyzer(Version.LUCENE_46)
					.getStopwordSet();
			TokenStream ts = new StandardTokenizer(Version.LUCENE_46,
					new StringReader(text));
			ts = new StopFilter(Version.LUCENE_46, ts, (CharArraySet) stopWords);
			ts = new PorterStemFilter(ts);
			try {
				CharTermAttribute termAtt = ts
						.addAttribute(CharTermAttribute.class);
				ts.reset();
				int wordCount = 0;
				while (ts.incrementToken()) {
					if (termAtt.length() > 0) {
						String word = termAtt.toString();
						postText.append(word + " ");

						wordCount++;
					}
				}

				String finalText = postText.toString().trim().replace("'", "");
				if (finalText.length() > 1 && !result.contains(finalText)) {
					result.add(finalText);
					System.out.println(finalText);
				}
			} finally {
				// Fixed error : close ts:TokenStream
				ts.end();
				ts.close();
			}
		} // end of entity loop

		return result;
	}

	public String processTextByLucene(String text) throws IOException {
		String result = text;

		StringBuffer postText = new StringBuffer();
		Set stopWords = new StandardAnalyzer(Version.LUCENE_46)
				.getStopwordSet();
		TokenStream ts = new StandardTokenizer(Version.LUCENE_46,
				new StringReader(text));
		ts = new StopFilter(Version.LUCENE_46, ts, (CharArraySet) stopWords);
		ts = new PorterStemFilter(ts);
		try {
			CharTermAttribute termAtt = ts
					.addAttribute(CharTermAttribute.class);
			ts.reset();
			int wordCount = 0;
			while (ts.incrementToken()) {
				if (termAtt.length() > 0) {
					String word = termAtt.toString();
					postText.append(word + " ");

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

	public static void main(String[] args) throws Exception {
		args = new String[5];
		args[0] = "/home/markpeng/Share/Kaggle/The Analytics Edge Competition/NYTimesBlogTrain.csv";
		args[1] = "/home/markpeng/Share/Kaggle/The Analytics Edge Competition/NYTimesBlogTest.csv";
		args[2] = "/home/markpeng/Share/Kaggle/The Analytics Edge Competition/entity/entities_headline_20150503.txt";
		args[3] = "/home/markpeng/Share/Kaggle/The Analytics Edge Competition/entityTrain.csv";
		args[4] = "/home/markpeng/Share/Kaggle/The Analytics Edge Competition/entityTest.csv";

		if (args.length < 5) {
			System.out
					.println("Arguments: [train.csv] [test.csv] [featureFiles] [output train] [output test]");
			return;
		}
		String trainFile = args[0];
		String testFile = args[1];
		String[] featureFiles = args[2].split("\\|");
		String outputTrain = args[3];
		String outputTest = args[4];
		EntityFeatureGenerator worker = new EntityFeatureGenerator();
		worker.generate(trainFile, testFile, outputTrain, outputTest,
				featureFiles);
		// List<String> entities = worker.readFeature(featureFiles);
		// worker.topicWordExtraction(trainFile, testFile);
		// worker.processEntityByLucene(entities);

		// System.out.println("1914: russians dominate in east poland"
		// .matches("[0-9]{4,4}:.*"));
	}
}
