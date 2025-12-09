# Mathematical Framework: The Composite Optimization Metric ($C_{final}$)

**Context:** Comparative Empirical Analysis of AI-Generated Infrastructure-as-Code vs. Cloud Native Buildpacks
**Metric Version:** 1.0

## 1. Abstract
To rigorously evaluate the efficacy of AI-generated Dockerfiles ("DockAI"), this research rejects single-dimensional metrics (e.g., measuring only image size) which fail to capture the holistic cost of software artifacts. Instead, we propose a multi-dimensional cost function, the **Composite Optimization Metric ($C_{final}$)**.

This framework quantifies the trade-off between **storage efficiency**, **build latency**, **security posture**, and **syntactic quality**. The metric normalizes all values against an industry-standard baseline (Cloud Native Buildpacks), ensuring that the resulting score is a dimensionless ratio representing relative improvement or regression while avoiding edge-case distortions.

---

## 2. The Governing Equation

The final score for any given build method $M$ (where $M \in \{Human, DockAI, CNB\}$) is defined as the sum of a weighted performance score and a static quality penalty:

$$C_{final}(M) = \underbrace{\left[ \sum_{i} W_i \cdot \hat{X}_i \right]}_{\text{Performance Score}} \times \underbrace{\left(1 + P_{quality}\right)}_{\text{Scaled Static Penalty}}$$

**Interpretation:**
* **Lower is Better.**
* $C_{final} < 1.0$: The method is **superior** to the industry baseline.
* $C_{final} = 1.0$: The method is **equivalent** to the industry baseline (CNB).
* $C_{final} > 1.0$: The method is **inferior** to the industry baseline.

---

## 3. Component Analysis

### 3.1 Baseline Normalization ($\hat{X}$)
Directly comparing "Megabytes" (Size), "Seconds" (Time), and "Integer Counts" (Vulnerabilities) is mathematically invalid due to unit mismatch. We standardize inputs via **Baseline Normalization**.

For every metric $X$, the normalized value $\hat{X}$ is calculated as:

$$\hat{X}_{model} = \frac{X_{model}}{\max\left(X_{baseline}, \epsilon \cdot \max(1, |X_{baseline}|)\right)}$$

* **$X_{baseline}$**: The value obtained from the Cloud Native Buildpack (CNB).
* **$\epsilon$**: A small relative constant (default $10^{-6}$) to prevent division-by-zero without distorting ratios when the baseline is small. The relative form keeps scaling proportional for near-zero baselines while still guarding against zero.

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

### 3.4 The Static Analysis Penalty ($P_{quality}$)
Optimization cannot come at the cost of code quality. We utilize **Hadolint** (Haskell Dockerfile Linter) to enforce best practices (e.g., version pinning, shell safety).

$$P_{quality} = (0.1 \cdot N_{error}) + (0.05 \cdot N_{warning})$$

* **Errors** (e.g., invalid syntax) incur a heavy penalty (+0.10 to the final score).
* **Warnings** (e.g., style suggestions) incur a moderate penalty (+0.05).
* The multiplicative form $C_{final} = (\sum W_i \hat{X}_i)(1 + P_{quality})$ keeps lint penalties proportional to the underlying performance score—penalties cannot swamp a near-zero performance score but still scale linearly with lint findings.
* *Note:* Cloud Native Buildpacks do not produce a Dockerfile to lint; therefore, their $P_{quality}$ is defined as 0.

---

## 4. Edge Case Handling: Survivorship Bias
To ensure scientific rigor, we address the "failed build" scenario. If a generated Dockerfile fails to build successfully, simply assigning it a score of 0 or infinity would distort the statistical mean.

**Protocol:**
If `build_status != success`:
$$C_{final} = 9999$$

This "Sentinel Value" ensures that failed experiments are clearly categorized as inferior to any functional build, preventing the AI from "winning" by generating empty or non-functional code.

**Baseline fallback:** If the CNB baseline build fails, it still receives $C_{final}=9999$; for normalization of other methods, the baseline switches to the best successful build (Human or DockAI) to keep ratios finite and comparable.

---

## 5. Worked Example Calculation

Let us calculate the score for a hypothetical **DockAI** run compared to a **CNB Baseline**.

**1. Raw Data Input**
* **CNB (Baseline):** Size=200MB, Time=40s, Security Index=500.
* **DockAI (Model):** Size=100MB, Time=10s, Security Index=50.
* **DockAI Linting:** 0 Errors, 2 Warnings.

**2. Normalization Step**
* $\hat{S}$ (Size Ratio) = $100 / 200 = \mathbf{0.5}$
* $\hat{T}$ (Time Ratio) = $10 / 40 = \mathbf{0.25}$
* $\hat{\Omega}$ (Security Ratio) = $50 / 500 = \mathbf{0.1}$

**3. Weighted Sum Calculation ($C_{total}$)**
$$C_{total} = (0.3 \cdot 0.5) + (0.2 \cdot 0.25) + (0.5 \cdot 0.1)$$
$$C_{total} = 0.15 + 0.05 + 0.05 = \mathbf{0.25}$$

**4. Penalty Application ($P_{quality}$)**
$$P_{quality} = (0.1 \cdot 0) + (0.05 \cdot 2) = \mathbf{0.10}$$

**5. Final Score ($C_{final}$)**
$$C_{final} = 0.25 \times (1 + 0.10) = \mathbf{0.275}$$

**Conclusion:** The DockAI method scored **0.275**. Since $0.275 < 1.0$, the AI model is significantly optimized compared to the industry standard, offering a 72.5% reduction in the composite cost.