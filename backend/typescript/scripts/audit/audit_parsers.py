#!/usr/bin/env python3
"""Shared parsers for deployment / supplemental / re-audit scripts."""
from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[4]


def norm_route(method: str, p: str) -> str:
    p = re.sub(r"/+", "/", p)
    if not p.startswith("/v1"):
        p = "/v1/" + p.lstrip("/")
    p = re.sub(r":([a-zA-Z]+)", r"{\1}", p)
    p = re.sub(r"\{[^}]+\}", "{}", p)
    return f"{method.upper()} {p}"


def parse_router_routes(router_path: Path | None = None) -> list[tuple[str, str]]:
    router_path = router_path or ROOT / "backend/typescript/src/api/v1/router.ts"
    text = router_path.read_text(encoding="utf-8")
    routes: list[tuple[str, str]] = []
    for m in re.finditer(r"v1Router\.(get|post|patch|delete)\(\s*['`]([^'`]+)['`]", text):
        path = "/v1" + re.sub(r":([a-zA-Z]+)", r"{\1}", m.group(2))
        if "${" in path:
            continue
        routes.append((m.group(1).upper(), path))

    business_facets = ["pulse", "life", "memory", "finance", "actions"]
    group_facets = ["pulse", "life", "memory", "finance"]
    business_lists = [
        "expenses",
        "revenues",
        "invoices",
        "issues",
        "improvements",
        "updates",
        "approvals",
        "memories",
    ]
    for facet in business_facets:
        routes.append(("GET", f"/v1/business/moments/{{momentId}}/{facet}"))
    for segment in business_lists:
        routes.append(("GET", f"/v1/business/moments/{{momentId}}/{segment}"))
    for facet in group_facets:
        routes.append(("GET", f"/v1/group/moments/{{momentId}}/{facet}"))
    return routes


def parse_router_mutations_missing_idempotency(router_path: Path | None = None) -> list[str]:
    router_path = router_path or ROOT / "backend/typescript/src/api/v1/router.ts"
    text = router_path.read_text(encoding="utf-8")
    missing: list[str] = []
    for m in re.finditer(
        r"v1Router\.(post|patch|delete)\(\s*['`]([^'`]+)['`][^)]*\)\s*,\s*async",
        text,
    ):
        block_start = m.start()
        snippet = text[block_start : block_start + 400]
        if "requireIdempotencyKey" not in snippet:
            missing.append(norm_route(m.group(1), "/v1" + re.sub(r":([a-zA-Z]+)", r"{\1}", m.group(2))))
    return missing


def parse_apk_api_routes(path: Path | None = None) -> set[str]:
    path = path or ROOT / "apk/app/src/main/java/com/example/momentra/data/api/ApiService.kt"
    text = path.read_text(encoding="utf-8")
    out: set[str] = set()
    for b in re.split(r"\n\s*(?=@(?:GET|POST|PATCH|DELETE))", text):
        meth = re.search(r"@(GET|POST|PATCH|DELETE)", b)
        p = re.search(r'"(v1/[^"]+)"', b)
        if meth and p:
            norm = "/v1/" + re.sub(r"\{[^}]+\}", "{}", p.group(1).replace("v1/", "", 1))
            out.add(norm_route(meth.group(1), norm))
    return out


def parse_ios_api_routes(path: Path | None = None) -> set[str]:
    path = path or ROOT / "momentra/momentra/API/APIClient.swift"
    text = path.read_text(encoding="utf-8")
    out: set[str] = set()
    patterns = (
        r"authorized(Get|Post|Patch|Delete)\(\s*path:\s*\"([^\"]+)\"",
        r"authorizedPostWithHints\(\s*path:\s*\"([^\"]+)\"",
    )
    for pattern in patterns:
        for m in re.finditer(pattern, text, re.S):
            if pattern.startswith(r"authorizedPostWithHints"):
                meth, p = "Post", m.group(1)
            else:
                meth, p = m.group(1), m.group(2)
            p = re.sub(r"\\\([^)]+\)", "{}", p)
            p = p.split("?", 1)[0]
            if not p.startswith("v1/"):
                p = "v1/" + p
            out.add(norm_route(meth, "/" + p))
    return out


