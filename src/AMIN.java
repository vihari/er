/***
-- ************Purpose		:AMIN (Approximate Matching of Indian Names)
-- ************Developed by	: Technical Team Election, Commission of India, New Delhi.

*/

public class AMIN{
    double AMIN_percentage(String strsource, String strtarget){
        double decPercentage ;
        Integer len1 = strsource.length();
        Integer len2 = strtarget.length();

        Integer max ;
        max = Math.max(len1, len2);
        decPercentage = Math.round((1.0 - ((((double)AMIN_English(strsource, strtarget))/max))) * 100);
        return decPercentage;
    }

    //ASSUMPTION IS THAT THE NAME STRINGS ARE CLEAN AND IN UPPERCASE ;
    public static double AMIN_English(String str1, String str2) {
        Integer i,j;
        char source[] ;
        char target[] ;
        double Simplificationcost ;

        double d[][] = new double[str1.length() + 1][str2.length() + 1];
        double value1;
        double value2;
        double value3;
        double minValue;
        //Building Source array;
        str1 = "#" + str1 + "!";
        source = str1.toCharArray();
        //******************;
        //Building Target array;
        str2 = "#" + str2 + "!";
        target = str2.toCharArray();
        //******************;


        Integer len1 = str1.length() - 2;
        Integer len2 = str2.length() - 2;

        for (i = 0; i <= len1; i++)
            d[i][0] = i;

        for (j = 0; j <= len2; j++)
            d[0][j] = j;

        for (i = 1; i <= len1; i++)
            for (j = 1; j <= len2; j++) {
                double values[] = new double[2];
                value1 = d[i - 1][j] + costInsertDelete_EN(source[i], source[i-1], source[i + 1]); //cost deletion;
                value2 = d[i][j - 1] + costInsertDelete_EN(target[j], target[j - 1], target[j + 1]); //cost insertion;
                minValue = Math.min(value1, value2);
                value3 = d[i - 1][j - 1] + costsubstitution_EN(source[i], target[j]); //cost substitution;
                minValue = Math.min(value3, minValue);
                d[i][j] = minValue;
                if ((i > 2) && (j > 2)) {
                    if ((source[i] == target[j - 1]) && (source[i - 1] == target[j])) {
                        values[0] = d[i][j];
                        values[1] = d[i - 2][j - 2] + costswapping_EN(source[i], target[j]);
                        d[i][j] = Math.min(values[0], values[1]);
                    }
                }
                //"KS" is treated almost equal to 'X';
                //value of matrix differs where KS is matched with X and it is different when X is matched with KS;
                // as length row and columns differs in both cases;
                //that is why two cases are handled differently.;
                Simplificationcost = 0.95;
                if ((source[i] == 'S') && (source[i - 1] == 'K')) {
                    if (target[j] == 'X') {
                        values[0] = d[i][j];
                        if (d[i - 1][j - 1] > 0.95)
                            values[1] = d[i - 1][j - 1] - Simplificationcost;
                        else
                            values[1] = d[i][j - 1] - Simplificationcost;
                        d[i][j] = Math.min(values[0], values[1]);
                    }
                } else if (target[j] == 'S') {
                    if ((target[j - 1] == 'K') && (source[i] == 'X')) {
                        values[0] = d[i][j];
                        // values[1] = d[i - 1][ j - 1] - SimplificationCost;
                        values[1] = d[i][j - 1] - Simplificationcost;
                        d[i][j] = Math.min(values[0], values[1]);
                    }
                }
                //"EE" is treated almost equal to 'I';
                if ((source[i] == 'E') && (source[i - 1] == 'E')) {
                    if (j == (i - 1)) {
                        if (target[j] == 'I') {
                            values[0] = d[i][j];
                            if (j > 1)
                                Simplificationcost = 0.2; //cost is less if case occurs in between;
                            else
                                Simplificationcost = 0.95; //cost is more if case occurs at the start of word;

                            //if d[i - 1][ j - 1] < 0.3 Then SimplificationCost = 0;
                            values[1] = d[i - 1][j - 1] - Simplificationcost;
                            d[i][j] = Math.min(values[0], values[1]);
                        }
                    }
                }
                //"OO" is treated almost equal to 'O';
                else if ((source[i] == 'O') && (source[i - 1] == 'O')) {
                    if (j == (i - 1)) {
                        if (target[j] == 'U') {
                            values[0] = d[i][j];
                            if (j > 1)
                                Simplificationcost = 0.2;
                            else
                                Simplificationcost = 0.95;
                            //if d[i - 1][ j - 1] < 0.3 Then SimplificationCost = 0;
                            values[1] = d[i - 1][j - 1] - Simplificationcost;
                            d[i][j] = Math.min(values[0], values[1]);
                        }
                    }
                } else if ((target[j] == 'E') && (target[j - 1] == 'E')) {
                    if (i == (j - 1)) {
                        if (source[i] == 'I') {
                            values[0] = d[i][j];
                            if (i > 1)
                                Simplificationcost = 0.2;
                            else
                                Simplificationcost = 0.95;
                            // if d[i - 1][ j - 1] < 0.3 Then SimplificationCost = 0;
                            values[1] = d[i - 1][j - 1] - Simplificationcost;
                            d[i][j] = Math.min(values[0], values[1]);
                        }
                    }
                } else if ((target[j] == 'O') && (target[j - 1] == 'O')) {
                    if (i == (j - 1)) {
                        if (source[i] == 'U') {
                            values[0] = d[i][j];
                            if (i > 1)
                                Simplificationcost = 0.2; //cost is less if case occurs in between;
                            else
                                Simplificationcost = 0.95; //cost is more if case occurs at the start of word;

                            //if d[i - 1][ j - 1] < 0.3 Then SimplificationCost = 0;
                            values[1] = d[i - 1][j - 1] - Simplificationcost;
                            ;
                            d[i][j] = Math.min(values[0], values[1]);
                        }
                    }
                }
            }
        return d[len1][len2];
    }

