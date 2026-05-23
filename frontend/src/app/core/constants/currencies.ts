/** Supported demo currencies — keep in sync with sample-rates.json */
export const SUPPORTED_CURRENCIES = [
  { code: 'EUR', name: 'Euro', spread: '0.00% (base)' },
  { code: 'USD', name: 'US Dollar', spread: '2.75%' },
  { code: 'GBP', name: 'British Pound', spread: '2.75%' },
  { code: 'PLN', name: 'Polish Zloty', spread: '2.75%' },
  { code: 'CHF', name: 'Swiss Franc', spread: '2.75%' },
  { code: 'JPY', name: 'Japanese Yen', spread: '3.25%' },
  { code: 'HKD', name: 'Hong Kong Dollar', spread: '3.25%' },
  { code: 'KRW', name: 'South Korean Won', spread: '3.25%' },
  { code: 'MYR', name: 'Malaysian Ringgit', spread: '4.50%' },
  { code: 'INR', name: 'Indian Rupee', spread: '4.50%' },
  { code: 'MXN', name: 'Mexican Peso', spread: '4.50%' },
  { code: 'RUB', name: 'Russian Ruble', spread: '6.00%' },
  { code: 'CNY', name: 'Chinese Yuan', spread: '6.00%' },
  { code: 'ZAR', name: 'South African Rand', spread: '6.00%' },
  { code: 'AUD', name: 'Australian Dollar', spread: '2.75%' },
  { code: 'CAD', name: 'Canadian Dollar', spread: '2.75%' },
  { code: 'SEK', name: 'Swedish Krona', spread: '2.75%' },
  { code: 'NOK', name: 'Norwegian Krone', spread: '2.75%' },
] as const;

export const CURRENCY_CODES = SUPPORTED_CURRENCIES.map((c) => c.code);