def parse_apk_repository_api_calls() -> dict[str, list[str]]:
    """Map normalized route -> repository files that invoke matching api.* methods."""
    repo_dir = ROOT / "apk/app/src/main/java/com/example/momentra/data/repository"
    api_text = (ROOT / "apk/app/src/main/java/com/example/momentra/data/api/ApiService.kt").read_text(
        encoding="utf-8"
    )
    method_to_route: dict[str, str] = {}
    blocks = re.split(r"\n\s*(?=@(?:GET|POST|PATCH|DELETE))", api_text)
    for b in blocks:
        meth = re.search(r"@(GET|POST|PATCH|DELETE)", b)
        path_m = re.search(r'"(v1/[^"]+)"', b)
        fn = re.search(r"(?:suspend\s+)?fun\s+(\w+)\(", b)
        if meth and path_m and fn:
            norm = "/v1/" + re.sub(r"\{[^}]+\}", "{}", path_m.group(1).replace("v1/", "", 1))
            method_to_route[fn.group(1)] = norm_route(meth.group(1), norm)

    calls: dict[str, list[str]] = {}
    if not repo_dir.exists():
        return calls
    for repo_file in repo_dir.glob("*.kt"):
        text = repo_file.read_text(encoding="utf-8")
        for fn, route in method_to_route.items():
            if re.search(rf"\bapi\.{fn}\b", text):
                calls.setdefault(route, []).append(str(repo_file.relative_to(ROOT)))
    return calls


def parse_ios_repository_api_calls() -> dict[str, list[str]]:
    """Map normalized route -> Swift call sites (APIClient methods + authorized* paths)."""
    client_path = ROOT / "momentra/momentra/API/APIClient.swift"
    text = client_path.read_text(encoding="utf-8")
    fn_to_route: dict[str, str] = {}
    for m in re.finditer(
        r"func\s+(\w+)\([^)]*\)[^{]*\{[^}]*authorized(Get|Post|Patch|Delete)\(\s*path:\s*\"([^\"]+)\"",
        text,
        re.S,
    ):
        p = re.sub(r"\\\([^)]+\)", "{}", m.group(3))
        if not p.startswith("v1/"):
            p = "v1/" + p
        fn_to_route[m.group(1)] = norm_route(m.group(2), "/" + p)

    for m in re.finditer(r'authorized(Get|Post|Patch|Delete)\(\s*path:\s*"([^"]+)"', text, re.S):
        p = re.sub(r"\\\([^)]+\)", "{}", m.group(2))
        if not p.startswith("v1/"):
            p = "v1/" + p
        route = norm_route(m.group(1), "/" + p)
        fn_to_route.setdefault(route, route)

    shell_dir = ROOT / "momentra/momentra"
    calls: dict[str, list[str]] = {}
    for swift in shell_dir.rglob("*.swift"):
        if "APIClient.swift" in str(swift) or "OpenAPI" in str(swift):
            continue
        try:
            st = swift.read_text(encoding="utf-8")
        except OSError:
            continue
        for fn, route in fn_to_route.items():
            if route.startswith("GET ") or route.startswith("POST "):
                continue
            if re.search(rf"APIClient\.shared\.{fn}\b", st):
                calls.setdefault(route, []).append(str(swift.relative_to(ROOT)))
        for route_key in fn_to_route.values():
            if route_key.startswith(("GET ", "POST ", "PATCH ", "DELETE ")):
                path_part = route_key.split(" ", 1)[1].replace("/v1/", "v1/")
                if f'path: "{path_part}"' in st or path_part.replace("{}", "") in st:
                    calls.setdefault(route_key, []).append(str(swift.relative_to(ROOT)))

    for m in re.finditer(r'authorized(Get|Post|Patch|Delete)\(\s*path:\s*"([^"]+)"', text, re.S):
        p = re.sub(r"\\\([^)]+\)", "{}", m.group(2))
        if not p.startswith("v1/"):
            p = "v1/" + p
        route = norm_route(m.group(1), "/" + p)
        for swift in shell_dir.rglob("*.swift"):
            if "APIClient.swift" in str(swift):
                continue
            try:
                st = swift.read_text(encoding="utf-8")
            except OSError:
                continue
            bare = p.replace("{}", "")
            if bare in st and route not in calls:
                calls.setdefault(route, []).append(str(swift.relative_to(ROOT)))
    return calls


def backend_refs_table(table: str, search_dirs: list[Path] | None = None) -> bool:
    search_dirs = search_dirs or [
        ROOT / "backend/typescript/src",
        ROOT / "backend/workers",
    ]
    bare = table.split(".")[-1]
    for d in search_dirs:
        if not d.exists():
            continue
        for fp in d.rglob("*"):
            if fp.suffix not in {".ts", ".tsx", ".js"} or "node_modules" in fp.parts:
                continue
            try:
                t = fp.read_text(encoding="utf-8", errors="ignore")
            except OSError:
                continue
            if table in t or bare in t:
                return True
    return False
