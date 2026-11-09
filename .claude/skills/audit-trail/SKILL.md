---
name: audit-trail
description: Generate audit reports and verify remediation status
---

# Audit Trail Skill

## When to Use
Track changes and generate compliance reports, especially for Business v3 reset and Maestro certification.

## Workflow
1. Identify audit scripts
2. Run verification checks
3. Generate reports
4. Track remediation status

## Project-Specific Context
- Audit scripts in: backend/typescript/scripts/audit/
- Commands: npm run audit:deployment, npm run audit:supplemental, npm run audit:reaudit-v3
- Maestro certification being reset

## Commands
```bash
# List audit scripts
ls backend/typescript/scripts/audit/

# Run deployment audit
cd backend/typescript && npm run audit:deployment

# Check remediation dashboard
cd backend/typescript && npm run audit:remediation-dashboard
```
