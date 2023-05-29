package input;

public class CAMessageDeleteEvent extends MessageDeleteEvent {
    /**
     * Creates a message delete event
     *
     * @param host Where to delete the message
     * @param id   ID of the message
     * @param time Time when the message is deleted
     * @param drop
     */
    public CAMessageDeleteEvent(int host, String id, double time, boolean drop) {
        super(host, id, time, drop);
    }
}
