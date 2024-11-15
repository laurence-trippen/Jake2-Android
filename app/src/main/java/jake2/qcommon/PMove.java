/*
 * Copyright (C) 1997-2001 Id Software, Inc.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 *
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 */

// Created on 25.01.2004 by RST.
// $Id: PMove.java,v 1.8 2006-01-21 21:53:32 salomo Exp $
package jake2.qcommon;

import jake2.qcommon.util.Math3D;

public class PMove {

    // all of the locals will be zeroed before each
    // pmove, just to make damn sure we don't have
    // any differences when running on client or server

    public static class pml_t {
        public float[] origin = { 0, 0, 0 }; // full float precision

        public float[] velocity = { 0, 0, 0 }; // full float precision

        public float[] forward = { 0, 0, 0 }, right = { 0, 0, 0 }, up = { 0, 0,
                0 };

        public float frametime;

        public csurface_t groundsurface;

        public int groundcontents;

        public float[] previous_origin = { 0, 0, 0 };

        public boolean ladder;
    }

    public static pmove_t pm;

    public static pml_t pml = new pml_t();

    // movement parameters
    public static float pm_stopspeed = 100;

    public static float pm_maxspeed = 300;

    public static float pm_duckspeed = 100;

    public static float pm_accelerate = 10;

    /*
     was initialized in spawnServerInstance:
         if (Cvar.getInstance().VariableValue("deathmatch") != 0) {
            gameImports.sv.configstrings[CS_AIRACCEL] = "" + sv_airaccelerate.value;
            PMove.pm_airaccelerate = sv_airaccelerate.value;
        } else {
            gameImports.sv.configstrings[CS_AIRACCEL] = "0";
            PMove.pm_airaccelerate = 0;
        }

     */
    public static float pm_airaccelerate = 0;

    public static float pm_wateraccelerate = 10;

    public static float pm_friction = 6;

    public static float pm_waterfriction = 1;

    public static float pm_waterspeed = 400;

    // try all single bits first
    public static int jitterbits[] = { 0, 4, 1, 2, 3, 5, 6, 7 };

    public static int offset[] = { 0, -1, 1 };


    /**
     * Slide off of the impacting object returns the blocked flags (1 = floor, 2 = step / wall)
     */
    public static void PM_ClipVelocity(float[] in, float[] normal, float[] out, float overbounce) {
        float backoff;
        float change;
        int i;

        backoff = Math3D.DotProduct(in, normal) * overbounce;

        for (i = 0; i < 3; i++) {
            change = normal[i] * backoff;
            out[i] = in[i] - change;
            if (out[i] > -Defines.MOVE_STOP_EPSILON
                    && out[i] < Defines.MOVE_STOP_EPSILON)
                out[i] = 0;
        }
    }

    /**
     * SV_FlyMove
     *
     * The basic solid body movement clip that slides along multiple planes
     * Returns the clipflags if the velocity was modified (hit something solid)
     * 1 = floor 2 = wall / step 4 = dead stop
     */
    public final static int MAX_CLIP_PLANES = 5;
    static float[] planes[] = new float[MAX_CLIP_PLANES][3];

