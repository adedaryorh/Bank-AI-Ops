# Manual sandbox tests: Docker first, Kubernetes second

Copy one code block at a time. This is a command reference, not a script. Stop when a request fails; inspect the response and server logs before continuing. Commands assume Bash, curl, jq, Go, Docker Compose and your trusted `.env`. Kubernetes additionally requires kubectl and a reachable development cluster. These commands have not been executed against your live infrastructure here.

Use Terminal 1 for the Go server and Terminal 2 for API requests. Secrets remain in `.env`. Do not enable `set -x`.

## A. Docker startup — Terminal 1

```bash
cd ~/Music/sandboxing_go/sandboxing_go
```

Edit `.env` and ensure these settings are present. Keep your actual database password and provisioned API key; DATABASE_URL must use port 5433.

```dotenv
SANDBOX_BACKEND=docker
PUBLIC_API_URL=http://localhost:8080
ENABLE_CHECKPOINTS=false
LOCAL_REGION=default
WARM_PYTHON=0
WARM_NODE=0
WARM_UBUNTU=0
```

```bash
set -a
source .env
set +a
```

```bash
docker version
```

Both Client and Server information should appear.

```bash
docker compose up -d postgres caddy
```

```bash
docker compose exec postgres pg_isready -U sandboxing -d sandboxing
```

Wait and repeat until it says `accepting connections`.

```bash
docker pull python:3.11-slim
```

```bash
docker pull ubuntu:22.04
```

```bash
docker pull node:20-slim
```

```bash
docker pull golang:1.22-bookworm
```

```bash
docker pull ruby:3.3-slim
```

```bash
go run ./cmd/server
```

Leave Terminal 1 running. Initialization must succeed before proceeding.

## B. Client setup — Terminal 2 (both backends)

```bash
cd ~/Music/sandboxing_go/sandboxing_go
set -a
source .env
set +a
API='http://localhost:8080'
CLIENT_KEY="$SANDBOX_BOOTSTRAP_API_KEY"
```

This tests the bootstrap tenant. To test a separately provisioned client, assign its key to CLIENT_KEY instead.

```bash
client_api() {
  curl --fail-with-body -sS --connect-timeout 10 --max-time 180 \
    -H "Authorization: Bearer $CLIENT_KEY" "$@"
}
```

```bash
curl -i "$API/health"
```

Expected HTTP 200; this only verifies the HTTP handler.

```bash
curl -i "$API/v1/sandboxes"
```

Expected HTTP 401 without authentication.

```bash
client_api "$API/v1/sandboxes" | jq .
```

Expected JSON containing `sandboxes`.

## C. Create a sandbox — both backends

Choose ONE template block below. Each replaces SB_ID. Delete that sandbox using section H before testing the next template, so you do not lose track of resources or exceed quotas. Use Python first for the complete file workflow.

### Python

```bash
RESPONSE=$(client_api -X POST "$API/v1/sandboxes" \
  -H 'Content-Type: application/json' \
  -d '{"template":"python","cpu":"0.5","memory":"512Mi","ttl":"1h","isolation":"standard"}')
printf '%s\n' "$RESPONSE" | jq .
SB_ID=$(printf '%s\n' "$RESPONSE" | jq -er '.id')
```

### Ubuntu

```bash
RESPONSE=$(client_api -X POST "$API/v1/sandboxes" \
  -H 'Content-Type: application/json' \
  -d '{"template":"ubuntu","cpu":"0.5","memory":"512Mi","ttl":"1h","isolation":"standard"}')
printf '%s\n' "$RESPONSE" | jq .
SB_ID=$(printf '%s\n' "$RESPONSE" | jq -er '.id')
```

### Node.js

```bash
RESPONSE=$(client_api -X POST "$API/v1/sandboxes" \
  -H 'Content-Type: application/json' \
  -d '{"template":"node","cpu":"0.5","memory":"512Mi","ttl":"1h","isolation":"standard"}')
printf '%s\n' "$RESPONSE" | jq .
SB_ID=$(printf '%s\n' "$RESPONSE" | jq -er '.id')
```

### Go

```bash
RESPONSE=$(client_api -X POST "$API/v1/sandboxes" \
  -H 'Content-Type: application/json' \
  -d '{"template":"go","cpu":"0.5","memory":"512Mi","ttl":"1h","isolation":"standard"}')
printf '%s\n' "$RESPONSE" | jq .
SB_ID=$(printf '%s\n' "$RESPONSE" | jq -er '.id')
```

### Ruby

```bash
RESPONSE=$(client_api -X POST "$API/v1/sandboxes" \
  -H 'Content-Type: application/json' \
  -d '{"template":"ruby","cpu":"0.5","memory":"512Mi","ttl":"1h","isolation":"standard"}')
printf '%s\n' "$RESPONSE" | jq .
SB_ID=$(printf '%s\n' "$RESPONSE" | jq -er '.id')
```

Stop if the response contains an error or SB_ID is empty/null.

