package jake2.android.gles;

import static android.opengl.GLES30.*;

import android.content.Context;
import android.opengl.GLSurfaceView;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import jake2.fullgame.Jake2Game;

public class Jake2GLRenderer implements GLSurfaceView.Renderer {

    private final Context context;

    private final Jake2Game game;

    public Jake2GLRenderer(Context context, Jake2Game game) {
        this.context = context;
        this.game = game;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f);

        // OpenGL caps
        // glEnable(GL_DEPTH_TEST);
        // glActiveTexture(GL_TEXTURE1);
        // glEnable(GL_BLEND);
        // glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        this.game.init();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        glViewport(0, 0, width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        this.game.update();
    }
}