    public static void PM_StepSlideMove_() {
        int bumpcount, numbumps;
        float[] dir = { 0, 0, 0 };
        float d;
        int numplanes;

        float[] primal_velocity = { 0, 0, 0 };
        int i, j;
        trace_t trace;
        float[] end = { 0, 0, 0 };
        float time_left;

        numbumps = 4;

        Math3D.VectorCopy(pml.velocity, primal_velocity);
        numplanes = 0;

        time_left = pml.frametime;

        for (bumpcount = 0; bumpcount < numbumps; bumpcount++) {
            for (i = 0; i < 3; i++)
                end[i] = pml.origin[i] + time_left
                        * pml.velocity[i];

            trace = pm.trace.trace(pml.origin, pm.mins,
                    pm.maxs, end);

            if (trace.allsolid) { // entity is trapped in another solid
                pml.velocity[2] = 0; // don't build up falling damage
                return;
            }

            if (trace.fraction > 0) { // actually covered some distance
                Math3D.VectorCopy(trace.endpos, pml.origin);
                numplanes = 0;
            }

            if (trace.fraction == 1)
                break; // moved the entire distance

            // save entity for contact
            if (pm.numtouch < Defines.MAXTOUCH && trace.ent != null) {
                pm.touchents[pm.numtouch] = trace.ent;
                pm.numtouch++;
            }

            time_left -= time_left * trace.fraction;

            // slide along this plane
            if (numplanes >= MAX_CLIP_PLANES) {
                // this shouldn't really happen
                Math3D.VectorCopy(Globals.vec3_origin, pml.velocity);
                break;
            }

            Math3D.VectorCopy(trace.plane.normal, planes[numplanes]);
            numplanes++;

            // modify original_velocity so it parallels all of the clip planes
            for (i = 0; i < numplanes; i++) {
                PM_ClipVelocity(pml.velocity, planes[i],
                        pml.velocity, 1.01f);
                for (j = 0; j < numplanes; j++)
                    if (j != i) {
                        if (Math3D.DotProduct(pml.velocity, planes[j]) < 0)
                            break; // not ok
                    }
                if (j == numplanes)
                    break;
            }

            if (i != numplanes) {
                // go along this plane
            } else {
                // go along the crease
                if (numplanes != 2) {
                    // Com.printf("clip velocity, numplanes == " + numplanes + "\n");
                    Math3D.VectorCopy(Globals.vec3_origin, pml.velocity);
                    break;
                }
                Math3D.CrossProduct(planes[0], planes[1], dir);
                d = Math3D.DotProduct(dir, pml.velocity);
                Math3D.VectorScale(dir, d, pml.velocity);
            }


            // if velocity is against the original velocity, stop dead
            // to avoid tiny occilations in sloping corners
            if (Math3D.DotProduct(pml.velocity, primal_velocity) <= 0) {
                Math3D.VectorCopy(Globals.vec3_origin, pml.velocity);
                break;
            }
        }

        if (pm.s.pm_time != 0) {
            Math3D.VectorCopy(primal_velocity, pml.velocity);
        }
    }

    /**
     * Each intersection will try to step over the obstruction instead of
     * sliding along it.
     *
     * Returns a new origin, velocity, and contact entity.
     * Does not modify any world state?
     */
    public static void PM_StepSlideMove() {
        float[] start_o = { 0, 0, 0 }, start_v = { 0, 0, 0 };
        float[] down_o = { 0, 0, 0 }, down_v = { 0, 0, 0 };
        trace_t trace;
        float down_dist, up_dist;
        //	float [] delta;
        float[] up = { 0, 0, 0 }, down = { 0, 0, 0 };

        Math3D.VectorCopy(pml.origin, start_o);
        Math3D.VectorCopy(pml.velocity, start_v);

        PM_StepSlideMove_();

        Math3D.VectorCopy(pml.origin, down_o);
        Math3D.VectorCopy(pml.velocity, down_v);

        Math3D.VectorCopy(start_o, up);
        up[2] += Defines.STEPSIZE;

        trace = pm.trace.trace(up, pm.mins, pm.maxs, up);
        if (trace.allsolid)
            return; // can't step up

        // try sliding above
        Math3D.VectorCopy(up, pml.origin);
        Math3D.VectorCopy(start_v, pml.velocity);

        PM_StepSlideMove_();

        // push down the final amount
        Math3D.VectorCopy(pml.origin, down);
        down[2] -= Defines.STEPSIZE;
        trace = pm.trace.trace(pml.origin, pm.mins,
                pm.maxs, down);
        if (!trace.allsolid) {
            Math3D.VectorCopy(trace.endpos, pml.origin);
        }

        Math3D.VectorCopy(pml.origin, up);

        // decide which one went farther
        down_dist = (down_o[0] - start_o[0]) * (down_o[0] - start_o[0])
                + (down_o[1] - start_o[1]) * (down_o[1] - start_o[1]);
        up_dist = (up[0] - start_o[0]) * (up[0] - start_o[0])
                + (up[1] - start_o[1]) * (up[1] - start_o[1]);

        if (down_dist > up_dist || trace.plane.normal[2] < Defines.MIN_STEP_NORMAL) {
            Math3D.VectorCopy(down_o, pml.origin);
            Math3D.VectorCopy(down_v, pml.velocity);
            return;
        }
        //!! Special case
        // if we were walking along a plane, then we need to copy the Z over
        pml.velocity[2] = down_v[2];
    }

    /**
     * Handles both ground friction and water friction.
     */
    public static void PM_Friction() {
        float vel[];
        float speed, newspeed, control;
        float friction;
        float drop;

        vel = pml.velocity;

        speed = (float) (Math.sqrt(vel[0] * vel[0] + vel[1] * vel[1] + vel[2] * vel[2]));
        if (speed < 1) {
            vel[0] = 0;
            vel[1] = 0;
            return;
        }

        drop = 0;

        // apply ground friction
        if ((pm.groundentity != null && pml.groundsurface != null &&
                0 == (pml.groundsurface.flags & Defines.SURF_SLICK))
                || (pml.ladder)) {
            friction = pm_friction;
            control = speed < pm_stopspeed ? pm_stopspeed : speed;
            drop += control * friction * pml.frametime;
        }

        // apply water friction
        if (pm.waterlevel != 0 && !pml.ladder)
            drop += speed * pm_waterfriction * pm.waterlevel
                    * pml.frametime;

        // scale the velocity
        newspeed = speed - drop;
        if (newspeed < 0) {
            newspeed = 0;
        }
        newspeed /= speed;

        vel[0] = vel[0] * newspeed;
        vel[1] = vel[1] * newspeed;
        vel[2] = vel[2] * newspeed;
    }

