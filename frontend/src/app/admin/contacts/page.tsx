import type { Metadata } from "next";
import { AdminShell } from "@/components/admin/admin-shell";
import { ContactOperations } from "@/components/contact/contact-operations";
export const metadata:Metadata={title:"Liên hệ hỗ trợ"};
export default function AdminContactsPage(){return <AdminShell active="contacts"><ContactOperations/></AdminShell>;}
