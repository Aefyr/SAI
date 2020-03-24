package com.aefyr.sai.billing;

public enum DonationStatus {
    UNKNOWN, PENDING, DONATED, NOT_DONATED, FLOSS_MODE, NOT_AVAILABLE;

    public boolean unlocksThemes() {
        return this == DONATED || this == FLOSS_MODE;
    }
}
