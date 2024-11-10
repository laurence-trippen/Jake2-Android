package jake2.qcommon.network.messages.server;

import jake2.qcommon.sizebuf_t;

import java.util.Collection;

/**
 * Temp entity
 */
public abstract class TEMessage extends ServerMessage {
    public TEMessage(int style) {
        super(ServerMessageType.svc_temp_entity);
        validateStyle(style);
        this.style = style;
    }

    public int style;

    @Override
    protected void writeProperties(sizebuf_t buffer) {
        buffer.writeByte((byte) style);
    }

    @Override
    public void parse(sizebuf_t buffer) {
        this.style = buffer.readByte();
    }

    @Override
    public String toString() {
        return "TEMessage{" +
                "style=" + style +
                '}';
    }

    abstract Collection<Integer> getSupportedStyles();

    protected final void validateStyle(int style) {
        if (!getSupportedStyles().contains(style)) {
            throw new IllegalArgumentException("Wrong style for temp entity: " + style + " for class: " + this.getClass().getSimpleName());
        }
    }
}
