package parsing;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

import java.util.ArrayList;
import java.util.List;

public class SyntaxErrorListener extends BaseErrorListener {
    public record SyntaxError(int line, int column, String message) {
        @Override
        public String toString() {
            return "[Syntax error] line " + line + ":" + column + " - " + message;
        }
    }

    private final List<SyntaxError> errors = new ArrayList<>();

    @Override
    public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol,
                            int line, int charPositionInLine, String msg,
                            RecognitionException e) {
        errors.add(new SyntaxError(line, charPositionInLine, msg));
    }

    public List<SyntaxError> errors() { return errors; }
    public boolean hasErrors() { return !errors.isEmpty(); }
}
