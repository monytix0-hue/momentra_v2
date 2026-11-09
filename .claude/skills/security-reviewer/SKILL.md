---
name: security-reviewer
description: Security audit for auth, payments, finance code
user-invocable: false
---

# Security Reviewer Skill

## When to Use
Invoke this skill when reviewing code for security vulnerabilities, especially in authentication, payment, or financial data handling.

## Workflow
1. Identify sensitive endpoints
2. Check authentication middleware
3. Verify input validation
4. Review secrets management
5. Check audit logging
6. Generate report

## Project-Specific Context
- Backend: Express + Prisma + Firebase + Supabase
- Auth: JWT, bcrypt, Firebase Auth
- Sensitive areas: finance projections, payments, user data

## Commands
```bash
# Find auth-related files
grep -r "jwt\|bcrypt\|firebase" backend/typescript/src --include="*.ts" -l

# Check for secrets
grep -r -i "password\|secret\|key" backend --include="*.ts" --include="*.json" | grep -v node_modules
```
