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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GooglePublicKeysManager;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.Clock;
import com.google.common.base.Strings;
import com.google.firebase.FirebaseApp;
import com.google.firebase.ImplFirebaseTrampolines;
import com.google.firebase.auth.FirebaseCredentials.CertCredential;
import com.google.firebase.auth.UserRecord.CreateRequest;
import com.google.firebase.auth.UserRecord.UpdateRequest;
import com.google.firebase.auth.internal.FirebaseTokenFactory;
import com.google.firebase.auth.internal.FirebaseTokenVerifier;
import com.google.firebase.internal.FirebaseService;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Map;

/**
 * You can get an instance of BlockingFirebaseAuth via
 * {@link FirebaseAuth#getBlockingInstance(FirebaseApp)} and then use it to perform a variety of
 * authentication-related operations, including generating custom tokens for use by client-side
 * code, and verifying Firebase ID Tokens received from clients. It also exposes methods that
 * support provisioning and managing user accounts in a Firebase project. All methods exposed by
 * this class are blocking.
 */
public class BlockingFirebaseAuth {

  private static final String INVALID_CREDENTIAL_ERROR = "INVALID_CREDENTIAL";
  private static final String TOKEN_CREATION_FAILED = "TOKEN_CREATION_FAILED";
  private static final String ID_TOKEN_PARSE_ERROR = "ID_TOKEN_PARSE_ERROR";
  private static final String ACCESS_TOKEN_ERROR = "ACCESS_TOKEN_ERROR";

  private final FirebaseApp firebaseApp;
  private final GooglePublicKeysManager googlePublicKeysManager;
  private final Clock clock;
  private final JsonFactory jsonFactory;
  private final FirebaseUserManager userManager;

  private BlockingFirebaseAuth(FirebaseApp firebaseApp) {
    this(firebaseApp, FirebaseTokenVerifier.DEFAULT_KEY_MANAGER, Clock.SYSTEM);
  }

  /**
   * Constructor for injecting a GooglePublicKeysManager, which is used to verify tokens are
   * correctly signed. This should only be used for testing to override the default key manager.
   */
  private BlockingFirebaseAuth(
      FirebaseApp firebaseApp, GooglePublicKeysManager googlePublicKeysManager, Clock clock) {
    this.firebaseApp = firebaseApp;
    this.googlePublicKeysManager = googlePublicKeysManager;
    this.clock = clock;
    this.jsonFactory = firebaseApp.getOptions().getJsonFactory();
    this.userManager = new FirebaseUserManager(jsonFactory,
        firebaseApp.getOptions().getHttpTransport());
  }

  static BlockingFirebaseAuth getInstance() {
    return BlockingFirebaseAuth.getInstance(FirebaseApp.getInstance());
  }

  static synchronized BlockingFirebaseAuth getInstance(FirebaseApp app) {
    FirebaseAuthService service = ImplFirebaseTrampolines.getService(app, SERVICE_ID,
        FirebaseAuthService.class);
    if (service == null) {
      service = ImplFirebaseTrampolines.addService(app, new FirebaseAuthService(app));
    }
    return service.getInstance();
  }

  /**
   * Creates a Firebase Custom Token associated with the given UID. This token can then be provided
   * back to a client application for use with the signInWithCustomToken authentication API.
   *
   * @param uid The UID to store in the token. This identifies the user to other Firebase services
   *     (Firebase Database, Firebase Auth, etc.)
   * @return A Firebase custom token string.
   * @throws FirebaseAuthException If the underlying credential is invalid, or if an error occurs
   *     while creating the custom token.
   */
  public String createCustomToken(String uid) throws FirebaseAuthException {
    return createCustomToken(uid, null);
  }

  /**
   * Creates a Firebase Custom Token associated with the given UID and additionally containing the
   * specified developerClaims. This token can then be provided back to a client application for use
   * with the signInWithCustomToken authentication API.
   *
   * @param uid The UID to store in the token. This identifies the user to other Firebase services
   *     (Realtime Database, Storage, etc.). Should be less than 128 characters.
   * @param developerClaims Additional claims to be stored in the token (and made available to
   *     security rules in Database, Storage, etc.). These must be able to be serialized to JSON
   *     (e.g. contain only Maps, Arrays, Strings, Booleans, Numbers, etc.)
   * @return A Firebase custom token string.
   * @throws FirebaseAuthException If the underlying credential is invalid, or if an error occurs
   *     while creating the custom token.
   */
  public String createCustomToken(
      String uid, Map<String, Object> developerClaims) throws FirebaseAuthException {
    FirebaseCredential credential = ImplFirebaseTrampolines.getCredential(firebaseApp);
    if (!(credential instanceof FirebaseCredentials.CertCredential)) {
      throw new FirebaseAuthException(INVALID_CREDENTIAL_ERROR,
          "Must initialize FirebaseApp with a certificate credential to call createCustomToken()");
    }

    FirebaseTokenFactory tokenFactory = FirebaseTokenFactory.getInstance();
    try {
      GoogleCredential baseCredential = ((CertCredential) credential).getCertificate();
      return tokenFactory.createSignedCustomAuthTokenForUser(
          uid,
          developerClaims,
          baseCredential.getServiceAccountId(),
          baseCredential.getServiceAccountPrivateKey());
    } catch (IOException | GeneralSecurityException e) {
      throw new FirebaseAuthException(TOKEN_CREATION_FAILED,
          "Error while creating custom token", e);
    }
  }

