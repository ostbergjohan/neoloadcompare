# NeoloadCompare

NeoloadCompare is a tool that automatically compares performance test results against a baseline using the NeoLoad Web API. It evaluates key performance metrics, such as average response times and percentiles, to ensure your system's performance is consistent with expectations.

## Features

- Compare **average response times** and **percentiles** (e.g., 95th percentile) between the latest test and a baseline.
- Ignores "init" transactions and compares only those in the "actions" part of the user flow.
- Automatically runs after a performance test in the pipeline.
- Fails the test if any transaction's response time deviates more than a set percentage from the baseline.

## Usage

After your performance test is completed, NeoloadCompare is invoked automatically to compare the results against a predefined baseline.

### API Request

NeoloadCompare uses the following parameters:

- `workspace`: The name of the workspace.
- `BaselineTest`: The test ID of the baseline to compare against.
- `scenario`: The name of the scenario being run.
- `element`: The performance metric to compare (e.g., `avgDuration`, `percentile90`, `percentile95`, `percentile99`). Use `nej` if no comparison is needed.
- `percentage`: The maximum allowable deviation percentage from the baseline before the test fails.

### Example Request

```bash
http://url/NeoLoadCompare?workspace=demo&scenario=demo_scenario&baseline=1&percentage=10&element=avgduration
