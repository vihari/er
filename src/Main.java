import dataslurp.ElectionDataParser;
import dataslurp.WikiTitleIndexer;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.lang3.StringUtils;
import util.Pair;
import util.Util;

import java.io.*;
import java.util.*;

public class Main {
    public static class Token{
        public String name, leftStr, rightStr;
        public Map<Character, Integer> leftFreqs, rightFreqs;
        public Token(String name, String leftStr, String rightStr){
            this.name = name;
            this.leftFreqs = new LinkedHashMap<>();
            this.rightFreqs = new LinkedHashMap<>();
            for(char c: leftStr.toCharArray()) {
                if(!leftFreqs.containsKey(c))
                    leftFreqs.put(c, 0);
                leftFreqs.put(c, leftFreqs.get(c)+1);
            }
            for(char c: rightStr.toCharArray()) {
                if(!rightFreqs.containsKey(c))
                    rightFreqs.put(c, 0);
                rightFreqs.put(c, rightFreqs.get(c)+1);
            }
            this.leftStr = leftStr;
            this.rightStr = rightStr;
        }

        private static double modulous(Map<Character, Integer> v){
            double d = 0;
            for(int val: v.values())
                d += val*val;
            return Math.sqrt(d);
        }

        private static double dotProduct(Map<Character,Integer> v1, Map<Character, Integer> v2){
            Set<Character> intersection = new HashSet<>(v1.keySet());
            intersection.retainAll(v2.keySet());
            double d = 0;
            for(Character c: intersection)
                d += v1.get(c)*v2.get(c);
            double m1 = modulous(v1);
            double m2 = modulous(v2);
            //System.err.println("d, m1, m2: "+d+" "+m1+" "+m2);
            if((m1>0) && (m2>0))
                return d/(m1*m2);
            //they both are empty
            else if ((m1 == 0) && (m2 == 0))
                return 1;
            else
                return d;
        }

        public double scoreWith(Token token){
//            System.err.println("tp: "+this.name+", "+this.leftFreqs.size()+", "+rightFreqs.size());
//            System.err.println(token.name+", "+token.leftFreqs.size()+", "+token.rightFreqs.size());
            double s1 = dotProduct(leftFreqs, token.leftFreqs);
            double s2 = dotProduct(rightFreqs, token.rightFreqs);
            return s1+s2;
        }

        @Override
        public String toString(){
            String str = "";
            str += "Name: "+name+", ";
            str += "Left-string: "+leftStr+", ";
            str += "Right-string: "+rightStr+", ";
//            for(char c: leftFreqs.keySet())
//                str += c+":"+leftFreqs.get(c)+" ";
            str += leftFreqs.size()+" ";
            str += ", ";
            str += rightFreqs.size()+" ";
//            for(char c: rightFreqs.keySet())
//                str += c+":"+rightFreqs.get(c)+" ";
            return str;
        }
    }

    //method copied from http://rosettacode.org/wiki/Levenshtein_distance#Java
    public static int distanceLevenhtein(String a, String b) {
        a = a.toLowerCase();
        b = b.toLowerCase();
        // i == 0
        int [] costs = new int [b.length() + 1];
        for (int j = 0; j < costs.length; j++)
            costs[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            costs[0] = i;
            int nw = i - 1;
            for (int j = 1; j <= b.length(); j++) {
                int cj = Math.min(1 + Math.min(costs[j], costs[j - 1]), a.charAt(i - 1) == b.charAt(j - 1) ? nw : nw + 1);
                nw = costs[j];
                costs[j] = cj;
            }
        }
        return costs[b.length()];
    }

    public static void testLD(String [] args) {
        String [] data = { "kitten", "sitting", "saturday", "sunday", "rosettacode", "raisethysword" };
        for (int i = 0; i < data.length; i += 2)
            System.out.println("distance(" + data[i] + ", " + data[i+1] + ") = " + distanceLevenhtein(data[i], data[i + 1]));
    }

