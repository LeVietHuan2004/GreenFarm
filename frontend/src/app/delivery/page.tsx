import type { Metadata } from "next";

import { DeliveryOrdersDashboard } from "@/components/delivery/delivery-orders-dashboard";

export const metadata: Metadata = { title: "Giao hàng" };

export default function DeliveryPage() {
  return <DeliveryOrdersDashboard />;
}
