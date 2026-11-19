package domain;

import java.util.Objects;

public class AirportCode {
    private final String code;
    private final int line;

    public AirportCode(String code, int line) {
        this.code = Objects.requireNonNull(code);
        this.line = line;
    }

    public String code() { return code; }
    public int line() { return line; }

    public boolean isIATA() { return code.length() == 3; }
    public boolean isICAO() { return code.length() == 4; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AirportCode a)) return false;
        return code.equals(a.code);
    }

    @Override
    public int hashCode() { return code.hashCode(); }

    @Override
    public String toString() { return code; }
}
