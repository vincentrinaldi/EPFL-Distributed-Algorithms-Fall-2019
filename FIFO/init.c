#include "init.h"

// Parse processes attributes from given input file
void parse_arguments(const char* membership_file, int current_process_id, Process** processes, int* number_of_processes_in_membership_file) {
	char * line = NULL;
	size_t len = 1024;
	ssize_t read;

	// Open membership file
	FILE* file = fopen(membership_file, "r+");
	if (file == NULL) {
		exit(EXIT_FAILURE);
	}

	// Read first line to get the number of processes
	if ((read = getline(&line, &len, file)) != -1) {
		(*number_of_processes_in_membership_file) = atoi(line);
	} else {
		exit(EXIT_FAILURE);
	}

	// Check if value of current process id is valid
	if (current_process_id < 0 || current_process_id >= (*number_of_processes_in_membership_file)) {
		exit(EXIT_FAILURE);
	}

	// Allocate memory for the list of processes
	(*processes) = (Process*) malloc((*number_of_processes_in_membership_file) * sizeof(Process));

	char* token;
	const char sep[2] = " ";
	size_t i = 0;

	// Read and initialize each process
	while ((read = getline(&line, &len, file)) != -1 && i < (*number_of_processes_in_membership_file)) {
		Process process;

		token = strtok(line, sep);
		process.id = atoi(token);

		token = strtok(NULL, sep);
		strcpy(process.ip_address, token);

		token = strtok(NULL, sep);
		process.port = atoi(token);

		(*processes)[i] = process;
		i++;
	}

	// Close membership file and return the list of processes
	fclose(file);
}

// Create UDP socket to broadcast messages and listen for incoming packets
int create_socket(char* ip_address, int port, struct sockaddr_in* sockaddr_in) {

	// Create the socket and set it as UDP
	int s;
	if ((s = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP)) == -1) {
		exit(EXIT_FAILURE);
	}

	// Zero out the structure
	memset((char *) sockaddr_in, 0, sizeof(*sockaddr_in));

	// Assign attributes
	sockaddr_in->sin_family = AF_INET;
	sockaddr_in->sin_port = htons(port);
   	sockaddr_in->sin_addr.s_addr = inet_addr(ip_address);

    // Set recvfrom timeout to 100ms
    struct timeval tv;
	tv.tv_sec = 0;
	tv.tv_usec = 100000;
    if (setsockopt(s, SOL_SOCKET, SO_RCVTIMEO,&tv,sizeof(tv)) < 0) {
    	exit(EXIT_FAILURE);
	}

	// Bind the socket to the chosen port
	if (bind(s, (struct sockaddr*)sockaddr_in, sizeof(*sockaddr_in)) == -1) {
		exit(EXIT_FAILURE);
	}

	return s;
}

// Initialize the Broadcast structure
void init_broadcast(Process current_process, Process* processes, int number_of_processes_in_membership_file, int message_array_size, int message_log_size, Broadcast** broadcast) {
	(*broadcast) = (Broadcast*) malloc(sizeof(Broadcast));

 	(*broadcast)->current_process = current_process;
	(*broadcast)->processes = processes;

 	(*broadcast)->forward_msg_list = (Message*) calloc(message_array_size, sizeof(Message));
	(*broadcast)->current_number_of_msg_in_forward_msg_list = 0;
	(*broadcast)->max_possible_number_of_msg_in_forward_msg_list = message_array_size;

 	(*broadcast)->delivered_msg_list = (Message*) calloc(message_array_size, sizeof(Message));
	(*broadcast)->current_number_of_msg_in_delivered_msg_list = 0;
	(*broadcast)->max_possible_number_of_msg_in_delivered_msg_list = message_array_size;

 	(*broadcast)->acked_msg_list = (Ack*) calloc(message_array_size, sizeof(Ack));
	(*broadcast)->current_number_of_msg_in_acked_msg_list = 0;
	(*broadcast)->max_possible_number_of_msg_in_acked_msg_list = message_array_size;

 	(*broadcast)->next_message_to_fifo_deliver_per_process = (int*) calloc(number_of_processes_in_membership_file, sizeof(int));
 	for (size_t i = 0; i < number_of_processes_in_membership_file; i++) {
		(*broadcast)->next_message_to_fifo_deliver_per_process[i] = 1;
	}

 	(*broadcast)->fifo_msg_list = (Message*) calloc(message_array_size, sizeof(Message));
	(*broadcast)->current_number_of_msg_in_fifo_msg_list = 0;
	(*broadcast)->max_possible_number_of_msg_in_fifo_msg_list = message_array_size;

	(*broadcast)->log = (Log*) malloc(sizeof(Log));
	(*broadcast)->log->max_log_str_len = message_log_size;
	(*broadcast)->log->log_str = (char*) calloc((*broadcast)->log->max_log_str_len, sizeof(char));
	(*broadcast)->log->log_str[0] = '\0';
	
	(*broadcast)->sent_msg_list = (Message*) calloc(message_array_size, sizeof(Message));
	(*broadcast)->current_number_of_msg_in_sent_msg_list = 0;
	(*broadcast)->max_possible_number_of_msg_in_sent_msg_list = message_array_size;
}

// Free every allocated blocks of memory 
// /!\ Since everything is cleaned up after termination of the program 
// /!\ this can lead to segmentation fault so we don't use it
void free_structures(Process* processes, Broadcast** broadcast) {
	for (size_t i = 0; i < (*broadcast)->current_number_of_msg_in_sent_msg_list; i++) {
		free((*broadcast)->sent_msg_list[i].is_acked_or_not_per_process);
		(*broadcast)->sent_msg_list[i].is_acked_or_not_per_process = NULL;		
	}													
	free((*broadcast)->sent_msg_list);
	(*broadcast)->sent_msg_list = NULL;
	//printf("OK Send message list\n");
	free((*broadcast)->forward_msg_list);
	(*broadcast)->forward_msg_list = NULL;
	//printf("OK Forward message list\n");
	free((*broadcast)->delivered_msg_list);
	(*broadcast)->delivered_msg_list = NULL;
	//printf("OK Delivered message list\n");
	for(size_t j = 0; j < (*broadcast)->current_number_of_msg_in_acked_msg_list; j++) {
		free((*broadcast)->acked_msg_list[j].list_of_processes_that_acked_the_msg);
		(*broadcast)->acked_msg_list[j].list_of_processes_that_acked_the_msg = NULL;
	}
	free((*broadcast)->acked_msg_list);
	(*broadcast)->acked_msg_list = NULL;
	//printf("OK Acked message list\n");
	free((*broadcast)->next_message_to_fifo_deliver_per_process);
	(*broadcast)->next_message_to_fifo_deliver_per_process = NULL;
	//printf("OK Next message to fifo deliver per process\n");
	free((*broadcast)->fifo_msg_list);
	(*broadcast)->fifo_msg_list = NULL;
	//printf("OK Fifo message list\n");
	free((*broadcast)->log->log_str);
	(*broadcast)->log->log_str = NULL;
	free((*broadcast)->log);
	(*broadcast)->log = NULL;
	//printf("OK Log\n");
	free(processes);
	processes = NULL;
	//printf("OK Processes\n");
	free((*broadcast));
	broadcast = NULL;
	//printf("OK Broadcast\n");
}
