package mrow4a.spark.java.alg;

import java.util.HashSet;
import java.util.Collection;

import static java.lang.Math.abs;

public final class Shingling {

    /**
     * Convert text to set of shingles (hashed strings). Shingling is sliding text window moving over text,
     * and window length is shingle length
     *
     * @param text text to be shingled
     * @param shingleLength shingle length
     * @return HashSet<Integer>
     */
    public static Collection<Integer> getShingles(String text, Integer shingleLength) {
        HashSet<Integer> shingles = new HashSet<>();

        // Convert the text to char array
        char[] charArray = text.toCharArray();

        // Implement sliding window over chars
        for (int i = 0; i < charArray.length - shingleLength; i++) {
            // Use hash code and absolute value to be sure of positive integers
            // Construct string of length shingleLenght starting from offset i, out of whole text
            shingles.add(abs((new String(charArray, i, shingleLength)).hashCode()));
        }
        return shingles;
    }
}
