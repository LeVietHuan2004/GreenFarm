"use client";

import { createContext, useCallback, useContext, useEffect, useRef, useState, type ReactNode } from "react";
import { Toaster, toast } from "sonner";

import { getApiErrorMessage } from "@/lib/api-error";
import { cartService } from "@/services/cart-service";
import { wishlistService } from "@/services/wishlist-service";
import { useAuthStore } from "@/stores/auth-store";
import type { Cart, Wishlist } from "@/types/commerce";

type CommerceContextValue = {
  cart: Cart;
  wishlist: Wishlist;
  enabled: boolean;
  loading: boolean;
  busy: boolean;
  error: string | null;
  lastAction: { target: "cart" | "wishlist"; id: number } | null;
  refresh: () => Promise<void>;
  addItem: (productId: number, quantity: number) => Promise<boolean>;
  updateItem: (itemId: number, quantity: number) => Promise<boolean>;
  removeItem: (itemId: number) => Promise<boolean>;
  clearCart: () => Promise<boolean>;
  toggleWishlist: (productId: number) => Promise<boolean>;
};

const CommerceContext = createContext<CommerceContextValue | null>(null);

export function CommerceProvider({ children }: { children: ReactNode }) {
  const { token, user, hasHydrated } = useAuthStore();
  const enabled = hasHydrated && Boolean(token) && user?.role === "customer";

  // Remount account state on session changes; a late response cannot populate another account.
  return (
    <CommerceSession key={enabled ? `${user?.id}:${token}` : "guest"} enabled={enabled}>
      {children}
      <Toaster richColors position="bottom-right" closeButton />
    </CommerceSession>
  );
}

function CommerceSession({ children, enabled }: { children: ReactNode; enabled: boolean }) {
  const [cart, setCart] = useState<Cart>({ items: [], totalItems: 0, subtotal: 0 });
  const [wishlist, setWishlist] = useState<Wishlist>({ items: [], count: 0 });
  const [loading, setLoading] = useState(enabled);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [lastAction, setLastAction] = useState<CommerceContextValue["lastAction"]>(null);
  const actionId = useRef(0);
  const loadPromise = useRef<Promise<void> | null>(null);
  const mutationLocked = useRef(false);
  const active = useRef(true);

  const load = useCallback((): Promise<void> => {
    if (!enabled) return Promise.resolve();
    if (loadPromise.current) return loadPromise.current;

    let request: Promise<void>;
    request = Promise.all([cartService.getCart(), wishlistService.getWishlist()])
    .then(([nextCart, nextWishlist]) => {
      if (!active.current) return;
      setCart(nextCart);
      setWishlist(nextWishlist);
      setError(null);
    }).catch((requestError) => {
      if (active.current) setError(getApiErrorMessage(requestError));
    }).finally(() => {
      if (loadPromise.current === request) loadPromise.current = null;
      if (active.current) setLoading(false);
    });
    loadPromise.current = request;
    return request;
  }, [enabled]);

  const refresh = useCallback(async () => {
    if (!enabled || mutationLocked.current) return;
    setLoading(true);
    await load();
  }, [enabled, load]);

  useEffect(() => {
    active.current = true;
    void load();
    const onFocus = () => { void refresh(); };
    window.addEventListener("focus", onFocus);
    return () => {
      active.current = false;
      window.removeEventListener("focus", onFocus);
    };
  }, [load, refresh]);

  async function mutate(
    task: () => Promise<void>,
    message: string,
    target: "cart" | "wishlist"
  ) {
    if (!enabled || mutationLocked.current) return false;
    mutationLocked.current = true;
    setBusy(true);
    try {
      // A click during the first account sync must wait, not be silently ignored.
      await loadPromise.current;
      await task();
      if (active.current) {
        setError(null);
        actionId.current += 1;
        setLastAction({ target, id: actionId.current });
        toast.success(message);
      }
      return active.current;
    } catch (requestError) {
      if (active.current) toast.error(getApiErrorMessage(requestError));
      return false;
    } finally {
      mutationLocked.current = false;
      if (active.current) setBusy(false);
    }
  }

  const value: CommerceContextValue = {
    cart, wishlist, enabled, loading, busy, error, lastAction, refresh,
    addItem: (productId, quantity) => mutate(async () => {
      const result = await cartService.addItem({ productId, quantity });
      if (active.current) setCart(result);
    }, "Đã thêm vào giỏ hàng", "cart"),
    updateItem: (itemId, quantity) => mutate(async () => {
      const result = await cartService.updateItem(itemId, { quantity });
      if (active.current) setCart(result);
    }, "Đã cập nhật số lượng", "cart"),
    removeItem: (itemId) => mutate(async () => {
      const result = await cartService.removeItem(itemId);
      if (active.current) setCart(result);
    }, "Đã xóa khỏi giỏ hàng", "cart"),
    clearCart: () => mutate(async () => {
      const result = await cartService.clear();
      if (active.current) setCart(result);
    }, "Đã xóa giỏ hàng", "cart"),
    toggleWishlist: (productId) => {
      const saved = wishlist.items.some((item) => item.product.id === productId);
      return mutate(async () => {
        const result = await (saved ? wishlistService.removeItem(productId) : wishlistService.addItem(productId));
        if (active.current) setWishlist(result);
      }, saved ? "Đã bỏ khỏi yêu thích" : "Đã lưu vào yêu thích", "wishlist");
    }
  };

  return <CommerceContext.Provider value={value}>
    {enabled && error && <div className="commerce-sync-error" role="alert">
      <span>Chưa thể tải giỏ hàng và yêu thích. {error}</span>
      <button type="button" onClick={() => void refresh()} disabled={loading}>Thử lại</button>
    </div>}
    {children}
  </CommerceContext.Provider>;
}

export function useCommerce() {
  const context = useContext(CommerceContext);
  if (!context) throw new Error("CommerceProvider is required");
  return context;
}
