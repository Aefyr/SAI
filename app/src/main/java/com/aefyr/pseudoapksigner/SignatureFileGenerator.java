package com.aefyr.pseudoapksigner;

import java.nio.charset.StandardCharsets;

class SignatureFileGenerator {

    private ManifestGenerator mManifest;
    private String mHashingAlgorithm;

    SignatureFileGenerator(ManifestGenerator manifestGenerator) {
        mManifest = manifestGenerator;
        mHashingAlgorithm = manifestGenerator.getHashingAlgorithm();
    }

    String generate() throws Exception {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(generateHeader().toString());

        for (ManifestGenerator.ManifestEntry manifestEntry : mManifest.getEntries()) {
            ManifestGenerator.ManifestEntry sfEntry = new ManifestGenerator.ManifestEntry();
            sfEntry.setAttribute("Name", manifestEntry.getAttribute("Name"));
            sfEntry.setAttribute(mHashingAlgorithm + "-Digest", Utils.base64Encode(Utils.hash(manifestEntry.toString().getBytes(StandardCharsets.UTF_8), mHashingAlgorithm)));
            stringBuilder.append(sfEntry.toString());
        }

        return stringBuilder.toString();
    }

    private ManifestGenerator.ManifestEntry generateHeader() throws Exception {
        ManifestGenerator.ManifestEntry header = new ManifestGenerator.ManifestEntry();
        header.setAttribute("Signature-Version", "1.0");
        header.setAttribute("Created-By", Constants.GENERATOR_NAME);
        header.setAttribute(mHashingAlgorithm + "-Digest-Manifest", Utils.base64Encode(Utils.hash(mManifest.generate().getBytes(StandardCharsets.UTF_8), mHashingAlgorithm)));
        return header;
    }


}
