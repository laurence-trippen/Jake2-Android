package jake2.client.render.gles1;

import android.opengl.GLES10;
import android.opengl.GLES10Ext;
import android.opengl.GLES11;
import android.opengl.GLES11Ext;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import jake2.client.render.opengl.QGL;

public class AndroidGLES1 implements QGL {

    @Override
    public void glActiveTextureARB(int texture) {
        GLES11.glActiveTexture(texture);
    }

    @Override
    public void glAlphaFunc(int func, float ref) {
        GLES11.glAlphaFunc(func, ref);
    }

    @Override
    public void glArrayElement(int index) {
        // TODO
    }

    @Override
    public void glBegin(int mode) {
        // GLES 1.x has no glBegin
    }

    @Override
    public void glBindTexture(int target, int texture) {
        GLES11.glBindTexture(target, texture);
    }

    @Override
    public void glBlendFunc(int sfactor, int dfactor) {
        GLES11.glBlendFunc(sfactor, dfactor);
    }

    @Override
    public void glClear(int mask) {
        GLES11.glClear(mask);
    }

    @Override
    public void glClearColor(float red, float green, float blue, float alpha) {
        GLES11.glClearColor(red, green, blue, alpha);
    }

    @Override
    public void glClientActiveTextureARB(int texture) {
        GLES11.glClientActiveTexture(texture);
    }

    @Override
    public void glColor3f(float red, float green, float blue) {
        GLES11.glColor4f(red, green, blue, 1f);
    }

    @Override
    public void glColor3ub(byte red, byte green, byte blue) {
        GLES11.glColor4ub(red, green, blue, (byte) 1);
    }

    @Override
    public void glColor4f(float red, float green, float blue, float alpha) {
        GLES11.glColor4f(red, green, blue, alpha);
    }

    @Override
    public void glColor4ub(byte red, byte green, byte blue, byte alpha) {
        GLES11.glColor4ub(red, green, blue, alpha);
    }

    @Override
    public void glColorPointer(int size, boolean unsigned, int stride, ByteBuffer pointer) {
        GLES11.glColorPointer(size, unsigned ? GLES11.GL_UNSIGNED_BYTE : GLES11.GL_FLOAT, stride, pointer);
    }

    @Override
    public void glColorPointer(int size, int stride, FloatBuffer pointer) {
        GLES11.glColorPointer(size, GLES11.GL_FLOAT, stride, pointer);
    }

    @Override
    public void glColorTable(int target, int internalFormat, int width, int format, int type, ByteBuffer data) {
        // TODO: Implement color-table as lookup-table with 1D OpenGL Texture
        // Article: https://chatgpt.com/c/67389a4c-a634-8004-a346-91e4133de71f
    }

    @Override
    public void glCullFace(int mode) {
        GLES11.glCullFace(mode);
    }

    @Override
    public void glDeleteTextures(IntBuffer textures) {
        // TODO: Check if (n) is correct!?
        GLES11.glDeleteTextures(1, textures);
    }

    @Override
    public void glDepthFunc(int func) {
        GLES11.glDepthFunc(func);
    }

    @Override
    public void glDepthMask(boolean flag) {
        GLES11.glDepthMask(flag);
    }

    @Override
    public void glDepthRange(double zNear, double zFar) {
        GLES11.glDepthRangef((float) zNear, (float) zFar);
    }

    @Override
    public void glDisable(int cap) {
        GLES11.glDisable(cap);
    }

    @Override
    public void glDisableClientState(int cap) {
        GLES11.glDisableClientState(cap);
    }

    @Override
    public void glDrawArrays(int mode, int first, int count) {
        GLES11.glDrawArrays(mode, first, count);
    }

    @Override
    public void glDrawBuffer(int mode) {
        // TODO: Find alternative for glDrawBuffer
    }

    @Override
    public void glDrawElements(int mode, IntBuffer indices) {
        // TODO: Implement count
        int count = 1;
        GLES11.glDrawElements(mode, count, GLES11.GL_UNSIGNED_BYTE, indices);
    }

    @Override
    public void glEnable(int cap) {
        GLES11.glEnable(cap);
    }

    @Override
    public void glEnableClientState(int cap) {
        GLES11.glEnableClientState(cap);
    }

