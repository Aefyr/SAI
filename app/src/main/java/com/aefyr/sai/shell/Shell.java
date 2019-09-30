package com.aefyr.sai.shell;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;

public interface Shell {

    boolean isAvailable();

    Result exec(Command command);

    Result exec(Command command, InputStream inputPipe);

    String makeLiteral(String arg);

    class Command {
        private ArrayList<String> mArgs = new ArrayList<>();

        public Command(String command, String... args) {
            mArgs.add(command);
            mArgs.addAll(Arrays.asList(args));
        }

        public String[] toStringArray() {
            String[] array = new String[mArgs.size()];

            for (int i = 0; i < mArgs.size(); i++)
                array[i] = mArgs.get(i);

            return array;
        }

        @NonNull
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();

            for (int i = 0; i < mArgs.size(); i++) {
                String arg = mArgs.get(i);
                sb.append(arg);
                if (i < mArgs.size() - 1)
                    sb.append(" ");
            }

            return sb.toString();
        }

        public static class Builder {
            private Command mCommand;

            public Builder(String command, String... args) {
                mCommand = new Command(command, args);
            }

            public Builder addArg(String argument) {
                mCommand.mArgs.add(argument);
                return this;
            }

            public Command build() {
                return mCommand;
            }
        }
    }

    class Result {
        Command cmd;
        public int exitCode;
        public String out;
        public String err;

        protected Result(Command cmd, int exitCode, String out, String err) {
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

}
