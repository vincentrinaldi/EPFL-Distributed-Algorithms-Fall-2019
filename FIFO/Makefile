all: da_proc

da_proc: da_proc.c init.c init.h logger.c logger.h sender.c sender.h deliverer.c deliverer.h helper.c helper.h structs.h
	gcc -Wall -pthread -o da_proc da_proc.c init.c init.h logger.c logger.h sender.c sender.h deliverer.c deliverer.h helper.c helper.h structs.h

clean:
	rm da_proc
