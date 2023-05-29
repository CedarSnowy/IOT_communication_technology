//package input;
//
//import core.CAMessage;
//import core.DTNHost;
//import core.World;
//
//public class CAMessageCreateEvent  extends MessageEvent {
//    private int size;
//    private int responseSize;
//    /**
//     * Creates a message creation event with a optional response request
//     *
//     * @param from         The creator of the message
//     * @param to           Where the message is destined to
//     * @param id           ID of the message
//     * @param size         Size of the message
//     * @param responseSize Size of the requested response message or 0 if
//     *                     no response is requested
//     * @param time         Time, when the message is created
//     */
//    public CAMessageCreateEvent(int from, int to, String id, int size, int responseSize, double time) {
//        super(from,to, id, time);
//        this.size = size;
//        this.responseSize = responseSize;
//    }
//
//    @Override
//    public void processEvent(World world) {
//        DTNHost to = world.getNodeByAddress(this.toAddr);
//        DTNHost from = world.getNodeByAddress(this.fromAddr);
//
//        CAMessage m = new CAMessage(from, to, this.id, this.size);
//        m.setResponseSize(this.responseSize);
//        from.createNewMessage(m);
//    }
//
//    @Override
//    public String toString() {
//        return super.toString() + " [" + fromAddr + "->" + toAddr + "] " +
//                "size:" + size + " CREATE";
//    }
//}
