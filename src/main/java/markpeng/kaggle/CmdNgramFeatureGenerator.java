package markpeng.kaggle;

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
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;

public class CmdNgramFeatureGenerator {

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
					if (tmp.length() > 0 && !features.contains(tmp))
						features.add(tmp);
				}
			} finally {
				in.close();
			}
		}

		// extra features
		if (!features.contains("db"))
			features.add("db");
		if (!features.contains("dd"))
			features.add("dd");

		return features;
	}

	public void generate(String trainFolder, String outputTxt, String fileType,
			int minDF, int ngram, String... featureFiles) throws Exception {
		List<String> features = readFeature(featureFiles);
		TreeMap<String, Integer> output = new TreeMap<String, Integer>();

		StringBuffer resultStr = new StringBuffer();

		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(outputTxt, false), "UTF-8"));
		try {

			List<String> fileList = new ArrayList<String>();
			for (final File fileEntry : (new File(trainFolder)).listFiles()) {
				if (fileEntry.getName().contains("." + fileType)) {
					String tmp = fileEntry.getName().substring(0,
							fileEntry.getName().lastIndexOf("."));
					fileList.add(tmp);
				}
			}

			for (String file : fileList) {
				File f = new File(trainFolder + "/" + file + "." + fileType);

				System.out.println("Loading " + f.getAbsolutePath());
				if (f.exists()) {
					List<String> lineCmds = new ArrayList<String>();
					String aLine = null;
					BufferedReader in = new BufferedReader(
							new InputStreamReader(new FileInputStream(
									f.getAbsolutePath()), "UTF-8"));
					while ((aLine = in.readLine()) != null) {
						String tmp = aLine.toLowerCase().trim();

						String[] sp = tmp.split("\\t{2,}\\s{2,}");
						if (sp.length > 1) {
							String cmd = sp[1].trim();
							if (features.contains(cmd)) {
								lineCmds.add(cmd);
							}
						}
					}
					in.close();

					// create ngrams
					HashSet<String> ngrams = getNgramFreqByLucene(lineCmds,
							ngram);

					// count DF
					for (String n : ngrams) {
						if (output.containsKey(n))
							output.put(n, output.get(n) + 1);
						else
							output.put(n, 1);
					}

					System.out.println("Completed filtering file: " + file);
				}
			} // end of file loop

			// check if each feature exists
			SortedSet<Map.Entry<String, Integer>> sortedFeatures = entriesSortedByValues(output);
			int validN = 0;
			for (Map.Entry<String, Integer> m : sortedFeatures) {
				String feature = m.getKey();
				int df = m.getValue();
				if (df >= minDF) {
					resultStr.append(feature.replace(" ", "_") + "," + df
							+ newLine);

					if (resultStr.length() >= BUFFER_LENGTH) {
						out.write(resultStr.toString());
						out.flush();
						resultStr.setLength(0);
					}

					validN++;
				}
			} // end of feature loop

			System.out.println("Total # of features (DF >= " + minDF + "): "
					+ validN);

		} finally {
			out.write(resultStr.toString());
			out.flush();
			out.close();
			resultStr.setLength(0);
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

	private HashSet<String> getNgramFreqByLucene(List<String> lineCmds,
			int ngram) throws IOException {
		HashSet<String> result = new HashSet<String>();

		StringBuffer text = new StringBuffer();
		for (String l : lineCmds)
			text.append(l + " ");

		TokenStream ts = new StandardTokenizer(Version.LUCENE_46,
				new StringReader(text.toString()));
		ts = new ShingleFilter(ts, ngram, ngram);
		try {
			CharTermAttribute termAtt = ts
					.addAttribute(CharTermAttribute.class);
			ts.reset();
			while (ts.incrementToken()) {
				if (termAtt.length() > 0) {
					String word = termAtt.toString();
					if (word.split("\\s").length == ngram) {
						System.out.println(word);
						result.add(word);
					}

				}
			}

		} finally {
			// Fixed error : close ts:TokenStream
			ts.end();
			ts.close();
		}

		return result;
	}

	public static void main(String[] args) throws Exception {
		// List<String> lineCmds = new ArrayList<String>();
		// lineCmds.add("push");
		// lineCmds.add("push");
		// lineCmds.add("pop");
		// lineCmds.add("push");
		// CmdNgramFeatureGenerator worker = new CmdNgramFeatureGenerator();
		// worker.getNgramFreqByLucene(lineCmds, 2);

		if (args.length < 6) {
			System.out
					.println("Arguments: [train folder] [featureFiles] [output txt] [file type] [minDF] [ngram] ");
			return;
		}
		String trainFolder = args[0];
		String[] featureFiles = args[1].split("\\|");
		String outputTxt = args[2];
		String fileType = args[3];
		int minDF = Integer.parseInt(args[4]);
		int ngram = Integer.parseInt(args[5]);
		CmdNgramFeatureGenerator worker = new CmdNgramFeatureGenerator();
		worker.generate(trainFolder, outputTxt, fileType, minDF, ngram,
				featureFiles);

	}
}
