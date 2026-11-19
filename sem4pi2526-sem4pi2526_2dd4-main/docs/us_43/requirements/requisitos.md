# US043 — Consult Weather Data

## 1. User Story

> **As a Weather Person, a Pilot, or a Flight Control Operator, I want to consult weather data in the system on a given day and in a specific air control area.**

---

## 2. Context & Scope

This User Story belongs to the **Weather Service** bounded context. It provides read-only access to weather observations previously registered through **US041** or imported through **US042**.

The consultation is filtered by:

- **Air control area** — the geographic region of interest;
- **Day** — calendar date for which observations are required.

The feature supports operational decision-making by Pilots and Flight Control Operators, and verification by Weather Persons.

---

## 3. Actors

| Actor | Role |
|-------|------|
| **Weather Person** | Consults registered/imported weather data for verification |
| **Pilot** | Consults weather data relevant to flight planning and validation |
| **Flight Control Operator** | Consults weather data for area simulation and monitoring |
| **System** | Retrieves and presents matching weather records |

---

## 4. Preconditions

| # | Precondition |
|---|-------------|
| PC1 | The actor must be authenticated and authorised in the system |
| PC2 | The requested **air control area** must exist (US050) |
| PC3 | The requested **date** must be a valid calendar date |
| PC4 | Weather data may or may not exist for the given area and date |

---

## 5. Postconditions

| # | Postcondition |
|---|--------------|
| PS1 | The system returns all weather observations matching the area and date |
| PS2 | If no data exists, the system informs the actor clearly (empty result, not an error) |
| PS3 | Results are ordered chronologically by observation timestamp |
| PS4 | No weather data is modified during consultation |

---

## 6. Main Flow (Basic Course of Events)

1. Actor selects the option to consult weather data.
2. System requests the air control area code and the target date.
3. Actor provides the area code and date.
4. System validates the area code format and confirms the area exists.
5. System queries weather records for the area within the selected calendar day (00:00–23:59 UTC).
6. System presents the list of matching observations (timestamp, temperature, humidity, wind, source).
7. Actor reviews the results.

---

## 7. Alternative Flows

### AF1 — Unknown air control area
- At step 4, if the area does not exist, the system presents an error and terminates the use case.

### AF2 — Invalid date
- At step 3, if the date format is invalid, the system presents an error and returns to step 2.

### AF3 — No data found
- At step 6, if no records match the criteria, the system informs the actor that no weather data is available for that area and date.

### AF4 — Unauthorised actor
- At step 1, if the actor lacks a permitted role, the system denies access.

---

## 8. Acceptance Criteria

| ID | Criterion |
|----|-----------|
| AC1 | Users with roles **WEATHER_PERSON**, **PILOT** or **FLIGHT_CONTROL_OPERATOR** may consult weather data |
| AC2 | Consultation requires an air control area code and a calendar date |
| AC3 | Results include all observations whose `observedAt` falls on the selected UTC day for the given area |
| AC4 | Each result displays at minimum: observation time, temperature, humidity, wind speed, wind direction and source |
| AC5 | Results are sorted by `observedAt` ascending |
| AC6 | Unknown area codes produce a clear error message |
| AC7 | An empty result set is not treated as a system failure |
| AC8 | The operation is read-only — no aggregate is modified |
| AC9 | Authentication and authorisation are enforced before query execution |

---

## 9. Business Rules

| ID | Rule |
|----|------|
| BR1 | A calendar day is interpreted in UTC: `[dateT00:00, dateT23:59:59.999]` |
| BR2 | Area codes are normalised (trim, uppercase) before lookup |
| BR3 | Only weather data linked to the requested area is returned |
| BR4 | Consultation does not expose data from unrelated air control areas |
| BR5 | Pilots and FCOs may consult any area; row-level authorisation by area membership is not required at this stage |

---

## 10. Related User Stories

| US | Relationship |
|----|-------------|
| US041 | Register weather data — data source |
| US042 | Import bulk weather data — data source |
| US050 | Register air control area — prerequisite |
| US082 | Insert weather data in a flight — consumes consulted data |
| US110 | Integrate environmental influences into simulation — downstream consumer |
| US030 | Authentication and authorisation — cross-cutting prerequisite |

---

## 11. Non-Functional Requirements Applicable

| NFR | Relevance |
|-----|-----------|
| NFR03 | Unit test coverage ≥ 90% for domain and controller packages |
| NFR08 | Persistence must be configurable (in-memory / relational RDBMS) |
| NFR09 | Authentication and authorisation must be enforced |
| NFR10 | Implementation language: Java |

---

## 12. Out of Scope for this US

- Registering or importing weather data (US041, US042)
- Associating weather data with flight plans (US082)
- Weather forecast computation or AI-enhanced predictions
- Map/visual weather presentation
- Remote TCP client access (US44)
