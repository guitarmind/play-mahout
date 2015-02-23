package markpeng.kaggle;

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;

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

	public void generateTrainFoldersByLabel(String trainLabelFile,
			String trainFileFoler, String outputFolder) throws Exception {
		Hashtable<String, List<String>> output = readTrainLabel(trainLabelFile);

		for (String label : output.keySet()) {
			String folderName = outputFolder + "/" + label;
			File f = new File(folderName);
			f.mkdir();

			List<String> tmp = output.get(label);
			for (String asm : tmp) {
				copyFileUsingJava7Files(new File(trainFileFoler + "/" + asm
						+ ".asm"), new File(folderName + "/" + asm + ".asm"));

				System.out.println(asm + " completed.");
			}

			System.out.println(label + ": " + tmp.size() + " files completed.");
		}
	}

	public void generateSampleTrainFolder(String trainLabelFile,
			String trainFileFoler, String outputFolder, int randomCount)
			throws Exception {
		Hashtable<String, List<String>> output = readTrainLabel(trainLabelFile);

		for (String label : output.keySet()) {
			String folderName = outputFolder + "/" + label;
			File f = new File(folderName);
			f.mkdir();

			List<String> tmp = output.get(label);

			// select random files
			List<String> rands = new ArrayList<String>();
			if (randomCount >= tmp.size()) {
				rands.addAll(tmp);
			} else {
				while (rands.size() < randomCount) {
					int rand = randInt(0, tmp.size() - 1);
					String sample = tmp.get(rand);
					if (!rands.contains(sample))
						rands.add(sample);
				}
			}

			for (String asm : rands) {
				copyFileUsingJava7Files(new File(trainFileFoler + "/" + asm
						+ ".asm"), new File(folderName + "/" + asm + ".asm"));

				System.out.println(asm + " completed.");
			}

			System.out.println(label + ": " + rands.size()
					+ " sample files completed.");
		}
	}

	private static void copyFileUsingJava7Files(File source, File dest)
			throws IOException {
		Files.copy(source.toPath(), dest.toPath(), REPLACE_EXISTING);
	}

	/**
	 * Returns a pseudo-random number between min and max, inclusive. The
	 * difference between min and max can be at most
	 * <code>Integer.MAX_VALUE - 1</code>.
	 *
	 * @param min
	 *            Minimum value
	 * @param max
	 *            Maximum value. Must be greater than min.
	 * @return Integer between min and max, inclusive.
	 * @see java.util.Random#nextInt(int)
	 */
	public static int randInt(int min, int max) {

		// NOTE: Usually this should be a field rather than a method
		// variable so that it is not re-seeded every call.
		Random rand = new Random();

		// nextInt is normally exclusive of the top value,
		// so add 1 to make it inclusive
		int randomNum = rand.nextInt((max - min) + 1) + min;

		return randomNum;
	}

	public static void main(String[] args) throws Exception {

		// ---------------------------------------------------------------------------------------------
		// Get sample dateset randomly from splitted train dataset

		MMCTrainingSampler test = new MMCTrainingSampler();
		test.generateSampleTrainFolder(args[0], args[1], args[2],
				Integer.parseInt(args[3]));

		// ---------------------------------------------------------------------------------------------
		// Split original train dataset

		// MMCTrainingSampler test = new MMCTrainingSampler();
		// test.generateTrainFoldersByLabel(args[0], args[1], args[2]);

		// ---------------------------------------------------------------------------------------------
		// Test Case

		// String trainLabelFile =
		// "/home/markpeng/Share/Kaggle/Microsoft Malware Classification/trainLabels.csv";
		// MMCTrainingSampler test = new MMCTrainingSampler();
		// Hashtable<String, List<String>> output = test
		// .readTrainLabel(trainLabelFile);
		// int count = 0;
		// for (String label : output.keySet()) {
		// List<String> tmp = output.get(label);
		// System.out.println(label + ": " + tmp.size() + " files");
		// count += tmp.size();
		// }
		// System.out.println("Total training files: " + count + " files");

	}

}
