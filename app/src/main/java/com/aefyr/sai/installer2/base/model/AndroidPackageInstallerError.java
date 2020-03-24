package com.aefyr.sai.installer2.base.model;

import android.content.Context;

import androidx.annotation.StringRes;

import com.aefyr.sai.R;

/**
 * Errors were scraped from here - https://cs.android.com/android/platform/superproject/+/master:frameworks/base/core/java/android/content/pm/PackageManager.java;l=989;bpv=0;bpt=1
 */
public enum AndroidPackageInstallerError {
    UNKNOWN("UNKNOWN", 1337, R.string.installer_rootless_error2_unknown),
    INSTALL_FAILED_ALREADY_EXISTS("INSTALL_FAILED_ALREADY_EXISTS", -1, R.string.installer_rootless_error2_install_failed_already_exists),
    INSTALL_FAILED_INVALID_APK("INSTALL_FAILED_INVALID_APK", -2, R.string.installer_rootless_error2_install_failed_invalid_apk),
    INSTALL_FAILED_INVALID_URI("INSTALL_FAILED_INVALID_URI", -3, R.string.installer_rootless_error2_install_failed_invalid_uri),
    INSTALL_FAILED_INSUFFICIENT_STORAGE("INSTALL_FAILED_INSUFFICIENT_STORAGE", -4, R.string.installer_rootless_error2_install_failed_insufficient_storage),
    INSTALL_FAILED_DUPLICATE_PACKAGE("INSTALL_FAILED_DUPLICATE_PACKAGE", -5, R.string.installer_rootless_error2_install_failed_duplicate_package),
    INSTALL_FAILED_NO_SHARED_USER("INSTALL_FAILED_NO_SHARED_USER", -6, R.string.installer_rootless_error2_install_failed_no_shared_user),
    INSTALL_FAILED_UPDATE_INCOMPATIBLE("INSTALL_FAILED_UPDATE_INCOMPATIBLE", -7, R.string.installer_rootless_error2_install_failed_update_incompatible),
    INSTALL_FAILED_SHARED_USER_INCOMPATIBLE("INSTALL_FAILED_SHARED_USER_INCOMPATIBLE", -8, R.string.installer_rootless_error2_install_failed_shared_user_incompatible),
    INSTALL_FAILED_MISSING_SHARED_LIBRARY("INSTALL_FAILED_MISSING_SHARED_LIBRARY", -9, R.string.installer_rootless_error2_install_failed_missing_shared_library),
    INSTALL_FAILED_REPLACE_COULDNT_DELETE("INSTALL_FAILED_REPLACE_COULDNT_DELETE", -10, R.string.installer_rootless_error2_install_failed_replace_couldnt_delete),
    INSTALL_FAILED_DEXOPT("INSTALL_FAILED_DEXOPT", -11, R.string.installer_rootless_error2_install_failed_dexopt),
    INSTALL_FAILED_OLDER_SDK("INSTALL_FAILED_OLDER_SDK", -12, R.string.installer_rootless_error2_install_failed_older_sdk),
    INSTALL_FAILED_CONFLICTING_PROVIDER("INSTALL_FAILED_CONFLICTING_PROVIDER", -13, R.string.installer_rootless_error2_install_failed_conflicting_provider),
    INSTALL_FAILED_NEWER_SDK("INSTALL_FAILED_NEWER_SDK", -14, R.string.installer_rootless_error2_install_failed_newer_sdk),
    INSTALL_FAILED_TEST_ONLY("INSTALL_FAILED_TEST_ONLY", -15, R.string.installer_rootless_error2_install_failed_test_only),
    INSTALL_FAILED_CPU_ABI_INCOMPATIBLE("INSTALL_FAILED_CPU_ABI_INCOMPATIBLE", -16, R.string.installer_rootless_error2_install_failed_cpu_abi_incompatible),
    INSTALL_FAILED_MISSING_FEATURE("INSTALL_FAILED_MISSING_FEATURE", -17, R.string.installer_rootless_error2_install_failed_missing_feature),
    INSTALL_FAILED_CONTAINER_ERROR("INSTALL_FAILED_CONTAINER_ERROR", -18, R.string.installer_rootless_error2_install_failed_container_error),
    INSTALL_FAILED_INVALID_INSTALL_LOCATION("INSTALL_FAILED_INVALID_INSTALL_LOCATION", -19, R.string.installer_rootless_error2_install_failed_invalid_install_location),
    INSTALL_FAILED_MEDIA_UNAVAILABLE("INSTALL_FAILED_MEDIA_UNAVAILABLE", -20, R.string.installer_rootless_error2_install_failed_media_unavailable),
    INSTALL_FAILED_VERIFICATION_TIMEOUT("INSTALL_FAILED_VERIFICATION_TIMEOUT", -21, R.string.installer_rootless_error2_install_failed_verification_timeout),
    INSTALL_FAILED_VERIFICATION_FAILURE("INSTALL_FAILED_VERIFICATION_FAILURE", -22, R.string.installer_rootless_error2_install_failed_verification_failure),
    INSTALL_FAILED_PACKAGE_CHANGED("INSTALL_FAILED_PACKAGE_CHANGED", -23, R.string.installer_rootless_error2_install_failed_package_changed),
    INSTALL_FAILED_UID_CHANGED("INSTALL_FAILED_UID_CHANGED", -24, R.string.installer_rootless_error2_install_failed_uid_changed),
    INSTALL_FAILED_VERSION_DOWNGRADE("INSTALL_FAILED_VERSION_DOWNGRADE", -25, R.string.installer_rootless_error2_install_failed_version_downgrade),
    INSTALL_FAILED_PERMISSION_MODEL_DOWNGRADE("INSTALL_FAILED_PERMISSION_MODEL_DOWNGRADE", -26, R.string.installer_rootless_error2_install_failed_permission_model_downgrade),
    INSTALL_FAILED_SANDBOX_VERSION_DOWNGRADE("INSTALL_FAILED_SANDBOX_VERSION_DOWNGRADE", -27, R.string.installer_rootless_error2_install_failed_sandbox_version_downgrade),
    INSTALL_FAILED_MISSING_SPLIT("INSTALL_FAILED_MISSING_SPLIT", -28, R.string.installer_rootless_error2_install_failed_missing_split),
    INSTALL_PARSE_FAILED_NOT_APK("INSTALL_PARSE_FAILED_NOT_APK", -100, R.string.installer_rootless_error2_install_parse_failed_not_apk),
    INSTALL_PARSE_FAILED_BAD_MANIFEST("INSTALL_PARSE_FAILED_BAD_MANIFEST", -101, R.string.installer_rootless_error2_install_parse_failed_bad_manifest),
    INSTALL_PARSE_FAILED_UNEXPECTED_EXCEPTION("INSTALL_PARSE_FAILED_UNEXPECTED_EXCEPTION", -102, R.string.installer_rootless_error2_install_parse_failed_unexpected_exception),
    INSTALL_PARSE_FAILED_NO_CERTIFICATES("INSTALL_PARSE_FAILED_NO_CERTIFICATES", -103, R.string.installer_rootless_error2_install_parse_failed_no_certificates),
    INSTALL_PARSE_FAILED_INCONSISTENT_CERTIFICATES("INSTALL_PARSE_FAILED_INCONSISTENT_CERTIFICATES", -104, R.string.installer_rootless_error2_install_parse_failed_inconsistent_certificates),
    INSTALL_PARSE_FAILED_CERTIFICATE_ENCODING("INSTALL_PARSE_FAILED_CERTIFICATE_ENCODING", -105, R.string.installer_rootless_error2_install_parse_failed_certificate_encoding),
    INSTALL_PARSE_FAILED_BAD_PACKAGE_NAME("INSTALL_PARSE_FAILED_BAD_PACKAGE_NAME", -106, R.string.installer_rootless_error2_install_parse_failed_bad_package_name),
    INSTALL_PARSE_FAILED_BAD_SHARED_USER_ID("INSTALL_PARSE_FAILED_BAD_SHARED_USER_ID", -107, R.string.installer_rootless_error2_install_parse_failed_bad_shared_user_id),
    INSTALL_PARSE_FAILED_MANIFEST_MALFORMED("INSTALL_PARSE_FAILED_MANIFEST_MALFORMED", -108, R.string.installer_rootless_error2_install_parse_failed_manifest_malformed),
    INSTALL_PARSE_FAILED_MANIFEST_EMPTY("INSTALL_PARSE_FAILED_MANIFEST_EMPTY", -109, R.string.installer_rootless_error2_install_parse_failed_manifest_empty),
    INSTALL_FAILED_INTERNAL_ERROR("INSTALL_FAILED_INTERNAL_ERROR", -110, R.string.installer_rootless_error2_install_failed_internal_error),
    INSTALL_FAILED_USER_RESTRICTED("INSTALL_FAILED_USER_RESTRICTED", -111, R.string.installer_rootless_error2_install_failed_user_restricted),
    INSTALL_FAILED_DUPLICATE_PERMISSION("INSTALL_FAILED_DUPLICATE_PERMISSION", -112, R.string.installer_rootless_error2_install_failed_duplicate_permission),
    INSTALL_FAILED_NO_MATCHING_ABIS("INSTALL_FAILED_NO_MATCHING_ABIS", -113, R.string.installer_rootless_error2_install_failed_no_matching_abis),
    INSTALL_FAILED_ABORTED("INSTALL_FAILED_ABORTED", -115, R.string.installer_rootless_error2_install_failed_aborted),
    INSTALL_FAILED_INSTANT_APP_INVALID("INSTALL_FAILED_INSTANT_APP_INVALID", -116, R.string.installer_rootless_error2_install_failed_instant_app_invalid),
    INSTALL_FAILED_BAD_DEX_METADATA("INSTALL_FAILED_BAD_DEX_METADATA", -117, R.string.installer_rootless_error2_install_failed_bad_dex_metadata),
    INSTALL_FAILED_BAD_SIGNATURE("INSTALL_FAILED_BAD_SIGNATURE", -118, R.string.installer_rootless_error2_install_failed_bad_signature),
    INSTALL_FAILED_OTHER_STAGED_SESSION_IN_PROGRESS("INSTALL_FAILED_OTHER_STAGED_SESSION_IN_PROGRESS", -119, R.string.installer_rootless_error2_install_failed_other_staged_session_in_progress),
    INSTALL_FAILED_MULTIPACKAGE_INCONSISTENCY("INSTALL_FAILED_MULTIPACKAGE_INCONSISTENCY", -120, R.string.installer_rootless_error2_install_failed_multipackage_inconsistency),
    INSTALL_FAILED_WRONG_INSTALLED_VERSION("INSTALL_FAILED_WRONG_INSTALLED_VERSION", -121, R.string.installer_rootless_error2_install_failed_wrong_installed_version);

    private String mError;
    private int mLegacyCode;

    @StringRes
    private int mDescription;

    AndroidPackageInstallerError(String error, int legacyCode, @StringRes int description) {
        mError = error;
        mLegacyCode = legacyCode;
        mDescription = description;
    }

    /**
     * @return error name (maybe more like a string code lol)
     */
    public String getError() {
        return mError;
    }

    /**
     * @return "legacy" error code used in Android\'s PackageManager
     */
    public int getLegacyErrorCode() {
        return mLegacyCode;
    }

    /**
     * @return human readable error description
     */
    public String getDescription(Context context) {
        return context.getString(mDescription);
    }

}
