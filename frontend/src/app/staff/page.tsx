import type { Metadata } from "next";
import { StaffOrdersDashboard } from "@/components/staff/staff-orders-dashboard";
export const metadata: Metadata = { title: "Van hanh don hang" };
export default function StaffPage() { return <StaffOrdersDashboard />; }
