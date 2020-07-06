#include "deliverer.h"

// Deliver a received message via a socket
void deliver_messages(Process* processes, int number_of_processes_in_membership_file, int current_process_id, int socket_nb, Broadcast** broadcast, 
					  void (*deliver_callback)(Message* message, int socket_nb, Broadcast** broadcast, int current_process_id, Process* processes, int number_of_processes_in_membership_file)) {

	struct sockaddr_in socket_other;
	socklen_t slen = sizeof(socket_other);
	int recv_len;
	char recv_buffer[BUFLEN];
	
	int number_iterations = 0;

	while(1) {
		
	    memset(recv_buffer,'\0', BUFLEN);
		if ((recv_len = recvfrom(socket_nb, recv_buffer, BUFLEN, 0, (struct sockaddr *) &socket_other, &slen)) >= 0) {
			
			// We check if the received message is an acknowledgment
			
			int seq_number, origin_sender_id, last_sender_id; 
			if (is_an_ack(recv_buffer, strlen(recv_buffer) * sizeof(char), &seq_number, &origin_sender_id, &last_sender_id)) {
				
				// If it is the case then we update the processes acks list attribute of the corresponding previously sent message
				
				int is_msg_found = 0; 
				int i = 0; 

				while (is_msg_found == 0 && i < (*broadcast)->current_number_of_msg_in_sent_msg_list) {
					if ((*broadcast)->sent_msg_list[i].seq_number == seq_number && (*broadcast)->sent_msg_list[i].origin_sender_id == origin_sender_id) {
						is_msg_found = 1; 
						(*broadcast)->sent_msg_list[i].is_acked_or_not_per_process[last_sender_id - 1] = 1;
						//printf("Proc %d received ack for %d,%d,%d and update pos %d to %d\n", (*broadcast)->current_process.id, seq_number, origin_sender_id, last_sender_id, last_sender_id - 1, 
						//(*broadcast)->sent_msg_list[i].is_acked_or_not_per_process[last_sender_id - 1]);
						Message message = {seq_number, origin_sender_id, last_sender_id, NULL, 0};
						if (deliver_callback != NULL) {
							(*deliver_callback)(&message, socket_nb, broadcast, current_process_id, processes, number_of_processes_in_membership_file);		
						}
					} 
					++i;
				}
				
			} else {
				
				// Otherwise we decode and deliver the message normally and we do not forget to send an acknowledment to the sender
			
				Message message = decode_message(recv_buffer, strlen(recv_buffer) * sizeof(char), number_of_processes_in_membership_file);
				//printf("Proc %d received message %d,%d,%d\n", (*broadcast)->current_process.id, message.seq_number, message.origin_sender_id, message.last_sender_id);
				
				send_ack(current_process_id, socket_nb, &socket_other, recv_buffer);		

				// Deliver the message to its upper layer
				if (deliver_callback != NULL) {
					(*deliver_callback)(&message, socket_nb, broadcast, current_process_id, processes, number_of_processes_in_membership_file);		
				}
				
			}
		}
		
		if (number_iterations == 20) {
			number_iterations = 0; 
			check_acknowledgements(processes, number_of_processes_in_membership_file, broadcast, socket_nb); 
		} 
		++number_iterations;
		
	}
}

