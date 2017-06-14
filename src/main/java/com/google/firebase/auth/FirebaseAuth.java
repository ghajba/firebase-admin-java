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

import com.google.common.base.Strings;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.UserRecord.CreateRequest;
import com.google.firebase.auth.UserRecord.UpdateRequest;
import com.google.firebase.tasks.Task;
import com.google.firebase.tasks.Tasks;

import java.util.Map;
import java.util.concurrent.Callable;

/**
 * This class is the entry point for all server-side Firebase Authentication actions.
 *
 * <p>You can get an instance of FirebaseAuth via {@link FirebaseAuth#getInstance(FirebaseApp)} and
 * then use it to perform a variety of authentication-related operations, including generating
 * custom tokens for use by client-side code, and verifying Firebase ID Tokens received from
 * clients. It also exposes methods that support provisioning and managing user accounts in a
 * Firebase project. All methods exposed by FirebaseAuth are asynchronous.
 */
public class FirebaseAuth {

  final BlockingFirebaseAuth blockingAuth;

  private FirebaseAuth(BlockingFirebaseAuth blockingAuth) {
    this.blockingAuth = blockingAuth;
  }

  /**
   * Gets the FirebaseAuth instance for the default {@link FirebaseApp}. The returned object
   * exposes a set of asynchronous methods.
   *
   * @return The FirebaseAuth instance for the default {@link FirebaseApp}.
   */
  public static FirebaseAuth getInstance() {
    return new FirebaseAuth(getBlockingInstance());
  }

  /**
   * Gets an instance of FirebaseAuth for a specific {@link FirebaseApp}. The returned
   * object exposes a set of asynchronous methods.
   *
   * @param app The {@link FirebaseApp} to get a FirebaseAuth instance for.
   * @return A FirebaseAuth instance.
   */
  public static FirebaseAuth getInstance(FirebaseApp app) {
    return new FirebaseAuth(getBlockingInstance(app));
  }

  /**
   * Gets the BlockingFirebaseAuth instance for the default {@link FirebaseApp}. The returned
   * object exposes a set of blocking methods.
   *
   * @return The BlockingFirebaseAuth instance for the default {@link FirebaseApp}.
   */
  public static BlockingFirebaseAuth getBlockingInstance() {
    return BlockingFirebaseAuth.getInstance();
  }

  /**
   * Gets an instance of BlockingFirebaseAuth for a specific {@link FirebaseApp}. The returned
   * object exposes a set of blocking methods.
   *
   * @param app The {@link FirebaseApp} to get a BlockingFirebaseAuth instance for.
   * @return A BlockingFirebaseAuth instance.
   */
  public static BlockingFirebaseAuth getBlockingInstance(FirebaseApp app) {
    return BlockingFirebaseAuth.getInstance(app);
  }

