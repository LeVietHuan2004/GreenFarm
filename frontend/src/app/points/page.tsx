import type { Metadata } from "next";
import { SiteHeader } from "@/components/layout/site-header";
import { LoyaltyPointsClient } from "@/components/loyalty/loyalty-points-client";

export const metadata: Metadata = { title: "Điểm tích lũy" };
export default function PointsPage() { return <div className="app-shell"><SiteHeader /><LoyaltyPointsClient /></div>; }
