package eapli.aisafe.pilot.application.dto;

import java.util.List;

/** Result of the pilot roster listing use case. Transport object — no behaviour. */
public record PilotRosterListResult(List<PilotRosterListItemDTO> pilots, String messageWhenEmpty) {

    public static final String NO_ACTIVE_PILOTS =
            "There are no active pilots in your company's roster.";
}
