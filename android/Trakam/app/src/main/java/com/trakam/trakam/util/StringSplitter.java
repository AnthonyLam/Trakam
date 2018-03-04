package com.trakam.trakam.util;

public class StringSplitter {

    private static final int DEFAULT_EXPECTED_NUMBER_OF_TOKENS = 8;

    public static String[] splitOnEmptySequence(String s) {
        return split(s, " ");
    }

    public static String[] splitOnEmptySequence(String s, int expectedNumberOfSubSequences) {
        return split(s, " ", expectedNumberOfSubSequences);
    }

    public static String[] split(String s, String delimiter) {
        return split(s, delimiter, DEFAULT_EXPECTED_NUMBER_OF_TOKENS);
    }

    public static String[] split(String s, String delimiter, int expectedNumberOfSubSequences) {
        if (s == null || s.isEmpty() || (delimiter != null && delimiter.length() > s.length())) {
            return new String[]{};
        }

        if (delimiter == null) {
            throw new IllegalStateException("delimiter is null");
        }

        if (delimiter.isEmpty()) {
            return new String[]{s};
        }

        if (expectedNumberOfSubSequences < 1) {
            throw new IllegalStateException("expectedNumberOfSubSequences is less than 1");
        }

        return fastSplit(s, delimiter, expectedNumberOfSubSequences);
    }

    private static String[] fastSplit(String s, String delimiter, int expectedNumberOfSubSequences) {
        int tokenLen = expectedNumberOfSubSequences;
        String[] tokens = new String[tokenLen];

        int count = 0;
        int subStringStartIndex = 0;

        final int delimN = delimiter.length();
        final int N = s.length();

        int i = 0;
        while (i < N) {
            final int subStringEndIndex = i;

            int matchCount = 0;
            int j = 0;
            int k = i;
            while (k < N) {
                final char sk = s.charAt(k);
                final char dj = delimiter.charAt(j);
                if (sk != dj && (sk != '\t' || dj != ' ')) {
                    break;
                }

                j++;
                k++;

                if (j == delimN) {
                    j = 0;
                    matchCount += delimN;
                }
            }

            // match
            if (matchCount > 0 && k - i == matchCount) {
                // make sure the match isn't the beginning of the string
                if (k != matchCount) {
                    if (count == tokenLen) {
                        tokens = growTwiceTheSize(tokens);
                        tokenLen = tokens.length;
                    }
                    tokens[count] = s.substring(subStringStartIndex, subStringEndIndex);
                    count++;
                }

                i = k;
                subStringStartIndex = i;
            } else {
                i++;
            }
        }

        if (subStringStartIndex < N) {
            if (count == tokenLen) {
                tokens = growTwiceTheSize(tokens);
                tokenLen = tokens.length;
            }
            tokens[count] = s.substring(subStringStartIndex, N);
            count++;
        }

        final String[] result;
        if (count == tokenLen) {
            result = tokens;
        } else {
            result = new String[count];
            System.arraycopy(tokens, 0, result, 0, count);
        }

        return result;
    }

    private static String[] growTwiceTheSize(String[] tokens) {
        final int N = tokens.length;
        final String[] temp = new String[N * 2];
        System.arraycopy(tokens, 0, temp, 0, N);
        return temp;
    }
}
