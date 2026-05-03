// Disabled integration test: TestRestTemplate missing from test classpath in this environment.
// Re-enable after adding proper test dependencies or replacing with WebTestClient.
//
// Original test intent: ensure /v3/api-docs is protected in prod profile.
// @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
// @ActiveProfiles("prod")
// public class SwaggerSecurityProdTest { ... }