    @Override
    public void glEnd() {
        // GLES 1.x has no glEnd
    }

    @Override
    public void glFinish() {
        GLES11.glFinish();
    }

    @Override
    public void glFlush() {
        GLES11.glFlush();
    }

    @Override
    public void glFrustum(double left, double right, double bottom, double top, double zNear, double zFar) {
        GLES11.glFrustumf(
                (float) left,
                (float) right,
                (float) bottom,
                (float) top,
                (float) zNear,
                (float) zFar);
    }

    @Override
    public int glGetError() {
        return GLES11.glGetError();
    }

    @Override
    public void glGetFloat(int pname, FloatBuffer params) {
        GLES11.glGetFloatv(pname, params);
    }

    @Override
    public String glGetString(int name) {
        return GLES11.glGetString(name);
    }

    @Override
    public void glHint(int target, int mode) {
        GLES11.glHint(target, mode);
    }

    @Override
    public void glInterleavedArrays(int format, int stride, FloatBuffer pointer) {
        // TODO: Find alternative for glInterleavedArrays
    }

    @Override
    public void glLockArraysEXT(int first, int count) {
        // TODO: Find alternative for glLockArraysEXT
    }

    @Override
    public void glLoadIdentity() {
        GLES11.glLoadIdentity();
    }

    @Override
    public void glLoadMatrix(FloatBuffer m) {
        GLES11.glLoadMatrixf(m);
    }

    @Override
    public void glMatrixMode(int mode) {
        GLES11.glMatrixMode(mode);
    }

    @Override
    public void glMultiTexCoord2f(int target, float s, float t) {
        // TODO: Check if (r and q) are correct
        GLES11.glMultiTexCoord4f(target, s, t, 0f, 0f);
    }

    @Override
    public void glOrtho(double left, double right, double bottom, double top, double zNear, double zFar) {
        GLES11.glOrthof(
                (float) left,
                (float) right,
                (float) bottom,
                (float) top,
                (float) zNear,
                (float) zFar);
    }

    @Override
    public void glPixelStorei(int pname, int param) {

    }

    @Override
    public void glPointParameterEXT(int pname, FloatBuffer pfParams) {

    }

    @Override
    public void glPointParameterfEXT(int pname, float param) {

    }

    @Override
    public void glPointSize(float size) {

    }

    @Override
    public void glPolygonMode(int face, int mode) {

    }

    @Override
    public void glPopMatrix() {

    }

    @Override
    public void glPushMatrix() {

    }

    @Override
    public void glReadPixels(int x, int y, int width, int height, int format, int type, ByteBuffer pixels) {

    }

    @Override
    public void glRotatef(float angle, float x, float y, float z) {

    }

    @Override
    public void glScalef(float x, float y, float z) {

    }

    @Override
    public void glScissor(int x, int y, int width, int height) {

    }

    @Override
    public void glShadeModel(int mode) {

    }

    @Override
    public void glTexCoord2f(float s, float t) {

    }

    @Override
    public void glTexCoordPointer(int size, int stride, FloatBuffer pointer) {

    }

    @Override
    public void glTexEnvi(int target, int pname, int param) {

    }

    @Override
    public void glTexImage2D(int target, int level, int internalformat, int width, int height, int border, int format, int type, ByteBuffer pixels) {

    }

    @Override
    public void glTexImage2D(int target, int level, int internalformat, int width, int height, int border, int format, int type, IntBuffer pixels) {

    }

    @Override
    public void glTexParameterf(int target, int pname, float param) {

    }

    @Override
    public void glTexParameteri(int target, int pname, int param) {

    }

    @Override
    public void glTexSubImage2D(int target, int level, int xoffset, int yoffset, int width, int height, int format, int type, IntBuffer pixels) {

    }

    @Override
    public void glTranslatef(float x, float y, float z) {

    }

    @Override
    public void glUnlockArraysEXT() {

    }

    @Override
    public void glVertex2f(float x, float y) {

    }

    @Override
    public void glVertex3f(float x, float y, float z) {

    }

    @Override
    public void glVertexPointer(int size, int stride, FloatBuffer pointer) {

    }

    @Override
    public void glViewport(int x, int y, int width, int height) {

    }

    @Override
    public void setSwapInterval(int interval) {

    }
}
