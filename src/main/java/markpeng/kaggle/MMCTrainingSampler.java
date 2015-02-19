package markpeng.kaggle;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class MMCTrainingSampler {

	private static final String newLine = System.getProperty("line.separator");

	public Hashtable<String, List<String>> readTrainLabel(String trainLabelFile)
			throws Exception {
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

	public void generateSampleTrainFolder(String trainFileFoler, int randomCount)
			throws Exception {
		
	}

	private static void copyFileUsingJava7Files(File source, File dest)
			throws IOException {
		Files.copy(source.toPath(), dest.toPath());
	}

	public static void main(String[] args) throws Exception {

		// ---------------------------------------------------------------------------------------------
		// Test Case

		String trainLabelFile = "/home/markpeng/Share/Kaggle/Microsoft Malware Classification/trainLabels.csv";
		MMCTrainingSampler test = new MMCTrainingSampler();
		Hashtable<String, List<String>> output = test
				.readTrainLabel(trainLabelFile);
		int count = 0;
		for (String label : output.keySet()) {
			List<String> tmp = output.get(label);
			System.out.println(label + ": " + tmp.size() + " files");
			count += tmp.size();
		}
		System.out.println("Total training files: " + count + " files");

	}

}
