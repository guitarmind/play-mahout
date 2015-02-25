package markpeng.mahout.test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.hadoop.fs.Path;
import org.apache.mahout.classifier.naivebayes.ComplementaryNaiveBayesClassifier;
import org.apache.mahout.classifier.naivebayes.NaiveBayesModel;
import org.apache.mahout.common.AbstractJob;
import org.apache.mahout.vectorizer.TFIDF;

public class BayesPredictTest extends AbstractJob {
	public static HashMap<String, String> dictionaryHashMap = new HashMap<>();
	public static HashMap<String, String> dfcountHashMap = new HashMap<>();
	public static HashMap<String, String> wordcountHashMap = new HashMap<>();
	public static HashMap<String, String> labelindexHashMap = new HashMap<>();

	public BayesPredictTest() {
		readDfCount("model/df-count.txt");
		readDictionary("model/dictionary.txt");
		readLabelIndex("model/labelindex.txt");
		readWordCount("model/wordcount.txt");
	}

	public static String[] readFile(String filename) {

		File file = new File(filename);
		BufferedReader reader;
		String tempstring = null;
		try {
			reader = new BufferedReader(new FileReader(file));
			tempstring = reader.readLine();
			reader.close();
			if (tempstring == null)
				return null;
		} catch (IOException e) {
			e.printStackTrace();
		}

		String[] mess = tempstring.trim().split(" ");
		return mess;
	}

