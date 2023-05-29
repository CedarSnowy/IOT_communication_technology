//package core;
//
//public class CAMessage extends Message {
//    private int timeOfSend = 0;   // 重传次数
//    public static enum FLAGS { REQUEST, ACK};
//    private FLAGS flag;  // 标志位
//    /**
//     * Creates a new Message.
//     *
//     * @param from Who the message is (originally) from
//     * @param to   Who the message is (originally) to
//     * @param id   Message identifier (must be unique for message but
//     *             will be the same for all replicates of the message)
//     * @param size Size of the message (in bytes)
//     */
//    public CAMessage(DTNHost from, DTNHost to, String id, int size) {
//        super(from, to, id, size);
//        flag = FLAGS.REQUEST;
//    }
//
//    public int getTimeOfSend() {
//        return timeOfSend;
//    }
//
//    public void setTimeOfReSend(int timeOfSend) {
//        this.timeOfSend = timeOfSend;
//    }
//
//    public FLAGS getFlag() {
//        return flag;
//    }
//
//    public void setFlag(FLAGS flag) {
//        this.flag = flag;
//    }
//
//    public boolean isACK() { return this.flag == FLAGS.ACK; }
//}
