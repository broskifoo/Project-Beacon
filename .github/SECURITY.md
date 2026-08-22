# Security Policy

Project Beacon is designed to operate in high-risk disaster zones and critical communication environments. Securing user data, cryptographic identities, and mesh stability is our highest priority.

---

## Supported Versions

Only the latest active major/minor release branch receives security updates.

| Version | Supported |
| --- | --- |
| `v0.1.x` | Active Security Support |
| `< v0.1.0` | Out of Support |

---

## Reporting a Vulnerability

**Do not open a public GitHub issue for security bugs or vulnerabilities.**

If you discover a vulnerability or security issue within Project Beacon (e.g. key extraction, routing loops, memory leaks in cryptographic operations), please report it securely through one of the following methods:

1. **Email:** Send an encrypted email to **[INSERT SECURITY EMAIL]** using our PGP Key:
   * **PGP Key ID:** `0xPLACEHOLDER`
   * **Fingerprint:** `XXXX XXXX XXXX XXXX XXXX XXXX XXXX XXXX XXXX XXXX`
2. **GitHub Private Disclosure:** Submit a private security advisory through the GitHub repository's Security tab (if enabled).

Please include the following details in your report:
* Subproject name (`beacon-core`, `beacon-mesh`, etc.) and tag/commit hash.
* A description of the vulnerability and the potential impact.
* Detailed steps to reproduce the vulnerability (proof-of-concept scripts or packets are highly appreciated).

---

## Disclosure Process

1. **Acknowledgment:** We will acknowledge receipt of your report within 48 hours.
2. **Triaging:** Our security response team will investigate the issue and assign a CVSS score.
3. **Remediation:** We will develop a fix and test it against our simulation suite. We aim to resolve vulnerabilities within 90 days of the report.
4. **Coordinated Disclosure:** We will publish a Security Advisory alongside a patched release, giving proper credit to the reporter.
