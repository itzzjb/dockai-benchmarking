# DockAI Benchmarking Suite

> **Comparative Empirical Analysis of AI-Generated Infrastructure-as-Code vs. Cloud Native Buildpacks**

[![GitHub Actions](https://img.shields.io/badge/CI-GitHub%20Actions-2088FF?logo=github-actions&logoColor=white)](#10-workflow-execution-pipeline)
[![License](https://img.shields.io/badge/License-Research-blue)](#)
[![Metric Version](https://img.shields.io/badge/Metric%20Version-1.0-green)](#2-the-governing-equation)

---

## Table of Contents

1. [Overview](#1-overview)
2. [The Governing Equation](#2-the-governing-equation)
3. [Component Analysis](#3-component-analysis)
4. [Edge Case Handling: Survivorship Bias](#4-edge-case-handling-survivorship-bias)
5. [Worked Example Calculation](#5-worked-example-calculation)
6. [Benchmark Participants](#6-benchmark-participants)
7. [DockAI Setup](#7-dockai-setup)
8. [IDE Agent Setup](#8-ide-agent-setup)
9. [CNB (Cloud Native Buildpacks) Baseline](#9-cnb-cloud-native-buildpacks-baseline)
10. [Workflow Execution Pipeline](#10-workflow-execution-pipeline)
11. [Running the Benchmark](#11-running-the-benchmark)
12. [Google Sheets Integration](#12-google-sheets-integration)
13. [Artifact Structure](#13-artifact-structure)
14. [Infrastructure & Tooling Versions](#14-infrastructure--tooling-versions)
15. [Repository Structure](#15-repository-structure)
16. [Troubleshooting](#16-troubleshooting)
17. [Contributing](#17-contributing)
18. [FAQ](#18-faq)

---

## 1. Overview

### 1.1 What Is This?

This repository contains an **automated benchmarking suite** that rigorously evaluates the quality of AI-generated Dockerfiles against human-written Dockerfiles and the industry-standard Cloud Native Buildpacks (CNB). It runs entirely within GitHub Actions — push a change to `config.json` or trigger manually, and the pipeline will:

1. **Clone** any target repository containing a Dockerfile.
2. **Generate** two alternative Dockerfiles using AI (DockAI multi-agent pipeline and an IDE Agent simulation).
3. **Build** all four container images (Human, DockAI, IDE Agent, CNB).
4. **Measure** image size, build time, and security vulnerabilities (via Trivy).
5. **Lint** all Dockerfiles with Hadolint.
6. **Score** each method using the Composite Optimization Metric ($C_{final}$).
7. **Report** results via GitHub Step Summary, a downloadable academic report, and optionally a Google Sheet.

### 1.2 Why This Exists

Single-dimensional metrics (e.g., measuring only image size) fail to capture the holistic cost of software artifacts. This research proposes a **multi-dimensional cost function** — the **Composite Optimization Metric ($C_{final}$)** — that quantifies the trade-off between:

- **Storage efficiency** (image size)
- **Build latency** (build time)
- **Security posture** (CVE severity-weighted index)
- **Syntactic quality** (Hadolint lint results)

All values are normalized against the CNB baseline, ensuring the resulting score is a **dimensionless ratio** representing relative improvement or regression.

### 1.3 Quick Start

```bash
# 1. Fork or clone this repository
git clone https://github.com/<your-org>/dockai-benchmarking.git
cd dockai-benchmarking

# 2. Configure the target repository in config.json
cat config.json
# {
#   "repository_url": "axllent/mailpit",
#   "subdirectory": "",
#   "weights": { "size": 0.3, "time": 0.2, "security": 0.5 }
# }

# 3. Set required secrets in your GitHub repository settings:
#    - OPENAI_API_KEY         (required for DockAI + IDE Agent)
#    - LANGCHAIN_API_KEY      (optional, for DockAI tracing)
#    - GDRIVE_SHEET_ID        (optional, for Google Sheets output)
#    - GDRIVE_SERVICE_ACCOUNT_KEY (optional, for Google Sheets output)

# 4. Push a change to config.json on main, or trigger manually via Actions UI
git add config.json && git commit -m "benchmark: target axllent/mailpit" && git push
```

The workflow will run automatically and results will appear in the **Actions** tab → **Job Summary**.

---

## 2. The Governing Equation

The final score for any given build method $M$ (where $M \in \{Human, DockAI, IDE Agent, CNB\}$) is defined as a weighted performance score scaled by a static quality penalty factor:

$$C_{final}(M) = \underbrace{\left[ \sum_{i} W_i \cdot \hat{X}_i \right]}_{\text{Performance Score}} \times \underbrace{\left(1 + P_{quality}\right)}_{\text{Scaled Static Penalty}}$$

**Symbols:**
- $W_i$ are non-negative weights that sum to 1
- $\hat{X}_i$ are the normalized, dimensionless metrics (size, time, security) for method $M$
- $P_{quality}$ is the non-negative lint penalty derived from Hadolint findings

**Why multiplicative?** The performance term is dimensionless and non-negative. Multiplying by the factor $(1 + P_{quality})$ applies a non-negative scalar: when $P_{quality}=0$ the score is unchanged; $P_{quality}=0.05$ inflates the score by about 5%; larger lint debt scales proportionally. This preserves ordering induced by the performance term and avoids the additive case where a tiny performance score could be overwhelmed by a fixed offset. Formally, for any two methods A and B with the same quality penalty, if the weighted performance score of A is less than B, then the final score of A will be less than B.

**Interpretation:**
* **Lower is Better.**
* $C_{final} < 1.0$: The method is **superior** to the industry baseline.
* $C_{final} = 1.0$: The method is **equivalent** to the industry baseline (CNB).
* $C_{final} > 1.0$: The method is **inferior** to the industry baseline.

**Mathematical rationale (short):**
* **Dimensionless core:** Each $\hat{X}_i$ is a ratio, so $\sum_i W_i \hat{X}_i$ is dimensionless and comparable across metrics.
* **Convex weighting:** $\sum_i W_i = 1$ (0.3 + 0.2 + 0.5), so the performance score is a convex combination; it remains between the min and max of the normalized inputs.
* **Multiplicative penalty:** $(1 + P_{quality})$ is a non-negative scalar. When lint is clean, it equals 1; when lint exists, it proportionally inflates the base score. This preserves ordering induced by the performance term and avoids an additive penalty overwhelming very small scores.
* **Non-negativity and ordering:** All components are non-negative, so $C_{final} \ge 0$. If every metric strictly improves relative to baseline and lint is clean, $C_{final} < 1$.

---

## 3. Component Analysis

### 3.1 Baseline Normalization ($\hat{X}$)

Directly comparing "Megabytes" (Size), "Seconds" (Time), and "Integer Counts" (Vulnerabilities) is mathematically invalid due to unit mismatch. We standardize inputs via **Baseline Normalization**.

For every metric $X$, the normalized value $\hat{X}$ is calculated as:

$$\hat{X}_{model} = \frac{X_{model}}{\max\left(X_{baseline}, \epsilon \cdot \max(1, |X_{baseline}|)\right)}$$

* **$X_{baseline}$**: The value obtained from the Cloud Native Buildpack (CNB).
* **$\epsilon$**: A small relative constant (default $10^{-6}$) to prevent division-by-zero without distorting ratios when the baseline is small. The relative form keeps scaling proportional for near-zero baselines while still guarding against zero.
* **Bounds:** If $X_{model} = X_{baseline}$, then $\hat{X}=1$. If $X_{model}<X_{baseline}$, $\hat{X}<1$; if $X_{model}>X_{baseline}$, $\hat{X}>1$. When $X_{baseline}=0$, the denominator becomes $\epsilon$, so $\hat{X}$ reflects how large the model value is relative to the small guard, avoiding infinite ratios.

This transformation converts all raw data into **dimensionless ratios**. For example, if $\hat{S} = 0.6$, the model's image is 60% the size of the baseline (indicating a 40% improvement).

### 3.2 Weighted Priorities ($W_i$)

The weights ($W$) determine the relative importance of each metric. These are derived from modern DevSecOps priorities, where security is a strictly dominating factor.

| Metric Symbol | Description | Weight ($W$) | Justification |
| :--- | :--- | :--- | :--- |
| $\hat{\Omega}$ | **Security Index** | **0.50** | In production environments, Critical CVEs are blockers. Security is weighted highest to penalize vulnerable images severely. |
| $\hat{S}$ | **Image Size** | **0.30** | Smaller images reduce container registry storage costs and network transfer time (bandwidth), a key efficiency metric for cloud scaling. |
| $\hat{T}$ | **Build Time** | **0.20** | CI/CD latency is important for developer feedback loops but is secondary to runtime security and operational efficiency. |

### 3.3 The Security Vulnerability Index ($\Omega$)

Raw vulnerability counts are insufficient; a single "Critical" CVE poses significantly more risk than 50 "Low" CVEs. We utilize a weighted sum based on the **Trivy** severity classification:

$$\Omega = (10 \cdot N_{critical}) + (5 \cdot N_{high}) + (2 \cdot N_{medium}) + (1 \cdot N_{low})$$

Where $N$ is the count of vulnerabilities at that severity level.

**Rationale:** The coefficients form an ordinal, monotone mapping of severity into a single scalar so that reducing a critical finding always improves $\Omega$ more than reducing any number of lows. Any positive counts keep $\Omega \ge 0$; zero findings yield $\Omega = 0$.

### 3.4 The Static Analysis Penalty ($P_{quality}$)

Optimization cannot come at the cost of code quality. We utilize **Hadolint** (Haskell Dockerfile Linter) to enforce best practices (e.g., version pinning, shell safety).

$$P_{quality} = (0.1 \cdot N_{error}) + (0.05 \cdot N_{warning})$$

* **Errors** (e.g., invalid syntax) incur a heavy penalty (+0.10 to the final score).
* **Warnings** (e.g., style suggestions) incur a moderate penalty (+0.05).
* The multiplicative form keeps lint penalties proportional to the underlying performance score — penalties cannot swamp a near-zero performance score but still scale linearly with lint findings because $(1+P_{quality})$ acts as a scalar on the weighted sum.
* *Note:* Cloud Native Buildpacks do not produce a Dockerfile to lint; therefore, their $P_{quality}$ is defined as 0.

---

## 4. Edge Case Handling: Survivorship Bias

To ensure scientific rigor, we address the "failed build" scenario. If a generated Dockerfile fails to build successfully, simply assigning it a score of 0 or infinity would distort the statistical mean.

**Protocol:**
If `build_status != success`:

$$C_{final} = 9999$$

This "Sentinel Value" ensures that failed experiments are clearly categorized as inferior to any functional build, preventing the AI from "winning" by generating empty or non-functional code.

**Baseline fallback:** If the CNB baseline build fails, it still receives $C_{final}=9999$; for normalization of other methods, each metric independently switches to the **smallest value among all successful builds** (Human, DockAI, or IDE Agent) to keep ratios finite and comparable. For example, if Human has the smallest image but IDE Agent has the fastest build time, the size baseline uses Human's value and the time baseline uses IDE Agent's value.

**Implementation note:** In the workflow, zero or negative baseline values are floored to 1 (rather than using the $\epsilon$-guard described in §3.1) for simplicity. The mathematical effect is equivalent for practical inputs: it prevents division-by-zero without distorting meaningful ratios.

---

## 5. Worked Example Calculation

Let us calculate the score for a hypothetical **DockAI** run compared to a **CNB Baseline**.

**1. Raw Data Input**
* **CNB (Baseline):** Size=200MB, Time=40s, Security Index=500.
* **DockAI (Model):** Size=100MB, Time=10s, Security Index=50.
* **DockAI Linting:** 0 Errors, 2 Warnings.

**2. Normalization Step**
* $\hat{S}$ (Size Ratio) = $100 / 200 = 0.5$
* $\hat{T}$ (Time Ratio) = $10 / 40 = 0.25$
* $\hat{\Omega}$ (Security Ratio) = $50 / 500 = 0.1$

**3. Weighted Sum Calculation ($C_{total}$)**

$$C_{total} = (0.3 \cdot 0.5) + (0.2 \cdot 0.25) + (0.5 \cdot 0.1)$$

$$C_{total} = 0.15 + 0.05 + 0.05 = 0.25$$

**4. Penalty Application ($P_{quality}$)**

$$P_{quality} = (0.1 \cdot 0) + (0.05 \cdot 2) = 0.10$$

**5. Final Score ($C_{final}$)**

$$C_{final} = 0.25 \times (1 + 0.10) = 0.275$$

**Conclusion:** The DockAI method scored **0.275**. Since $0.275 < 1.0$, the AI model is significantly optimized compared to the industry standard, offering a **72.5% reduction** in the composite cost.

---

## 6. Benchmark Participants

The workflow evaluates **four** Dockerfile generation methods:

| Method | Label | Description |
| :--- | :--- | :--- |
| **Human** | `Dockerfile` | The original, hand-written Dockerfile already present in the target repository. |
| **DockAI** | `Dockerfile.dockai` | Generated by the DockAI multi-agent pipeline (Analyzer → Blueprint → Generator → Reviewer → Reflector loop). Uses `itzzjb/dockai@v4`. |
| **IDE Agent** | `Dockerfile.ide` | Generated by a simulated IDE-embedded AI agent (see [§8](#8-ide-agent-setup)). |
| **CNB (Baseline)** | Cloud Native Buildpacks | Built with `pack build` (v0.36.2) using `paketobuildpacks/builder:base`. Serves as the normalization baseline ($C_{final} = 1.0$ when lint-clean). |

---

## 7. DockAI Setup

### 7.1 What Is DockAI?

**DockAI** is a purpose-built multi-agent LLM pipeline for generating production-optimized Dockerfiles. It is consumed as a GitHub Action (`itzzjb/dockai@v4`) and orchestrates **6+ specialized agents**, each responsible for a distinct phase of Dockerfile creation.

### 7.2 Pipeline Architecture

```
Analyzer → Blueprint → Generator → Reviewer ⇄ Reflector (loop) → Final Dockerfile
                                       ↑                    ↓
                                  Error Analyzer ← Build Failure
```

| Agent | Role |
| :--- | :--- |
| **Analyzer** | Inspects the project structure, language, framework, and dependencies. |
| **Blueprint** | Produces a high-level build plan (base image, stages, layer order). |
| **Generator** | Writes the initial Dockerfile from the blueprint. |
| **Reviewer** | Evaluates the generated Dockerfile for correctness and best practices. |
| **Reflector** | Incorporates reviewer feedback and revises the Dockerfile. |
| **Error Analyzer** | Diagnoses build failures and feeds fixes back into the loop. |
| **Iterative Improver** | Applies targeted fixes during the retry cycle. |

The Reviewer → Reflector feedback loop can run up to **10 retries** (`max_retries: 10`), including build-test-fix iterations where a failed `docker build` triggers the Error Analyzer.

### 7.3 Action Inputs

The workflow invokes DockAI with the following configuration:

```yaml
- name: Generate DockAI Dockerfile
  uses: itzzjb/dockai@v4
  continue-on-error: true
  with:
    openai_api_key: ${{ secrets.OPENAI_API_KEY }}
    project_path: <working-directory>
    max_retries: 10
    langchain_tracing_v2: true
    langchain_api_key: ${{ secrets.LANGCHAIN_API_KEY }}
    langchain_project: dockai
    llm_provider: openai
    model_analyzer: gpt-5-mini
    model_blueprint: gpt-5-mini
    model_generator: gpt-5-mini
    model_generator_iterative: gpt-5-mini
    model_reviewer: gpt-5-mini
    model_reflector: gpt-5-mini
    model_error_analyzer: gpt-5-mini
    model_iterative_improver: gpt-5-mini
```

| Parameter | Value | Notes |
| :--- | :--- | :--- |
| All 8 model slots | `gpt-5-mini` | Every agent uses the same model for consistency. |
| `max_retries` | `10` | Maximum Reviewer ⇄ Reflector + build-fix iterations. |
| `llm_provider` | `openai` | OpenAI Chat Completions API. |
| `langchain_tracing_v2` | `true` | Enables LangSmith observability for all agent calls. |
| `langchain_project` | `dockai` | LangSmith project name for trace grouping. |
| `continue-on-error` | `true` | DockAI failures are non-fatal; the benchmark proceeds with whatever methods succeeded. |

### 7.4 Dockerfile Handling

DockAI writes its output to `Dockerfile` in-place (overwriting the human file). The workflow handles this via a **backup/restore dance**:

1. **Before generation:** The human `Dockerfile` is moved to `../Dockerfile.human.bak` and any stale `Dockerfile.*` files are removed.
2. **Restore for DockAI:** The human backup is copied back to `Dockerfile` so DockAI sees the original project state.
3. **After DockAI:** The generated output is copied to `Dockerfile.dockai`, and the human backup is restored to `Dockerfile`. The backup file is then deleted.

This ensures all three Dockerfiles coexist: `Dockerfile` (human), `Dockerfile.dockai` (DockAI), `Dockerfile.ide` (IDE Agent).

---

## 8. IDE Agent Setup

### 8.1 What Is the IDE Agent?

The **IDE Agent** simulates the workflow of an AI coding assistant embedded in an IDE — tools like **Gemini CLI**, **Claude Code**, or **GitHub Copilot Agent Mode** — where the model can explore files, read source code, and iteratively build context before generating an artifact.

Unlike DockAI (which uses a structured multi-agent pipeline with dedicated agents for each phase), the IDE Agent follows a **single-model, multi-turn conversation** pattern that mirrors how a developer would interact with an AI assistant in their editor.

### 8.2 How It Works

The IDE Agent generation runs inside the GitHub Actions workflow (`continue-on-error: true` — failures are non-fatal) and proceeds through four phases:

#### Phase 1 — Project Exploration

The agent scans the project directory (up to depth 3), excluding noise directories (`node_modules`, `vendor`, `__pycache__`, `dist`, `build`, dotfiles). This produces a file listing (capped at 50 entries) analogous to a developer opening a project in their IDE.

#### Phase 2 — File Selection (LLM Call #1)

The file listing is sent to `gpt-4o` with a system prompt establishing the agent persona. The model is asked to identify the 3–5 most important files to read in order to create a Dockerfile. This mirrors an IDE agent's "tool use" step — deciding which files to inspect.

#### Phase 3 — Context Gathering

The agent reads well-known dependency/config files that exist in the project:

`package.json`, `requirements.txt`, `go.mod`, `pom.xml`, `Cargo.toml`, `Gemfile`, `composer.json`, `setup.py`, `pyproject.toml`

File contents are truncated to 100 lines and capped at 10 KB to stay within token budgets.

#### Phase 4 — Dockerfile Generation (LLM Call #2)

A second call to `gpt-4o` receives the full conversation history (system prompt → file list → model's file picks → gathered file contents) and is instructed to produce a **production-ready Dockerfile** following best practices:

- Appropriate base image for the detected language/framework
- Multi-stage build where beneficial
- Optimized layer caching (dependencies before application code)
- Non-root user for security
- Minimal final image size
- Proper `CMD`/`ENTRYPOINT`

The raw output is cleaned (markdown fences stripped), validated to contain a `FROM` instruction, and saved as `Dockerfile.ide`. If generation fails, a minimal `alpine:latest` fallback is written.

### 8.3 Model & Parameters

| Parameter | Value |
| :--- | :--- |
| Model | `gpt-4o` |
| Temperature | `0.3` |
| Max tokens | `2000` |
| API | OpenAI Chat Completions |
| Total LLM calls | **2** (file selection + generation) |

### 8.4 IDE Agent vs. DockAI — Key Differences

| Dimension | IDE Agent | DockAI |
| :--- | :--- | :--- |
| Architecture | Single model, multi-turn chat | Multi-agent pipeline (8 specialized model slots) |
| Model | `gpt-4o` | `gpt-5-mini` (all agents) |
| Iteration | No self-review loop | Built-in Reviewer → Reflector feedback loop with up to 10 retries |
| Context strategy | Reads known config files heuristically | Dedicated Analyzer agent inspects project structure |
| Build validation | None (generates and saves) | Iterative build-test-fix cycle with Error Analyzer |
| LLM calls | 2 | 6+ (varies with retry count) |
| Observability | None | LangSmith tracing via LangChain |
| Purpose in benchmark | Represents the **"quick IDE assist"** baseline | Represents a **purpose-built optimization pipeline** |

---

## 9. CNB (Cloud Native Buildpacks) Baseline

### 9.1 What Are Cloud Native Buildpacks?

[Cloud Native Buildpacks](https://buildpacks.io/) (CNB) automatically detect a project's language/framework and produce an OCI-compliant container image **without a Dockerfile**. They represent the industry-standard "zero-config" baseline.

### 9.2 Build Configuration

| Parameter | Value |
| :--- | :--- |
| CLI | `pack` v0.36.2 (installed via `buildpacks/github-actions/setup-pack@v5.9.7`) |
| Builder | `paketobuildpacks/builder:base` |
| Flags | `--pull-policy if-not-present`, `--clear-cache` |
| Path | Points to the working directory (respects `subdirectory` in config) |

### 9.3 Role in Scoring

- CNB serves as the **normalization denominator** for all metrics: $\hat{X} = X_{method} / X_{CNB}$.
- Because no Dockerfile is produced, Hadolint cannot lint it. The workflow stubs the lint output as `[]`, giving CNB $P_{quality} = 0$.
- The performance term is hard-coded to `1.0000` (by definition, the ratio of CNB to itself), so $C_{final} = 1.0$ for a lint-clean CNB build.
- If the CNB build fails, it receives $C_{final} = 9999$ and the normalization baseline falls back per-metric to the smallest successful value across Human, DockAI, and IDE Agent (see [§4](#4-edge-case-handling-survivorship-bias)).

---

## 10. Workflow Execution Pipeline

The benchmark runs as a single GitHub Actions job (`compare`) on `ubuntu-latest`. Below is the step-by-step execution order:

### 10.1 High-Level Flow Diagram

```
┌──────────────┐    ┌──────────────────┐    ┌──────────────────┐    ┌────────────────┐    ┌──────────────┐    ┌──────────────┐
│  1. SETUP    │───▶│  2. GENERATION   │───▶│  3. BUILD &      │───▶│  4. STATIC     │───▶│  5. SCORING  │───▶│  6. REPORT   │
│              │    │                  │    │     MEASURE      │    │     ANALYSIS   │    │              │    │   & CLEANUP  │
│ • Checkout   │    │ • Backup Human   │    │                  │    │                │    │ • Normalize  │    │              │
│ • Install    │    │   Dockerfile     │    │ • Human build    │    │ • Hadolint ×3  │    │ • Weight     │    │ • results.json│
│   deps       │    │ • IDE Agent gen  │    │ • DockAI build   │    │ • Trivy ×4     │    │ • Penalize   │    │ • report.md  │
│ • Clone repo │    │ • DockAI gen     │    │ • IDE build      │    │ • CNB stub     │    │ • Winner     │    │ • GH Summary │
│ • Trivy/Pack │    │ • Restore files  │    │ • CNB build      │    │                │    │              │    │ • Sheets     │
│   CLI        │    │                  │    │ (all --no-cache) │    │                │    │              │    │ • Artifacts  │
└──────────────┘    └──────────────────┘    └──────────────────┘    └────────────────┘    └──────────────┘    └──────────────┘
```

### 10.2 Setup Phase

| Step | What It Does |
| :--- | :--- |
| **Checkout workflow repo** | Clones this benchmarking repo (`actions/checkout@v6.0.1`). |
| **Install dependencies** | `apt-get install jq bc` for JSON processing and float math. |
| **Set up Python 3.11** | For the Google Sheets integration script (`actions/setup-python@v5`, pip cache keyed on `.github/requirements.txt`). |
| **Set up Trivy CLI** | Pins Trivy v0.68.1 (`aquasecurity/setup-trivy`, with cache). |
| **Set up pack CLI** | Pins pack v0.36.2 (`buildpacks/github-actions/setup-pack@v5.9.7`). |
| **Resolve repository** | Parses `config.json` → derives clone URL. Supports both full `https://` URLs and `owner/repo` shorthand (auto-prefixes `https://github.com/`). Inserts `GITHUB_TOKEN` auth into the clone URL, then resets the remote after cloning to avoid token leakage. Uses shallow clone (`--depth 1`). |
| **Set working directory** | If `subdirectory` is set in config, validates it exists in the cloned repo and uses it as the working directory. |

### 10.3 Generation Phase

| Step | What It Does |
| :--- | :--- |
| **Backup human Dockerfile** | Moves `Dockerfile` → `../Dockerfile.human.bak`; removes stale `Dockerfile.*` files. |
| **Generate IDE Agent Dockerfile** | Runs the 4-phase IDE Agent process ([§8](#8-ide-agent-setup)). `continue-on-error: true`. |
| **Restore original Dockerfile** | Copies backup back so DockAI sees the original project. |
| **Generate DockAI Dockerfile** | Invokes `itzzjb/dockai@v4` ([§7](#7-dockai-setup)). `continue-on-error: true`. Skipped if `skip_dockai` input is `"true"`. |
| **Organize Dockerfiles** | Copies DockAI output to `Dockerfile.dockai`, restores human `Dockerfile`, cleans up backup. |

### 10.4 Build & Measure Phase

All Docker builds use `--no-cache` for reproducible timing.

| Build | Command | Output Files |
| :--- | :--- | :--- |
| Human | `docker build --no-cache -f Dockerfile -t human:compare .` | `human_time.txt`, `human_status.txt` |
| DockAI | `docker build --no-cache -f Dockerfile.dockai -t dockai:compare .` | `dockai_time.txt`, `dockai_status.txt` |
| IDE Agent | `docker build --no-cache -f Dockerfile.ide -t ide:compare .` | `ide_time.txt`, `ide_status.txt` |
| CNB | `pack build cnb:compare --builder paketobuildpacks/builder:base ...` | `cnb_time.txt`, `cnb_status.txt` |

Each build is timed with `date +%s` (wall-clock seconds). Build failures are captured as `status=failed` (not fatal to the workflow).

### 10.5 Static Analysis Phase

| Tool | Targets | Configuration |
| :--- | :--- | :--- |
| **Hadolint** (`hadolint/hadolint-action@v3.1.0`) | `Dockerfile`, `Dockerfile.dockai`, `Dockerfile.ide` | `format: json`, `failure-threshold: error`, `no-fail: true` |
| **Hadolint stub for CNB** | None (no Dockerfile) | Writes `[]` to `hadolint-cnb.json` |
| **Trivy** (v0.68.1) | All 4 built images | `--format json`, `--severity CRITICAL,HIGH,MEDIUM,LOW`. Writes `{}` if image doesn't exist (build failed). |

### 10.6 Scoring Phase

1. **Read weights** from `config.json` (defaults: size=0.3, time=0.2, security=0.5).
2. **Extract raw metrics:** image size (`docker image inspect`), build time (from `*_time.txt`), security index $\Omega$ (from Trivy JSON), lint counts (from Hadolint JSON).
3. **Coerce empty/missing values:** empty strings default to 0; baseline values ≤ 0 are floored to 1 to prevent division-by-zero.
4. **Baseline selection:** If CNB failed, each metric independently falls back to the smallest successful value across Human, DockAI, and IDE Agent.
5. **Normalize & score:** Each method's metrics are divided by the baseline, then the weighted sum is computed.
6. **Apply lint penalty:** $C_{final} = C_{total} \times (1 + P_{quality})$.
7. **Disqualify failed builds:** Failed methods receive $C_{final} = 9999$.
8. **Determine winner:** The method with the lowest $C_{final}$ among successful builds wins. Float comparison uses `bc -l` with safe fallback.

### 10.7 Reporting Phase

| Output | Description |
| :--- | :--- |
| **`results.json`** | Structured JSON with status, size, time, omega, errors, warnings, penalty, and cost for all 4 methods plus winner. |
| **GitHub Step Summary** | A concise comparison table written to `$GITHUB_STEP_SUMMARY` (visible in the Actions UI). |
| **`report.md`** | Academic-style report with executive summary table, mathematical definitions, and raw JSON data. |
| **Google Sheet row** | Appended via Python script if sheet secrets are configured (see [§12](#12-google-sheets-integration)). |

### 10.8 Cleanup

- All study images (`human:compare`, `dockai:compare`, `ide:compare`, `cnb:compare`) are removed with `docker image rm`.
- BuildKit cache is pruned with `docker builder prune -af`.
- GitHub Action images (e.g., checkout, setup-python) are not affected.

---

## 11. Running the Benchmark

### 11.1 Configuration (`config.json`)

```json
{
  "repository_url": "axllent/mailpit",
  "subdirectory": "",
  "weights": {
    "size": 0.3,
    "time": 0.2,
    "security": 0.5
  }
}
```

| Field | Required | Description |
| :--- | :--- | :--- |
| `repository_url` | **Yes** | Target repo. Accepts full `https://` URLs or `owner/repo` shorthand (auto-prefixed with `https://github.com/`). |
| `subdirectory` | No | Subdirectory within the repo containing the Dockerfile and source. Leave `""` for repo root. Validated at runtime — workflow fails if the directory doesn't exist. |
| `weights.size` | No | Weight for image size metric (default `0.3`). |
| `weights.time` | No | Weight for build time metric (default `0.2`). |
| `weights.security` | No | Weight for security index metric (default `0.5`). |

> **Important:** Weights must sum to 1.0 (0.3 + 0.2 + 0.5) for the convex combination property described in [§2](#2-the-governing-equation).

### 11.2 Changing the Target Repository

To benchmark a different project, simply update `config.json`:

```bash
# Example: Benchmark a Go project
jq '.repository_url = "golang/go"' config.json > tmp.json && mv tmp.json config.json

# Example: Benchmark a subdirectory
jq '.repository_url = "monorepo/example" | .subdirectory = "services/api"' config.json > tmp.json && mv tmp.json config.json

# Commit and push — the workflow triggers automatically
git add config.json && git commit -m "benchmark: target golang/go" && git push
```

### 11.3 Triggers

| Trigger | Condition |
| :--- | :--- |
| **Push to `main`** | Only when `config.json` changes (via `paths` filter). |
| **Manual dispatch** | `workflow_dispatch` with optional `skip_dockai` input (`"true"` / `"false"`, default `"false"`). |

To trigger manually: **Actions** tab → **Compare DockAI vs Human vs CNB** → **Run workflow** → optionally set `skip_dockai` to `"true"` for faster testing runs.

### 11.4 Required Secrets

Configure these in **Settings** → **Secrets and variables** → **Actions**:

| Secret | Required By | Purpose |
| :--- | :--- | :--- |
| `OPENAI_API_KEY` | DockAI + IDE Agent | OpenAI API authentication for all LLM calls. |
| `LANGCHAIN_API_KEY` | DockAI | LangSmith tracing for DockAI agent observability. Optional — DockAI works without it but traces won't be recorded. |
| `GDRIVE_SHEET_ID` | Google Sheets step | The spreadsheet ID to append results to. If unset, the sheet step is silently skipped. |
| `GDRIVE_SERVICE_ACCOUNT_KEY` | Google Sheets step | JSON key for the Google service account with Sheets + Drive access. |
| `GITHUB_TOKEN` | Repo cloning | **Automatic** — provided by GitHub Actions. Used to authenticate shallow clones of private/public repositories. |

### 11.5 Job Permissions

The workflow requests only `contents: read` permission, which is the minimum needed to clone repositories and read workflow files.

### 11.6 Skipping DockAI

For faster testing (e.g., when debugging the IDE Agent or scoring logic), DockAI generation can be skipped:

- **Manual dispatch:** Set `skip_dockai` to `"true"` in the workflow dispatch form.
- DockAI will receive `status=missing` and a $C_{final} = 9999$ (disqualified) score.
- All other methods (Human, IDE Agent, CNB) still run normally.

---

## 12. Google Sheets Integration

### 12.1 Overview

After scoring, a Python script (Python 3.11) appends a row of results to a Google Sheet for longitudinal tracking across benchmark runs.

### 12.2 Dependencies

Installed from `.github/requirements.txt`:
- `gspread` (≥ 6.0.0) — Google Sheets API client
- `google-auth` (≥ 2.0.0) — Service account authentication

### 12.3 Authentication

- **OAuth scopes:** `https://www.googleapis.com/auth/spreadsheets` + `https://www.googleapis.com/auth/drive.file`
- The service account JSON key (`GDRIVE_SERVICE_ACCOUNT_KEY` secret) is written to a temp file for `gspread` to consume.
- The target sheet is opened by ID (`GDRIVE_SHEET_ID` secret).
- A specific worksheet tab can be targeted via the `SHEET_TAB` environment variable (defaults to `sheet1` if unset).

### 12.4 Setting Up Google Sheets (Step-by-Step)

1. **Create a Google Cloud project** (or use an existing one).
2. **Enable APIs:** Google Sheets API + Google Drive API.
3. **Create a service account** and download the JSON key file.
4. **Create a Google Sheet** and share it with the service account email (Editor permission).
5. **Copy the Sheet ID** from the URL: `https://docs.google.com/spreadsheets/d/<SHEET_ID>/edit`
6. **Add secrets** to your GitHub repo:
   - `GDRIVE_SHEET_ID` = the Sheet ID from step 5
   - `GDRIVE_SERVICE_ACCOUNT_KEY` = the entire JSON key file contents from step 3

### 12.5 Sheet Structure

If the sheet is empty, a **36-column header row** is auto-created:

| Column Group | Columns |
| :--- | :--- |
| **Metadata** | `timestamp_utc`, `repo`, `branch`, `winner` |
| **Baseline** (8 cols) | `baseline_status`, `baseline_size_mb`, `baseline_time_s`, `baseline_omega`, `baseline_errors`, `baseline_warnings`, `baseline_penalty`, `baseline_cost` |
| **Human** (8 cols) | `human_status`, `human_size_mb`, `human_time_s`, `human_omega`, `human_errors`, `human_warnings`, `human_penalty`, `human_cost` |
| **DockAI** (8 cols) | `dockai_status`, `dockai_size_mb`, `dockai_time_s`, `dockai_omega`, `dockai_errors`, `dockai_warnings`, `dockai_penalty`, `dockai_cost` |
| **IDE Agent** (8 cols) | `ide_status`, `ide_size_mb`, `ide_time_s`, `ide_omega`, `ide_errors`, `ide_warnings`, `ide_penalty`, `ide_cost` |

Image sizes are converted to MB before writing. Each benchmark run appends **one row**.

### 12.6 Auto-Formatting

The script applies formatting for readability (best-effort, non-fatal if it fails):

- **Frozen header row** with bold text and light gray background
- **Auto-filter** on all columns
- **Numeric formatting:** `0.00` for sizes, `0` for integer metrics, `0.0000` for penalties and costs
- **Conditional highlighting:** Green background when the winner is "DockAI"
- **Bold cost columns** for each method
- **Auto-resized column widths**

---

## 13. Artifact Structure

Each run uploads a structured artifact named `research-data-{repo-name}` (via `actions/upload-artifact@v5.0.0`):

```
artifact/{repo-name}/
├── report.md                          # Academic report with tables and formulas
├── dockerfiles/
│   ├── Dockerfile.human               # Original human-written Dockerfile
│   ├── Dockerfile.dockai              # DockAI-generated Dockerfile
│   └── Dockerfile.ide                 # IDE Agent-generated Dockerfile
└── data/
    ├── results.json                   # Structured metrics for all 4 methods
    ├── trivy-human.json               # Trivy scan results (Human)
    ├── trivy-dockai.json              # Trivy scan results (DockAI)
    ├── trivy-ide.json                 # Trivy scan results (IDE Agent)
    ├── trivy-cnb.json                 # Trivy scan results (CNB)
    ├── hadolint-human.json            # Hadolint results (Human)
    ├── hadolint-dockai.json           # Hadolint results (DockAI)
    ├── hadolint-ide.json              # Hadolint results (IDE Agent)
    ├── hadolint-cnb.json              # Hadolint stub (always [])
    ├── human_time.txt / human_status.txt
    ├── dockai_time.txt / dockai_status.txt
    ├── ide_time.txt / ide_status.txt
    └── cnb_time.txt / cnb_status.txt
```

The `repo-name` is derived from the repository URL: protocol/host are stripped, `.git` suffix removed, slashes and spaces replaced with hyphens (e.g., `axllent/mailpit` → `axllent-mailpit`).

### 13.1 Downloading Artifacts

1. Go to **Actions** tab → select the completed workflow run.
2. Scroll to the **Artifacts** section at the bottom.
3. Click **research-data-{repo-name}** to download the ZIP.
4. Artifacts are retained for the default GitHub retention period (90 days).

---

## 14. Infrastructure & Tooling Versions

| Tool | Version | Installation |
| :--- | :--- | :--- |
| **Runner OS** | `ubuntu-latest` | GitHub-hosted runner |
| **Trivy** | v0.68.1 | `aquasecurity/setup-trivy` (pinned commit SHA) |
| **Hadolint** | v3.1.0 | `hadolint/hadolint-action@v3.1.0` |
| **Pack CLI** | v0.36.2 | `buildpacks/github-actions/setup-pack@v5.9.7` |
| **Python** | 3.11 | `actions/setup-python@v5` |
| **jq** / **bc** | System | `apt-get install` |
| **DockAI Action** | v4 | `itzzjb/dockai@v4` |
| **actions/checkout** | v6.0.1 | — |
| **actions/cache** | v4 | — |
| **actions/upload-artifact** | v5.0.0 | — |

### 14.1 Caching Strategy

The workflow uses **five caches** to minimize redundant downloads and speed up subsequent runs:

| Cache | Path | Key Strategy | Purpose |
| :--- | :--- | :--- | :--- |
| **APT packages** | `/var/cache/apt/archives` | Hash of workflow file | Avoid re-downloading `jq`, `bc`. |
| **pip packages** | Managed by `setup-python` | Hash of `.github/requirements.txt` | Cache `gspread`, `google-auth`. |
| **Docker layers (BuildKit)** | `/tmp/.buildx-cache` | `github.sha` | Speed up repeated builds. |
| **UV packages** | `~/.cache/uv` | Hash of `requirements.txt` / `pyproject.toml` | Cache CUDA/PyTorch packages for DockAI. |
| **Trivy DB** | Managed by `setup-trivy` | Built-in | Avoid re-downloading the vulnerability database. |

---

## 15. Repository Structure

```
dockai-benchmarking/
├── config.json                              # Target repository & weight configuration
├── README.md                                # This file
└── .github/
    ├── requirements.txt                     # Python dependencies (gspread, google-auth)
    └── workflows/
        └── compare-builds.yml               # Main benchmark workflow (1075 lines)
```

| File | Purpose |
| :--- | :--- |
| `config.json` | Defines the target repository URL, optional subdirectory, and metric weights. |
| `README.md` | Comprehensive documentation (this file). |
| `.github/requirements.txt` | Python packages for the Google Sheets integration step. |
| `.github/workflows/compare-builds.yml` | The complete GitHub Actions workflow that orchestrates the entire benchmark. |

---

## 16. Troubleshooting

### 16.1 Common Issues

| Problem | Cause | Solution |
| :--- | :--- | :--- |
| **Workflow doesn't trigger on push** | `config.json` wasn't changed, or push was to a non-`main` branch. | Ensure the commit modifies `config.json` and targets the `main` branch. Or use manual dispatch. |
| **"Repo URL required" error** | `repository_url` is empty or missing in `config.json`. | Add a valid `repository_url` value. |
| **"Subdirectory not found" error** | The `subdirectory` value doesn't exist in the cloned repo. | Verify the path exists in the target repo, or set to `""`. |
| **All methods score 9999** | All Docker builds failed. | Check build logs. The target repo may not have a valid Dockerfile, or its dependencies may not be available on the runner. |
| **CNB build fails** | The project's language/framework isn't supported by Paketo buildpacks. | Expected for some projects. Other methods still benchmark against the best-available fallback baseline. |
| **IDE Agent generates a fallback** | OpenAI API call failed or returned invalid content. | Check that `OPENAI_API_KEY` is set and valid. Check for API rate limits. |
| **DockAI step is skipped** | `skip_dockai` is set to `"true"` or the step's `if` condition isn't met. | Set `skip_dockai` to `"false"` (default). |
| **Google Sheets not updating** | `GDRIVE_SHEET_ID` or `GDRIVE_SERVICE_ACCOUNT_KEY` is not set. | Secrets are optional; the step silently skips if unset. Add them to enable sheet output. |
| **"Permission denied" on Google Sheet** | Service account doesn't have Editor access to the sheet. | Share the sheet with the service account email address. |

### 16.2 Interpreting Results

| $C_{final}$ Value | Meaning |
| :--- | :--- |
| **0.00 – 0.49** | Dramatically better than the CNB baseline. The method excels across multiple dimensions. |
| **0.50 – 0.99** | Better than the CNB baseline. Meaningful improvement in at least some metrics. |
| **1.00** | Equivalent to the CNB baseline (this is the CNB's own score by definition). |
| **1.01 – 2.00** | Worse than the CNB baseline. May still be a valid, usable image. |
| **2.01+** | Significantly worse. Likely has large image size, many vulnerabilities, or heavy lint debt. |
| **9999** | Build failed — method disqualified. Check build logs for the root cause. |

### 16.3 Debugging Tips

- **Enable debug logging:** Re-run the workflow with `ACTIONS_STEP_DEBUG=true` in repository variables.
- **Check individual build steps:** Each build step outputs its status to `*_status.txt` and timing to `*_time.txt`.
- **Inspect Trivy/Hadolint JSON:** Download the artifact and examine the raw scan files for detailed vulnerability and lint information.
- **Test locally:** Clone the target repo and run `docker build` manually to verify the Dockerfile works outside CI.

---

## 17. Contributing

### 17.1 Adding a New Metric

1. Add the metric extraction logic in the "Calculate Academic Metrics" step.
2. Add a new weight to `config.json` (ensure all weights still sum to 1.0).
3. Update the normalization and scoring `awk` script to include the new dimension.
4. Update the `results.json` output schema.
5. Update the Google Sheets header and row generation.
6. Document the new metric in this README.

### 17.2 Adding a New Build Method

1. Add a generation step in the workflow (after the existing generation steps).
2. Add a build & timing step following the same pattern (capture status + time).
3. Add Hadolint and Trivy analysis for the new method.
4. Add scoring variables and logic in the "Calculate Academic Metrics" step.
5. Include the new method in `results.json`, the GitHub Summary, and the academic report.
6. Add the method's columns to the Google Sheets header.

### 17.3 Modifying Weights

Simply edit `config.json` — no code changes needed:

```json
{
  "weights": {
    "size": 0.20,
    "time": 0.10,
    "security": 0.70
  }
}
```

Push to `main` and the workflow will re-run with the new weights.

---

## 18. FAQ

**Q: Can I benchmark a private repository?**
A: Yes. The workflow uses `GITHUB_TOKEN` (automatically provided by GitHub Actions) to authenticate clones. For private repos in the same org, this works out of the box. For cross-org private repos, you may need a PAT with `repo` scope configured as a secret.

**Q: Why does the CNB baseline sometimes fail?**
A: Cloud Native Buildpacks support a specific set of languages and frameworks. If the target project uses an unsupported stack (e.g., a custom C++ project), CNB cannot auto-detect how to build it. When this happens, the fallback baseline logic kicks in (see [§4](#4-edge-case-handling-survivorship-bias)).

**Q: Why are different models used for DockAI and the IDE Agent?**
A: This is intentional. DockAI uses `gpt-5-mini` across all 8 agents for consistency within its pipeline, while the IDE Agent uses `gpt-4o` to simulate what a typical IDE coding assistant would use. The benchmark compares **systems**, not models — DockAI's advantage comes from its multi-agent architecture, not necessarily the underlying model.

**Q: How long does a benchmark run take?**
A: Typically 10–30 minutes depending on the target project size, Docker build complexity, and DockAI retry count. Skipping DockAI (`skip_dockai: "true"`) can reduce this significantly.

**Q: Can I run this locally instead of in GitHub Actions?**
A: The workflow is designed for GitHub Actions and relies on its secret management, runner environment, and action ecosystem. Running locally would require significant adaptation. For quick tests, use manual dispatch with `skip_dockai: "true"`.

**Q: What happens if the target repo has no Dockerfile?**
A: The Human method will be marked as `missing` (scoring 9999). DockAI and IDE Agent will still attempt to generate Dockerfiles from scratch, and CNB will attempt its buildpack detection. The benchmark remains valid for the methods that succeed.

**Q: How are the Hadolint severity levels mapped?**
A: Hadolint findings at `error` level add 0.10 each to the penalty, while `warning` and `info` level findings add 0.05 each. The penalty is multiplicative against the performance score.