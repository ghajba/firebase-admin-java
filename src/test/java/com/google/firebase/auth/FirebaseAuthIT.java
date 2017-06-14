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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.api.client.googleapis.util.Utils;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.GenericJson;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.common.collect.ImmutableMap;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.UserRecord.CreateRequest;
import com.google.firebase.auth.UserRecord.UpdateRequest;
import com.google.firebase.testing.IntegrationTestUtils;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import org.junit.BeforeClass;
import org.junit.Test;

public class FirebaseAuthIT {

  private static final String ID_TOOLKIT_URL =
      "https://www.googleapis.com/identitytoolkit/v3/relyingparty/verifyCustomToken";
  private static final JsonFactory jsonFactory = Utils.getDefaultJsonFactory();
  private static final HttpTransport transport = Utils.getDefaultTransport();

  private static FirebaseAuth auth;

  @BeforeClass
  public static void setUpClass() throws Exception {
    FirebaseApp masterApp = IntegrationTestUtils.ensureDefaultApp();
    auth = FirebaseAuth.getInstance(masterApp);
  }

  @Test
  public void testGetNonExistingUser() throws Exception {
    try {
      auth.getUser("non.existing").get();
      fail("No error thrown for non existing uid");
    } catch (ExecutionException e) {
      assertTrue(e.getCause() instanceof FirebaseAuthException);
      assertEquals(FirebaseUserManager.USER_NOT_FOUND_ERROR,
          ((FirebaseAuthException) e.getCause()).getErrorCode());
    }
  }

  @Test
  public void testGetNonExistingUserByEmail() throws Exception {
    try {
      auth.getUserByEmail("non.existing@definitely.non.existing").get();
      fail("No error thrown for non existing email");
    } catch (ExecutionException e) {
      assertTrue(e.getCause() instanceof FirebaseAuthException);
      assertEquals(FirebaseUserManager.USER_NOT_FOUND_ERROR,
          ((FirebaseAuthException) e.getCause()).getErrorCode());
    }
  }

  @Test
  public void testUpdateNonExistingUser() throws Exception {
    try {
      auth.updateUser(new UpdateRequest("non.existing")).get();
      fail("No error thrown for non existing uid");
    } catch (ExecutionException e) {
      assertTrue(e.getCause() instanceof FirebaseAuthException);
      assertEquals(FirebaseUserManager.USER_UPDATE_ERROR,
          ((FirebaseAuthException) e.getCause()).getErrorCode());
    }
  }

  @Test
  public void testDeleteNonExistingUser() throws Exception {
    try {
      auth.deleteUser("non.existing").get();
      fail("No error thrown for non existing uid");
    } catch (ExecutionException e) {
      assertTrue(e.getCause() instanceof FirebaseAuthException);
      assertEquals(FirebaseUserManager.USER_DELETE_ERROR,
          ((FirebaseAuthException) e.getCause()).getErrorCode());
    }
  }

  @Test
  public void testCreateUserWithParams() throws Exception {
    String randomId = UUID.randomUUID().toString().replaceAll("-", "");
    String userEmail = ("test" + randomId.substring(0, 12) + "@example." + randomId.substring(12)
        + ".com").toLowerCase();
    CreateRequest user = new CreateRequest()
        .setUid(randomId)
        .setEmail(userEmail)
        .setDisplayName("Random User")
        .setPhotoUrl("https://example.com/photo.png")
        .setEmailVerified(true)
        .setPassword("password");

    UserRecord userRecord = auth.createUser(user).get();
    try {
      assertEquals(randomId, userRecord.getUid());
      assertEquals("Random User", userRecord.getDisplayName());
      assertEquals(userEmail, userRecord.getEmail());
      assertEquals("https://example.com/photo.png", userRecord.getPhotoUrl());
      assertTrue(userRecord.isEmailVerified());
      assertFalse(userRecord.isDisabled());

      checkRecreate(randomId);
    } finally {
      auth.deleteUser(userRecord.getUid()).get();
    }
  }

  private void checkRecreate(String uid) throws Exception {
    try {
      auth.createUser(new CreateRequest().setUid(uid)).get();
      fail("No error thrown for creating user with existing ID");
    } catch (ExecutionException e) {
      assertTrue(e.getCause() instanceof FirebaseAuthException);
      assertEquals(FirebaseUserManager.USER_CREATE_ERROR,
          ((FirebaseAuthException) e.getCause()).getErrorCode());
    }
  }

