package dataslurp;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.File;
import java.nio.charset.Charset;
import java.util.*;

/**
 * Parses election data in csv format
 */
public class ElectionDataParser {
    static String DATA_FILE = "data/ElectionData.csv";
    static List<CSVRecord> records = null;
    static Map<String,Integer> headerMap = null;
    static void parse() {
        try {
            File csvData = new File(DATA_FILE);
            //please make sure the header does not contain any repeated header names
            CSVParser parser = CSVParser.parse(csvData, Charset.defaultCharset(), CSVFormat.EXCEL.withHeader().withAllowMissingColumnNames());
            records = parser.getRecords();
            headerMap = parser.getHeaderMap();
        } catch(Exception e){
           e.printStackTrace();
        }
    }

    public static Set<String> getAllPeopleNames(){
        if((records==null)||(headerMap==null))
            parse();
        Set<String> names = new HashSet<>();
       // int l = 0;
        for(CSVRecord rec: records) {
            if(headerMap!=null)
                names.add(rec.get("Name"));
            else
                names.add(rec.get(25));
//            if((l++)>=100)
//                break;
        }
        return names;
    }

    public static void main(String[] args){
        parse();
        CSVRecord rec = records.get(3);
        if(headerMap!=null) {
            System.out.println(rec + "\n---\n" + rec.get("Name")+" -- "+headerMap.get("Name"));
        }
    }
}
