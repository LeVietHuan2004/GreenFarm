const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const vm = require("node:vm");
const test = require("node:test");
const ts = require("typescript");
const axios = require("axios");

function load(file, dependencies, globals = {}) {
  const source = fs.readFileSync(path.join(__dirname, "../src", file), "utf8");
  const code = ts.transpileModule(source, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020, esModuleInterop: true }
  }).outputText;
  const module = { exports: {} };
  vm.runInNewContext(code, {
    module, exports: module.exports, process, ...globals,
    require(name) { if (name in dependencies) return dependencies[name]; throw new Error(`Unexpected import: ${name}`); }
  }, { filename: file });
  return module.exports;
}

const sessionKey = "greenfarm_guest_session";
const checkoutKey = "greenfarm_guest_checkout_key";
const orderKey = "greenfarm_guest_orders";
const storedSession = (token) => ({ token, expiresAt: "2099-01-01T00:00:00Z" });
const cart = { items: [], totalItems: 0, subtotal: 0 };

function harness(adapter, { token = "old-session", authenticated = false } = {}) {
  const storage = new Map();
  if (token) storage.set(sessionKey, JSON.stringify(storedSession(token)));
  const localStorage = {
    getItem: (key) => storage.get(key) ?? null,
    setItem: (key, value) => storage.set(key, value),
    removeItem: (key) => storage.delete(key)
  };
  const session = load("lib/guest-session.ts", {}, { window: {}, localStorage });
  let clears = 0;
  const state = { token: authenticated ? "account-token" : null, refreshToken: null,
    clearSession() { clears++; }, setSession() { throw new Error("Unexpected account refresh"); } };
  const { apiClient } = load("lib/axios-client.ts", {
    axios, "@/stores/auth-store": { useAuthStore: { getState: () => state } }
  });
  const calls = [];
  apiClient.defaults.adapter = async (config) => {
    calls.push(config);
    const result = await adapter(config, calls);
    const response = { data: result.data ?? { data: cart }, status: result.status ?? 200, statusText: "", headers: {}, config };
    if (response.status >= 400) throw new axios.AxiosError("Request rejected", "ERR_BAD_REQUEST", config, null, response);
    return response;
  };
  const { guestCommerceService: service } = load("services/guest-commerce-service.ts", {
    axios, "@/lib/axios-client": { apiClient }, "@/lib/guest-session": session
  });
  return { service, storage, session, calls, get accountClears() { return clears; } };
}

function recoveryAdapter(config) {
  if (config.url === "/public/guest/sessions") return { data: { data: storedSession("new-session") } };
  if (config.headers.get("X-Guest-Token") === "old-session") return { status: 401, data: { code: "INVALID_GUEST_SESSION" } };
  return {};
}

test("missing database session renews once and loads the cart, preserving saved order lookup data", async () => {
  const h = harness(recoveryAdapter);
  h.storage.set(checkoutKey, "old-checkout-attempt");
  h.storage.set(orderKey, "saved-order-lookup");
  assert.deepEqual(await h.service.getCart(), cart);
  assert.equal(h.session.getGuestSession().token, "new-session");
  assert.equal(h.calls.length, 3);
  assert.equal(h.storage.has(checkoutKey), false);
  assert.equal(h.storage.get(orderKey), "saved-order-lookup");
});

test("concurrent requests share one replacement session", async () => {
  const h = harness(recoveryAdapter);
  await Promise.all([h.service.getCart(), h.service.getCart()]);
  assert.equal(h.calls.filter((c) => c.url === "/public/guest/sessions").length, 1);
  assert.equal(h.session.getGuestSession().token, "new-session");
});

test("concurrent initial requests create only one session", async () => {
  const h = harness(recoveryAdapter, { token: null });
  const tokens = await Promise.all([h.service.token(), h.service.token(), h.service.token()]);
  assert.deepEqual(tokens, ["new-session", "new-session", "new-session"]);
  assert.equal(h.calls.length, 1);
});

for (const [status, code] of [[401, "GUEST_SESSION_EXPIRED"], [409, "GUEST_SESSION_CONSUMED"]]) {
  test(`renews rejected guest session ${code}`, async () => {
    const h = harness((config) => config.headers.get("X-Guest-Token") === "old-session"
      ? { status, data: { code } } : recoveryAdapter(config));
    await h.service.getCart();
    assert.equal(h.session.getGuestSession().token, "new-session");
  });
}

for (const [status, code] of [[500, "INTERNAL_ERROR"], [401, "UNAUTHORIZED"], [409, "INSUFFICIENT_STOCK"]]) {
  test(`does not replace a session for unrelated failure ${code}`, async () => {
    const h = harness(() => ({ status, data: { code } }));
    await assert.rejects(h.service.getCart());
    assert.equal(h.calls.length, 1);
    assert.equal(h.session.getGuestSession().token, "old-session");
  });
}

test("limits recovery to one retry", async () => {
  const h = harness((config) => config.url === "/public/guest/sessions" ? recoveryAdapter(config)
    : { status: 401, data: { code: "INVALID_GUEST_SESSION" } });
  await assert.rejects(h.service.getCart());
  assert.equal(h.calls.length, 3);
});

test("invalid guest merge does not sign out the account or block login", async () => {
  const h = harness(() => ({ status: 401, data: { code: "INVALID_GUEST_SESSION" } }), { authenticated: true });
  assert.equal(await h.service.merge(), null);
  assert.equal(h.accountClears, 0);
  assert.equal(h.session.getGuestSession(), null);
  assert.equal(h.calls.length, 1);
});

test("merge stock conflicts remain visible and preserve the guest cart", async () => {
  const h = harness(() => ({ status: 409, data: { code: "INSUFFICIENT_STOCK" } }), { authenticated: true });
  await assert.rejects(h.service.merge());
  assert.equal(h.session.getGuestSession().token, "old-session");
  assert.equal(h.accountClears, 0);
});

test("checkout is never automatically replayed", async () => {
  const h = harness(() => ({ status: 401, data: { code: "INVALID_GUEST_SESSION" } }));
  await assert.rejects(h.service.checkout({}));
  assert.equal(h.calls.length, 1);
  assert.equal(h.session.getGuestSession().token, "old-session");
});

test("malformed or expired stored sessions are rejected", () => {
  const h = harness(recoveryAdapter);
  for (const value of [{ token: "bad", expiresAt: "not-a-date" }, { token: 42, expiresAt: "2099-01-01" }, { token: "old", expiresAt: "2000-01-01" }]) {
    h.storage.set(sessionKey, JSON.stringify(value));
    assert.equal(h.session.getGuestSession(), null);
  }
});
