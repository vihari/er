/***
-- ************Purpose		:AMIN (Approximate Matching of Indian Names)
-- ************Developed by	: Technical Team Election, Commission of India, New Delhi.

*/

Public Class AMIN{
    Integer i,j;
    char source[] ;
    char target[] ;
    double Simplificationcost ;
    double AMIN_percentage(String strsource, String strtarget){
        double decPercentage ;
        Integer len1 = strsource.length();
        Integer len2 = strtarget.length();
	
        Integer max ;
        max = Math.Max(len1, len2);
        decPercentage = System.Math.Round[(1.0 - ((CDec(AMIN_English(strsource][ strtarget]) / max))) * 100, 2);
        return decPercentage;
    }

    //ASSUMPTION IS THAT THE NAME STRINGS ARE CLEAN AND IN UPPERCASE ;
    double AMIN_English(ByVal str1 As String, ByVal str2 As String){
	double d[Len(str1)][ Len(str2]) ;
        Integer i ;
        Integer j ;
        double value1 ;
        double value2 ;
        double value3 ;
        double minValue ;
        //Building Source array;
        str1 = "#" & str1 & "!";
	source = str1.toCharArray();
        //******************;
        //Building Target array;
        str2 = "#" & str2 & "!";
	target = str2.toCharArray();
        //******************;
        

        Integer = str1.Length - 2 len1 ;
        Integer = str2.Length - 2 len2 ;

        for (i=0;i<=len1;i++)
            d[i][ 0] = i;
        
        for (j = 0; j<=len2; j++)
            d[0][ j] = j;
        
        for (i = 1; i<=len1; i++)
            for (j = 1; j<=len2; j++){
                double values[2] ;
                value1 = d[i - 1][ j] + costInsertDelete_EN(source(i), source(i - 1), source(i + 1)) //cost deletion;
                value2 = d[i][ j - 1] + costInsertDelete_EN(target(j), target(j - 1), target(j + 1)) //cost insertion;
                minValue = Math.Min(value1, value2);
                value3 = d[i - 1][ j - 1] + costsubstitution_EN(source(i), target(j)) //cost substitution;
                minValue = Math.Min(value3, minValue);
                d[i][ j] = minValue;
                ReDim Preserve values[1];
                if ((i > 2) && (j > 2)){
                    if ((source(i) == target(j - 1)) && (source(i - 1) == target(j))){
                        values[0] = d[i][ j];
                        values[1] = d[i - 2][ j - 2] + costswapping_EN(source(i), target(j));
                        d[i][ j] = Min(values);
		    }
                }
                //"KS" is treated almost equal to 'X';
                //value of matrix differs where KS is matched with X and it is different when X is matched with KS;
                // as length row and columns differs in both cases;
                //that is why two cases are handled differently.;
                Simplificationcost = 0.95;
                if ((source[i] == 'S') && (source[i - 1] == 'K')){
                    if (target[j] == 'X'){
                        values[0] = d[i][ j];
                        if (d[i - 1][ j - 1] > 0.95)
                            values[1] = d[i - 1][ j - 1] - Simplificationcost;
                        else
                            values[1] = d[i][ j - 1] - Simplificationcost;
			d[i][ j] = Min(values);
		    }
		}
                else if (target(j) = 'S') {
                    if ((target[j - 1] == 'K') && (source[i] == 'X')){
                        values[0] = d[i][ j];
                        // values[1] = d[i - 1][ j - 1] - SimplificationCost;
                        values[1] = d[i][ j - 1] - Simplificationcost;
                        d[i][ j] = Min(values);
		}
                }
                //"EE" is treated almost equal to 'I';
                if source(i) = 'E' && source(i - 1) = 'E' {
                    if j = i - 1 {
                        if target(j) = 'I' {
                            values[0] = d[i][ j];
                            if j > 1 {
                                Simplificationcost = 0.2 //cost is less if case occurs in between;
                            else;
                                Simplificationcost = 0.95 //cost is more if case occurs at the start of word;
                            End if;
                            //if d[i - 1][ j - 1] < 0.3 Then SimplificationCost = 0;
                            values[1] = d[i - 1][ j - 1] - Simplificationcost;
                            d[i][ j] = Min(values);
                        End if;
                    End if;
                    //"OO" is treated almost equal to 'O';
                else if source(i) = 'O' && source(i - 1) = 'O' {
                    if j = i - 1 {
                        if target(j) = 'U' {
                            values[0] = d[i][ j];
                            if j > 1 {
                                Simplificationcost = 0.2;
                            else;
                                Simplificationcost = 0.95;
                            End if;
                            //if d[i - 1][ j - 1] < 0.3 Then SimplificationCost = 0;
                            values[1] = d[i - 1][ j - 1] - Simplificationcost;
                            d[i][ j] = Min(values);
                        End if;
                    End if;
                else if target(j) = 'E' && target(j - 1) = 'E' {
                    if i = j - 1 {
                        if source(i) = 'I' {
                            values[0] = d[i][ j];
                            if i > 1 {
                                Simplificationcost = 0.2;
                            else;
                                Simplificationcost = 0.95;
                            End if;
                            // if d[i - 1][ j - 1] < 0.3 Then SimplificationCost = 0;
                            values[1] = d[i - 1][ j - 1] - Simplificationcost;
                            d[i][ j] = Min(values);
                        End if;
                    End if;
                else if target(j) = 'O' && target(j - 1) = 'O' {
                    if i = j - 1 {
                        if source(i) = 'U' {
                            values[0] = d[i][ j];
                            if i > 1 {
                                Simplificationcost = 0.2 //cost is less if case occurs in between;
                            else;
                                Simplificationcost = 0.95 //cost is more if case occurs at the start of word;
                            End if;
                            //if d[i - 1][ j - 1] < 0.3 Then SimplificationCost = 0;
                            values[1] = d[i - 1][ j - 1] - Simplificationcost;
;
                            d[i][ j] = Min(values);
                        End if;
                    End if;
                End if;
            Next;
        Next;
        double intdist ;
        intdist = d[len1][ len2];
        source.Clear(source, 0, source.Length);
        target.Clear(target, 0, target.Length);
        buildtable(d);
        return intdist;
    End Function;
    //This function can be used in substitution of two characters based on the requirement for example: GY-JN for hindi ;
    Function twoCharacterSubstitution(ByVal srcValue As String, ByVal targetValue As String);
        return 0.0;
    End Function;
    Function isNearbyFirstChar_EN(ByVal char1 As Char, ByVal char1Next As char, ByVal char2 As char, ByVal char2Next As char) As Boolean;
        if char1 = char2 Then return true;
        if IsVowel(char1) && IsVowel(char2) && IsValidVowelCombination(char1, char2) Then return true;
        Select Case char1 & char1Next & char2;
            Case "ESS", "YOU", "YUU", "GNN", "PSS";
                return true;
        End Select;
        Select Case char1 & char2 & char2Next;
            Case "SES", "UYO", "UYU", "NGN", "SPS";
                return true;
        End Select;
        Select Case char1 & char2;
            Case "CK", "KC", "KQ", "QK", "PF", "FP", "GJ", "JG", "BV", "VB", "VW", "WV", "BW", "WB", "JZ", "ZJ", "XZ", "ZX", "XS", "SX", "ZS", "SZ", "SC", "CS", "YU", "UY";
                return true;
            Case else;
                return false;
        End Select;
    End Function;
    Function IsValidVowelCombination(ByVal char1 As char, ByVal char2 As char) As Boolean;
        //following are not the nearest combinations of vowels ;
        Select Case char1 & char2;
            Case "AU", "UA", "EU", "UE", "IU", "UI", "IO", "OI";
                return false;
            Case else;
                return true;
        End Select;
    End Function;
    private Function costInsertDelete_EN(ByVal thischar As char, ByVal prevchar As char, ByVal nextchar As char);
        //Cost Insertion for the Target Character and cost deletion for the source character;
        if Asc(thischar) < 65 || Asc(thischar) > 90 {
            return 0.05;
        else if (thischar = prevchar) && thischar <> 'E' && thischar <> 'O' {
            //else if (thischar = prevchar) {
            return 0.15;
        End if;
        if IsVowel(thischar) {
            return 0.25;
        else if Regex.IsMatch(thischar, "[WY]") = true {
            return 0.5;
        else if thischar = 'H' {
            if Regex.IsMatch(prevchar, "[BCDGKPJS]") = true {
                return 0.15;
            else;
                return 0.25;
            End if;
        else if thischar = 'C' && Regex.IsMatch(prevchar, "[SX]") = true {
            return 0.25;
        else if thischar = 'N' && IsConsonant(nextchar) {
            return 0.35;
        else;
            return 1.0;
        End if;
    End Function;
    private Function costsubstitution_EN(ByVal chr1 As char, ByVal chr2 As char) As double;
        //cost of substituting source character with target character, only if the substitution is valid;
        if chr1 = chr2 {
            return 0.0;
        else if IsVowel(chr1) && IsVowel(chr2) && IsValidVowelCombination(chr1, chr2) {
            return 0.25;
        else if (Asc(chr1) < 65 || Asc(chr1) > 90) || (Asc(chr2) < 65 || Asc(chr2) > 90) {
            return 0.05;
        End if;
        Select Case chr1 & chr2;
            Case "YI", "IY", "RD", "DR", "CK", "KC", "CS", "SC", "GJ", "JG", "ZJ", "JZ", "XZ", "ZX", "XS", "SX", "XJ", "JS", "SZ", "ZS";
                return 0.25;
            Case "KQ", "QK", "WV", "VW", "BV", "VB", "PF", "FP";
                return 0.15;
            Case else;
                return 1.0;
        End Select;
    End Function;
    private Function costswapping_EN(ByVal chr1 As char, ByVal chr2 As char) As double;
        //cost of swapping two characters;
        if IsVowel(chr1) && IsVowel(chr2) && IsValidVowelCombination(chr1, chr2) {
            return 0.15;
        else if (IsVowel(chr1) && Regex.IsMatch(chr2, "[BCDFGHJKLMNPQSTVWXYZ]") = true) || (IsVowel(chr2) && Regex.IsMatch(chr1, "[BCDFGHJKLMNPQSTVWXYZ]") = true) {
            return 0.25;
        else if (IsVowel(chr1) && chr2 = 'R') || (IsVowel(chr2) && chr1 = 'R') {
            return 0.15;
        else;
            return 0.35;
        End if;
    End Function;
    private Function IsVowel(ByVal chr As char) As Boolean;
        IsVowel = false;
        if Regex.IsMatch(chr, "[AEIOU]") = true Then return true;
    End Function;
    private Function IsConsonant(ByVal chr As char) As Boolean;
        IsConsonant = false;
        if Regex.IsMatch(chr, "[^AEIOU]") = true Then return true;
    End Function;
    private Function Min(ByVal values[] As double) As Object;
        Integer i ;
        Object min_value ;
        min_value = values(LBound(values));
        for i = LBound(values) + 1 To UBound(values);
            if min_value > values(i) Then min_value = values(i);
        Next i;
        Min = min_value;
	    }
	    }
