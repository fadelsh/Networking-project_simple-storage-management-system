Date: Feb 2020
# Centralized Storage Management system

## Project Description

 This project designs and implements a simplified storage management system. The system consists a number of agents and a manager. 
 An agent is a piece of software that runs on a computer and manages the storage device such as redundant array of independent disk (RAID), which is connected to the computer.
 The manager is another piece of software that runs by a system administrator who manages the storage devices across the enterprise.
 Each agent periodically reports the system health information to the manager and executes the commands from the manager. 

## Languages

1- Agent code is in C++

2- Manager code is in Java


## Funcationalites

1- Running the manager

2- Opening several consoles and run an agent on each console

3-  Each agent would be able to send beacon packets periodically every time interval in its beacon (e.g. ONE minute as specified in pdf) with a message saying that a packet was received with its agent information displayed in the following format[ID,startUptime,TimeInterval,IP,cmdPort,beacon Receive Time in ms]. I added the last field (beacon Receive) to help detecting the last time an agent was active so it helps me in detecting its health status. 

4- Upon receiving a beacon, I check if this is from a new agent (by checking if the ID is not in my active agents list OR if it's there but startup time is different), and if that's the case I print out a message saying "a new agent has joined" as specified in the pdf.

5- Upon a termination of an agent(e.g. closing the terminal for that agent). The dead agent will stop sending packets. If an agent is not active (by termination or if it crashes somehow) for 2 times the time interval(e.g., 2 MINUTES) then it will be considered dead and be removed from my active agents list.  A message will be printed indicating the death of an agent with its ID. PLEASE WAIT for a 2-3 minute to see such message.

6- Terminating one agent and run the agent again immediately and the manager should be able to detect the agent dying and resurrecting.

7- Successfully getting the TCP connection(with the corresponding agent in the cmdPort received in the beacon in UDP) and requesting and getting the local OS & local time of each agent.

8. launching the threads in C++ & Java to do all of the things mentioned above. 


## To run (please run in pyrite or any linux vm): 

1- Please uncomment line 73 if not running in pyrite. Please run it in pyrite/linux though. 

2- Run the manager first by opening a terminal and doing "javac manager.javaç" then "java manager"

3- Open one or serval terminals to run the agent(s) by doing "g++ agent.cpp -o agent -lpthread" then "./agent" 

4- Please wait to see the packets coming from the same agent every ~1 minute (or feel free to change that in the agent code to what you wish it to be in line 42).

5- Upon death of an agent, please wait for ~2-3 minutes to see the message indicating the death of the agent and its ID. 


Please ask for clarification if needed.

Thank you!!

Fadel
