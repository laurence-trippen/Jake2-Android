/*
 * java
 * Copyright (C) 2004
 * 
 * $Id: CL_input.java,v 1.7 2005-06-26 09:17:33 hzi Exp $
 */
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
package jake2.client;

import jake2.qcommon.Com;
import jake2.qcommon.Defines;
import jake2.qcommon.Globals;
import jake2.qcommon.exec.Cmd;
import jake2.qcommon.exec.Cvar;
import jake2.qcommon.exec.cvar_t;
import jake2.qcommon.network.messages.client.MoveMessage;
import jake2.qcommon.network.messages.client.UserInfoMessage;
import jake2.qcommon.usercmd_t;
import jake2.qcommon.util.Lib;
import jake2.qcommon.util.Math3D;

import java.util.List;

/**
 * CL_input
 */
public class CL_input {

	private static long frame_msec;

	private static long old_sys_frame_time;

	private static cvar_t cl_nodelta;

	/*
	 * ===============================================================================
	 * 
	 * KEY BUTTONS
	 * 
	 * Continuous button event tracking is complicated by the fact that two
	 * different input sources (say, mouse button 1 and the control key) can
	 * both press the same button, but the button should only be released when
	 * both of the pressing key have been released.
	 * 
	 * When a key event issues a button command (+forward, +attack, etc), it
	 * appends its key number as a parameter to the command so it can be matched
	 * up with the release.
	 * 
	 * state bit 0 is the current state of the key state bit 1 is edge triggered
	 * on the up to down transition state bit 2 is edge triggered on the down to
	 * up transition
	 * 
	 * 
	 * Key_Event (int key, qboolean down, unsigned time);
	 * 
	 * +mlook src time
	 * 
	 * ===============================================================================
	 */

	private static kbutton_t in_klook = new kbutton_t();

	private static kbutton_t in_left = new kbutton_t();

	private static kbutton_t in_right = new kbutton_t();

	private static kbutton_t in_forward = new kbutton_t();

	private static kbutton_t in_back = new kbutton_t();

	private static kbutton_t in_lookup = new kbutton_t();

	private static kbutton_t in_lookdown = new kbutton_t();

	private static kbutton_t in_moveleft = new kbutton_t();

	private static kbutton_t in_moveright = new kbutton_t();

	public static kbutton_t in_strafe = new kbutton_t();

	private static kbutton_t in_speed = new kbutton_t();

	private static kbutton_t in_use = new kbutton_t();

	private static kbutton_t in_attack = new kbutton_t();

	private static kbutton_t in_up = new kbutton_t();

	private static kbutton_t in_down = new kbutton_t();

	private static int in_impulse;

	private static void KeyDown(kbutton_t b, List<String> args) {
		int k;

		if (args.size() >= 2)
			k = Lib.atoi(args.get(1));
		else
			k = -1; // typed manually at the console for continuous down

		if (k == b.down[0] || k == b.down[1])
			return; // repeating key

		if (b.down[0] == 0)
			b.down[0] = k;
		else if (b.down[1] == 0)
			b.down[1] = k;
		else {
			Com.Printf("Three keys down for a button!\n");
			return;
		}

		if ((b.state & 1) != 0)
			return; // still down

		// save timestamp
		if (args.size() >= 3)
			b.downtime = Lib.atoi(args.get(2));
		else
			b.downtime = Globals.sys_frame_time - 100;

		b.state |= 3; // down + impulse down
	}

	private static void KeyUp(kbutton_t b, List<String> args) {
		int k;

		if (args.size() >= 2)
			k = Lib.atoi(args.get(1));
		else {
			// typed manually at the console, assume for unsticking, so clear
			// all
			b.down[0] = b.down[1] = 0;
			b.state = 4; // impulse up
			return;
		}

		if (b.down[0] == k)
			b.down[0] = 0;
		else if (b.down[1] == k)
			b.down[1] = 0;
		else
			return; // key up without coresponding down (menu pass through)
		if (b.down[0] != 0 || b.down[1] != 0)
			return; // some other key is still holding it down

		if ((b.state & 1) == 0)
			return; // still up (this should not happen)

		// save timestamp
		if (args.size() >= 3)
			b.msec += Lib.atoi(args.get(2)) - b.downtime;
		else
			b.msec += 10;

		b.state &= ~1; // now up
		b.state |= 4; // impulse up
	}

