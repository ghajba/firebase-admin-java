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

import com.google.api.client.googleapis.util.Utils;
import com.google.api.client.http.HttpTransport;
import com.google.auth.http.HttpTransportFactory;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.auth.oauth2.UserCredentials;
import com.google.common.collect.ImmutableList;
import com.google.common.io.CharStreams;
import com.google.firebase.internal.NonNull;
import com.google.firebase.tasks.Task;
import com.google.firebase.tasks.Tasks;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Standard {@link FirebaseCredential} implementations for use with {@link
 * com.google.firebase.FirebaseOptions}.
 */
public class FirebaseCredentials {

  private static final List<String> FIREBASE_SCOPES =
      ImmutableList.of(
          // Enables access to Firebase Realtime Database.
          "https://www.googleapis.com/auth/firebase.database",

          // Enables access to the email address associated with a project.
          "https://www.googleapis.com/auth/userinfo.email",

          // Enables access to Google Identity Toolkit (for user management APIs).
          "https://www.googleapis.com/auth/identitytoolkit",

          // Enables access to Google Cloud Storage.
          "https://www.googleapis.com/auth/devstorage.full_control");

  private FirebaseCredentials() {
  }

  private static String streamToString(InputStream inputStream) throws IOException {
    checkNotNull(inputStream, "InputStream must not be null");
    InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
    return CharStreams.toString(reader);
  }

  /**
   * Returns a {@link FirebaseCredential} based on Google Application Default Credentials which can
   * be used to authenticate the SDK.
   *
   * <p>See <a
   * href="https://developers.google.com/identity/protocols/application-default-credentials">Google
   * Application Default Credentials</a> for details on Google Application Deafult Credentials.
   *
   * <p>See <a href="/docs/admin/setup#initialize_the_sdk">Initialize the SDK</a> for code samples
   * and detailed documentation.
   *
   * @return A {@link FirebaseCredential} based on Google Application Default Credentials which can
   *     be used to authenticate the SDK.
   */
  @NonNull
  public static FirebaseCredential applicationDefault() throws IOException {
    return applicationDefault(Utils.getDefaultTransport());
  }

  /**
   * Returns a {@link FirebaseCredential} based on Google Application Default Credentials which can
   * be used to authenticate the SDK. Allows specifying the <code>HttpTransport</code> and the
   * <code>JsonFactory</code> to be used when communicating with the remote authentication server.
   *
   * <p>See <a
   * href="https://developers.google.com/identity/protocols/application-default-credentials">Google
   * Application Default Credentials</a> for details on Google Application Deafult Credentials.
   *
   * <p>See <a href="/docs/admin/setup#initialize_the_sdk">Initialize the SDK</a> for code samples
   * and detailed documentation.
   *
   * @param transport <code>HttpTransport</code> used to communicate with the remote
   *     authentication server.
   * @return A {@link FirebaseCredential} based on Google Application Default Credentials which can
   *     be used to authenticate the SDK.
   */
  @NonNull
  public static FirebaseCredential applicationDefault(HttpTransport transport) throws IOException {
    return new ApplicationDefaultCredential(transport);
  }

  /**
   * Returns a {@link FirebaseCredential} generated from the provided service account certificate
   * which can be used to authenticate the SDK.
   *
   * <p>See <a href="/docs/admin/setup#initialize_the_sdk">Initialize the SDK</a> for code samples
   * and detailed documentation.
   *
   * @param serviceAccount An <code>InputStream</code> containing the JSON representation of a
   *     service account certificate.
   * @return A {@link FirebaseCredential} generated from the provided service account certificate
   *     which can be used to authenticate the SDK.
   * @throws IOException If an error occurs while parsing the service account certificate.
   */
  @NonNull
  public static FirebaseCredential fromCertificate(InputStream serviceAccount) throws IOException {
    return fromCertificate(serviceAccount, Utils.getDefaultTransport());
  }

