package markpeng.kaggle;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.mahout.classifier.evaluation.Auc;
import org.apache.mahout.classifier.sgd.L1;
import org.apache.mahout.classifier.sgd.OnlineLogisticRegression;
import org.apache.mahout.math.DenseVector;
import org.apache.mahout.math.Vector;
import org.apache.mahout.vectorizer.encoders.ConstantValueEncoder;
import org.apache.mahout.vectorizer.encoders.StaticWordValueEncoder;

public class LogisticRegressionTraining {

	public static void main(String[] args) {
		LogisticRegressionTraining logisticRegression = new LogisticRegressionTraining();

		// Load the input data
		List<Observation> trainingData = logisticRegression
				.parseInputFile("/home/markpeng/Share/Kaggle/Microsoft Malware Classification/train_full_asm_cmdfunc_20150315.csv");

		// Train a model
		OnlineLogisticRegression olr = logisticRegression.train(trainingData);

		// Test the model
		logisticRegression.testModel(olr);
	}

	public List<Observation> parseInputFile(String inputFile) {
		List<Observation> result = new ArrayList<Observation>();
		BufferedReader br = null;
		String line = "";
		try {
			// Load the file which contains training data
			br = new BufferedReader(new FileReader(new File(inputFile)));
			// Skip the first line which contains the header values
			line = br.readLine();
			// Prepare the observation data
			while ((line = br.readLine()) != null) {
				String[] values = line.split(",");
				result.add(new Observation(values));
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return result;
	}

	public OnlineLogisticRegression train(List<Observation> trainData) {
		OnlineLogisticRegression olr = new OnlineLogisticRegression(9, 786,
				new L1());

		// Train the model using 30 passes
		for (int pass = 0; pass < 30; pass++) {
			for (Observation observation : trainData) {
				olr.train(observation.getActual(), observation.getVector());
			}
			// Every 10 passes check the accuracy of the trained model
			if (pass % 10 == 0) {
				Auc eval = new Auc(0.5);
				for (Observation observation : trainData) {
					eval.add(observation.getActual(),
							olr.classifyScalar(observation.getVector()));
				}
				System.out.format(
						"Pass: %2d, Learning rate: %2.4f, Accuracy: %2.4f\n",
						pass, olr.currentLearningRate(), eval.auc());
			}
		}
		return olr;
	}

	void testModel(OnlineLogisticRegression olr) {
		Observation newObservation = new Observation(new String[] { "family",
				"10", "100000", "0" });
		Vector result = olr.classifyFull(newObservation.getVector());

		System.out.println("------------- Testing -------------");
		System.out.format("Probability of (1) = %.3f\n", result.get(0));
		System.out.format("Probability of (2) = %.3f\n", result.get(1));
		System.out.format("Probability of (3) = %.3f\n", result.get(2));
		System.out.format("Probability of (4) = %.3f\n", result.get(3));
		System.out.format("Probability of (5) = %.3f\n", result.get(4));
		System.out.format("Probability of (6) = %.3f\n", result.get(5));
		System.out.format("Probability of (7) = %.3f\n", result.get(6));
		System.out.format("Probability of (8) = %.3f\n", result.get(7));
		System.out.format("Probability of (9) = %.3f\n", result.get(8));
	}

	class Observation {
		private DenseVector vector = new DenseVector(4);
		private int actual;

		public Observation(String[] values) {
			ConstantValueEncoder interceptEncoder = new ConstantValueEncoder(
					"intercept");
			StaticWordValueEncoder encoder = new StaticWordValueEncoder(
					"feature");

			interceptEncoder.addToVector("1", vector);
			vector.set(0, Double.valueOf(values[1]));
			// Feature scaling, divide mileage by 10000
			vector.set(1, Double.valueOf(values[2]) / 10000);
			encoder.addToVector(values[0], vector);

			this.actual = Integer.valueOf(values[3]);
		}

		public Vector getVector() {
			return vector;
		}

		public int getActual() {
			return actual;
		}
	}
}
