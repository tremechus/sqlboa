package sqlboa.util;

/**
 * Created by trevor on 3/3/2015.
 */
public class StringUtil {

    public static String findCurrentWord(String text, int position) {
        int start = position;
        int end = start + 1;

        // Find front
        while (start > 0 && !Character.isWhitespace(text.charAt(start))) {
            start --;
        }

        // Find end
        while (end < text.length() && !Character.isWhitespace(text.charAt(end))) {
            end ++;
        }

        return text.substring(start, end).trim();
    }

}
