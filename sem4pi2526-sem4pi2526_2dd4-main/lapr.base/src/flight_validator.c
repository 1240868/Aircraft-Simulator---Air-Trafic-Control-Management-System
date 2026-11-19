#include <ctype.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/stat.h>

#include "flight_validator.h"

static const char *skip_ws(const char *p) {
    while (p && *p && isspace((unsigned char) *p)) {
        p++;
    }
    return p;
}

static char *read_payload_file(const char *filename) {
    FILE *file = fopen(filename, "rb");
    if (file == NULL) {
        perror("Erro ao abrir o ficheiro");
        return NULL;
    }

    struct stat st;
    if (fstat(fileno(file), &st) != 0) {
        perror("Erro ao obter tamanho do ficheiro");
        fclose(file);
        return NULL;
    }

    if (st.st_size < 0) {
        fclose(file);
        return NULL;
    }

    char *buffer = (char *) malloc((size_t) st.st_size + 1);
    if (buffer == NULL) {
        perror("Erro ao alocar memoria");
        fclose(file);
        return NULL;
    }

    size_t n = fread(buffer, 1, (size_t) st.st_size, file);
    buffer[n] = '\0';
    fclose(file);
    return buffer;
}

static char *find_after_key(const char *json, const char *key) {
    const char *pos = strstr(json, key);
    if (!pos) {
        return NULL;
    }

    pos += strlen(key);
    pos = skip_ws(pos);
    if (*pos == ':') {
        pos++;
    }
    pos = skip_ws(pos);
    if (*pos == '"') {
        pos++;
    }

    return (char *) pos;
}

static double extract_number(const char *json, const char *key) {
    char *pos = find_after_key(json, key);
    if (!pos) {
        return 0.0;
    }
    return strtod(pos, NULL);
}

static int validate_fuel_on_board(const char *json) {
    const double fuel = extract_number(json, "\"fuelOnBoardKg\"");
    if (fuel <= 0.0) {
        printf("FAIL|FUEL_INVALID|fuelOnBoardKg must be positive\n");
        return 1;
    }

    const double reserve = extract_number(json, "\"reserveFuelKg\"");
    const double min_dest = extract_number(json, "\"minFuelKg\"");
    const double remaining = fuel - reserve;

    if (remaining < min_dest) {
        printf("FAIL|FUEL_INSUFFICIENT|remaining=%.2f|required=%.2f\n",
               remaining, min_dest);
        return 1;
    }
    return 0;
}

static int validate_segment_altitudes(const char *json) {
    const double service_ceiling = extract_number(json, "\"serviceCeilingM\"");
    const double min_alt = extract_number(json, "\"minAltitudeM\"");

    const char *key = "\"segmentAltitudesM\"";
    const char *pos = strstr(json, key);
    if (!pos) {
        printf("FAIL|ALTITUDE_DATA_MISSING|segmentAltitudesM not found\n");
        return 1;
    }

    pos = strchr(pos, '[');
    if (!pos) {
        printf("FAIL|ALTITUDE_DATA_MISSING|segmentAltitudesM array not found\n");
        return 1;
    }
    pos++;

    int segment = 0;
    int errors = 0;

    while (*pos && *pos != ']') {
        while (*pos == ' ' || *pos == ',' || *pos == '\n' || *pos == '\r') {
            pos++;
        }
        if (*pos == ']') {
            break;
        }

        const double cruise_alt = strtod(pos, (char **) &pos);
        segment++;

        if (service_ceiling > 0.0 && cruise_alt > service_ceiling) {
            printf("FAIL|ALTITUDE_ABOVE_CEILING|segment=%d|cruise=%.2f|ceiling=%.2f\n",
                   segment, cruise_alt, service_ceiling);
            errors++;
        }

        if (min_alt > 0.0 && cruise_alt < min_alt) {
            printf("FAIL|ALTITUDE_BELOW_MIN|segment=%d|cruise=%.2f|min=%.2f\n",
                   segment, cruise_alt, min_alt);
            errors++;
        }
    }

    if (segment == 0) {
        printf("FAIL|ALTITUDE_DATA_MISSING|no segment altitudes in payload\n");
        return 1;
    }

    return errors;
}

int run_flight_plan_test(const char *json_path) {
    if (!json_path || json_path[0] == '\0') {
        fprintf(stderr, "Missing JSON path\n");
        return 2;
    }

    char *json = read_payload_file(json_path);
    if (!json) {
        fprintf(stderr, "Cannot read input file: %s\n", json_path);
        return 2;
    }

    int errors = 0;
    errors += validate_fuel_on_board(json);
    errors += validate_segment_altitudes(json);
    free(json);

    if (errors > 0) {
        return 1;
    }

    printf("PASS\n");
    return 0;
}