    /**
     * Handles user intended acceleration.
     */
    public static void PM_Accelerate(float[] wishdir, float wishspeed,
                                     float accel) {
        int i;
        float addspeed, accelspeed, currentspeed;

        currentspeed = Math3D.DotProduct(pml.velocity, wishdir);
        addspeed = wishspeed - currentspeed;
        if (addspeed <= 0)
            return;
        accelspeed = accel * pml.frametime * wishspeed;
        if (accelspeed > addspeed)
            accelspeed = addspeed;

        for (i = 0; i < 3; i++)
            pml.velocity[i] += accelspeed * wishdir[i];
    }

    /**
     * PM_AirAccelerate.
     */

    public static void PM_AirAccelerate(float[] wishdir, float wishspeed,
                                        float accel) {
        int i;
        float addspeed, accelspeed, currentspeed, wishspd = wishspeed;

        if (wishspd > 30)
            wishspd = 30;
        currentspeed = Math3D.DotProduct(pml.velocity, wishdir);
        addspeed = wishspd - currentspeed;
        if (addspeed <= 0)
            return;
        accelspeed = accel * wishspeed * pml.frametime;
        if (accelspeed > addspeed)
            accelspeed = addspeed;

        for (i = 0; i < 3; i++)
            pml.velocity[i] += accelspeed * wishdir[i];
    }

    /**
     * PM_AddCurrents.
     */
    public static void PM_AddCurrents(float[] wishvel) {
        float[] v = { 0, 0, 0 };
        float s;

        // account for ladders
        if (pml.ladder && Math.abs(pml.velocity[2]) <= 200) {
            if ((pm.viewangles[Defines.PITCH] <= -15)
                    && (pm.cmd.forwardmove > 0))
                wishvel[2] = 200;
            else if ((pm.viewangles[Defines.PITCH] >= 15)
                    && (pm.cmd.forwardmove > 0))
                wishvel[2] = -200;
            else if (pm.cmd.upmove > 0)
                wishvel[2] = 200;
            else if (pm.cmd.upmove < 0)
                wishvel[2] = -200;
            else
                wishvel[2] = 0;

            // limit horizontal speed when on a ladder
            if (wishvel[0] < -25)
                wishvel[0] = -25;
            else if (wishvel[0] > 25)
                wishvel[0] = 25;

            if (wishvel[1] < -25)
                wishvel[1] = -25;
            else if (wishvel[1] > 25)
                wishvel[1] = 25;
        }

        // add water currents
        if ((pm.watertype & Defines.MASK_CURRENT) != 0) {
            Math3D.VectorClear(v);

            if ((pm.watertype & Defines.CONTENTS_CURRENT_0) != 0)
                v[0] += 1;
            if ((pm.watertype & Defines.CONTENTS_CURRENT_90) != 0)
                v[1] += 1;
            if ((pm.watertype & Defines.CONTENTS_CURRENT_180) != 0)
                v[0] -= 1;
            if ((pm.watertype & Defines.CONTENTS_CURRENT_270) != 0)
                v[1] -= 1;
            if ((pm.watertype & Defines.CONTENTS_CURRENT_UP) != 0)
                v[2] += 1;
            if ((pm.watertype & Defines.CONTENTS_CURRENT_DOWN) != 0)
                v[2] -= 1;

            s = pm_waterspeed;
            if ((pm.waterlevel == 1) && (pm.groundentity != null))
                s /= 2;

            Math3D.VectorMA(wishvel, s, v, wishvel);
        }

        // add conveyor belt velocities
        if (pm.groundentity != null) {
            Math3D.VectorClear(v);

            if ((pml.groundcontents & Defines.CONTENTS_CURRENT_0) != 0)
                v[0] += 1;
            if ((pml.groundcontents & Defines.CONTENTS_CURRENT_90) != 0)
                v[1] += 1;
            if ((pml.groundcontents & Defines.CONTENTS_CURRENT_180) != 0)
                v[0] -= 1;
            if ((pml.groundcontents & Defines.CONTENTS_CURRENT_270) != 0)
                v[1] -= 1;
            if ((pml.groundcontents & Defines.CONTENTS_CURRENT_UP) != 0)
                v[2] += 1;
            if ((pml.groundcontents & Defines.CONTENTS_CURRENT_DOWN) != 0)
                v[2] -= 1;

            Math3D.VectorMA(wishvel, 100 /* pm.groundentity.speed */, v, wishvel);
        }
    }

