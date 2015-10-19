package whi.ucla.erlab.gimbal;

import android.os.FileObserver;

import java.io.File;

/**
 * Created by arjun on 7/1/15.
 */
public class SyncObserver extends FileObserver {

    String rootPath;
    static final int mask = (FileObserver.CREATE |
            FileObserver.DELETE |
            FileObserver.DELETE_SELF |
            FileObserver.MODIFY |
            FileObserver.MOVED_FROM |
            FileObserver.MOVED_TO |
            FileObserver.MOVE_SELF);

    public SyncObserver(String root){
        super(root, mask);

        if (! root.endsWith(File.separator)){
            root += File.separator;
        }
        rootPath = root;
    }
    @Override
    public void onEvent(int i, String path) {

    }
}
