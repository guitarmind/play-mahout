package markpeng.mahout.test;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.util.ReflectionUtils;

public class HdfsAPITest {

	public void showRemoteFileList(String hdfsPath) throws Exception {
		Configuration conf = new Configuration();
		conf.set("fs.defaultFS", "hdfs://dev-host18:8020");

		FileSystem fs = FileSystem.get(conf);
		Path path = new Path(hdfsPath);

		FileStatus[] status = fs.listStatus(path);
		for (int i = 0; i < status.length; i++) {
			System.out.println(status[i].getPath());
		}
	}

	public void showFileList(String hdfsPath) throws Exception {
		Configuration conf = new Configuration();
		conf.set("fs.defaultFS", "hdfs://localhost:9000");

		FileSystem fs = FileSystem.get(conf);
		Path path = new Path(hdfsPath);

		FileStatus[] status = fs.listStatus(path);
		for (int i = 0; i < status.length; i++) {
			System.out.println(status[i].getPath());
		}
	}

	public void showFileContent(String hdfsPath) throws Exception {
		Configuration conf = new Configuration();
		conf.set("fs.defaultFS", "hdfs://172.16.10.16:9000");

		FileSystem fs = FileSystem.get(conf);
		Path path = new Path(hdfsPath);

		SequenceFile.Reader reader = new SequenceFile.Reader(conf,
				SequenceFile.Reader.file(path));
		// SequenceFile.Reader reader = new SequenceFile.Reader(FileSystem.get(
		// URI.create(hdfsPath), conf), new Path(outputFile), conf);

		Writable key = (Writable) ReflectionUtils.newInstance(
				reader.getKeyClass(), conf);
		Writable value = (Writable) ReflectionUtils.newInstance(
				reader.getValueClass(), conf);
		System.out.println("Key: " + key.getClass().getName());
		System.out.println("Value: " + value.getClass().getName());
		while (reader.next(key, value)) {
			System.out.println(key + "  <===>  " + value.toString());
		}
		reader.close();
	}

	public static void main(String[] args) throws Exception {
		HdfsAPITest test = new HdfsAPITest();
		// test.showFileList("/user/markpeng/testdata");
		// test.showFileContent("hdfs://localhost:9000/user/markpeng/testoutput");
		test.showRemoteFileList("/user/hdfs/markpeng/sparse-text-ik/tokenized-documents/part-m-00000");
	}

}
