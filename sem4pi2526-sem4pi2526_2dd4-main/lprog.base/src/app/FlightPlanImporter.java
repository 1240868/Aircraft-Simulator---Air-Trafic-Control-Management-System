package app;

import domain.FlightPlan;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import parsing.FlightDSLParser;
import parsing.FlightDSLLexer;
import parsing.FlightPlanBuilder;
import parsing.Listener;
import parsing.SyntaxErrorListener;
import semantic.SemanticError;
import semantic.SemanticValidator;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class FlightPlanImporter {

    private final SemanticValidator semanticValidator = new SemanticValidator();

    public ImportResult importFromFile(Path file) throws IOException {
        CharStream input = CharStreams.fromPath(file);
        return importFromStream(input);
    }

    public ImportResult importFromString(String source) {
        CharStream input = CharStreams.fromString(source);
        return importFromStream(input);
    }

    private ImportResult importFromStream(CharStream input) {
        FlightDSLLexer lexer = new FlightDSLLexer(input);
        SyntaxErrorListener errorListener = new SyntaxErrorListener();
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);

        CommonTokenStream tokens = new CommonTokenStream(lexer);
        FlightDSLParser parser = new FlightDSLParser(tokens);
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);

        FlightDSLParser.FlightPlanContext tree = parser.flightPlan();

        if (errorListener.hasErrors()) {
            return ImportResult.syntaxFailure(errorListener.errors());
        }

        ParseTreeWalker.DEFAULT.walk(new Listener(), tree);

        FlightPlanBuilder builder = new FlightPlanBuilder();
        FlightPlan plan = builder.visitFlightPlan(tree);

        List<SemanticError> semanticErrors = semanticValidator.validate(plan);
        if (!semanticErrors.isEmpty()) {
            return ImportResult.semanticFailure(semanticErrors);
        }

        return ImportResult.ok(plan);
    }
}