	/*
	 * =============== CL_KeyState
	 * 
	 * Returns the fraction of the frame that the key was down ===============
	 */
	private static float KeyState(kbutton_t key) {
		float val;
		long msec;

		key.state &= 1; // clear impulses

		msec = key.msec;
		key.msec = 0;

		if (key.state != 0) {
			// still down
			msec += Globals.sys_frame_time - key.downtime;
			key.downtime = Globals.sys_frame_time;
		}

		val = (float) msec / frame_msec;
		if (val < 0)
			val = 0;
		if (val > 1)
			val = 1;

		return val;
	}

	//	  ==========================================================================

	/*
	 * ================ CL_AdjustAngles
	 * 
	 * Moves the local angle positions ================
	 */
	private static void AdjustAngles() {
		float speed;
		float up, down;

		if ((in_speed.state & 1) != 0)
			speed = ClientGlobals.cls.frametime * ClientGlobals.cl_anglespeedkey.value;
		else
			speed = ClientGlobals.cls.frametime;

		if ((in_strafe.state & 1) == 0) {
			ClientGlobals.cl.viewangles[Defines.YAW] -= speed * ClientGlobals.cl_yawspeed.value * KeyState(in_right);
			ClientGlobals.cl.viewangles[Defines.YAW] += speed * ClientGlobals.cl_yawspeed.value * KeyState(in_left);
		}
		if ((in_klook.state & 1) != 0) {
			ClientGlobals.cl.viewangles[Defines.PITCH] -= speed * ClientGlobals.cl_pitchspeed.value * KeyState(in_forward);
			ClientGlobals.cl.viewangles[Defines.PITCH] += speed * ClientGlobals.cl_pitchspeed.value * KeyState(in_back);
		}

		up = KeyState(in_lookup);
		down = KeyState(in_lookdown);

		ClientGlobals.cl.viewangles[Defines.PITCH] -= speed * ClientGlobals.cl_pitchspeed.value * up;
		ClientGlobals.cl.viewangles[Defines.PITCH] += speed * ClientGlobals.cl_pitchspeed.value * down;
	}

	/*
	 * ================ CL_BaseMove
	 * 
	 * Send the intended movement message to the server ================
	 */
	private static void BaseMove(usercmd_t cmd) {
		AdjustAngles();

		//memset (cmd, 0, sizeof(*cmd));
		cmd.clear();

		Math3D.VectorCopy(ClientGlobals.cl.viewangles, cmd.angles);
		if ((in_strafe.state & 1) != 0) {
			cmd.sidemove += ClientGlobals.cl_sidespeed.value * KeyState(in_right);
			cmd.sidemove -= ClientGlobals.cl_sidespeed.value * KeyState(in_left);
		}

		cmd.sidemove += ClientGlobals.cl_sidespeed.value * KeyState(in_moveright);
		cmd.sidemove -= ClientGlobals.cl_sidespeed.value * KeyState(in_moveleft);

		cmd.upmove += ClientGlobals.cl_upspeed.value * KeyState(in_up);
		cmd.upmove -= ClientGlobals.cl_upspeed.value * KeyState(in_down);

		if ((in_klook.state & 1) == 0) {
			cmd.forwardmove += ClientGlobals.cl_forwardspeed.value * KeyState(in_forward);
			cmd.forwardmove -= ClientGlobals.cl_forwardspeed.value * KeyState(in_back);
		}

		//
		//	   adjust for speed key / running
		//
		if (((in_speed.state & 1) ^ (int) (ClientGlobals.cl_run.value)) != 0) {
			cmd.forwardmove *= 2;
			cmd.sidemove *= 2;
			cmd.upmove *= 2;
		}

	}

	private static void ClampPitch() {

		float pitch;

		pitch = Math3D.SHORT2ANGLE(ClientGlobals.cl.frame.playerstate.pmove.delta_angles[Defines.PITCH]);
		if (pitch > 180)
			pitch -= 360;

		if (ClientGlobals.cl.viewangles[Defines.PITCH] + pitch < -360)
			ClientGlobals.cl.viewangles[Defines.PITCH] += 360; // wrapped
		if (ClientGlobals.cl.viewangles[Defines.PITCH] + pitch > 360)
			ClientGlobals.cl.viewangles[Defines.PITCH] -= 360; // wrapped

		if (ClientGlobals.cl.viewangles[Defines.PITCH] + pitch > 89)
			ClientGlobals.cl.viewangles[Defines.PITCH] = 89 - pitch;
		if (ClientGlobals.cl.viewangles[Defines.PITCH] + pitch < -89)
			ClientGlobals.cl.viewangles[Defines.PITCH] = -89 - pitch;
	}

