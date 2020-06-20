package com.aefyr.sai.billing;

import android.content.Context;
import android.graphics.drawable.Drawable;

public interface DonationStatusRenderer {

    String getText(Context context, DonationStatus donationStatus);

    Drawable getIcon(Context context, DonationStatus donationStatus);

}
