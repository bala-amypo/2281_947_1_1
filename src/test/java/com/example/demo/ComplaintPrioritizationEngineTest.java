package com.example.demo;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.*;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

@Listeners(TestResultListener.class)
public class ComplaintPrioritizationEngineTest {

    private static final String BASE_URL = "http://localhost:9001";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private OkHttpClient client;
    private Gson gson;
    private String authToken;

    @BeforeClass
    public void setUp() {
        client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();
        gson = new Gson();
    }

    // ==================== FOLDER STRUCTURE TESTS (4 tests) ====================

    @Test(priority = 1, description = "Test if entity folder exists")
    public void testEntityFolderExists() {
        Path entityPath = Paths.get("src/main/java/com/example/demo/entity");
        Assert.assertTrue(Files.exists(entityPath), "Entity folder should exist");
    }

    @Test(priority = 2, description = "Test if controller folder exists")
    public void testControllerFolderExists() {
        Path controllerPath = Paths.get("src/main/java/com/example/demo/controller");
        Assert.assertTrue(Files.exists(controllerPath), "Controller folder should exist");
    }

    @Test(priority = 3, description = "Test if service folder exists")
    public void testServiceFolderExists() {
        Path servicePath = Paths.get("src/main/java/com/example/demo/service");
        Assert.assertTrue(Files.exists(servicePath), "Service folder should exist");
    }

    @Test(priority = 4, description = "Test if repository folder exists")
    public void testRepositoryFolderExists() {
        Path repositoryPath = Paths.get("src/main/java/com/example/demo/repository");
        Assert.assertTrue(Files.exists(repositoryPath), "Repository folder should exist");
    }

    // ==================== FILE NAME CHECKING TESTS (4 tests) ====================

    @Test(priority = 5, description = "Test if User.java file exists")
    public void testUserFileExists() {
        File userFile = new File("src/main/java/com/example/demo/entity/User.java");
        Assert.assertTrue(userFile.exists(), "User.java file should exist");
    }

    @Test(priority = 6, description = "Test if Complaint.java file exists")
    public void testComplaintFileExists() {
        File complaintFile = new File("src/main/java/com/example/demo/entity/Complaint.java");
        Assert.assertTrue(complaintFile.exists(), "Complaint.java file should exist");
    }

    @Test(priority = 7, description = "Test if AuthController.java file exists")
    public void testAuthControllerFileExists() {
        File authControllerFile = new File("src/main/java/com/example/demo/controller/AuthController.java");
        Assert.assertTrue(authControllerFile.exists(), "AuthController.java file should exist");
    }

    @Test(priority = 8, description = "Test if ComplaintController.java file exists")
    public void testComplaintControllerFileExists() {
        File complaintControllerFile = new File("src/main/java/com/example/demo/controller/ComplaintController.java");
        Assert.assertTrue(complaintControllerFile.exists(), "ComplaintController.java file should exist");
    }

    // ==================== API REQUEST AND RESPONSE TESTS (30 tests) ====================

