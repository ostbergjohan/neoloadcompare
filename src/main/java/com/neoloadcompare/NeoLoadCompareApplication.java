package com.neoloadcompare;

import io.swagger.v3.oas.annotations.ExternalDocumentation;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.TrustStrategy;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.*;

import static java.lang.Double.parseDouble;
@OpenAPIDefinition(
		info = @Info(
				title = "NeoLoad Compare",
				version = "v1.0",
				description = "NeoLoad Compare API to validate performance test results against baseline metrics.\n" +
						"Endpoints:\n" +
						"**Health**\n" +
						"1. **GET /healthcheck** - API health check.\n\n" +
						"**Performance Comparison**\n" +
						"2. **GET /NeoLoadCompare** - Compare latest test results against baseline.\n" +
						"   - **Parameters:**\n" +
						"     - `workspace` (required) - Workspace name\n" +
						"     - `scenario` (required) - Scenario name to compare\n" +
						"     - `baseline` (required) - Baseline test number (e.g., 1, 2, 3)\n" +
						"     - `percentage` (required) - Allowed percentage increase threshold\n" +
						"     - `element` (required) - Metric to compare: avgduration, percentile90, percentile95, percentile99, or false\n" +
						"   - **Example:** `/NeoLoadCompare?workspace=demo&scenario=demo_scenario&baseline=1&percentage=10&element=avgduration`\n" +
						"   - **Response:** Returns comparison results with status OK or FAILED if thresholds exceeded.\n\n" +
						"**Configuration:**\n" +
						"Requires environment variables:\n" +
						"- `Server` - NeoLoad API server URL (e.g., http://neoload.example.com)\n" +
						"- `Token` - NeoLoad API authentication token\n"
		),
		externalDocs = @ExternalDocumentation(
				description = "GitHub Repository",
				url = "https://github.com/ostbergjohan/neoloadcompare"
		)
)
@SpringBootApplication
@RestController

public class NeoLoadCompareApplication {

	public static void main(String[] args) {
		SpringApplication.run(NeoLoadCompareApplication.class, args);
	}

	ColorLogger colorLogger = new ColorLogger();

