package com.aefyr.sai.model.backup;

import android.content.Context;

import androidx.annotation.Nullable;

import com.aefyr.sai.R;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class BackupNameFormatBuilder {

    private List<Part> mParts = new ArrayList<>();

    public BackupNameFormatBuilder(@Nullable Collection<Part> parts) {
        if (parts != null) {
            mParts.addAll(parts);
            Collections.sort(mParts);
        }
    }

    public void addPart(Part part) {
        mParts.add(part);
        Collections.sort(mParts);
    }

    public void removePart(Part part) {
        mParts.remove(part);
        Collections.sort(mParts);
    }

    public List<Part> getParts() {
        return mParts;
    }

    public String build() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < mParts.size(); i++) {
            if (i > 0)
                sb.append("_");

            Part part = mParts.get(i);

            if (part == Part.CODE)
                sb.append("(").append(part.name()).append(")");
            else
                sb.append(mParts.get(i).name());
        }

        return sb.toString();
    }

    public static BackupNameFormatBuilder fromFormatString(String formatString) {
        ArrayList<Part> parts = new ArrayList<>();

        for (Part part : Part.values()) {
            if (formatString.contains(part.name()))
                parts.add(part);
        }

        return new BackupNameFormatBuilder(parts);
    }

    public enum Part {

        NAME, VERSION, CODE, PACKAGE, TIMESTAMP;

        public String getDisplayName(Context context) {
            return context.getResources().getStringArray(R.array.name_format_parts)[ordinal()];
        }

    }
}