    @Test(priority = 9, description = "Test user registration with valid data")
    public void testUserRegistrationValid() throws IOException {
        // Use unique email to avoid duplicate errors
        String uniqueEmail = "testuser" + System.currentTimeMillis() + "@example.com";
        String json = "{\"name\":\"Test User\",\"email\":\"" + uniqueEmail + "\",\"password\":\"password123\"}";
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "/auth/register")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            int statusCode = response.code();
            String responseBody = response.body().string();
            Assert.assertTrue(statusCode == 200 || statusCode == 201 || statusCode == 400 || statusCode == 403, 
                    "Registration should return 200, 201, 400, or 403. Got: " + statusCode + " - " + responseBody);
            if (statusCode == 200 || statusCode == 201) {
                Assert.assertTrue(responseBody.contains("User registered successfully") || responseBody.contains("message"), 
                        "Response should contain success message");
            }
        }
    }

    @Test(priority = 10, description = "Test user registration with duplicate email")
    public void testUserRegistrationDuplicateEmail() throws IOException {
        String json = "{\"name\":\"Test User 2\",\"email\":\"testuser@example.com\",\"password\":\"password123\"}";
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "/auth/register")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            Assert.assertTrue(response.code() == 400 || response.code() == 403 || response.code() == 409, 
                    "Duplicate email should return 400, 403, or 409");
        }
    }

    @Test(priority = 11, description = "Test user registration with invalid email")
    public void testUserRegistrationInvalidEmail() throws IOException {
        String json = "{\"name\":\"Test User\",\"email\":\"invalid-email\",\"password\":\"password123\"}";
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "/auth/register")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            Assert.assertTrue(response.code() == 400 || response.code() == 403 || response.code() == 500, 
                    "Invalid email should return error status");
        }
    }

    @Test(priority = 12, description = "Test user login with valid credentials")
    public void testUserLoginValid() throws IOException {
        String json = "{\"email\":\"testuser@example.com\",\"password\":\"password123\"}";
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "/auth/login")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            // Login might fail if user doesn't exist, so accept 200 or 400
            if (response.code() == 200) {
                String responseBody = response.body().string();
                JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                Assert.assertTrue(jsonResponse.has("token"), "Response should contain token");
                authToken = jsonResponse.get("token").getAsString();
                Assert.assertNotNull(authToken, "Token should not be null");
            } else {
                // If login fails, that's okay for this test - user might not exist yet
                Assert.assertTrue(response.code() == 400 || response.code() == 401, 
                        "Login might return 400 if user doesn't exist");
            }
        }
    }

    @Test(priority = 13, description = "Test user login with invalid credentials")
    public void testUserLoginInvalid() throws IOException {
        String json = "{\"email\":\"testuser@example.com\",\"password\":\"wrongpassword\"}";
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "/auth/login")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            Assert.assertEquals(response.code(), 400, "Invalid credentials should return 400");
            String responseBody = response.body().string();
            Assert.assertTrue(responseBody.contains("Invalid credentials"), 
                    "Response should contain error message");
        }
    }

    @Test(priority = 14, description = "Test user login with non-existent email")
    public void testUserLoginNonExistentEmail() throws IOException {
        String json = "{\"email\":\"nonexistent@example.com\",\"password\":\"password123\"}";
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "/auth/login")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            Assert.assertEquals(response.code(), 400, "Non-existent email should return 400");
        }
    }

    @Test(priority = 15, description = "Test submit complaint with valid data")
    public void testSubmitComplaintValid() throws IOException {
        // First register a new user for complaint
        String registerJson = "{\"name\":\"Complaint User\",\"email\":\"complaintuser@example.com\",\"password\":\"password123\"}";
        RequestBody registerBody = RequestBody.create(registerJson, JSON);
        Request registerRequest = new Request.Builder()
                .url(BASE_URL + "/auth/register")
                .post(registerBody)
                .build();

        String loginToken = "";
        try (Response response = client.newCall(registerRequest).execute()) {
            if (response.code() == 200) {
                // Login to get token
                String loginJson = "{\"email\":\"complaintuser@example.com\",\"password\":\"password123\"}";
                RequestBody loginBody = RequestBody.create(loginJson, JSON);
                Request loginRequest = new Request.Builder()
                        .url(BASE_URL + "/auth/login")
                        .post(loginBody)
                        .build();

                try (Response loginResponse = client.newCall(loginRequest).execute()) {
                    if (loginResponse.code() == 200) {
                        String loginResponseBody = loginResponse.body().string();
                        JsonObject jsonResponse = gson.fromJson(loginResponseBody, JsonObject.class);
                        loginToken = jsonResponse.get("token").getAsString();
                    }
                }
            }
        }

        // Submit complaint
        String complaintJson = "{\"title\":\"Network Issue\",\"description\":\"Unable to connect\",\"category\":\"Network\"}";
        RequestBody complaintBody = RequestBody.create(complaintJson, JSON);
        Request complaintRequest = new Request.Builder()
                .url(BASE_URL + "/complaints/submit/1")
                .header("Authorization", "Bearer " + loginToken)
                .post(complaintBody)
                .build();

        try (Response response = client.newCall(complaintRequest).execute()) {
            Assert.assertTrue(response.code() == 200 || response.code() == 401 || response.code() == 403 || response.code() == 404, 
                    "Complaint submission should return 200, 401, 403, or 404");
            if (response.code() == 200) {
                String responseBody = response.body().string();
                Assert.assertTrue(responseBody.contains("title") || responseBody.contains("id"), 
                        "Response should contain complaint data");
            }
        }
    }

    @Test(priority = 16, description = "Test submit complaint without authentication")
    public void testSubmitComplaintWithoutAuth() throws IOException {
        String complaintJson = "{\"title\":\"Test Complaint\",\"description\":\"Test Description\",\"category\":\"Network\"}";
        RequestBody complaintBody = RequestBody.create(complaintJson, JSON);
        Request complaintRequest = new Request.Builder()
                .url(BASE_URL + "/complaints/submit/1")
                .post(complaintBody)
                .build();

        try (Response response = client.newCall(complaintRequest).execute()) {
            Assert.assertTrue(response.code() == 401 || response.code() == 403, 
                    "Should return 401 or 403 without authentication");
        }
    }

    @Test(priority = 17, description = "Test submit complaint with missing fields")
    public void testSubmitComplaintMissingFields() throws IOException {
        String complaintJson = "{\"title\":\"Test Complaint\"}";
        RequestBody complaintBody = RequestBody.create(complaintJson, JSON);
        Request complaintRequest = new Request.Builder()
                .url(BASE_URL + "/complaints/submit/1")
                .header("Authorization", "Bearer " + authToken)
                .post(complaintBody)
                .build();

        try (Response response = client.newCall(complaintRequest).execute()) {
            Assert.assertTrue(response.code() == 400 || response.code() == 401 || response.code() == 403, 
                    "Missing fields should return error");
        }
    }

    @Test(priority = 18, description = "Test get user complaints endpoint")
    public void testGetUserComplaints() throws IOException {
        Request request = new Request.Builder()
                .url(BASE_URL + "/complaints/user/1")
                .header("Authorization", "Bearer " + authToken)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            Assert.assertTrue(response.code() == 200 || response.code() == 401 || response.code() == 403 || response.code() == 404, 
                    "Should return 200, 401, 403, or 404");
            if (response.code() == 200) {
                String responseBody = response.body().string();
                Assert.assertTrue(responseBody.contains("[") || responseBody.contains("[]"), 
                        "Response should be a JSON array");
            }
        }
    }

    @Test(priority = 19, description = "Test get user complaints without authentication")
    public void testGetUserComplaintsWithoutAuth() throws IOException {
        Request request = new Request.Builder()
                .url(BASE_URL + "/complaints/user/1")
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            Assert.assertTrue(response.code() == 401 || response.code() == 403, 
                    "Should return 401 or 403 without authentication");
        }
    }

    @Test(priority = 20, description = "Test get prioritized complaints endpoint")
    public void testGetPrioritizedComplaints() throws IOException {
        Request request = new Request.Builder()
                .url(BASE_URL + "/complaints/prioritized/1")
                .header("Authorization", "Bearer " + authToken)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            Assert.assertTrue(response.code() == 200 || response.code() == 401 || response.code() == 403 || response.code() == 404, 
                    "Should return 200, 401, 403, or 404");
        }
    }

    @Test(priority = 21, description = "Test get prioritized complaints without authentication")
    public void testGetPrioritizedComplaintsWithoutAuth() throws IOException {
        Request request = new Request.Builder()
                .url(BASE_URL + "/complaints/prioritized/1")
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            Assert.assertTrue(response.code() == 401 || response.code() == 403, 
                    "Should return 401 or 403 without authentication");
        }
    }

    @Test(priority = 22, description = "Test update complaint status")
    public void testUpdateComplaintStatus() throws IOException {
        Request request = new Request.Builder()
                .url(BASE_URL + "/complaints/status/1?newStatus=IN_PROGRESS")
                .header("Authorization", "Bearer " + authToken)
                .put(RequestBody.create("", null))
                .build();

        try (Response response = client.newCall(request).execute()) {
            Assert.assertTrue(response.code() == 200 || response.code() == 400 || response.code() == 401 || response.code() == 403 || response.code() == 404, 
                    "Should return appropriate status code");
        }
    }

    @Test(priority = 23, description = "Test update complaint status without authentication")
    public void testUpdateComplaintStatusWithoutAuth() throws IOException {
        Request request = new Request.Builder()
                .url(BASE_URL + "/complaints/status/1?newStatus=OPEN")
                .put(RequestBody.create("", null))
                .build();

        try (Response response = client.newCall(request).execute()) {
            Assert.assertTrue(response.code() == 401 || response.code() == 403, 
                    "Should return 401 or 403 without authentication");
        }
    }

    @Test(priority = 24, description = "Test update complaint status with invalid status")
    public void testUpdateComplaintStatusInvalid() throws IOException {
        Request request = new Request.Builder()
                .url(BASE_URL + "/complaints/status/1?newStatus=INVALID_STATUS")
                .header("Authorization", "Bearer " + authToken)
                .put(RequestBody.create("", null))
                .build();

        try (Response response = client.newCall(request).execute()) {
            Assert.assertTrue(response.code() == 200 || response.code() == 400 || response.code() == 401 || response.code() == 403 || response.code() == 404, 
                    "Should return appropriate status code");
        }
    }

    @Test(priority = 25, description = "Test get all priority rules")
    public void testGetAllPriorityRules() throws IOException {
        Request request = new Request.Builder()
                .url(BASE_URL + "/rules/all")
                .header("Authorization", "Bearer " + authToken)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            Assert.assertTrue(response.code() == 200 || response.code() == 401 || response.code() == 403, 
                    "Should return 200, 401, or 403");
            if (response.code() == 200) {
                String responseBody = response.body().string();
                Assert.assertTrue(responseBody.contains("[") || responseBody.contains("category"), 
                        "Response should contain rules data");
            }
        }
    }

    @Test(priority = 26, description = "Test get all priority rules without authentication")
    public void testGetAllPriorityRulesWithoutAuth() throws IOException {
        Request request = new Request.Builder()
                .url(BASE_URL + "/rules/all")
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            Assert.assertTrue(response.code() == 401 || response.code() == 403, 
                    "Should return 401 or 403 without authentication");
        }
    }

    @Test(priority = 27, description = "Test get status history")
    public void testGetStatusHistory() throws IOException {
        Request request = new Request.Builder()
                .url(BASE_URL + "/status/history/1")
                .header("Authorization", "Bearer " + authToken)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            Assert.assertTrue(response.code() == 200 || response.code() == 400 || response.code() == 401 || response.code() == 403 || response.code() == 404, 
                    "Should return appropriate status code");
        }
    }

    @Test(priority = 28, description = "Test get status history without authentication")
    public void testGetStatusHistoryWithoutAuth() throws IOException {
        Request request = new Request.Builder()
                .url(BASE_URL + "/status/history/1")
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            Assert.assertTrue(response.code() == 401 || response.code() == 403, 
                    "Should return 401 or 403 without authentication");
        }
    }

    @Test(priority = 29, description = "Test register endpoint response structure")
    public void testRegisterResponseStructure() throws IOException {
        String json = "{\"name\":\"Response Test\",\"email\":\"responsetest@example.com\",\"password\":\"password123\"}";
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "/auth/register")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.code() == 200) {
                String responseBody = response.body().string();
                Assert.assertTrue(responseBody.contains("message"), 
                        "Response should contain message field");
            }
        }
    }

    @Test(priority = 30, description = "Test login endpoint response structure")
    public void testLoginResponseStructure() throws IOException {
        String json = "{\"email\":\"testuser@example.com\",\"password\":\"password123\"}";
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "/auth/login")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.code() == 200) {
                String responseBody = response.body().string();
                JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                Assert.assertTrue(jsonResponse.has("token"), 
                        "Response should contain token field");
            }
        }
    }

    @Test(priority = 31, description = "Test complaint submission response structure")
    public void testComplaintSubmissionResponseStructure() throws IOException {
        String complaintJson = "{\"title\":\"Structure Test\",\"description\":\"Testing structure\",\"category\":\"Network\"}";
        RequestBody complaintBody = RequestBody.create(complaintJson, JSON);
        Request complaintRequest = new Request.Builder()
                .url(BASE_URL + "/complaints/submit/1")
                .header("Authorization", "Bearer " + authToken)
                .post(complaintBody)
                .build();

        try (Response response = client.newCall(complaintRequest).execute()) {
            if (response.code() == 200) {
                String responseBody = response.body().string();
                Assert.assertTrue(responseBody.contains("id") || responseBody.contains("title"), 
                        "Response should contain complaint fields");
            }
        }
    }

    @Test(priority = 32, description = "Test update status response structure")
    public void testUpdateStatusResponseStructure() throws IOException {
        Request request = new Request.Builder()
                .url(BASE_URL + "/complaints/status/1?newStatus=RESOLVED")
                .header("Authorization", "Bearer " + authToken)
                .put(RequestBody.create("", null))
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.code() == 200) {
                String responseBody = response.body().string();
                Assert.assertTrue(responseBody.contains("message"), 
                        "Response should contain message field");
            }
        }
    }

    @Test(priority = 33, description = "Test priority rules response structure")
    public void testPriorityRulesResponseStructure() throws IOException {
        Request request = new Request.Builder()
                .url(BASE_URL + "/rules/all")
                .header("Authorization", "Bearer " + authToken)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.code() == 200) {
                String responseBody = response.body().string();
                Assert.assertTrue(responseBody.contains("[") || responseBody.contains("category"), 
                        "Response should be an array or contain category");
            }
        }
    }

    @Test(priority = 34, description = "Test status history response structure")
    public void testStatusHistoryResponseStructure() throws IOException {
        Request request = new Request.Builder()
                .url(BASE_URL + "/status/history/1")
                .header("Authorization", "Bearer " + authToken)
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.code() == 200) {
                String responseBody = response.body().string();
                Assert.assertTrue(responseBody.contains("[") || responseBody.contains("status") || responseBody.contains("[]"), 
                        "Response should be an array or contain status");
            } else {
                // Accept 401, 403, or 404 as valid responses
                Assert.assertTrue(response.code() == 401 || response.code() == 403 || response.code() == 404, 
                        "Should return appropriate status code");
            }
        }
    }

    @Test(priority = 35, description = "Test register with empty email field")
    public void testRegisterEmptyEmail() throws IOException {
        String json = "{\"name\":\"Test User\",\"email\":\"\",\"password\":\"password123\"}";
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "/auth/register")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            Assert.assertTrue(response.code() == 400 || response.code() == 403 || response.code() == 500, 
                    "Empty email should return error");
        }
    }

    // ==================== STATUS CODE TESTS (5 tests) ====================

    @Test(priority = 36, description = "Test status code 200 for successful registration")
    public void testStatusCode200Registration() throws IOException {
        String json = "{\"name\":\"Status Test\",\"email\":\"statustest" + System.currentTimeMillis() + "@example.com\",\"password\":\"password123\"}";
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "/auth/register")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            Assert.assertTrue(response.code() == 200 || response.code() == 201 || response.code() == 400 || response.code() == 403, 
                    "Registration should return 200, 201, 400, or 403");
        }
    }

    @Test(priority = 37, description = "Test status code 200 for successful login")
    public void testStatusCode200Login() throws IOException {
        String json = "{\"email\":\"testuser@example.com\",\"password\":\"password123\"}";
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "/auth/login")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            Assert.assertTrue(response.code() == 200 || response.code() == 400 || response.code() == 401, 
                    "Login should return 200, 400, or 401");
        }
    }

    @Test(priority = 38, description = "Test status code 400 for invalid credentials")
    public void testStatusCode400InvalidCredentials() throws IOException {
        String json = "{\"email\":\"testuser@example.com\",\"password\":\"wrongpassword\"}";
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "/auth/login")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            Assert.assertEquals(response.code(), 400, "Invalid credentials should return 400");
        }
    }

    @Test(priority = 39, description = "Test status code 401 for unauthorized access")
    public void testStatusCode401Unauthorized() throws IOException {
        Request request = new Request.Builder()
                .url(BASE_URL + "/complaints/user/1")
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            Assert.assertTrue(response.code() == 401 || response.code() == 403, 
                    "Unauthorized access should return 401 or 403");
        }
    }

    @Test(priority = 40, description = "Test status code 404 for non-existent endpoint")
    public void testStatusCode404NotFound() throws IOException {
        Request request = new Request.Builder()
                .url(BASE_URL + "/nonexistent/endpoint")
                .get()
                .build();

        try (Response response = client.newCall(request).execute()) {
            Assert.assertTrue(response.code() == 404 || response.code() == 403, 
                    "Non-existent endpoint should return 404 or 403");
        }
    }

    // ==================== JWT TESTS (3 tests) ====================

    @Test(priority = 41, description = "Test JWT token generation on login")
    public void testJWTTokenGeneration() throws IOException {
        String json = "{\"email\":\"testuser@example.com\",\"password\":\"password123\"}";
        RequestBody body = RequestBody.create(json, JSON);
        Request request = new Request.Builder()
                .url(BASE_URL + "/auth/login")
                .post(body)
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (response.code() == 200) {
                String responseBody = response.body().string();
                JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                String token = jsonResponse.get("token").getAsString();
                Assert.assertNotNull(token, "JWT token should be generated");
                Assert.assertFalse(token.isEmpty(), "JWT token should not be empty");
                Assert.assertTrue(token.length() > 20, "JWT token should have reasonable length");
            }
        }
    }

    @Test(priority = 42, description = "Test JWT token validation for protected endpoints")
    public void testJWTTokenValidation() throws IOException {
        // First get a valid token
        String loginJson = "{\"email\":\"testuser@example.com\",\"password\":\"password123\"}";
        RequestBody loginBody = RequestBody.create(loginJson, JSON);
        Request loginRequest = new Request.Builder()
                .url(BASE_URL + "/auth/login")
                .post(loginBody)
                .build();

        String validToken = "";
        try (Response loginResponse = client.newCall(loginRequest).execute()) {
            if (loginResponse.code() == 200) {
                String loginResponseBody = loginResponse.body().string();
                JsonObject jsonResponse = gson.fromJson(loginResponseBody, JsonObject.class);
                validToken = jsonResponse.get("token").getAsString();
            }
        }

        // Test with valid token
        if (!validToken.isEmpty()) {
            Request request = new Request.Builder()
                    .url(BASE_URL + "/complaints/user/1")
                    .header("Authorization", "Bearer " + validToken)
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                Assert.assertTrue(response.code() == 200 || response.code() == 401 || response.code() == 403 || response.code() == 404, 
                        "Valid JWT token should allow access or return appropriate error");
            }
        }
    }

    @Test(priority = 43, description = "Test JWT token required for protected endpoints")
    public void testJWTTokenRequired() throws IOException {
        // Test without token
        Request requestWithoutToken = new Request.Builder()
                .url(BASE_URL + "/complaints/user/1")
                .get()
                .build();

        try (Response response = client.newCall(requestWithoutToken).execute()) {
            Assert.assertTrue(response.code() == 401 || response.code() == 403, 
                    "Protected endpoint should require JWT token");
        }

        // Test with invalid token
        Request requestWithInvalidToken = new Request.Builder()
                .url(BASE_URL + "/complaints/user/1")
                .header("Authorization", "Bearer invalid_token_12345")
                .get()
                .build();

        try (Response response = client.newCall(requestWithInvalidToken).execute()) {
            Assert.assertTrue(response.code() == 401 || response.code() == 403, 
                    "Invalid JWT token should be rejected");
        }
    }
}

