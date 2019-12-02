/**
 * ApplicationSender.java
 * Package: GobackN_BJTU
 * Author: Yantao SUN
 * Date: 2019/10/26
 * Version: V2.0
 * All rights Reserved, Designed By BJTU
 **/
package GoBackN_BJTU;

public class ApplicationSender {

    private static final int MIN_BLOCK_SIZE = 50;
    private static final int MAX_BLOCK_SIZE = 1440;

    private static int minBlockSize = MIN_BLOCK_SIZE;
    private static int maxBlockSize = MAX_BLOCK_SIZE;

    private static void setDataBlockSizeRange(int min, int max) {
        minBlockSize = min;
        maxBlockSize = max;
    }

    private static int genDataBlockSize() {
        return minBlockSize +  (int)(Math.random() * (maxBlockSize - minBlockSize));
    }

    private static void sendData() {
        RdtProto rdtSender = new RdtProto(CommEndType.COMM_SENDER);
        setDataBlockSizeRange(500, 1000);
        String receiverIpAddress = "127.0.0.1";
        rdtSender.openChannel(receiverIpAddress);

        //初始化发送数据
        byte[] data = new byte[MAX_BLOCK_SIZE];
        for (int k = 2; k < MAX_BLOCK_SIZE; k++) {
            data[k] = (byte)k;
        }

        //发送数据
        for (int i = 0; i < 50; i++) {
            //生成报文编号
            data[0] = (byte) (i % 128);
            data[1] = (byte) (i / 128);
            int dataSize = genDataBlockSize();
            rdtSender.rdtSend(data, dataSize);
            System.out.println("Send a packet, id = " + i + ", size = " + dataSize);
        }
        rdtSender.closeChannel();
    }


    public static void main(String[] args) {
        long startTime=System.currentTimeMillis();   //获取开始时间
        sendData();
        long endTime=System.currentTimeMillis(); //获取结束时间
        System.out.println("程序运行时间： "+(endTime-startTime)+"ms");
        System.out.println("Finished!");
    }
}
