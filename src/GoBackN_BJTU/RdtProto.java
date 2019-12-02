package GoBackN_BJTU;

import java.awt.event.ActionEvent;
import java.util.Timer;
import java.util.TimerTask;

public class RdtProto {
    private CommEndType commEndType;
    private UdtChannel udtChannel;

    private static int MAX_RDT_PKT_SIZE = 2048;
    private static final long timeOut = 500;
    private static int WindowSize = 15; //最大的窗口
    private int base = 0;
    private int nextSeqNum = 0;
    private int expectedSeqNum = 0;
    private int SeqNum = nextSeqNum % (2 * WindowSize);  //当前在packet中的最后一个包
    private final Thread RecvThread = new Thread(new AckRecvThread());
    private static boolean reSend = false;  //是否接收到正确的ack

    private Timer timer = new Timer();



    private static byte[][] sndpkt = new byte[WindowSize * 2][];

    public RdtProto(CommEndType endType) {
        commEndType = endType;
        udtChannel = new UdtChannel(endType);
        udtChannel.setBandwidth(1); //设置带宽为1Mbps
        udtChannel.setLossRatio(10); //设置丢包率为3%
        udtChannel.setBRT(0.5);     //设置误比特率为万分之0.5
        udtChannel.setPropDelay(5,15);
    }

    public void rdtSend(byte[] buffer, int len){
        while (true) {
            if (nextSeqNum < base + WindowSize) {
                //System.out.println("Send packet " + nextSeqNum);
                SeqNum = nextSeqNum % (2 * WindowSize);
                sndpkt[SeqNum] = null;
                sndpkt[SeqNum] = makePacket(nextSeqNum, buffer, len);
                udtChannel.udtSend(sndpkt[SeqNum], len + 4);
                System.out.println("<--Client send packet " + base);
                if (base == nextSeqNum){
                    reSend = true;
                    timer = new Timer();
                    timer.schedule(new AckTimeout(), timeOut);
                }
                nextSeqNum ++;
                break;
            }
            else {
                try {
                    Thread.sleep(3);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //ack接收
    class AckRecvThread implements Runnable {
        byte[] buffer = new byte[MAX_RDT_PKT_SIZE];
        @Override
        public void run() {
            while (true) {
                int len = udtChannel.udtRecv(buffer);
                int ackNum = buffer[0] + buffer[1] * 128;
                if (notCorrupt(buffer, len)) {
                    System.out.println("-->Client receive ack " + ackNum);
                    timer.cancel();
                    timer = new Timer();
                    timer.schedule(new AckTimeout(), timeOut);
                    base = ackNum + 1;
                    if (base == nextSeqNum) {
                        reSend = false;
                    } else {
                        reSend = true;
                    }
                }
            }
        }
    }


    //接收端
    public int rdtRecv(byte[] buffer) {
        byte[] data = new byte[MAX_RDT_PKT_SIZE];
        int len = udtChannel.udtRecv(data);
        int seq = data[0] + data[1] * 128;
        byte[] ACK = {'a', 'c', 'k'};
        if (notCorrupt(data, len) && seq == expectedSeqNum) {
            System.arraycopy(data, 4, buffer, 0, len - 4);
            System.out.println("<--Server send ack " + seq);
            udtChannel.udtSend(makePacket(expectedSeqNum, ACK, ACK.length), ACK.length + 4);
            expectedSeqNum ++;
        }
        else {
            System.out.println("<--Server send ack " + (expectedSeqNum - 1));
            udtChannel.udtSend(makePacket(expectedSeqNum - 1, ACK, ACK.length), ACK.length + 4);
            return rdtRecv(buffer);
        }
        return len - 4;
    }

    //Timeout处理
    class AckTimeout extends TimerTask {
        @Override
        public void run() {
            System.out.println("\nTimeout，Client will resend " + base + " - " + nextSeqNum);
            timer.cancel();
            timer = new Timer();
            timer.schedule(new AckTimeout(), timeOut);
            if (reSend) {
                for (int i = base; i < nextSeqNum; i++) {
                    System.out.println("<--Client send packet " + i);
                    udtChannel.udtSend(sndpkt[i % (2 * WindowSize)], sndpkt[i % (2 * WindowSize)].length);
                }
            }
        }
    }

    //打包
    private byte[] makePacket (int nextSeqNum, byte[]data, int len) {
        byte []packet = new byte[len + 4];
        packet[0] = (byte)(nextSeqNum % 128); //存nextseqnum
        packet[1] = (byte)(nextSeqNum / 128);
        packet[2] = 0;
        packet[3] = 0;
        if (len > 4) System.arraycopy(data, 0, packet, 4, len - 4);
        int sum = Checksum(packet);
        packet[2] = (byte)(sum % 128);  //存checksum
        packet[3] = (byte)(sum / 128);
        return packet;
    }

    private int Checksum(byte[] data) {
        int sum = 0;
        for (int i = 0; i < data.length; i++) {
            sum += data[i];
        }
        return sum;
    }

    //差错检查
    private boolean notCorrupt(byte[] pck, int len) {
        int sum = pck[2] + pck[3] * 128;
        pck[2] = 0;
        pck[3] = 0;
        return Checksum(pck) == sum;
    }


    public void openChannel(String peerIpAddr){
        if (commEndType == CommEndType.COMM_SENDER){
            RecvThread.start();
        }
        udtChannel.setPeerAddress(peerIpAddr);
        udtChannel.open();
    }
    public void closeChannel(){
        try{
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (commEndType == CommEndType.COMM_SENDER){
            timer.cancel();
            RecvThread.stop();
        }
        udtChannel.close();
    }
}
