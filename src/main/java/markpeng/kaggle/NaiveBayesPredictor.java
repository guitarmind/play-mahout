package markpeng.kaggle;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;
import org.apache.mahout.classifier.naivebayes.BayesUtils;
import org.apache.mahout.classifier.naivebayes.ComplementaryNaiveBayesClassifier;
import org.apache.mahout.classifier.naivebayes.NaiveBayesModel;
import org.apache.mahout.common.Pair;
import org.apache.mahout.common.iterator.sequencefile.SequenceFileIterable;
import org.apache.mahout.math.RandomAccessSparseVector;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.Vector.Element;
import org.apache.mahout.vectorizer.TFIDF;

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.Multiset;

public class NaiveBayesPredictor {

	private static final int BUFFER_LENGTH = 1000;
	private static final String newLine = System.getProperty("line.separator");

	public static Map<String, Integer> readDictionnary(Configuration conf,
			Path dictionnaryPath) {
		Map<String, Integer> dictionnary = new HashMap<String, Integer>();

		System.out.println("Loading word dictionary file ......");
		for (Pair<Text, IntWritable> pair : new SequenceFileIterable<Text, IntWritable>(
				dictionnaryPath, true, conf)) {
			dictionnary.put(pair.getFirst().toString(), pair.getSecond().get());
		}
		return dictionnary;
	}

	public static Map<Integer, Long> readDocumentFrequency(Configuration conf,
			Path documentFrequencyPath) {
		Map<Integer, Long> documentFrequency = new HashMap<Integer, Long>();

		System.out.println("Loading document frequency file ......");
		for (Pair<IntWritable, LongWritable> pair : new SequenceFileIterable<IntWritable, LongWritable>(
				documentFrequencyPath, true, conf)) {
			documentFrequency
					.put(pair.getFirst().get(), pair.getSecond().get());
		}
		return documentFrequency;
	}

