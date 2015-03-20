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

public class FunctionFeatureGenerator {

	private static final int BUFFER_LENGTH = 1000;
	private static final String newLine = System.getProperty("line.separator");

	public void generate(String trainFolder, String outputTxt, String fileType)
			throws Exception {
		List<String> features = new ArrayList<String>();

		StringBuffer resultStr = new StringBuffer();

		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(outputTxt, false), "UTF-8"));
		try {

			String folderName = trainFolder;
			List<String> fileList = new ArrayList<String>();
			for (final File fileEntry : (new File(trainFolder)).listFiles()) {
				if (fileEntry.getName().contains("." + fileType)) {
					String tmp = fileEntry.getName().substring(0,
							fileEntry.getName().lastIndexOf("."));
					fileList.add(tmp);
				}
			}

			for (String file : fileList) {
				File f = new File(folderName + "/" + file + "." + fileType);

				System.out.println("Loading " + f.getAbsolutePath());
				if (f.exists()) {
					String aLine = null;
					BufferedReader in = new BufferedReader(
							new InputStreamReader(new FileInputStream(
									f.getAbsolutePath()), "UTF-8"));
					while ((aLine = in.readLine()) != null) {
						String tmp = aLine.toLowerCase().trim();

						// extract function as feature
						String function = extractFunction(tmp);
						if (function != null && !features.contains(function)) {
							features.add(function);
							System.out
									.println("Detected function: " + function);
						}
					}
					in.close();

					System.out.println("Completed filtering file: " + file);
				}
			} // end of file loop

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

	private String extractFunction(String text) {
		String tmp = null;

		if (text.contains(";") && text.contains("(") && !text.contains(".text")) {
			int commentIndex = text.indexOf(";");
			int quoteIndex = text.indexOf("(");

			if (commentIndex < quoteIndex) {
				String[] arr = text.substring(0, quoteIndex).split("\\s");
				if (arr != null) {
					String candidate = arr[arr.length - 1];
					if (candidate.length() >= 3
							&& candidate.matches("[a-zA-Z]+[0-9]?"))
						tmp = candidate;
				}
			}
		}

		return tmp;
	}

	public static void main(String[] args) throws Exception {

		if (args.length < 3) {
			System.out
					.println("Arguments: [train folder] [output txt] [file type] ");
			return;
		}
		String trainFolder = args[0];
		String outputTxt = args[1];
		String fileType = args[2];
		FunctionFeatureGenerator worker = new FunctionFeatureGenerator();
		worker.generate(trainFolder, outputTxt, fileType);

	}
}
