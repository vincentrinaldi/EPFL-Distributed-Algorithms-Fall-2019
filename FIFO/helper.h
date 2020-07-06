#ifndef HEADER_FILE_HELPER
#define HEADER_FILE_HELPER

#include <stdio.h>
#include <string.h>

#include "structs.h"

Message decode_message(char* recv_buffer, size_t buff_length, int number_of_processes_in_membership_file);

int is_an_ack(char* recv_buffer, size_t buff_length, int* seq_number, int* origin_sender_id, int* last_sender_id);

int message_equals(Message msg1, Message msg2);

int is_message_in_list(Message msg, Message* messages_list, size_t number_of_messages_in_list);

int check_process_id_in_ack_list(int process_id, int* process_ids_list, size_t number_of_process_ids_in_list);

size_t append_process_id_to_ack_list(int process_id, int* process_ids_list, size_t number_of_process_ids_in_list);

Ack find_ack(Message msg, Ack* acks_list, size_t number_of_acks_in_list);

void update_or_append_ack_to_list(int last_sender_id, Message msg, Ack** acks_list, size_t* number_of_acks_in_list, size_t* max_possible_number_of_acks_in_list, int number_of_processes_in_membership_file);

void append_msg_to_list(Message msg, Message** messages_list, size_t* number_of_messages_in_list, size_t* max_possible_number_of_messages_in_list);

int is_next_message (Message msg, Broadcast* broadcast);

#endif
