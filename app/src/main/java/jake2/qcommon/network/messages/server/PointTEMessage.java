package jake2.qcommon.network.messages.server;

import jake2.qcommon.sizebuf_t;

import java.util.Arrays;
import java.util.Collection;
import java.util.Set;

import static jake2.qcommon.Defines.*;

/**
 * TE_EXPLOSION2
 * TE_GRENADE_EXPLOSION
 * TE_GRENADE_EXPLOSION_WATER
 * TE_PLASMA_EXPLOSION
 * TE_EXPLOSION1
 * TE_EXPLOSION1_BIG
 * TE_ROCKET_EXPLOSION
 * TE_ROCKET_EXPLOSION_WATER
 * TE_EXPLOSION1_NP
 * TE_BFG_EXPLOSION
 * TE_BFG_BIGEXPLOSION
 * TE_BOSSTPORT
 * TE_PLAIN_EXPLOSION
 * TE_CHAINFIST_SMOKE
 * TE_TRACKER_EXPLOSION
 * TE_TELEPORT_EFFECT
 * TE_DBALL_GOAL
 * TE_WIDOWSPLASH
 */
public class PointTEMessage extends TEMessage {

    public static final Collection<Integer> SUBTYPES = Set.of(
            TE_EXPLOSION2,
            TE_GRENADE_EXPLOSION,
            TE_GRENADE_EXPLOSION_WATER,
            TE_PLASMA_EXPLOSION,
            TE_EXPLOSION1,
            TE_EXPLOSION1_BIG,
            TE_ROCKET_EXPLOSION,
            TE_ROCKET_EXPLOSION_WATER,
            TE_EXPLOSION1_NP,
            TE_BFG_EXPLOSION,
            TE_BFG_BIGEXPLOSION,
            TE_BOSSTPORT,
            TE_PLAIN_EXPLOSION,
            TE_CHAINFIST_SMOKE,
            TE_TRACKER_EXPLOSION,
            TE_TELEPORT_EFFECT,
            TE_DBALL_GOAL,
            TE_WIDOWSPLASH
    );

    public PointTEMessage(int style) {
        super(style);
    }

    public PointTEMessage(int style, float[] position) {
        super(style);
        this.position = position;
    }

    public float[] position;

    @Override
    protected void writeProperties(sizebuf_t buffer) {
        super.writeProperties(buffer);
        buffer.writePos(position);
    }

    @Override
    public void parse(sizebuf_t buffer) {
        this.position = new float[3];
        buffer.readPos(position);
    }

    @Override
    public int getSize() {
        return 8;
    }

    @Override
    Collection<Integer> getSupportedStyles() {
        return SUBTYPES;
    }

    @Override
    public String toString() {
        return "PointTEMessage{" +
                "position=" + Arrays.toString(position) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PointTEMessage that = (PointTEMessage) o;

        return Arrays.equals(position, that.position);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(position);
    }
}