    /**
     * PM_WaterMove.
     */
    public static void PM_WaterMove() {
        int i;
        float[] wishvel = { 0, 0, 0 };
        float wishspeed;
        float[] wishdir = { 0, 0, 0 };


        // user intentions
        for (i = 0; i < 3; i++)
            wishvel[i] = pml.forward[i] * pm.cmd.forwardmove
                    + pml.right[i] * pm.cmd.sidemove;

        if (0 == pm.cmd.forwardmove && 0 == pm.cmd.sidemove
                && 0 == pm.cmd.upmove)
            wishvel[2] -= 60; // drift towards bottom
        else
            wishvel[2] += pm.cmd.upmove;

        PM_AddCurrents(wishvel);

        Math3D.VectorCopy(wishvel, wishdir);
        wishspeed = Math3D.VectorNormalize(wishdir);

        if (wishspeed > pm_maxspeed) {
            Math3D.VectorScale(wishvel, pm_maxspeed / wishspeed, wishvel);
            wishspeed = pm_maxspeed;
        }
        wishspeed *= 0.5;

        PM_Accelerate(wishdir, wishspeed, pm_wateraccelerate);

        PM_StepSlideMove();
    }

    /**
     * PM_AirMove.
     */
    public static void PM_AirMove() {
        float[] wishvel = { 0, 0, 0 };
        float fmove, smove;
        float[] wishdir = { 0, 0, 0 };
        float wishspeed;
        float maxspeed;

        fmove = pm.cmd.forwardmove;
        smove = pm.cmd.sidemove;

        wishvel[0] = pml.forward[0] * fmove + pml.right[0] * smove;
        wishvel[1] = pml.forward[1] * fmove + pml.right[1] * smove;

        wishvel[2] = 0;

        PM_AddCurrents(wishvel);

        Math3D.VectorCopy(wishvel, wishdir);
        wishspeed = Math3D.VectorNormalize(wishdir);


        // clamp to server defined max speed
        maxspeed = (pm.s.pm_flags & Defines.PMF_DUCKED) != 0 ? pm_duckspeed
                : pm_maxspeed;

        if (wishspeed > maxspeed) {
            Math3D.VectorScale(wishvel, maxspeed / wishspeed, wishvel);
            wishspeed = maxspeed;
        }

        if (pml.ladder) {
            PM_Accelerate(wishdir, wishspeed, pm_accelerate);
            if (0 == wishvel[2]) {
                if (pml.velocity[2] > 0) {
                    pml.velocity[2] -= pm.s.gravity * pml.frametime;
                    if (pml.velocity[2] < 0)
                        pml.velocity[2] = 0;
                } else {
                    pml.velocity[2] += pm.s.gravity * pml.frametime;
                    if (pml.velocity[2] > 0)
                        pml.velocity[2] = 0;
                }
            }
            PM_StepSlideMove();
        } else if (pm.groundentity != null) { // walking on ground
            pml.velocity[2] = 0; //!!! this is before the accel
            PM_Accelerate(wishdir, wishspeed, pm_accelerate);

            // PGM -- fix for negative trigger_gravity fields
            //		pml.velocity[2] = 0;
            if (pm.s.gravity > 0)
                pml.velocity[2] = 0;
            else
                pml.velocity[2] -= pm.s.gravity * pml.frametime;
            // PGM
            if (0 == pml.velocity[0] && 0 == pml.velocity[1])
                return;
            PM_StepSlideMove();
        } else { // not on ground, so little effect on velocity
            if (pm_airaccelerate != 0)
                PM_AirAccelerate(wishdir, wishspeed, pm_accelerate);
            else
                PM_Accelerate(wishdir, wishspeed, 1);
            // add gravity
            pml.velocity[2] -= pm.s.gravity * pml.frametime;
            PM_StepSlideMove();
        }
    }