```bash
client_api "$API/v1/sandboxes/$SB_ID" | jq .
```

Expected status `running`.

Docker-only verification:

```bash
docker ps --filter "label=sandbox.id=$SB_ID"
```

Kubernetes-only verification:

```bash
kubectl -n sandboxes get pods -l "sandbox-id=$SB_ID" -o wide
```

## D. Execute commands — both backends

Works in all five templates:

```bash
client_api -X POST "$API/v1/sandboxes/$SB_ID/exec" \
  -H 'Content-Type: application/json' \
  -d '{"cmd":"cat /etc/os-release; id; ls -la /workspace","timeout_seconds":30}' | jq .
```

Choose the command matching your template:

```bash
client_api -X POST "$API/v1/sandboxes/$SB_ID/exec" -H 'Content-Type: application/json' \
  -d '{"cmd":"python -c \"print(2 + 3)\""}' | jq .
```

```bash
client_api -X POST "$API/v1/sandboxes/$SB_ID/exec" -H 'Content-Type: application/json' \
  -d '{"cmd":"node -e \"console.log(2 + 3)\""}' | jq .
```

```bash
client_api -X POST "$API/v1/sandboxes/$SB_ID/exec" -H 'Content-Type: application/json' \
  -d '{"cmd":"go version"}' | jq .
```

```bash
client_api -X POST "$API/v1/sandboxes/$SB_ID/exec" -H 'Content-Type: application/json' \
  -d '{"cmd":"ruby -e \"puts 2 + 3\""}' | jq .
```

```bash
client_api -X POST "$API/v1/sandboxes/$SB_ID/exec" -H 'Content-Type: application/json' \
  -d '{"cmd":"echo Hello from Ubuntu"}' | jq .
```

Expected exit_code 0. An HTTP success does not itself prove the command succeeded.

## E. File workflow — Python sandbox, either backend

Create a local test file:

```bash
cat > /tmp/sandbox-manual-demo.py <<'PY'
import json
from pathlib import Path
result = {"total": sum([10, 20, 30, 40]), "average": 25}
Path('/workspace/result.json').write_text(json.dumps(result))
print('Calculation finished')
PY
```

Upload:

```bash
client_api -X PUT "$API/v1/sandboxes/$SB_ID/files/workspace/main.py" \
  -H 'Content-Type: text/plain' --data-binary @/tmp/sandbox-manual-demo.py
```

Read:

```bash
client_api "$API/v1/sandboxes/$SB_ID/files/workspace/main.py"
```

Execute:

```bash
client_api -X POST "$API/v1/sandboxes/$SB_ID/exec" -H 'Content-Type: application/json' \
  -d '{"cmd":"python /workspace/main.py","timeout_seconds":30}' | jq .
```

Download the result:

```bash
client_api "$API/v1/sandboxes/$SB_ID/files/workspace/result.json" | jq .
```

Expected total 100 and average 25.

Check nonzero exit reporting:

```bash
client_api -X POST "$API/v1/sandboxes/$SB_ID/exec" -H 'Content-Type: application/json' \
  -d '{"cmd":"python -c \"raise SystemExit(7)\""}' | jq .
```

Expected exit_code 7.

## F. Metrics, quota, audit, MCP — both backends

```bash
client_api "$API/v1/sandboxes/$SB_ID/metrics" | jq .
```

Kubernetes metrics require metrics-server; unavailable metrics are not a passing test.

```bash
client_api "$API/v1/quota" | jq .
```

```bash
client_api "$API/v1/audit" | jq .
```

```bash
client_api -X POST "$API/v1/sandboxes/$SB_ID/mcp/enable" | jq .
```

```bash
client_api -X POST "$API/v1/sandboxes/$SB_ID/mcp/rpc" \
  -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list","params":{}}' | jq .
```

Expected result.tools, without a JSON-RPC error. This verifies discovery, not all MCP tool behavior.

## G. Tenant ownership — both backends

Provision a different tenant key. This writes the raw key to a private ignored file, not a shell-compatible environment file.

```bash
(umask 077; go run ./cmd/key --tenant second-client > .env.second-client)
```

```bash
OTHER_KEY=$(cat .env.second-client)
curl -sS -o /dev/null -w 'HTTP %{http_code}\n' \
  -H "Authorization: Bearer $OTHER_KEY" "$API/v1/sandboxes/$SB_ID"
```

Expected HTTP 404. Do not source `.env.second-client`.

## H. Cleanup — both backends

Download any files you need before deleting the sandbox.

```bash
client_api -X DELETE "$API/v1/sandboxes/$SB_ID"
```

Docker verification — no matching container should remain:

```bash
docker ps -a --filter "label=sandbox.id=$SB_ID"
```

Kubernetes verification — deletion can take a little time:

```bash
kubectl -n sandboxes get pods -l "sandbox-id=$SB_ID"
```

Delete all Docker test sandboxes before changing backend. The database is shared; changing backend while old sandboxes remain can send lifecycle operations to the wrong runtime.