	/*
	 * ============== CL_FinishMove ==============
	 */
	private static void FinishMove(usercmd_t cmd) {
		int ms;
		int i;

		//
		//	   figure button bits
		//	
		if ((in_attack.state & 3) != 0)
			cmd.buttons |= Defines.BUTTON_ATTACK;
		in_attack.state &= ~2;

		if ((in_use.state & 3) != 0)
			cmd.buttons |= Defines.BUTTON_USE;
		in_use.state &= ~2;

		if (Key.anykeydown != 0 && ClientGlobals.cls.key_dest == Defines.key_game)
			cmd.buttons |= Defines.BUTTON_ANY;

		// send milliseconds of time to apply the move
		ms = (int) (ClientGlobals.cls.frametime * 1000);
		if (ms > 250)
			ms = 100; // time was unreasonable
		cmd.msec = (byte) ms;

		ClampPitch();
		for (i = 0; i < 3; i++)
			cmd.angles[i] = (short) Math3D.ANGLE2SHORT(ClientGlobals.cl.viewangles[i]);

		cmd.impulse = (byte) in_impulse;
		in_impulse = 0;

		// send the ambient light level at the player's current position
		cmd.lightlevel = (byte) ClientGlobals.cl_lightlevel.value;
	}

	/*
	 * ================= CL_CreateCmd =================
	 */
	private static void CreateCmd(usercmd_t cmd) {
		//usercmd_t cmd = new usercmd_t();

		frame_msec = Globals.sys_frame_time - old_sys_frame_time;
		if (frame_msec < 1)
			frame_msec = 1;
		if (frame_msec > 200)
			frame_msec = 200;

		// get basic movement from keyboard
		BaseMove(cmd);

		// allow mice or other external controllers to add to the move
		IN.Move(cmd);

		FinishMove(cmd);

		old_sys_frame_time = Globals.sys_frame_time;

		//return cmd;
	}

	/*
	 * ============ CL_InitInput ============
	 */
	static void InitInput() {
		Cmd.AddCommand("centerview", (List<String> args) -> IN.CenterView());
		Cmd.AddCommand("+moveup", (List<String> args) -> KeyDown(in_up, args));
		Cmd.AddCommand("-moveup", (List<String> args) -> KeyUp(in_up, args));
		Cmd.AddCommand("+movedown", (List<String> args) -> KeyDown(in_down, args));
		Cmd.AddCommand("-movedown", (List<String> args) -> KeyUp(in_down, args));
		Cmd.AddCommand("+left", (List<String> args) -> KeyDown(in_left, args));
		Cmd.AddCommand("-left", (List<String> args) -> KeyUp(in_left, args));
		Cmd.AddCommand("+right", (List<String> args) -> KeyDown(in_right, args));
		Cmd.AddCommand("-right", (List<String> args) -> KeyUp(in_right, args));
		Cmd.AddCommand("+forward", (List<String> args) -> KeyDown(in_forward, args));
		Cmd.AddCommand("-forward", (List<String> args) -> KeyUp(in_forward, args));
		Cmd.AddCommand("+back", (List<String> args) -> KeyDown(in_back, args));
		Cmd.AddCommand("-back", (List<String> args) -> KeyUp(in_back, args));
		Cmd.AddCommand("+lookup", (List<String> args) -> KeyDown(in_lookup, args));
		Cmd.AddCommand("-lookup", (List<String> args) -> KeyUp(in_lookup, args));
		Cmd.AddCommand("+lookdown", (List<String> args) -> KeyDown(in_lookdown, args));
		Cmd.AddCommand("-lookdown", (List<String> args) -> KeyUp(in_lookdown, args));
		Cmd.AddCommand("+strafe", (List<String> args) -> KeyDown(in_strafe, args));
		Cmd.AddCommand("-strafe", (List<String> args) -> KeyUp(in_strafe, args));
		Cmd.AddCommand("+moveleft", (List<String> args) -> KeyDown(in_moveleft, args));
		Cmd.AddCommand("-moveleft", (List<String> args) -> KeyUp(in_moveleft, args));
		Cmd.AddCommand("+moveright", (List<String> args) -> KeyDown(in_moveright, args));
		Cmd.AddCommand("-moveright", (List<String> args) -> KeyUp(in_moveright, args));
		Cmd.AddCommand("+speed", (List<String> args) -> KeyDown(in_speed, args));
		Cmd.AddCommand("-speed", (List<String> args) -> KeyUp(in_speed, args));
		Cmd.AddCommand("+attack", (List<String> args) -> KeyDown(in_attack, args));
		Cmd.AddCommand("-attack", (List<String> args) -> KeyUp(in_attack, args));
		Cmd.AddCommand("+use", (List<String> args) -> KeyDown(in_use, args));
		Cmd.AddCommand("-use", (List<String> args) -> KeyUp(in_use, args));
		Cmd.AddCommand("impulse", (List<String> args) -> in_impulse = Lib.atoi(args.get(1)));
		Cmd.AddCommand("+klook", (List<String> args) -> KeyDown(in_klook, args));
		Cmd.AddCommand("-klook", (List<String> args) -> KeyUp(in_klook, args));

		cl_nodelta = Cvar.getInstance().Get("cl_nodelta", "0", 0);
	}

