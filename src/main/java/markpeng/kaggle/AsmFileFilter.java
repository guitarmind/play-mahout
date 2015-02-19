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

public class AsmFileFilter {

	private static final int BUFFER_LENGTH = 1000;
	private static final String newLine = System.getProperty("line.separator");

	public List<String> readAssemblyCommands(List<String> files)
			throws Exception {
		// http://en.wikibooks.org/wiki/X86_Disassembly/Variables#.22extern.22_Variables
		List<String> cmds = new ArrayList<String>();

		for (String file : files) {
			BufferedReader in = new BufferedReader(new InputStreamReader(
					new FileInputStream(file), "UTF-8"));
			String aLine = null;
			while ((aLine = in.readLine()) != null) {
				String tmp = aLine.toLowerCase().trim();
				if (!cmds.contains(tmp))
					cmds.add(tmp);
			}

			in.close();
		}

		return cmds;
	}

	private int matchedCommandIndex(List<String> cmds, String text,
			List<String> tokens) {
		int startIndex = -1;

		for (String cmd : cmds) {
			if (text.contains(cmd) && tokens.contains(cmd)
					&& !tokens.contains("db")) {
				int index = text.indexOf(cmd);
				if (startIndex == -1)
					startIndex = index;
				else if (startIndex > index)
					startIndex = index;
			}
		}

		return startIndex;
	}

	public void filter(List<String> cmds, String asmFileFolder,
			String outputFolder) throws Exception {
		File checker = new File(asmFileFolder);
		if (checker.exists()) {
			List<String> asmFiles = new ArrayList<String>();
			for (final File fileEntry : checker.listFiles()) {
				if (fileEntry.getName().contains(".asm")) {
					String tmp = fileEntry.getAbsolutePath();
					asmFiles.add(tmp);
				}
			}

			for (String asmFile : asmFiles) {
				String filterFileName = (new File(asmFile)).getName()
						+ "_filtered";

				StringBuffer resultStr = new StringBuffer();
				BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
						new FileOutputStream(outputFolder + "/"
								+ filterFileName, false), "UTF-8"));

				BufferedReader in = new BufferedReader(new InputStreamReader(
						new FileInputStream(asmFile), "UTF-8"));

				try {
					String aLine = null;
					while ((aLine = in.readLine()) != null) {
						String tmp = aLine.toLowerCase().trim();
						String[] sp = tmp.split("\\s");
						List<String> tokens = Arrays.asList(sp);
						int commentIdx = -1;
						if (tokens.contains(";")) {
							commentIdx = tmp.indexOf(";");
							tmp = tmp.substring(0, commentIdx);
						}
						// System.out.println(tmp);
						int startIndex = matchedCommandIndex(cmds, tmp, tokens);
						if (startIndex >= 0) {
							if (commentIdx >= 0 && commentIdx < startIndex)
								continue;

							String filtered = tmp.substring(startIndex);
							// System.out.println(filtered);

							resultStr.append(filtered + " " + newLine);
							if (resultStr.length() >= BUFFER_LENGTH) {
								out.write(resultStr.toString());
								out.flush();
								resultStr.setLength(0);
							}
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
			} // end of for loop
		}
	}

	public static void main(String[] args) throws Exception {
		String usageString = "Usage: AsmFileFilter <cmdFile> <asmFolder> <outputFolder> \n\n";

		usageString += "Where <cmdFile> = the path of assembly command files. Concatenate by '|' \n";
		usageString += "<asmFolder> = the path of asm files.\n";
		usageString += "<outputFolder> = output folder for filtered files.\n";

		// args = new String[3];
		// args[0] =
		// "/home/markpeng/Share/Kaggle/Microsoft Malware Classification/ireullin/8086.txt|/home/markpeng/Share/Kaggle/Microsoft Malware Classification/ireullin/80386.txt";
		// args[1] =
		// "/home/markpeng/Share/Kaggle/Microsoft Malware Classification/dataSample";
		// args[2] =
		// "/home/markpeng/Share/Kaggle/Microsoft Malware Classification/dataSample";

		if (args.length == 3) {
			String cmdFile = args[0];
			String asmFolder = args[1];
			String outputFolder = args[2];
			List<String> files = new ArrayList<String>();
			if (cmdFile.contains("|")) {
				String[] sp = cmdFile.split("\\|");
				files = Arrays.asList(sp);
			} else
				files.add(cmdFile);

			AsmFileFilter worker = new AsmFileFilter();
			List<String> cmds = worker.readAssemblyCommands(files);
			worker.filter(cmds, asmFolder, outputFolder);

		} else {
			System.out.println(usageString);
		}

		// ---------------------------------------------------------------------------------------------
		// Test Case

		// AsmFileFilter test = new AsmFileFilter();
		// List<String> files = new ArrayList<String>();
		// files.add("/home/markpeng/Share/Kaggle/Microsoft Malware Classification/ireullin/8086.txt");
		// files.add("/home/markpeng/Share/Kaggle/Microsoft Malware Classification/ireullin/80386.txt");
		// List<String> cmds = test.readAssemblyCommands(files);
		// test.filter(
		// cmds,
		// "/home/markpeng/Share/Kaggle/Microsoft Malware Classification/dataSample",
		// "/home/markpeng/Share/Kaggle/Microsoft Malware Classification/dataSample");
	}

}
