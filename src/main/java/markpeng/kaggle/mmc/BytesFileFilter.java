package markpeng.kaggle.mmc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BytesFileFilter {

	private static final int BUFFER_LENGTH = 1000;
	private static final String newLine = System.getProperty("line.separator");

	public void filterByPattern(String bytesFileFolder, String outputFolder,
			boolean subfolder) throws Exception {
		int fileCount = 0;

		File f = new File(outputFolder);
		if (!f.exists())
			f.mkdir();

		int loopN = 9;
		if (!subfolder)
			loopN = 1;

		// 9 labels
		for (int i = 1; i <= loopN; i++) {

			File inputChecker = null;
			if (!subfolder)
				inputChecker = new File(bytesFileFolder);
			else
				inputChecker = new File(bytesFileFolder + "/" + i);

			if (inputChecker.exists()) {
				if (subfolder) {
					File outputChecker = new File(outputFolder + "/" + i);
					if (!outputChecker.exists())
						outputChecker.mkdir();
				}

				List<String> bytesFiles = new ArrayList<String>();
				for (final File fileEntry : inputChecker.listFiles()) {
					if (fileEntry.getName().contains(".bytes")) {
						String tmp = fileEntry.getAbsolutePath();
						bytesFiles.add(tmp);
					}
				}

				for (String bytesFile : bytesFiles) {
					String filterFileName = (new File(bytesFile)).getName()
							+ "_filtered";

					StringBuffer resultStr = new StringBuffer();

					BufferedWriter out = null;

					if (!subfolder)
						out = new BufferedWriter(new OutputStreamWriter(
								new FileOutputStream(outputFolder + "/"
										+ filterFileName, false), "UTF-8"));
					else
						out = new BufferedWriter(
								new OutputStreamWriter(new FileOutputStream(
										outputFolder + "/" + i + "/"
												+ filterFileName, false),
										"UTF-8"));

					BufferedReader in = new BufferedReader(
							new InputStreamReader(
									new FileInputStream(bytesFile), "UTF-8"));

					try {
						String aLine = null;
						while ((aLine = in.readLine()) != null) {
							String tmp = aLine.toLowerCase().trim();

							String[] sp = tmp.split("\\s");
							List<String> tokens = Arrays.asList(Arrays
									.copyOfRange(sp, 1, sp.length));

							int ccCount = 0;
							int qmarkCount = 0;
							int zeroCount = 0;

							for (String token : tokens) {
								if (token.equals("cc"))
									ccCount++;
								else if (token.equals("??"))
									qmarkCount++;
								else if (token.equals("00"))
									zeroCount++;
							}

							if ((ccCount + qmarkCount + zeroCount) > (tokens
									.size() / 2))
								continue;

							// System.out.println("Size: " + tokens.size());
							StringBuffer fStr = new StringBuffer();
							int index = 0;
							for (String token : tokens) {
								fStr.append(token + " ");
								index++;
							}

							String newBytesLine = fStr.toString().trim();
							if (newBytesLine.length() > 0)
								resultStr.append(newBytesLine + " " + newLine);

							if (resultStr.length() >= BUFFER_LENGTH) {
								out.write(resultStr.toString());
								out.flush();
								resultStr.setLength(0);
							}
						}
					} finally {
						in.close();

						out.write(resultStr.toString());
						out.flush();
						out.close();
					}

					System.out.println("Completed filtering file: "
							+ filterFileName);
					fileCount++;
				} // end of for loop

			}

		} // end of label loop

		System.out.println(fileCount + " files filtered.");
	}

	public static void main(String[] args) throws Exception {
		String usageString = "Usage: BytesFileFilter <mode> <bytesFolder> <outputFolder>\n\n";

		usageString += "Where <mode> = {1=subfolder pattern|2=original folder pattern} \n";
		usageString += "<bytes> = the path of bytes files.\n";
		usageString += "<outputFolder> = output folder for filtered files.\n";

		// args = new String[3];
		// args[0] = "2";
		// args[1] =
		// "/home/markpeng/Share/Kaggle/Microsoft Malware Classification/dataSample";
		// args[2] =
		// "/home/markpeng/Share/Kaggle/Microsoft Malware Classification/dataSample";

		if (args.length == 3) {
			String mode = args[0];
			String bytesFolder = args[1];
			String outputFolder = args[2];
			BytesFileFilter worker = new BytesFileFilter();
			if (mode.equals("1"))
				worker.filterByPattern(bytesFolder, outputFolder, true);
			else
				worker.filterByPattern(bytesFolder, outputFolder, false);

		} else {
			System.out.println(usageString);
		}
	}

}