## I. Switch to Kubernetes — Terminal 1

This section keeps Go running on your laptop and PostgreSQL in Compose, but creates sandbox pods in your selected Kubernetes cluster. Do not also deploy k8s/base/control-plane.yaml for this mode: that would start a second Go controller.

Prerequisites: a dedicated development cluster with a network-policy-capable CNI, reachable Kubernetes API, and a kubeconfig authorized to manage pods/exec, secrets, PVCs and network policies in `sandboxes`. Kubernetes installation is specific to your chosen distribution; stop if the following commands fail. Do not select a production cluster for these tests.

Stop the Go server with Ctrl+C. Check your cluster:

```bash
kubectl config current-context
```

```bash
kubectl cluster-info
```

```bash
kubectl get nodes
```

Nodes should be Ready.

```bash
kubectl create namespace sandboxes --dry-run=client -o yaml | kubectl apply -f -
```

Check your kubeconfig permissions:

```bash
kubectl auth can-i create pods -n sandboxes
```

```bash
kubectl auth can-i create pods/exec -n sandboxes
```

```bash
kubectl auth can-i create secrets -n sandboxes
```

```bash
kubectl auth can-i create networkpolicies.networking.k8s.io -n sandboxes
```

Each should say yes. A RoleBinding for the in-cluster orchestrator service account does not automatically grant permissions to your laptop's kubeconfig user.

Edit `.env`:

```dotenv
SANDBOX_BACKEND=kubernetes
KUBECONFIG=/home/adedaryorh/.kube/config
SANDBOX_NAMESPACE=sandboxes
LOCAL_REGION=default
ENABLE_CHECKPOINTS=false
WARM_PYTHON=0
WARM_NODE=0
WARM_UBUNTU=0
```

Use your actual kubeconfig path. Keep DATABASE_URL pointing to local PostgreSQL on 5433.

```bash
set -a
source .env
set +a
go run ./cmd/server
```

In Terminal 2, repeat B through H. Start with Python, standard isolation, and no persistent disk. Docker pulls from section A do not populate remote Kubernetes worker image caches; Kubernetes pulls the images itself.

If a sandbox fails to start:

```bash
kubectl -n sandboxes get pods
```

```bash
kubectl -n sandboxes get events --sort-by=.metadata.creationTimestamp
```

```bash
kubectl -n sandboxes describe pod ACTUAL_POD_NAME
```

Inspect baseline network policy:

```bash
kubectl -n sandboxes get networkpolicy sandbox-baseline -o yaml
```

Policy existence alone does not prove the CNI enforces isolation. The baseline admits sandbox ingress from an in-cluster orchestrator-labelled pod; a Go process on your laptop is not such a pod. Therefore preview proxy connectivity needs a separate in-cluster deployment/routing test. Core exec/file tests use the Kubernetes API and are the first target here.

## J. Optional Kubernetes prerequisites (not proof of functionality)

Metrics:

```bash
kubectl top pods -n sandboxes
```

Secure isolation runtime:

```bash
kubectl get runtimeclass
```

Storage and snapshots:

```bash
kubectl get storageclass
```

```bash
kubectl get volumesnapshotclass
```

GPU availability:

```bash
kubectl get nodes -o custom-columns='NAME:.metadata.name,NVIDIA:.status.allocatable.nvidia\.com/gpu,AMD:.status.allocatable.amd\.com/gpu'
```

A CUDA image alone does not allocate a GPU. CSI snapshot controllers/drivers, GPU plugins, and gVisor must actually be installed before testing those features. Keep CRIU checkpoint recovery disabled until a dedicated recovery qualification is performed.

## K. Go and PostgreSQL integration checks

Run from the project root:

```bash
go test -race ./... -count=1
```

```bash
go vet ./...
```

```bash
go build ./cmd/... ./cli/...
```

Create a separate test database once:

```bash
docker compose exec postgres createdb -U sandboxing sandboxing_test
```

Add TEST_DATABASE_URL to `.env` with the correct password, port 5433, and database sandboxing_test. Reload the file, then:

```bash
go test -v ./internal/api -run TestPostgresTenantIsolationAndTaskPersistence -count=1
```

Expected PASS, not SKIP. This uses a real database and a simulated runtime; the manual Docker/Kubernetes tests above exercise real workloads.

## L. Stop services

After cleaning up test sandboxes, press Ctrl+C in Terminal 1, then:

```bash
docker compose stop
```

This preserves database volumes. Never use `down -v` as a normal restart.

## Coverage limits

This guide covers the commands discussed for all five templates, sandbox lifecycle create/read/delete, execution, files, metrics, quota, audit, MCP discovery and a tenant ownership check, with Docker and Kubernetes startup paths. It does not claim complete coverage of tasks, cron, webhooks, previews, REPL/PTY, pause/resume data semantics, CSI restore, GPU execution or CRIU recovery. The optional prerequisite commands only inspect readiness; they do not certify those features.
