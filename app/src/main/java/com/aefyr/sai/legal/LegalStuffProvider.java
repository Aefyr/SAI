package com.aefyr.sai.legal;

public interface LegalStuffProvider {

    boolean hasPrivacyPolicy();

    String getPrivacyPolicyUrl();

    boolean hasEula();

    String getEulaUrl();

}
