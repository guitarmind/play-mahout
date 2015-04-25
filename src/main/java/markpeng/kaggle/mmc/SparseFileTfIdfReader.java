package markpeng.kaggle.mmc;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.mahout.math.NamedVector;
import org.apache.mahout.math.SequentialAccessSparseVector;
import org.apache.mahout.math.Vector.Element;
import org.apache.mahout.math.VectorWritable;

public class SparseFileTfIdfReader {

	public void readMahoutSequenceFile(String dicFilePath, String vectorFilePath) {
		Configuration conf = new Configuration();
		FileSystem fs;
		SequenceFile.Reader read;
		try {
			fs = FileSystem.get(conf);
			// fs = FileSystem.getLocal(conf);

			read = new SequenceFile.Reader(fs, new Path(dicFilePath), conf);
			IntWritable dicKey = new IntWritable();
			Text text = new Text();
			HashMap dictionaryMap = new HashMap();
			try {
				while (read.next(text, dicKey)) {
					dictionaryMap.put(Integer.parseInt(dicKey.toString()),
							text.toString());
				}
			} catch (NumberFormatException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			read.close();

			read = new SequenceFile.Reader(fs, new Path(vectorFilePath), conf);
			Text key = new Text();
			VectorWritable value = new VectorWritable();
			SequentialAccessSparseVector vect;
			while (read.next(key, value)) {
				NamedVector namedVector = (NamedVector) value.get();
				vect = (SequentialAccessSparseVector) namedVector.getDelegate();
				String name = namedVector.getName();

				Iterator<Element> looper = vect.iterator();
				while (looper.hasNext()) {
					Element e = looper.next();
					String token = (String) dictionaryMap.get(e.index());
					double tfidf = e.get();
					if (tfidf > 0)
						System.out.println("Token: " + token
								+ ", TF-IDF weight: " + tfidf);
				}

			}
			read.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		SparseFileTfIdfReader test = new SparseFileTfIdfReader();
		// test.readMahoutSequenceFile(
		// "/home/markpeng/test/mmc_train_10samples-vector/dictionary.file-0",
		// "/home/markpeng/test/mmc_train_10samples-vector/tfidf-vectors/part-r-00000");
		test.readMahoutSequenceFile(
				"/home/markpeng/test/train_10samples_filtered-vector/dictionary.file-0",
				"/home/markpeng/test/train_10samples_filtered-vector/tfidf-vectors/part-r-00000");
	}

}
