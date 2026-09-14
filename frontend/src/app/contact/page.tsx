import type { Metadata } from "next";
import { ContactForm } from "@/components/contact/contact-form";
import { SiteHeader } from "@/components/layout/site-header";

export const metadata: Metadata = { title: "Liên hệ hỗ trợ" };
export default function ContactPage(){return <><SiteHeader/><ContactForm/></>;}