  /**
   * Creates a Firebase Custom Token associated with the given UID. This token can then be provided
   * back to a client application for use with the signInWithCustomToken authentication API.
   *
   * @param uid The UID to store in the token. This identifies the user to other Firebase services
   *     (Firebase Database, Firebase Auth, etc.)
   * @return A {@link Task} which will complete successfully with the created Firebase Custom Token,
   *     or unsuccessfully with the failure Exception.
   */
  public Task<String> createCustomToken(final String uid) {
    return Tasks.call(new Callable<String>() {
      @Override
      public String call() throws Exception {
        return blockingAuth.createCustomToken(uid);
      }
    });
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
   * @return A {@link Task} which will complete successfully with the created Firebase Custom Token,
   *     or unsuccessfully with the failure Exception.
   */
  public Task<String> createCustomToken(
      final String uid, final Map<String, Object> developerClaims) {
    return Tasks.call(new Callable<String>() {
      @Override
      public String call() throws Exception {
        return blockingAuth.createCustomToken(uid, developerClaims);
      }
    });
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
   * <p>If the token is valid, the returned {@link Task} will complete successfully and provide a
   * parsed version of the token from which the UID and other claims in the token can be inspected.
   * If the token is invalid, the Task will fail with an exception indicating the failure.
   *
   * @param token A Firebase ID Token to verify and parse.
   * @return A {@link Task} which will complete successfully with the parsed token, or
   *     unsuccessfully with the failure Exception.
   */
  public Task<FirebaseToken> verifyIdToken(final String token) {
    return Tasks.call(new Callable<FirebaseToken>() {
      @Override
      public FirebaseToken call() throws Exception {
        return blockingAuth.verifyIdToken(token);
      }
    });
  }

  /**
   * Gets the user data corresponding to the specified user ID.
   *
   * @param uid A user ID string.
   * @return A {@link Task} which will complete successfully with a {@link UserRecord} instance.
   *     If an error occurs while retrieving user data or if the specified user ID does not exist,
   *     the task fails with a FirebaseAuthException.
   * @throws IllegalArgumentException If the user ID string is null or empty.
   */
  public Task<UserRecord> getUser(final String uid) {
    checkArgument(!Strings.isNullOrEmpty(uid), "uid must not be null or empty");
    return Tasks.call(new Callable<UserRecord>() {
      @Override
      public UserRecord call() throws Exception {
        return blockingAuth.getUser(uid);
      }
    });
  }

  /**
   * Gets the user data corresponding to the specified user email.
   *
   * @param email A user email address string.
   * @return A {@link Task} which will complete successfully with a {@link UserRecord} instance.
   *     If an error occurs while retrieving user data or if the email address does not correspond
   *     to a user, the task fails with a FirebaseAuthException.
   * @throws IllegalArgumentException If the email is null or empty.
   */
  public Task<UserRecord> getUserByEmail(final String email) {
    checkArgument(!Strings.isNullOrEmpty(email), "email must not be null or empty");
    return Tasks.call(new Callable<UserRecord>() {
      @Override
      public UserRecord call() throws Exception {
        return blockingAuth.getUserByEmail(email);
      }
    });
  }

  /**
   * Creates a new user account with the attributes contained in the specified
   * {@link CreateRequest}.
   *
   * @param request A non-null {@link CreateRequest} instance.
   * @return A {@link Task} which will complete successfully with a {@link UserRecord} instance
   *     corresponding to the newly created account. If an error occurs while creating the user
   *     account, the task fails with a FirebaseAuthException.
   * @throws NullPointerException if the provided request is null.
   */
  public Task<UserRecord> createUser(final CreateRequest request) {
    checkNotNull(request, "create request must not be null");
    return Tasks.call(new Callable<UserRecord>() {
      @Override
      public UserRecord call() throws Exception {
        return blockingAuth.createUser(request);
      }
    });
  }

  /**
   * Updates an existing user account with the attributes contained in the specified
   * {@link UpdateRequest}.
   *
   * @param request A non-null {@link UpdateRequest} instance.
   * @return A {@link Task} which will complete successfully with a {@link UserRecord} instance
   *     corresponding to the updated user account. If an error occurs while updating the user
   *     account, the task fails with a FirebaseAuthException.
   * @throws NullPointerException if the provided update request is null.
   */
  public Task<UserRecord> updateUser(final UpdateRequest request) {
    checkNotNull(request, "update request must not be null");
    return Tasks.call(new Callable<UserRecord>() {
      @Override
      public UserRecord call() throws Exception {
        return blockingAuth.updateUser(request);
      }
    });
  }

  /**
   * Deletes the user identified by the specified user ID.
   *
   * @param uid A user ID string.
   * @return A {@link Task} which will complete successfully when the specified user account has
   *     been deleted. If an error occurs while deleting the user account, the task fails with a
   *     FirebaseAuthException.
   * @throws IllegalArgumentException If the user ID string is null or empty.
   */
  public Task<Void> deleteUser(final String uid) {
    checkArgument(!Strings.isNullOrEmpty(uid), "uid must not be null or empty");
    return Tasks.call(new Callable<Void>() {
      @Override
      public Void call() throws Exception {
        blockingAuth.deleteUser(uid);
        return null;
      }
    });
  }
}
