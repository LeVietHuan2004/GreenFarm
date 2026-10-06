import type { Metadata } from "next";
import type { ReactNode } from "react";
import { CommerceProvider } from "@/components/commerce/commerce-provider";
import { ChatWidget } from "@/components/chat/chat-widget";

import "./globals.css";

export const metadata: Metadata = {
  title: {
    default: "GreenFarm",
    template: "%s | GreenFarm"
  },
  description: "Hệ thống kinh doanh nông sản trực tuyến GreenFarm"
};

type RootLayoutProps = Readonly<{
  children: ReactNode;
}>;

export default function RootLayout({ children }: RootLayoutProps) {
  return (
    <html lang="vi">
      <body><CommerceProvider>{children}<ChatWidget /></CommerceProvider></body>
    </html>
  );
}
