import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.net.ServerSocket;

import java.io.*; 
import java.util.*; 
import java.text.SimpleDateFormat;

public class manager {

    static final int BUFFSIZE = 1024;
    static int cmdPort; 
    protected static ArrayList <String[]> acativeAgents=new ArrayList<String[]>(); //list of active agents
    static byte[] buffer= new byte[BUFFSIZE];
    static DatagramSocket ds;
    static DatagramPacket dp = null;

//indices for the recieved UDP packet
    static final int IDIDX=0;
    static final int STARTUPTIMEIDX=1;
    static final int TIMEINTERVALIDX=2;
    static final int IPIDX=3;
    static final int CMDPORTIDX=4;
    static final int BEACONARRIVETIMEIDX=5;

    static final int UDPPORT=4321; 



    public static boolean isIDInList(ArrayList <String[]> lst, String id){
        for (int i=0; i< lst.size(); i++){
            String []strArr=lst.get(i);
            if(strArr[0].equals(id)){
                return true;
            }
        }
        return false;
    }

    public static boolean isTimeInList(ArrayList <String[]> lst, String startUpTime){
        for (int i=0; i< lst.size(); i++){
            String []strArr=lst.get(i);
            if(strArr[1].equals(startUpTime)){
                return true;
            }
        }
        return false;
    }

    public static String[] addRecieveTime(String arr[], String element){

     List<String> arrlist = new ArrayList<String>(Arrays.asList(arr)); 
     arrlist.add(element);
     arr=arrlist.toArray(arr);

    return arr;
}


public static StringBuilder convert(byte[] buf){
        if (buf == null){
            return null;
    }
        StringBuilder ret = new StringBuilder();
        int i = 0;
        while (buf[i] != 0){
            ret.append((char) buf[i]);
            i++;
        }
        return ret;
    }

public static int findIdx (ArrayList <String[]> lst, String targetID){
    int i;
    for (i=0;  i<lst.size(); i++){
        String []strArr=lst.get(i);
        if(strArr[0].equals(targetID)){
            break;
        }
    }
    return i;
}

   static class BeaconListener implements Runnable  { 
        @Override
        public  void run() { 
            try{ 
               ds= new DatagramSocket(UDPPORT); 
   
            //listens to a UDP port for beacons from all agents
            for(;;){
            dp = new DatagramPacket(buffer, buffer.length); 
            ds.receive(dp);

        StringBuilder UDPpacket = convert(buffer);

        String  p = UDPpacket.toString();

            String []packets=p.split(",");
          //  System.out.println(packets.length);

            String[] fullPackets=new String[packets.length+1];

            System.arraycopy(packets, 0, fullPackets, 0, packets.length);

            long t=  System. currentTimeMillis();
            String strLong = Long.toString(t);
            fullPackets[fullPackets.length-1]=strLong;


            //checks if this is from a new agent, in which case it prints a message to inform the system administrator
                if(!isIDInList (acativeAgents,fullPackets[IDIDX]) || (isIDInList (acativeAgents,fullPackets[IDIDX])==true && isTimeInList(acativeAgents,fullPackets[STARTUPTIMEIDX]) ==false )){

                acativeAgents.add(fullPackets); // since it's new, we add it to the active agents list
                System.out.println("A new agent has joined"); 
                cmdPort=Integer.parseInt(fullPackets[CMDPORTIDX]);

                //When a new agent is detected, a new thread CmdAgent is launched to execute two commands, GetLocalOS() and GetLocalTime(), and print out the execution result.
                Thread clientAgent=new Thread(new ClientAgent()); 
                clientAgent.start(); 
            }
            //otherwise, this beacon is not from a new agent. Needs to update last beacon recived time for this agent
            else{
                int index=findIdx(acativeAgents, fullPackets[IDIDX]);
                acativeAgents.get(index)[BEACONARRIVETIMEIDX]=strLong;
                
            }

            System.out.println("Recieved packet[ID,startUptime,TimeInterval,IP,cmdPort,beacon Recieve Time in ms]: " + Arrays.toString(fullPackets));
            System.out.println();
            
            buffer = new byte[BUFFSIZE];
          
        }     
     } catch (Exception e) { 
                System.out.println ("Exception in BeaconListener"); 
            }
        }
    }
    
   static class AgentMonitor implements Runnable  { 
        @Override
        public void run() { 
            try{
                Thread.sleep(9000);

                //AgentMonitor maintains a list of active agents. Periodically it checks each agent for its heath status.
                // If the time since it receives the last beacon from the agent exceeds 2 times of the timeInterval that it specifies in its beacon, the agent is considered dead.
                // In this case, AgentMonitor prints out an alert message to inform the system administrator.
            for(;;){
                for(int i=0; i<acativeAgents.size(); i++){
                   Long beaconLastUpdate= Long.parseLong(acativeAgents.get(i)[BEACONARRIVETIMEIDX]);
                   int TimeInterval= Integer.parseInt(acativeAgents.get(i)[TIMEINTERVALIDX]);
            
                    if(System.currentTimeMillis() - beaconLastUpdate > 2 * TimeInterval* 1000){
                        System.out.println ("An agent with ID "+ acativeAgents.get(i)[0]+ " has DIED because it was not active for a time that's 2 times (2 minutes) of the timeInterval that is specified in its beacon! ");
                        acativeAgents.remove(i);
                        System.out.println();
                    }
                }    
        }
        } catch (Exception e){
            System.out.println ("Exception in AgentMonitor"); 
        }
    }
    }

    
    static class ClientAgent implements Runnable  { 
        @Override
        public void run() { 
            try{

                // This thread handles sending command GetLocalOS() & GetLocalTime() to the corresponding client and display results 
            Socket TCPSocket=new Socket("127.0.0.1", cmdPort);
            System.out.println("A TCP connection  has been established in port "+cmdPort); 
            DataInputStream inStream  = new DataInputStream(TCPSocket.getInputStream());
            DataOutputStream outStream = new DataOutputStream(TCPSocket.getOutputStream());

            String localOsAndLocalTime="getLocalOsOfOperatingsgetLocalTimeSysmClocksRequestCmd  ";

            byte[]buff=localOsAndLocalTime.getBytes();
            byte[] bufLengthInBinary = toBytes(buff.length);
            outStream.write(bufLengthInBinary, 0, bufLengthInBinary.length);
            outStream.write(buff, 0, buff.length);

            outStream.flush();
                
            inStream.readFully(bufLengthInBinary); 
            inStream.readFully(buff); 
         
            // convert the binary bytes to string
            String ret = new String(buff);

            String [] cmdResults=ret.split(",");

            
            System.out.println("The local OS of this agent: "+ cmdResults[0]);
            System.out.println("The local TIME of this agent: "+ cmdResults[1]);


            }catch (Exception e){
                System.out.println("Exception in clientAgent");
            }

        }
        
        //The below is the sample code from Canvas  
        static private byte[] toBytes(int i) {
            byte[] result = new byte[4];
    
            result[0] = (byte) (i >> 24);
            result[1] = (byte) (i >> 16);
            result[2] = (byte) (i >> 8);
            result[3] = (byte) (i /*>> 0*/);
           
            return result;
        }
    }
    
	public static void main(String[] args) throws Exception {

    Thread beaconListner=new Thread(new BeaconListener());
    Thread agentMonitor=new Thread(new AgentMonitor());

    beaconListner.start();
    agentMonitor.start();

    //beaconListner.join();
    //agentMonitor.join();

    }
}
