package jake2.android.gles;

import static android.opengl.GLES30.*;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.Log;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class Jake2GLRenderer implements GLSurfaceView.Renderer {

    private static final String TAG = Jake2GLRenderer.class.getSimpleName();

    private final Context context;

    public Jake2GLRenderer(Context context) {
        this.context = context;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        // OpenGL caps
        // glEnable(GL_DEPTH_TEST);
        // glActiveTexture(GL_TEXTURE1);
        // glEnable(GL_BLEND);
        // glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        // Game init code here ...

        // Log.i(TAG, "GAME INIT");
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        // Game update code here

        // Log.i(TAG, "GAME UPDATE");
    }
}
