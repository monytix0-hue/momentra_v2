"""Momentra FastAPI compute plane — optional heavy/forecast/narrative. No CRUD. No unrestricted DB."""
from __future__ import annotations

import os
import time
import uuid
from typing import Any, Optional

from fastapi import Depends, FastAPI, Header, HTTPException, Request, Response
from pydantic import BaseModel, Field

CONTRACT_VERSION = "1"

app = FastAPI(title="momentra-ai", version="1.0.0")


class AuthorizedFact(BaseModel):
    code: str
    value: Any = None
    unit: Optional[str] = None
    label: Optional[str] = None


class AnalyticsInput(BaseModel):
    contractVersion: str = CONTRACT_VERSION
    userId: str
    context: str
    companyId: Optional[str] = None
    momentId: Optional[str] = None
    currency: Optional[str] = None
    timeWindow: str = "P30D"
    metricCode: Optional[str] = None
    sourceVersion: str = "1"
    authorizedFacts: list[AuthorizedFact] = Field(default_factory=list)
    correlationId: Optional[str] = None
    purpose: str = "narrative"  # narrative | anomaly | forecast


class ComputeResult(BaseModel):
    contractVersion: str = CONTRACT_VERSION
    status: str
    version: str = "1"
    computedAt: str
    dataThrough: Optional[str] = None
    title: Optional[str] = None
    body: Optional[str] = None
    insightCode: Optional[str] = None
    severity: Optional[str] = None
    provider: str = "template"
    latencyMs: int = 0
    correlationId: Optional[str] = None


def require_internal_auth(
    x_momentra_internal_key: Optional[str] = Header(default=None, alias="X-Momentra-Internal-Key"),
) -> None:
    expected = os.environ.get("MOMENTRA_AI_INTERNAL_KEY", "").strip()
    if not expected:
        # Dev-open when unset; production must set the key.
        return
    if not x_momentra_internal_key or x_momentra_internal_key != expected:
        raise HTTPException(status_code=401, detail="Unauthorized compute plane")


@app.middleware("http")
async def correlation_middleware(request: Request, call_next):
    cid = request.headers.get("X-Correlation-Id") or str(uuid.uuid4())
    request.state.correlation_id = cid
    started = time.perf_counter()
    response: Response = await call_next(request)
    response.headers["X-Correlation-Id"] = cid
    elapsed_ms = int((time.perf_counter() - started) * 1000)
    print(
        __import__("json").dumps(
            {
                "level": "info",
                "service": "fastapi-ai",
                "path": request.url.path,
                "method": request.method,
                "status": response.status_code,
                "correlationId": cid,
                "durationMs": elapsed_ms,
            }
        ),
        flush=True,
    )
    return response


@app.get("/health/live")
def live():
    return {"status": "alive", "service": "fastapi-ai"}


@app.get("/health/ready")
def ready():
    # Own readiness plane — does not gate Momentra TypeScript /health/ready.
    return {"status": "ready", "service": "fastapi-ai", "contractVersion": CONTRACT_VERSION}


def _fact_map(facts: list[AuthorizedFact]) -> dict[str, Any]:
    return {f.code: f.value for f in facts}


def build_narrative(payload: AnalyticsInput) -> ComputeResult:
    now = time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime())
    facts = _fact_map(payload.authorizedFacts)
    started = time.perf_counter()

    spend = facts.get("spendTotal")
    mom = facts.get("momChangePct")
    burn = facts.get("burnRate")
    runway = facts.get("runwayMonths")
    contribution = facts.get("contributionPct")

    title = "Activity summary"
    body = "Structured facts are available for this scope."
    code = "TEMPLATE_SUMMARY"
    severity = "INFO"

    if runway is not None and burn is not None:
        title = "Runway outlook"
        body = (
            f"At the current burn rate, runway is approximately {runway} months "
            f"(burn {burn} {payload.currency or ''} per window)."
        )
        code = "BUSINESS_RUNWAY_NARRATIVE"
        severity = "MEDIUM" if float(runway) < 3 else "INFO"
    elif contribution is not None:
        title = "Contribution pattern"
        body = f"Group contribution completion is about {contribution}% for this window."
        code = "GROUP_CONTRIBUTION_NARRATIVE"
    elif mom is not None and spend is not None:
        direction = "up" if float(mom) >= 0 else "down"
        title = "Spending trend"
        body = (
            f"Spend is {spend} {payload.currency or ''} this window, "
            f"{abs(float(mom)):.1f}% {direction} versus the prior window."
        )
        code = "SPEND_TREND_NARRATIVE"
    elif spend is not None:
        title = "Spending snapshot"
        body = f"Recorded spend is {spend} {payload.currency or ''} for {payload.timeWindow}."
        code = "SPEND_SNAPSHOT_NARRATIVE"

    latency = int((time.perf_counter() - started) * 1000)
    return ComputeResult(
        status="READY",
        computedAt=now,
        dataThrough=now,
        title=title,
        body=body,
        insightCode=code,
        severity=severity,
        provider="template",
        latencyMs=latency,
        correlationId=payload.correlationId,
        version=payload.sourceVersion,
    )


@app.post("/v1/compute/narrative", response_model=ComputeResult)
def compute_narrative(
    payload: AnalyticsInput,
    request: Request,
    _: None = Depends(require_internal_auth),
):
    if payload.contractVersion != CONTRACT_VERSION:
        raise HTTPException(status_code=400, detail=f"Unsupported contractVersion; expected {CONTRACT_VERSION}")
    if not payload.correlationId:
        payload.correlationId = getattr(request.state, "correlation_id", None)
    return build_narrative(payload)


@app.post("/v1/inference")
def inference_legacy(payload: dict, _: None = Depends(require_internal_auth)):
    """Backward-compatible stub path — prefer /v1/compute/narrative."""
    return {
        "status": "stub",
        "insight": None,
        "message": "Use /v1/compute/narrative with AnalyticsInput contractVersion=1",
        "inputKeys": list(payload.keys()) if isinstance(payload, dict) else [],
        "contractVersion": CONTRACT_VERSION,
    }
