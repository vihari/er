/**
 * Created by vihari on 01/08/15.
 */
import dataslurp.ElectionDataParser;
import sun.jvm.hotspot.debugger.cdbg.Sym;
import util.Pair;
import util.Util;

import java.io.File;
import java.io.FileWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Trainer {
    public static class Symbol{
        public String name;
        Pattern lookLike;
        //represents if this symbol which corresponds to name is abbreviated(Nara->N), partially abbreviated (Mohummad -> Mohd.)
        // or expanded form (N -> Nara) of the corresponding name
        public int form;
        //dont add TITLE and NEW to the list of symbols as these are not dependent on the token/name
        static int ORIGINAL = 0, ABBREVIATED = 1, PARTIALLY_ABBREVIATED = 2, EXPANDED = 3, LOWERCASED = 4;

        public Symbol(String name, int form){
            this.name = name;
            this.form = form;
        }

        public static int getNumSymbolTypes(){
            //TODO: keep this updated
            return 5;
        }

        /**Returns if the name looks like the name transformation of this type*/
        public boolean lookLike(String tgt){
            //fraction of chars that can be replaced
            double TOLERANT_EDIT_DISTANCE = 3,
            MAX_AMIN_PENALTY = 0.15;
            if(form == ORIGINAL){
                //for smaller distances, edit distance tolerance is not reliable
                if((name.length() < 4) || (tgt.length() < 4))
                    return name.equals(tgt);

                //edit distance should only account for internal variations
                if(name.charAt(0)!=tgt.charAt(0))
                    return false;
                /// /consider an edit distance measure
                //int ed = Util.distanceLevenshtein(name, tgt);
                double penalty =  AMIN.AMIN_English(name, tgt);
                //System.err.println("src: "+name+", tgt: "+tgt+", ed: "+ed);
                //return (ed<=TOLERANT_EDIT_DISTANCE);
                return (penalty <= MAX_AMIN_PENALTY);
            }
            else if(form == ABBREVIATED){
                String fc = name.substring(0,1);
                return tgt.equals(fc);
            }
            else if(form == PARTIALLY_ABBREVIATED){
                //target should be a substring of name
                return (name.indexOf(tgt) == 0);
            }
            else if(form == EXPANDED){
                return (tgt.indexOf(name) == 0);
            }
            else if(form == LOWERCASED){
                String lc = name.toLowerCase();
                return lc.equals(tgt);
            }
            else
                return false;
        }

        //ideally a check for if the next char is period e.t.c can improve things
        public static boolean isTitle(String word) {
            //Sen cannot be a title as its a valid indian word
            List<String> titles = Arrays.asList("Mrs", "Mr", "Dr", "Doctor", "Professor", "Prof", "President", "Marshal", "Lieutenant", "Shri", "Smt", "Sri",
                    "Padmashree", "Padmasree", "Saint", "Sant", "Pt", "Pandit", "Sepoy", "Param", "Hans", "Sir", "Col", "Colonel", "Fr", "Father", "Capt", "Captain",
                    "Maj", "Major", "Gen", "General", "Senate", "Senator", "Jr", "Sr", "Miss", "Hon", "Rev", "Reverend", "Lieut", "Msgr", "Magister", "Commander",
                    "Lord", "Advocate", "Justice", "MD", "PM", "Bharatratna", "Padmavibhushan");
            return titles.contains(word);
        }

        public static String inferSymbolType(String word, String name, int startIndex, int endIndex) {
            Character nextChar = null;
            if(endIndex<name.length())
                nextChar = name.charAt(endIndex);
            if ((startIndex == 0) || (endIndex == word.length()))
                if (isTitle(word))
                    return "TITLE";
            char wc = word.charAt(0);
            if ((wc >= 'A') && (wc <= 'Z') && (word.length() == 1))
                return "ABBREVIATED";

            if ((nextChar!=null) && (nextChar == '.')) {
                if ((wc >= 'A') && (wc <= 'Z')) {
                    if (word.length() > 1)
                        return "PARTIALLY_ABBREVIATED";
                    else
                        return "ABBREVIATED";
                }
            }

            return "NORMAL";
        }

        @Override
        public String toString(){
            return name+"-"+form;
        }
    }

    public static String canonicaliseSeparatingChars(String sc){
        String csc = sc.replaceAll("\\-+","-").replaceAll(",+",",").replaceAll("\\s+"," ").replaceAll("\\.+",".");
        return csc;
    }

    public static String getNotationOriginal(String name){
        return getNotationOriginal(name, new LinkedHashSet<String>());
    }

    //returns a notation for the original title from which symbols are generated
    public static String getNotationOriginal(String name, Set<String> usedWords){
        Pattern wordP = Pattern.compile("[A-Za-z]+");
        Matcher m = wordP.matcher(name);
        int prevEnd = -1;
        String notation = "";
        while(m.find()){
            String word = m.group();
            int start = m.start();
            int end = m.end();

            if(prevEnd>-1){
                String sc = name.substring(prevEnd, start);
                notation += canonicaliseSeparatingChars(sc);
            }
            String stype = null;
            if(usedWords == null || usedWords.contains(word))
                stype = Symbol.inferSymbolType(word, name, start, end);
            else
                stype = "["+word+"]";
            notation += stype;
            prevEnd = end;
        }
        return notation;
    }

    /**
     * returns null if the variant cannot be satisfactorily represented by the symbols, for example when the variant
     * contains too many new words
     * @param variant Name for which the notation is to be generated
     * @param symbols
     * @return Notation and strings of symbols used for notation*/
    public static Pair<String, Set<String>> getNotation(String variant, Symbol[] symbols){
        Pattern wordP = Pattern.compile("[A-Za-z][a-z]*");
        Matcher m = wordP.matcher(variant);
        //index at which the previous word ended, useful for special char isolation
        int prevEnd = -1;
        int numNEWWords = 0, numWords = 0;
        String notation = "", sepChar = ":";
        //sometimes recognising titles on word level may not be enough, consider "Wing Commander (Retd.) Sashikant Oak", "Wing Commander (Retd.)" is the title
        //so that we don't assign two words in the target name to the same symbol variant
        Set<Integer> assignedSymbols = new LinkedHashSet<>();
        while(m.find()){
            String word = m.group();
            numWords++;
            int start = m.start();
            int end = m.end();

            //append separating chars first
            String sc = null;
            if(prevEnd>=0){
                sc = canonicaliseSeparatingChars(variant.substring(prevEnd, start));
                notation += sc;
            }
            //check if its the title
            if((start == 0 || end == variant.length()) && Symbol.isTitle(word)) {
                notation += "-1";
            }
            //TODO: we should do a better job at word in a sequence alignment
            else {
                int si = -1;
                for (int i = 0; i < symbols.length; i++) {
                    if(assignedSymbols.contains(Math.round(i/Symbol.getNumSymbolTypes())))
                        continue;
                    Symbol symbol = symbols[i];
                    if (symbol.lookLike(word)) {
                        si = i;
                        notation += sepChar + si + sepChar;
                        //if its an expansion, then look closely at what it expands to
                        if(symbol.form == Symbol.EXPANDED){
                            //we only consider the possibility of merging two words
                            String sw = word.substring(symbol.name.length());
                            int j = i + Symbol.getNumSymbolTypes() - Symbol.EXPANDED;
                            if(j<symbols.length) {
                                String nxtWord = symbols[j].name;
                                //case may have changed Chandra Babu -> Chandrababu
                                if(nxtWord.toLowerCase().equals(sw.toLowerCase())) {
                                    System.out.println("Found a merge: " + symbols[si].name + " " + nxtWord + " -> " +  word);
                                    notation += sepChar + (si+1) + sepChar;
                                    int r = Math.round(si/Symbol.getNumSymbolTypes());
                                    assignedSymbols.add(r);
                                    assignedSymbols.add(r+1);
                                    break;
                                }
                            }
                        }
                        //Ab -> A B
                        //check for a merge
                        else if (symbol.form == Symbol.PARTIALLY_ABBREVIATED){
                            Matcher m1 = wordP.matcher(variant);
                            if(m1.find(end)) {
                                String nxtWord = m1.group();
                                if (nxtWord != null)
                                    if ((word.toLowerCase() + nxtWord.toLowerCase()).equals(symbol.name.toLowerCase())) {
                                        //since we consider only two words, the end index does not make sense
                                        int s = m1.start();
                                        System.out.println("Found a merge: " + symbol.name + " -> " + word + variant.substring(end, s) + nxtWord);
                                        String csc = canonicaliseSeparatingChars(variant.substring(end, s));
                                        notation += csc + sepChar + (si - symbol.form) + "[" + nxtWord.toLowerCase() + "]" + sepChar;
                                        //push the pointer of matcher
                                        m.find();
                                        end = m.end();
                                        assignedSymbols.add(Math.round(si/Symbol.getNumSymbolTypes()));
                                        break;
                                    }
                            }
                        }
                        //System.err.println("Adding: "+si + ", "+ Symbol.getNumSymbolTypes()+", "+Math.round(si/Symbol.getNumSymbolTypes()));
                        assignedSymbols.add(Math.round(si/Symbol.getNumSymbolTypes()));
                        break;
                    }
                }
                if (si == -1) {
                    numNEWWords++;
                    //this is NEW Symbol
                    si = symbols.length;
                    //notation += sepChar+si+sepChar;
                    notation += "[" + word + "]";
                }
            }
            prevEnd = end;
        }
        //append symbols not used to represent target
        Set<String> wordsUsed = new LinkedHashSet<>();
        for(int i: assignedSymbols)
            wordsUsed.add(symbols[i*Symbol.getNumSymbolTypes()].name);
        //System.err.println(wordsUsed + ", " + variant);

        //if half the words are unseen, dont consider
        //could be the case of Indra Gandhi -> Indira Nehru
        //or Indira Gandhi -> First Women Prime Minister Indira Gandhi
        if( ((double)numNEWWords/numWords) >= 0.5)
            return null;
        return new Pair<>(notation, wordsUsed);
    }

    public static Symbol[] getSymbols(String name){
        if(name == null)
            return null;
        List<Symbol> symbols = new ArrayList<>();
        String[] words = name.split("\\W+");
        for(String word: words) {
            //we append all the possible types, some of the types may be redundant
            //for example when the word is already an abbreviation, then the forms ABBREVIATED, PARTIALLY_ABBREVIATED will never be used
            //similarly when the word is fully expanded then the form EXPANDED is redundant
            int[] formTypes = new int[]{Symbol.ORIGINAL, Symbol.ABBREVIATED, Symbol.PARTIALLY_ABBREVIATED, Symbol.LOWERCASED, Symbol.EXPANDED};
            for(int ft: formTypes)
                symbols.add(new Symbol(word, ft));
        }
        return symbols.toArray(new Symbol[symbols.size()]);
    }

    public static Map<String,Integer> train(){
        //we expect the first string in the array to be the proper title and the variants of the title follow
        String[][] tdata = Main.gatherTrainingData();
        Map<String, Integer> transformations = new LinkedHashMap<>();
        //sample example for each transformation
        Map<String,String> sample = new LinkedHashMap<>();
        for(String[] vars: tdata) {
            if((vars!=null) && (vars.length>0)) {
                String title = vars[0];
                Symbol[] symbols = getSymbols(title);
                //get title notation based on the symbols we know
                //String tn = getNotationOriginal(title);
                for (int v = 1;v < vars.length; v++) {
                    if(vars[v].equals(title))
                        continue;
                    Pair<String, Set<String>> p = getNotation(vars[v], symbols);
                    if(p == null)
                        continue;
                    String notation = p.getFirst();
                    Set<String> usedWords = p.getSecond();
                    String tn = getNotationOriginal(title, usedWords);
                    if(notation == null ||(notation.length() == 0))
                        continue;
                    String mapping = tn + "->" + notation;
                    if(!transformations.containsKey(mapping))
                        transformations.put(mapping, 0);
                    transformations.put(mapping, transformations.get(mapping)+1);
                    sample.put(mapping, title+"->"+vars[v]);
                }
            }
        }
        //TODO: Some filtering of the transformations may be required to remove some xns that are too general and can pull noise
        //It can also be accounted for when the data is collected
        List<Pair<String,Integer>> sxns = Util.sortMapByValue(transformations);
        for(Pair<String,Integer> sxn: sxns)
            System.out.println(sxn + sample.get(sxn.first));
        return transformations;
    }

    public static class Cluster{
        public Set<String> elements;
        //the one that stands as a representative for this cluster and pulls in any new name
        public String sentinel;
        public Symbol[] symbols;
        //all possible transformations on sentinel
        public Map<String,Integer> possibleXns;

        public Cluster(String sentinel, Map<String,Integer> allXns){
            this.sentinel = sentinel;
            symbols = getSymbols(sentinel);
            elements = new LinkedHashSet<>();
            elements.add(sentinel);
            possibleXns = allXns;
            //System.err.println("Possible Xns size for:"+notation+" is "+possibleXns.size());
        }

        //similarity of a name to this cluster
        public double getSimilarityScore(String name){
            boolean overlap = false;
            for(int i=0;i<symbols.length;i+=Symbol.getNumSymbolTypes())
                if(symbols[i].name.length()>2 && name.contains(symbols[i].name)){
                    overlap = true;
                    break;
                }
            //we dont want to consider overlap like "T. N. Moorthy" -> "Tangutoori Narayana"
            if(!overlap)
                return 0.0;
            Pair<String,Set<String>> p = getNotation(name, symbols);
            if(p == null || p.getFirst() == null)
                return 0.0;
            String nt = p.getFirst();
            String notation = getNotationOriginal(sentinel, p.getSecond());
            Integer s = possibleXns.get(notation+"->"+nt);
            if(s!=null)
                System.err.println("Similarity: "+s+" "+sentinel+":"+name+", "+notation+"->"+nt);
            if(s == null)
                return 0.0;
            else
                return s;
        }

        public void add(String name){
            elements.add(name);
        }

        @Override
        public String toString(){
            String ret = "";
            for(String str: elements)
                ret += str+":::";
            return ret;
        }
    }

    //replaces multiple upper case letter occurrances with Upper case followed by lower case
    public static String canonicaliseName(String n){
        Pattern wordP = Pattern.compile("[A-Z]+");
        Matcher m = wordP.matcher(n);
        StringBuffer sb = new StringBuffer();
        while(m.find()){
            String ucname = m.group();

            String repl = ucname.length()==1?ucname:(ucname.substring(0,1)+ucname.substring(1).toLowerCase());
            m.appendReplacement(sb, repl);

        }
        return sb.toString();
    }

    public static List<String> canonicaliseNames(Collection<String> names){
        List<String> cnames = new ArrayList<>();
        for(String n: names){
            cnames.add(canonicaliseName(n));
        }
        return cnames;
    }

    public static void main1(String[] args){
        System.err.println(Math.round(18 / Symbol.getNumSymbolTypes()));
    }

    public static void main(String[] args){
        Map<String,Integer> xns = train();
        List<String> peopleNames = new ArrayList<>();
        peopleNames.addAll(ElectionDataParser.getAllPeopleNames());
        peopleNames = canonicaliseNames(peopleNames);
        List<Cluster> clusters = new ArrayList<>();
        int pi = 0, ps = peopleNames.size();

        for(String pn: peopleNames) {
            clusters.add(new Cluster(pn, xns));
        }
        List<Pair<Integer, Integer>> list = new ArrayList<>();
        for(int i=0;i<ps;i++) {
            String pn = peopleNames.get(i);
            double bs = -1;
            int bi = -1;
            for (int j = 0; j < ps; j++) {
                if (i == j)
                    continue;
                Cluster c = clusters.get(j);
                double s = c.getSimilarityScore(pn);
                if (s > bs) {
                    bi = j;
                    bs = s;
                }
            }
            if ((bi >= 0) && (bs>0)) {
                System.err.println(pn + "->" + clusters.get(bi)+", bs: "+bs);
                list.add(new Pair<>(i, bi));
            }else{
                list.add(new Pair<>(i,-1));
            }
            if((++pi%100) == 0)
                System.err.println("Analyzed "+pi+"/"+ps+" names");
        }

        List<Set<String>> cs = new ArrayList<>();
        //mapping from person name index to cluster index
        Map<Integer, Integer> cmap = new LinkedHashMap<>();
        int ci = -1;
        for(Pair<Integer,Integer> p: list) {
            Integer c = cmap.get(p.first);
            if(c == null)
                c = cmap.get(p.second);
            //a new cluster need to be added
            if(c == null){
                ci++;
                cs.add(new LinkedHashSet<String>());
                if(p.first>=0) {
                    cmap.put(p.first, ci);
                    cs.get(ci).add(peopleNames.get(p.first));
                }
                if(p.second>=0) {
                    cmap.put(p.second, ci);
                    cs.get(ci).add(peopleNames.get(p.second));
                }
            } else {
                if(p.first>=0) {
                    cs.get(c).add(peopleNames.get(p.first));
                    cmap.put(p.first, c);
                }
                if(p.second>=0) {
                    cs.get(c).add(peopleNames.get(p.second));
                    cmap.put(p.second, c);
                }
            }
        }
        System.out.println("Total number of names: "+ps+", number of clusters "+ci);
        try{
            FileWriter fw = new FileWriter(new File("data/clusters.txt"));
            for(Set<String> set: cs){
                String line = "";
                for(String str: set)
                    line += str+":::";
                fw.write(line + "\n");
            }
            fw.close();
        } catch(Exception e){
            e.printStackTrace();
        }
    }

    public static void main3(String[] args){
        String name = "Naara Chandra Babu";
        String var = "Nara Chandrababu Naidu";
        Symbol[] syms = getSymbols(name);
        System.err.println("Symbols");
        for(Symbol sym: syms)
            System.err.println(sym);
        System.err.println("Notation for original string: "+getNotationOriginal(name));
        System.err.println("Notation for variant: "+ getNotation(var, syms));
    }
}
