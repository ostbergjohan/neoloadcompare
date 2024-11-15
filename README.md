# NeoloadCompare

NeoloadCompare is a tool that automatically compares performance test results against a baseline using the NeoLoad Web API. It evaluates key performance metrics, such as average response times and percentiles, to ensure your system's performance is consistent with expectations.

## Features

- Compare **average response times** and **percentiles** (e.g., 95th percentile) between the latest test and a baseline.
- Ignores "init" transactions and compares only those in the "actions" part of the user flow.
- run this after a performance test in a pipeline.
- Fails the test if any transaction's response time deviates more than a set percentage from the baseline.

## Usage

After your performance test is completed, run NeoloadCompare to automatically compare the results against a predefined baseline.

### API Request

NeoloadCompare uses the following parameters:

- `workspace`: The name of the workspace.
- `BaselineTest`: The test ID of the baseline to compare against.
- `scenario`: The name of the scenario being run.
- `element`: The performance metric to compare (e.g., `avgDuration`, `percentile90`, `percentile95`, `percentile99`). Use `false` if no comparison is needed.
- `percentage`: The maximum allowable deviation percentage from the baseline before the test fails.

### Example Request

http://your_endpoint/NeoLoadCompare?workspace=demo&scenario=demo_scenario&baseline=1&percentage=10&element=avgduration

### Example Response 200 OK
The following is an example of a successful response from NeoloadCompare:

```json
{
    "workspace": {
        "name": "demo",
        "id": "62568bb580cec421c189ff01"
    },
    "comparison": {
        "percentageDifference": "1",
        "element": "avgDuration"
    },
    "latestTest": {
        "name": "#11",
        "testId": "4cc0ccc3-f3f2-4325-b201-9f5f68607a61"
    },
    "baseline": {
        "name": "6",
        "testId": "080a9251-c653-4279-abd3-7fc5e17a54ad"
    },
    "status": "Check against baseline OK!"
}
```
### Explanation of Fields:

- **percentage**: The allowed percentage difference that was set for the test.
- **baselinetest**: The ID of the baseline test that the current test is compared to.
- **transactions**: A list of transactions that failed the comparison due to exceeding the allowed percentage difference.
    - **baselineValue**: The baseline value for the transaction.
    - **latestValue**: The value from the latest test for the transaction.
    - **increase**: The percentage increase between the baseline value and the latest test value.
    - **transaction**: The name or ID of the transaction that caused the failure.
- **info**: A message explaining why the test failed.
- **element**: The performance metric that was compared (e.g., `avgDuration`).
- **currenttest**: The ID or name of the current test being compared to the baseline.

### Example Response: 500 Internal Server Error (to trigger fail of pipeline)

In case of an error, such as exceeding the allowed percentage difference from the baseline, the response may look like this:

```json
{
    "percentage": "30%",
    "baselinetest": "1",
    "transactions": [
        {
            "baselineValue": "527.0",
            "latestValue": "1054.0",
            "increase": "100.0%",
            "transaction": "Transaktion uno"
        },
        {
            "baselineValue": "503.0",
            "latestValue": "1006.0",
            "increase": "100.0%",
            "transaction": "Transaktion dos"
        },
        {
            "baselineValue": "503.0",
            "latestValue": "1006.0",
            "increase": "100.0%",
            "transaction": "Tramsaktion tres"
        }
    ],
    "info": "Test failed because the increase from baseline is bigger than the allowed percentage value",
    "element": "avgDuration"
}
```

### Explanation of Fields:

- **percentage**: The allowed percentage difference that was set for the test.
- **baselinetest**: The ID of the baseline test that the current test is compared to.
- **transactions**: A list of transactions that failed the comparison due to exceeding the allowed percentage difference.
  - **baselineValue**: The baseline value for the transaction.
  - **latestValue**: The value from the latest test for the transaction.
  - **increase**: The percentage increase between the baseline value and the latest test value.
  - **transaction**: The name or ID of the transaction that caused the failure.
- **info**: A message explaining why the test failed.
- **element**: The performance metric that was compared (e.g., `avgDuration`).
- **currenttest**: The ID or name of the current test being compared to the baseline.

This repository contains the source code for the NeoLoad Compare Application, a tool designed to analyze and compare test results from NeoLoad.  

The application is containerized and available as a prebuilt Docker image on Docker Hub.

---

## **Running the Application**

### **Docker Hub Image**  
The application is available on Docker Hub:  
[**Docker Hub - NeoLoad Compare**](https://hub.docker.com/repository/docker/johanostberg/neoloadcompare)

### **Running with Docker**  
To run the application, use the following command:  
```bash
docker run -d \
  -p 8080:8080 \
  -e Server=<neoloadapi_server> \
  -e Token=<admin_token> \
  your-dockerhub-username/neoloadcompare:latest
```

This example demonstrates a simple pipeline to automate:
1. Logging in to NeoLoad Web.
2. Setting up test configurations.
3. Uploading NeoLoad test files.
4. Executing a test.
5. Retrieving test comparison results using a `curl` command.


```bash
#!/usr/bin/bash

# Configuration Variables
workspace="demo"           # Workspace name
token="xx"                 # Personal NeoloadWeb token
scenario="demo_scenario"   # Scenario name
testname="TEST"            # Test name in NeoLoad Web
testpath="/git/mytests"    # Path to NeoLoad test files
zone="abcs"                # NeoLoad Web zone for load generators (LGs) and controller

# Step 1: Log in to NeoLoad Web
neoload login --ssl-cert False \
              --url https://neoload-api-endpoint \
              --workspace $workspace $token

# Step 2: Create or Patch Test Settings
neoload test-settings --zone $zone \
                      --lgs 1 \
                      --scenario $scenario \
                      createorpatch $testname

# Step 3: Upload NeoLoad Project
neoload project -p $testpath upload $testname

# Step 4: Run the Test
neoload run

# NeoLoad Compare URL

# Step 5: Add Curl Command to Fetch Comparison URL Data
curl -X GET "http://api-endpoint/NeoLoadCompare?workspace=demo&scenario=demo_scenario&baseline=1&percentage=10&element=avgduration"
```
