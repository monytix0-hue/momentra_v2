/** ISO 4217 travel currencies commonly used for international trips. */
export const TRAVEL_CURRENCY_CODES = [
  'USD',
  'EUR',
  'GBP',
  'INR',
  'AED',
  'SAR',
  'JPY',
  'CNY',
  'HKD',
  'SGD',
  'THB',
  'MYR',
  'IDR',
  'PHP',
  'VND',
  'KRW',
  'TWD',
  'AUD',
  'NZD',
  'CAD',
  'CHF',
  'SEK',
  'NOK',
  'DKK',
  'PLN',
  'CZK',
  'HUF',
  'TRY',
  'ILS',
  'EGP',
  'ZAR',
  'MAD',
  'KES',
  'MXN',
  'BRL',
  'ARS',
  'CLP',
  'COP',
  'PEN',
  'PKR',
  'BDT',
  'LKR',
  'NPR',
  'RUB',
  'QAR',
  'KWD',
  'BHD',
  'OMR',
  'RON',
  'ISK',
  'NGN',
  'FJD',
  'BTN',
  'MMK',
  'KHR',
  'LAK',
] as const;

export type TravelCurrencyCode = (typeof TRAVEL_CURRENCY_CODES)[number];

const TRAVEL_CURRENCY_SET = new Set<string>(TRAVEL_CURRENCY_CODES);

export function isTravelCurrencyCode(code: string): code is TravelCurrencyCode {
  return TRAVEL_CURRENCY_SET.has(code.toUpperCase());
}

export function assertTravelCurrencyCode(code: string): string {
  const upper = code.toUpperCase();
  if (!TRAVEL_CURRENCY_SET.has(upper)) {
    throw new Error(`Unsupported currency code: ${code}`);
  }
  return upper;
}
