import java.io.*;
import java.util.ArrayList;

/**
 *
 * [Add your documentation here]
 *
 * @author Austin Barrow
 * @author Jacob Sandefur
 * @version 4/27/2020
 */

public class ChatFilter {

    ArrayList<String> badWords = new ArrayList<>();

    public ChatFilter(String badWordsFileName) throws IOException {

        File f = new File( badWordsFileName );
        FileReader fr = new FileReader( f );
        BufferedReader bfr = new BufferedReader(fr);

        while (true) {
            String line = bfr.readLine();
            if (line == null) {
                break;
            }

            badWords.add( line );
        }

    }

    public String filter(String msg) {
        String replacement;
        for ( String badWord: badWords ) {
            replacement = "*".repeat( badWord.length() );
            msg = msg.replaceAll( badWord, replacement );
        }

        return msg;
    }
}