    /**Train data should be in the form of list of variants which are known to correspond*/
    public static Map<String,Double> trainModel(String[][] trainData){
        //collect list of list of words that are edit distance close
        int TOLERENT_EDIT_DISTANCE = 4, MAX_WINDOW_SIZE = 3, MIN_WORD_LENGTH = 4;
        int MAX_FEATURES_PER_VARIANT = 1000;
        List<List<String>> wordVariants = new ArrayList<List<String>>();
        for(String[] variants: trainData) {
            for (String variant : variants) {
                String[] words = variant.split("[\\s_]+");
                for (String word : words) {
                    if (word.length() < 4)
                        continue;

                    if (wordVariants.size() > 0 && wordVariants.get(0).size() > 0) {
                        boolean added = false;
                        for (List<String> vars : wordVariants) {
                            //comparing it to the first one with a higher tolerance is more efficient than comparing with all the known variants
                            String kv = vars.get(0);
                            int ld = distanceLevenhtein(kv, word);
                            if (ld <= TOLERENT_EDIT_DISTANCE) {
                                vars.add(word);
                                added = true;
                                break;
                            }
                        }
                        if (!added) {
                            List<String> arr = new ArrayList<>();
                            arr.add(word);
                            wordVariants.add(arr);
                        }
                    } else {
                        List<String> arr = new ArrayList<>();
                        arr.add(word);
                        wordVariants.add(arr);
                    }
                }
            }
        }

        for(List<String> vars: wordVariants) {
            for (String word : vars) {
                System.out.print(word + " ");
            }
            System.out.println();
        }

        //we emit tokens from substrings in the names
        List<Token> tokens = new ArrayList<>();
        for(List<String> vars: wordVariants)
            for(String word: vars){
                if(word.length()<MIN_WORD_LENGTH)
                    continue;
                for(int i=0;i<word.length();i++) {
                    for(int w=1;w<=MAX_WINDOW_SIZE;w++) {
                        if((i+w)<word.length()) {
                            String substr = word.substring(i, i + w);
                            String leftStr = word.substring(0, i);
                            String rightStr = word.substring(i+1+w, word.length());
                            tokens.add(new Token(substr, leftStr, rightStr));
                        }
                    }
                }
            }
//        for(Token t: tokens)
//            System.err.println(t);
        System.err.println("Number of tokens: "+tokens.size());
        Map<String, Double> features = new LinkedHashMap<>();
        for(int i=0;i<tokens.size();i++){
            for(int j=0;j<tokens.size();j++) {
                Token t1 = tokens.get(i);
                Token t2 = tokens.get(j);
                if ((i==j) || t1.name.equals(t2.name))
                    continue;
                //canonicalisation
                if(t1.name.hashCode()<t2.name.hashCode())
                    features.put(t1.name+":::"+t2.name, t1.scoreWith(t2));
                else
                    features.put(t2.name+":::"+t1.name, t1.scoreWith(t2));
            }
        }
        return features;
    }

    /**
     * Takes in a list of indian names, see this list as training sequence with which it tries to understand what indian names look like
     * @param indianNames list of indian names
     * @param dbpedia dbpedia list in the form of title -> type*/
    public static Set<String> collectIndianNames(Set<String> indianNames, Map<String,String> dbpedia){
        //likely indian names from DBpedia
        Set<String> inames = new LinkedHashSet<>();
        Map<String,Double> allWords = new LinkedHashMap<>();
        //filtered high confidence indian words
        Set<String> iwords = new LinkedHashSet<>();
        //this method to identify indian names may also collect few people names who are not originally indian
        //for example Sheik, Mohd. etc. As long as people with such names also exist in India, this is OK.
        for(String in: indianNames) {
            String[] words = in.split("\\s+");
            for(String w: words) {
                w = w.toLowerCase();
                //sometimes there are words like k.t.
                if(w.length()<=4)
                    continue;
                if(!allWords.containsKey(w))
                    allWords.put(w, 0.0);
                allWords.put(w, allWords.get(w) + 1.0);
            }
        }
        Map<String, Integer> dbpediaFreqs = new LinkedHashMap<>();
        for(String str: dbpedia.keySet()) {
            String[] words = str.split("\\s+");
            for (String w : words) {
                w = w.toLowerCase();
                if(!dbpediaFreqs.containsKey(w))
                    dbpediaFreqs.put(w, 0);
                dbpediaFreqs.put(w, dbpediaFreqs.get(w));
            }
        }
        //build a tf-idf like score
        for(String str: allWords.keySet()) {
            if(dbpediaFreqs.containsKey(str))
                allWords.put(str, allWords.get(str) / dbpediaFreqs.get(str));
            //no point considering this word further
            else
                allWords.put(str, -1.0);
        }
            //filter words based on number of times hey occurred in indian names
        List<Pair<String,Double>> sWords = Util.sortMapByValue(allWords);
        int THRESHOLD = 10, i=0;
        long maxWords = Math.round(sWords.size()*0.1);
        for(Pair<String,Double> sw: sWords) {
            //System.err.println(sw);
            if(i++<maxWords)
                iwords.add(sw.getFirst());
        }

        System.err.println("All words: "+allWords.size()+" Filtered Words: "+iwords.size()+"\n"+iwords);

        for(String str: dbpedia.keySet()) {
            //if any word is an instance of indian word
            String[] words = str.toLowerCase().split("\\s+");
            boolean indian = false;
            String type = dbpedia.get(str);
            for(String w: words)
                if(iwords.contains(w)) {
                    indian = true;
                    break;
                }
            //without the check for type, may pull in many location and org names
            if(indian && type.endsWith("Person"))
                inames.add(str);
        }
        return inames;
    }

