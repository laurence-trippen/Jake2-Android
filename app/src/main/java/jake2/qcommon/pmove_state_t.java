/*
Copyright (C) 1997-2001 Id Software, Inc.

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  

See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

*/

// Created on 31.10.2003 by RST.

package jake2.qcommon;

import jake2.qcommon.util.Math3D;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;

public class pmove_state_t {
    //	this structure needs to be communicated bit-accurate
    //	from the server to the client to guarantee that
    //	prediction stays in sync, so no floats are used.
    //	if any part of the game code modifies this struct, it
    //	will result in a prediction error of some degree.

    public int pm_type;

    public short origin[] = { 0, 0, 0 }; // 12.3
    public short velocity[] = { 0, 0, 0 }; // 12.3
    /** ducked, jump_held, etc. */
    public byte pm_flags;
    /** each unit = 8 ms. */
    public byte pm_time;
    public short gravity;
    /** add to command angles to get view direction. */
    public short delta_angles[] = { 0, 0, 0 };
    /** changed by spawns, rotating objects, and teleporters.*/

    public void clear()
    {
        pm_type = 0;
        Math3D.VectorClear(origin);
        Math3D.VectorClear(velocity);
        pm_flags = 0;
        pm_time = 0;
        gravity = 0;
        Math3D.VectorClear(delta_angles);
    }

    public void set(pmove_state_t from) {
        pm_type = from.pm_type;
        Math3D.VectorCopy(from.origin, origin);
        Math3D.VectorCopy(from.velocity, velocity);
        pm_flags = from.pm_flags;
        pm_time = from.pm_time;
        gravity = from.gravity;
        Math3D.VectorCopy(from.delta_angles, delta_angles);
    }

    public boolean equals(pmove_state_t p2) {
        return (pm_type == p2.pm_type
                && origin[0] == p2.origin[0]
                && origin[1] == p2.origin[1]
                && origin[2] == p2.origin[2]
                && velocity[0] == p2.velocity[0]
                && velocity[1] == p2.velocity[1]
                && velocity[2] == p2.velocity[2]
                && pm_flags == p2.pm_flags
                && pm_time == p2.pm_time
                && gravity == p2.gravity
                && delta_angles[0] == p2.delta_angles[0]
                && delta_angles[1] == p2.delta_angles[1]
                && delta_angles[2] == p2.delta_angles[2]);
    }

    /** Reads the playermove from the file.*/
    public void load(RandomAccessFile f) throws IOException {

        pm_type = f.readInt();

        origin[0] = f.readShort();
        origin[1] = f.readShort();
        origin[2] = f.readShort();

        velocity[0] = f.readShort();
        velocity[1] = f.readShort();
        velocity[2] = f.readShort();

        pm_flags = f.readByte();
        pm_time = f.readByte();
        gravity = f.readShort();

        f.readShort();

        delta_angles[0] = f.readShort();
        delta_angles[1] = f.readShort();
        delta_angles[2] = f.readShort();

    }

    /** Writes the playermove to the file. */
    public void write (RandomAccessFile f) throws IOException {

        f.writeInt(pm_type);

        f.writeShort(origin[0]);
        f.writeShort(origin[1]);
        f.writeShort(origin[2]);

        f.writeShort(velocity[0]);
        f.writeShort(velocity[1]);
        f.writeShort(velocity[2]);

        f.writeByte(pm_flags);
        f.writeByte(pm_time);
        f.writeShort(gravity);

        f.writeShort(0);

        f.writeShort(delta_angles[0]);
        f.writeShort(delta_angles[1]);
        f.writeShort(delta_angles[2]);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        pmove_state_t that = (pmove_state_t) o;

        if (pm_type != that.pm_type) return false;
        if (pm_flags != that.pm_flags) return false;
        if (pm_time != that.pm_time) return false;
        if (gravity != that.gravity) return false;
        if (!Arrays.equals(origin, that.origin)) return false;
        if (!Arrays.equals(velocity, that.velocity)) return false;
        return Arrays.equals(delta_angles, that.delta_angles);
    }

    @Override
    public int hashCode() {
        int result = pm_type;
        result = 31 * result + Arrays.hashCode(origin);
        result = 31 * result + Arrays.hashCode(velocity);
        result = 31 * result + (int) pm_flags;
        result = 31 * result + (int) pm_time;
        result = 31 * result + (int) gravity;
        result = 31 * result + Arrays.hashCode(delta_angles);
        return result;
    }

    @Override
    public String toString() {
        return "pmove_state_t{" +
                "pm_type=" + pm_type +
                ", origin=" + Arrays.toString(origin) +
                ", velocity=" + Arrays.toString(velocity) +
                ", pm_flags=" + pm_flags +
                ", pm_time=" + pm_time +
                ", gravity=" + gravity +
                ", delta_angles=" + Arrays.toString(delta_angles) +
                '}';
    }
}