	public static void main(String[] args) throws Exception {

		args = new String[6];
		args[0] = "/home/markpeng/Share/Kaggle/Microsoft Malware Classification/result/mmc_train_filtered_cnb_model";
		args[1] = "/home/markpeng/Share/Kaggle/Microsoft Malware Classification/result/mmc_train_filtered_cnb_labelindex";
		args[2] = "/home/markpeng/Share/Kaggle/Microsoft Malware Classification/result/dictionary.file-0";
		args[3] = "/home/markpeng/Share/Kaggle/Microsoft Malware Classification/result/df-count";
		args[4] = "/home/markpeng/Share/Kaggle/Microsoft Malware Classification/dataSample";
		args[5] = "/home/markpeng/Share/Kaggle/Microsoft Malware Classification/dataSample/submission.csv";

		if (args.length < 6) {
			System.out
					.println("Arguments: [model] [label index] [dictionnary] [document frequency] [test folder] [output csv]");
			return;
		}
		String modelPath = args[0];
		String labelIndexPath = args[1];
		String dictionaryPath = args[2];
		String documentFrequencyPath = args[3];
		String testFolderPath = args[4];
		String csvFilePath = args[5];

		File checker = new File(testFolderPath);
		if (checker.exists()) {

			Configuration configuration = new Configuration();
			// model is a matrix (wordId, labelId) => probability score
			NaiveBayesModel model = NaiveBayesModel.materialize(new Path(
					modelPath), configuration);
			ComplementaryNaiveBayesClassifier classifier = new ComplementaryNaiveBayesClassifier(
					model);
			// labels is a map label => classId
			Map<Integer, String> labels = BayesUtils.readLabelIndex(
					configuration, new Path(labelIndexPath));
			// word => word_id
			Map<String, Integer> dictionary = readDictionnary(configuration,
					new Path(dictionaryPath));
			// word_id => DF
			Map<Integer, Long> documentFrequency = readDocumentFrequency(
					configuration, new Path(documentFrequencyPath));
			int labelCount = labels.size();
			int documentCount = documentFrequency.get(-1).intValue();
			System.out.println("Number of labels: " + labelCount);
			System.out.println("Number of documents in training set: "
					+ documentCount);

			// get all test file path
			List<String> asmFiles = new ArrayList<String>();
			for (final File fileEntry : checker.listFiles()) {
				if (fileEntry.getName().contains(".asm_filtered")) {
					String tmp = fileEntry.getAbsolutePath();
					asmFiles.add(tmp);
				}
			}

			StringBuffer outputStr = new StringBuffer();
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(csvFilePath, false), "UTF-8"));
			try {

				for (String asmFile : asmFiles) {
					File f = new File(asmFile);
					String fileName = f.getName().replace(".asm_filtered", "")
							.trim();

					// analyzer used to extract word from file
					Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_46);

					// read test file content
					BufferedReader reader = new BufferedReader(new FileReader(
							asmFile));

					// write header
					outputStr
							.append("\"Id\",\"Prediction1\",\"Prediction2\",\"Prediction3\","
									+ "\"Prediction4\",\"Prediction5\",\"Prediction6\","
									+ "\"Prediction7\",\"Prediction8\",\"Prediction9\""
									+ newLine);

					try {

						StringBuffer text = new StringBuffer();
						String line = null;
						while ((line = reader.readLine()) != null) {
							text.append(line + newLine);
						}

						Multiset<String> words = ConcurrentHashMultiset
								.create();
						// extract words from current line
						TokenStream ts = analyzer.tokenStream("text",
								new StringReader(text.toString()));
						CharTermAttribute termAtt = ts
								.addAttribute(CharTermAttribute.class);
						ts.reset();
						int wordCount = 0;
						while (ts.incrementToken()) {
							if (termAtt.length() > 0) {
								String word = termAtt.toString();
								Integer wordId = dictionary.get(word);
								// if the word is not in the dictionary, skip it
								if (wordId != null) {
									words.add(word);
									wordCount++;
								}
							}
						}
						// Fixed error : close ts:TokenStream
						ts.end();
						ts.close();

						// create vector wordId => weight using tfidf
						Vector vector = new RandomAccessSparseVector(100000);
						TFIDF tfidf = new TFIDF();
						for (Multiset.Entry<String> entry : words.entrySet()) {
							String word = entry.getElement();
							// TF
							int count = entry.getCount();
							Integer wordId = dictionary.get(word);
							// DF
							Long freq = documentFrequency.get(wordId);
							double tfIdfValue = tfidf.calculate(count,
									freq.intValue(), wordCount, documentCount);
							vector.setQuick(wordId, tfIdfValue);
						}
						// With the classifier, we get one score for each label
						// The label with the highest score is the one the file
						// is more likely to be associated to\
						TreeMap<Integer, Double> map = new TreeMap<Integer, Double>();
						Vector resultVector = classifier.classifyFull(vector);
						double bestScore = -Double.MAX_VALUE;
						int bestCategoryId = -1;
						for (Element element : resultVector.all()) {
							int categoryId = element.index();
							double score = element.get();

							map.put(categoryId, score);

							if (score > bestScore) {
								bestScore = score;
								bestCategoryId = categoryId;
							}
						}
						System.out.println(" => " + labels.get(bestCategoryId));

						// write to csv
						outputStr.append("\"" + fileName + "\",");
						for (Integer id : map.keySet()) {
							outputStr.append(labels.get(id) + ",");
						}
						outputStr.append(newLine);
						if (outputStr.length() >= BUFFER_LENGTH) {
							out.write(outputStr.toString());
							out.flush();
							outputStr.setLength(0);
						}

					} finally {
						analyzer.close();
						reader.close();
					}
				} // end of asm file loop

			} finally {
				out.write(outputStr.toString());
				out.flush();
				out.close();
			}
		}
	}
}
