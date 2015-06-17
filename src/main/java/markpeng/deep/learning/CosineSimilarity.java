package markpeng.deep.learning;

public class CosineSimilarity {
	public static double compute(double[] vec1, double[] vec2) {
		double dp = dotProduct(vec1, vec2);
		double magnitudeA = findMagnitude(vec1);
		double magnitudeB = findMagnitude(vec2);
		return (dp) / (magnitudeA * magnitudeB);
	}

	private static double findMagnitude(double[] vec) {
		double sum_mag = 0;
		for (int i = 0; i < vec.length; i++) {
			sum_mag = sum_mag + vec[i] * vec[i];
		}
		return Math.sqrt(sum_mag);
	}

	private static double dotProduct(double[] vec1, double[] vec2) {
		double sum = 0;
		for (int i = 0; i < vec1.length; i++) {
			sum = sum + vec1[i] * vec2[i];
		}
		return sum;
	}
}
