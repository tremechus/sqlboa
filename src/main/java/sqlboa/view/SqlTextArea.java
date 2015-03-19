package sqlboa.view;

import javafx.concurrent.Task;
import org.fxmisc.richtext.*;
import org.reactfx.EventStream;
import org.reactfx.util.Try;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SqlTextArea extends StyleClassedTextArea {

    private static final String[] KEYWORDS = new String[] {
            "select", "from", "where", "insert", "update", "delete", "table", "index", "trigger", "like", "join", "left", "inner"
    };

    private static final String KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORDS) + ")\\b";
    private static final String PAREN_PATTERN = "\\(|\\)";
    private static final String BRACE_PATTERN = "\\{|\\}";
    private static final String BRACKET_PATTERN = "\\[|\\]";
    private static final String STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*'";
    private static final String VARIABLE_PATTERN = ":.*\\b";

    private static final Pattern PATTERN = Pattern.compile(
            "(?<KEYWORD>" + KEYWORD_PATTERN + ")"
                    + "|(?<PAREN>" + PAREN_PATTERN + ")"
                    + "|(?<BRACE>" + BRACE_PATTERN + ")"
                    + "|(?<BRACKET>" + BRACKET_PATTERN + ")"
                    + "|(?<STRING>" + STRING_PATTERN + ")"
                    + "|(?<VARIABLE>"+VARIABLE_PATTERN + ")"
    );

    private static ExecutorService executor;

    public SqlTextArea(String body) {
        super(false);

        getUndoManager().forgetHistory();
        getUndoManager().mark();


        // Set up highlighting
//        setParagraphGraphicFactory(LineNumberFactory.get(this));
        EventStream<PlainTextChange> textChanges = plainTextChanges();
        textChanges
                .successionEnds(Duration.ofMillis(250))
                .supplyTask(this::computeHighlightingAsync)
                .awaitLatest(textChanges)
                .map(Try::get)
                .subscribe(this::applyHighlighting);

        appendText(body);

        // position the caret at the beginning
        selectRange(0, 0);
    }

    public static void start() {
        executor = Executors.newSingleThreadExecutor();
    }

    public static void stop() {
        if (executor != null) {
            executor.shutdown();
            executor = null;
        }
    }

    private Task<StyleSpans<Collection<String>>> computeHighlightingAsync() {
        Task<StyleSpans<Collection<String>>> task = new Task<StyleSpans<Collection<String>>>() {
            @Override
            protected StyleSpans<Collection<String>> call() throws Exception {
                return computeHighlighting(getText());
            }
        };

        if (executor != null) {
            executor.execute(task);
        }

        return task;
    }

    private void applyHighlighting(StyleSpans<Collection<String>> highlighting) {
        setStyleSpans(0, highlighting);
    }

    private static StyleSpans<Collection<String>> computeHighlighting(String text) {
        Matcher matcher = PATTERN.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        while(matcher.find()) {
            String styleClass =
                    matcher.group("KEYWORD") != null ? "keyword" :
                    matcher.group("PAREN") != null ? "paren" :
                    matcher.group("BRACE") != null ? "brace" :
                    matcher.group("BRACKET") != null ? "bracket" :
                    matcher.group("VARIABLE") != null ? "variable" :
                    matcher.group("STRING") != null ? "string" :
                    null; /* never happens */ assert styleClass != null;
            spansBuilder.add(Collections.emptyList(), matcher.start() - lastKwEnd);
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
        }
        spansBuilder.add(Collections.emptyList(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }
}
