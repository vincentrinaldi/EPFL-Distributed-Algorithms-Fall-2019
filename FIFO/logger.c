#include "logger.h"

// Save and concatenate an operation into the log string
void save_log(Log* log, char* op) {
	size_t new_l = strlen(log->log_str) + strlen(op);
	if (new_l > log->max_log_str_len) {
		log->max_log_str_len = log->max_log_str_len * 2;
		log->log_str = realloc(log->log_str, log->max_log_str_len * sizeof(char));
	}
	strcat(log->log_str, op);
	strcat(log->log_str, "\n");
}

// Write a complete log string into a file
void write_log(char* log, int current_process_id) {
	char file_name[30], str_process_id[12], ext_file[5];

	// Fill components which form name of file
	strcpy(file_name, "da_proc_");
	sprintf(str_process_id, "%d", current_process_id);
	strcpy(ext_file, ".out");

	// Set name of file
	strcat(file_name, str_process_id);
	strcat(file_name, ext_file);

	// Create and open file for writing
	FILE* fp = fopen(file_name, "w");
	if(fp == NULL){
		exit(EXIT_FAILURE);
	}

	// Write down the log string
	fprintf(fp, "%s", log);

	// Close file
	fclose(fp);
}
