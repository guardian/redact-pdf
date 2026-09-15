# Copilot Coding Agent Instructions for guardian/redact-pdf

Trust these instructions and only search the codebase if information here is incomplete or found to be in error.

## Project Overview

This is a **Play Framework web application** (Scala 3) that redacts names, pronouns, email addresses, and URLs from PDF CVs for name-blind recruitment. It uses Apache PDFBox for PDF manipulation. The project is small (~2300 KB) and deployed to AWS via `riff-raff` continuous deployment.

## Tech Stack & Versions

- **Language:** Scala 3.3.7
- **Framework:** Play Framework 3.0.10
- **Build tool:** sbt 1.12.3
- **Java:** Corretto 21 (required)
- **CDK infrastructure:** TypeScript with Bun 1.1.42 (version pinned in `cdk/.bun-version`)
- **Node (reference):** v20.18.1 (`.nvmrc`)
- **Key library:** Apache PDFBox 3.0.6

## Repository Layout

```
build.sbt                    # Main sbt build definition (project name: cv-redact-tool)
project/
  build.properties           # sbt version (1.12.3)
  plugins.sbt                # Play plugin, sbt-native-packager
app/
  controllers/
    Application.scala        # Main controller: index, upload, importFromTaleo, healthcheck
  redact/
    PdfRedactor.scala         # Core redaction logic (object PdfRedactor)
    TextFinder.scala          # Text search in PDFs (string, regex, email, URL matching)
    ImageRedactor.scala       # Replaces images with placeholders
    Image.scala               # Generates placeholder images
    CustomSplitter.scala      # Custom PDF page splitter for multi-candidate PDFs
  views/
    index.scala.html          # Main page template (Twirl)
    main.scala.html           # Layout template (Twirl)
  wiring/
    AppLoader.scala           # Custom Play ApplicationLoader
    AppComponents.scala       # Dependency wiring (compile-time DI, not Guice)
conf/
  application.conf            # App config (feature flags, secret key, CSP)
  redactedwords.conf          # Gendered words and pet names lists
  routes                      # Play routes file
  logback.xml                 # Logging configuration
  messages                    # i18n (empty)
public/                       # Static assets (images, javascripts, stylesheets)
scripts/
  build.sh                    # CI build script (runs sbt + CDK build)
startapp.sh                   # Local run script
cdk/                          # AWS CDK infrastructure (TypeScript/Bun)
  bin/cdk.ts                  # CDK app entry point
  lib/cv-redact-tool.ts       # Stack definition (GuEc2App)
  lib/cv-redact-tool.test.ts  # CDK snapshot test
  package.json                # CDK dependencies and scripts
  bun.lock                    # Bun lockfile
  bunfig.toml                 # Bun test config
  tsconfig.json               # TypeScript config
  .bun-version                # Pinned Bun version (1.1.42)
.github/workflows/
  build.yml                   # CI: build + deploy (runs on PRs and pushes to main)
  check-labels.yaml           # PR label enforcement
  sbt-dependency-graph.yml    # Dependency graph submission (main only)
```

## Build & Validate Commands

### Prerequisites
- Always ensure **Java 21** (Corretto) is available before running sbt commands.
- Always ensure **sbt** is installed/available.
- For CDK work, ensure **Bun** is installed (version from `cdk/.bun-version`: 1.1.42).

### Compile the Scala application
```bash
sbt compile
```

### Run tests (Scala — note: there are currently no Scala tests in the repo)
```bash
sbt test
```

### Build the Debian package (full CI build for the Scala app)
```bash
sbt clean compile test debian:packageBin normalisePackageName
```
This produces `target/cv-redact-tool.deb`.

### Run the application locally
```bash
./startapp.sh
```
This runs `sbt -J-Xmx2g -J-Xms2g -v run`. The app needs 2 GB heap due to PDF processing. The app listens on port **9000** by default.

### CDK (infrastructure) commands — always run from the `cdk/` directory
```bash
cd cdk
bun install --frozen-lockfile   # Install dependencies (always run first)
bun lint                         # ESLint on lib/** and bin/**
bun test                         # Snapshot tests (bun:test)
bun synth                        # Synthesize CloudFormation templates
```

### Full CI build (what GitHub Actions runs)
```bash
./scripts/build.sh
```
This script runs:
1. `sbt clean compile test debian:packageBin normalisePackageName`
2. `cd cdk && bun install --frozen-lockfile && bun lint && bun test && bun synth`

**Important:** The `set -e` in `scripts/build.sh` is placed AFTER the sbt command, meaning sbt failures may not abort the script. However, the CDK section will fail-fast.

## CI Checks on Pull Requests

The following GitHub Actions workflows run on PRs:
1. **build** (`.github/workflows/build.yml`): Runs `./scripts/build.sh` then uploads to Riff-Raff. The sbt build and CDK lint/test/synth must all pass.
2. **CORE4 label enforcement** (`.github/workflows/check-labels.yaml`): Requires specific labels on PRs.

Always verify your changes pass: `sbt compile` for Scala changes, and `cd cdk && bun install --frozen-lockfile && bun lint && bun test && bun synth` for CDK changes.

## Architecture Notes

- **Compile-time DI:** The app uses Play's compile-time dependency injection (NOT Guice). The application loader is `wiring.AppLoader` (configured in `conf/application.conf` as `play.application.loader`). Components are wired in `wiring.AppComponents`.
- **Routes:** Defined in `conf/routes`. There is one controller (`controllers.Application`) with endpoints: `GET /` (index), `POST /` (upload single PDF), `POST /importFromTaleo` (batch), `GET /healthcheck`.
- **Feature flags:** Configured in `conf/application.conf`: `redacted-exact-strings.enabled`, `greedy-name-match.enabled`, `new-page-split-behaviour.enabled`.
- **Word lists:** `conf/redactedwords.conf` contains gendered words and pet names to redact.
- **Templates:** Twirl templates in `app/views/` (`.scala.html` files).
- **Packaging:** sbt-native-packager builds a `.deb` package. The `normalisePackageName` custom sbt task renames the deb file.
- **CDK snapshot tests:** If you change `cdk/lib/cv-redact-tool.ts`, update snapshots with `cd cdk && bun test --update-snapshots`.

## Known Quirks

- In `project/plugins.sbt`, there is a `libraryDependencySchemes` override for `scala-xml` to resolve a version conflict between sbt and sbt-native-packager. Do not remove this.
- The app requires `-J-Xmx2g -J-Xms2g` JVM flags for local runs due to memory-intensive PDF rendering (rasterisation at 300 DPI).
- There are no Scala unit tests in the repository (`sbt test` will succeed with 0 tests). The only automated tests are CDK snapshot tests in `cdk/lib/cv-redact-tool.test.ts`.