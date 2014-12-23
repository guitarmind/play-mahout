package markpeng.mahout.test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.Charsets;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.mahout.common.HadoopUtil;
import org.apache.mahout.common.Pair;
import org.apache.mahout.common.Parameters;
import org.apache.mahout.common.iterator.FileLineIterable;
import org.apache.mahout.common.iterator.StringRecordIterator;
import org.apache.mahout.fpm.pfpgrowth.PFPGrowth;
import org.apache.mahout.fpm.pfpgrowth.convertors.ContextStatusUpdater;
import org.apache.mahout.fpm.pfpgrowth.convertors.SequenceFileOutputCollector;
import org.apache.mahout.fpm.pfpgrowth.convertors.string.StringOutputConverter;
import org.apache.mahout.fpm.pfpgrowth.convertors.string.TopKStringPatterns;
import org.apache.mahout.fpm.pfpgrowth.fpgrowth.FPGrowth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.google.common.io.Closeables;

public class FPGrowthTest {

	private static final Logger log = LoggerFactory
			.getLogger(FPGrowthTest.class);

	private static void runSequentialFPGrowth(Parameters params)
			throws IOException {
		log.info("Starting Sequential FPGrowth");
		int maxHeapSize = Integer.valueOf(params.get("maxHeapSize", "50"));
		int minSupport = Integer.valueOf(params.get("minSupport", "3"));

		Path output = new Path(params.get("output", "output.txt"));
		Path input = new Path(params.get("input"));

		Configuration conf = new Configuration();
		FileSystem fs = FileSystem.get(output.toUri(), conf);

		Charset encoding = Charset.forName(params.get("encoding"));

		String pattern = params.get("splitPattern",
				PFPGrowth.SPLITTER.toString());

		SequenceFile.Writer writer = new SequenceFile.Writer(fs, conf, output,
				Text.class, TopKStringPatterns.class);

		FSDataInputStream inputStream = null;
		FSDataInputStream inputStreamAgain = null;

		Collection<String> features = Sets.newHashSet();

		if ("true".equals(params.get(PFPGrowth.USE_FPG2))) {
			org.apache.mahout.fpm.pfpgrowth.fpgrowth2.FPGrowthObj<String> fp = new org.apache.mahout.fpm.pfpgrowth.fpgrowth2.FPGrowthObj<String>();

			try {
				inputStream = fs.open(input);
				inputStreamAgain = fs.open(input);
				fp.generateTopKFrequentPatterns(
						new StringRecordIterator(new FileLineIterable(
								inputStream, encoding, false), pattern),
						fp.generateFList(new StringRecordIterator(
								new FileLineIterable(inputStreamAgain,
										encoding, false), pattern), minSupport),
						minSupport,
						maxHeapSize,
						features,
						new StringOutputConverter(
								new SequenceFileOutputCollector<Text, TopKStringPatterns>(
										writer)));
			} finally {
				Closeables.close(writer, false);
				Closeables.close(inputStream, true);
				Closeables.close(inputStreamAgain, true);
			}
		} else {
			FPGrowth<String> fp = new FPGrowth<String>();

			inputStream = fs.open(input);
			inputStreamAgain = fs.open(input);
			try {
				fp.generateTopKFrequentPatterns(
						new StringRecordIterator(new FileLineIterable(
								inputStream, encoding, false), pattern),
						fp.generateFList(new StringRecordIterator(
								new FileLineIterable(inputStreamAgain,
										encoding, false), pattern), minSupport),
						minSupport,
						maxHeapSize,
						features,
						new StringOutputConverter(
								new SequenceFileOutputCollector<Text, TopKStringPatterns>(
										writer)),
						new ContextStatusUpdater(null));
			} finally {
				Closeables.close(writer, false);
				Closeables.close(inputStream, true);
				Closeables.close(inputStreamAgain, true);
			}
		}

		List<Pair<String, TopKStringPatterns>> frequentPatterns = FPGrowth
				.readFrequentPattern(conf, output);
		for (Pair<String, TopKStringPatterns> entry : frequentPatterns) {
			log.info("Dumping Patterns for Feature: {} \n{}", entry.getFirst(),
					entry.getSecond());
		}
	}

	public static void main(String[] args) throws Exception {
		String input = "/home/markpeng/test/itemsets.dat";
		// String input = "/user/markpeng/testdata/itemsets.dat";
		String pattern = "[,]";
		// String output = "/home/markpeng/test/testoutput";
		// String output = "/user/markpeng/testdata/testoutput";
		String output = "./testdata/testoutput";
		// String output =
		// "/home/markpeng/GitHub/play-mahout/testdata/testoutput";
		String outputFolder = "./testdata";

		int minSupport = 2;
		int maxHeapSize = 50;

		final Configuration conf = new Configuration();
		// not necessary
		// conf.set("fs.defaultFS", "hdfs://localhost:9000");
		// conf.set("mapreduce.framework.name", "yarn");

		final Path inputPath = new Path(input);
		final Path outputPath = new Path(output);
		final Path outputFolderPath = new Path(outputFolder);
		FileSystem fs = FileSystem.get(inputPath.toUri(), conf);
		HadoopUtil.delete(conf, outputPath);

		final SequenceFile.Writer writer = SequenceFile.createWriter(fs, conf,
				outputPath, Text.class, TopKStringPatterns.class);

		try {
			FPGrowth<String> fp = new FPGrowth<String>();
			Set<String> features = new HashSet<String>();
			fp.generateTopKFrequentPatterns(
					new StringRecordIterator(new FileLineIterable(new File(
							input), Charsets.UTF_8, false), pattern),
					fp.generateFList(new StringRecordIterator(
							new FileLineIterable(new File(input),
									Charsets.UTF_8, false), pattern),
							minSupport),
					minSupport,
					maxHeapSize,
					features,
					new StringOutputConverter(
							new SequenceFileOutputCollector<Text, TopKStringPatterns>(
									writer)), new ContextStatusUpdater(null));

			// dump results
			List<Pair<String, TopKStringPatterns>> frequentPatterns = FPGrowth
					.readFrequentPattern(conf, outputPath);
			for (Pair<String, TopKStringPatterns> entry : frequentPatterns) {
				log.info("Dumping Patterns for Feature: {} \n{}",
						entry.getFirst(), entry.getSecond());
			}
		} finally {
			Closeables.close(writer, false);
		}
	}

}
