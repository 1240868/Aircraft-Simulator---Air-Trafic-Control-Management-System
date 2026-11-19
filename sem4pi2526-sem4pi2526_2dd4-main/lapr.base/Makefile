# US085 — flight plan physics validator (C)

CC      = gcc
CFLAGS  = -Wall -Wextra -g -std=c99 -D_GNU_SOURCE -Iinclude
LDFLAGS = -lm

SRC_DIR = src
OBJ_DIR = build
INC_DIR = include

TARGET = flight_plan_validator
OBJS   = $(OBJ_DIR)/flight_validator.o $(OBJ_DIR)/flight_validator_main.o

all: $(TARGET)

compile: $(TARGET)

$(TARGET): $(OBJS)
	$(CC) $(CFLAGS) -o $@ $^ $(LDFLAGS)

$(OBJ_DIR)/%.o: $(SRC_DIR)/%.c $(wildcard $(INC_DIR)/*.h) | $(OBJ_DIR)
	$(CC) $(CFLAGS) -c $< -o $@

$(OBJ_DIR):
	mkdir -p $(OBJ_DIR)

clean:
	rm -rf $(OBJ_DIR) $(TARGET)

.PHONY: all compile clean
