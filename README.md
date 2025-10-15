# 🚀 NeoLoadCompare

[![Docker Hub](https://img.shields.io/badge/Docker%20Hub-johanostberg%2Fneoloadcompare-blue)](https://hub.docker.com/repository/docker/johanostberg/neoloadcompare)
[![GitHub](https://img.shields.io/badge/GitHub-ostbergjohan%2Fneoloadcompare-black)](https://github.com/ostbergjohan/neoloadcompare)
[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-green)](https://spring.io/projects/spring-boot)

NeoLoadCompare automatically compares performance test results against a baseline using NeoLoad Web's API. It validates transaction response times, percentiles, and triggers a failure if discrepancies exceed the defined threshold percentage.

## 📋 Table of Contents

- [Features](#-features)
- [Quick Start](#-quick-start)
- [API Documentation](#-api-documentation)
- [Parameters](#-parameters)
- [Response Examples](#-response-examples)
- [Configuration](#%EF%B8%8F-configuration)
- [Deployment](#-deployment)
- [CI/CD Integration](#-cicd-integration)
- [NeoLoad CLI Pipeline Example](#-neoload-cli-pipeline-example)
- [Health Check](#-health-check)

## ✨ Features

- ✅ Compare **average response times** and **percentiles** (90th, 95th, 99th) between latest test and baseline
- 🎯 Automatically **filters out "Init" transactions** and compares only "Actions" transactions
- 🔄 **CI/CD integration ready** - designed to run after performance tests in pipelines
- ❌ **Fails tests automatically** when transactions exceed threshold percentage
- 📊 **Updates NeoLoad Web** test description with detailed failure information
- 🔍 **OpenAPI/Swagger documentation** available at `/swagger-ui/index.html`
- 🐳 **Pre-built Docker image** available on Docker Hub

## 🚀 Quick Start

### Docker Hub Image

Pull and run the pre-built image from Docker Hub:

```bash
docker run -d \
  -p 8080:8080 \
  -e Server=https://your-neoload-server.com \
  -e Token=your-api-token \
  johanostberg/neoloadcompare:latest
```

### API Endpoint

```
GET /NeoLoadCompare?workspace={workspace}&scenario={scenario}&baseline={baseline}&percentage={percentage}&element={element}
```

### Example Request

```bash
curl "http://localhost:8080/NeoLoadCompare?workspace=demo&scenario=demo_scenario&baseline=1&percentage=10&element=avgduration"
```

## 📚 API Documentation

Once the application is running, access the interactive API documentation:

- **Swagger UI**: `http://localhost:8080/swagger-ui/index.html`
- **OpenAPI JSON**: `http://localhost:8080/v3/api-docs`

## 📝 Parameters

| Parameter | Required | Description | Example Values |
|-----------|----------|-------------|----------------|
| `workspace` | ✅ Yes | The name of the NeoLoad workspace | `demo`, `production`, `qa-env` |
| `scenario` | ✅ Yes | The name of the test scenario | `demo_scenario`, `load_test_v2` |
| `baseline` | ✅ Yes | The baseline test number for comparison | `1`, `5`, `10` |
| `percentage` | ✅ Yes | Maximum allowed percentage increase from baseline | `5`, `10`, `25` |
| `element` | ✅ Yes | Performance metric to compare | `avgduration`, `percentile90`, `percentile95`, `percentile99`, `false` |

> **Note:** Setting `element=false` disables baseline comparison and returns immediately.

## 📊 Response Examples

### ✅ Success Response (HTTP 200)

When all transactions are within the acceptable threshold:

```json
{
    "workspace": {
        "name": "demo",
        "id": "62568bb580cec421c189ff01"
    },
    "baseline": {
        "testId": "080a9251-c653-4279-abd3-7fc5e17a54ad",
        "name": "1"
    },
    "comparison": {
        "percentageDifference": "10",
        "element": "avgDuration"
    },
    "latestTest": {
        "testId": "4cc0ccc3-f3f2-4325-b201-9f5f68607a61",
        "name": "#11"
    },
    "status": "Check against baseline OK!"
}
```

### ❌ Failure Response (HTTP 500)

When one or more transactions exceed the threshold:

```json
{
    "info": "Test failed because the increase from baseline is bigger than the allowed percentage value",
    "element": "avgDuration",
    "percentage": "10%",
    "baselinetest": "1",
    "transactions": [
        {
            "baselineValue": "527.0",
            "latestValue": "1054.0",
            "increase": "100.0%",
            "transaction": "transaction uno"
        },
        {
            "baselineValue": "503.0",
            "latestValue": "1006.0",
            "increase": "100.0%",
            "transaction": "transaction dos"
        }
    ]
}
```

> **Important:** The test description in NeoLoad Web is automatically updated with failure details.

### Response Fields Explained

#### Success Response
- **workspace**: Contains workspace name and ID
- **baseline**: Information about the baseline test used for comparison
- **comparison**: Details of the comparison criteria (percentage threshold and element)
- **latestTest**: Information about the most recent test that was compared
- **status**: Overall status message

#### Failure Response
- **info**: Explanation message for the failure
- **element**: The performance metric compared (e.g., `avgDuration`, `percentile90`)
- **percentage**: The allowed percentage threshold that was exceeded
- **baselinetest**: The baseline test number
- **transactions**: Array of transactions that failed the comparison
  - **baselineValue**: Baseline value for the transaction (milliseconds)
  - **latestValue**: Value from the latest test (milliseconds)
  - **increase**: Percentage increase from baseline
  - **transaction**: Name of the transaction that failed

### HTTP Status Codes

| Status Code | Description |
|------------|-------------|
| **200 OK** | All transactions passed the baseline comparison within the defined threshold |
| **500 Internal Server Error** | One or more transactions exceeded the allowed percentage increase from baseline |

## ⚙️ Configuration

### Required Environment Variables

| Variable | Description | Example |
|----------|-------------|---------|
| `Server` | NeoLoad API server URL | `https://neoload-api.example.com` |
| `Token` | NeoLoad API authentication token | `your-api-token-here` |

> **⚠️ Warning:** The application will not start without these environment variables.

## 🐳 Deployment

### Docker

```bash
# Pull from Docker Hub
docker pull johanostberg/neoloadcompare:latest

# Run with environment variables
docker run -d \
  -p 8080:8080 \
  -e Server=https://your-neoload-server.com \
  -e Token=your-api-token \
  --name neoloadcompare \
  johanostberg/neoloadcompare:latest
```

### Podman

```bash
# Build from source
podman build -t neoloadcompare:latest -f Containerfile .

# Run with environment variables
podman run -d \
  -e Server=https://your-neoload-server.com \
  -e Token=your-api-token \
  -p 8080:8080 \
  --name neoloadcompare \
  neoloadcompare:latest
```

### Kubernetes

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: neoloadcompare-config
data:
  server: "https://your-neoload-server.com"
---
apiVersion: v1
kind: Secret
metadata:
  name: neoloadcompare-secret
type: Opaque
stringData:
  token: "your-api-token"
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: neoloadcompare
spec:
  replicas: 1
  selector:
    matchLabels:
      app: neoloadcompare
  template:
    metadata:
      labels:
        app: neoloadcompare
    spec:
      containers:
      - name: neoloadcompare
        image: johanostberg/neoloadcompare:latest
        ports:
        - containerPort: 8080
        env:
        - name: Server
          valueFrom:
            configMapKeyRef:
              name: neoloadcompare-config
              key: server
        - name: Token
          valueFrom:
            secretKeyRef:
              name: neoloadcompare-secret
              key: token
---
apiVersion: v1
kind: Service
metadata:
  name: neoloadcompare
spec:
  selector:
    app: neoloadcompare
  ports:
  - protocol: TCP
    port: 8080
    targetPort: 8080
  type: ClusterIP
```

### OpenShift

```yaml
apiVersion: apps.openshift.io/v1
kind: DeploymentConfig
metadata:
  name: neoloadcompare
spec:
  replicas: 1
  selector:
    app: neoloadcompare
  template:
    metadata:
      labels:
        app: neoloadcompare
    spec:
      containers:
      - name: neoloadcompare
        image: johanostberg/neoloadcompare:latest
        ports:
        - containerPort: 8080
        env:
        - name: Server
          valueFrom:
            configMapKeyRef:
              name: neoloadcompare-config
              key: server
        - name: Token
          valueFrom:
            secretKeyRef:
              name: neoloadcompare-secret
              key: token
---
apiVersion: v1
kind: Service
metadata:
  name: neoloadcompare
spec:
  selector:
    app: neoloadcompare
  ports:
  - port: 8080
    targetPort: 8080
---
apiVersion: route.openshift.io/v1
kind: Route
metadata:
  name: neoloadcompare
spec:
  to:
    kind: Service
    name: neoloadcompare
  port:
    targetPort: 8080
```

## 🔄 CI/CD Integration

### Jenkins Pipeline

```groovy
pipeline {
    agent any
    
    stages {
        stage('Performance Test') {
            steps {
                // Run your NeoLoad test here
                echo 'Running performance test...'
            }
        }
        
        stage('Baseline Validation') {
            steps {
                script {
                    def response = sh(
                        script: """
                            curl -s -o response.json -w "%{http_code}" \
                            "http://neoloadcompare:8080/NeoLoadCompare?\
workspace=production&\
scenario=api_load_test&\
baseline=5&\
percentage=10&\
element=avgduration"
                        """,
                        returnStdout: true
                    ).trim()
                    
                    if (response != "200") {
                        def result = readJSON file: 'response.json'
                        error("Performance test failed: ${result.info}")
                    } else {
                        echo "Baseline validation passed!"
                    }
                }
            }
        }
    }
    
    post {
        always {
            archiveArtifacts artifacts: 'response.json', allowEmptyArchive: true
        }
    }
}
```

### GitLab CI

```yaml
stages:
  - test
  - validate

performance_test:
  stage: test
  script:
    - echo "Running performance test..."
    # Your NeoLoad test execution here

performance_validation:
  stage: validate
  script:
    - |
      HTTP_CODE=$(curl -s -o response.json -w "%{http_code}" \
        "http://neoloadcompare:8080/NeoLoadCompare?\
workspace=production&\
scenario=api_load_test&\
baseline=5&\
percentage=10&\
element=avgduration")
      
      if [ "$HTTP_CODE" != "200" ]; then
        echo "Performance validation failed!"
        cat response.json
        exit 1
      else
        echo "Performance validation passed!"
      fi
  artifacts:
    when: always
    paths:
      - response.json
    expire_in: 30 days
```

### GitHub Actions

```yaml
name: Performance Test Validation

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  performance-validation:
    runs-on: ubuntu-latest
    
    steps:
    - name: Checkout code
      uses: actions/checkout@v3
    
    - name: Run Performance Test
      run: |
        echo "Running performance test..."
        # Your NeoLoad test execution here
    
    - name: Validate Against Baseline
      run: |
        HTTP_CODE=$(curl -s -o response.json -w "%{http_code}" \
          "http://neoloadcompare:8080/NeoLoadCompare?\
workspace=production&\
scenario=api_load_test&\
baseline=5&\
percentage=10&\
element=avgduration")
        
        if [ "$HTTP_CODE" != "200" ]; then
          echo "Performance validation failed!"
          cat response.json
          exit 1
        else
          echo "Performance validation passed!"
        fi
    
    - name: Upload Results
      if: always()
      uses: actions/upload-artifact@v3
      with:
        name: performance-results
        path: response.json
```

### Azure DevOps

```yaml
trigger:
- main

pool:
  vmImage: 'ubuntu-latest'

stages:
- stage: PerformanceTest
  jobs:
  - job: RunTest
    steps:
    - script: |
        echo "Running performance test..."
        # Your NeoLoad test execution here
      displayName: 'Execute Performance Test'

- stage: ValidateBaseline
  jobs:
  - job: Validate
    steps:
    - script: |
        HTTP_CODE=$(curl -s -o $(Build.ArtifactStagingDirectory)/response.json -w "%{http_code}" \
          "http://neoloadcompare:8080/NeoLoadCompare?\
workspace=production&\
scenario=api_load_test&\
baseline=5&\
percentage=10&\
element=avgduration")
        
        if [ "$HTTP_CODE" != "200" ]; then
          echo "Performance validation failed!"
          cat $(Build.ArtifactStagingDirectory)/response.json
          exit 1
        else
          echo "Performance validation passed!"
        fi
      displayName: 'Validate Against Baseline'
    
    - task: PublishBuildArtifacts@1
      condition: always()
      inputs:
        PathtoPublish: '$(Build.ArtifactStagingDirectory)/response.json'
        ArtifactName: 'performance-results'
```

## 🔧 NeoLoad CLI Pipeline Example

Complete pipeline example using [NeoLoad CLI](https://github.com/Neotys-Labs/neoload-cli):

### Prerequisites

- [NeoLoad CLI](https://github.com/Neotys-Labs/neoload-cli) installed
- Access to NeoLoad Web
- NeoLoad test project configured

### Pipeline Script

```bash
#!/usr/bin/bash

# ============================================
# Configuration Variables
# ============================================
workspace="demo"                              # Workspace name
token="your-neoload-token"                    # Personal NeoLoad Web token
scenario="demo_scenario"                      # Scenario name
testname="PERFORMANCE_TEST"                   # Test name in NeoLoad Web
testpath="/path/to/neoload/project"          # Path to NeoLoad test files
zone="default-zone"                          # NeoLoad Web zone for LGs and controller
baseline="1"                                 # Baseline test number
percentage="10"                              # Allowed percentage deviation
element="avgduration"                        # Metric to compare
neoload_api="https://neoload-api.example.com" # NeoLoad API URL
compare_api="http://neoloadcompare:8080"     # NeoLoadCompare API URL

# ============================================
# Step 1: Log in to NeoLoad Web
# ============================================
echo "🔐 Logging in to NeoLoad Web..."
neoload login --ssl-cert False \
              --url $neoload_api \
              --workspace $workspace $token

if [ $? -ne 0 ]; then
    echo "❌ Login failed!"
    exit 1
fi
echo "✅ Login successful"

# ============================================
# Step 2: Create or Update Test Settings
# ============================================
echo "⚙️ Configuring test settings..."
neoload test-settings --zone $zone \
                      --lgs 1 \
                      --scenario $scenario \
                      createorpatch $testname

if [ $? -ne 0 ]; then
    echo "❌ Test settings configuration failed!"
    exit 1
fi
echo "✅ Test settings configured"

# ============================================
# Step 3: Upload NeoLoad Project
# ============================================
echo "📤 Uploading NeoLoad project..."
neoload project -p $testpath upload $testname

if [ $? -ne 0 ]; then
    echo "❌ Project upload failed!"
    exit 1
fi
echo "✅ Project uploaded"

# ============================================
# Step 4: Run the Test
# ============================================
echo "🚀 Running performance test..."
neoload run

if [ $? -ne 0 ]; then
    echo "❌ Test execution failed!"
    exit 1
fi
echo "✅ Test completed"

# ============================================
# Step 5: Compare Against Baseline
# ============================================
echo "📊 Comparing results against baseline..."
HTTP_CODE=$(curl -s -o response.json -w "%{http_code}" \
    "$compare_api/NeoLoadCompare?\
workspace=$workspace&\
scenario=$scenario&\
baseline=$baseline&\
percentage=$percentage&\
element=$element")

echo "Response code: $HTTP_CODE"

if [ "$HTTP_CODE" == "200" ]; then
    echo "✅ Performance validation PASSED!"
    cat response.json | jq '.'
    exit 0
else
    echo "❌ Performance validation FAILED!"
    echo "Transactions exceeded baseline threshold:"
    cat response.json | jq '.'
    exit 1
fi
```

### Make Script Executable

```bash
chmod +x run_performance_test.sh
./run_performance_test.sh
```

## 🏥 Health Check

Verify the service is running:

```bash
curl http://localhost:8080/healthcheck
```

**Response:**
```json
{
    "status": "ok",
    "service": "API Health Check"
}
```

## 📖 Additional Resources

- 📚 [Interactive API Documentation (Swagger UI)](http://localhost:8080/swagger-ui/index.html)
- 📄 [OpenAPI JSON Specification](http://localhost:8080/v3/api-docs)
- 🐙 [GitHub Repository](https://github.com/ostbergjohan/neoloadcompare)
- 🐳 [Docker Hub Image](https://hub.docker.com/repository/docker/johanostberg/neoloadcompare)
- 📖 [NeoLoad Documentation](https://www.tricentis.com/products/performance-testing-neoload)
- 🔧 [NeoLoad CLI](https://github.com/Neotys-Labs/neoload-cli)

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 📄 License

This project is licensed under the Apache License 2.0.

## 🔖 Version Information

- **Version**: 1.0
- **Java Version**: 21
- **Spring Boot**: 3.3.4
- **OpenAPI**: 3.0

---

Made with ❤️ for performance engineers