	@EventListener
	public void handleContextRefresh(ContextRefreshedEvent event) {
		final Environment env = event.getApplicationContext().getEnvironment();
		if (env.getProperty("Server") == null | env.getProperty("Token") == null) {
			colorLogger.logError("NeoLoad API server and token must be set -e Server=\"http://XXX\" and -e Server=XXX");

			System.exit(0);
		}

		try {
			JSONObject jsonObj = new JSONObject(doHttpGet(env.getProperty("Server") + "/v3/information", env.getProperty("Token")));
			if (jsonObj.has("message")) {
				colorLogger.logError("message: " + jsonObj.getString("message"));
				colorLogger.logInfo("===========================================");
				System.exit(0);
			}
			colorLogger.logInfo("====== Environment and configuration ======");
			colorLogger.logInfo("front_url: " + jsonObj.getString("front_url"));
			colorLogger.logInfo("api_url: " + env.getProperty("Server"));
			colorLogger.logInfo("filestorage_url: " + jsonObj.getString("filestorage_url"));
			colorLogger.logInfo("version: " + jsonObj.getString("version"));
			colorLogger.logInfo("===========================================");
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	@Autowired
	public Environment env;

	@GetMapping(value = "healthcheck")
	public ResponseEntity<String> healthcheck() {
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.CACHE_CONTROL, "no-cache");
		headers.add(HttpHeaders.CONTENT_TYPE, "text/plain; charset=UTF-8");
		headers.add(HttpHeaders.CONTENT_ENCODING, "UTF-8");
		return ResponseEntity.ok()
				.headers(headers)
				.body("{\"status\":\"ok\",\"service\":\"API Health Check\"}");
	}

	@GetMapping(value = "NeoLoadCompare")
	public ResponseEntity<String> NeoLoadCompare(@RequestParam String workspace, @RequestParam String scenario, @RequestParam String baseline, @RequestParam String percentage, @RequestParam String element) throws JSONException, IOException, URISyntaxException, InterruptedException {
		String serverUrl;
		String Token;
		String testId;
		String jsonString;
		String workspaceId;
		String testname;

		serverUrl = env.getProperty("Server");
		Token = env.getProperty("Token");

		HttpHeaders headers = createHeaders();

// Check and handle missing parameters with default values or error messages
		if (workspace == null) {
			return logAndRespondError(String.format("Workspace is required. Please provide a valid workspace.\nExample usage: %s/NeoLoadCompare?workspace=demo&scenario=demo_scenario&baseline=1&percentage=10&element=avgduration", serverUrl));
		}
		if (scenario == null) {
			return logAndRespondError(String.format("Scenario is required. Please provide a valid scenario.\nExample usage: %s/NeoLoadCompare?workspace=demo&scenario=demo_scenario&baseline=1&percentage=10&element=avgduration", serverUrl));
		}
		if (baseline == null) {
			return logAndRespondError(String.format("Baseline is required. Please provide a valid baseline.\nExample usage: %s/NeoLoadCompare?workspace=demo&scenario=demo_scenario&baseline=1&percentage=10&element=avgduration", serverUrl));
		}
		if (percentage == null) {
			return logAndRespondError(String.format("Percentage is required. Please provide a percentage value.\nExample usage: %s/NeoLoadCompare?workspace=demo&scenario=demo_scenario&baseline=1&percentage=10&element=avgduration", serverUrl));
		}
		if (element == null) {
			return logAndRespondError(String.format("Element is required. Please provide a valid element.\nExample usage: %s/NeoLoadCompare?workspace=demo&scenario=demo_scenario&baseline=1&percentage=10&element=avgduration", serverUrl));
		}

		// Validate element type
		Optional<String> elementOpt = validateElement(element);
		if (!elementOpt.isPresent()) {
			return logAndRespondError(String.format("Invalid 'element' parameter. Valid options are: avgduration, percentile90, percentile95, percentile99 or false. Example usage: %s/NeoLoadCompare?workspace=demo&scenario=demo_scenario&baseline=1&percentage=10&element=avgduration", serverUrl));
		}
		String validElement = elementOpt.get();
		if (validElement == null || validElement.equalsIgnoreCase("no") || validElement.equalsIgnoreCase("nej") || validElement.equalsIgnoreCase("false")) {
			return Respond("compare against baseline=false");
		}

		//check workspace
		jsonString = doHttpGet(serverUrl + "/v3/workspaces?allWorkspaces=true", Token);

		workspaceId = getWorkspaceId(workspace, jsonString);

		if (workspaceId.equals("NOT FOUND")) {
			return logAndRespondError("workspace not found:" + workspace);
		}

		//Hämta id på baseline
		jsonString = doHttpGet(serverUrl + "/v3/workspaces/" + workspaceId + "/test-results?limit=30000&status=TERMINATED&&sort=-startDate", Token);
		testId = getId("#" + baseline, scenario, jsonString);

		if (testId.equals("NOT FOUND")) {
			return logAndRespondError("Baseline not found:" + baseline);
		}

		String latestTestId = getlatestTestId(jsonString, scenario);

		if (latestTestId == "1") {
			return logAndRespondError("Not same scenario name in the latest test");
		}

		testname = getlatestName(jsonString);
		HashMap<String, String> TransactionsElementsBaseline = new HashMap<String, String>();
		HashMap<String, String> TransactionsElementsLatestTest = new HashMap<String, String>();
		HashMap<String, String> TransactionsLatestTest = new HashMap<String, String>();
		HashMap<String, String> TransactionsBaseline = new HashMap<String, String>();

		jsonString = doHttpGet(serverUrl + "/v3/workspaces/" + workspaceId + "/test-results/" + latestTestId + "/elements?category=TRANSACTION", Token);
		TransactionsElementsLatestTest = getTransactionElements(jsonString);
		jsonString = doHttpGet(serverUrl + "/v3/workspaces/" + workspaceId + "/test-results/" + testId + "/elements?category=TRANSACTION", Token);
		TransactionsElementsBaseline = getTransactionElements(jsonString);

		if (TransactionsElementsLatestTest.size() != TransactionsElementsBaseline.size()) {
			return logAndRespondError("The tests that are compared have different number of unique transactions names");
		}

		for (Map.Entry<String, String> entry : TransactionsElementsLatestTest.entrySet()) {
			jsonString = doHttpGet(serverUrl + "/v3/workspaces/" + workspaceId + "/test-results/" + latestTestId + "/elements/" + entry.getValue() + "/values", Token);
			TransactionsLatestTest.put(entry.getKey(), getValues(jsonString, validElement));
		}
		for (Map.Entry<String, String> entry : TransactionsElementsBaseline.entrySet()) {
			jsonString = doHttpGet(serverUrl + "/v3/workspaces/" + workspaceId + "/test-results/" + testId + "/elements/" + entry.getValue() + "/values", Token);
			TransactionsBaseline.put(entry.getKey(), getValues(jsonString, validElement));
		}

		DecimalFormat df = new DecimalFormat("####0");
		int AntalFel = 0;
		Iterator<Map.Entry<String, String>> itr = TransactionsBaseline.entrySet().iterator();

		double perIncre;
		double baselineValue;
		double latestValue;
		List<String> felMeddelandenList = new ArrayList<>();
		Map<String, List<String>> errorMap = new HashMap<>();

		JSONObject mainJson = new JSONObject();
		JSONArray transactions = new JSONArray();

		while (itr.hasNext()) {
			Map.Entry<String, String> entry = itr.next();

			// Skip entries with "all-userpaths" in the key
			if (entry.getKey().contains("all-userpaths")) {
				continue;
			}

			// Calculate the percentage increase
			perIncre = Double.parseDouble(df.format(checkProcent(entry.getValue(), TransactionsLatestTest.get(entry.getKey()))));

			baselineValue = Double.parseDouble(df.format(parseDouble(entry.getValue())));
			latestValue = Double.parseDouble(df.format(parseDouble(TransactionsLatestTest.get(entry.getKey()))));
			mainJson.put("info", "Test failed because the increase from baseline is bigger than the allowed percentage value");
			mainJson.put("element", validElement);
			mainJson.put("percentage", percentage + "%");
			mainJson.put("baselinetest", baseline);

			// If the percentage increase exceeds the threshold, log an error
			if (perIncre > parseDouble(percentage)) {
				JSONObject transaction = new JSONObject();
				transaction.put("baselineValue", String.valueOf(baselineValue));
				transaction.put("latestValue", String.valueOf(latestValue));
				transaction.put("increase", String.valueOf(perIncre) + "%");
				transaction.put("transaction", entry.getKey());
				// Add the transaction object to the transactions array
				transactions.put(transaction);
				// Populate mainJson with necessary data
				mainJson.put("transactions", transactions);
				AntalFel++; // Increment the error count
			}
		}

		// Process results and return response
		if (AntalFel > 0) {
			// Create a JSON payload for the POST request
			JSONObject jsonDescription = new JSONObject();
			jsonDescription.put("name", testname);
			jsonDescription.put("description", "NeoLoadCompare:\n" + formatJsonObjectWithArrayAndMain(mainJson, "transactions"));
			jsonDescription.put("qualityStatus", "FAILED");

			// Send the HTTP POST request with the error details
			doHttpPost(serverUrl + "/v3/workspaces/" + workspaceId + "/test-results/" + latestTestId, jsonDescription.toString(), Token);

			return ResponseEntity.internalServerError()
					.headers(headers)
					.body(mainJson.toString(4));
		}
		// If no errors, return a success message with test details
		// Create outer JSON object
		JSONObject jsonObject = new JSONObject();
		// Create inner "workspace" object
		JSONObject workspaceObject = new JSONObject();
		workspaceObject.put("name", workspace);
		workspaceObject.put("id", workspaceId);
		// Create inner "baseline" object
		JSONObject baselineObject = new JSONObject();
		baselineObject.put("testId", testId);
		baselineObject.put("name", baseline);
		JSONObject comparisonObject = new JSONObject();
		comparisonObject.put("percentageDifference", percentage);
		comparisonObject.put("element", validElement);
		// Create inner "latestTest" object
		JSONObject latestTestObject = new JSONObject();
		latestTestObject.put("testId", latestTestId);
		latestTestObject.put("name", testname);
		// Adding nested objects to the main JSON object
		jsonObject.put("workspace", workspaceObject);
		jsonObject.put("baseline", baselineObject);
		jsonObject.put("comparison", comparisonObject);
		jsonObject.put("latestTest", latestTestObject);
		// Add "status" field (string)
		jsonObject.put("status", "Check against baseline OK!");

		return ResponseEntity.ok()
				.headers(headers)
				.body(jsonObject.toString(4)
				);
	}

	public String getValues(String jsonString, String value) throws JSONException {
		JSONObject object = new JSONObject(jsonString);
		return object.get(value).toString();
	}

	public static HashMap<String, String> getTransactionElements(String jsonString) throws JSONException {
		HashMap<String, String> Transaction = new HashMap<String, String>();
		JSONArray jsonArray = new JSONArray(jsonString);
		for (int n = 1; n < jsonArray.length(); n++) {
			JSONObject object = jsonArray.getJSONObject(n);
			if (object.get("path").toString().contains("Init")) {
				continue;
			}
			Transaction.put(object.get("name").toString(), object.get("id").toString());
		}
		return Transaction;
	}

	public static double checkProcent(String BaselineResultat, String CurrentResultat) throws JSONException {
		double perIncre;
		perIncre = ((Double.parseDouble(CurrentResultat) - Double.parseDouble(BaselineResultat)) / Double.parseDouble(BaselineResultat)) * 100;
		return perIncre;
	}

	public String getlatestTestId(String jsonString, String Scenario) throws JSONException {
		JSONArray jsonArray = new JSONArray(jsonString);
		JSONObject object = jsonArray.getJSONObject(0);
		if (object.get("scenario").toString().equals(Scenario)) {

		} else {
			return "1";
		}
		return object.get("id").toString();
	}

	public static String getlatestName(String jsonString) throws JSONException {
		JSONArray jsonArray = new JSONArray(jsonString);
		JSONObject object = jsonArray.getJSONObject(0);
		return object.get("name").toString();
	}

	public static String getId(String Name, String Scenario, String jsonString) throws JSONException {
		JSONArray jsonArray = new JSONArray(jsonString);
		for (int n = 0; n < jsonArray.length(); n++) {
			java.util.logging.Logger log = java.util.logging.Logger.getLogger(NeoLoadCompareApplication.class.getName());
			//log.info(jsonString);
			JSONObject object = jsonArray.getJSONObject(n);
			if (object.get("name").toString().equals(Name) && object.get("scenario").toString().equals(Scenario)) {
				return object.get("id").toString();
			}
		}
		return "NOT FOUND";
	}

	public static String getWorkspaceId(String Name, String jsonString) throws JSONException {
		JSONArray jsonArray = new JSONArray(jsonString);
		for (int n = 0; n < jsonArray.length(); n++) {
			JSONObject object = jsonArray.getJSONObject(n);
			if (object.get("name").toString().equals(Name)) {
				return object.get("id").toString();
			}
		}
		return "NOT FOUND";
	}

	public class ColorLogger {

		private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger("");

		public void logDebug(String logging) {
			LOGGER.debug("\u001B[92m" + logging + "\u001B[0m");
		}

		public void logInfo(String logging) {
			LOGGER.info("\u001B[93m" + logging + "\u001B[0m");
		}

		public void logError(String logging) {
			LOGGER.error("\u001B[91m" + logging + "\u001B[0m");
		}
	}

	// Helper methods for building headers, validating elements, and making API requests
	private HttpHeaders createHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.CACHE_CONTROL, "no-cache");
		headers.add(HttpHeaders.CONTENT_TYPE, "text/plain; charset=UTF-8");
		headers.add(HttpHeaders.CONTENT_ENCODING, "UTF-8");
		return headers;
	}
	private ResponseEntity<String> logAndRespondError(String errorMessage) {
		colorLogger.logError(errorMessage);
		return ResponseEntity.internalServerError()
				.headers(createHeaders())
				.body("{\"error\":\"" + errorMessage + "\"}");
	}

	private ResponseEntity<String> Respond(String Message) {
		return ResponseEntity.ok()
				.headers(createHeaders())
				.body("{\"message\":\"" + Message + "\"}");
	}

	private Optional<String> validateElement(String element) {
		Set<String> validElements = Set.of("avgduration", "avgDuration", "percentile90", "percentile95", "percentile99","no","nej", "false");
		String lowerElement = element.toLowerCase();
		if (lowerElement.contains("avgduration")) {
			lowerElement = "avgDuration";
		}
		return validElements.contains(lowerElement) ? Optional.of(lowerElement) : Optional.empty();
	}

	private static final String CONTENT_TYPE = "application/json";
	private static final String ACCEPT = "application/json";
	private static final String AUTH_ERROR_MSG = "ERROR: Unauthorized - check API token";
	private static final String UNAUTHORIZED_KEYWORD = "Unauthorized";

	private CloseableHttpClient createHttpClient() {
		try {
			SSLContext sslContext = SSLContextBuilder.create()
					.loadTrustMaterial(null, (TrustStrategy) (cert, authType) -> true)
					.build();

			return HttpClients.custom()
					.setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
					.setSSLContext(sslContext)
					.build();
		} catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
			throw new RuntimeException("Failed to create SSL context for HTTP client", e);
		}
	}

	public String doHttpGet(String url, String accountToken) throws IOException, URISyntaxException {
		HttpUriRequest request = RequestBuilder.get()
				.setUri(url)
				.setHeader(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE)
				.setHeader(HttpHeaders.ACCEPT, ACCEPT)
				.setHeader("accountToken", accountToken)
				.build();

		return executeRequest(request);
	}

	public String doHttpPost(String url, String body, String accountToken) throws IOException, URISyntaxException {
		HttpUriRequest request = RequestBuilder.patch()
				.setUri(url)
				.setHeader(HttpHeaders.CONTENT_TYPE, CONTENT_TYPE)
				.setHeader(HttpHeaders.ACCEPT, ACCEPT)
				.setHeader("accountToken", accountToken)
				.setEntity(new StringEntity(body, StandardCharsets.UTF_8))
				.build();

		return executeRequest(request);
	}

	private String executeRequest(HttpUriRequest request) throws IOException {
		try (CloseableHttpClient httpClient = createHttpClient();
			 CloseableHttpResponse response = httpClient.execute(request)) {

			String result = EntityUtils.toString(response.getEntity());
			if (result.contains(UNAUTHORIZED_KEYWORD)) {
				colorLogger.logError(AUTH_ERROR_MSG);
				throw new IOException(AUTH_ERROR_MSG);
			}
			return result;
		}
	}

	public String formatJsonObjectWithArrayAndMain(JSONObject jsonObject, String arrayKey) {
		StringBuilder result = new StringBuilder();

		// Process key-value pairs from the main JSON object, excluding the array
		for (String key : jsonObject.keySet()) {
			if (!key.equals(arrayKey)) { // Skip the array
				result.append("\"").append(key).append("\":\"")
						.append(jsonObject.get(key)).append("\"\n");
			}
		}

		// Check if the key exists and is a JSONArray
		if (jsonObject.has(arrayKey) && jsonObject.get(arrayKey) instanceof JSONArray) {
			JSONArray jsonArray = jsonObject.getJSONArray(arrayKey);

			for (int i = 0; i < jsonArray.length(); i++) {
				JSONObject arrayObject = jsonArray.getJSONObject(i);
				result.append("\n");
				// Process each key-value pair in the JSONObject
				for (String arrayKeyItem : arrayObject.keySet()) {
					result.append("\"").append(arrayKeyItem).append("\":\"")
							.append(arrayObject.get(arrayKeyItem)).append("\"\n");
				}
				result.append("\n\n");
			}
		} else {
			result.append("No valid JSONArray found for key: ").append(arrayKey).append("\n");
		}
		return result.toString().replace("\"", "").replace("."," : ").replace(": 0","");
	}
}