	public static void readDictionary(String fileName) {
		File file = new File(fileName);
		BufferedReader reader;
		String tempstring = null;
		try {

			reader = new BufferedReader(new FileReader(file));
			while ((tempstring = reader.readLine()) != null) {

				if (tempstring.startsWith("Key:")) {
					String key = tempstring.substring(
							tempstring.indexOf(":") + 1,
							tempstring.indexOf("Value") - 2);
					String value = tempstring.substring(tempstring
							.lastIndexOf(":") + 1);
					dictionaryHashMap.put(key.trim(), value.trim());
				}
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void readDfCount(String fileName) {
		File file = new File(fileName);
		BufferedReader reader;
		String tempstring = null;
		try {

			reader = new BufferedReader(new FileReader(file));
			while ((tempstring = reader.readLine()) != null) {

				if (tempstring.startsWith("Key:")) {
					String key = tempstring.substring(
							tempstring.indexOf(":") + 1,
							tempstring.indexOf("Value") - 2);
					String value = tempstring.substring(tempstring
							.lastIndexOf(":") + 1);
					dfcountHashMap.put(key.trim(), value.trim());
				}

			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void readWordCount(String fileName) {
		File file = new File(fileName);
		BufferedReader reader;
		String tempstring = null;
		try {

			reader = new BufferedReader(new FileReader(file));
			while ((tempstring = reader.readLine()) != null) {

				if (tempstring.startsWith("Key:")) {
					String key = tempstring.substring(
							tempstring.indexOf(":") + 1,
							tempstring.indexOf("Value") - 2);
					String value = tempstring.substring(tempstring
							.lastIndexOf(":") + 1);
					wordcountHashMap.put(key.trim(), value.trim());
				}
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void readLabelIndex(String fileName) {
		File file = new File(fileName);
		BufferedReader reader;
		String tempstring = null;
		try {

			reader = new BufferedReader(new FileReader(file));
			while ((tempstring = reader.readLine()) != null) {

				if (tempstring.startsWith("Key:")) {
					String key = tempstring.substring(
							tempstring.indexOf(":") + 1,
							tempstring.indexOf("Value") - 2);
					String value = tempstring.substring(tempstring
							.lastIndexOf(":") + 1);
					labelindexHashMap.put(key.trim(), value.trim());
				}
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static HashMap<Integer, Double> calcTfIdf(String filename) {
		String[] words = readFile(filename);
		if (words == null)
			return null;
		HashMap<Integer, Double> tfidfHashMap = new HashMap<Integer, Double>();
		HashMap<String, Integer> wordHashMap = new HashMap<String, Integer>();
		for (int k = 0; k < words.length; k++) {
			if (wordHashMap.get(words[k]) == null) {
				wordHashMap.put(words[k], 1);
			} else {
				wordHashMap.put(words[k], wordHashMap.get(words[k]) + 1);
			}
		}

		// System.out.println("wordcount:"+wordHashMap.size());

		/*
		 * System.out.println("dfcount:"+dfcountHashMap.size());
		 * System.out.println("dictionary:"+dictionaryHashMap.size());
		 * System.out.println("labelindex:"+labelindexHashMap.size());
		 * System.out.println("wordcount:"+wordcountHashMap.size());
		 */

		Iterator iterator = wordHashMap.entrySet().iterator();
		int numDocs = Integer.parseInt(dfcountHashMap.get("-1"));

		while (iterator.hasNext()) {
			Map.Entry<String, Integer> entry = (Map.Entry<String, Integer>) iterator
					.next();
			String key = entry.getKey();
			int value = entry.getValue();
			int tf = value;
			// System.out.println(key+":"+value);
			if (dictionaryHashMap.get(key) != null) {
				String idString = dictionaryHashMap.get(key);
				int df = Integer.parseInt(dfcountHashMap.get(idString));
				TFIDF tfidf = new TFIDF();
				double tfidf_value = tfidf.calculate(tf, df, 0, numDocs);

				tfidfHashMap.put(Integer.parseInt(idString), tfidf_value);
				// System.out.println(idString+":"+tfidf_value);
			}

		}
		return tfidfHashMap;
	}

	public String predict(String filename) throws IOException {

		HashMap<Integer, Double> tfidfHashMap = calcTfIdf(filename);
		if (tfidfHashMap == null)
			return "file is empty,unknow classify";
		// FileSystem fs = FileSystem.get(getConf());
		NaiveBayesModel model = NaiveBayesModel.materialize(new Path(
				"model/model/"), getConf());
		ComplementaryNaiveBayesClassifier classifier;
		classifier = new ComplementaryNaiveBayesClassifier(model);

		double label_1 = 0;
		double label_2 = 0;

		Iterator iterator = tfidfHashMap.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<Integer, Double> entry = (Map.Entry<Integer, Double>) iterator
					.next();
			int key = entry.getKey();
			double value = entry.getValue();
			label_1 += value * classifier.getScoreForLabelFeature(0, key);
			label_2 += value * classifier.getScoreForLabelFeature(1, key);
		}
		// System.out.println("label_1:"+label_1);
		// System.out.println("label_2:"+label_2);
		if (label_1 > label_2)
			return "fraud-female";
		else
			return "norm-female";
	}

	@Override
	public int run(String[] arg0) throws Exception {
		// TODO Auto-generated method stub
		return 0;
	}

	public static void main(String[] args) {

		// dictionary test
		/*
		 * readDictionary("model/dictionary.txt"); Iterator iterator =
		 * dictionaryHashMap.entrySet().iterator(); while(iterator.hasNext()) {
		 * Map.Entry<String, String> entry = (Map.Entry<String,
		 * String>)iterator.next();
		 * System.out.println(entry.getKey()+"--"+entry.getValue()); }
		 * System.out.println(dictionaryHashMap.size());
		 * System.out.println(System.getProperty("user.dir"));
		 */
		long startTime = System.currentTimeMillis();
		BayesPredictTest bPredict = new BayesPredictTest();
		try {
			File file = new File("model/test/");
			String[] filenames = file.list();
			int count1 = 0;
			int count2 = 0;
			int count = 0;
			for (int i = 0; i < filenames.length; i++) {
				String result = bPredict.predict("model/test/" + filenames[i]);
				count++;
				if (result.equals("fraud-female"))
					count1++;
				else if (result.equals("norm-female"))
					count2++;
				System.out.println(filenames[i] + ":" + result);

			}
			System.out.println("count:" + count);
			System.out.println("count1:" + count1);
			System.out.println("count2:" + count2);
			System.out.println("time:"
					+ (System.currentTimeMillis() - startTime) / 1000.0);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