    /**
     * PM_CatagorizePosition.
     */
    public static void PM_CatagorizePosition() {
        float[] point = { 0, 0, 0 };
        int cont;
        trace_t trace;
        int sample1;
        int sample2;

        // if the player hull point one unit down is solid, the player
        // is on ground

        // see if standing on something solid
        point[0] = pml.origin[0];
        point[1] = pml.origin[1];
        point[2] = pml.origin[2] - 0.25f;
        if (pml.velocity[2] > 180) //!!ZOID changed from 100 to 180 (ramp
        // accel)
        {
            pm.s.pm_flags &= ~Defines.PMF_ON_GROUND;
            pm.groundentity = null;
        } else {
            trace = pm.trace.trace(pml.origin, pm.mins,
                    pm.maxs, point);
            pml.groundsurface = trace.surface;
            pml.groundcontents = trace.contents;

            if (null == trace.ent
                    || (trace.plane.normal[2] < 0.7 && !trace.startsolid)) {
                pm.groundentity = null;
                pm.s.pm_flags &= ~Defines.PMF_ON_GROUND;
            } else {
                pm.groundentity = trace.ent;
                // hitting solid ground will end a waterjump
                if ((pm.s.pm_flags & Defines.PMF_TIME_WATERJUMP) != 0) {
                    pm.s.pm_flags &= ~(Defines.PMF_TIME_WATERJUMP
                            | Defines.PMF_TIME_LAND | Defines.PMF_TIME_TELEPORT);
                    pm.s.pm_time = 0;
                }

                if (0 == (pm.s.pm_flags & Defines.PMF_ON_GROUND)) {

                    // just hit the ground
                    pm.s.pm_flags |= Defines.PMF_ON_GROUND;
                    // don't do landing time if we were just going down a slope
                    if (pml.velocity[2] < -200) {
                        pm.s.pm_flags |= Defines.PMF_TIME_LAND;
                        // don't allow another jump for a little while
                        if (pml.velocity[2] < -400)
                            pm.s.pm_time = 25;
                        else
                            pm.s.pm_time = 18;
                    }
                }
            }

            if (pm.numtouch < Defines.MAXTOUCH && trace.ent != null) {
                pm.touchents[pm.numtouch] = trace.ent;
                pm.numtouch++;
            }
        }


        // get waterlevel, accounting for ducking

        pm.waterlevel = 0;
        pm.watertype = 0;

        sample2 = (int) (pm.viewheight - pm.mins[2]);
        sample1 = sample2 / 2;

        point[2] = pml.origin[2] + pm.mins[2] + 1;
        cont = pm.pointcontents.pointcontents(point);

        if ((cont & Defines.MASK_WATER) != 0) {
            pm.watertype = cont;
            pm.waterlevel = 1;
            point[2] = pml.origin[2] + pm.mins[2] + sample1;
            cont = pm.pointcontents.pointcontents(point);
            if ((cont & Defines.MASK_WATER) != 0) {
                pm.waterlevel = 2;
                point[2] = pml.origin[2] + pm.mins[2] + sample2;
                cont = pm.pointcontents.pointcontents(point);
                if ((cont & Defines.MASK_WATER) != 0)
                    pm.waterlevel = 3;
            }
        }

    }

    /**
     * PM_CheckJump.
     */
    public static void PM_CheckJump() {
        if ((pm.s.pm_flags & Defines.PMF_TIME_LAND) != 0) {
            // hasn't been long enough since landing to jump again
            return;
        }

        if (pm.cmd.upmove < 10) { // not holding jump
            pm.s.pm_flags &= ~Defines.PMF_JUMP_HELD;
            return;
        }

        // must wait for jump to be released
        if ((pm.s.pm_flags & Defines.PMF_JUMP_HELD) != 0)
            return;

        if (pm.s.pm_type == Defines.PM_DEAD)
            return;

        if (pm.waterlevel >= 2) { // swimming, not jumping
            pm.groundentity = null;

            if (pml.velocity[2] <= -300)
                return;

            if (pm.watertype == Defines.CONTENTS_WATER)
                pml.velocity[2] = 100;
            else if (pm.watertype == Defines.CONTENTS_SLIME)
                pml.velocity[2] = 80;
            else
                pml.velocity[2] = 50;
            return;
        }

        if (pm.groundentity == null)
            return; // in air, so no effect

        pm.s.pm_flags |= Defines.PMF_JUMP_HELD;

        pm.groundentity = null;
        pml.velocity[2] += 270;
        if (pml.velocity[2] < 270)
            pml.velocity[2] = 270;
    }

