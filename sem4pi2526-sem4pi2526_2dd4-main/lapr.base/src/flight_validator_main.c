#include <stdio.h>
#include <stdlib.h>

#include "flight_validator.h"

int main(int argc, char *argv[]) {
    if (argc < 2) {
        fprintf(stderr, "Usage: %s <input.json>\n", argv[0]);
        return 2;
    }
    return run_flight_plan_test(argv[1]);
}
