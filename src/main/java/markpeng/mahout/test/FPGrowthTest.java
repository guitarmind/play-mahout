package markpeng.mahout.test;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.Charsets;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.mahout.common.HadoopUtil;
import org.apache.mahout.common.iterator.FileLineIterable;
import org.apache.mahout.common.iterator.StringRecordIterator;
import org.apache.mahout.fpm.pfpgrowth.convertors.ContextStatusUpdater;
import org.apache.mahout.fpm.pfpgrowth.convertors.SequenceFileOutputCollector;
import org.apache.mahout.fpm.pfpgrowth.convertors.string.StringOutputConverter;
import org.apache.mahout.fpm.pfpgrowth.convertors.string.TopKStringPatterns;
import org.apache.mahout.fpm.pfpgrowth.fpgrowth.FPGrowth;

public class FPGrowthTest {

	public static void main(String[] args) throws Exception {
		String input = "/home/markpeng/test/itemsets.dat";
		// String input = "/user/markpeng/testdata/itemsets.dat";
		String pattern = "[,]";
		// String output = "/home/markpeng/test/testoutput";
		// String output = "/user/markpeng/testdata/testoutput";
		String output = "./testdata/testoutput";

		int minSupport = 2;
		int maxHeapSize = 50;

		final Configuration conf = new Configuration();
		conf.set("fs.defaultFS", "hdfs://localhost:9000");
		conf.set("mapreduce.framework.name", "yarn");

		// final Path inputPath = new Path("/user/markpeng");
		final Path outputPath = new Path(output);
		FileSystem fs = FileSystem.get(conf);
		// FileSystem fs = FileSystem.get(inputPath.toUri(), conf);
		HadoopUtil.delete(conf, outputPath);

		final SequenceFile.Writer writer = SequenceFile.createWriter(fs, conf,
				outputPath, Text.class, TopKStringPatterns.class);

		FPGrowth<String> fp = new FPGrowth<String>();
		Set<String> features = new HashSet<String>();
		fp.generateTopKFrequentPatterns(
				new StringRecordIterator(new FileLineIterable(new File(input),
						Charsets.UTF_8, false), pattern),
				fp.generateFList(new StringRecordIterator(new FileLineIterable(
						new File(input), Charsets.UTF_8, false), pattern),
						minSupport),
				minSupport,
				maxHeapSize,
				features,
				new StringOutputConverter(
						new SequenceFileOutputCollector<Text, TopKStringPatterns>(
								writer)), new ContextStatusUpdater(null));
	}

}