    /**
     * PM_CheckSpecialMovement.
     */
    public static void PM_CheckSpecialMovement() {
        float[] spot = { 0, 0, 0 };
        int cont;
        float[] flatforward = { 0, 0, 0 };
        trace_t trace;

        if (pm.s.pm_time != 0)
            return;

        pml.ladder = false;

        // check for ladder
        flatforward[0] = pml.forward[0];
        flatforward[1] = pml.forward[1];
        flatforward[2] = 0;
        Math3D.VectorNormalize(flatforward);

        Math3D.VectorMA(pml.origin, 1, flatforward, spot);
        trace = pm.trace.trace(pml.origin, pm.mins,
                pm.maxs, spot);
        if ((trace.fraction < 1)
                && (trace.contents & Defines.CONTENTS_LADDER) != 0)
            pml.ladder = true;

        // check for water jump
        if (pm.waterlevel != 2)
            return;

        Math3D.VectorMA(pml.origin, 30, flatforward, spot);
        spot[2] += 4;
        cont = pm.pointcontents.pointcontents(spot);
        if (0 == (cont & Defines.CONTENTS_SOLID))
            return;

        spot[2] += 16;
        cont = pm.pointcontents.pointcontents(spot);
        if (cont != 0)
            return;
        // jump out of water
        Math3D.VectorScale(flatforward, 50, pml.velocity);
        pml.velocity[2] = 350;

        pm.s.pm_flags |= Defines.PMF_TIME_WATERJUMP;
        pm.s.pm_time = -1; // was 255
    }

    /**
     * PM_FlyMove.
     */
    public static void PM_FlyMove(boolean doclip) {
        float speed, drop, friction, control, newspeed;
        float currentspeed, addspeed, accelspeed;
        int i;
        float[] wishvel = { 0, 0, 0 };
        float fmove, smove;
        float[] wishdir = { 0, 0, 0 };
        float wishspeed;
        float[] end = { 0, 0, 0 };
        trace_t trace;

        pm.viewheight = 22;

        // friction

        speed = Math3D.VectorLength(pml.velocity);
        if (speed < 1) {
            Math3D.VectorCopy(Globals.vec3_origin, pml.velocity);
        } else {
            drop = 0;

            friction = pm_friction * 1.5f; // extra friction
            control = speed < pm_stopspeed ? pm_stopspeed : speed;
            drop += control * friction * pml.frametime;

            // scale the velocity
            newspeed = speed - drop;
            if (newspeed < 0)
                newspeed = 0;
            newspeed /= speed;

            Math3D.VectorScale(pml.velocity, newspeed, pml.velocity);
        }

        // accelerate
        fmove = pm.cmd.forwardmove;
        smove = pm.cmd.sidemove;

        Math3D.VectorNormalize(pml.forward);
        Math3D.VectorNormalize(pml.right);

        for (i = 0; i < 3; i++)
            wishvel[i] = pml.forward[i] * fmove + pml.right[i]
                    * smove;
        wishvel[2] += pm.cmd.upmove;

        Math3D.VectorCopy(wishvel, wishdir);
        wishspeed = Math3D.VectorNormalize(wishdir);

        // clamp to server defined max speed
        if (wishspeed > pm_maxspeed) {
            Math3D.VectorScale(wishvel, pm_maxspeed / wishspeed, wishvel);
            wishspeed = pm_maxspeed;
        }

        currentspeed = Math3D.DotProduct(pml.velocity, wishdir);
        addspeed = wishspeed - currentspeed;
        if (addspeed <= 0)
            return;
        accelspeed = pm_accelerate * pml.frametime * wishspeed;
        if (accelspeed > addspeed)
            accelspeed = addspeed;

        for (i = 0; i < 3; i++)
            pml.velocity[i] += accelspeed * wishdir[i];

        if (doclip) {
            for (i = 0; i < 3; i++)
                end[i] = pml.origin[i] + pml.frametime * pml.velocity[i];

            trace = pm.trace.trace(pml.origin, pm.mins, pm.maxs, end);

            Math3D.VectorCopy(trace.endpos, pml.origin);
        } else {
            // move
            Math3D.VectorMA(pml.origin, pml.frametime, pml.velocity, pml.origin);
        }
    }

    /**
     * Sets mins, maxs, and pm.viewheight.
     */
    public static void PM_CheckDuck() {
        trace_t trace;

        pm.mins[0] = -16;
        pm.mins[1] = -16;

        pm.maxs[0] = 16;
        pm.maxs[1] = 16;

        if (pm.s.pm_type == Defines.PM_GIB) {
            pm.mins[2] = 0;
            pm.maxs[2] = 16;
            pm.viewheight = 8;
            return;
        }

        pm.mins[2] = -24;

        if (pm.s.pm_type == Defines.PM_DEAD) {
            pm.s.pm_flags |= Defines.PMF_DUCKED;
        } else if (pm.cmd.upmove < 0 && (pm.s.pm_flags & Defines.PMF_ON_GROUND) != 0) { // duck
            pm.s.pm_flags |= Defines.PMF_DUCKED;
        } else { // stand up if possible
            if ((pm.s.pm_flags & Defines.PMF_DUCKED) != 0) {
                // try to stand up
                pm.maxs[2] = 32;
                trace = pm.trace.trace(pml.origin, pm.mins, pm.maxs, pml.origin);
                if (!trace.allsolid)
                    pm.s.pm_flags &= ~Defines.PMF_DUCKED;
            }
        }

        if ((pm.s.pm_flags & Defines.PMF_DUCKED) != 0) {
            pm.maxs[2] = 4;
            pm.viewheight = -2;
        } else {
            pm.maxs[2] = 32;
            pm.viewheight = 22;
        }
    }

