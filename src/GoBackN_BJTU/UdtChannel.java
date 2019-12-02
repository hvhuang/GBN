/**
 * UdtChannel.java
 * Package: GobackN_BJTU
 * Author: Yantao SUN
 * Date: 2019/10/26
 * Version: V2.0
 * All rights Reserved, Designed By BJTU
 **/
package GoBackN_BJTU;

import java.io.IOException;
import java.net.*;
import java.util.LinkedList;
import java.util.Queue;

enum CommEndType {COMM_SENDER, COMM_RECEIVER};

public class UdtChannel {
    private final int ONE_MILLION = 1000000;
    private final String LOCAL_IP = "127.0.0.1";
    private final long QUEUE_SIZE = 100;
    private final double MIN_BANDWIDTH = 0.1;
    private final double MAX_BANDWIDTH = 10;
    private final int SENDER_LISTEN_PORT = 13500;
    private final int RECEIVER_LISTEN_PORT = 13501;

    private InetAddress peerIpAddress;   //对端的IP地址
    private int peerListenPort;             //对端的端口号
    private double lossRatio = 0.01;      //百分之一
    private double bitErrorRatio = 0.00005;   //万分之0.5
    private double propLowerDelay = 5.0; //ms
    private double propUpperDelay = 15.0; //ms
    private double bandwidth = 1.0;      //Mbps
    private DatagramSocket mySendSocket;     //我方的发送Socket
    private DatagramSocket myRecvSocket;   //我方的接收Socket

    private CommEndType commEndType;
    /*
     * LinkedList不是线程安全的。
     * 在本程序中，有两个线程同时访问packetQueue，理论上讲，应该使用互斥锁保护该队列。
     */
    private Queue<byte[]>  packetQueue = new LinkedList<byte[]>();
    private PacketSendThread packetSendThread = new PacketSendThread();

    private boolean stopSendThread = false;

    class PacketSendThread implements Runnable {
        @Override
        public void run() {
            long sendTime;    //报文发送时间
            long currentTime; //当前时间
            long sleepTime;   //sleep时间，等待下一个报文发送。

            stopSendThread = false;

            sendTime = System.currentTimeMillis();
            while (!stopSendThread) {
                currentTime = System.currentTimeMillis();
                if (currentTime > sendTime) {
                    if (!packetQueue.isEmpty()) {
                        byte[] packet = packetQueue.remove();
                        if(!isDropPacket(packet.length)) {
                            corruptPacket(packet);
                            udpSend(packet);
                        }

                        long delay = calcE2EDelay(packet.length);
                        sendTime = sendTime + delay;
                        sleepTime = delay / 2;

                    } else {
                        sendTime = currentTime;
                        sleepTime = 3;
                    }
                } else {
                    sleepTime = sendTime - currentTime;
                } //end if

                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }//end while
        }
    }

    private void startSendThread() {
        Thread t = new Thread(packetSendThread);
        t.start();
    }

    private void stopSendThread() {
        try {
            Thread.sleep(5);
            while(packetQueue.size() > 0) {
                Thread.sleep(5);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        stopSendThread = true;
    }

    public UdtChannel(CommEndType endType)  {
        //确定本方是发送端还是接收端
        commEndType = endType;

        //对端的IP地址
        try {
            peerIpAddress = InetAddress.getByName(LOCAL_IP);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        //创建发送和接收Socket
        try {
            mySendSocket = new DatagramSocket();
            if (commEndType == CommEndType.COMM_SENDER) {
                peerListenPort = RECEIVER_LISTEN_PORT;
                myRecvSocket = new DatagramSocket(SENDER_LISTEN_PORT);
            } else {
                peerListenPort = SENDER_LISTEN_PORT;
                myRecvSocket = new DatagramSocket(RECEIVER_LISTEN_PORT);
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
    }

    //打开Channel
    public void open()
    {
        startSendThread();
    }

    //关闭Channel
    public void close(){
        stopSendThread();
        mySendSocket.close();
        myRecvSocket.close();
    }

    public void setPeerAddress(String ipAddress) {
        try {
            peerIpAddress = InetAddress.getByName(ipAddress);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    //设置信道的丢包率
    public void setLossRatio(double lossRatio) {
        this.lossRatio = lossRatio / 100;
    }

    //设置信道的误比特率
    public void setBRT(double brt) {
        bitErrorRatio = brt / 10000;
    }

    //设置信道的传输延迟
    void setPropDelay(double lower, float upper) {
        propLowerDelay = lower;
        propUpperDelay = upper;
    }

    //设置信道的传输宽带
    void setBandwidth(double bandwidth){
        if (bandwidth < MIN_BANDWIDTH || bandwidth > MAX_BANDWIDTH) {
            return;
        }
        this.bandwidth = bandwidth;
    }

    //计算是否丢包
    private boolean  isDropPacket(int packetSize){  //packetSize 单位：byte
        if (Math.random() < lossRatio) {
            return true;
        }
        return false;
    }

    private void corruptPacket( byte[] packet){  //packetSize 单位：byte
        double packetErrorRatio = 1 - Math.pow(1 - bitErrorRatio, (packet.length * 8));

        //corrupt packet
        if (Math.random() < packetErrorRatio) {
            if (packet[1] == 0) {
                packet[0] = (byte)(-packet[0]);
            } else {
                packet[1] = (byte) (-packet[1]);
            }
            packet[2] = (byte)(Math.random()*256);
            packet[packet.length - 1] = (byte)(Math.random()*256);
        }
    }

    //计算端到端延迟 = 发送延迟 + 传播延迟，单位：毫秒, 忽略了排队和处理延迟等。
    private long calcE2EDelay(int packetSize) {
        double transmissionDelay = (packetSize * 8) / (bandwidth * ONE_MILLION) * 1000;
        double propagationDelay = propLowerDelay +  Math.random() * (propUpperDelay - propLowerDelay);
        return (long)(transmissionDelay + propagationDelay);
    }


    //传输层发送数据
    public void udtSend(byte [] data, int length) {
        byte[] packet = new byte[length];
        System.arraycopy(data, 0, packet,0, length);
        while(true) {
            if (packetQueue.size() > QUEUE_SIZE) {
                try {
                    Thread.sleep(3);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }
            packetQueue.add(packet);
            break;
        }
    }

    //
    private void udpSend(byte[] packet){
        DatagramPacket dataPacket = new DatagramPacket(packet, packet.length, peerIpAddress, peerListenPort);
        try {
            mySendSocket.send(dataPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //接收数据
    public int udtRecv(byte[] buffer)  {
        //新建数据包，会把后面收到的内容放到buffer字节数组里，最大长度为buffer.length
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        try {
            //此方法为阻塞方法,直到监听到数据包后才会往下执行
            myRecvSocket.receive(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return packet.getLength();
    }

}
