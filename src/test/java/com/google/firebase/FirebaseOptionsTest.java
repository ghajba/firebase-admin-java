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

package com.google.firebase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.firebase.auth.FirebaseCredential;
import com.google.firebase.auth.FirebaseCredentials;
import com.google.firebase.auth.TestOnlyImplFirebaseAuthTrampolines;
import com.google.firebase.testing.ServiceAccount;
import com.google.firebase.testing.TestUtils;
import java.io.IOException;
import org.junit.Test;

/** 
 * Tests for {@link FirebaseOptions}.
 */
public class FirebaseOptionsTest {

  private static final String FIREBASE_DB_URL = "https://mock-project.firebaseio.com";

  private static final FirebaseOptions ALL_VALUES_OPTIONS =
      new FirebaseOptions.Builder()
          .setDatabaseUrl(FIREBASE_DB_URL)
          .setCredential(TestUtils.getCertCredential(ServiceAccount.EDITOR.asStream()))
          .build();

  @Test
  public void createOptionsWithAllValuesSet() throws IOException, FirebaseException {
    GsonFactory jsonFactory = new GsonFactory();
    NetHttpTransport httpTransport = new NetHttpTransport();
    FirebaseOptions firebaseOptions =
        new FirebaseOptions.Builder()
            .setDatabaseUrl(FIREBASE_DB_URL)
            .setCredential(FirebaseCredentials.fromCertificate(ServiceAccount.EDITOR.asStream()))
            .setJsonFactory(jsonFactory)
            .setHttpTransport(httpTransport)
            .build();
    assertEquals(FIREBASE_DB_URL, firebaseOptions.getDatabaseUrl());
    assertSame(jsonFactory, firebaseOptions.getJsonFactory());
    assertSame(httpTransport, firebaseOptions.getHttpTransport());
    GoogleCredential googleCredential = TestOnlyImplFirebaseAuthTrampolines.getCertificate(
        firebaseOptions.getCredential());
    assertEquals(
        ServiceAccount.EDITOR.getEmail(), googleCredential.getServiceAccountId());
  }

  @Test
  public void createOptionsWithOnlyMandatoryValuesSet() throws IOException, FirebaseException {
    FirebaseOptions firebaseOptions =
        new FirebaseOptions.Builder()
            .setCredential(FirebaseCredentials.fromCertificate(ServiceAccount.EDITOR.asStream()))
            .build();
    assertNotNull(firebaseOptions.getJsonFactory());
    assertNotNull(firebaseOptions.getHttpTransport());
    GoogleCredential googleCredential = TestOnlyImplFirebaseAuthTrampolines.getCertificate(
        firebaseOptions.getCredential());
    assertEquals(
        GoogleCredential.fromStream(ServiceAccount.EDITOR.asStream()).getServiceAccountId(),
        googleCredential.getServiceAccountId());
  }

  @Test
  public void createOptionsWithServiceAccountSetsProjectId() throws Exception {
    FirebaseOptions firebaseOptions =
        new FirebaseOptions.Builder()
            .setCredential(FirebaseCredentials.fromCertificate(ServiceAccount.EDITOR.asStream()))
            .build();
    String projectId =
        TestOnlyImplFirebaseAuthTrampolines.getProjectId(firebaseOptions.getCredential());
    assertEquals("mock-project-id", projectId);
  }

  @Test(expected = NullPointerException.class)
  public void createOptionsWithCredentialMissing() {
    new FirebaseOptions.Builder().build();
  }

  @Test
  public void checkToBuilderCreatesNewEquivalentInstance() {
    FirebaseOptions allValuesOptionsCopy = new FirebaseOptions.Builder(ALL_VALUES_OPTIONS).build();
    assertNotSame(ALL_VALUES_OPTIONS, allValuesOptionsCopy);
    assertEquals(ALL_VALUES_OPTIONS.getCredential(), allValuesOptionsCopy.getCredential());
    assertEquals(ALL_VALUES_OPTIONS.getDatabaseUrl(), allValuesOptionsCopy.getDatabaseUrl());
    assertEquals(ALL_VALUES_OPTIONS.getJsonFactory(), allValuesOptionsCopy.getJsonFactory());
    assertEquals(ALL_VALUES_OPTIONS.getHttpTransport(), allValuesOptionsCopy.getHttpTransport());
  }

  @Test
  public void testNotEquals() throws IOException {
    FirebaseCredential credential = FirebaseCredentials
        .fromCertificate(ServiceAccount.EDITOR.asStream());
    FirebaseOptions options1 =
        new FirebaseOptions.Builder()
            .setCredential(credential)
            .build();
    FirebaseOptions options2 =
        new FirebaseOptions.Builder()
            .setCredential(credential)
            .setDatabaseUrl("https://test.firebaseio.com")
            .build();
    assertFalse(options1.equals(options2));
  }
}
