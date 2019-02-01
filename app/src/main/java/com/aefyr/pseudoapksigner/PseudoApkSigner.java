package com.aefyr.pseudoapksigner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class PseudoApkSigner {
    private static final String HASHING_ALGORITHM = "SHA1";

    private RSAPrivateKey mPrivateKey;
    private File mTemplateFile;

    public PseudoApkSigner(File template, File privateKey) throws Exception {
        mTemplateFile = template;
        mPrivateKey = readPrivateKey(privateKey);
    }

    public void sign(File apkFile, File output) throws Exception {
        ManifestGenerator manifest = new ManifestGenerator(apkFile, HASHING_ALGORITHM);
        SignatureFileGenerator signature = new SignatureFileGenerator(manifest);

        ZipFile apkZipFile = new ZipFile(apkFile);

        ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(output));
        Enumeration<? extends ZipEntry> zipEntries = apkZipFile.entries();
        while (zipEntries.hasMoreElements()) {
            ZipEntry zipEntry = zipEntries.nextElement();

            if (zipEntry.getName().toLowerCase().startsWith("meta-inf"))
                continue;

            InputStream entryInputStream = apkZipFile.getInputStream(zipEntry);

            zipOutputStream.putNextEntry(new ZipEntry(zipEntry.getName()));
            Utils.copyStream(entryInputStream, zipOutputStream);
            zipOutputStream.closeEntry();
        }

        zipOutputStream.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
        zipOutputStream.write(manifest.generate().getBytes(StandardCharsets.UTF_8));
        zipOutputStream.closeEntry();

        zipOutputStream.putNextEntry(new ZipEntry("META-INF/TESTKEY.SF"));
        zipOutputStream.write(signature.generate().getBytes(StandardCharsets.UTF_8));
        zipOutputStream.closeEntry();

        zipOutputStream.putNextEntry(new ZipEntry("META-INF/TESTKEY.RSA"));
        zipOutputStream.write(readFile(mTemplateFile));
        zipOutputStream.write(sign(mPrivateKey, signature.generate().getBytes(StandardCharsets.UTF_8)));
        zipOutputStream.closeEntry();

        apkZipFile.close();
        zipOutputStream.close();
    }

    private byte[] sign(PrivateKey privateKey, byte[] message) throws Exception {
        Signature sign = Signature.getInstance(HASHING_ALGORITHM + "withRSA");
        sign.initSign(privateKey);
        sign.update(message);
        return sign.sign();
    }

    private RSAPrivateKey readPrivateKey(File file) throws Exception {
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(readFile(file));
        return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(keySpec);
    }

    private byte[] readFile(File file) throws IOException {
        byte[] fileBytes = new byte[(int) file.length()];

        FileInputStream inputStream = new FileInputStream(file);
        inputStream.read(fileBytes);
        inputStream.close();

        return fileBytes;
    }
}
