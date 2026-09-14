import type { Metadata } from "next";
import { RoleGuard } from "@/components/auth/role-guard";
import { ContactOperations } from "@/components/contact/contact-operations";
import { SiteHeader } from "@/components/layout/site-header";
export const metadata:Metadata={title:"Hỗ trợ khách hàng"};
export default function StaffContactsPage(){return <RoleGuard allowedRoles={["staff"]} loginHref="/staff/login"><div className="portal-shell"><SiteHeader/><ContactOperations/></div></RoleGuard>;}