	/*
	 * ================= CL_SendCmd =================
	 */
	static void SendCmd() {

		// build a command even if not connected

		// save this command off for prediction
		int cmdIndex = ClientGlobals.cls.netchan.outgoing_sequence & (Defines.CMD_BACKUP - 1);
		usercmd_t cmd = ClientGlobals.cl.cmds[cmdIndex];
		ClientGlobals.cl.cmd_time[cmdIndex] = (int) ClientGlobals.cls.realtime; // for netgraph
															 // ping calculation

		// fill the cmd
		CreateCmd(cmd);

		ClientGlobals.cl.cmd.set(cmd);

		if (ClientGlobals.cls.state == Defines.ca_disconnected || ClientGlobals.cls.state == Defines.ca_connecting)
			return;

		if (ClientGlobals.cls.state == Defines.ca_connected) {
			if (ClientGlobals.cls.netchan.reliablePending.size() != 0 || Globals.curtime - ClientGlobals.cls.netchan.last_sent > 1000)
				ClientGlobals.cls.netchan.transmit(null);
			return;
		}

		// send a userinfo update if needed
		if (Globals.userinfo_modified) {
			CL.FixUpGender();
			Globals.userinfo_modified = false;
			ClientGlobals.cls.netchan.reliablePending.add(new UserInfoMessage(Cvar.getInstance().Userinfo()));
		}


		if (cmd.buttons != 0 && ClientGlobals.cl.cinematictime > 0
				&& !ClientGlobals.cl.attractloop
				&& ClientGlobals.cls.realtime - ClientGlobals.cl.cinematictime > 1000) {
			// skip the rest of the cinematic
			SCR.nextServerCommand();
		}

		// let the server know what the last frame we
		// got was, so the next message can be delta compressed
		boolean noCompress = cl_nodelta.value != 0.0f || !ClientGlobals.cl.frame.valid || ClientGlobals.cls.demowaiting;

		// Send 3 latest commands with compression
		int oldestCmdIndex = (ClientGlobals.cls.netchan.outgoing_sequence - 2) & (Defines.CMD_BACKUP - 1);
		usercmd_t oldestCmd = ClientGlobals.cl.cmds[oldestCmdIndex];

		int oldCmdIndex = (ClientGlobals.cls.netchan.outgoing_sequence - 1) & (Defines.CMD_BACKUP - 1);
		usercmd_t oldCmd = ClientGlobals.cl.cmds[oldCmdIndex];

		int latestCmdIndex = ClientGlobals.cls.netchan.outgoing_sequence & (Defines.CMD_BACKUP - 1);
		usercmd_t latestCmd = ClientGlobals.cl.cmds[latestCmdIndex];

		//
		// deliver the message
		//
		ClientGlobals.cls.netchan.transmit(List.of(new MoveMessage(
				noCompress,
				ClientGlobals.cl.frame.serverframe,
				oldestCmd,
				oldCmd,
				latestCmd,
				ClientGlobals.cls.netchan.outgoing_sequence
		)));
	}
}
