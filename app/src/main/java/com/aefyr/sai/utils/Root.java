package com.aefyr.sai.utils;

import android.annotation.SuppressLint;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Root {
    private static final String TAG = "SAIRoot";

    public static boolean requestRoot() {
        try {
            return exec("exit").isSuccessful();
        } catch (Exception e) {
            Log.w(TAG, "Unable to acquire root access: ");
            Log.w(TAG, e);
            return false;
        }
    }

    public static Result exec(String command) {
        return execInternal(command, null);
    }

    public static Result exec(String command, InputStream inputPipe) {
        return execInternal(command, inputPipe);
    }

    public static Session newSession() throws Exception {
        return new Session();
    }

    private static Result execInternal(String command, @Nullable InputStream inputPipe) {
        StringBuilder stdOutSb = new StringBuilder();
        StringBuilder stdErrSb = new StringBuilder();

        try {
            Process process = Runtime.getRuntime().exec(String.format("su -c %s", command));


            Thread stdOutD = writeStreamToStringBuilder(stdOutSb, process.getInputStream());
            Thread stdErrD = writeStreamToStringBuilder(stdErrSb, process.getErrorStream());

            if (inputPipe != null) {
                IOUtils.copyStream(inputPipe, process.getOutputStream());
                inputPipe.close();
                process.getOutputStream().close();
            }

            process.waitFor();
            stdOutD.join();
            stdErrD.join();

            return new Result(command, process.exitValue(), stdOutSb.toString().trim(), stdErrSb.toString().trim());
        } catch (Exception e) {
            Log.w(TAG, "Unable execute command: ");
            Log.w(TAG, e);
            return new Result(command, -1, stdOutSb.toString().trim(), stdErrSb.toString() + "\n\n<!> SAI Root Java exception: " + Utils.throwableToString(e));
        }
    }

    private static Thread writeStreamToStringBuilder(StringBuilder builder, InputStream inputStream) {
        Thread t = new Thread(() -> {
            try {
                char[] buf = new char[1024];
                int len;
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                while ((len = reader.read(buf)) > 0)
                    builder.append(buf, 0, len);

                reader.close();
            } catch (Exception e) {
                Log.wtf(TAG, e);
            }
        });
        t.start();
        return t;
    }

    public static class Result {
        String cmd;
        public int exitCode;
        public String out;
        public String err;

        private Result(String cmd, int exitCode, String out, String err) {
            this.cmd = cmd;
            this.exitCode = exitCode;
            this.out = out;
            this.err = err;
        }

        public boolean isSuccessful() {
            return exitCode == 0;
        }

        @SuppressLint("DefaultLocale")
        @NonNull
        @Override
        public String toString() {
            return String.format("Command: %s\nExit code: %d\nOut:\n%s\n=============\nErr:\n%s", cmd, exitCode, out, err);
        }
    }

    public static class Session {
        private static final String TAG = "SAIRootSession";

        private Process mSuProcess;
        private boolean mIsRootAcquired = true;
        private boolean mIsTerminated;

        private BufferedWriter mWriter;
        private BufferedReader mReader;
        private BufferedReader mErrorReader;

        private Session() throws Exception {
            mSuProcess = Runtime.getRuntime().exec("su");
            mWriter = new BufferedWriter(new OutputStreamWriter(mSuProcess.getOutputStream()));
            mReader = new BufferedReader(new InputStreamReader(mSuProcess.getInputStream()));
            mErrorReader = new BufferedReader(new InputStreamReader(mSuProcess.getErrorStream()));
            exec("echo test");
            if (!isRootAcquired() || isTerminated())
                throw new RuntimeException("Unable to acquire root access, check log above for exception that has occurred during checking root access availability");
        }

        public Result exec(String command) {
            try {
                StringBuilder sb = new StringBuilder();
                String breaker = "『BREAKER』";//Echoed after main command and used to determine when to stop reading from the stream
                mWriter.write(command + "\necho EXIT CODE HERE $? EXIT CODE HERE" + "\necho " + breaker + "\n");
                mWriter.flush();

                char[] buffer = new char[256];
                int exitCode = -1;
                while (true) {
                    sb.append(buffer, 0, mReader.read(buffer));

                    int bi = sb.indexOf(breaker);
                    if (bi != -1) {
                        sb.delete(bi, bi + breaker.length());

                        Pattern exitCodePattern = Pattern.compile("EXIT CODE HERE (\\d+) EXIT CODE HERE");
                        Matcher exitCodeMatcher = exitCodePattern.matcher(sb.toString());
                        if (exitCodeMatcher.find()) {
                            exitCode = Integer.parseInt(exitCodeMatcher.group(1));
                            sb.replace(exitCodeMatcher.start(), exitCodeMatcher.end(), "");
                        }

                        break;
                    }
                }

                return new Result(command, exitCode, sb.toString().trim(), readError());
            } catch (Exception e) {
                mIsRootAcquired = false;
                mIsTerminated = true;
                Log.w(TAG, "Unable execute command: ");
                Log.w(TAG, e);
                return new Result(command, -1, "", "Java exception: " + Utils.throwableToString(e));
            }


        }

        public String readError() {
            try {
                StringBuilder sb = new StringBuilder();
                String breaker = "『BREAKER』";
                mWriter.write("echo " + breaker + " >&2\n");
                mWriter.flush();

                char[] buffer = new char[256];
                while (true) {
                    sb.append(buffer, 0, mErrorReader.read(buffer));

                    int bi = sb.indexOf(breaker);
                    if (bi != -1) {
                        sb.delete(bi, bi + breaker.length());
                        break;
                    }
                }

                return sb.toString().trim();
            } catch (Exception e) {
                mIsRootAcquired = false;
                mIsTerminated = true;
                Log.w(TAG, "Unable execute command: ");
                Log.w(TAG, e);
            }

            return null;
        }

        public void terminate() {
            if (mIsTerminated)
                return;

            mIsTerminated = true;
            mSuProcess.destroy();
        }

        public boolean isTerminated() {
            return mIsTerminated;
        }

        public boolean isRootAcquired() {
            return mIsRootAcquired;
        }
    }
}
