import type { Metadata } from "next";

import { PortalDashboard } from "@/components/portal/portal-dashboard";

export const metadata: Metadata = { title: "Giao hàng" };

export default function DeliveryPage() {
  return <PortalDashboard kind="delivery" />;
}