    /**
     * Dead bodies have extra friction.
     */
    public static void PM_DeadMove() {
        float forward;

        if (null == pm.groundentity)
            return;

        // extra friction
        forward = Math3D.VectorLength(pml.velocity);
        forward -= 20;
        if (forward <= 0) {
            Math3D.VectorClear(pml.velocity);
        } else {
            Math3D.VectorNormalize(pml.velocity);
            Math3D.VectorScale(pml.velocity, forward, pml.velocity);
        }
    }

    public static boolean PM_GoodPosition() {
        trace_t trace;
        float[] origin = { 0, 0, 0 }, end = { 0, 0, 0 };
        int i;

        if (pm.s.pm_type == Defines.PM_SPECTATOR)
            return true;

        for (i = 0; i < 3; i++)
            origin[i] = end[i] = pm.s.origin[i] * 0.125f;
        trace = pm.trace.trace(origin, pm.mins, pm.maxs, end);

        return !trace.allsolid;
    }

    /**
     * On exit, the origin will have a value that is pre-quantized to the 0.125
     * precision of the network channel and in a valid position.
     */

    public static void PM_SnapPosition() {
        int sign[] = { 0, 0, 0 };
        int i, j, bits;
        short base[] = { 0, 0, 0 };

        // snap velocity to eigths
        for (i = 0; i < 3; i++)
            pm.s.velocity[i] = (short) (pml.velocity[i] * 8);

        for (i = 0; i < 3; i++) {
            if (pml.origin[i] >= 0)
                sign[i] = 1;
            else
                sign[i] = -1;
            pm.s.origin[i] = (short) (pml.origin[i] * 8);
            if (pm.s.origin[i] * 0.125 == pml.origin[i])
                sign[i] = 0;
        }
        Math3D.VectorCopy(pm.s.origin, base);

        // try all combinations
        for (j = 0; j < 8; j++) {
            bits = jitterbits[j];
            Math3D.VectorCopy(base, pm.s.origin);
            for (i = 0; i < 3; i++)
                if ((bits & (1 << i)) != 0)
                    pm.s.origin[i] += sign[i];

            if (PM_GoodPosition())
                return;
        }

        // go back to the last position
        Math3D.VectorCopy(pml.previous_origin, pm.s.origin);
        // Com.DPrintf("using previous_origin\n");
    }

    /**
     * Snaps the origin of the player move to 0.125 grid.
     */
    public static void PM_InitialSnapPosition() {
        int x, y, z;
        short base[] = { 0, 0, 0 };

        Math3D.VectorCopy(pm.s.origin, base);

        for (z = 0; z < 3; z++) {
            pm.s.origin[2] = (short) (base[2] + offset[z]);
            for (y = 0; y < 3; y++) {
                pm.s.origin[1] = (short) (base[1] + offset[y]);
                for (x = 0; x < 3; x++) {
                    pm.s.origin[0] = (short) (base[0] + offset[x]);
                    if (PM_GoodPosition()) {
                        pml.origin[0] = pm.s.origin[0] * 0.125f;
                        pml.origin[1] = pm.s.origin[1] * 0.125f;
                        pml.origin[2] = pm.s.origin[2] * 0.125f;
                        Math3D.VectorCopy(pm.s.origin,
                                pml.previous_origin);
                        return;
                    }
                }
            }
        }

        Com.DPrintf("Bad InitialSnapPosition\n");
    }

