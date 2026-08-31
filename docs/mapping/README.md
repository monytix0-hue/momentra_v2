# Momentra Figma → Backend → APK Mapping (Freeze v4)

Implementation contract between Figma Android screens, Node `/v1` API, PostgreSQL, and the Compose APK.

**Law:** Figma owns labels. V018/V019 own wire codes. OpenAPI owns transport. Node owns commands. PostgreSQL owns truth. Projection owns reads.

## Chain

```
Screen → Capability → /v1 → Handler → Governance → Canonical|Projection → Event → Invalidation → APK refetch
```

## Documents

| File | Purpose |
|---|---|
| [FIELD_CONTRACTS.md](./FIELD_CONTRACTS.md) | Writable form field → API → SQL |
| [CAPABILITY_MATRIX.md](./CAPABILITY_MATRIX.md) | Action Center chip → route → handler |
| [COVERAGE_REPORT.md](./COVERAGE_REPORT.md) | Node status taxonomy + completeness gate |

## Forward migrations (after V001–V029)

| Version | Purpose |
|---|---|
| V031 | `business.company_location`, `core.moment.custom_type_label` |
| V032 | `shared.poll` kernel + Group poll migration |
| V033 | RLS, grants, indexes, device + media tables |
| V034 | Validation extensions |

V030 is validation-only; not install failure.
