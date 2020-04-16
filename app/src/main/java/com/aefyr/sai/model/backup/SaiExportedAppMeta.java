package com.aefyr.sai.model.backup;

import com.aefyr.sai.model.common.PackageMeta;
import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.nio.charset.StandardCharsets;

public class SaiExportedAppMeta {

    public static final String META_FILE = "meta.sai_v1.json";
    public static final String ICON_FILE = "icon.png";

    @SerializedName("package")
    @Expose
    private String mPackage;

    @SerializedName("label")
    @Expose
    private String mLabel;

    @SerializedName("version_name")
    @Expose
    private String mVersionName;

    @SerializedName("version_code")
    @Expose
    private Long mVersionCode;

    @SerializedName("export_timestamp")
    @Expose
    private Long mExportTimestamp;

    private SaiExportedAppMeta() {

    }

    public static SaiExportedAppMeta fromPackageMeta(PackageMeta packageMeta, long exportTimestamp) {
        SaiExportedAppMeta appMeta = new SaiExportedAppMeta();
        appMeta.mPackage = packageMeta.packageName;
        appMeta.mLabel = packageMeta.label;
        appMeta.mVersionName = packageMeta.versionName;
        appMeta.mVersionCode = packageMeta.versionCode;
        appMeta.mExportTimestamp = exportTimestamp;

        return appMeta;
    }

    public static SaiExportedAppMeta deserialize(byte[] serializedMeta) {
        return new Gson().fromJson(new String(serializedMeta, StandardCharsets.UTF_8), SaiExportedAppMeta.class);
    }

    public String packageName() {
        return mPackage;
    }

    public String label() {
        return mLabel;
    }

    public String versionName() {
        return mVersionName;
    }

    public long versionCode() {
        return mVersionCode != null ? mVersionCode : 0;
    }

    public long exportTime() {
        return mExportTimestamp != null ? mExportTimestamp : 0;
    }

    public byte[] serialize() {
        return new Gson().toJson(this).getBytes(StandardCharsets.UTF_8);
    }

}