    //This function can be used in substitution of two characters based on the requirement for example: GY-JN for hindi ;
    double twoCharacterSubstitution(String srcValue, String targetValue) {
        return 0.0;
    }

    boolean isNearbyFirstChar_EN(char char1, char char1Next, char char2, char char2Next) {
        if (char1 == char2) return true;
        if (IsVowel(char1) && IsVowel(char2) && IsValidVowelCombination(char1, char2)) return true;
        switch ("" + char1 + char1Next + char2) {
            case "ESS":
            case "YOU":
            case "YUU":
            case "GNN":
            case "PSS":
                return true;
        }
        switch ("" + char1 + char2 + char2Next) {
            case "SES":
            case "UYO":
            case "UYU":
            case "NGN":
            case "SPS":
                return true;
        }
        switch ("" + char1 + char2) {
            case "CK":
            case "KC":
            case "KQ":
            case "QK":
            case "PF":
            case "FP":
            case "GJ":
            case "JG":
            case "BV":
            case "VB":
            case "VW":
            case "WV":
            case "BW":
            case "WB":
            case "JZ":
            case "ZJ":
            case "XZ":
            case "ZX":
            case "XS":
            case "SX":
            case "ZS":
            case "SZ":
            case "SC":
            case "CS":
            case "YU":
            case "UY":
                return true;
            default:
                return false;
        }
    }

    static boolean IsValidVowelCombination(char char1, char char2) {
        //following are not the nearest combinations of vowels ;
        switch ("" + char1 + char2) {
            case "AU":
            case "UA":
            case "EU":
            case "UE":
            case "IU":
            case "UI":
            case "IO":
            case "OI":
                return false;
            default:
                return true;
        }
    }

    private static double costInsertDelete_EN(char thischar, char prevchar, char nextchar) {
        //Cost Insertion for the Target Character and cost deletion for the source character;
        if ((thischar < 65) || (thischar > 90))
            return 0.05;
        else if ((thischar == prevchar) && (thischar != 'E') && (thischar != 'O'))
            //else if (thischar = prevchar) {
            return 0.15;

        if (IsVowel(thischar))
            return 0.25;
        else if ((thischar + "").matches("[WY]"))
            return 0.5;
        else if (thischar == 'H') {
            if ((prevchar + "").matches("[BCDGKPJS]"))
                return 0.15;
            else
                return 0.25;
        } else if ((thischar == 'C') && (prevchar + "").matches("[SX]"))
            return 0.25;
        else if ((thischar == 'N') && IsConsonant(nextchar))
            return 0.35;
        else
            return 1.0;
    }

    private static double costsubstitution_EN(char chr1, char chr2) {
        //cost of substituting source character with target character, only if the substitution is valid;
        if (chr1 == chr2)
            return 0.0;
        else if (IsVowel(chr1) && IsVowel(chr2) && IsValidVowelCombination(chr1, chr2))
            return 0.25;
        else if (((chr1 < 65) || (chr1 > 90)) || ((chr2 < 65) || (chr2 > 90)))
            return 0.05;

        switch ("" + chr1 + chr2) {
            case "YI":
            case "IY":
            case "RD":
            case "DR":
            case "CK":
            case "KC":
            case "CS":
            case "SC":
            case "GJ":
            case "JG":
            case "ZJ":
            case "JZ":
            case "XZ":
            case "ZX":
            case "XS":
            case "SX":
            case "XJ":
            case "JS":
            case "SZ":
            case "ZS":
                return 0.25;
            case "KQ":
            case "QK":
            case "WV":
            case "VW":
            case "BV":
            case "VB":
            case "PF":
            case "FP":
                return 0.15;
            default:
                return 1.0;
        }
    }

    private static double costswapping_EN(char chr1, char chr2) {
        //cost of swapping two characters;
        if (IsVowel(chr1) && IsVowel(chr2) && IsValidVowelCombination(chr1, chr2))
            return 0.15;
        else if ((IsVowel(chr1) && (chr2 + "").matches("[BCDFGHJKLMNPQSTVWXYZ]")) || (IsVowel(chr2) && (chr1 + "").matches("[BCDFGHJKLMNPQSTVWXYZ]")))
            return 0.25;
        else if ((IsVowel(chr1) && (chr2 == 'R')) || (IsVowel(chr2) && (chr1 == 'R')))
            return 0.15;
        else
            return 0.35;

    }

    private static boolean IsVowel(char chr) {
        return (chr + "").matches("[AEIOU]");
    }

    private static boolean IsConsonant(char chr) {
        return (chr + "").matches("[^AEIOU]");
    }

    public static void main(String[] args){
        AMIN amin = new AMIN();
        String str1 = "Yaddyurappa";
        String str2 = "Yadiurapa";
        double d = amin.AMIN_English(str1, str2);
        System.out.println("Penalty for (" + str1 + "," + str2 + ") -> " + d);
    }
}