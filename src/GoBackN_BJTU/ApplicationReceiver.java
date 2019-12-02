/**
 * ApplicationReceiver.java
 * Author: Yantao SUN
 * Date: 2019/10/26
 * Version: V2.0
 * All rights Reserved, Designed By BJTU
 **/
package GoBackN_BJTU;

import java.util.Scanner;

public class ApplicationReceiver {
    private static final int MIN_BLOCK_SIZE = 50;
    private static final int MAX_BLOCK_SIZE = 1440;

    private static void RecvData() {
        RdtProto rdtReceiver = new RdtProto(CommEndType.COMM_RECEIVER);
        String senderIpAddress = "127.0.0.1";
        rdtReceiver.openChannel(senderIpAddress);

        byte[] data = new byte[MAX_BLOCK_SIZE];
        while (true) {
            int len = rdtReceiver.rdtRecv(data);
            int id = data[1] * 128 + data[0];
            System.out.println("Receive a packet, id = " + id + ", size = " + len);
        }
    }

    public static void main(String[] args) {
        RecvData();
        System.out.println("Finished!");
    }
}
