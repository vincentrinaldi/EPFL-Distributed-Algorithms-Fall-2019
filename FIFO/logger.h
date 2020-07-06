#ifndef HEADER_FILE_LOGGER
#define HEADER_FILE_LOGGER

#include <stdio.h>
#include <string.h>

#include "structs.h"

void save_log(Log* log, char* op);

void write_log(char* log, int current_process_id);

#endif
