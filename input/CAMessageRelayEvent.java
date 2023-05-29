package input;

import core.World;

public class CAMessageRelayEvent extends MessageRelayEvent {
    /**
     * Creates a message relaying event
     *
     * @param from  Where the message comes from (at this hop)
     * @param to    Who the message goes to (at this hop)
     * @param id    ID of the message
     * @param time  Time when this event happens
     * @param stage The stage of the event (SENDING, TRANSFERRED, or ABORTED)
     */
    public CAMessageRelayEvent(int from, int to, String id, double time, int stage) {
        super(from, to, id, time, stage);
    }

    // TODO:
    @Override
    public void processEvent(World world) {
        super.processEvent(world);
    }
}
