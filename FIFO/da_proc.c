#include <stdio.h>
#include <stdlib.h>
#include <signal.h>
#include <time.h>
#include <string.h>
#include <pthread.h>

#include "structs.h"
#include "init.h"
#include "logger.h"
#include "sender.h"
#include "deliverer.h"

static int wait_for_start = 1;
static int current_process_id;
static int number_of_processes_in_membership_file;
static int number_of_messages_to_send;
static int message_array_size = 512;
static int message_log_size = 2048;
static int socket_nb;
static char* membership_file;
static Process* processes;
static Broadcast* broadcast;

// Message Delivery thread function
void *myThreadForMessageDelivery(void *vargp){
	deliver_messages(processes, number_of_processes_in_membership_file, processes[current_process_id].id, socket_nb, &broadcast, &deliver_procedure);	
	return NULL;
}

static void start(int signum) {
	wait_for_start = 0;
}

static void stop(int signum) {
	//reset signal handlers to default
	signal(SIGTERM, SIG_DFL);
	signal(SIGINT, SIG_DFL);

	//immediately stop network packet processing
	//printf("Immediately stopping network packet processing.\n");
	

	//write/flush output file if necessary
	//printf("Writing output.\n");
	write_log(broadcast->log->log_str, processes[current_process_id].id);
	
	//free_structures(processes, &broadcast);

	//exit directly from signal handler
	exit(0);
}

int main(int argc, char** argv) {

	//set signal handlers
	signal(SIGUSR2, start);
	signal(SIGTERM, stop);
	signal(SIGINT, stop);


	//parse arguments, including membership
	current_process_id = atoi(argv[1]) - 1;
	membership_file = argv[2];
	number_of_messages_to_send = atoi(argv[3]);
	parse_arguments(membership_file, current_process_id, &processes, &number_of_processes_in_membership_file);
	Process current_process = processes[current_process_id];
	//printf("Proc. %d : Process is is %d,%s,%d\n", current_process.id, current_process.id, current_process.ip_address, current_process.port);
	
	//initialize application
	struct sockaddr_in sockaddr_in;
	socket_nb = create_socket(current_process.ip_address, current_process.port, &sockaddr_in);
	init_broadcast(current_process, processes, number_of_processes_in_membership_file, message_array_size, message_log_size, &broadcast);
	
	//start listening for incoming UDP packets
	//printf("Initializing.\n");
	pthread_t thread_id_message_delivery;
	if (pthread_create(&thread_id_message_delivery, NULL, myThreadForMessageDelivery, NULL) != 0) {
		exit(EXIT_FAILURE);
	}

	//wait until start signal
	while(wait_for_start) {
		struct timespec sleep_time;
		sleep_time.tv_sec = 0;
		sleep_time.tv_nsec = 1000;
		nanosleep(&sleep_time, NULL);
	}


	//broadcast messages
	//printf("Broadcasting messages.\n");
	for (size_t seq_num = 1; seq_num <= number_of_messages_to_send; seq_num++) {
		Message message = {seq_num, current_process.id, current_process.id, NULL, 0};
		//printf("Proc. %d : Message is %d,%d,%d\n", current_process.id, message.seq_number, message.origin_sender_id, message.last_sender_id);
		broadcast_message(&message, socket_nb, 1, &broadcast, processes, number_of_processes_in_membership_file);
		//printf("Proc. %d : Number of message sent in broadcast is %zu\n", current_process.id, broadcast->current_number_of_msg_in_sent_msg_list);
		//for (int i = 0 ; i < broadcast->current_number_of_msg_in_sent_msg_list ; i++) {
			//printf("Proc. %d : Message in list is %d, %d, %d, [%d %d %d %d %d], %ld\n", current_process.id, broadcast->sent_msg_list[i].seq_number, broadcast->sent_msg_list[i].origin_sender_id, broadcast->sent_msg_list[i].last_sender_id, 
			//broadcast->sent_msg_list[i].is_acked_or_not_per_process[0], broadcast->sent_msg_list[i].is_acked_or_not_per_process[1], broadcast->sent_msg_list[i].is_acked_or_not_per_process[2], 
			//broadcast->sent_msg_list[i].is_acked_or_not_per_process[3], broadcast->sent_msg_list[i].is_acked_or_not_per_process[4], broadcast->sent_msg_list[i].send_time);
		//}
		deliver_procedure(&message, socket_nb, &broadcast, current_process.id, processes, number_of_processes_in_membership_file);
	}

	//wait until stopped
	while(1) {
		struct timespec sleep_time;
		sleep_time.tv_sec = 1;
		sleep_time.tv_nsec = 0;
		nanosleep(&sleep_time, NULL);
	}
}
