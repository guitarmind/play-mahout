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
import java.util.Hashtable;
import java.util.List;

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
			String outputTxt, String fileType, int ngram) throws Exception {
		List<String> features = new ArrayList<String>();
		Hashtable<String, List<String>> labels = readTrainLabel(trainLabelFile);

		StringBuffer resultStr = new StringBuffer();

		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(outputTxt, false), "UTF-8"));
		try {

			for (String label : labels.keySet()) {
				String folderName = trainFolder + "/" + label;
				List<String> fileList = labels.get(label);

				for (String file : fileList) {
					File f = new File(folderName + "/" + file + "." + fileType);

					System.out.println("Loading " + f.getAbsolutePath());
					if (f.exists()) {

						StringBuffer fileContent = new StringBuffer();
						String aLine = null;
						BufferedReader in = new BufferedReader(
								new InputStreamReader(new FileInputStream(
										f.getAbsolutePath()), "UTF-8"));
						while ((aLine = in.readLine()) != null) {
							String tmp = aLine.toLowerCase().trim();

							String[] sp = tmp.split("\\s");
							List<String> tokens = Arrays.asList(sp);
							for (String token : tokens) {
								if (token.length() == 2 && !token.equals("??")) {
									fileContent.append(token + " ");
								}
							}

							fileContent.append(newLine);
						}
						in.close();

						// extract ngram
						List<String> result = extractNgram(
								fileContent.toString(), ngram);
						for (String token : result) {
							if (!features.contains(token)) {
								features.add(token);
								System.out.println("Detected ngram: " + token);
							}
						}

						System.out.println("Completed filtering file: " + file);
					}
				} // end of file loop

			} // end of label loop

			// check if each feature exists
			for (String feature : features) {
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

	private List<String> extractNgram(String text, int ngram) throws Exception {
		List<String> result = new ArrayList<String>();

		TokenStream ts = new WhitespaceTokenizer(Version.LUCENE_46,
				new StringReader(text));
		ts = new ShingleFilter(ts, ngram, ngram);
		try {
			CharTermAttribute termAtt = ts
					.addAttribute(CharTermAttribute.class);
			ts.reset();
			while (ts.incrementToken()) {
				if (termAtt.length() > 0) {
					String word = termAtt.toString();
					if (!result.contains(word))
						result.add(word);
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

		if (args.length < 5) {
			System.out
					.println("Arguments: [train folder] [train label file] [output txt] [file type] [ngram]");
			return;
		}
		String trainFolder = args[0];
		String trainLabelFile = args[1];
		String outputTxt = args[2];
		String fileType = args[3];
		int ngram = Integer.parseInt(args[4]);
		ByteCodeNgramGenerator worker = new ByteCodeNgramGenerator();
		worker.generate(trainLabelFile, trainFolder, outputTxt, fileType, ngram);

	}
}
