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
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.shingle.ShingleFilter;
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

	private static final int MAX_NGRAM = 2;
	private static final int MIN_DF = 2;
	private static final double MAX_DF_PERCENT = 0.85;

	public static Map<String, Integer> readDictionary(Configuration conf,
			String dictionaryPath) {
		Map<String, Integer> dictionnary = new HashMap<String, Integer>();

		System.out.println("Loading word dictionary file ......");

		File checker = new File(dictionaryPath);
		if (checker.isDirectory()) {
			for (final File fileEntry : checker.listFiles()) {
				if (fileEntry.getName().startsWith("dictionary.file-")) {
					String filePath = fileEntry.getAbsolutePath();
					for (Pair<Text, IntWritable> pair : new SequenceFileIterable<Text, IntWritable>(
							new Path(filePath), true, conf)) {
						dictionnary.put(pair.getFirst().toString(), pair
								.getSecond().get());
					}
				}
			}
		} else {
			for (Pair<Text, IntWritable> pair : new SequenceFileIterable<Text, IntWritable>(
					new Path(dictionaryPath), true, conf)) {
				dictionnary.put(pair.getFirst().toString(), pair.getSecond()
						.get());
			}
		}

		return dictionnary;
	}

	public static Map<Integer, Long> readDocumentFrequency(Configuration conf,
			String documentFrequencyPath) {
		Map<Integer, Long> documentFrequency = new HashMap<Integer, Long>();

		System.out.println("Loading document frequency file ......");

		File checker = new File(documentFrequencyPath);
		if (checker.isDirectory()) {
			for (final File fileEntry : checker.listFiles()) {
				if (fileEntry.getName().startsWith("part-r-")) {
					String filePath = fileEntry.getAbsolutePath();
					for (Pair<IntWritable, LongWritable> pair : new SequenceFileIterable<IntWritable, LongWritable>(
							new Path(filePath), true, conf)) {
						documentFrequency.put(pair.getFirst().get(), pair
								.getSecond().get());
					}
				}
			}
		} else {
			for (Pair<IntWritable, LongWritable> pair : new SequenceFileIterable<IntWritable, LongWritable>(
					new Path(documentFrequencyPath), true, conf)) {
				documentFrequency.put(pair.getFirst().get(), pair.getSecond()
						.get());
			}
		}
		return documentFrequency;
	}

	public static void main(String[] args) throws Exception {

		// args = new String[6];
		// args[0] = "/home/markpeng/test/cnb_model";
		// args[1] = "/home/markpeng/test/cnb_labelindex";
		// args[2] = "/home/markpeng/test/train_10samples_filtered-vector";
		// args[3] =
		// "/home/markpeng/test/train_10samples_filtered-vector/df-count";
		// args[4] =
		// "/home/markpeng/Share/Kaggle/Microsoft Malware Classification/dataSample";
		// args[5] =
		// "/home/markpeng/Share/Kaggle/Microsoft Malware Classification/dataSample/submission.csv";
		// args[0] =
		// "/home/markpeng/Share/Kaggle/Microsoft Malware Classification/result/mmc_train_filtered_cnb_model";
		// args[1] =
		// "/home/markpeng/Share/Kaggle/Microsoft Malware Classification/result/mmc_train_filtered_cnb_labelindex";
		// args[2] =
		// "/home/markpeng/Share/Kaggle/Microsoft Malware Classification/result/dictionary.file-0";
		// args[3] =
		// "/home/markpeng/Share/Kaggle/Microsoft Malware Classification/result/df-count";
		// args[4] =
		// "/home/markpeng/Share/Kaggle/Microsoft Malware Classification/dataSample";
		// args[5] =
		// "/home/markpeng/Share/Kaggle/Microsoft Malware Classification/dataSample/submission.csv";

		if (args.length < 7) {
			System.out
					.println("Arguments: [model] [fileType] [label index] [dictionnary] [document frequency] [test folder] [output csv]");
			return;
		}
		String modelPath = args[0];
		String fileType = args[1];
		String labelIndexPath = args[2];
		String dictionaryPath = args[3];
		String documentFrequencyPath = args[4];
		String testFolderPath = args[5];
		String csvFilePath = args[6];

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
			Map<String, Integer> dictionary = readDictionary(configuration,
					dictionaryPath);
			// word_id => DF
			Map<Integer, Long> documentFrequency = readDocumentFrequency(
					configuration, documentFrequencyPath);
			int labelCount = labels.size();
			int documentCount = documentFrequency.get(-1).intValue();
			System.out.println("Number of labels: " + labelCount);
			System.out.println("Number of documents in training set: "
					+ documentCount);

			// get all test file path
			List<String> asmFiles = new ArrayList<String>();
			for (final File fileEntry : checker.listFiles()) {
				if (fileEntry.getName().contains("." + fileType + "_filtered")) {
					String tmp = fileEntry.getAbsolutePath();
					asmFiles.add(tmp);
				}
			}

			StringBuffer outputStr = new StringBuffer();
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(csvFilePath, false), "UTF-8"));
			try {

				// write header
				outputStr
						.append("\"Id\",\"Prediction1\",\"Prediction2\",\"Prediction3\","
								+ "\"Prediction4\",\"Prediction5\",\"Prediction6\","
								+ "\"Prediction7\",\"Prediction8\",\"Prediction9\""
								+ newLine);

				for (String asmFile : asmFiles) {
					File f = new File(asmFile);
					String fileName = f.getName()
							.replace("." + fileType + "_filtered", "").trim();

					// read test file content
					BufferedReader reader = new BufferedReader(new FileReader(
							asmFile));

					try {

						StringBuffer text = new StringBuffer();
						String line = null;
						while ((line = reader.readLine()) != null) {
							text.append(line + newLine);
						}

						Multiset<String> words = ConcurrentHashMultiset
								.create();
						// extract words from current line
						TokenStream ts = new WhitespaceTokenizer(
								Version.LUCENE_46, new StringReader(
										text.toString()));
						// TokenStream ts = new StandardTokenizer(
						// Version.LUCENE_46, new StringReader(
						// text.toString()));
						// get n-gram filter (N=2)
						ts = new ShingleFilter(ts, MAX_NGRAM, MAX_NGRAM);
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

									// System.out.println(word);
								}
							}
						}
						// Fixed error : close ts:TokenStream
						ts.end();
						ts.close();

						// create vector wordId => weight using tfidf
						Vector vector = new RandomAccessSparseVector(100000);
						TFIDF tfidf = new TFIDF();
						int l2norm = 0;
						for (Multiset.Entry<String> entry : words.entrySet()) {
							String word = entry.getElement();
							// TF
							int count = entry.getCount();
							Integer wordId = dictionary.get(word);
							// DF
							Long freq = documentFrequency.get(wordId);
							if (freq < MIN_DF)
								continue;
							if (((double) freq / documentCount) >= MAX_DF_PERCENT)
								continue;

							// it ignores wordCount (length)
							// TF => Math.sqrt(freq)
							// IDF => Math.log(numDocs/(double)(docFreq+1))+1.0
							double tfIdfValue = tfidf.calculate(count,
									freq.intValue(), wordCount, documentCount);
							l2norm += Math.pow(tfIdfValue, 2);
						}
						for (Multiset.Entry<String> entry : words.entrySet()) {
							String word = entry.getElement();
							// TF
							int count = entry.getCount();
							Integer wordId = dictionary.get(word);
							// DF
							Long freq = documentFrequency.get(wordId);
							if (freq < MIN_DF)
								continue;
							if (((double) freq / documentCount) >= MAX_DF_PERCENT)
								continue;

							// it ignores wordCount (length)
							// TF => Math.sqrt(freq)
							// IDF => Math.log(numDocs/(double)(docFreq+1))+1.0
							double tfIdfValue = tfidf.calculate(count,
									freq.intValue(), wordCount, documentCount);
							// enforce L2 Euclidean norm
							tfIdfValue = (double) tfIdfValue
									/ Math.sqrt(l2norm);
							vector.setQuick(wordId, tfIdfValue);
						}

						// With the classifier, we get one score for each label
						// The label with the highest score is the one the file
						// is more likely to be associated to\
						TreeMap<Integer, Double> map = new TreeMap<Integer, Double>();
						Vector resultVector = classifier.classifyFull(vector);
						double bestScore = -Double.MAX_VALUE;
						double minScore = Double.MAX_VALUE;
						double sumScore = -1;
						int bestCategoryId = -1;

						for (Element element : resultVector.all()) {
							int categoryId = element.index();
							double score = element.get();

							// use log normalization
							map.put(categoryId, score);

							if (score > bestScore) {
								bestScore = score;
								bestCategoryId = categoryId;
							}

							if (score < minScore)
								minScore = score;

							sumScore += score;
						}
						System.out.println(fileName + " => "
								+ labels.get(bestCategoryId));

						// compute Euclidean length of the vector
						// double unitLength = 0;
						// for (Integer id : map.keySet()) {
						// double score = map.get(id);
						//
						// // normalize by mean score
						// unitLength += Math.pow(score, 2);
						// }
						// unitLength = Math.sqrt(unitLength);

						// compuate mean and sd
						// double mean = 0;
						// double sd = 0;
						// for (Integer id : map.keySet()) {
						// double score = map.get(id);
						// mean += score;
						// }
						// mean = mean / 9;
						// for (Integer id : map.keySet()) {
						// double score = map.get(id);
						// sd += Math.pow(score - mean, 2);
						// }
						// sd = sd / 9;
						// sd = Math.sqrt(sd);

						// write to csv
						outputStr.append("\"" + fileName + "\",");
						int count = 0;
						for (Integer id : map.keySet()) {
							double score = map.get(id);

							// normalize by standard score
							// score = (double) (score - mean) / sd;

							// normalize by unit vector length
							// score = (double) score / unitLength;

							// max-min normalization
							score = (double) (score - minScore)
									/ (bestScore - minScore);

							// use equal probability if not valid
							if (Double.isInfinite(score) || Double.isNaN(score)
									|| score == 0)
								score = (double) 1 / 9;

							if (count < map.size() - 1)
								outputStr.append(score + ",");
							else
								outputStr.append(score);
							count++;
						}
						outputStr.append(newLine);
						if (outputStr.length() >= BUFFER_LENGTH) {
							out.write(outputStr.toString());
							out.flush();
							outputStr.setLength(0);
						}

					} finally {
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
