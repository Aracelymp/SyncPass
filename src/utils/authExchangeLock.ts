/**
 * Mecanismo para evitar intercambios Cognito->JWT propio duplicados
 */

let exchangePromise: Promise<unknown> | null = null;

export function runExchangeOnce<T>(fn: () => Promise<T>): Promise<T> {
  if (exchangePromise) {
    return exchangePromise as Promise<T>;
  }
  exchangePromise = fn()
    .finally(() => {
      exchangePromise = null;
    }) as Promise<T>;
  return exchangePromise as Promise<T>;
}

export function clearExchangeLock(): void {
  exchangePromise = null;
}
