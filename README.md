# Research Protocol: Composite Optimization Metric ($C_{final}$)

This document specifies a reproducible evaluation protocol for comparing three build methods for a target repository: **Human Dockerfile**, **DockAI-generated Dockerfile**, and **Cloud Native Buildpacks (CNB)**. The GitHub Actions workflow in `.github/workflows/compare-builds.yml` executes the protocol end to end.

### At a Glance
- Objective: minimize $C_{final}$ (performance + lint penalty).
- Treatments: Human vs DockAI vs CNB baseline.
- Signals: image size, build time, vulnerability index, Hadolint findings.
- Outputs: `report.md`, `results.json`, Trivy + Hadolint JSON, timing/status files.

## 0. Quickstart (GitHub Actions)
1) Ensure secrets: `OPENAI_API_KEY` (required), default `GITHUB_TOKEN` (auto).
2) Set target repo via `workflow_dispatch` inputs (`repo_url`, `branch`) or `config.json` (`repository_url`).
3) Run the workflow `Compare DockAI vs Human vs CNB (Academic Benchmark)`.
4) Inspect the GitHub Summary and download the `research-data` artifact for full JSON traces.

## 1. Hypothesis and Design
- **Hypothesis:** AI-generated Dockerfiles (DockAI) can match or surpass human Dockerfiles and CNB baselines on image size, build time, and security posture without increasing lint violations.
- **Design:** One-shot build-off across three treatments (Human, DockAI, CNB) on the same codebase and commit. All steps run in a single workflow to minimize environmental variance.
- **Disqualification:** Any build that fails receives a sentinel score of `9999` to prevent selection bias from early termination.

## 2. Metrics and Objective Function
The final objective is minimized:

$$C_{final} = C_{total} + P_{quality}$$

### 2.1 Normalized Performance Metric ($C_{total}$)
Each term is a ratio to the CNB baseline (1.0 = baseline performance):

$$C_{total} = W_{size}\left(\frac{S_{model}}{S_{baseline}}\right) + W_{time}\left(\frac{T_{model}}{T_{baseline}}\right) + W_{sec}\left(\frac{\Omega_{model}}{\Omega_{baseline}}\right)$$

Where $S$ is image size (bytes), $T$ is build duration (seconds), and $\Omega$ is the vulnerability index. Weights reflect DevSecOps priorities: $W_{sec}=0.5$, $W_{size}=0.3$, $W_{time}=0.2$.

| Term | Symbol | Weight | Rationale |
| :--- | :--- | :--- | :--- |
| Security | $W_{sec}$ | 0.5 | CVE risk dominates release gating. |
| Size | $W_{size}$ | 0.3 | Smaller images reduce transfer/storage cost. |
| Time | $W_{time}$ | 0.2 | CI latency matters but is secondary. |

### 2.2 Vulnerability Index ($\Omega$)
Weighted by severity using Trivy JSON output:

$$\Omega = 10N_{critical} + 5N_{high} + 2N_{medium} + 1N_{low}$$

### 2.3 Static Analysis Penalty ($P_{quality}$)
Hadolint JSON output drives a linear penalty:

$$P_{quality} = 0.1N_{errors} + 0.05N_{warnings}$$

For CNB (no Dockerfile), an empty Hadolint report is emitted so its penalty is zero by construction.

## 3. Experimental Procedure (Workflow)
1) **Checkout**: Fetch the workflow repo and the target repo (from `workflow_dispatch` inputs or `config.json`).
2) **DockAI generation**: Run `itzzjb/dockai@v3` to produce `Dockerfile.dockai`; preserve the human `Dockerfile` if present.
3) **Static analysis**: Run `hadolint/hadolint-action@v3.1.0` on human and DockAI Dockerfiles; emit `hadolint-human.json`, `hadolint-dockai.json`; write `hadolint-cnb.json` as `[]`.
4) **Builds and timing**: Build Human, DockAI, and CNB images (pack builder `paketobuildpacks/builder:base`), capturing wall-clock durations and statuses.
5) **Security scanning**: Trivy scans each built image for CRITICAL/HIGH/MEDIUM/LOW CVEs, emitting `trivy-*.json`.
6) **Scoring**: Compute normalized metrics, penalties, and $C_{final}$; disqualify failed builds (`9999`). Select the winner as the lowest valid $C_{final}$.
7) **Reporting**: Publish a GitHub summary and persist artifacts (`report.md`, `results.json`, Trivy outputs, Hadolint outputs, timing/status files).

## 4. Inputs, Secrets, and Environment
- **Inputs (workflow_dispatch):** `repo_url` (e.g., `owner/repo` or full HTTPS), `branch` (default `main`).
- **Config fallback:** `config.json` with `repository_url` is used if inputs are empty.
- **Secrets:** `OPENAI_API_KEY` is required by DockAI; `GITHUB_TOKEN` is used for authenticated clone.
- **Runner:** `ubuntu-latest` with Docker, Buildx, pack, Trivy, and Hadolint (installed via official actions). Expect several GB of disk for three images.

## 5. Reproducibility Notes
- All three builds run in the same job to control for host variance.
- Baseline normalization uses CNB metrics; zero-division is guarded by fallback to 1.
- Failed builds are retained in the dataset with `status` and `9999` scores to avoid survivorship bias.
- Artifacts contain the full JSON traces to enable post-hoc analysis and re-scoring.

## 6. Outputs and Interpretation
- **Artifacts:** `report.md`, `results.json`, `trivy-*.json`, `hadolint-*.json`, `*_time.txt`, `*_status.txt`.
- **Winner selection:** lowest valid $C_{final}$; failed builds stay in the record with `9999`.
- **Penalty behavior:** Hadolint errors add 0.1 each; warnings add 0.05 each; CNB penalty is zero (no Dockerfile).
- **Security weight:** dominates the objective; large CVE reductions can offset slower builds.