  @Test
  public void testUserLifecycle() throws Exception {
    // Create user
    UserRecord userRecord = auth.createUser(new CreateRequest()).get();
    String uid = userRecord.getUid();

    // Get user
    userRecord = auth.getUser(userRecord.getUid()).get();
    assertEquals(uid, userRecord.getUid());
    assertNull(userRecord.getDisplayName());
    assertNull(userRecord.getEmail());
    assertNull(userRecord.getPhotoUrl());
    assertFalse(userRecord.isEmailVerified());
    assertFalse(userRecord.isDisabled());
    assertTrue(userRecord.getUserMetadata().getCreationTimestamp() > 0);
    assertEquals(0, userRecord.getUserMetadata().getLastSignInTimestamp());

    // Update user
    String randomId = UUID.randomUUID().toString().replaceAll("-", "");
    String userEmail = ("test" + randomId.substring(0, 12) + "@example." + randomId.substring(12)
        + ".com").toLowerCase();
    UpdateRequest request = userRecord.updateRequest()
        .setDisplayName("Updated Name")
        .setEmail(userEmail)
        .setPhotoUrl("https://example.com/photo.png")
        .setEmailVerified(true)
        .setPassword("secret");
    userRecord = auth.updateUser(request).get();
    assertEquals(uid, userRecord.getUid());
    assertEquals("Updated Name", userRecord.getDisplayName());
    assertEquals(userEmail, userRecord.getEmail());
    assertEquals("https://example.com/photo.png", userRecord.getPhotoUrl());
    assertTrue(userRecord.isEmailVerified());
    assertFalse(userRecord.isDisabled());

    // Get user by email
    userRecord = auth.getUserByEmail(userRecord.getEmail()).get();
    assertEquals(uid, userRecord.getUid());

    // Disable user and remove properties
    request = userRecord.updateRequest()
        .setPhotoUrl(null)
        .setDisplayName(null)
        .setDisabled(true);
    userRecord = auth.updateUser(request).get();
    assertEquals(uid, userRecord.getUid());
    assertNull(userRecord.getDisplayName());
    assertEquals(userEmail, userRecord.getEmail());
    assertNull(userRecord.getPhotoUrl());
    assertTrue(userRecord.isEmailVerified());
    assertTrue(userRecord.isDisabled());

    // Delete user
    auth.deleteUser(userRecord.getUid()).get();
    try {
      auth.getUser(userRecord.getUid()).get();
      fail("No error thrown for deleted user");
    } catch (ExecutionException e) {
      assertTrue(e.getCause() instanceof FirebaseAuthException);
      assertEquals(FirebaseUserManager.USER_NOT_FOUND_ERROR,
          ((FirebaseAuthException) e.getCause()).getErrorCode());
    }
  }

  @Test
  public void testCustomToken() throws Exception {
    String customToken = auth.createCustomToken("user1").get();
    String idToken = signInWithCustomToken(customToken);
    FirebaseToken decoded = auth.verifyIdToken(idToken).get();
    assertEquals("user1", decoded.getUid());
  }

  @Test
  public void testCustomTokenWithClaims() throws Exception {
    Map<String, Object> devClaims = ImmutableMap.<String, Object>of(
        "premium", true, "subscription", "silver");
    String customToken = auth.createCustomToken("user2", devClaims).get();
    String idToken = signInWithCustomToken(customToken);
    FirebaseToken decoded = auth.verifyIdToken(idToken).get();
    assertEquals("user2", decoded.getUid());
    assertTrue((Boolean) decoded.getClaims().get("premium"));
    assertEquals("silver", decoded.getClaims().get("subscription"));
  }

  private String signInWithCustomToken(String customToken) throws IOException {
    GenericUrl url = new GenericUrl(ID_TOOLKIT_URL + "?key="
        + IntegrationTestUtils.getApiKey());
    Map<String, Object> content = ImmutableMap.<String, Object>of(
        "token", customToken, "returnSecureToken", true);
    HttpRequest request = transport.createRequestFactory().buildPostRequest(url,
        new JsonHttpContent(jsonFactory, content));
    request.setParser(new JsonObjectParser(jsonFactory));
    HttpResponse response = request.execute();
    try {
      GenericJson json = response.parseAs(GenericJson.class);
      return json.get("idToken").toString();
    } finally {
      response.disconnect();
    }
  }

}
