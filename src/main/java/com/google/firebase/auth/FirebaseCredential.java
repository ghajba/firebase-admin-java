/*
 * Copyright 2017 Google Inc.
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

package com.google.firebase.auth;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.common.collect.ImmutableList;
import java.util.List;

/**
 * Provides Google OAuth2 access tokens used to authenticate with Firebase services. In most cases,
 * you will not need to implement this yourself and can instead use the default implementations
 * provided by {@link FirebaseCredentials}.
 */
public abstract class FirebaseCredential {

  private static final List<String> FIREBASE_SCOPES =
      ImmutableList.of(
          "https://www.googleapis.com/auth/firebase.database",
          "https://www.googleapis.com/auth/userinfo.email",
          "https://www.googleapis.com/auth/identitytoolkit");

  private final GoogleCredentials googleCredentials;

  public FirebaseCredential(GoogleCredentials googleCredentials) {
    this.googleCredentials = checkNotNull(googleCredentials).createScoped(FIREBASE_SCOPES);
  }

  public final GoogleCredentials getGoogleCredentials() {
    return googleCredentials;
  }

}
