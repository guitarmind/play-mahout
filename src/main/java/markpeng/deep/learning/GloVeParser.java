package markpeng.deep.learning;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.TreeMap;

public class GloVeParser {

	private static final String newLine = System.getProperty("line.separator");

	public TreeMap<String, double[]> parse(String gloveFilePath, int vectorSize)
			throws Exception {
		TreeMap<String, double[]> gloveVectors = new TreeMap<String, double[]>();

		BufferedReader in = new BufferedReader(new InputStreamReader(
				new FileInputStream(gloveFilePath), "UTF-8"));

		try {
			String aLine = null;
			while ((aLine = in.readLine()) != null) {
				if (aLine.length() > 0) {
					String[] tokens = aLine.split("\\s");
					if (tokens.length == vectorSize + 1) {
						String word = tokens[0];
						double[] vector = new double[vectorSize];
						for (int i = 0; i < vectorSize; i++)
							vector[i] = Double.parseDouble(tokens[i + 1]);

						gloveVectors.put(word, vector);
					}
				}
			}
		} finally {
			in.close();
		}

		return gloveVectors;
	}

	public double vectorSimilarity(String target1, String target2,
			TreeMap<String, double[]> gloveVectors) {
		double score = 0.0;
		double[] vector1 = gloveVectors.get(target1);
		double[] vector2 = gloveVectors.get(target2);

		score = CosineSimilarity.compute(vector1, vector2);
		return score;
	}

	public double vectorDistance(String target1, String target2,
			TreeMap<String, double[]> gloveVectors) {
		double dist = 0.0;
		double[] vector1 = gloveVectors.get(target1);
		double[] vector2 = gloveVectors.get(target2);

		// Euclidean distance
		double sum = 0.0;
		for (int i = 0; i < vector1.length; i++)
			sum += Math.pow(vector1[i] - vector2[i], 2);

		dist = Math.sqrt(sum);

		return dist;
	}

	public static void main(String[] args) throws Exception {

		String gloveFilePath = "/home/markpeng/Share/Kaggle/Search Results Relevance/ssr_vectors_20150616.txt";
		GloVeParser parser = new GloVeParser();
		TreeMap<String, double[]> gloveVectors = parser.parse(gloveFilePath,
				300);
		// String target1 = "woman";
		// String target2 = "man";
		// String target3 = "bag";
		// String target1 = "tv";
		// String target2 = "video";
		// String target3 = "bag";
		String target1 = "ps";
		String target2 = "video";
		String target3 = "dress";
		// double score = parser.vectorSimilarity(target1, target2,
		// gloveVectors);
		// System.out.println("Similarity between " + target1 + " and " +
		// target2
		// + ": " + score);
		double dist = parser.vectorDistance(target1, target2, gloveVectors);
		System.out.println("Distance between " + target1 + " and " + target2
				+ ": " + dist);
		double dist2 = parser.vectorDistance(target1, target3, gloveVectors);
		System.out.println("Distance between " + target1 + " and " + target3
				+ ": " + dist2);
	}
}