    /**
     * PM_ClampAngles.
     */
    public static void PM_ClampAngles() {
        short temp;
        int i;

        if ((pm.s.pm_flags & Defines.PMF_TIME_TELEPORT) != 0) {
            pm.viewangles[Defines.YAW] = Math3D
                    .SHORT2ANGLE(pm.cmd.angles[Defines.YAW]
                            + pm.s.delta_angles[Defines.YAW]);
            pm.viewangles[Defines.PITCH] = 0;
            pm.viewangles[Defines.ROLL] = 0;
        } else {
            // circularly clamp the angles with deltas
            for (i = 0; i < 3; i++) {
                temp = (short) (pm.cmd.angles[i] + pm.s.delta_angles[i]);
                pm.viewangles[i] = Math3D.SHORT2ANGLE(temp);
            }

            // don't let the player look up or down more than 90 degrees
            if (pm.viewangles[Defines.PITCH] > 89 && pm.viewangles[Defines.PITCH] < 180)
                pm.viewangles[Defines.PITCH] = 89;
            else if (pm.viewangles[Defines.PITCH] < 271 && pm.viewangles[Defines.PITCH] >= 180)
                pm.viewangles[Defines.PITCH] = 271;
        }
        Math3D.AngleVectors(pm.viewangles, pml.forward, pml.right, pml.up);
    }

    /**
     * Can be called by either the server or the client.
     */
    public static void Pmove(pmove_t pmove) {
        pm = pmove;

        // clear results
        pm.numtouch = 0;
        Math3D.VectorClear(pm.viewangles);
        pm.viewheight = 0;
        pm.groundentity = null;
        pm.watertype = 0;
        pm.waterlevel = 0;

        pml.groundsurface = null;
        pml.groundcontents = 0;

        // convert origin and velocity to float values
        pml.origin[0] = pm.s.origin[0] * 0.125f;
        pml.origin[1] = pm.s.origin[1] * 0.125f;
        pml.origin[2] = pm.s.origin[2] * 0.125f;

        pml.velocity[0] = pm.s.velocity[0] * 0.125f;
        pml.velocity[1] = pm.s.velocity[1] * 0.125f;
        pml.velocity[2] = pm.s.velocity[2] * 0.125f;

        // save old org in case we get stuck
        Math3D.VectorCopy(pm.s.origin, pml.previous_origin);

        pml.frametime = (pm.cmd.msec & 0xFF) * 0.001f;

        PM_ClampAngles();

        if (pm.s.pm_type == Defines.PM_SPECTATOR) {
            PM_FlyMove(false);
            PM_SnapPosition();
            return;
        }

        if (pm.s.pm_type >= Defines.PM_DEAD) {
            pm.cmd.forwardmove = 0;
            pm.cmd.sidemove = 0;
            pm.cmd.upmove = 0;
        }

        if (pm.s.pm_type == Defines.PM_FREEZE)
            return; // no movement at all

        // set mins, maxs, and viewheight
        PM_CheckDuck();

        if (pm.snapinitial)
            PM_InitialSnapPosition();

        // set groundentity, watertype, and waterlevel
        PM_CatagorizePosition();

        if (pm.s.pm_type == Defines.PM_DEAD)
            PM_DeadMove();

        PM_CheckSpecialMovement();

        // drop timing counter
        if (pm.s.pm_time != 0) {
            int msec;

            // TOD o bugfix cwei
            msec = pm.cmd.msec >>> 3;
            if (msec == 0)
                msec = 1;
            if (msec >= (pm.s.pm_time & 0xFF)) {
                pm.s.pm_flags &= ~(Defines.PMF_TIME_WATERJUMP
                        | Defines.PMF_TIME_LAND | Defines.PMF_TIME_TELEPORT);
                pm.s.pm_time = 0;
            } else
                pm.s.pm_time = (byte) ((pm.s.pm_time & 0xFF) - msec);
        }

        if ((pm.s.pm_flags & Defines.PMF_TIME_TELEPORT) != 0) {
            // teleport pause stays exaclty in place
        } else if ((pm.s.pm_flags & Defines.PMF_TIME_WATERJUMP) != 0) {
            // waterjump has no control, but falls
            pml.velocity[2] -= pm.s.gravity * pml.frametime;
            if (pml.velocity[2] < 0) {
                // cancel as soon as we are falling down again
                pm.s.pm_flags &= ~(Defines.PMF_TIME_WATERJUMP
                        | Defines.PMF_TIME_LAND | Defines.PMF_TIME_TELEPORT);
                pm.s.pm_time = 0;
            }

            PM_StepSlideMove();
        } else {
            PM_CheckJump();

            PM_Friction();

            if (pm.waterlevel >= 2)
                PM_WaterMove();
            else {
                float[] angles = { 0, 0, 0 };

                Math3D.VectorCopy(pm.viewangles, angles);

                if (angles[Defines.PITCH] > 180)
                    angles[Defines.PITCH] = angles[Defines.PITCH] - 360;

                angles[Defines.PITCH] /= 3;

                Math3D.AngleVectors(angles, pml.forward, pml.right, pml.up);

                PM_AirMove();
            }
        }

        // set groundentity, watertype, and waterlevel for final spot
        PM_CatagorizePosition();
        PM_SnapPosition();
    }
}
