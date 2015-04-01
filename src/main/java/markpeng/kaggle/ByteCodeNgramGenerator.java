package markpeng.kaggle;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.TreeSet;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;

public class ByteCodeNgramGenerator {

	private static final int BUFFER_LENGTH = 1000;
	private static final String newLine = System.getProperty("line.separator");

	public Hashtable<String, List<String>> readTrainLabel(String trainLabelFile)
			throws Exception {
		// <label, list<doc_ids>>
		Hashtable<String, List<String>> output = new Hashtable<String, List<String>>();

		BufferedReader in = new BufferedReader(new InputStreamReader(
				new FileInputStream(trainLabelFile), "UTF-8"));

		try {
			String aLine = null;
			// skip header line
			in.readLine();
			while ((aLine = in.readLine()) != null) {
				String[] sp = aLine.split(",");
				if (sp != null && sp.length > 0) {
					String fileName = sp[0].replaceAll("\"", "");
					String label = sp[1];

					// System.out.println(fileName + ", " + label);
					if (output.get(label) == null) {
						List<String> tmp = new ArrayList<String>();
						tmp.add(fileName);
						output.put(label, tmp);
					} else {
						List<String> tmp = output.get(label);
						tmp.add(fileName);
						output.put(label, tmp);
					}
				}
			}
		} finally {
			in.close();
		}

		return output;
	}

	public void generate(String trainLabelFile, String trainFolder,
			String outputTxt, int ngram) throws Exception {
		TreeSet<Long> features = new TreeSet<Long>();

		Hashtable<String, List<String>> labels = readTrainLabel(trainLabelFile);

		StringBuffer resultStr = new StringBuffer();

		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(outputTxt, false), "UTF-8"));
		try {

			for (String label : labels.keySet()) {
				String folderName = trainFolder + "/" + label;
				List<String> fileList = labels.get(label);

				for (String file : fileList) {
					File f = new File(folderName + "/" + file
							+ ".bytes_filtered");

					System.out.println("Loading " + f.getAbsolutePath());
					if (f.exists()) {

						List<String> tokens = new ArrayList<String>();
						List<String> prevLastThreetokens = new ArrayList<String>();
						String aLine = null;
						BufferedReader in = new BufferedReader(
								new InputStreamReader(new FileInputStream(
										f.getAbsolutePath()), "UTF-8"));
						while ((aLine = in.readLine()) != null) {
							String tmp = aLine.toLowerCase().trim();

							String[] sp = tmp.split("\\s");
							for (String token : sp) {
								if (!token.equals("??"))
									tokens.add(token);
							}

							// count byte ngram
							if (prevLastThreetokens.size() > 0)
								tokens.addAll(0, prevLastThreetokens);
							for (int i = 0; i < tokens.size(); i++) {
								int ngramEnd = i + (ngram - 1);

								if (i % ngram == 0 && ngramEnd < tokens.size()) {
									// String seq = tokens.get(i) + tokens.get(i
									// +
									// 1);
									String seq = "";
									for (int j = i; j <= ngramEnd; j++) {
										seq += tokens.get(j);
									}

									long code = Long.parseLong(seq, 16);
									features.add(code);
								}
							}

							// keep last N-1 tokens
							if (tokens.size() > 0) {
								prevLastThreetokens.clear();
								for (int k = ngram - 1; k >= 1; k--)
									prevLastThreetokens.add(tokens.get(tokens
											.size() - k));
							}
							tokens.clear();
						}
						in.close();
						tokens.clear();
						prevLastThreetokens.clear();

						System.out.println("Completed filtering file: " + file);
					}
				} // end of file loop

			} // end of label loop

			// check if each feature exists
			for (Long feature : features) {
				resultStr.append(feature + newLine);

				if (resultStr.length() >= BUFFER_LENGTH) {
					out.write(resultStr.toString());
					out.flush();
					resultStr.setLength(0);
				}
			} // end of feature loop

			System.out.println("Total # of features: " + features.size());

		} finally {
			out.write(resultStr.toString());
			out.flush();
			out.close();
			resultStr.setLength(0);
		}
	}

	public static void main(String[] args) throws Exception {

		if (args.length < 4) {
			System.out
					.println("Arguments: [train folder] [train label file] [output txt] [ngram]");
			return;
		}
		String trainFolder = args[0];
		String trainLabelFile = args[1];
		String outputTxt = args[2];
		int ngram = Integer.parseInt(args[3]);
		ByteCodeNgramGenerator worker = new ByteCodeNgramGenerator();
		worker.generate(trainLabelFile, trainFolder, outputTxt, ngram);

	}
}
