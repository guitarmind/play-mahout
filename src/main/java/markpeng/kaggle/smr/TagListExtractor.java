package markpeng.kaggle.smr;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.TreeMap;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class TagListExtractor implements Runnable {

	private static final int BUFFER_LENGTH = 1000;
	private static final String newLine = System.getProperty("line.separator");

	private String htmlFolderPath = null;
	private Hashtable<String, String> trainFileList = null;
	private List<String> testFileList = null;
	private String outputTrain = null;
	private String outputTest = null;

	private String folderId = null;

	TreeMap<String, Integer> tagList = new TreeMap<String, Integer>();

	private final String[] TAGS = { "html", "head", "meta", "title", "body",
			"p", "a", "image", "base", "link", "script", "style", "div", "ul",
			"li", "span", "i", "nav" };

	public TagListExtractor(String htmlFolderPath,
			Hashtable<String, String> trainFileList, List<String> testFileList,
			String outputTrain, String outputTest, String folderId) {
		this.htmlFolderPath = htmlFolderPath;
		this.trainFileList = trainFileList;
		this.testFileList = testFileList;
		this.outputTrain = outputTrain;
		this.outputTest = outputTest;
		this.folderId = folderId;
	}

	public static Hashtable<String, String> readTrainListFile(String filePath)
			throws Exception {
		Hashtable<String, String> result = new Hashtable<String, String>();
		BufferedReader in = new BufferedReader(new InputStreamReader(
				new FileInputStream(filePath), "UTF-8"));

		// skip header
		in.readLine();

		try {
			String aLine = null;
			while ((aLine = in.readLine()) != null) {
				String tmp = aLine.trim();
				if (tmp.length() > 0) {
					String[] tokens = tmp.split(",");
					result.put(tokens[0], tokens[1]);
				}
			}
		} finally {
			in.close();
		}

		return result;
	}

	public static List<String> readTestListFile(String filePath)
			throws Exception {
		List<String> result = new ArrayList<String>();
		BufferedReader in = new BufferedReader(new InputStreamReader(
				new FileInputStream(filePath), "UTF-8"));

		// skip header
		in.readLine();

		try {
			String aLine = null;
			while ((aLine = in.readLine()) != null) {
				String tmp = aLine.trim();
				if (tmp.length() > 0) {
					String[] tokens = tmp.split(",");
					result.add(tokens[0]);
				}
			}
		} finally {
			in.close();
		}

		return result;
	}

	@Override
	public void run() {
		try {
			BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(outputTrain, false), "UTF-8"));
			StringBuffer resultStr = new StringBuffer();

			try {
				File checker = new File(htmlFolderPath + "/" + folderId);
				if (checker.exists()) {
					System.out.println("Folder " + folderId + ": "
							+ checker.listFiles().length + " files.");

					List<String> files = new ArrayList<String>();
					for (final File fileEntry : checker.listFiles()) {
						if (fileEntry.getName().contains(".txt")) {
							String tmp = fileEntry.getAbsolutePath();
							files.add(tmp);
						}
					}

					for (String file : files) {

						BufferedReader in = new BufferedReader(
								new InputStreamReader(
										new FileInputStream(file), "UTF-8"));
						File f = new File(file);
						try {
							StringBuffer htmlStr = new StringBuffer();
							String aLine = null;
							while ((aLine = in.readLine()) != null) {
								htmlStr.append(aLine + newLine);
							}

							Document doc = Jsoup.parse(htmlStr.toString());
							doc.
							// // get all raw text
							// String title = doc.title();
							// System.out.println(title);
							// String allText = doc.text();
							// System.out.println(allText.length());
						} finally {
							in.close();
						}

						if (resultStr.length() >= BUFFER_LENGTH) {
							out.write(resultStr.toString());
							out.flush();
							resultStr.setLength(0);
						}

						System.out.println("Scanned html file in label "
								+ folderId + ": " + f.getName());

					} // end of for loop
				}
			} finally {
				try {
					out.write(resultStr.toString());
					out.flush();
					out.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static void main(String[] args) throws Exception {
		String htmlFolderPath = args[0];
		String trainFileListPath = args[1];
		String testFileListPath = args[2];
		String outputTrain = args[3];
		String outputTest = args[4];

		Hashtable<String, String> trainFileList = readTrainListFile(trainFileListPath);
		List<String> testFileList = readTestListFile(testFileListPath);

		for (int i = 0; i < 6; i++) {
			System.out.println("Running for folder " + i + " ...");
			TagListExtractor worker = new TagListExtractor(htmlFolderPath,
					trainFileList, testFileList, outputTrain, outputTest,
					Integer.toString(i));
			worker.run();

			System.out.println("Running thread for folder " + i + " ...");
			Thread.sleep(2000);
		}
	}

}
