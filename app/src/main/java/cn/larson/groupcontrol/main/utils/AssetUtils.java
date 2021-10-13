package cn.larson.groupcontrol.main.utils;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * @author Rony
 * @date 2018/8/9
 */

public class AssetUtils {

    public static String getAssetsCacheFile(Context context, String fileName, boolean rewrite) throws IOException {
        File cacheFile = new File(context.getCacheDir(), fileName);
        if (!cacheFile.exists() || rewrite) {
            InputStream inputStream = context.getAssets().open(fileName);
            FileOutputStream outputStream = new FileOutputStream(cacheFile);
            byte[] buf = new byte[1024];
            int len;
            while ((len = inputStream.read(buf)) > 0) {
                outputStream.write(buf, 0, len);
            }
            outputStream.close();
            inputStream.close();
            if (!cacheFile.canExecute()) {
                boolean b = cacheFile.setExecutable(true);
                if (!b) {
                    throw new IOException(cacheFile.getAbsolutePath() + " is not executable!");
                }
            }
        }
        return cacheFile.getAbsolutePath();
    }
}
