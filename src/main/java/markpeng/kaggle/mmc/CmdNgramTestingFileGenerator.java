package markpeng.kaggle.mmc;

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
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;

public class CmdNgramTestingFileGenerator {

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
					if (aLine.contains(",")) {
						String tmp = aLine.split(",")[0];
						tmp = tmp.replace("_", " ").trim();
						if (tmp.length() > 0)
							features.add(tmp);
					} else {
						String tmp = aLine.replace("_", " ").trim();
						if (tmp.length() > 0)
							features.add(tmp);
					}
				}
			} finally {
				in.close();
			}
		}

		return features;
	}

	public void generateCsv(String testFolder, String outputFile,
			String fileType, boolean filtered, int ngram,
			String... featureFiles) throws Exception {
		List<String> features = readFeature(featureFiles);

		System.out.println("Total # of cmd line ngram features: "
				+ features.size());

		StringBuffer resultStr = new StringBuffer();

		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(outputFile, false), "UTF-8"));
		try {

			File checker = new File(testFolder);
			if (checker.exists()) {

				// get all test file path
				List<String> testFiles = new ArrayList<String>();
				for (final File fileEntry : checker.listFiles()) {
					if (fileEntry.getName().contains("." + fileType)) {
						String tmp = fileEntry.getAbsolutePath();
						testFiles.add(tmp);
					}
				}

				// add header line
				resultStr.append("fileName,");
				int index = 0;
				for (String feature : features) {
					if (index < features.size() - 1)
						resultStr.append(feature.replace(" ", "_") + ",");
					else
						resultStr.append(feature.replace(" ", "_") + newLine);
					index++;
				}

				for (String file : testFiles) {
					File f = new File(file);
					String fileName = f.getName().trim();
					fileName = fileName.substring(0, fileName.lastIndexOf("."));

					System.out.println("Loading " + f.getAbsolutePath());
					if (f.exists()) {
						// add fileName
						resultStr.append(fileName + ",");

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
								lineCmds.add(cmd);
							}
						}
						in.close();

						// create ngrams
						Hashtable<String, Integer> ngrams = getNgramFreqByLucene(
								lineCmds, ngram);
						if (ngrams != null) {
							int cIndex = 0;
							for (String c : features) {
								int freq = 0;
								if (ngrams.containsKey(c))
									freq = ngrams.get(c);

								if (cIndex < features.size() - 1)
									resultStr.append(freq + ",");
								else
									resultStr.append(freq + newLine);
								// System.out.println(c + ": " + freq);

								cIndex++;
							}
						}

						if (resultStr.length() >= BUFFER_LENGTH) {
							out.write(resultStr.toString());
							out.flush();
							resultStr.setLength(0);
						}

						System.out.println("Completed filtering file: "
								+ fileName);
					}
				} // end of test file loop

				System.out.println("Total # of features: " + features.size());
			}
		} finally {
			out.write(resultStr.toString());
			out.flush();
			out.close();
			resultStr.setLength(0);
		}

	}

	private Hashtable<String, Integer> getNgramFreqByLucene(
			List<String> lineCmds, int ngram) throws IOException {
		Hashtable<String, Integer> result = new Hashtable<String, Integer>();

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
						// System.out.println(word);
						if (result.get(word) == null)
							result.put(word, 1);
						else {
							result.put(word, result.get(word) + 1);
						}
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

		// args = new String[7];
		// args[0] = "libsvm";
		// args[1] =
		// "/home/markpeng/Share/Kaggle/Microsoft Malware Classification/dataSample";
		// args[2] =
		// "/home/markpeng/Share/Kaggle/Microsoft Malware Classification/ireullin/80386_all.txt|/home/markpeng/Share/Kaggle/Microsoft Malware Classification/idcFunctions.txt";
		// args[3] =
		// "/home/markpeng/Share/Kaggle/Microsoft Malware Classification/test_libsvm.libsvm";
		// args[4] =
		// "/home/markpeng/Share/Kaggle/Microsoft Malware Classification/test_libsvm.index";
		// args[5] = "asm";
		// args[6] = "false";

		if (args.length < 6) {
			System.out
					.println("Arguments: [test folder] [feature file] [output file] [file type] [filtered] [ngram]");
			return;
		}
		String testFolder = args[0];
		String[] featureFiles = args[1].split("\\|");
		String outputFile = args[2];
		String fileType = args[3];
		boolean filterred = Boolean.parseBoolean(args[4]);
		int ngram = Integer.parseInt(args[5]);
		CmdNgramTestingFileGenerator worker = new CmdNgramTestingFileGenerator();
		worker.generateCsv(testFolder, outputFile, fileType, filterred, ngram,
				featureFiles);

	}
}
