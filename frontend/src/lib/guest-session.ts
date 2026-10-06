const SESSION_KEY = "greenfarm_guest_session";
const ORDER_KEY = "greenfarm_guest_orders";
const CHECKOUT_KEY = "greenfarm_guest_checkout_key";

export type StoredGuestSession = { token: string; expiresAt: string };
export type StoredGuestOrder = { orderId: number; email: string; token: string };
export type StoredGuestCheckoutAttempt = { sessionToken: string; key: string; email: string };

export function getGuestSession(): StoredGuestSession | null {
  if (typeof window === "undefined") return null;
  try {
    const value = JSON.parse(localStorage.getItem(SESSION_KEY) ?? "null") as StoredGuestSession | null;
    if (!value?.token || new Date(value.expiresAt).getTime() <= Date.now()) { localStorage.removeItem(SESSION_KEY); return null; }
    return value;
  } catch { localStorage.removeItem(SESSION_KEY); return null; }
}

export function saveGuestSession(value: StoredGuestSession) { localStorage.setItem(SESSION_KEY, JSON.stringify(value)); }
export function clearGuestSession() {
  if (typeof window !== "undefined") {
    localStorage.removeItem(SESSION_KEY);
    localStorage.removeItem(CHECKOUT_KEY);
  }
}

export function getGuestCheckoutAttempt(sessionToken: string): StoredGuestCheckoutAttempt | null {
  try {
    const stored = JSON.parse(localStorage.getItem(CHECKOUT_KEY) ?? "null") as StoredGuestCheckoutAttempt | null;
    if (stored?.sessionToken === sessionToken && stored.key && /^[A-Za-z0-9_-]{16,64}$/.test(stored.key)) return stored;
  } catch { /* Ignore a malformed local value. */ }
  return null;
}

export function guestCheckoutKey(sessionToken: string, email: string): string {
  const stored = getGuestCheckoutAttempt(sessionToken);
  if (stored) {
    localStorage.setItem(CHECKOUT_KEY, JSON.stringify({ ...stored, email: email.trim().toLowerCase() }));
    return stored.key;
  }
  const key = crypto.randomUUID().replaceAll("-", "");
  localStorage.setItem(CHECKOUT_KEY, JSON.stringify({ sessionToken, key, email: email.trim().toLowerCase() }));
  return key;
}

export function saveGuestOrder(value: StoredGuestOrder) {
  const orders = getGuestOrders().filter((item) => item.orderId !== value.orderId);
  localStorage.setItem(ORDER_KEY, JSON.stringify([value, ...orders].slice(0, 10)));
}
export function getGuestOrders(): StoredGuestOrder[] {
  if (typeof window === "undefined") return [];
  try { const value=JSON.parse(localStorage.getItem(ORDER_KEY)??"[]"); return Array.isArray(value)?value:[]; } catch { return []; }
}

