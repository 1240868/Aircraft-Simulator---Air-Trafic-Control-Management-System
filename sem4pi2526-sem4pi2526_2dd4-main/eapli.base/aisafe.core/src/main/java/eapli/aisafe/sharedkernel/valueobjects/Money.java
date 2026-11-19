package eapli.aisafe.sharedkernel.valueobjects;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.util.Currency;

@Embeddable
public class Money implements ValueObject {
    private BigDecimal amount;
    private String currency;

    protected Money() {
    }

    public Money(final BigDecimal amount, final Currency currency) {
        Preconditions.nonNull(amount, "Amount is required");
        Preconditions.nonNull(currency, "Currency is required");
        Preconditions.ensure(amount.compareTo(BigDecimal.ZERO) >= 0, "Amount must be non-negative");
        this.amount = amount;
        this.currency = currency.getCurrencyCode();
    }

    public BigDecimal amount() { return amount; }
    public String currency() { return currency; }
}
