package jake2.qcommon.network.messages.server;

import jake2.qcommon.sizebuf_t;

/**
 * Print message to console or to the top of the screen
 */
public class PrintMessage extends ServerMessage {

    public int level;
    public String text;

    public PrintMessage() {
        super(ServerMessageType.svc_print);
    }

    public PrintMessage(int level, String text) {
        this();
        this.level = level;
        this.text = text;
    }

    @Override
    protected void writeProperties(sizebuf_t buffer) {
        buffer.writeByte((byte) level);
        buffer.writeString(text);

    }

    @Override
    public void parse(sizebuf_t buffer) {
        this.level = buffer.readByte();
        this.text = buffer.readString();
    }

    @Override
    public int getSize() {
        return 2 + text.length() + 1;
    }

    @Override
    public String toString() {
        return "PrintMessage{" + level + "=" + text + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PrintMessage that = (PrintMessage) o;

        if (level != that.level) return false;
        return text != null ? text.equals(that.text) : that.text == null;
    }

    @Override
    public int hashCode() {
        int result = level;
        result = 31 * result + (text != null ? text.hashCode() : 0);
        return result;
    }
}
