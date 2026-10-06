import type { Metadata } from "next";
import { AdminInventory } from "@/components/admin/admin-inventory";

export const metadata: Metadata = { title: "Quản lý kho" };

export default function InventoryPage() { return <AdminInventory />; }
