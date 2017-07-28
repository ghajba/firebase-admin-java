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
import com.google.auth.oauth2.AccessToken;
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

import java.util.concurrent.Callable;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Standard {@link FirebaseCredential} implementations for use with {@link
 * com.google.firebase.FirebaseOptions}.
 */
public class FirebaseCredentials {

  private static final List<String> FIREBASE_SCOPES =
      ImmutableList.of(
          "https://www.googleapis.com/auth/firebase.database",
          "https://www.googleapis.com/auth/userinfo.email",
          "https://www.googleapis.com/auth/identitytoolkit");

  private FirebaseCredentials() {
  }

  private static String streamToString(InputStream inputStream) throws IOException {
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
  public static FirebaseCredential applicationDefault() {
    return DefaultCredentialsHolder.getInstance();
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
  public static FirebaseCredential applicationDefault(HttpTransport transport) {
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
    return new CertCredential(serviceAccount, transport);
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

  /**
   * Helper class that implements {@link FirebaseCredential} on top of {@link GoogleCredentials} and
   * provides caching of access tokens and credentials.
   */
  abstract static class BaseCredential implements FirebaseCredential {

    abstract GoogleCredentials getGoogleCredentials() throws IOException;

    /**
     * Returns an access token for this credential. Does not cache tokens.
     */
    @Override
    public final Task<GoogleOAuthAccessToken> getAccessToken() {
      return Tasks.call(new Callable<GoogleOAuthAccessToken>() {
        @Override
        public GoogleOAuthAccessToken call() throws Exception {
          return fetchToken(getGoogleCredentials());
        }
      });
    }

    // To be overridden during tests
    protected GoogleOAuthAccessToken fetchToken(GoogleCredentials credentials) throws IOException {
      return newAccessToken(credentials.refreshAccessToken());
    }
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

  static class CertCredential extends BaseCredential {

    private final String projectId;
    private final GoogleCredentials googleCredentials;

    CertCredential(InputStream inputStream, HttpTransport transport) throws IOException {
      String jsonData = streamToString(checkNotNull(inputStream));
      byte[] jsonBytes = jsonData.getBytes("UTF-8");
      this.googleCredentials = ServiceAccountCredentials
          .fromStream(new ByteArrayInputStream(jsonBytes), wrap(transport))
          .createScoped(FIREBASE_SCOPES);

      JSONObject jsonObject = new JSONObject(jsonData);
      try {
        this.projectId = jsonObject.getString("project_id");
      } catch (JSONException e) {
        throw new IOException("Failed to parse service account: 'project_id' must be set", e);
      }
    }

    @Override
    GoogleCredentials getGoogleCredentials() {
      return googleCredentials;
    }

    Task<String> getProjectId() {
      return Tasks.forResult(projectId);
    }
  }

  static class ApplicationDefaultCredential extends BaseCredential {

    private final HttpTransport transport;

    ApplicationDefaultCredential(HttpTransport transport) {
      this.transport = transport;
    }

    @Override
    GoogleCredentials getGoogleCredentials() throws IOException {
      // Defer initialization of GoogleCredentials until they are necessary. This may make RPC
      // calls in some environments. These credentials get cached by the underlying auth library.
      return GoogleCredentials.getApplicationDefault(wrap(transport))
          .createScoped(FIREBASE_SCOPES);
    }
  }

  static class RefreshTokenCredential extends BaseCredential {

    private final GoogleCredentials googleCredentials;

    RefreshTokenCredential(InputStream inputStream, HttpTransport transport) throws IOException {
      this.googleCredentials = UserCredentials.fromStream(inputStream, wrap(transport))
          .createScoped(FIREBASE_SCOPES);
    }

    @Override
    GoogleCredentials getGoogleCredentials() {
      return googleCredentials;
    }
  }

  static GoogleOAuthAccessToken newAccessToken(AccessToken accessToken) {
    checkNotNull(accessToken);
    return new GoogleOAuthAccessToken(accessToken.getTokenValue(),
        accessToken.getExpirationTime().getTime());
  }

  private static class DefaultCredentialsHolder {

    static FirebaseCredential INSTANCE;

    static synchronized FirebaseCredential getInstance() {
      if (INSTANCE == null) {
        INSTANCE = applicationDefault(Utils.getDefaultTransport());
      }
      return INSTANCE;
    }
  }

}
