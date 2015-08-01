package dataslurp;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queries.CustomScoreProvider;
import org.apache.lucene.queries.CustomScoreQuery;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.*;

/**
 * Indexing completed in 838298ms (10 mins)
 *
 * Reads pages table
 * page_id | page_namespace | page_title | page_restrictions | page_counter | page_is_redirect | page_is_new | page_random | page_touched | page_links_updated | page_latest | page_len | page_content_model |
 * redirect table that contains
 * | rd_from | rd_namespace | rd_title | rd_interwiki | rd_fragment |
 *
 * The final indexTitlesWithRedirects contains
 * Note all pages other than article namespace are excluded from being added to the indexTitlesWithRedirects.
 * The index contains title (text field and hence tokenized), id (stringField), is_redirect(stored field), redirect (String field), length (stored field)
 */
public class WikiTitleIndexer {
    static Analyzer			analyzer		=  new KeywordAnalyzer();
    static IndexWriter		w				= null;

    //------------stuff for reading indexTitlesWithRedirects-------------
    static IndexReader		reader			= null;
    static IndexSearcher	searcher		= null;

    static String			indexPath		= "indexes" + File.separator + "wiki_index";
    static String			REDIRECT_FILE	= "wiki-data" + File.separator + "enwiki-latest-redirect.sql.gz";
    static String			PAGE_FILE		= "wiki-data" + File.separator + "enwiki-latest-page.sql.gz";
    public static String[] STOP_WORDS		= new String[] { "but", "be", "with", "such", "for", "no", "will", "not", "are", "and", "their", "if", "this", "on", "into", "a", "there", "in", "that", "they", "was", "it", "an", "the", "as", "at", "these", "to", "of" };

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
            iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
            w = new IndexWriter(index, iwc);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (w == null) {
            System.err.println("Couldn't open indexTitlesWithRedirects to write... exiting.");
        }
    }

    public static class WikiPage {
        public String	title, id, is_redirect, redirect, length;

        public WikiPage(Document doc) {
            title = doc.get("title");
            id = doc.get("id");
            is_redirect = doc.get("is_redirect");
            redirect = doc.get("redirect");
            length = doc.get("length");
        }

        @Override
        public String toString() {
            String str = "";
            str += "Title: " + title + "\t";
            str += "Id: " + id + "\t";
            str += "is_redirect: " + is_redirect + "\t";
            str += "Redirect: " + redirect + "\t";
            str += "Length: " + length;
            return str;
        }

        public static String getResourceUrl(WikiPage wp) {
            String title = wp.title;
            if (title != null) {
                title = title.replaceAll("\\s", "_");
                return title;
            }
            else
                return null;
        }
    }

    public static class CompletionsScorer extends CustomScoreQuery {
        private Query	query;

        public CompletionsScorer(Query query) {
            super(query);
            this.query = query;
        }

        @Override
        public CustomScoreProvider getCustomScoreProvider(final AtomicReaderContext reader) {
            return new CustomScoreProvider(reader) {
                @Override
                public float customScore(int doc,
                                         float subQueryScore,
                                         float valSrcScore) throws IOException {

                    Set<Term> terms = new HashSet<Term>();
                    query.extractTerms(terms);
                    Set<String> queries = new HashSet<String>();
                    for (Term t : terms)
                        queries.add(t.text());

                    Document d = reader.reader().document(doc);
                    String l = d.get("length");
                    String title = d.get("title");
                    int length = 0;
                    float score = 1;
                    try {
                        length = Integer.parseInt(l);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    int i = 0;
                    String words[] = title.split("(\\_|\\s)");
                    score *= length;
                    if (title.contains("(disambiguation)") || title.contains("filmography") || title.contains("List of") || title.contains("Timeline of"))
                        score *= -1;
                    else {
                        score *= (1 / (float) words.length);
                        boolean found = false;
                        int j = 0;
                        for (String word : words) {
                            if (queries.contains(word)) {//|| word.startsWith("(")) {
                                i++;
                                found = true;
                            }
                            if (!found)
                                j++;
                        }
                        if ((i == words.length) && (words.length > 1))
                            score *= 100;
                        //if the matching term does not start with search term, then penalise it.
                        //						if (j > 0)
                        //							score /= 10;
                    }
                    //System.err.println(title + ", score:" + score + ", length: " + length + ", wl:" + words.length + ", queries: " + queries + ", i:" + i);
                    return score;
                }
            };
        }
    }

    public static class IndexSearcherCustom extends IndexSearcher {
        public IndexSearcherCustom(IndexReader r) {
            super(r);
        }

        TopDocs searchCustom(Weight weight, ScoreDoc after, int nDocs) throws IOException {
            System.err.println("Searching....");
            return super.search(weight, after, nDocs);
        }
    }

    public static String[] parseTuple(String str) {
        List<String> vals = new ArrayList<String>();
        boolean inside = false;
        String val = "";
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if ((c == ',') && (!inside)) {
                if (val.charAt(0) == '\'') {
                    val = StringUtils.stripEnd(val, "'");
                    val = StringUtils.stripStart(val, "'");
                    val = StringUtils.replaceChars(val, '_', ' ');
                }
                vals.add(val);
                val = "";
                continue;
            }
            val += c;
            //' - 39  '\'-92
            if (c == '\'') {
                int j = i - 1;
                int numEscape = 0;
                while ((j >= 0) && (str.charAt(j) == 92)) {
                    numEscape++;
                    j--;
                }
                if (numEscape % 2 == 0)
                    inside = !inside;
            }
        }
        if (val.charAt(0) == '\'') {
            val = StringUtils.stripEnd(val, "'");
            val = StringUtils.stripStart(val, "'");
            val = StringUtils.replaceChars(val, '_', ' ');
            //System.err.println(prev + " stripped to " + val);
        }
        vals.add(val);

        return vals.toArray(new String[vals.size()]);
    }

    //start parsing line from offset
    public static String[][] parseLine(String line, CSVFormat format, int offset) throws IOException {
        line = line.substring(offset);
        //remove the last ')'
        line = line.substring(0, line.length() - 1);
        //StringTokenizer st = new StringTokenizer(line, "\\),\\(");
        String[] tuples = StringUtils.splitByWholeSeparator(line, "),(");
        List<String[]> records = new ArrayList<String[]>();
        //while (st.hasMoreTokens()) {
        //		String[] tuples = line.split("\\),\\(");
        for (String tuple : tuples) {
            //String tuple = st.nextToken();
            String[] vals = parseTuple(tuple);
            //			CSVParser parser = CSVParser.parse(tuple, CSVFormat.DEFAULT);
            //			//System.err.println("Tuple: " + tuple);
            //			List<CSVRecord> recs = parser.getRecords();
            //			if (recs.size() > 1)
            //				System.err.println("What?! There are more than one record in: " + tuple);
            //			if (recs.size() == 0) {
            //				System.err.println("there are no records on this line: " + tuple);
            //				continue;
            //			}
            //			CSVRecord record = recs.get(0);
            //			String[] vals = parseRecord(record);
            records.add(vals);
        }
        return records.toArray(new String[records.size()][]);
    }

    public static void indexTitlesWithRedirects() {
        //not so big, probably around 500MB
        Map<String, String> redirects = new LinkedHashMap<String, String>();
        final String REDIRECT_INSERT_STATEMENT = "INSERT INTO `redirect` VALUES (";
        CSVFormat format = CSVFormat.DEFAULT;
        try {
            LineNumberReader lnr = new LineNumberReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(new File(REDIRECT_FILE)))));
            String line = null;
            int ln = 0;
            while ((line = lnr.readLine()) != null) {
                if (!line.startsWith("INSERT INTO")) {
                    System.err.println("Skipping initial comments...");
                    continue;
                }

                //System.err.println("Parsing line");
                String[][] tuples = parseLine(line, format, REDIRECT_INSERT_STATEMENT.length());
                //System.err.println("Done parsing line");
                for (String[] tuple : tuples) {
                    if (tuple.length < 3) {
                        System.err.println("What?! Tuple size is less than 3 len:" + tuple.length);
                        continue;
                    }
                    if ("0".equals(tuple[1]))
                        //tuple[0] os page id tuple[2] is page title it redirects to
                        redirects.put(tuple[0], tuple[2]);
                }

                if ((ln++) % 100 == 0)
                    System.err.println("Parsed: " + ln + " lines in redirect");
                //				if (ln > 1)
                //					break;
            }
            lnr.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        prepareToWriteIndex(indexPath);
        final String PAGES_INSERT_STATEMENT = "INSERT INTO `pages` VALUES (";
        int ln = 0;
        try {
            LineNumberReader lnr = new LineNumberReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(new File(PAGE_FILE)))));
            String line = null;
            while ((line = lnr.readLine()) != null) {
                if (!line.startsWith("INSERT INTO"))
                    continue;

                String[][] tuples = parseLine(line, format, PAGES_INSERT_STATEMENT.length());
                for (String[] tuple : tuples) {
                    if (tuple.length < 13) {
                        System.err.println("What?! Tuple size is less than 13 len:" + tuple.length);
                        continue;
                    }
                    if ("0".equals(tuple[1])) {
                        //tuple[0] os page id tuple[2] is page title it redirects to
                        String id = tuple[0];
                        String title = tuple[2];
                        String is_redirect = tuple[5];
                        String redirect = null;
                        if ("1".equals(is_redirect))
                            redirect = redirects.get(id);
                        String len = tuple[11];
                        Document doc = new Document();
                        doc.add(new TextField("title", title, Field.Store.YES));
                        doc.add(new StringField("id", id, Field.Store.YES));
                        doc.add(new StoredField("is_redirect", is_redirect));
                        if (redirect != null) {
                            //System.err.println("Id: " + tuple[0] + ", title: " + tuple[2] + ", is redirect: " + tuple[5] + ", len: " + tuple[11] + ", redirect: " + redirect);
                            doc.add(new StringField("redirect", redirect, Field.Store.YES));
                        }
                        doc.add(new StoredField("length", len));
                        w.addDocument(doc);
                    }
                }
                if ((++ln) % 1000 == 0)
                    System.err.println("Parsed: " + ln + " lines in pages");
            }
            lnr.close();
            w.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void init() {
        prepareToReadIndex(indexPath);
    }

    //This method may be inefficient when the number of titles is not very large, as this method iterates over the entire index
    public static Map<String,Set<String>> getVariantsFor(Set<String> titles){
        prepareToReadIndex(indexPath);
        Map<String,Set<String>> allVariants = new LinkedHashMap<>();
        try {
            int numDocs = 0, totalDocs = reader.numDocs();
            for(numDocs = 0;numDocs<reader.maxDoc();numDocs++) {
                Document doc = reader.document(numDocs);
                String redirect = doc.get("redirect");
                //safety check
                redirect = StringUtils.replaceChars(redirect, ' ', '_');
                String title = doc.get("title");
                if (titles.contains(redirect) && (title != null)) {
                    if (!allVariants.containsKey(redirect))
                        allVariants.put(redirect, new HashSet<String>());
                    allVariants.get(redirect).add(title);
                }
                if (numDocs % 100000 == 0)
                    System.err.println("Scanned " + numDocs + " of " + totalDocs + " for redirects collection");

            }
            reader.close();
        }catch(Exception e){
            e.printStackTrace();
        }
        return allVariants;
    }

    public static Document[] search(String query, String[] fields, int lt) {
        try {
            analyzer = new KeywordAnalyzer();
            BooleanClause.Occur[] o = new BooleanClause.Occur[fields.length];
            for (int i = 0; i < fields.length; i++)
                o[i] = BooleanClause.Occur.SHOULD;

            QueryParser qp = new MultiFieldQueryParser(Version.LUCENE_CURRENT, fields, analyzer);
            Query q = qp.parse(query);
            System.err.println("Query: " + q);
            TopDocs tds = searcher.search(q, lt);
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

    //_ separated words in phrase
    public static Set<WikiPage> getRedirects(String phrase){
        Set<WikiPage> pages = new LinkedHashSet<>();
        Document[] docs = search(phrase, new String[]{"redirect"},1000);
        for(Document doc: docs)
            pages.add(new WikiPage(doc));
        return pages;
    }

    public static Set<WikiPage> getRedirectsByIterating(String phrase){
        try {
            Set<WikiPage> pages = new LinkedHashSet<>();
            List<AtomicReaderContext> atomicReaderContexts = reader.leaves();
            for (AtomicReaderContext atomicReaderContext : atomicReaderContexts) {
                AtomicReader atomicReader = atomicReaderContext.reader();
                Bits bits = atomicReader.getLiveDocs();
                for (int i = 0; i < atomicReader.numDocs(); i++) {
                    Document doc = reader.document(i);
                    String redirect = doc.get("redirect");
                    if(phrase.equals(redirect))
                        pages.add(new WikiPage(doc));
                }
            }
            reader.close();
            return pages;
        }catch(Exception e){
            e.printStackTrace();
            return null;
        }
    }

    public static void testIndex() throws IOException {
        prepareToReadIndex(indexPath);
        Set<WikiPage> pages;
        int lt = 1000;
        //docs = search("/.*/", new String[] { "subject" }, lt);
        String[] queries = new String[] { "\"Indira Gandhi\"" };
        for (String q : queries) {
            long st = System.currentTimeMillis();
//            Document[] docs = new Document[] { searcher.doc(23) };
//            for(Document doc: docs) {
//                WikiPage wp = new WikiPage(doc);
//                System.out.println(wp);
//            }
            //pages = search2(q, "title", lt);
            pages = getRedirects(q);
            if (pages != null) {
                System.out.println("Query: " + q + " -> " + pages.size() + ", " + (System.currentTimeMillis() - st));
                for (WikiPage wp : pages)
                    System.out.println(wp + "\n----------");
            }
        }

        try {
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
//        long st = System.currentTimeMillis();
//        indexTitlesWithRedirects();
//        long time = System.currentTimeMillis()-st;
//        System.out.println("Indexing completed in " + time + " ms");
        //testIndex();
        Set<String> t = new HashSet<>();
        t.add("Indira_Gandhi");
        Map<String,Set<String>> vars = getVariantsFor(t);
        for(String str: vars.keySet())
            System.out.println(str+" : "+vars.get(str));
        //Set<WikiPage> links = getCompletions("Sri Lanka");
        //		String[] test = new String[] { "(John) Knox", "Ceylon (or Sri Lanka)", "the Congress Party", "Air France" };
        //		for (String t : test)
        //			System.err.println("Str: " + t + ", " + cleanTitle(t)+"::");
        //		Map<WikiPage, Integer> lens = new LinkedHashMap<WikiPage, Integer>();
        //		for (WikiPage wp : links)
        //			if (wp != null && wp.length != null)
        //				lens.put(wp, Integer.parseInt(wp.length));
        //		List<Pair<WikiPage, Integer>> slens = Util.sortMapByValue(lens);
        //		int i = 0;
        //		if ((slens != null) && (slens.size() > 0)) {
        //			for (Pair<WikiPage, Integer> len : slens) {
        //				System.err.println(len.first.title + "\t" + len.second);
        //				//				if (i++ > 10)
        //				//					break;
        //			}
        //		}
        //		String[] vals = parseTuple("244805,0,'Ballpoint_pen','',''");
        //		for (String val : vals)
        //			System.err.println(val);
//        for (WikiPage wp : links)
//            System.err.println(wp.title + "\tLength:" + wp.length);
    }
}