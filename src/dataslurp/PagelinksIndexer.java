package dataslurp;

import java.io.*;
import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import dataslurp.WikiTitleIndexer.WikiPage;
import util.Pair;

/** Need to be more careful here, wikipedia provides pagelinks table only as an SQL table and parsing sql table is taking too long, doing it this way */
public class PagelinksIndexer {
    static Analyzer			analyzer	= null;
    static IndexWriter		w			= null;

    //------------stuff for reading indexTitlesWithRedirects-------------
    static IndexReader		reader		= null;
    static IndexSearcher	searcher	= null;

    static String			indexPath	= "indexes" + File.separator + "pagelinks";
    static String			SQL_FILE	= "wiki-data" + File.separator + "enwiki-latest-pagelinks-condense.sql";

    //Done in: 6195003

    public static void prepareToReadIndex(String indexPath) {
        try {
            reader = DirectoryReader.open(FSDirectory.open(new File(indexPath)));
            searcher = new IndexSearcher(reader);
        } catch (IOException e) {
            // System.err.println("Couldn't find/read indexTitlesWithRedirects files.");
            e.printStackTrace();
        }
    }

    public static void prepareToWriteIndex(String indexPath) {
        try {
            System.err.println("Index path: " + indexPath);
            Directory index = FSDirectory.open(new File(indexPath));
            IndexWriterConfig iwc = new IndexWriterConfig(Version.LUCENE_CURRENT,
                    analyzer);
            iwc.setOpenMode(OpenMode.CREATE);
            w = new IndexWriter(index, iwc);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (w == null) {
            System.err.println("Couldn't open indexTitlesWithRedirects to write... exiting.");
        }
    }

    //checks if str1 starts with str2
    public static boolean startsWith(String str1, String str2) {
        if (str1 == null || str2 == null)
            return false;
        if (str1.length() < str2.length())
            return false;

        for (int i = 0; i < str2.length(); i++)
            if (str1.charAt(i) != str2.charAt(i))
                return false;
        return true;
    }

    //define the table here
    public static class Tuple {
        String	pl_from, pl_namespace, pl_title, pl_from_namespace;

        public Tuple(String p_f, String p_n, String p_t, String p_f_n) {
            pl_from = p_f;
            pl_namespace = p_n;
            pl_title = p_t;
            pl_from_namespace = p_f_n;
        }

        public Document getDocument() {
            Document doc = new Document();
            if ((pl_from == null) || (pl_namespace == null) || (pl_title == null) || (pl_from_namespace == null))
                return null;
            doc.add(new StringField("pl_from", pl_from, Store.YES));
            doc.add(new StringField("pl_namespace", pl_namespace, Store.YES));
            doc.add(new StringField("pl_title", pl_title, Store.YES));
            doc.add(new StringField("pl_from_namespace", pl_from_namespace, Store.YES));
            return doc;
        }

        @Override
        public String toString() {
            String str = "";
            str += "(" + pl_from + "," + pl_namespace + "," + pl_title + "," + pl_from_namespace + ")";
            return str;
        }
    }

    public static Pair<String, Integer> parse(String line, int offset, int max_num) {
        String value = "";
        boolean inside = false;
        for (int i = offset; i < Math.min(line.length(), offset + max_num + 1); i++) {
            char c = line.charAt(i);
            if (((c == ')') || (c == ',')) && !inside) {
                //System.err.println("Parsed: " + value);
                return new Pair<String, Integer>(value, (i + 1));
            }
            //",\,' chars respectively
            else if ((c != 34) && (c != 92) && c != 39)
                value += c;
            //inside string, need to be more careful
            if (c == 39) {
                if (i == 0)
                    inside = !inside;
                    //has previous char,but that char is not '\'
                else if ((i > 0) && (line.charAt(i - 1) != 92))
                    inside = !inside;
                    // consider ///'
                else if ((i >= 2) && (line.charAt(i - 1) == 92) && line.charAt(i - 2) == 92) {
                    if (i == 2)
                        inside = !inside;
                    if ((i > 2) && (line.charAt(i - 3) != 92))
                        inside = !inside;
                }
            }
        }
        System.err.println("Coudln't parse num from: " + line.substring(offset, Math.min(line.length(), 300 + offset)));
        return null;
    }

    //returns the tuple and offset for next tuple, if its the last one, then  returns -1;
    public static Pair<Tuple, Integer> getTuple(String line, int offset) {
        if (line.charAt(offset) != '(') {
            System.err.println("Wrong offset !? Offset: " + offset + ", in a line of length of: " + line.length());
            return null;
        }
        int io = offset;
        offset++;
        String[] fields = new String[4];
        int[] vals = new int[] { 8, 11, 10000, 11 };
        for (int i = 0; i < 4; i++) {
            Pair<String, Integer> p = parse(line, offset, vals[i]);
            if (p == null) {
                System.err.println("Parse returned null " + line.substring(io, Math.min(line.length(), 300 + io)));
                return null;
            }
            fields[i] = p.first;
            offset = p.second;
        }

        if (offset > 0 && offset < line.length()) {
            int ret = -1;
            if (line.charAt(offset++) == ',') {
                ret = offset;
            }
            Tuple t = new Tuple(fields[0], fields[1], fields[2], fields[3]);
            return new Pair<Tuple, Integer>(t, ret);
        }

        else {
            System.err.println("There's something wrong with the tuple or the offset: " + line.substring(io, Math.min(line.length(), io + 300)) + ", " + offset + "," + fields[0] + ", " + fields[1] + ", " + fields[2] + ", " + fields[3]);
            return null;
        }
    }

    /** Gets tuples in an insert line, starting at offset and returns tuples as pre-defined by table */
    public static Tuple[] getTuples(String line, int offset) {
        //System.err.println("Request get tuple from: " + offset + ", " + line.substring(offset, offset + 300));
        List<Tuple> tuples = new ArrayList<Tuple>();
        while (offset != -1) {
            Pair<Tuple, Integer> p = getTuple(line, offset);
            if (p != null) {
                tuples.add(p.first);
                if ((p.second <= offset) && (p.second != -1))
                    System.err.println("!!!!Warning!!!!\tThe next offset " + p.second + " is less than previous offset:" + offset);
                offset = p.second;
            }
            else {
                System.err.println("Problem parsing tuples at: " + line.substring(offset, offset + 100));
                return null;
            }
        }
        return tuples.toArray(new Tuple[tuples.size()]);
    }

    public static void index() {
        if (SQL_FILE == null) {
            System.err.println("SQL file path is null");
            return;
        }
        prepareToWriteIndex(indexPath);
        try {
            BufferedReader br = new BufferedReader(new FileReader(new File(SQL_FILE)));
            String line = null;
            int ln = 0;
            int initialOffset = "INSERT INTO `pagelinks` VALUES ".length();
            long st = System.currentTimeMillis();
            long totalLines = 12087;
            int nt = 0, fp = 0;
            while ((line = br.readLine()) != null) {
                //careful, this is one big line.
                boolean ok = startsWith(line, "INSERT");
                //probably some regular sql statement
                if (!ok)
                    continue;
                //then its insert operation.
                //a line like this is expected: INSERT INTO `pagelinks` VALUES
                int offset = initialOffset;
                line = line.trim();
                Tuple[] tuples = getTuples(line, offset);
                if (tuples != null) {
                    for (Tuple t : tuples) {
                        Document doc = t.getDocument();
                        if (doc != null)
                            w.addDocument(doc);
                    }
                    nt += tuples.length;
                } else {
                    System.err.println("Failed to parse: " + line.substring(0, 300));
                    fp++;
                }
                if (++ln % 10 == 0)
                    System.err.println("NUmber of lines: " + ln + ", number of tuples: " + nt + ", failed to parse: " + fp + " lines." + "\nTime: " + (System.currentTimeMillis() - st) + ", ETA: " + ((double) ((System.currentTimeMillis() - st) * totalLines)) / (ln * 1000 * 3600));
                //				if (ln > 100)
                //					break;

            }
            System.err.println("Done in: " + (System.currentTimeMillis() - st));
            br.close();
            w.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static Document[] search2(String str, String field, int limit) {
        try {
            TopDocs tds = searcher.search(new TermQuery(new Term(field, str)), limit);
            Document[] docs = new Document[tds.scoreDocs.length];
            int i = 0;
            for (ScoreDoc sd : tds.scoreDocs)
                docs[i++] = searcher.doc(sd.doc);
            return docs;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getText(Document doc) {
        List<IndexableField> fields = doc.getFields();
        String str = "";
        for (IndexableField inf : fields)
            str += inf.name() + "\t" + inf.stringValue() + "\n";
        //String str = doc.get("subject") + ":::\t:::" + doc.get("relation") + ":::\t:::" + doc.get("object");
        return str;
    }

    public static void init() {
        prepareToReadIndex(indexPath);
        System.err.println("Init'ing with indexTitlesWithRedirects path: " + indexPath);
    }

    public static Set<String> getInLinks(String phrase) {
        if (searcher == null)
            init();
        phrase = StringUtils.replaceChars(phrase, " ", "_");
        Document[] docs = null;
        int lt = 400000;

        long st = System.currentTimeMillis();
        docs = search2(phrase, "pl_title", lt);
        Set<String> links = new HashSet<String>();
        if (docs != null)
            for (Document doc : docs)
                if ("0".equals(doc.get("pl_from_namespace")))
                    links.add(doc.get("pl_from"));
        System.err.println("Query: " + phrase + " -> " + docs.length + ", " + (System.currentTimeMillis() - st));
        return links;
    }

    public static void testIndex() {
        prepareToReadIndex(indexPath);
        Document[] docs = null;
        int lt = 400000;
        //docs = search("/.*/", new String[] { "subject" }, lt);

        String[] queries = new String[] { "Water", "India", "Charles_Bernstein", "Abraham_Lincoln", "Stanford_University" };
        for (String q : queries) {
            long st = System.currentTimeMillis();
            docs = search2(q, "pl_title", lt);
            if (docs != null)
                System.err.println("Query: " + q + " -> " + docs.length + ", " + (System.currentTimeMillis() - st));
        }
        try {
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        analyzer = new KeywordAnalyzer();
        String[] strs = new String[] { "Stanford_University", "Silicon_Valley" };
//        WikiPage wp1 = WikiTitleIndexer.getMatch(strs[0]);
//        WikiPage wp2 = WikiTitleIndexer.getMatch(strs[1]);

        Set<String> in1 = getInLinks(strs[0]);
        Set<String> in2 = getInLinks(strs[1]);
        int i = 0;
        for (String str : in2)
            if (in1.contains(str))
                i++;
        //boolean t2 = in1.contains(wp2.id)&&in2.contains(wp1.id);
        //System.err.println("In1 size: " + in1.size() + ", in2 size: " + in2.size() + ", common: " + i+", cross-link: "+t2);
        //testIndex();
    }
}
