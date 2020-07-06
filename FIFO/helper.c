#include "helper.h"

// Extract the message information in the format 'int msqn, int opid, int lpid, [m1:m2:...]'
Message decode_message(char* recv_buffer, size_t buff_length, int number_of_processes_in_membership_file) {

	// Parse and copy the elements of the message to a temporary location
	char* token;
	const char sep[2] = ",";
	char data[buff_length];
	strcpy(data, recv_buffer);

	token = strtok(data, sep);
	int msqn = atoi(token);
	token = strtok(NULL, sep);
	int opid = atoi(token);
	token = strtok(NULL, sep);
	int lpid = atoi(token);
								
	Message msg = {msqn, opid, lpid, NULL, 0};																							

	return msg;
}

// Check if a received message is an acknowledgment
int is_an_ack(char* recv_buffer, size_t buff_length, int* seq_number, int* origin_sender_id, int* last_sender_id) {
	char data[buff_length];
	strcpy(data, recv_buffer); 

	char* str = strtok(data, ",");
	int ret_val;
 
	if (strcmp(str, "ACK") == 0) { 
		ret_val = 1;
		*seq_number = atoi(strtok(NULL, ",")); 
		*origin_sender_id = atoi(strtok(NULL, ",")); 
		*last_sender_id = atoi(strtok(NULL, ","));
	} else { 
		ret_val = 0; 
	} 

	return ret_val;
}

// Function to check if two messages have the same original sender and message id
int message_equals(Message msg1, Message msg2) {
	return msg1.origin_sender_id == msg2.origin_sender_id && msg1.seq_number == msg2.seq_number;
}

// Function to check if a specific message is in a specific list
int is_message_in_list(Message msg, Message* messages_list, size_t number_of_messages_in_list) {
 	for (size_t i = 0; i < number_of_messages_in_list; i++) {
		if (message_equals(msg, messages_list[i])) {
			return 1;
		}
	}
	return 0;
}

// Function to check if a specific process acked a specific message
int check_process_id_in_ack_list(int process_id, int* process_ids_list, size_t number_of_process_ids_in_list) {
 	for (size_t i = 0; i < number_of_process_ids_in_list; i++) {
		if (process_id == process_ids_list[i]) {
			return 1;
		}
	}
	return 0;
}

// Function to append a specific process id to the processes id list of an ack that acked the corresponding message
size_t append_process_id_to_ack_list(int process_id, int* process_ids_list, size_t number_of_process_ids_in_list) {
	if (check_process_id_in_ack_list(process_id, process_ids_list, number_of_process_ids_in_list)) {
		return number_of_process_ids_in_list;
	}

	// This part should never be executed if number_of_process_ids_in_list is equal to number_of_processes_in_membership_file
	process_ids_list[number_of_process_ids_in_list] = process_id;
	number_of_process_ids_in_list++;
	return number_of_process_ids_in_list;
}

// Function to return the list of processes id that acked a specific message
Ack find_ack(Message msg, Ack* acks_list, size_t number_of_acks_in_list) {
 	for (size_t i = 0; i < number_of_acks_in_list; i++) {
		if (message_equals(msg, acks_list[i].msg)) {
			return acks_list[i];
		}
	}

 	Ack invalid = {msg, NULL, -1};
	return invalid;
}

// Function to append a new ack to a list and if it's already here we simply update the process ids list of this ack
void update_or_append_ack_to_list(int last_sender_id, Message msg, Ack** acks_list, size_t* number_of_acks_in_list, size_t* max_possible_number_of_acks_in_list, int number_of_processes_in_membership_file) {
	Ack ack = find_ack(msg, (*acks_list), (*number_of_acks_in_list));
	
	if (ack.number_of_processes_that_acked_the_msg == -1) {
		int* process_ids_list = (int*) calloc(number_of_processes_in_membership_file, sizeof(int));
		Ack new_ack = {msg, process_ids_list, 0};
		ack = new_ack;
		ack.number_of_processes_that_acked_the_msg = append_process_id_to_ack_list(last_sender_id, ack.list_of_processes_that_acked_the_msg, ack.number_of_processes_that_acked_the_msg);

		if ((*number_of_acks_in_list) == (*max_possible_number_of_acks_in_list)) {
			(*max_possible_number_of_acks_in_list) = 2 * (*max_possible_number_of_acks_in_list);
			(*acks_list) = realloc((*acks_list), (*max_possible_number_of_acks_in_list) * sizeof(Ack));
		}
	
		(*acks_list)[(*number_of_acks_in_list)] = ack;
		(*number_of_acks_in_list)++;
	} else {	
		ack.number_of_processes_that_acked_the_msg = append_process_id_to_ack_list(last_sender_id, ack.list_of_processes_that_acked_the_msg, ack.number_of_processes_that_acked_the_msg);
		for (size_t i = 0; i < (*number_of_acks_in_list); i++) {
			if (message_equals(ack.msg, (*acks_list)[i].msg)) {
				(*acks_list)[i] = ack;
			}
		}
	}
}

// Function to append a message to a list
void append_msg_to_list(Message msg, Message** messages_list, size_t* number_of_messages_in_list, size_t* max_possible_number_of_messages_in_list) {

 	// Check that msg in not already in the list
	if (!is_message_in_list(msg, (*messages_list), (*number_of_messages_in_list))) {
		// Realloc if list is full
		if ((*number_of_messages_in_list) == (*max_possible_number_of_messages_in_list)) {
			(*max_possible_number_of_messages_in_list) = 2 * (*max_possible_number_of_messages_in_list);
			(*messages_list) = realloc((*messages_list), (*max_possible_number_of_messages_in_list) * sizeof(Message));
		}

		(*messages_list)[(*number_of_messages_in_list)] = msg;
		(*number_of_messages_in_list)++;
	}
}

// Function to check if we found the message we are looking for
int is_next_message (Message msg, Broadcast* broadcast) {
	return broadcast->next_message_to_fifo_deliver_per_process[msg.origin_sender_id - 1] == msg.seq_number;
}
