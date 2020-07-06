#ifndef HEADER_FILE_DELIVERER
#define HEADER_FILE_DELIVERER

#include "structs.h"
#include "logger.h"
#include "sender.h"
#include "helper.h"

#define BUFLEN 512

void deliver_messages(Process* processes, int number_of_processes_in_membership_file, int current_process_id, int socket_nb, Broadcast** broadcast, 
					  void (*deliver_callback)(Message* message, int socket_nb, Broadcast** broadcast, int current_process_id, Process* processes, int number_of_processes_in_membership_file));
					  
void deliver_procedure(Message* message, int socket_nb, Broadcast** broadcast, int current_process_id, Process* processes, int number_of_processes_in_membership_file);

void check_fifo_delivery_list(Broadcast** broadcast);

void fifo_deliver(Message msg, Broadcast** broadcast);

#endif
