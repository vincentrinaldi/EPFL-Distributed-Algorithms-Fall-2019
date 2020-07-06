#ifndef HEADER_FILE_INIT
#define HEADER_FILE_INIT

#include <stdio.h>
#include <string.h>
#include <arpa/inet.h>
#include <sys/socket.h>

#include "structs.h"

void parse_arguments(const char* filename, int current_process_id, Process** processes, int* number_of_processes_in_membership_file);

int create_socket(char* ip_address, int port, struct sockaddr_in* sockaddr_in);

void init_broadcast(Process current_process, Process* processes, int number_of_processes_in_membership_file, int message_array_size, int message_log_size, Broadcast** broadcast);

void free_structures(Process* processes, Broadcast** broadcast);

#endif
