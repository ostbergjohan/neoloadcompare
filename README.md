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

### Example Response: 500 Internal Server Error (to trigger fail och pipeline)

In case of an error, such as exceeding the allowed percentage difference from the baseline, the response may look like this:

```json
{
    "percentage": "50%",
    "baselinetest": "6",
    "transactions": [
        {
            "baselineValue": "542.0",
            "latestValue": "869.0",
            "increase": "60.0%",
            "transaction": "get-stub-wiremock-faker"
        }
    ],
    "info": "Test failed because the increase from baseline is bigger than the allowed percentage value",
    "element": "avgDuration",
    "currenttest": "#11"
}

