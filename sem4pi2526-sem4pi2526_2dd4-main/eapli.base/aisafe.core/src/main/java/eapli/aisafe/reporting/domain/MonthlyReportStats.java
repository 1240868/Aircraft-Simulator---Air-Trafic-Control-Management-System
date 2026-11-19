package eapli.aisafe.reporting.domain;

import eapli.framework.domain.model.ValueObject;
import eapli.framework.validations.Preconditions;

public final class MonthlyReportStats implements ValueObject {

    private final int total;
    private final int approved;
    private final int rejected;
    private final int draft;
    private final int submitted;
    private final int completed;
    private final int cancelled;
    private final int active;
    private final int pendingTest;
    private final int tested;

    private final int commercial;
    private final int cargo;
    private final int charter;
    private final int privateFlights;
    private final int military;
    private final int emergency;

    private final int distinctPilots;
    private final int distinctAircraft;

    private MonthlyReportStats(final Builder b) {
        Preconditions.ensure(b.total >= 0, "Total must be non-negative");
        Preconditions.ensure(b.approved >= 0, "Approved must be non-negative");
        Preconditions.ensure(b.rejected >= 0, "Rejected must be non-negative");
        Preconditions.ensure(b.draft >= 0, "Draft must be non-negative");
        Preconditions.ensure(b.submitted >= 0, "Submitted must be non-negative");
        Preconditions.ensure(b.completed >= 0, "Completed must be non-negative");
        Preconditions.ensure(b.cancelled >= 0, "Cancelled must be non-negative");
        Preconditions.ensure(b.active >= 0, "Active must be non-negative");
        Preconditions.ensure(b.pendingTest >= 0, "PendingTest must be non-negative");
        Preconditions.ensure(b.tested >= 0, "Tested must be non-negative");
        Preconditions.ensure(b.commercial >= 0, "Commercial must be non-negative");
        Preconditions.ensure(b.cargo >= 0, "Cargo must be non-negative");
        Preconditions.ensure(b.charter >= 0, "Charter must be non-negative");
        Preconditions.ensure(b.privateFlights >= 0, "Private must be non-negative");
        Preconditions.ensure(b.military >= 0, "Military must be non-negative");
        Preconditions.ensure(b.emergency >= 0, "Emergency must be non-negative");
        Preconditions.ensure(b.distinctPilots >= 0, "DistinctPilots must be non-negative");
        Preconditions.ensure(b.distinctAircraft >= 0, "DistinctAircraft must be non-negative");
        this.total = b.total;
        this.approved = b.approved;
        this.rejected = b.rejected;
        this.draft = b.draft;
        this.submitted = b.submitted;
        this.completed = b.completed;
        this.cancelled = b.cancelled;
        this.active = b.active;
        this.pendingTest = b.pendingTest;
        this.tested = b.tested;
        this.commercial = b.commercial;
        this.cargo = b.cargo;
        this.charter = b.charter;
        this.privateFlights = b.privateFlights;
        this.military = b.military;
        this.emergency = b.emergency;
        this.distinctPilots = b.distinctPilots;
        this.distinctAircraft = b.distinctAircraft;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private int total, approved, rejected, draft, submitted, completed,
                cancelled, active, pendingTest, tested;
        private int commercial, cargo, charter, privateFlights, military, emergency;
        private int distinctPilots, distinctAircraft;

        public Builder total(final int v)          { total = v;          return this; }
        public Builder approved(final int v)       { approved = v;       return this; }
        public Builder rejected(final int v)       { rejected = v;       return this; }
        public Builder draft(final int v)          { draft = v;          return this; }
        public Builder submitted(final int v)      { submitted = v;      return this; }
        public Builder completed(final int v)      { completed = v;      return this; }
        public Builder cancelled(final int v)      { cancelled = v;      return this; }
        public Builder active(final int v)         { active = v;         return this; }
        public Builder pendingTest(final int v)    { pendingTest = v;    return this; }
        public Builder tested(final int v)         { tested = v;         return this; }
        public Builder commercial(final int v)     { commercial = v;     return this; }
        public Builder cargo(final int v)          { cargo = v;          return this; }
        public Builder charter(final int v)        { charter = v;        return this; }
        public Builder privateFlights(final int v) { privateFlights = v; return this; }
        public Builder military(final int v)       { military = v;       return this; }
        public Builder emergency(final int v)      { emergency = v;      return this; }
        public Builder distinctPilots(final int v) { distinctPilots = v; return this; }
        public Builder distinctAircraft(final int v) { distinctAircraft = v; return this; }

        public MonthlyReportStats build() {
            return new MonthlyReportStats(this);
        }
    }

    public int totalFlightPlans()   { return total; }
    public int approvedFlightPlans() { return approved; }
    public int rejectedFlightPlans() { return rejected; }
    public int draftFlightPlans()   { return draft; }
    public int submittedFlightPlans() { return submitted; }
    public int completedFlightPlans() { return completed; }
    public int cancelledFlightPlans() { return cancelled; }
    public int activeFlightPlans()  { return active; }
    public int pendingTestFlightPlans() { return pendingTest; }
    public int testedFlightPlans()  { return tested; }
    public int commercialFlights()  { return commercial; }
    public int cargoFlights()       { return cargo; }
    public int charterFlights()     { return charter; }
    public int privateFlights()     { return privateFlights; }
    public int militaryFlights()    { return military; }
    public int emergencyFlights()   { return emergency; }
    public int distinctPilots()     { return distinctPilots; }
    public int distinctAircraft()   { return distinctAircraft; }
}
