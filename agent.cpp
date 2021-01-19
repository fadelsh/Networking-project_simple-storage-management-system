#include <stdio.h>
#include <string.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <stdlib.h>
#include <arpa/inet.h>
#include <stdlib.h>
#include<time.h>
#include <chrono>
#include <ctime>
#include <iostream>
#include <pthread.h>
#include <vector>
#include <string>
#include <sys/types.h>
#include <unistd.h>
#include <sys/utsname.h>

using namespace std;
#define UDPPORT 4321

int tcpCmdPort;
int interval;
string all; 

typedef struct BEACON{
    int ID;
    string startUpTime;
    int timeInterval;
    string IP;
    int cmdPort;
    
    BEACON (){
        auto timenow = chrono::system_clock::to_time_t(chrono::system_clock::now()); 
        srand(time(NULL));
        
        char* temp= ctime(&timenow);
        temp[strlen(temp)-1]= '\0';
        
        this -> ID=rand(); // randomly generated ID during startup
        this -> startUpTime= temp;
        this -> timeInterval=60; 
        this -> IP="127.0.0.1"; //localhost
        this -> cmdPort= 1000 + ( std::rand() % ( 9999 - 1000 + 1 ));  //randomly generated cmdPort during startup to be used for TCP connection for each agent
        tcpCmdPort=cmdPort;
        interval=timeInterval;
    }
    
}  *beacon;

void initliaze (){
    beacon b=new BEACON;
    string id=to_string(b->ID);
    string interval= to_string(b->timeInterval);
    string port= to_string(b->cmdPort);

     all= id+","+b->startUpTime+ ","+interval+","+b->IP+","+port;
}

void* sendBeacon(void* arg){
   // beacon b=new BEACON;
    
    int server_socket;
    if ( (server_socket= socket(AF_INET, SOCK_DGRAM, 0)) < 0 ){
        perror("udp server: socket call\n");
        exit(1);
    }
    struct sockaddr_in servaddr, cliaddr;
    
    memset(&servaddr, 0, sizeof(servaddr));
    memset(&cliaddr, 0, sizeof(cliaddr));
    
    //servaddr.sin_len = sizeof(servaddr);  // comment this line out if running on pyrite (linux)
    servaddr.sin_family = AF_INET;
    servaddr.sin_addr.s_addr = inet_addr("127.0.0.1");
    servaddr.sin_port = htons(UDPPORT);
    if (::connect(server_socket, (struct sockaddr *)&servaddr, sizeof(servaddr)) < 0)
    {
        printf("bind error\n");
    }
    
    char* UDPpacket = const_cast<char*>(all.c_str());
    
    for(;;){
        
        sendto(server_socket, UDPpacket ,1000,0,(struct sockaddr *) NULL, sizeof(cliaddr));
        sleep(interval); // send UDP packet, referred to as a beacon, periodically (e.g., every 1 minute) to the manager
        
    }
    
    return NULL;
}

//used the template posted on Canvas here
int receive_one_byte(int client_socket, char *cur_char)
{
    ssize_t bytes_received = 0;
    while (bytes_received != 1)
    {
        bytes_received = recv(client_socket, cur_char, 1, 0);
    }
    
    return 1;
}

//used the template posted on Canvas here
int receiveFully(int client_socket, char *buffer, int length)
{
    char *cur_char = buffer;
    ssize_t bytes_received = 0;
    while (bytes_received != length)
    {
        receive_one_byte(client_socket, cur_char);
        cur_char++;
        bytes_received++;
    }
    
    return 1;
}

//used the template posted on Canvas here to handle difference in endianness
int toInteger32(char *bytes)
{
    int tmp = (bytes[0] << 24) +
    (bytes[1] << 16) +
    (bytes[2] << 8) +
    bytes[3];
    
    return tmp;
}

//GetLocalOS function. Here OS[16] contains the name of the operating system where the agent is running, 
//and the integer pointed by valid indicates the execution result. If it is 1, the data in OS is valid.
void GetLocalOS(char OS[16], int *valid){

    if(*valid==1){
           struct utsname details;
           int ret = uname(&details);
            if (ret == 0){
                strcat(details.sysname, " ");
                strcat (details.sysname,details.release);
                strcat(OS,details.sysname);
            }
    }
    else{
        strcat(OS, "Not valid OS");
    }

}
//Here the integer pointed by time represents the current system clock,
// and the integer pointed by valid indicates the execution result. If it is 1, the data pointed by time is valid.
void GetLocalTime(char localTime[16], int *valid){

  if(*valid==1){
    time_t my_time = time(NULL); 
    strcat(localTime,ctime(&my_time));
  }
  else{
      strcat (localTime, "Time is not valid");
  }

}

//used the template posted on Canvas on some stuff here
void *listenPort(void* arg){

    int server_socket = socket(PF_INET, SOCK_STREAM, IPPROTO_TCP);
    printf("server_socket = %d\n", server_socket);
    struct sockaddr_in sin;
    memset(&sin, 0, sizeof(sin));
    sin.sin_family = AF_INET;
    sin.sin_port = htons(tcpCmdPort);
    sin.sin_addr.s_addr= inet_addr("127.0.0.1");

    if (bind(server_socket, (struct sockaddr *)&sin, sizeof(sin)) < 0){
        printf("bind error\n");
    }
    
    listen(server_socket, 5);
    int counter = 0;

    while (1){

        struct sockaddr client_addr;
        unsigned int client_len;
        
        printf("TCP accepting ....\n");
        int client_socket = accept(server_socket, &client_addr, &client_len);
        printf("request %d comes ...\n", counter++);
        char packet_length_bytes[4];
        receiveFully(client_socket, packet_length_bytes, 4);
        
        //printBinaryArray(packet_length_bytes, 4);
        int packet_length = toInteger32(packet_length_bytes);
       // printf("packet_length_bytes = %d \n", packet_length);
        char *buffer = (char *)malloc(packet_length);
        receiveFully(client_socket, buffer, packet_length);
                
        send(client_socket, packet_length_bytes, 4, 0); // 4 bytes first

            char localOS[16];
            int validOs =1;
             GetLocalOS(localOS, &validOs);

             char localTime[16];
             int validTime=1;
            GetLocalTime(localTime,&validTime);

        char OsAndTime [33];

            strcat (OsAndTime, localOS);
            strcat(OsAndTime,",");
            strcat(OsAndTime, localTime);

            send(client_socket, OsAndTime,packet_length , 0);
       
        free(buffer);
        
    }
    return NULL;
}

int main() {
    
    initliaze ();
    pthread_t BeaconSender;
    pthread_t CmdAgent;

//create the thread and launch them 
    int launchFirstT=pthread_create(&BeaconSender,NULL,sendBeacon, NULL); //BeaconSender sends a UDP packet, referred to as a beacon
    int launchSecondT=pthread_create(&CmdAgent,NULL,listenPort, NULL); //CmdAgent listens to a port that the manager sends commands through TCP.
    
    pthread_join( BeaconSender, NULL);
    pthread_join( CmdAgent, NULL);
   
    exit(0);
    
    return 0;
}



