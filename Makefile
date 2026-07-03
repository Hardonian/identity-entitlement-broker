.PHONY: setup dev test lint build smoke clean help
.PHONY: install-backend install-frontend init-keycloak
.PHONY: start-db start-opa start-keycloak start-api start-web
.PHONY: test-backend test-frontend test-opa
.PHONY: lint-backend lint-frontend
.PHONY: build-backend build-frontend
.PHONY: run-smoke
.PHONY: clean-backend clean-frontend clean-containers

# ============================================================================
# Identity Entitlement Broker - Makefile
# ============================================================================

# Default: show help
.DEFAULT_GOAL := help

## --------------------------------------------------------------------------
## Setup
## --------------------------------------------------------------------------

setup: install-backend install-frontend init-keycloak
	@echo "=== Setup complete ==="

install-backend:
	cd apps/api && mvn dependency:resolve

install-frontend:
	cd apps/web && npm install

init-keycloak:
	@echo "See docs/runbooks/configure-oidc-provider.md for Keycloak setup instructions"
	@echo "Quick start: docker compose up -d keycloak && open http://localhost:8080"

## --------------------------------------------------------------------------
## Development
## --------------------------------------------------------------------------

dev: start-db start-opa start-keycloak start-api start-web
	@echo "=== Development stack started ==="
	@echo "  API:      http://localhost:8081"
	@echo "  Web:      http://localhost:5173"
	@echo "  Keycloak: http://localhost:8080"
	@echo "  OPA:      http://localhost:8181"
	@echo ""
	@echo "Run 'make smoke' to verify the stack."

start-db:
	docker compose up -d mariadb

start-opa:
	docker compose up -d opa

start-keycloak:
	docker compose up -d keycloak

start-api:
	cd apps/api && mvn quarkus:dev &

start-web:
	cd apps/web && npm run dev &

## --------------------------------------------------------------------------
## Testing
## --------------------------------------------------------------------------

test: test-backend test-frontend test-opa
	@echo "=== All tests passed ==="

test-backend:
	cd apps/api && mvn test

test-frontend:
	cd apps/web && npm run build -- --mode test

test-opa:
	opa test policies/opa -v

## --------------------------------------------------------------------------
## Linting
## --------------------------------------------------------------------------

lint: lint-backend lint-frontend
	@echo "=== Lint complete ==="

lint-backend:
	cd apps/api && mvn checkstyle:check || true

lint-frontend:
	cd apps/web && npm run lint

## --------------------------------------------------------------------------
## Build
## --------------------------------------------------------------------------

build: build-backend build-frontend
	@echo "=== Build complete ==="

build-backend:
	cd apps/api && mvn package -DskipTests

build-frontend:
	cd apps/web && npm run build

## --------------------------------------------------------------------------
## Smoke Tests
## --------------------------------------------------------------------------

smoke: run-smoke

run-smoke:
	cd tests/smoke && bash smoke-test.sh

## --------------------------------------------------------------------------
## Cleanup
## --------------------------------------------------------------------------

clean: clean-backend clean-frontend clean-containers
	@echo "=== Clean complete ==="

clean-containers:
	docker compose down -v

clean-backend:
	cd apps/api && mvn clean

clean-frontend:
	cd apps/web && rm -rf node_modules dist

## --------------------------------------------------------------------------
## Help
## --------------------------------------------------------------------------

help:
	@echo "Identity Entitlement Broker - Makefile"
	@echo ""
	@echo "Usage: make <target>"
	@echo ""
	@echo "Setup:"
	@echo "  setup             Install all dependencies and init Keycloak"
	@echo "  install-backend   Resolve Maven dependencies"
	@echo "  install-frontend  Install npm dependencies"
	@echo "  init-keycloak     Print instructions for Keycloak setup"
	@echo ""
	@echo "Development:"
	@echo "  dev               Start full development stack"
	@echo "  start-db          Start MariaDB only"
	@echo "  start-opa         Start OPA only"
	@echo "  start-keycloak    Start Keycloak only"
	@echo "  start-api         Start Quarkus dev server (background)"
	@echo "  start-web         Start frontend dev server (background)"
	@echo ""
	@echo "Testing:"
	@echo "  test              Run all tests"
	@echo "  test-backend      Run Java backend tests"
	@echo "  test-frontend     Run frontend build check"
	@echo "  test-opa          Run OPA policy tests"
	@echo ""
	@echo "Linting:"
	@echo "  lint              Run all linters"
	@echo "  lint-backend      Run Checkstyle on Java code"
	@echo "  lint-frontend     Run frontend linter"
	@echo ""
	@echo "Build:"
	@echo "  build             Build all artifacts"
	@echo "  build-backend     Package Java backend (skip tests)"
	@echo "  build-frontend    Build frontend bundle"
	@echo ""
	@echo "Smoke:"
	@echo "  smoke             Run smoke tests against running stack"
	@echo ""
	@echo "Clean:"
	@echo "  clean             Clean everything"
	@echo "  clean-containers  Stop and remove Docker containers"
	@echo "  clean-backend     Clean Maven build artifacts"
	@echo "  clean-frontend    Remove node_modules and dist"
