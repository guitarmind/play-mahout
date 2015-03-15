package markpeng.kaggle;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

public class LibSVMPredictionParser {

	private static final int BUFFER_LENGTH = 1000;
	private static final String newLine = System.getProperty("line.separator");

	public static void main(String[] args) throws Exception {

		// args = new String[3];
		// args[0] = "test_full_asm_cmdfunc_20150315.libsvm.scale.predict";
		// args[1] = "test_full_asm_cmdfunc_20150315.index";
		// args[2] = "test_full_asm_cmdfunc_20150315.libsvm.csv";

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
					String fileName = fileNames.get(i);
					String output = predictions.get(i);
					String[] tokens = output.split("\\s");
					if (tokens.length == 10) {
						outputStr.append(fileName + ",");

						// skip first token (prediction label)
						for (int j = 9; j > 0; j--) {
							double prob = Double.parseDouble(tokens[j]);
							if (i > 1)
								outputStr.append(prob + ",");
							else
								outputStr.append(prob);
						}

						outputStr.append(newLine);
						if (outputStr.length() >= BUFFER_LENGTH) {
							out.write(outputStr.toString());
							out.flush();
							outputStr.setLength(0);
						}
					}
				}
			} else
				System.out.println("WRONG!! predictions.size(): "
						+ predictions.size() + ", fileNames.size(): "
						+ fileNames.size());

		} finally {
			out.write(outputStr.toString());
			out.flush();
			out.close();
		}
	}
}
