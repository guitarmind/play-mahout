package markpeng.mahout;

import java.io.BufferedReader;
import java.io.FileReader;
import java.net.URI;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;

public class CsvToSeqUtil {

	public static void csvToSingleValueSeqFile(String inputFile,
			String outputFile, String valueColumn) throws Exception {
		// int NUM_COLUMNS = 1;

		Configuration conf = new Configuration();
		conf.set("fs.defaultFS", "hdfs://localhost:9000");
		conf.set("fs.hdfs.impl",
				org.apache.hadoop.hdfs.DistributedFileSystem.class.getName());

		FileSystem fs = FileSystem.get(conf);
		Path path = new Path(outputFile);

		// FileStatus[] status = fs.listStatus(path);
		// for (int i = 0; i < status.length; i++) {
		// BufferedReader br = new BufferedReader(new InputStreamReader(
		// fs.open(status[i].getPath())));
		// String line;
		// line = br.readLine();
		// while (line != null) {
		// System.out.println(line);
		// line = br.readLine();
		// }
		// }

		// SequenceFile.Writer writer = new SequenceFile.Writer(fs, conf, path,
		// Text.class, Text.class);
		SequenceFile.Writer writer = new SequenceFile.Writer(FileSystem.get(
				URI.create(inputFile), conf), conf, path, Text.class,
				Text.class);

		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(inputFile));
			String aLine;
			// get columns of CSV
			aLine = br.readLine();
			String[] columns = aLine.split(",");
			int valueIndex = -1;
			for (int i = 0; i < columns.length; i++) {
				if (columns[i].equals(valueColumn)) {
					valueIndex = i;
					break;
				}
			}
			int rowIndex = 0;
			while ((aLine = br.readLine()) != null) {
				String[] rowData = aLine.split(",");
				String key = Integer.toString(rowIndex);
				writer.append(new Text(key), new Text(rowData[valueIndex]));

				rowIndex++;
			}
		} finally {
			if (br != null)
				br.close();
			writer.close();
		}

		// check result file
		SequenceFile.Reader reader = new SequenceFile.Reader(fs, new Path(
				outputFile), conf);
		// SequenceFile.Reader reader = new SequenceFile.Reader(FileSystem.get(
		// URI.create(outputFile), conf), new Path(outputFile), conf);
		Text key = new Text();
		Text value = new Text();
		while (reader.next(key, value)) {
			System.out.println(key.toString() + " , " + value.toString());
		}
		reader.close();
	}

	public static void main(String[] args) throws Exception {
		// String libpath = System.getProperty("java.library.path");
		// System.out.println("LIBPATH =" + libpath);

		String INPUT_FILE = "hdfs://localhost:9000/user/markpeng/testdata/out_2014-12-08.csv";
		// String INPUT_FILE = "/user/markpeng/testdata/out_2014-12-08.csv";
		// String INPUT_FILE =
		// "/home/markpeng/Share/UitoxSearchLog/RegularCustomer/AW000001/out_2014-12-08.csv";
		String OUTPUT_FILE = "hdfs://localhost:9000/user/markpeng/testdata/out_2014-12-08.seq";
		// String OUTPUT_FILE = "/home/markpeng/test/out_2014-12-08.seq";
		CsvToSeqUtil.csvToSingleValueSeqFile(INPUT_FILE, OUTPUT_FILE, "q");
	}
}
