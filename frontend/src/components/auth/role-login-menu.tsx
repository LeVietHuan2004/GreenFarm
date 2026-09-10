"use client";

import { ChevronDown, ShieldCheck, Truck, UserRound } from "lucide-react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useRef, useState } from "react";

import { LOGIN_PORTALS, type LoginPortalId } from "@/config/login-portals";

type RoleLoginMenuProps = {
  currentPortal?: LoginPortalId;
  compact?: boolean;
};

function PortalIcon({ id, size = 17 }: { id: LoginPortalId; size?: number }) {
  if (id === "admin") return <ShieldCheck size={size} aria-hidden="true" />;
  if (id === "delivery") return <Truck size={size} aria-hidden="true" />;
  return <UserRound size={size} aria-hidden="true" />;
}

export function RoleLoginMenu({ currentPortal, compact = false }: RoleLoginMenuProps) {
  const pathname = usePathname();
  const rootRef = useRef<HTMLDivElement>(null);
  const [open, setOpen] = useState(false);

  useEffect(() => {
    const closeOnOutsideClick = (event: MouseEvent) => {
      if (!rootRef.current?.contains(event.target as Node)) setOpen(false);
    };
    const closeOnEscape = (event: KeyboardEvent) => {
      if (event.key === "Escape") setOpen(false);
    };

    document.addEventListener("mousedown", closeOnOutsideClick);
    document.addEventListener("keydown", closeOnEscape);
    return () => {
      document.removeEventListener("mousedown", closeOnOutsideClick);
      document.removeEventListener("keydown", closeOnEscape);
    };
  }, []);

  return (
    <div className="role-login-menu" ref={rootRef}>
      <button
        type="button"
        className={`role-menu-trigger${compact ? " compact" : ""}`}
        aria-label="Chọn cổng đăng nhập"
        aria-haspopup="menu"
        aria-expanded={open}
        title="Chọn cổng đăng nhập"
        onClick={() => setOpen((value) => !value)}
      >
        <UserRound size={19} aria-hidden="true" />
        {!compact && <span>Đăng nhập</span>}
        <ChevronDown size={15} aria-hidden="true" />
      </button>

      {open && (
        <div className="role-menu-popover" role="menu" aria-label="Cổng đăng nhập">
          {LOGIN_PORTALS.map((portal) => {
            const active = currentPortal
              ? currentPortal === portal.id
              : pathname === portal.href;
            return (
              <Link
                key={portal.id}
                href={portal.href}
                className={active ? "active" : undefined}
                role="menuitem"
                aria-current={active ? "page" : undefined}
                onClick={() => setOpen(false)}
              >
                <PortalIcon id={portal.id} />
                <span>{portal.label}</span>
              </Link>
            );
          })}
        </div>
      )}
    </div>
  );
}
