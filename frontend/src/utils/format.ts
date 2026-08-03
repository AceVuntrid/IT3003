const dateTimeFormat = new Intl.DateTimeFormat(undefined, {
  year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit',
});
const dateFormat = new Intl.DateTimeFormat(undefined, {
  year: 'numeric', month: 'short', day: 'numeric',
});

export function formatDateTime(value?: string | null): string {
  if (!value) return '—';
  return dateTimeFormat.format(new Date(value));
}

export function formatDate(value?: string | null): string {
  if (!value) return '—';
  // Date-only strings are parsed as UTC; add midday to avoid timezone rollover.
  const date = value.length === 10 ? new Date(`${value}T12:00:00`) : new Date(value);
  return dateFormat.format(date);
}

export function formatMoney(value?: number | null, currency = 'LKR'): string {
  if (value === null || value === undefined) return '—';
  const targetCurrency = (!currency || currency === 'AUD' || currency === 'USD') ? 'LKR' : currency;
  try {
    return new Intl.NumberFormat('en-LK', {
      style: 'currency', currency: targetCurrency, maximumFractionDigits: 2,
    }).format(value);
  } catch {
    return `LKR ${new Intl.NumberFormat().format(value)}`;
  }
}

export function formatNumber(value?: number | null): string {
  if (value === null || value === undefined) return '—';
  return new Intl.NumberFormat().format(value);
}

export function titleCase(value?: string | null): string {
  if (!value) return '—';
  return value.replaceAll('_', ' ').toLowerCase().replace(/^\w/, (c) => c.toUpperCase());
}

/** Converts a datetime-local input value to an ISO instant string. */
export function localInputToIso(value: string): string {
  return new Date(value).toISOString();
}

/** Default value for datetime-local inputs: now + offset hours, minute precision. */
export function localInputValue(offsetHours = 0): string {
  const date = new Date(Date.now() + offsetHours * 3600_000);
  date.setSeconds(0, 0);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}