    /**
     * Collects indian names from DBpedia infobox*/
    public static Set<String> collectIndianNamesFromDBpedia() {
        String DBPEDIA_INFO_FILE = "wiki-data/infobox-properties_en.nt.bz2";
        //dump these names in to a file so that we can reuse
        String DUMP_FILE = "indexes/IndianNamesFromDBpedia.txt";
        Set<String> indianNames = new HashSet<>();

        File f = new File(DUMP_FILE);
        if(f == null || !f.exists()) {
            try {
                FileWriter fw = new FileWriter(f);
                //boolean true to compressor to parse till the end of file
                LineNumberReader lnr = new LineNumberReader(new InputStreamReader(new BZip2CompressorInputStream(new FileInputStream(new File(DBPEDIA_INFO_FILE)), true)));
                String line = null;
                //there can be placeOfBirth and birthPlace
                String[] personProps = new String[]{"Birth", "Death", "Nationality"};
                while ((line = lnr.readLine()) != null) {
                    boolean cand = false;
                    line = line.trim();
                    for (String pp : personProps)
                        if (StringUtils.containsIgnoreCase(line, pp)) {
                            cand = true;
                            break;
                        }
                    if (cand) {
                        //splitting on space can be dangerous
                        String[] tuples = line.split(">");
                        if(tuples.length>2) {
                            String val = tuples[2];
                            int idx = val.indexOf("India");
                            if(idx>=0){
                                Character rightChar = null;
                                idx += "India".length();
                                if(idx<val.length())
                                    rightChar = val.charAt(idx);
                                if( (rightChar == null) || (rightChar == ' ') || (rightChar == '"') || (rightChar == '>')) {
                                    String n = tuples[0];
                                    //the trailing > is already removed
                                    n = StringUtils.replaceOnce(n, "<http://dbpedia.org/resource/", "");
                                    indianNames.add(n);
                                    //write the entire line, this can help in understanding how name is in the list and to put further filtering on the names
                                    fw.write(line + "\n");
                                }
                            }
                        }
                    }
                    if(lnr.getLineNumber()%10000 == 0)
                        System.out.println("Read: "+lnr.getLineNumber());
                }
                System.out.println("Number of lines read from the infobox file: " + lnr.getLineNumber());
                fw.close();
                lnr.close();
            }catch(Exception e){
                System.err.println("Returning incomplete set of names");
                e.printStackTrace();
            }
            return indianNames;
        }else{
            try {
                BufferedReader br = new BufferedReader(new FileReader(f));
                String line;
                while ((line = br.readLine()) != null) {
                    String[] tuples = line.split(">");
                    if(tuples.length>2) {
                        String n = tuples[0];
                        //the trailing > is already removed
                        n = StringUtils.replaceOnce(n, "<http://dbpedia.org/resource/", "");
                        indianNames.add(n);
                    }
                }
            }catch(Exception e){
                System.err.println("Returning an empty set of indian names");
                e.printStackTrace();
            }
            return indianNames;
        }
    }

    // illegal wrt chars that occur in names
    public static boolean containsIllegalChars(String str) {
        str = str.replaceAll("\\w+","");
        for (char c : str.toCharArray())
            if ((c != '-') && (c != ',') && (c != ' ') && (c != '.')) {
                //alien to typical characters found in people names, not a hieroglyphic :)
                System.err.println("Found an alien character " + c);
                return true;
            }
        return false;
    }

    public static boolean containsStopWord(String str){
        List<String> sws = Arrays.asList(WikiTitleIndexer.STOP_WORDS);
        String[] words = str.split("\\W+");
        for(String w: words)
            if(sws.contains(w.toLowerCase()))
                return true;
        return false;
    }

