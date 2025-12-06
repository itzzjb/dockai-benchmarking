# Benchmarking Methodology: The Composite Optimization Metric ($C_{final}$)

This document outlines the mathematical framework used to empirically evaluate the performance of AI-generated Dockerfiles ("DockAI") against Human-authored configurations and industry-standard Cloud Native Buildpacks (CNB).

## 1. Overview
To provide a holistic evaluation, we reject single-dimensional metrics (e.g., measuring only image size). Instead, we utilize a **Composite Cost Function** that balances three competing priorities in modern DevOps:
1.  **Storage Efficiency** (Image Size)
2.  **Pipeline Latency** (Build Time)
3.  **Security Posture** (Vulnerability Exposure)
4.  **Code Quality** (Static Analysis Compliance)

## 2. The Core Equation

The final score ($C_{final}$) is calculated as the sum of a normalized performance metric and a quality penalty. **A lower score indicates better performance.**

$$C_{final} = C_{total} + P_{quality}$$

### 2.1 The Normalized Performance Metric ($C_{total}$)
We compare the Model (Human or DockAI) against the Baseline (CNB). All values are normalized to a ratio, where $1.0$ represents the baseline performance.

$$C_{total} = W_{size} \cdot \left( \frac{S_{model}}{S_{baseline}} \right) + W_{time} \cdot \left( \frac{T_{model}}{T_{baseline}} \right) + W_{sec} \cdot \left( \frac{\Omega_{model}}{\Omega_{baseline}} \right)$$

Where:
* $S$ = **Image Size** (MB)
* $T$ = **Build Time** (Seconds)
* $\Omega$ = **Vulnerability Index** (See Section 3)

### 2.2 Weight Distribution
Weights ($W$) are assigned based on **DevSecOps best practices**, prioritizing security above all else.

| Parameter | Weight ($W$) | Justification |
| :--- | :--- | :--- |
| **Security ($\Omega$)** | **0.5 (50%)** | In production environments, vulnerabilities are critical blockers. Mitigation is the highest priority. |
| **Size ($S$)** | **0.3 (30%)** | Smaller images reduce registry storage costs and speed up Kubernetes node provisioning. |
| **Time ($T$)** | **0.2 (20%)** | While build speed is important for CI feedback loops, it is secondary to runtime security and efficiency. |

---

## 3. The Vulnerability Index ($\Omega$)
Raw vulnerability counts are misleading (a Critical CVE is worse than 10 Low CVEs). We calculate a weighted index using **Trivy** scan results:

$$\Omega = (10 \times N_{critical}) + (5 \times N_{high}) + (2 \times N_{medium}) + (1 \times N_{low})$$

* $N_{severity}$ represents the count of CVEs at that severity level.

---

## 4. The Quality Penalty ($P_{quality}$)
To ensure the generated code adheres to syntactic best practices, we run **Hadolint** (Haskell Dockerfile Linter). The model is penalized for every violation found.

$$P_{quality} = (0.1 \times N_{errors}) + (0.05 \times N_{warnings})$$

* **Errors** (e.g., Invalid syntax) incur a heavy penalty (+0.1).
* **Warnings** (e.g., Not pinning versions) incur a moderate penalty (+0.05).

---

## 5. Example Calculation (Real Data)
*Data extracted from Report ID: `run-16606-dockai`*

**Baseline (CNB) Data:**
* Size: 263.25 MB
* Time: 37s
* Security Index ($\Omega$): 747

**DockAI Data:**
* Size: 166.06 MB
* Time: 1s
* Security Index ($\Omega$): 67
* Hadolint Errors: 0

**Step 1: Normalization**
* $\hat{S} = 166.06 / 263.25 \approx 0.63$
* $\hat{T} = 1 / 37 \approx 0.03$
* $\hat{\Omega} = 67 / 747 \approx 0.09$

**Step 2: Weighted Sum ($C_{total}$)**
$$C_{total} = (0.3 \times 0.63) + (0.2 \times 0.03) + (0.5 \times 0.09)$$
$$C_{total} = 0.189 + 0.006 + 0.045 = \mathbf{0.240}$$

**Step 3: Final Score**
Since there were 0 Hadolint errors, $P_{quality} = 0$.
$$C_{final} = 0.240 + 0 = \mathbf{0.240}$$

**Conclusion:** DockAI (0.240) performs $\approx 76\%$ better than the Industry Standard (1.0).