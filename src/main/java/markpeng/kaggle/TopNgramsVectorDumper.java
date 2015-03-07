package markpeng.kaggle;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.mahout.common.Pair;
import org.apache.mahout.common.iterator.sequencefile.SequenceFileIterable;
import org.apache.mahout.math.NamedVector;
import org.apache.mahout.math.SequentialAccessSparseVector;
import org.apache.mahout.math.VectorWritable;
import org.apache.mahout.math.Vector.Element;

public class TopNgramsVectorDumper {
	private static final String newLine = System.getProperty("line.separator");

	public static HashMap<Integer, String> readInverseDictionary(
			Configuration conf, String dictionaryPath) {
		HashMap<Integer, String> dictionnary = new HashMap<Integer, String>();

		System.out.println("Loading word dictionary file ......");

		File checker = new File(dictionaryPath);
		if (checker.isDirectory()) {
			for (final File fileEntry : checker.listFiles()) {
				if (fileEntry.getName().startsWith("dictionary.file-")) {
					String filePath = fileEntry.getAbsolutePath();
					for (Pair<Text, IntWritable> pair : new SequenceFileIterable<Text, IntWritable>(
							new Path(filePath), true, conf)) {
						dictionnary.put(pair.getSecond().get(), pair.getFirst()
								.toString());
					}
				}
			}
		} else {
			for (Pair<Text, IntWritable> pair : new SequenceFileIterable<Text, IntWritable>(
					new Path(dictionaryPath), true, conf)) {
				dictionnary.put(pair.getSecond().get(), pair.getFirst()
						.toString());
			}
		}

		return dictionnary;
	}

	public static Map<Integer, Long> readTFIDF(Configuration conf,
			String tfidfPath) {
		Map<Integer, Long> tfidfMap = new HashMap<Integer, Long>();

		System.out.println("Loading TF/IDF file ......");

		File checker = new File(tfidfPath);
		if (checker.isDirectory()) {
			for (final File fileEntry : checker.listFiles()) {
				if (fileEntry.getName().startsWith("part-r-")) {
					String filePath = fileEntry.getAbsolutePath();
					try {
						FileSystem fs = FileSystem.get(conf);
						SequenceFile.Reader read = new SequenceFile.Reader(fs,
								new Path(filePath), conf);
						Text key = new Text();
						VectorWritable value = new VectorWritable();
						SequentialAccessSparseVector vect;
						while (read.next(key, value)) {
							NamedVector namedVector = (NamedVector) value.get();
							vect = (SequentialAccessSparseVector) namedVector
									.getDelegate();
							String name = namedVector.getName();
							Iterator<Element> looper = vect.iterator();
							while (looper.hasNext()) {
								Element e = looper.next();
								int word_id = e.index();
								double tfidf = e.get();
							}

						}

						read.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		} else {
			for (Pair<IntWritable, LongWritable> pair : new SequenceFileIterable<IntWritable, LongWritable>(
					new Path(tfidfPath), true, conf)) {
				tfidfMap.put(pair.getFirst().get(), pair.getSecond().get());
			}
		}
		return tfidfMap;
	}

	public static Map<Integer, Long> getTopWords(Map<Integer, Long> tfidfMap,
			int topWordsCount) {
		List<Map.Entry<Integer, Long>> entries = new ArrayList<Map.Entry<Integer, Long>>(
				tfidfMap.entrySet());
		Collections.sort(entries, new Comparator<Map.Entry<Integer, Long>>() {
			@Override
			public int compare(Entry<Integer, Long> e1, Entry<Integer, Long> e2) {
				return -e1.getValue().compareTo(e2.getValue());
			}
		});
		Map<Integer, Long> topWords = new HashMap<Integer, Long>();
		int i = 0;
		for (Map.Entry<Integer, Long> entry : entries) {
			topWords.put(entry.getKey(), entry.getValue());
			i++;
			if (i > topWordsCount) {
				break;
			}
		}
		return topWords;
	}

	public static class WordWeight implements Comparable<WordWeight> {
		private int wordId;
		private double weight;

		public WordWeight(int wordId, double weight) {
			this.wordId = wordId;
			this.weight = weight;
		}

		public int getWordId() {
			return wordId;
		}

		public Double getWeight() {
			return weight;
		}

		@Override
		public int compareTo(WordWeight w) {
			return -getWeight().compareTo(w.getWeight());
		}
	}

	public static void main(String[] args) throws Exception {
		// args = new String[2];
		// args[0] = "/home/markpeng/test/train_10samples_filtered-vector";
		// args[1] =
		// "/home/markpeng/test/train_10samples_filtered-vector/tfidf-vectors";

		if (args.length < 2) {
			System.out.println("Arguments:[dictionaryPath] [tfidfPath]");
			return;
		}
		String dictionaryPath = args[0];
		String tfidfPath = args[1];

		Configuration configuration = new Configuration();
		Map<Integer, String> inverseDictionary = readInverseDictionary(
				configuration, dictionaryPath);
		Map<Integer, Long> tfidfMap = readTFIDF(configuration, tfidfPath);
		Map<Integer, Long> topWords = getTopWords(tfidfMap, 10);
		System.out.println("Top words");
		for (Map.Entry<Integer, Long> entry : topWords.entrySet()) {
			System.out.println(" - " + inverseDictionary.get(entry.getKey())
					+ ": " + entry.getValue());
		}
		int documentCount = tfidfMap.get(-1).intValue();
		System.out.println("Number of documents in training set: "
				+ documentCount);
	}
}
