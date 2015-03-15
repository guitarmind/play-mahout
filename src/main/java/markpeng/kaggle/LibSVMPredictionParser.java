package markpeng.kaggle;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStreamReader;
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

public class LibSVMPredictionParser {

	private static final int BUFFER_LENGTH = 1000;
	private static final String newLine = System.getProperty("line.separator");

	public static void main(String[] args) throws Exception {

		// args = new String[6];
		// args[0] = "/home/markpeng/test/cnb_model";
		// args[1] = "/home/markpeng/test/cnb_labelindex";
		// args[2] = "/home/markpeng/test/train_10samples_filtered-vector";

		if (args.length < 3) {
			System.out
					.println("Arguments: [predict file] [index file] [submission file]");
			return;
		}
		String predictFile = args[0];
		String indexFile = args[1];
		String submitFile = args[2];

		StringBuffer outputStr = new StringBuffer();
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(submitFile, false), "UTF-8"));
		try {
			List<String> fileNames = new ArrayList<String>();
			List<String> predictions = new ArrayList<String>();

			// write header
			outputStr
					.append("\"Id\",\"Prediction1\",\"Prediction2\",\"Prediction3\","
							+ "\"Prediction4\",\"Prediction5\",\"Prediction6\","
							+ "\"Prediction7\",\"Prediction8\",\"Prediction9\""
							+ newLine);

			// read index file
			BufferedReader in = new BufferedReader(new InputStreamReader(
					new FileInputStream(indexFile), "UTF-8"));
			try {
				String aLine = null;
				while ((aLine = in.readLine()) != null) {
					String tmp = aLine.toLowerCase().trim();
					if (tmp.length() > 0) {
						fileNames.add(tmp);
					}
				}
			} finally {
				in.close();
			}
			// read prediction file
			in = new BufferedReader(new InputStreamReader(new FileInputStream(
					predictFile), "UTF-8"));
			try {
				String aLine = null;
				// skip first line
				aLine = in.readLine();
				while ((aLine = in.readLine()) != null) {
					String tmp = aLine.toLowerCase().trim();
					if (tmp.length() > 0) {
						predictions.add(tmp);
					}
				}
			} finally {
				in.close();
			}

			if (predictions.size() == fileNames.size()) {
				for (int i = 0; i < predictions.size(); i++) {

					outputStr.append(newLine);
					if (outputStr.length() >= BUFFER_LENGTH) {
						out.write(outputStr.toString());
						out.flush();
						outputStr.setLength(0);
					}
				}
			}

		} finally {
			out.write(outputStr.toString());
			out.flush();
			out.close();
		}
	}
}
