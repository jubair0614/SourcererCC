/**
 * 
 */
package indexbased;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.Version;

import models.QueryBlock;
import models.TokenInfo;
import noindex.CloneHelper;

/**
 * @author vaibhavsaini
 * 
 */
public class CodeSearcher {
    private String indexDir;
    private IndexSearcher searcher;
    private Analyzer analyzer;
    private IndexReader reader;
    private QueryParser queryParser;
    private String field;

    public CodeSearcher(String indexDir, String field) {
        System.out.println("index directory: "+ indexDir);
        this.field = field;
        this.indexDir = indexDir;
        try {
            this.reader = DirectoryReader.open(FSDirectory.open(new File(
                    this.indexDir)));
        } catch (IOException e) {
            System.out.println("cant get the reader to index dir, exiting, "
                    + indexDir);
            e.printStackTrace();
            System.exit(1);
        }
        this.searcher = new IndexSearcher(this.reader);
        this.analyzer = new WhitespaceAnalyzer(Version.LUCENE_46); // TODO: pass
                                                                   // the
                                                                   // analyzer
                                                                   // as
                                                                   // argument
                                                                   // to
                                                                   // constructor
        new CloneHelper(); // i don't remember why we are making this object?
        this.queryParser = new QueryParser(Version.LUCENE_46, this.field,
                analyzer);
    }

    public void search(QueryBlock queryBlock, TermSearcher termSearcher)
            throws IOException {
        // List<String> tfsToRemove = new ArrayList<String>();
        termSearcher.setReader(this.reader);
        // System.out.println("setting reader: "+this.reader +
        // Util.debug_thread());
        termSearcher.setQuerySize(queryBlock.getSize());
        termSearcher.setComputedThreshold(queryBlock.getComputedThreshold());
        int termsSeenInQuery = 0;
        StringBuilder prefixTerms = new StringBuilder();
        for (Entry<String, TokenInfo> entry : queryBlock.getPrefixMap()
                .entrySet()) {
	    Query query = null;
            try {
                prefixTerms.append(entry.getKey() + " ");
                synchronized (this) {
                    query = queryParser.parse("\"" + entry.getKey() + "\"");
                }
                termSearcher.setSearchTerm(query.toString(this.field));
                termSearcher.setFreqTerm(entry.getValue().getFrequency());
                termsSeenInQuery += entry.getValue().getFrequency();
                termSearcher.searchWithPosition(termsSeenInQuery);
            } catch (org.apache.lucene.queryparser.classic.ParseException e) {
                System.out.println("cannot parse " + entry.getKey() );
            }
        }
    }

    public CustomCollectorFwdIndex search(Document doc) throws IOException {
        CustomCollectorFwdIndex result = new CustomCollectorFwdIndex();
        Query query = null;
        try {
            synchronized (this) {
                query = queryParser.parse(doc.get("id"));
            }
            /*
             * System.out.println("Searching for: " + query.toString(this.field)
             * + " : " + doc.get("id"));
             */
            this.searcher.search(query, result);
        } catch (org.apache.lucene.queryparser.classic.ParseException e) {
            System.out.println("cannot parse (id): " + doc.get("id") + ". Ignoring this.");
        }
        return result;
    }
    
    public CustomCollectorFwdIndex search(String id) throws IOException {
        CustomCollectorFwdIndex result = new CustomCollectorFwdIndex();
        Query query = null;
        try {
            synchronized (this) {
                query = queryParser.parse(id);
            }
            /*
             * System.out.println("Searching for: " + query.toString(this.field)
             * + " : " + doc.get("id"));
             */
            this.searcher.search(query, result);
        } catch (org.apache.lucene.queryparser.classic.ParseException e) {
            System.out.println("cannot parse (" + id +"):" + id + ". Ignoring this.");
        }
        return result;
    }

    public long getFrequency(String key) {
        CustomCollectorFwdIndex result = new CustomCollectorFwdIndex();
        Query query = null;
        long frequency = -1l;
        try {
            synchronized (this) {
                query = queryParser.parse(key);
            }
            /*
             * System.out.println("Searching for: " + query.toString(this.field)
             * + " : " + doc.get("id"));
             */
            this.searcher.search(query, result);
            List<Integer> blocks = result.getBlocks();
            if (blocks.size() == 1) {
                Document document = this.getDocument(blocks.get(0));
                frequency = Long.parseLong(document.get("frequency"));
            }
        } catch (org.apache.lucene.queryparser.classic.ParseException e) {
            System.out.println("cannot parse (freq): " + key + ". Ignoring this.");
        } catch (NumberFormatException e) {
            System.out.println("getPosition method in CodeSearcher "
                    + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return frequency;
    }

    /*
     * public synchronized CustomCollectorFwdIndex search(Document doc, int i)
     * throws IOException { CustomCollectorFwdIndex result = new
     * CustomCollectorFwdIndex(); Query query; try { query =
     * queryParser.parse(doc.get("id"));
     * 
     * System.out.println("Searching for: " + query.toString(this.field) + " : "
     * + doc.get("id"));
     * 
     * this.searcher.search(query, result); } catch
     * (org.apache.lucene.queryparser.classic.ParseException e) {
     * System.out.println("cannot parse " + e.getMessage()); } return result; }
     */

    public Document getDocument(long docId) throws IOException {
	try {
	    return this.searcher.doc((int) docId);
	} catch (IllegalArgumentException e) {
	    System.out.println(SearchManager.NODE_PREFIX + ", CodeSearcher on " + indexDir + ": invalid docId " + docId);
	    return null;
	}
    }

    /**
     * @return the reader
     */
    public IndexReader getReader() {
        return reader;
    }

    /**
     * @param reader
     *            the reader to set
     */
    public void setReader(IndexReader reader) {
        this.reader = reader;
    }

    public void close() {
	try {
	    this.reader.close();
	} catch (IOException e) {
            System.out.println(e.getMessage());
	}
    }

}
