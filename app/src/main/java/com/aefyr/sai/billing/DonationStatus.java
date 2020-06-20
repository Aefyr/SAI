package com.aefyr.sai.billing;

public enum DonationStatus {
    UNKNOWN, PENDING, DONATED, NOT_DONATED, NOT_AVAILABLE;

    public boolean unlocksThemes() {
        return this == DONATED;
    }
}
