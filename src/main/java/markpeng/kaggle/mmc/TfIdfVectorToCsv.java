package markpeng.kaggle.mmc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.text.DecimalFormat;
import java.util.Iterator;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.apache.mahout.math.NamedVector;
import org.apache.mahout.math.SequentialAccessSparseVector;
import org.apache.mahout.math.Vector.Element;
import org.apache.mahout.math.VectorWritable;

public class TfIdfVectorToCsv {
	private static final int BUFFER_LENGTH = 1000;
	private static final String newLine = System.getProperty("line.separator");
	private static final DecimalFormat formatter = new DecimalFormat("#.#####");

	public static void main(String[] args) throws Exception {
		if (args.length < 2) {
			System.out.println("Arguments:[tfidfPath] [csvPath]");
			return;
		}
		String tfidfPath = args[0];
		String csvPath = args[1];

		Configuration conf = new Configuration();
		System.out.println("Loading TF/IDF file ......");

		int count = 0;
		File checker = new File(tfidfPath);
		if (checker.isDirectory()) {
			for (final File fileEntry : checker.listFiles()) {
				if (fileEntry.getName().startsWith("part-r-")) {
					String filePath = fileEntry.getAbsolutePath();

					StringBuffer outputStr = new StringBuffer();
					BufferedWriter out = new BufferedWriter(
							new OutputStreamWriter(new FileOutputStream(
									csvPath, false), "UTF-8"));
					try {
						FileSystem fs = FileSystem.get(conf);
						SequenceFile.Reader read = new SequenceFile.Reader(fs,
								new Path(filePath), conf);
						try {
							Text key = new Text();
							VectorWritable value = new VectorWritable();
							SequentialAccessSparseVector vect;
							while (read.next(key, value)) {
								NamedVector namedVector = (NamedVector) value
										.get();
								vect = (SequentialAccessSparseVector) namedVector
										.getDelegate();
								String name = namedVector.getName();
								String label = name.substring(1, 2);
								outputStr.append(label + " ");

								System.out.println(name + " ==> " + label);

								Iterator<Element> looper = vect.iterator();
								while (looper.hasNext()) {
									Element e = looper.next();
									int word_id = e.index();
									double tfidf = e.get();
									outputStr.append(word_id + ":"
											+ formatter.format(tfidf) + " ");
								}

								outputStr.append(newLine);
								if (outputStr.length() >= BUFFER_LENGTH) {
									out.write(outputStr.toString());
									out.flush();
									outputStr.setLength(0);
								}

								count++;
							}
						} finally {
							read.close();
						}

					} finally {
						out.write(outputStr.toString());
						out.flush();
						out.close();
					}
				}
			}
		}
		System.out.println("Number of processed documents in training set: "
				+ count);
	}
}