  /**
   * Parses and verifies a Firebase ID Token.
   *
   * <p>A Firebase application can identify itself to a trusted backend server by sending its
   * Firebase ID Token (accessible via the getToken API in the Firebase Authentication client) with
   * its request.
   *
   * <p>The backend server can then use the verifyIdToken() method to verify the token is valid,
   * meaning: the token is properly signed, has not expired, and it was issued for the project
   * associated with this FirebaseAuth instance (which by default is extracted from your service
   * account)
   *
   * <p>If the token is valid, this method returns successfully with a
   * parsed version of the token from which the UID and other claims in the token can be inspected.
   * If the token is invalid, the method throws an exception.
   *
   * @param token A Firebase ID Token to verify and parse.
   * @return A {@link FirebaseToken} instance..
   * @throws FirebaseAuthException If the underlying credential is invalid, or if an error occurs
   *     while verifying the ID token.
   */
  public FirebaseToken verifyIdToken(final String token) throws FirebaseAuthException {
    FirebaseCredential credential = ImplFirebaseTrampolines.getCredential(firebaseApp);
    if (!(credential instanceof FirebaseCredentials.CertCredential)) {
      throw new FirebaseAuthException(INVALID_CREDENTIAL_ERROR,
          "Must initialize FirebaseApp with a certificate credential to call verifyIdToken()");
    }

    FirebaseTokenVerifier firebaseTokenVerifier =
        new FirebaseTokenVerifier.Builder()
            .setProjectId(((CertCredential) credential).getProjectId())
            .setPublicKeysManager(googlePublicKeysManager)
            .setClock(clock)
            .build();
    try {
      FirebaseToken firebaseToken = FirebaseToken.parse(jsonFactory, token);
      // This will throw a FirebaseAuthException with details on how the token is invalid.
      firebaseTokenVerifier.verifyTokenAndSignature(firebaseToken.getToken());
      return firebaseToken;
    } catch (IOException e) {
      throw new FirebaseAuthException(ID_TOKEN_PARSE_ERROR, "Error while parsing ID token", e);
    }
  }

  /**
   * Gets the user data corresponding to the specified user ID.
   *
   * @param uid A user ID string.
   * @return A {@link UserRecord} instance.
   * @throws IllegalArgumentException If the user ID string is null or empty.
   * @throws FirebaseAuthException If an error occurs while retrieving the user or if the the
   *     specified user ID does not exist.
   */
  public UserRecord getUser(final String uid) throws FirebaseAuthException {
    checkArgument(!Strings.isNullOrEmpty(uid), "uid must not be null or empty");
    return userManager.getUserById(uid, getToken());
  }

  /**
   * Gets the user data corresponding to the specified user email.
   *
   * @param email A user email address string.
   * @return A {@link UserRecord} instance.
   * @throws IllegalArgumentException If the email is null or empty.
   * @throws FirebaseAuthException If an error occurs while retrieving the user or if the the
   *     specified user ID does not exist.
   */
  public UserRecord getUserByEmail(final String email) throws FirebaseAuthException {
    checkArgument(!Strings.isNullOrEmpty(email), "email must not be null or empty");
    return userManager.getUserByEmail(email, getToken());
  }

  /**
   * Creates a new user account with the attributes contained in the specified
   * {@link CreateRequest}.
   *
   * @param request A non-null {@link CreateRequest} instance.
   * @return A {@link UserRecord} instance corresponding to the newly created account.
   * @throws FirebaseAuthException If an error occurs while creating the user.
   * @throws NullPointerException if the provided request is null.
   */
  public UserRecord createUser(final CreateRequest request) throws FirebaseAuthException {
    checkNotNull(request, "create request must not be null");
    String token = getToken();
    String uid = userManager.createUser(request, token);
    return userManager.getUserById(uid, token);
  }

  /**
   * Updates an existing user account with the attributes contained in the specified
   * {@link UpdateRequest}.
   *
   * @param request A non-null {@link UpdateRequest} instance.
   * @return A {@link UserRecord} instance corresponding to the updated user account.
   * @throws FirebaseAuthException If an error occurs while updating the user.
   * @throws NullPointerException if the provided update request is null.
   */
  public UserRecord updateUser(final UpdateRequest request) throws FirebaseAuthException {
    checkNotNull(request, "update request must not be null");
    String token = getToken();
    userManager.updateUser(request, token);
    return userManager.getUserById(request.getUid(), token);
  }

  /**
   * Deletes the user identified by the specified user ID.
   *
   * @param uid A user ID string.
   * @throws IllegalArgumentException If the user ID string is null or empty.
   * @throws FirebaseAuthException If an error occurs while deleting the user.
   */
  public void deleteUser(final String uid) throws FirebaseAuthException {
    checkArgument(!Strings.isNullOrEmpty(uid), "uid must not be null or empty");
    userManager.deleteUser(uid, getToken());
  }

  private String getToken() throws FirebaseAuthException {
    try {
      return ImplFirebaseTrampolines.getToken(firebaseApp, false).getToken();
    } catch (IOException e) {
      throw new FirebaseAuthException(ACCESS_TOKEN_ERROR, "Failed to obtain an access token", e);
    }
  }

  private static final String SERVICE_ID = FirebaseAuth.class.getName();

  private static class FirebaseAuthService extends FirebaseService<BlockingFirebaseAuth> {

    FirebaseAuthService(FirebaseApp app) {
      super(SERVICE_ID, new BlockingFirebaseAuth(app));
    }

    @Override
    public void destroy() {
      // NOTE: We don't explicitly tear down anything here, but public methods of FirebaseAuth
      // will now fail because calls to getCredential() and getToken() will hit FirebaseApp,
      // which will throw once the app is deleted.
    }
  }

}
