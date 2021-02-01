package com.aefyr.sai.shell;

import android.os.Build;
import android.util.Log;

import androidx.annotation.Nullable;

import com.aefyr.sai.utils.IOUtils;
import com.aefyr.sai.utils.Utils;

import java.io.InputStream;
import java.io.OutputStream;

import rikka.shizuku.Shizuku;
import rikka.shizuku.ShizukuRemoteProcess;

public class ShizukuShell implements Shell {
    private static final String TAG = "ShizukuShell";

    private static ShizukuShell sInstance;

    public static ShizukuShell getInstance() {
        synchronized (ShizukuShell.class) {
            return sInstance != null ? sInstance : new ShizukuShell();
        }
    }

    private ShizukuShell() {
        sInstance = this;
    }

    @Override
    public boolean isAvailable() {
        if (!Shizuku.pingBinder())
            return false;

        try {
            return exec(new Command("echo", "test")).isSuccessful();
        } catch (Exception e) {
            Log.w(TAG, "Unable to access shizuku: ");
            Log.w(TAG, e);
            return false;
        }
    }

    @Override
    public Result exec(Command command) {
        return execInternal(command, null);
    }

    @Override
    public Result exec(Command command, InputStream inputPipe) {
        return execInternal(command, inputPipe);
    }

    @Override
    public String makeLiteral(String arg) {
        return "'" + arg.replace("'", "'\\''") + "'";
    }

    private Result execInternal(Command command, @Nullable InputStream inputPipe) {
        StringBuilder stdOutSb = new StringBuilder();
        StringBuilder stdErrSb = new StringBuilder();

        try {
            Command.Builder shCommand = new Command.Builder("sh", "-c", command.toString());

            ShizukuRemoteProcess process = Shizuku.newProcess(shCommand.build().toStringArray(), null, null);

            Thread stdOutD = IOUtils.writeStreamToStringBuilder(stdOutSb, process.getInputStream());
            Thread stdErrD = IOUtils.writeStreamToStringBuilder(stdErrSb, process.getErrorStream());

            if (inputPipe != null) {
                try (OutputStream outputStream = process.getOutputStream(); InputStream inputStream = inputPipe) {
                    IOUtils.copyStream(inputStream, outputStream);
                } catch (Exception e) {
                    stdOutD.interrupt();
                    stdErrD.interrupt();

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                        process.destroyForcibly();
                    else
                        process.destroy();

                    throw new RuntimeException(e);
                }
            }

            process.waitFor();
            stdOutD.join();
            stdErrD.join();

            return new Result(command, process.exitValue(), stdOutSb.toString().trim(), stdErrSb.toString().trim());
        } catch (Exception e) {
            Log.w(TAG, "Unable execute command: ");
            Log.w(TAG, e);
            return new Result(command, -1, stdOutSb.toString().trim(), stdErrSb.toString() + "\n\n<!> SAI ShizukuShell Java exception: " + Utils.throwableToString(e));
        }
    }
}
