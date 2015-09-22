package markpeng.kaggle.cpp;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.TreeSet;

public class CouponTestsetCreator {

	private static final int BUFFER_LENGTH = 10000;
	private static final String newLine = System.getProperty("line.separator");

	private static final double MIN_SIMILARITY = 0.75;

	public List<String> readFile(String filePath) throws Exception {
		List<String> result = new ArrayList<String>();
		BufferedReader in = new BufferedReader(new InputStreamReader(
				new FileInputStream(filePath), "UTF-8"));

		try {
			String aLine = null;
			while ((aLine = in.readLine()) != null) {
				String tmp = aLine.toLowerCase().trim();
				if (tmp.length() > 0 && !result.contains(tmp))
					result.add(tmp);
			}
		} finally {
			in.close();
		}

		return result;
	}

	public void run(String userListPath, String testCouponPath,
			String testOutPath) throws Exception {

		BufferedReader userIn = new BufferedReader(new InputStreamReader(
				new FileInputStream(userListPath), "UTF-8"));

		BufferedReader testMatrixIn = new BufferedReader(new InputStreamReader(
				new FileInputStream(testCouponPath), "UTF-8"));

		BufferedWriter testOut = new BufferedWriter(new OutputStreamWriter(
				new FileOutputStream(testOutPath, false), "UTF-8"));
		StringBuffer resultStr = new StringBuffer();

		try {
			// get user data
			TreeSet<String> userIDs = new TreeSet<String>();
			Hashtable<String, String> userData = new Hashtable<String, String>();
			String aLine = null;
			int userCount = 0;
			String userHeader = userIn.readLine();
			System.out.println(userHeader);
			String[] headerTokens = userHeader.split(",");
			String filterUserHeader = headerTokens[0] + "," + headerTokens[6]
					+ "," + headerTokens[3] + "," + headerTokens[4] + ","
					+ headerTokens[9] + "," + headerTokens[7] + ","
					+ headerTokens[10];

			while ((aLine = userIn.readLine()) != null) {
				String[] tokens = aLine.split(",");
				String userID = tokens[0].replace("\"", "");
				String SEX_ID = tokens[3].replace("\"", "");
				String AGE = tokens[4].replace("\"", "");
				String en_pref = tokens[6].replace("\"", "");
				String registerdDays = tokens[7].replace("\"", "");
				String AGE_RANGE = tokens[9].replace("\"", "");
				String boughtPerWeek = tokens[10].replace("\"", "");
				// System.out.println(userID);

				userIDs.add(userID);

				String row = userID + "," + en_pref + "," + SEX_ID + "," + AGE
						+ "," + AGE_RANGE + "," + registerdDays + ","
						+ boughtPerWeek;
				userData.put(userID, row);

				userCount++;
			}
			System.out.println("User Count: " + userData.size());

			// get test item data
			TreeSet<String> itemIDs = new TreeSet<String>();
			Hashtable<String, String> itemData = new Hashtable<String, String>();
			aLine = null;
			int itemCount = 0;
			String itemHeader = testMatrixIn.readLine();
			System.out.println(itemHeader);
			while ((aLine = testMatrixIn.readLine()) != null) {
				String[] tokens = aLine.split(",");
				String itemID = tokens[0].replace("\"", "");
				System.out.println(itemID);

				itemIDs.add(itemID);
				itemData.put(itemID, aLine);

				itemCount++;
			}

			System.out.println("Item Count: " + itemData.size());

			// create test dataset
			String finalHeader = filterUserHeader + "," + itemHeader;
			resultStr.append(finalHeader);
			resultStr.append(newLine);

			int loopCount = 0;
			for (String userID : userIDs) {
				String userRow = userData.get(userID);
				for (String itemID : itemIDs) {
					String itemRow = itemData.get(itemID);
					resultStr.append(userRow + "," + itemRow);
					resultStr.append(newLine);

					if (loopCount % BUFFER_LENGTH == 0) {
						testOut.write(resultStr.toString());
						testOut.flush();
						resultStr.setLength(0);

						System.out.println("Write record offset: " + loopCount);
					}

					loopCount++;
				}
			}

		} finally {
			userIn.close();
			testMatrixIn.close();

			testOut.write(resultStr.toString());
			testOut.flush();
			testOut.close();
			resultStr.setLength(0);
		}

	}

	public static void main(String[] args) throws Exception {
		// String userListPath =
		// "/home/markpeng/Share/Kaggle/cpp/user_list_en_markpeng.csv";
		// String testCouponPath =
		// "/home/markpeng/Share/Kaggle/cpp/test_coupon_sim_matrix_markpeng.csv";
		// String testOutPath =
		// "/home/markpeng/Share/Kaggle/cpp/test_merge_user_list_markpeng.csv";

		String userListPath = "/home/uitox/kaggle/cpp/user_list_en_markpeng.csv";
		String testCouponPath = "/home/uitox/kaggle/cpp/test_coupon_sim_matrix_markpeng.csv";
		String testOutPath = "/home/uitox/kaggle/cpp/test_merge_user_list_markpeng.csv";

		CouponTestsetCreator worker = new CouponTestsetCreator();
		worker.run(userListPath, testCouponPath, testOutPath);
	}

}
