#ifndef HEADER_FILE_SENDER
#define HEADER_FILE_SENDER

#include <time.h>
#include <arpa/inet.h>
#include <sys/socket.h>

#include "structs.h"
#include "logger.h"
#include "helper.h"

#define MESSAGE_ACK_TIMEOUT 0.0025

void broadcast_message(Message* msg, int socket_nb, int concat_to_log, Broadcast** broadcast, Process* processes, int number_of_processes_in_membership_file);

void send_message(int socket_nb, Message* message, Process process);

void check_acknowledgements(Process* processes, int number_of_processes_in_membership_file, Broadcast** broadcast, int socket_nb);

void send_ack(int current_process_id, int socket_nb, struct sockaddr_in* socket_dest, char* msg);

#endif
