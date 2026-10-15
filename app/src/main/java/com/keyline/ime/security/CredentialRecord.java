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

public final class CredentialRecord {
    public String id;
    public String title;
    public String appPackage;
    public String appLabel;
    public String username;
    public String password;

    public CredentialRecord() {}

    public CredentialRecord(String id, String title, String appPackage, String appLabel,
                            String username, String password) {
        this.id = id;
        this.title = title;
        this.appPackage = appPackage;
        this.appLabel = appLabel;
        this.username = username;
        this.password = password;
    }
}
