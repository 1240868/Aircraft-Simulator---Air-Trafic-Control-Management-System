#ifndef FLIGHT_VALIDATOR_H
#define FLIGHT_VALIDATOR_H

/**
 * US085 — standalone flight plan physics validation (fuel / segment altitudes).
 * Input: JSON file produced by Java FlightPlanTestPayloadBuilder.
 * minFuelKg reflects destination air control area regulations.
 * Output: stdout lines "PASS" or "FAIL|code|message"; exit 0 pass, 1 fail, 2 input error.
 */
int run_flight_plan_test(const char *json_path);

#endif
