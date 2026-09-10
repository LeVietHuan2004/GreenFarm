import type { Metadata } from "next";

import { redirect } from "next/navigation";

export const metadata: Metadata = { title: "Cửa hàng" };

export default function StorePage() {
  redirect("/");
}
