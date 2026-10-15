/*
 * Copyright 2026 deepcodecreate
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.keyline.ime.security;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public final class CredentialVault {
    private static final String PREF_FILE = "keyline_credential_vault";
    private static final String DATA_KEY = "encrypted_v1";
    private static final String KEY_ALIAS = "com.keyline.ime.credential-vault.aes.v1";
    private static final int IV_BYTES = 12;
    private static final int MAX_ENTRIES = 500;

    private CredentialVault() {}

    public static List<CredentialRecord> load(Context context) throws Exception {
        SharedPreferences prefs = privatePrefs(context);
        String encoded = prefs.getString(DATA_KEY, null);
        if (encoded == null || encoded.isEmpty()) return new ArrayList<>();

        byte[] payload = Base64.decode(encoded, Base64.NO_WRAP);
        if (payload.length <= IV_BYTES + 16) {
            throw new IOException("The encrypted credential store is incomplete.");
        }
        byte[] iv = new byte[IV_BYTES];
        byte[] encrypted = new byte[payload.length - IV_BYTES];
        System.arraycopy(payload, 0, iv, 0, IV_BYTES);
        System.arraycopy(payload, IV_BYTES, encrypted, 0, encrypted.length);

        byte[] plaintext = null;
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, getExistingKey(), new GCMParameterSpec(128, iv));
            plaintext = cipher.doFinal(encrypted);
            JSONObject root = new JSONObject(new String(plaintext, StandardCharsets.UTF_8));
            if (root.optInt("version", 0) != 1) {
                throw new IOException("Unsupported encrypted credential-store version.");
            }
            JSONArray array = root.optJSONArray("entries");
            List<CredentialRecord> out = new ArrayList<>();
            if (array == null) return out;
            if (array.length() > MAX_ENTRIES) throw new IOException("Credential store is too large.");
            Set<String> ids = new HashSet<>();
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.optJSONObject(i);
                if (item == null) continue;
                CredentialRecord record = new CredentialRecord(
                        bounded(item.optString("id", ""), 80),
                        bounded(item.optString("title", ""), 128),
                        bounded(item.optString("package", ""), 255),
                        bounded(item.optString("app", ""), 128),
                        bounded(item.optString("username", ""), 256),
                        bounded(item.optString("password", ""), 2048));
                if (record.id.isEmpty() || record.username.isEmpty() || !ids.add(record.id)) continue;
                out.add(record);
            }
            return out;
        } finally {
            if (plaintext != null) java.util.Arrays.fill(plaintext, (byte) 0);
            java.util.Arrays.fill(payload, (byte) 0);
            java.util.Arrays.fill(encrypted, (byte) 0);
            java.util.Arrays.fill(iv, (byte) 0);
        }
    }

    public static void save(Context context, List<CredentialRecord> records) throws Exception {
        if (records == null || records.size() > MAX_ENTRIES) {
            throw new IOException("Invalid credential count.");
        }
        JSONArray array = new JSONArray();
        for (CredentialRecord record : records) {
            if (record == null || record.id == null || record.id.isEmpty()
                    || record.username == null || record.username.trim().isEmpty()) {
                throw new IOException("A credential is missing required fields.");
            }
            JSONObject item = new JSONObject();
            item.put("id", bounded(record.id, 80));
            item.put("title", bounded(record.title, 128));
            item.put("package", bounded(record.appPackage, 255));
            item.put("app", bounded(record.appLabel, 128));
            item.put("username", bounded(record.username.trim(), 256));
            item.put("password", bounded(record.password, 2048));
            array.put(item);
        }

        byte[] plaintext = new JSONObject().put("version", 1).put("entries", array)
                .toString().getBytes(StandardCharsets.UTF_8);
        byte[] payload = null;
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey());
            byte[] iv = cipher.getIV();
            if (iv == null || iv.length != IV_BYTES) throw new IOException("Could not create a secure IV.");
            byte[] ciphertext = cipher.doFinal(plaintext);
            payload = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(ciphertext, 0, payload, iv.length, ciphertext.length);
            java.util.Arrays.fill(ciphertext, (byte) 0);
            java.util.Arrays.fill(iv, (byte) 0);
            boolean committed = privatePrefs(context).edit()
                    .putString(DATA_KEY, Base64.encodeToString(payload, Base64.NO_WRAP))
                    .commit();
            if (!committed) throw new IOException("Could not save the encrypted credential store.");
        } finally {
            java.util.Arrays.fill(plaintext, (byte) 0);
            if (payload != null) java.util.Arrays.fill(payload, (byte) 0);
        }
    }

    public static List<CredentialRecord> suggestions(Context context, String packageName,
                                                       boolean emailField, boolean passwordField)
            throws Exception {
        return suggestions(context, packageName, emailField, passwordField, "");
    }

    public static List<CredentialRecord> suggestions(Context context, String packageName,
                                                       boolean emailField, boolean passwordField,
                                                       String typed)
            throws Exception {
        List<CredentialRecord> records = load(context);
        String needle = typed == null ? "" : typed.trim().toLowerCase(java.util.Locale.ROOT);
        if (emailField && !needle.isEmpty()) {
            List<CredentialRecord> starts = new ArrayList<>();
            List<CredentialRecord> contains = new ArrayList<>();
            for (CredentialRecord r : records) {
                String u = r.username.toLowerCase(java.util.Locale.ROOT);
                if (u.equals(needle)) continue;
                if (u.startsWith(needle)) starts.add(r);
                else if (u.contains(needle)) contains.add(r);
            }
            records = new ArrayList<>(starts);
            records.addAll(contains);
        }
        if (!emailField && isBrowserPackage(context, packageName)) {
            return Collections.emptyList();
        }
        if (!emailField && (packageName == null || packageName.isEmpty())) {
            return Collections.emptyList();
        }
        if (emailField && packageName != null && !packageName.isEmpty()) {
            records.sort((a, b) -> Boolean.compare(
                    !packageName.equals(a.appPackage), !packageName.equals(b.appPackage)));
        }
        List<CredentialRecord> out = new ArrayList<>(3);
        Set<String> usernames = new HashSet<>();
        for (CredentialRecord record : records) {
            if (emailField) {
                if (!looksLikeEmail(record.username)) continue;
            } else {
                if (!packageName.equals(record.appPackage)) continue;
                if (passwordField && record.password.isEmpty()) continue;
            }
            String normalized = record.username.toLowerCase(java.util.Locale.ROOT);
            if (!usernames.add(normalized)) continue;
            CredentialRecord suggestion = new CredentialRecord(record.id, record.title,
                    record.appPackage, record.appLabel, record.username,
                    passwordField ? record.password : "");
            out.add(suggestion);
            if (out.size() == 3) break;
        }
        return out;
    }

    public static String newId() {
        return UUID.randomUUID().toString();
    }

    @SuppressWarnings("deprecation")
    public static Set<String> browserPackages(Context context) {
        Intent query = new Intent(Intent.ACTION_MAIN);
        query.addCategory(Intent.CATEGORY_APP_BROWSER);
        List<ResolveInfo> handlers = context.getPackageManager().queryIntentActivities(query, 0);
        Set<String> packages = new HashSet<>();
        for (ResolveInfo handler : handlers) {
            if (handler.activityInfo != null && handler.activityInfo.packageName != null) {
                packages.add(handler.activityInfo.packageName);
            }
        }
        return packages;
    }

    public static boolean isBrowserPackage(Context context, String packageName) {
        return packageName != null && !packageName.isEmpty()
                && browserPackages(context).contains(packageName);
    }

    private static boolean looksLikeEmail(String value) {
        if (value == null || value.indexOf(' ') >= 0) return false;
        int at = value.indexOf('@');
        return at > 0 && at < value.length() - 1;
    }

    private static String bounded(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static SharedPreferences privatePrefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
    }

    private static SecretKey getOrCreateKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        if (keyStore.containsAlias(KEY_ALIAS)) {
            java.security.Key key = keyStore.getKey(KEY_ALIAS, null);
            if (key instanceof SecretKey) return (SecretKey) key;
            throw new IOException("Credential encryption key is unavailable.");
        }
        KeyGenerator generator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
        KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build();
        generator.init(spec);
        return generator.generateKey();
    }

    private static SecretKey getExistingKey() throws Exception {
        KeyStore keyStore = KeyStore.getInstance("AndroidKeyStore");
        keyStore.load(null);
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            throw new IOException("Credential encryption key is unavailable.");
        }
        java.security.Key key = keyStore.getKey(KEY_ALIAS, null);
        if (key instanceof SecretKey) return (SecretKey) key;
        throw new IOException("Credential encryption key is unavailable.");
    }
}
