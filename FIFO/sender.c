#include "sender.h"

// Function to concatenate to the log sequence the broadcast operation of the message we want to broadcast
void broadcast_message(Message* msg, int socket_nb, int concat_to_log, Broadcast** broadcast, Process* processes, int number_of_processes_in_membership_file) {

 	// Concatenate to the log sequence if condition is met
	if (concat_to_log) {
		char op[16];
		char msg_seq_num[16];
		sprintf(op, "b ");
		sprintf(msg_seq_num, "%d", msg->seq_number);
		strcat(op, msg_seq_num);
		save_log((*broadcast)->log, op);
	}
	
	// Set the sending time and the processes acks list attributes of the message that is being sent
	msg->send_time = clock(); 
	msg->is_acked_or_not_per_process = (int*) calloc(sizeof(int), number_of_processes_in_membership_file); 
	memset(msg->is_acked_or_not_per_process, 0, sizeof(int) * number_of_processes_in_membership_file); 
	msg->is_acked_or_not_per_process[(*broadcast)->current_process.id - 1] = 1;
	
	// Add the message that is being sent to the messages sent list attribute of the Broadcast structure
	if (!is_message_in_list(*msg, (*broadcast)->sent_msg_list, (*broadcast)->current_number_of_msg_in_sent_msg_list)) { 
		if ((*broadcast)->current_number_of_msg_in_sent_msg_list == (*broadcast)->max_possible_number_of_msg_in_sent_msg_list) {
			(*broadcast)->max_possible_number_of_msg_in_sent_msg_list = 2 * (*broadcast)->max_possible_number_of_msg_in_sent_msg_list; 
			(*broadcast)->sent_msg_list = realloc((*broadcast)->sent_msg_list, (*broadcast)->max_possible_number_of_msg_in_sent_msg_list * sizeof(Message)); 
		} 
 		(*broadcast)->sent_msg_list[(*broadcast)->current_number_of_msg_in_sent_msg_list] = *msg; 
		(*broadcast)->current_number_of_msg_in_sent_msg_list++;
	}
	//printf("Process sending is %d\n", (*broadcast)->current_process.id);

 	// Broadcast the message
	for (int i = 0 ; i < number_of_processes_in_membership_file ; i++) {
		if ((i+1) != msg->last_sender_id) {
			send_message(socket_nb, msg, processes[i]);
		}
	}
}

// Send a message through a socket
void send_message(int socket_nb, Message* message, Process process) {
	struct sockaddr_in socket_dest;
	socklen_t slen = sizeof(socket_dest);

	// Fill sockaddr_in structure with the destination information
	memset((char *) &socket_dest, 0, slen);
	socket_dest.sin_family = AF_INET;
	socket_dest.sin_port = htons(process.port);
	socket_dest.sin_addr.s_addr = inet_addr(process.ip_address);

	// Convert the message to string in order to send it through the socket
	char msqn_part[16];
	sprintf(msqn_part, "%d,", message->seq_number);
	char opid_part[16];
	sprintf(opid_part, "%d,", message->origin_sender_id);
	char lpid_part[16];
	sprintf(lpid_part, "%d", message->last_sender_id);
	//printf("Mess Seq Part is %s\n", msqn_part);
	//printf("Orig Proc Part is %s\n", opid_part);
	//printf("Last Proc Part is %s\n", lpid_part);

	char msg_str[64];
	msg_str[0] = '\0';
	strcat(msg_str, msqn_part);
	strcat(msg_str, opid_part);
	strcat(msg_str, lpid_part);
	//printf("Sending message %s to %d\n", msg_str, process.port);

	// Send the message
	if (sendto(socket_nb, msg_str, strlen(msg_str), 0, (struct sockaddr *) &socket_dest, slen) == -1) {
		exit(EXIT_FAILURE);
	}
}

// Send again a message to every processes that still did not ack it 
void check_acknowledgements(Process* processes, int number_of_processes_in_membership_file, Broadcast** broadcast, int socket_nb) { 
	clock_t curr_time = clock();
	
	for (int i = 0 ; i < (*broadcast)->current_number_of_msg_in_sent_msg_list ; ++i) { 
		for (int j = 0 ; j < number_of_processes_in_membership_file ; ++j) { 
			if ((*broadcast)->sent_msg_list[i].is_acked_or_not_per_process[j] == 0 && ((double)(curr_time - (*broadcast)->sent_msg_list[i].send_time) / CLOCKS_PER_SEC) > MESSAGE_ACK_TIMEOUT) { 
				send_message(socket_nb, &((*broadcast)->sent_msg_list[i]), processes[j]); 
				//printf("Proc. %d : Message %d,%d,%d is sent again to process %d because %f is greater than %f\n", (*broadcast)->current_process.id, (*broadcast)->sent_msg_list[i].seq_number, 
				//(*broadcast)->sent_msg_list[i].origin_sender_id, (*broadcast)->sent_msg_list[i].last_sender_id, processes[j].id, ((double)(curr_time - (*broadcast)->sent_msg_list[i].send_time) / CLOCKS_PER_SEC), 
				//MESSAGE_ACK_TIMEOUT);
			} 
		} 
		(*broadcast)->sent_msg_list[i].send_time = clock(); 
	} 
}

// Send an acknowledgment
void send_ack(int current_process_id, int socket_nb, struct sockaddr_in* socket_dest, char* msg) { 
	char msg_seq[64];
	strcpy(msg_seq, msg); 
 
	char ack[64];
	strcpy(ack, "ACK,");
	strcat(ack, strtok(msg_seq, ","));
	strcat(ack, ",");
	strcat(ack, strtok(NULL, ","));
	strcat(ack, ",");
 
	char sender_id[16];
	sprintf(sender_id, "%d", current_process_id);
	strcat(ack, sender_id); 
	//printf("Proc %d sends the ACK : %s\n", current_process_id, ack);
 
	socklen_t slen = sizeof(*socket_dest);

	if (sendto(socket_nb, ack, strlen(ack), 0, (struct sockaddr *) socket_dest, slen) == -1) { 
		exit(EXIT_FAILURE);
	}
}
