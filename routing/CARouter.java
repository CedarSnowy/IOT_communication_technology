package routing;

import core.*;
import util.Tuple;

import java.util.List;
import java.util.Random;

public class CARouter extends ActiveRouter {
    private static enum states {INIT, REGRESS, WAIT_ACK}
    private static final int SIFS = 1;
    private static final int DIFS = 5;
    private static final int ORGINAL = 100;
    private static final int BUSY = MessageRouter.TRY_LATER_BUSY;
    private static final int OK = 0;
    private static Random r = new Random();   // 随机数
    private int counter = DIFS;    // 计数值
    private int crashTime = 1;  // 碰撞次数
    private Connection lastSendCon = null;
    private Message curMsg = null;
    private int waitTime = 0;


    private states state = states.INIT;

    public CARouter(Settings s) {
        super(s);
        counter = DIFS; // 初始为 DIFS
        deleteDelivered = true;
    }

    public CARouter(CARouter c) {
        super(c);
        counter = DIFS; // 初始为 DIFS
        deleteDelivered = true;
    }

    public static int binaryExpRegress(int n) {
        return (int) Math.pow(2, r.nextInt(Math.min(n, 10)));
    }

    @Override
    public void update() {
        // super.update();

        // TODO:
        Connection con = lastSendCon;
        boolean removeCurrent = false;
        /* finalize ready transfers */
        if (con != null) {
            if (con.isMessageTransferred()) {
                if (con.getMessage() != null) {
                    // transferDone(con);
                    Message m =con.getMessage();
                    con.finalizeTransfer();
                    // requestACKMessages(con);
                    if(m.isACK() && m.getTo() == getHost()&& state == states.WAIT_ACK){
                        waitTime = 0;
                        //if(m.getId() == "ack" + curMsg.getId())   
                        state = states.REGRESS;
                    }
                    else if(m.getFrom() == getHost() && !m.isACK()){
                        state = states.WAIT_ACK;
                    }
                } /* else: some other entity aborted transfer */
                removeCurrent = true;
            }
            /* remove connections that have gone down */
            else if (!con.isUp()) {
                if (con.getMessage() != null) {
                    transferAborted(con);
                    con.abortTransfer();
                }
                removeCurrent = true;
            }
        }

        if (removeCurrent) {
            // if the message being sent was holding excess buffer, free it
            if (this.getFreeBufferSize() < 0) {
                this.makeRoomForMessage(0);
            }
            sendingConnections.remove(con);
        }

        switch (state) {
            case INIT: {
                if (canStartTransfer()) {    // 缓冲区有消息发送，并且目前有连接
                    counter = DIFS;
                    state = states.REGRESS;
                    return;
                }
                break;
            }
            case REGRESS: {
                if (isTransferring()) {   // 监听到链路忙（目前在传输或者邻居在传输）
                    return;
                } else {   // 信道空闲
                    if (counter == 0) {   // 当计数器为零时
                        // 发送最早的整个数据帧并等待确认
                        int retval = sendDeliverableMessages();
                        if (retval == OK) {
                            // state = states.WAIT_ACK;
                            // return;
                        } else if (retval == ORGINAL) {
                            counter = DIFS;
                            // return;
                        } else if (retval == BUSY) {
                            crashTime++;
                            counter = binaryExpRegress(crashTime); // 二进制指数后退
                            // return;
                        }
                    } else
                        counter--;  // 计数器递减
                }

                break;
            }
            case WAIT_ACK: {
//                if (requestACKMessages(lastSendCon))
//                    state = states.REGRESS;
                waitTime++;
                if(waitTime > 50){
                    //lastSendCon.abortTransfer();
                    // lastSendCon.finalizeTransfer();
                    if(curMsg != null){
                        curMsg.setTimeOfReSend(curMsg.getTimeOfSend()+1);
                        curMsg.setReceiveTime(0);
                        addToMessages(curMsg, false);
                        if(curMsg.getTimeOfSend() > 3){
                            removeFromMessages(curMsg.getId());
                        }
                    }
                    state = states.REGRESS;
                    waitTime = 0;
                }
                break;
            }
            default:
                throw new IllegalStateException("Unexpected value: " + state);
        }

    }


    @Override
    public CARouter replicate() {
        return new CARouter(this);
    }

    /*
    尝试发送list中第一条Tuple<Message, Connection>，list一般按照FIFO顺序
     */
    protected int tryFirstMessageForConnected(List<Tuple<Message, Connection>> tuples) {
        if (tuples.size() == 0) {
            return 100;
        }

        Tuple<Message, Connection> first = tuples.get(0);
        Message m = first.getKey();
        Connection con = first.getValue();
        lastSendCon = con;
        curMsg = m;
        return startTransfer(m, con);
    }

    protected int sendDeliverableMessages() {
        List<Connection> connections = getConnections();

        if (connections.size() == 0) {
            return 100;
        }

        @SuppressWarnings(value = "unchecked")
        int retval = tryFirstMessageForConnected(sortByQueueMode(getMessagesForConnected()));

        return retval;
    }


    @Override
    public Message messageTransferred(String id, DTNHost from) {
        Message m = super.messageTransferred(id, from);

        if (((Message) m).isACK()) {
            state = states.REGRESS;
            lastSendCon = null;
        } else {
            Message ack = new Message(m.getTo(), m.getFrom(), "ack" + m.getId(), m.getSize());
            ack.setFlag(Message.FLAGS.ACK);
            ack.setReceiveTime(0);
            createNewMessage(ack);
            counter = SIFS;
            for (Connection con : getConnections()) {
                if (con.getOtherNode(getHost()) == m.getFrom()) {
                    lastSendCon = con;
                    break;
                }
            }
            // sendMessageToConnected(m);
        }
        return m;
    }

    @Override
    public int receiveMessage(Message m, DTNHost from) {
        return super.receiveMessage(m, from);
    }

//    public void dropFaildMessage() {
////        Message[] messages = getMessageCollection().toArray(new Message[0]);
////        for (int i = 0; i < messages.length; i++) {
////            int tos = messages[i].getTimeOfSend();
////            if (tos >= 5) {
////                deleteMessage(messages[i].getId(), true);
////            }
////        }
////    }


//    private void sendMessageToConnected(Message m) {
//        DTNHost host = getHost();
//
//        for (Connection con : getConnections()) {
//            DTNHost to = con.getOtherNode(getHost());
//            if (m.getTo() == to) {
//                if (con.isReadyForTransfer() && con.startTransfer(host, m) == RCV_OK) {
//                    // con.finalizeTransfer(); /* and finalize it right away */
//                }
//            }
//        }
//    }

//    public boolean requestACKMessages(Connection con) {
//        if (isTransferring()) {
//            return false;
//        }

//        DTNHost other = con.getOtherNode(getHost());
//        /* do a copy to avoid concurrent modification exceptions
//         * (startTransfer may remove messages) */
//        ArrayList<Message> temp =
//                new ArrayList<Message>(this.getMessageCollection());
//        for (Message m : temp) {
//            if (other == m.getTo() && ((Message) m).isACK()) {
//                if (startTransfer(m, con) == RCV_OK) {
//                    return true;
//                }
//            }
//        }
//        return false;
//    }

//    @Override
//    protected void transferDone(Connection con) {
//        super.transferDone(con);
//        state = states.WAIT_ACK;
//    }

}