import type { Metadata } from "next";

import { SiteHeader } from "@/components/layout/site-header";
import { ProfileClient } from "@/components/profile/profile-client";

export const metadata: Metadata = { title: "Hồ sơ" };

export default function ProfilePage() {
  return (
    <div className="app-shell">
      <SiteHeader />
      <ProfileClient />
    </div>
  );
}
