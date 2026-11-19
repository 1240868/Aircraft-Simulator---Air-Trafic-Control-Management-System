package semantic;

public record SemanticError(String rule, int line, String message) {
    @Override
    public String toString() {
        return "[Semantic error] (" + rule + ") line " + line + " - " + message;
    }
}
