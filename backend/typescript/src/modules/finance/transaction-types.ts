/** Shared transaction CRUD DTO shapes (OpenAPI + clients). */
export interface ExpenseDetailDto {
  expenseId: string;
  momentId: string;
  amount: string;
  currencyCode: string;
  status: string;
  version: number;
  description: string | null;
  merchantName: string | null;
  categoryCode: string | null;
  subcategoryCode: string | null;
  financialAccountId: string | null;
  paymentMethodCode: string | null;
  effectiveAt: string | null;
  recurringScheduleId: string | null;
  attachmentIds: string[];
}

export interface TransactionVoidResponse {
  resourceId: string;
  momentId: string;
  status: string;
  version: number;
}

export type PaymentMethodCode = 'CASH' | 'CARD' | 'UPI' | 'BANK_TRANSFER' | 'WALLET' | 'OTHER';
