#ifndef HEADER_FILE_STRUCTS
#define HEADER_FILE_STRUCTS

#include <stdlib.h>

typedef struct Process {
	int id;
	char ip_address[16];
	int port;
} Process;

typedef struct Message {
	int seq_number;
	int origin_sender_id;
	int last_sender_id;
	int* is_acked_or_not_per_process;
	clock_t send_time;
} Message;

typedef struct Ack {
	Message msg;
	int* list_of_processes_that_acked_the_msg;
	size_t number_of_processes_that_acked_the_msg;
} Ack;

typedef struct Log {
	char* log_str;
	int max_log_str_len;
} Log;

typedef struct Broadcast {
	Process current_process;
	Process* processes;
	Message* forward_msg_list;
	size_t current_number_of_msg_in_forward_msg_list;
	size_t max_possible_number_of_msg_in_forward_msg_list;
	Message* delivered_msg_list;
	size_t current_number_of_msg_in_delivered_msg_list;
	size_t max_possible_number_of_msg_in_delivered_msg_list;
	Ack* acked_msg_list;
	size_t current_number_of_msg_in_acked_msg_list;
	size_t max_possible_number_of_msg_in_acked_msg_list;
	int* next_message_to_fifo_deliver_per_process;
	Message* fifo_msg_list;
	size_t current_number_of_msg_in_fifo_msg_list;
	size_t max_possible_number_of_msg_in_fifo_msg_list;
	Log* log;
	Message* sent_msg_list;
	size_t current_number_of_msg_in_sent_msg_list;
	size_t max_possible_number_of_msg_in_sent_msg_list;
} Broadcast;

#endif
