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
import java.util.Hashtable;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;

public class TestingFileGenerator {

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

	public void generateCsv(String testFolder, String outputFile,
			String outputIndexFile, String fileType, boolean filtered,
			String... featureFiles) throws Exception {
		List<String> features = readFeature(featureFiles);

		StringBuffer resultStr = new StringBuffer();
		StringBuffer indexStr = new StringBuffer();

		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(outputFile, false), "UTF-8"));

		BufferedWriter index = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(outputIndexFile, false), "UTF-8"));
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
				int labelIndex = 0;
				for (String feature : features) {
					if (labelIndex < features.size() - 1)
						resultStr.append(feature + ",");
					else
						resultStr.append(feature + newLine);
					labelIndex++;
				}

				for (String file : testFiles) {
					File f = new File(file);
					String fileName = f.getName().trim();
					fileName = fileName.substring(0, fileName.lastIndexOf("."));

					System.out.println("Loading " + f.getAbsolutePath());
					if (f.exists()) {

						StringBuffer fileContent = new StringBuffer();
						String aLine = null;
						BufferedReader in = new BufferedReader(
								new InputStreamReader(new FileInputStream(
										f.getAbsolutePath()), "UTF-8"));
						while ((aLine = in.readLine()) != null) {
							String tmp = aLine.toLowerCase().trim();

							String[] sp = tmp.split("\\t{2,}\\s{2,}");
							List<String> tokens = Arrays.asList(sp);
							int tokenIndex = 0;
							for (String token : tokens) {
								if (tokenIndex > 0 && token.length() > 1) {
									fileContent.append(token + " ");
								}
								tokenIndex++;
							}

							fileContent.append(newLine);
						}
						in.close();
						String content = fileContent.toString();
						fileContent.setLength(0);

						// get term frequency
						Hashtable<String, Integer> tfMap = getTermFreqByLucene(content);

						// check if each feature exists
						int featureIndex = 0;
						for (String feature : features) {
							// int termFreq = countTermFreqByRegEx(feature,
							// content);
							int termFreq = 0;
							if (tfMap.containsKey(feature))
								termFreq = tfMap.get(feature);

							if (featureIndex < features.size() - 1)
								resultStr.append(termFreq + ",");
							else
								resultStr.append(termFreq);

							featureIndex++;
						} // end of feature loop

						resultStr.append(newLine);

						// record file name in order
						indexStr.append(fileName + newLine);

						if (resultStr.length() >= BUFFER_LENGTH) {
							out.write(resultStr.toString());
							out.flush();
							resultStr.setLength(0);
						}
						if (indexStr.length() >= BUFFER_LENGTH) {
							index.write(indexStr.toString());
							index.flush();
							indexStr.setLength(0);
						}

						System.out.println("Completed file: " + file);
					}
				} // end of label file loop

				System.out.println("Total # of features: " + features.size());
			}
		} finally {
			out.write(resultStr.toString());
			out.flush();
			out.close();
			resultStr.setLength(0);

			index.write(indexStr.toString());
			index.flush();
			index.close();
			indexStr.setLength(0);
		}

	}

	public void generateLibsvm(String testFolder, String outputFile,
			String outputIndexFile, String fileType, boolean filtered,
			String... featureFiles) throws Exception {
		List<String> features = readFeature(featureFiles);

		StringBuffer resultStr = new StringBuffer();
		StringBuffer indexStr = new StringBuffer();

		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(outputFile, false), "UTF-8"));

		BufferedWriter index = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(outputIndexFile, false), "UTF-8"));
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

				for (String file : testFiles) {
					File f = new File(file);
					String fileName = f.getName().trim();
					fileName = fileName.substring(0, fileName.lastIndexOf("."));

					System.out.println("Loading " + f.getAbsolutePath());
					if (f.exists()) {

						StringBuffer fileContent = new StringBuffer();
						String aLine = null;
						BufferedReader in = new BufferedReader(
								new InputStreamReader(new FileInputStream(
										f.getAbsolutePath()), "UTF-8"));
						while ((aLine = in.readLine()) != null) {
							String tmp = aLine.toLowerCase().trim();

							String[] sp = tmp.split("\\t{2,}\\s{2,}");
							List<String> tokens = Arrays.asList(sp);
							int tokenIndex = 0;
							for (String token : tokens) {
								if (tokenIndex > 0 && token.length() > 1) {
									fileContent.append(token + " ");
								}
								tokenIndex++;
							}

							fileContent.append(newLine);
						}
						in.close();
						String content = fileContent.toString();
						fileContent.setLength(0);

						// get term frequency
						Hashtable<String, Integer> tfMap = getTermFreqByLucene(content);

						// check if each feature exists
						int featureIndex = 1;
						for (String feature : features) {
							// int termFreq = countTermFreqByRegEx(feature,
							// content);
							int termFreq = 0;
							if (tfMap.containsKey(feature)) {
								termFreq = tfMap.get(feature);
								resultStr.append(featureIndex + ":" + termFreq
										+ " ");
							}

							featureIndex++;
						} // end of feature loop

						resultStr.append(newLine);

						// record file name in order
						indexStr.append(fileName + newLine);

						if (resultStr.length() >= BUFFER_LENGTH) {
							out.write(resultStr.toString());
							out.flush();
							resultStr.setLength(0);
						}
						if (indexStr.length() >= BUFFER_LENGTH) {
							index.write(indexStr.toString());
							index.flush();
							indexStr.setLength(0);
						}

						System.out.println("Completed file: " + file);
					}
				} // end of label file loop

				System.out.println("Total # of features: " + features.size());
			}
		} finally {
			out.write(resultStr.toString());
			out.flush();
			out.close();
			resultStr.setLength(0);

			index.write(indexStr.toString());
			index.flush();
			index.close();
			indexStr.setLength(0);
		}

	}

	private Hashtable<String, Integer> getTermFreqByLucene(String text)
			throws IOException {
		Hashtable<String, Integer> result = new Hashtable<String, Integer>();

		TokenStream ts = new StandardTokenizer(Version.LUCENE_46,
				new StringReader(text));
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

		if (args.length < 7) {
			System.out
					.println("Arguments: [model{csv|libsvm}] [test folder] [feature files] [output file] [output index] [file type] [filtered]");
			return;
		}
		String mode = args[0];
		String testFolder = args[1];
		String[] featureFiles = args[2].split("\\|");
		String outputFile = args[3];
		String outputIndex = args[4];
		String fileType = args[5];
		boolean filterred = Boolean.parseBoolean(args[6]);
		TestingFileGenerator worker = new TestingFileGenerator();
		if (mode.equals("csv"))
			worker.generateCsv(testFolder, outputFile, outputIndex, fileType,
					filterred, featureFiles);
		else if (mode.equals("libsvm"))
			worker.generateLibsvm(testFolder, outputFile, outputIndex,
					fileType, filterred, featureFiles);

	}
}
