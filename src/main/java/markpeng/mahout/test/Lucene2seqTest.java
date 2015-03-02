package markpeng.mahout.test;

import java.util.Arrays;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.apache.mahout.text.LuceneStorageConfiguration;
import org.apache.mahout.text.SequenceFilesFromLuceneStorage;

/**
 * Reference:
 * http://blog.trifork.com/2012/03/05/using-your-lucene-index-as-input
 * -to-your-mahout-job-part-i/
 * 
 * @author markpeng
 *
 */
public class Lucene2seqTest {

	public static void main(String[] args) throws Exception {
		Configuration conf = new Configuration();

		Path indexFilesPath = new Path("lucene/indexes/product");
		Path seqFilesOutputPath = new Path("clustering/testdata/sequencefiles/");

		LuceneStorageConfiguration luceneStorageConf = new LuceneStorageConfiguration(
				conf, Arrays.asList(indexFilesPath), seqFilesOutputPath, "id",
				Arrays.asList("title", "description"));
		luceneStorageConf.setQuery(new TermQuery(new Term("body", "Java")));
		luceneStorageConf.setMaxHits(10000);

		SequenceFilesFromLuceneStorage lucene2Seq = new SequenceFilesFromLuceneStorage();
		lucene2Seq.run(luceneStorageConf);

		// Map-Reduce version
		// SequenceFilesFromLuceneStorageMRJob lucene2Seq = new
		// SequenceFilesFromLuceneStorageMRJob();
		// lucene2seq.run(luceneStorageConf);
	}

}
