package jake2.fullgame;

import android.content.Context;

import java.util.Arrays;
import java.util.List;

import jake2.client.CL;
import jake2.client.Key;
import jake2.client.SCR;
import jake2.qcommon.Com;
import jake2.qcommon.Defines;
import jake2.qcommon.Globals;
import jake2.qcommon.MainCommon;
import jake2.qcommon.exec.Cbuf;
import jake2.qcommon.exec.Cmd;
import jake2.qcommon.exec.Cvar;
import jake2.qcommon.filesystem.FS;
import jake2.qcommon.longjmpException;
import jake2.qcommon.network.Netchan;
import jake2.qcommon.sys.Sys;
import jake2.qcommon.sys.Timer;
import jake2.server.JakeServer;
import jake2.server.SV_MAIN;

public class Jake2Game {

    private static final String BUILDSTRING = "Java " + System.getProperty("java.version");

    private static final String CPUSTRING = System.getProperty("os.arch");

    private final Context context;

    private JakeServer serverMain;

    private int oldtime;

    public Jake2Game(Context context) {
        this.context = context;
    }

    public void init() {
        boolean dedicated = false;

        // TODO: Set dedicated here from Android App UI. Maybe possible to run android app only as server / background?

        Globals.dedicated = Cvar.getInstance().Get("dedicated", "0", Defines.CVAR_NOSET );

        if (dedicated)
            Globals.dedicated.value = 1.0f;

        final String[] args = new String[] {
                "Jake2"
        };

        this.serverMain = initServerSubsystem(args);

        this.oldtime = Timer.Milliseconds();
    }

    public void update() {
        // find time spending rendering last frame
        int newtime = Timer.Milliseconds();
        int time = newtime - this.oldtime;

        if (time > 0) {
            try {

                int adjustedTime = MainCommon.adjustTime(time);

                MainCommon.debugLogTraces();

                Cbuf.Execute();

                this.serverMain.update(adjustedTime);

                CL.Frame(adjustedTime);


            } catch (longjmpException e) {
                Com.DPrintf("longjmp exception:" + e);
            }
        }

        this.oldtime = newtime;
    }

    /**
     * This function initializes the different subsystems of
     * the game engine. The setjmp/longjmp mechanism of the original
     * was replaced with exceptions.
     * @param argsMain the original unmodified command line arguments
     * @return
     */
    private JakeServer initServerSubsystem(String[] argsMain) {
        JakeServer result = null;
        try {
            // prepare enough of the subsystems to handle
            // cvar and command buffer management
            List<String> args = Arrays.asList(argsMain);

            Cmd.Init();

            // TODO LT: Check why getInstance() was used here?
            // Cvar.getInstance().Init();
            // replaced with:
            Cvar.Init();


            Key.Init();

            // we need to add the early commands twice, because
            // a basedir or cddir needs to be set before execing
            // config files, but we want other parms to override
            // the settings of the config files
            Cbuf.AddEarlySetCommands(args, false);
            Cbuf.Execute();


            FS.InitFilesystem();

            Cbuf.reconfigure(args, false);

            FS.setCDDir(); // use cddir from config.cfg

            Cbuf.reconfigure(args, true); // reload default.cfg and config.cfg

            Globals.host_speeds= Cvar.getInstance().Get("host_speeds", "0", 0);
            Globals.developer= Cvar.getInstance().Get("developer", "0", Defines.CVAR_ARCHIVE);
            Globals.timescale= Cvar.getInstance().Get("timescale", "0", 0);
            Globals.fixedtime= Cvar.getInstance().Get("fixedtime", "0", 0);
            Globals.showtrace= Cvar.getInstance().Get("showtrace", "0", 0);
            Globals.dedicated= Cvar.getInstance().Get("dedicated", "0", Defines.CVAR_NOSET);
            String version = Globals.VERSION + " " + CPUSTRING + " " + BUILDSTRING;
            Cvar.getInstance().Get("version", version, Defines.CVAR_SERVERINFO | Defines.CVAR_NOSET);

            Netchan.Netchan_Init();	//ok

            result = new SV_MAIN(); //SV_MAIN.SV_Init();	//ok

            CL.Init();

            // add + commands from command line
            if (Cbuf.AddLateCommands(args)) {
                // if the user didn't give any commands, run default action
                if (Globals.dedicated.value == 0)
                    Cbuf.AddText("d1\n");
                else
                    Cbuf.AddText("dedicated_start\n");

                Cbuf.Execute();
            } else {
                // the user asked for something explicit
                // so drop the loading plaque
                SCR.EndLoadingPlaque();
            }

            Com.Printf("====== Quake2 Initialized ======\n\n");

            // save config when configuration is completed
            CL.WriteConfiguration();

        } catch (longjmpException e) {
            Sys.Error("Error during initialization");
        }
        return result;
    }
}
