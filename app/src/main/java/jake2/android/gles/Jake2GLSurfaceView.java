package jake2.android.gles;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;
import android.view.View;

public class Jake2GLSurfaceView extends GLSurfaceView implements View.OnTouchListener {

    private final Jake2GLRenderer renderer;

    public Jake2GLSurfaceView(Context context) {
        super(context);

        // Create an OpenGL ES 3.0 context
        setEGLContextClientVersion(3);

        // Set Renderer object
        this.renderer = new Jake2GLRenderer(context);
        this.setRenderer(this.renderer);

        // Set Renderer mode
        this.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        // Register Input Listener
        setOnTouchListener(this);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        // Input.x = event.getX();
        // Input.y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Input.setState(Input.TOUCH_DOWN, true);
                break;
            case MotionEvent.ACTION_MOVE:
                // Input.setState(Input.TOUCH_MOVE, true);
                break;
            case MotionEvent.ACTION_UP:
                // Input.setState(Input.TOUCH_UP, true);
                break;
        }

        return true; // Return true to indicate that the event has been consume
    }
}
