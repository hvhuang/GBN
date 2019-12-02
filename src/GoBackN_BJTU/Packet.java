package GoBackN_BJTU;

public class Packet {
    private int sequenceNumber;
    private int checksum;
    private byte []data;

    public int getChecksum() {
        return checksum;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public void setChecksum(int checksum) {
        this.checksum = checksum;
    }

    public void setSequenceNumber(int sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public void setCorrectChecksum() {
        this.checksum = countChecksum();
    }

    public int countChecksum() {
        int sum = -1;
        for (int i = 0; i < data.length; i++) {
            sum += data[i];
        }
        return sum;
    }
}
