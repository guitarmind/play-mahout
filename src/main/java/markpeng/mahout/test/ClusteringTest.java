package markpeng.mahout.test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.mahout.clustering.Cluster;
import org.apache.mahout.clustering.classify.WeightedPropertyVectorWritable;
import org.apache.mahout.clustering.kmeans.KMeansDriver;
import org.apache.mahout.clustering.kmeans.Kluster;
import org.apache.mahout.common.HadoopUtil;
import org.apache.mahout.common.distance.EuclideanDistanceMeasure;
import org.apache.mahout.math.RandomAccessSparseVector;
import org.apache.mahout.math.Vector;
import org.apache.mahout.math.VectorWritable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ClusteringTest {
	// ---- Static

	private static final Logger LOG = LoggerFactory
			.getLogger(ClusteringTest.class);
	private static final String BASE_PATH = "";
	private static final String POINTS_PATH = BASE_PATH + "/points";
	private static final String CLUSTERS_PATH = BASE_PATH + "/clusters";
	private static final String OUTPUT_PATH = BASE_PATH + "/output";

	// ---- Fields

	private final double[][] points = { { 1, 1 }, { 2, 1 }, { 1, 2 }, { 2, 2 },
			{ 3, 3 }, { 8, 8 }, { 9, 8 }, { 8, 9 }, { 9, 9 } };

	private final int numberOfClusters = 2;

	// ---- Methods

	private void start() throws Exception {

		final Configuration configuration = new Configuration();

		// Create input directories for data
		final File pointsDir = new File(POINTS_PATH);
		if (!pointsDir.exists()) {
			pointsDir.mkdir();
		}

		// read the point values and generate vectors from input data
		final List<Vector> vectors = vectorize(points);

		// Write data to sequence hadoop sequence files
		writePointsToFile(configuration, vectors);

		// Write initial centers for clusters
		writeClusterInitialCenters(configuration, vectors);

		// Run K-means algorithm
		final Path inputPath = new Path(POINTS_PATH);
		final Path clustersPath = new Path(CLUSTERS_PATH);
		final Path outputPath = new Path(OUTPUT_PATH);
		HadoopUtil.delete(configuration, outputPath);

		KMeansDriver.run(configuration, inputPath, clustersPath, outputPath,
				0.001, 10, true, 0, false);

		// Read and print output values
		readAndPrintOutputValues(configuration);
	}

	private void writePointsToFile(final Configuration conf,
			final List<Vector> points) throws IOException {

		final Path path = new Path(POINTS_PATH + "/pointsFile");

		FileSystem fs = FileSystem.getLocal(conf);
		final SequenceFile.Writer writer = SequenceFile.createWriter(fs, conf,
				path, IntWritable.class, VectorWritable.class);

		int recNum = 0;
		final VectorWritable vec = new VectorWritable();

		for (final Vector point : points) {
			vec.set(point);
			writer.append(new IntWritable(recNum++), vec);
		}

		writer.close();
	}

	private void writeClusterInitialCenters(final Configuration conf,
			final List points) throws IOException {
		final Path writerPath = new Path(CLUSTERS_PATH + "/part-00000");

		FileSystem fs = FileSystem.getLocal(conf);
		final SequenceFile.Writer writer = SequenceFile.createWriter(fs, conf,
				writerPath, Text.class, Kluster.class);

		for (int i = 0; i < numberOfClusters; i++) {
			final Vector vec = (Vector) points.get(i);

			// write the initial centers
			final Kluster cluster = new Kluster(vec, i,
					new EuclideanDistanceMeasure());
			writer.append(new Text(cluster.getIdentifier()), cluster);
		}

		writer.close();
	}

	private void readAndPrintOutputValues(final Configuration conf)
			throws IOException {
		final Path input = new Path(OUTPUT_PATH + "/"
				+ Cluster.CLUSTERED_POINTS_DIR + "/part-m-00000");

		FileSystem fs = FileSystem.getLocal(conf);
		final SequenceFile.Reader reader = new SequenceFile.Reader(fs, input,
				conf);

		final IntWritable key = new IntWritable();
		final WeightedPropertyVectorWritable value = new WeightedPropertyVectorWritable();

		while (reader.next(key, value)) {
			LOG.info("{} belongs to cluster {}", value.toString(),
					key.toString());
		}
		reader.close();
	}

	// Read the points to vector from 2D array
	public List vectorize(final double[][] raw) {
		final List points = new ArrayList();

		for (int i = 0; i < raw.length; i++) {
			final Vector vec = new RandomAccessSparseVector(raw[i].length);
			vec.assign(raw[i]);
			points.add(vec);
		}

		return points;
	}

	public static void main(final String[] args) {
		final ClusteringTest application = new ClusteringTest();

		try {
			application.start();
		} catch (final Exception e) {
			LOG.error("ClusteringTest failed", e);
		}
	}
}