// Function to ensure that every other processes received the message before delivering it
void deliver_procedure(Message* message, int socket_nb, Broadcast** broadcast, int current_process_id, Process* processes, int number_of_processes_in_membership_file) {
	
	// We ack the received message
	update_or_append_ack_to_list(message->last_sender_id, *message, &((*broadcast)->acked_msg_list), &((*broadcast)->current_number_of_msg_in_acked_msg_list), &((*broadcast)->max_possible_number_of_msg_in_acked_msg_list), 
								 number_of_processes_in_membership_file);
	update_or_append_ack_to_list(current_process_id, *message, &((*broadcast)->acked_msg_list), &((*broadcast)->current_number_of_msg_in_acked_msg_list), &((*broadcast)->max_possible_number_of_msg_in_acked_msg_list), 
								 number_of_processes_in_membership_file);
	//for (int i = 0 ; i < (*broadcast)->current_number_of_msg_in_acked_msg_list ; i++) {
		//for (int j = 0 ; j < (*broadcast)->acked_msg_list[i].number_of_processes_that_acked_the_msg ; j++) {
			//printf("For %d : size of ack list is %zu ; total elem that acked the message %d,%d,%d with time %ld is %zu and elem are %d\n", (*broadcast)->current_process.id, (*broadcast)->current_number_of_msg_in_acked_msg_list, 
			//(*broadcast)->acked_msg_list[i].msg.seq_number, (*broadcast)->acked_msg_list[i].msg.origin_sender_id, (*broadcast)->acked_msg_list[i].msg.last_sender_id, (*broadcast)->acked_msg_list[i].msg.send_time,
			//(*broadcast)->acked_msg_list[i].number_of_processes_that_acked_the_msg, (*broadcast)->acked_msg_list[i].list_of_processes_that_acked_the_msg[j]);
		//}
	//}
	
	// We forward the message to the other processes after receiving it in the case this has not already been done for this specific message
	if (!is_message_in_list(*message, (*broadcast)->forward_msg_list, (*broadcast)->current_number_of_msg_in_forward_msg_list)) {
		append_msg_to_list(*message, &((*broadcast)->forward_msg_list), &((*broadcast)->current_number_of_msg_in_forward_msg_list), &((*broadcast)->max_possible_number_of_msg_in_forward_msg_list));
		// We forward it only if the current process is not the original sender
		if (message->origin_sender_id != current_process_id) {
			message->last_sender_id = current_process_id;
			broadcast_message(message, socket_nb, 0, broadcast, processes, number_of_processes_in_membership_file);
		}
	}
	//for (int i = 0 ; i < (*broadcast)->current_number_of_msg_in_forward_msg_list ; i++) {
		//printf("For %d : size of forward list is %zu and message is %d,%d,%d with clock %ld\n", (*broadcast)->current_process.id, (*broadcast)->current_number_of_msg_in_forward_msg_list, 
		//(*broadcast)->forward_msg_list[i].seq_number, (*broadcast)->forward_msg_list[i].origin_sender_id, (*broadcast)->forward_msg_list[i].last_sender_id, (*broadcast)->forward_msg_list[i].send_time);
	//}

 	Message msg;
 	int are_majority_acked;
 	for(size_t i = 0; i < (*broadcast)->current_number_of_msg_in_forward_msg_list; i++) {
		msg = (*broadcast)->forward_msg_list[i];
		if (is_message_in_list(msg, (*broadcast)->delivered_msg_list, (*broadcast)->current_number_of_msg_in_delivered_msg_list)) {
			continue;
		}
		
		Ack ack = find_ack(msg, (*broadcast)->acked_msg_list, (*broadcast)->current_number_of_msg_in_acked_msg_list);
		are_majority_acked = 0;
			
		if (ack.number_of_processes_that_acked_the_msg > number_of_processes_in_membership_file / 2) {
			are_majority_acked = 1;
			//printf("Proc. %d : Majority acked the messsage for %d,%d,%d with clock %ld\n", (*broadcast)->current_process.id, ack.msg.seq_number, ack.msg.origin_sender_id, ack.msg.last_sender_id, ack.msg.send_time);
			//printf("Majority for %d,%d,%d = %zu\n", ack.msg.seq_number, ack.msg.origin_sender_id, ack.msg.last_sender_id, ack.number_of_processes_that_acked_the_msg);
			//for (int j = 0 ; j < ack.number_of_processes_that_acked_the_msg ; j++) {
				//printf("Member of majority for %d,%d,%d is %d\n", ack.msg.seq_number, ack.msg.origin_sender_id, ack.msg.last_sender_id, ack.list_of_processes_that_acked_the_msg[j]);
			//}
		}

		if (are_majority_acked) {
			append_msg_to_list(msg, &((*broadcast)->delivered_msg_list), &((*broadcast)->current_number_of_msg_in_delivered_msg_list), &((*broadcast)->max_possible_number_of_msg_in_delivered_msg_list));
			//for (int i = 0 ; i < (*broadcast)->current_number_of_msg_in_delivered_msg_list ; i++) {
				//printf("For %d : size of delivered list is %zu and message is %d,%d,%d with clock %ld\n", (*broadcast)->current_process.id, (*broadcast)->current_number_of_msg_in_delivered_msg_list, 
				//(*broadcast)->delivered_msg_list[i].seq_number, (*broadcast)->delivered_msg_list[i].origin_sender_id, (*broadcast)->delivered_msg_list[i].last_sender_id, (*broadcast)->delivered_msg_list[i].send_time);
			//}
			fifo_deliver(msg, broadcast);
		}
	}
}

// Function to deliver the next message that has to be delivered if it is contained into the list
void check_fifo_delivery_list(Broadcast** broadcast) {
	size_t to_deliver = -1;
 	for (size_t i = 0; i < (*broadcast)->current_number_of_msg_in_fifo_msg_list; i++) {
		if (is_next_message((*broadcast)->fifo_msg_list[i], (*broadcast))) {
			to_deliver = i;
			break;
		}
	}

	if (to_deliver != -1) {
		fifo_deliver((*broadcast)->fifo_msg_list[to_deliver], broadcast);
	}
}

// Function to concatenate to the log sequence the delivered operation of the delivered message
void fifo_deliver(Message msg, Broadcast** broadcast) {

 	// Concatenate the log sequence if the condition is met
	if (is_next_message(msg, (*broadcast))) {
		//printf("Proc. %d : Fifo delivering of %d,%d,%d with clock %ld\n", (*broadcast)->current_process.id, msg.seq_number, msg.origin_sender_id, msg.last_sender_id, msg.send_time);
		//printf("Proc. %d : list of id to deliver at pos %d has the value %d\n", (*broadcast)->current_process.id, msg.origin_sender_id - 1, (*broadcast)->next_message_to_fifo_deliver_per_process[msg.origin_sender_id - 1]);
		(*broadcast)->next_message_to_fifo_deliver_per_process[msg.origin_sender_id - 1] += 1;
		//printf("Proc. %d : list of id to deliver at pos %d has now the updated value %d\n", (*broadcast)->current_process.id, msg.origin_sender_id - 1, (*broadcast)->next_message_to_fifo_deliver_per_process[msg.origin_sender_id - 1]);
		char op[16];
		char msg_seq_num[16];
		sprintf(op, "d ");
		sprintf(msg_seq_num, "%d ", msg.origin_sender_id);
		strcat(op, msg_seq_num);
		sprintf(msg_seq_num, "%d", msg.seq_number);
		strcat(op, msg_seq_num);
		save_log((*broadcast)->log, op);
		
		check_fifo_delivery_list(broadcast);
	} else {
		// Add the message to the fifo list if it is not the next message from that process to deliver
		append_msg_to_list(msg, &((*broadcast)->fifo_msg_list), &((*broadcast)->current_number_of_msg_in_fifo_msg_list), &((*broadcast)->max_possible_number_of_msg_in_fifo_msg_list));
	}
}
