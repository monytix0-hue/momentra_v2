/**
 * E-invoice (IRN) seam.
 *
 * An IRN is issued by the GST Invoice Registration Portal through a GST Suvidha Provider, using
 * credentials this deployment does not have. The shape is defined here so the snapshot can carry
 * the fields and the UI can render an honest "Not configured" state, but **no provider is wired
 * up**: `NullEInvoiceProvider` reports that fact rather than inventing an IRN.
 *
 * When a GSP account exists, implement `EInvoiceProvider` against it and register it at the
 * bottom of this file. Nothing else in the codebase needs to change.
 */

export type EInvoiceStatus = 'NOT_CONFIGURED' | 'PENDING' | 'REGISTERED' | 'FAILED';

/** The e-invoice block a posted invoice carries. Absent means the invoice was never submitted. */
export interface EInvoiceSnapshot {
  status: EInvoiceStatus;
  /** 64-character Invoice Reference Number from the IRP. */
  irn: string | null;
  /** Acknowledgement number and date the IRP returned. */
  ackNo: string | null;
  ackDate: string | null;
  /** The IRP's signed QR payload. */
  signedQr: string | null;
  /** Why the submission did not produce an IRN, in operator-readable words. */
  reason: string | null;
}

export interface EInvoiceRequest {
  /** The business's GSTIN. */
  sellerGstin: string;
  /** The counterparty's GSTIN; a B2C invoice is not eligible unless turnover requires it. */
  buyerGstin: string | null;
  invoiceNumber: string;
  invoiceDate: string;
  /** Rupees strings, exactly as the GST snapshot holds them. */
  taxableValue: string;
  cgst: string;
  sgst: string;
  igst: string;
  total: string;
}

export interface EInvoiceProvider {
  /** Whether this deployment can actually issue an IRN. */
  readonly configured: boolean;
  issue(request: EInvoiceRequest): Promise<EInvoiceSnapshot>;
  cancel(irn: string, reason: string): Promise<EInvoiceSnapshot>;
}

/** The honest default: this deployment cannot issue an IRN. */
export class NullEInvoiceProvider implements EInvoiceProvider {
  readonly configured = false;

  async issue(): Promise<EInvoiceSnapshot> {
    return {
      status: 'NOT_CONFIGURED',
      irn: null,
      ackNo: null,
      ackDate: null,
      signedQr: null,
      reason:
        'E-invoice needs a GSP account and IRP credentials, which this deployment does not have. ' +
        'The invoice is recorded without an IRN.',
    };
  }

  async cancel(): Promise<EInvoiceSnapshot> {
    return {
      status: 'NOT_CONFIGURED',
      irn: null,
      ackNo: null,
      ackDate: null,
      signedQr: null,
      reason: 'E-invoice is not configured, so there is no IRN to cancel.',
    };
  }
}

/**
 * The provider the application uses. Swap this for a GSP-backed implementation when credentials
 * exist; every call site already tolerates an unconfigured provider.
 */
export const eInvoiceProvider: EInvoiceProvider = new NullEInvoiceProvider();

/** Parse an e-invoice block off a stored contract, tolerating rows written before it existed. */
export function parseEInvoiceSnapshot(raw: unknown): EInvoiceSnapshot | null {
  if (!raw || typeof raw !== 'object') return null;
  const rec = raw as Record<string, unknown>;
  const status = String(rec.status ?? '') as EInvoiceStatus;
  if (!['NOT_CONFIGURED', 'PENDING', 'REGISTERED', 'FAILED'].includes(status)) return null;
  return {
    status,
    irn: rec.irn == null ? null : String(rec.irn),
    ackNo: rec.ackNo == null ? null : String(rec.ackNo),
    ackDate: rec.ackDate == null ? null : String(rec.ackDate),
    signedQr: rec.signedQr == null ? null : String(rec.signedQr),
    reason: rec.reason == null ? null : String(rec.reason),
  };
}