    public static String[][] gatherTrainingData(){
        String TRAINING_FILE = "data/IndianNameTrainingData.txt";
        File f = new File(TRAINING_FILE);
        if((f == null) || !f.exists()) {
            try {
                FileWriter fw = new FileWriter(f);
                Set<String> names = collectIndianNamesFromDBpedia();
                System.out.println("Found: " + names.size() + " indian names in DBpedia");
                Map<String, Set<String>> allVars = WikiTitleIndexer.getVariantsFor(names);
                String[][] trainingData = new String[allVars.size()][];
                int t = 0, numVars = 0;
                outer:
                for (String k : allVars.keySet()) {
                    String title = StringUtils.replaceChars(k, '_', ' ');
                    Set<String> vars = allVars.get(k);
                    List<String> all = new ArrayList<>();
                    all.add(title);
                    all.addAll(vars);
                    // /should be more careful while gathering data
                    List<String> cleaned = new ArrayList<>();
                    for (int i = 0; i < all.size(); i++) {
                        String str = all.get(i);
                        str = str.replaceAll(" \\(.*\\)","");
                        str = str.replaceAll("[0-9]+","");
                        //strings of foreign language contains \ and should also be filterd by the check below
                        if(containsIllegalChars(str) || containsStopWord(str))
                            if(i==0) {
                                //if the title itself does not look good (not english?)
                                continue outer;
                            }else{
                                continue;
                            }
                        if(!str.contains(" "))
                            continue;
                        cleaned.add(str);
                    }
                    if(cleaned.size() == 1)
                        continue ;
                    //redirects are all space delimited
                    trainingData[t++] = cleaned.toArray(new String[cleaned.size()]);
                    numVars += cleaned.size();
                    String str = "";
                    for(String var: cleaned)
                        str += var+":::";
                    fw.write(str+"\n");
                }
                System.out.println("Found variations for " + allVars.size() + " on an average of " + (numVars / allVars.size()) + " per title");
                fw.close();
                return trainingData;
            }catch(Exception e){
                e.printStackTrace();
                return null;
            }
        }else {
            List<String[]> allVars = new ArrayList<>();
            try {
                BufferedReader br = new BufferedReader(new FileReader(f));
                String line;
                while((line=br.readLine())!=null) {
                    line = line.trim();
                    String[] vars = line.split(":::");
                    allVars.add(vars);
                }
                br.close();
            }catch(Exception e){
                e.printStackTrace();
            }
            return allVars.toArray(new String[allVars.size()][]);
        }
    }

    public static void main(String[] args){
        String[][] trainData = gatherTrainingData();
        trainModel(trainData);
        Map<String, Double> features = trainModel(trainData);
        List<Pair<String, Double>> sfeatures = Util.sortMapByValue(features);
        //just print top 1000
        int i=0;
        for (Pair<String, Double> p : sfeatures){
            System.out.println(p);
            if (i++ > 1000)
                break;
        }
    }

    public static void main1(String[] args) {
        String[] trainData = {"B S Yediyurappa",
                "B. S. Yaddyurappa",
                "B. S. Yediyurappa",
                "B.S. Yadiurappa",
                "B.S. Yadiyurappa",
                "B.S. Yeddyurappa",
                "B.S. Yediyurappa",
                "B.S.Yediyurappa",
                "BS Yeddyurappa",
                "BS Yediyurappa",
                "Dr. Bokanakere Siddalingappa Yeddyurappa",
                "Dr.Bokanakere Siddalingappa Yeddyurappa",
                "Yaddiyurappa",
                "Yadiurappa",
                "Yadiyurappa",
                "Yeddy urappa",
                "Yeddyurappa",
                "Yediyurappa",
                "Yedyurappa"
        };
//        trainData = new String[]{
//                "Gandhi, Indira",
//                "Gandhi, Indira Priyadarshini",
//                "Indira Bundghi",
//                "Indira Ghandi",
//                "Indira Nehru",
//                "Indira Nehru Gandhi",
//                "Indira Priyadarshini Gandhi",
//                "Indira gandhi",
//                "Indira ghandi",
//                "Mrs. Gandhi",
//                "mt Indira Gandhi",
//                "Smt. Indira Gandhi",
//                "Summary of indira gandhi as prime minister"
//        };
        //Map<String, Double> features = trainModel(trainData);
//        List<Pair<String, Double>> sfeatures = Util.sortMapByValue(features);
//        //just print top 1000
//        int i=0;
//        for (Pair<String, Double> p : sfeatures){
//            System.out.println(p);
//            if (i++ > 1000)
//                break;
//        }
    }
}
