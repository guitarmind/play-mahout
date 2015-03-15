package markpeng.kaggle;

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

									String[] tokens = tmp.split("\\s");
									StringBuffer newTmp = new StringBuffer();
									for (String token : tokens) {
										newTmp.append(token + " ");
									}
									tmp = newTmp.toString().trim();

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

	public List<String> readAssemblyCommands(String cmdFileFoler)
			throws Exception {
		// http://en.wikibooks.org/wiki/X86_Disassembly/Variables#.22extern.22_Variables
		List<String> cmds = new ArrayList<String>();

		BufferedReader in = new BufferedReader(new InputStreamReader(
				new FileInputStream(cmdFileFoler), "UTF-8"));
		String aLine = null;
		while ((aLine = in.readLine()) != null) {
			String tmp = aLine.toLowerCase().trim();
			if (tmp.length() > 0 && !cmds.contains(tmp))
				cmds.add(tmp);
		}

		in.close();
		return cmds;
	}

	public List<String> readAssemblyFuncs(String funcFileFolder)
			throws Exception {
		// http://en.wikibooks.org/wiki/X86_Disassembly/Variables#.22extern.22_Variables
		List<String> cmds = new ArrayList<String>();

		BufferedReader in = new BufferedReader(new InputStreamReader(
				new FileInputStream(funcFileFolder), "UTF-8"));
		String aLine = null;
		while ((aLine = in.readLine()) != null) {
			String tmp = aLine.toLowerCase().trim();
			if (tmp.length() > 0 && !cmds.contains(tmp))
				cmds.add(tmp);
		}

		in.close();
		return cmds;
	}

	public void showCommandAndFunc(String cmdFileFoler, String funcFileFolder,
			String asmLabeledFileFolder, String outputFolder,
			String outputFileName) throws Exception {

		TreeMap<String, TreeMap<String, Integer>> stats = new TreeMap<String, TreeMap<String, Integer>>();

		BufferedWriter out = new BufferedWriter(
				new OutputStreamWriter(new FileOutputStream(outputFolder + "/"
						+ outputFileName, false), "UTF-8"));
		StringBuffer resultStr = new StringBuffer();

		List<String> cmds = readAssemblyCommands(cmdFileFoler);
		List<String> funcs = readAssemblyFuncs(funcFileFolder);

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

								String[] sp = tmp.split("\\t{2,}\\s{2,}");
								List<String> tokens = Arrays.asList(sp);

								resultStr.append(i + "," + f.getName() + ",");

								int index = 0;
								for (String token : tokens) {
									if (index > 0 && token.length() > 1) {
										if (cmds.contains(token)) {
											if (map.get(token) == null)
												map.put(token, 1);
											else {
												map.put(token,
														map.get(token) + 1);
											}

											resultStr.append(token + ",");
											found = true;
										} else if (funcs.contains(token)) {
											if (map.get(token) == null)
												map.put(token, 1);
											else {
												map.put(token,
														map.get(token) + 1);
											}

											resultStr.append(token + ",");
											found = true;
										}
									}

									index++;
								}

								// System.out.println(f.getName() + " ==> "
								// + tmp);
								if (found)
									resultStr.append(newLine);

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
									+ " no command or function found! ");
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
		// worker.showFileFormat(args[0], args[1]);
		worker.showCommandAndFunc(args[0], args[1], args[2], args[3], args[4]);
	}

}
