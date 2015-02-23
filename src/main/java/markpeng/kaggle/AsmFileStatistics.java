package markpeng.kaggle;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class AsmFileStatistics {

	private static final int BUFFER_LENGTH = 1000;
	private static final String newLine = System.getProperty("line.separator");

	public void showFileFormat(String asmLabeledFileFolder, String outputFolder)
			throws Exception {

		TreeMap<String, TreeMap<String, Integer>> stats = new TreeMap<String, TreeMap<String, Integer>>();

		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(outputFolder + "/asmFileFormats.txt",
						false), "UTF-8"));
		StringBuffer resultStr = new StringBuffer();

		try {

			// 9 labels
			for (int i = 1; i <= 9; i++) {
				TreeMap<String, Integer> map = new TreeMap<String, Integer>();

				File checker = new File(asmLabeledFileFolder + "/" + i);
				if (checker.exists()) {
					List<String> asmFiles = new ArrayList<String>();
					for (final File fileEntry : checker.listFiles()) {
						if (fileEntry.getName().contains(".asm")) {
							String tmp = fileEntry.getAbsolutePath();
							asmFiles.add(tmp);
						}
					}

					for (String asmFile : asmFiles) {

						BufferedReader in = new BufferedReader(
								new InputStreamReader(new FileInputStream(
										asmFile), "UTF-8"));
						File f = new File(asmFile);
						boolean found = false;

						try {

							String aLine = null;
							while ((aLine = in.readLine()) != null) {
								String tmp = aLine.trim();
								if (tmp.contains("; Format")
										|| tmp.contains(";	Format")
										|| tmp.contains("Processor	   :")) {

									tmp = tmp.substring(
											tmp.lastIndexOf(":") + 1).trim();

									// System.out.println(f.getName() + " ==> "
									// + tmp);
									resultStr.append(i + "," + f.getName()
											+ "," + tmp + newLine);

									if (map.get(tmp) == null)
										map.put(tmp, 1);
									else {
										map.put(tmp, map.get(tmp) + 1);
									}

									found = true;
									break;
								}
							}
						} finally {
							in.close();
						}

						if (resultStr.length() >= BUFFER_LENGTH) {
							out.write(resultStr.toString());
							out.flush();
							resultStr.setLength(0);
						}

						System.out.println("Scanned asm file in label " + i
								+ ": " + f.getName());

						if (!found) {
							System.out.println(f.getName()
									+ " no format found! ");
							resultStr.append(i + "," + f.getName() + ",NULL"
									+ newLine);
						}
					} // end of for loop

				}

				stats.put(Integer.toString(i), map);
			} // end of label loop

		} finally {
			out.write(resultStr.toString());
			out.flush();
			out.close();
		}

		if (stats.size() > 0) {
			int totalCount = 0;
			for (String label : stats.keySet()) {
				System.out.println("[Label " + label + "]");
				TreeMap<String, Integer> map = stats.get(label);
				for (String format : map.keySet()) {
					int count = map.get(format);
					System.out.println(format + ": " + count);

					totalCount += count;
				}
			}

			System.out.println("Total processed files: " + totalCount);
		}

	}

	public static void main(String[] args) throws Exception {

		AsmFileStatistics worker = new AsmFileStatistics();
		worker.showFileFormat(args[0], args[1]);

	}

}