  /**
   * Returns a {@link FirebaseCredential} generated from the provided service account certificate
   * which can be used to authenticate the SDK. Allows specifying the <code>HttpTransport</code>
   * and the <code>JsonFactory</code> to be used when communicating with the remote authentication
   * server.
   *
   * <p>See <a href="/docs/admin/setup#initialize_the_sdk">Initialize the SDK</a> for code samples
   * and detailed documentation.
   *
   * @param serviceAccount An <code>InputStream</code> containing the JSON representation of a
   *     service account certificate.
   * @param transport <code>HttpTransport</code> used to communicate with the remote
   *     authentication server.
   * @return A {@link FirebaseCredential} generated from the provided service account certificate
   *     which can be used to authenticate the SDK.
   * @throws IOException If an error occurs while parsing the service account certificate.
   */
  @NonNull
  public static FirebaseCredential fromCertificate(InputStream serviceAccount,
      HttpTransport transport) throws IOException {
    // Consume the stream, and parse the content here since we need to extract the project_id.
    // We can drop this if https://github.com/google/google-auth-library-java/issues/117 gets
    // fixed.
    String jsonData = streamToString(serviceAccount);
    ServiceAccountCredentials credentials = ServiceAccountCredentials.fromStream(
        new ByteArrayInputStream(jsonData.getBytes("UTF-8")),
        wrap(transport));
    JSONObject json = new JSONObject(jsonData);
    String projectId;
    try {
      projectId = json.getString("project_id");
    } catch (JSONException e) {
      throw new IOException("Failed to parse service account: 'project_id' must be set", e);
    }
    return new CertCredential(credentials, projectId);
  }

  /**
   * Returns a {@link FirebaseCredential} generated from the provided refresh token which can be
   * used to authenticate the SDK.
   *
   * <p>See <a href="/docs/admin/setup#initialize_the_sdk">Initialize the SDK</a> for code samples
   * and detailed documentation.
   *
   * @param refreshToken An <code>InputStream</code> containing the JSON representation of a refresh
   *     token.
   * @return A {@link FirebaseCredential} generated from the provided service account credential
   *     which can be used to authenticate the SDK.
   * @throws IOException If an error occurs while parsing the refresh token.
   */
  @NonNull
  public static FirebaseCredential fromRefreshToken(InputStream refreshToken) throws IOException {
    return fromRefreshToken(
        refreshToken, Utils.getDefaultTransport());
  }

  /**
   * Returns a {@link FirebaseCredential} generated from the provided refresh token which can be
   * used to authenticate the SDK. Allows specifying the <code>HttpTransport</code> and the
   * <code>JsonFactory</code> to be used when communicating with the remote authentication server.
   *
   * <p>See <a href="/docs/admin/setup#initialize_the_sdk">Initialize the SDK</a> for code samples
   * and detailed documentation.
   *
   * @param refreshToken An <code>InputStream</code> containing the JSON representation of a refresh
   *     token.
   * @param transport <code>HttpTransport</code> used to communicate with the remote
   *     authentication server.
   * @return A {@link FirebaseCredential} generated from the provided service account credential
   *     which can be used to authenticate the SDK.
   * @throws IOException If an error occurs while parsing the refresh token.
   */
  @NonNull
  public static FirebaseCredential fromRefreshToken(final InputStream refreshToken,
      HttpTransport transport) throws IOException {
    return new RefreshTokenCredential(refreshToken, transport);
  }

  private static HttpTransportFactory wrap(final HttpTransport transport) {
    checkNotNull(transport, "HttpTransport must not be null");
    return new HttpTransportFactory() {
      @Override
      public HttpTransport create() {
        return transport;
      }
    };
  }

  static class CertCredential extends FirebaseCredential {

    private final String projectId;

    CertCredential(ServiceAccountCredentials credentials, String projectId) {
      super(credentials);
      this.projectId = projectId;
    }

    Task<String> getProjectId() {
      return Tasks.forResult(projectId);
    }
  }

  static class ApplicationDefaultCredential extends FirebaseCredential {

    ApplicationDefaultCredential(HttpTransport transport) throws IOException {
      super(GoogleCredentials.getApplicationDefault(wrap(transport)));
    }
  }

  static class RefreshTokenCredential extends FirebaseCredential {

    RefreshTokenCredential(InputStream inputStream, HttpTransport transport) throws IOException {
      super(UserCredentials.fromStream(inputStream, wrap(transport)));
    }
  }

}
