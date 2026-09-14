"use client";

import { Headphones, Mail, Phone, Send } from "lucide-react";
import { FormEvent, useEffect, useState } from "react";

import { getApiErrorMessage } from "@/lib/api-error";
import { createContact, getMyContacts } from "@/services/engagement-service";
import { useAuthStore } from "@/stores/auth-store";
import type { Contact } from "@/types/engagement";

export function ContactForm() {
  const { user } = useAuthStore();
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [phoneNumber, setPhoneNumber] = useState("");
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);
  const [contacts, setContacts] = useState<Contact[]>([]);
  const [feedback, setFeedback] = useState<{type:"success"|"error"; message:string}|null>(null);

  useEffect(()=>{if(!user)return;const timer=window.setTimeout(()=>{getMyContacts().then(setContacts).catch(()=>undefined);},0);return()=>window.clearTimeout(timer);},[user]);

  const submit = async (event: FormEvent) => {
    event.preventDefault(); setBusy(true); setFeedback(null);
    try {
      const contact = await createContact({ fullName: fullName.trim() || user?.name || "", email: email.trim() || user?.email, phoneNumber: phoneNumber.trim() || user?.phoneNumber || undefined, message });
      setMessage(""); setContacts(current=>[contact,...current]); setFeedback({ type:"success", message:`Đã gửi yêu cầu #${contact.id}. GreenFarm sẽ phản hồi sớm nhất.` });
    } catch (cause) { setFeedback({ type:"error", message:getApiErrorMessage(cause) }); }
    finally { setBusy(false); }
  };

  return <main className="catalog-main contact-main">
    <header className="contact-hero"><span><Headphones size={25}/></span><div><span className="eyebrow">GreenFarm hỗ trợ</span><h1>Chúng tôi có thể giúp gì cho bạn?</h1><p>Gửi câu hỏi về sản phẩm, đơn hàng hoặc giao nhận. Bộ phận hỗ trợ sẽ tiếp nhận và phản hồi.</p></div></header>
    <div className="contact-layout">
      <aside className="contact-info"><h2>Thông tin liên hệ</h2><p><Mail size={18}/> support@greenfarm.vn</p><p><Phone size={18}/> 1900 6868</p><small>Thời gian hỗ trợ: 08:00–20:00 mỗi ngày.</small></aside>
      <form className="contact-form" onSubmit={submit}>
        <label>Họ và tên<input required={!user?.name} maxLength={255} value={fullName} onChange={(event)=>setFullName(event.target.value)} placeholder={user?.name || "Nguyễn Văn A"}/></label>
        <div className="contact-form-row"><label>Email<input type="email" maxLength={255} value={email} onChange={(event)=>setEmail(event.target.value)} placeholder={user?.email || "ban@example.com"}/></label><label>Số điện thoại<input maxLength={30} value={phoneNumber} onChange={(event)=>setPhoneNumber(event.target.value)} placeholder={user?.phoneNumber || "0901234567"}/></label></div>
        <label>Nội dung<textarea required minLength={10} maxLength={2000} rows={7} value={message} onChange={(event)=>setMessage(event.target.value)} placeholder="Mô tả vấn đề bạn cần hỗ trợ..."/></label>
        {feedback && <p className={`form-message ${feedback.type}`}>{feedback.message}</p>}
        <button className="primary-button" type="submit" disabled={busy}><Send size={17}/>{busy?"Đang gửi...":"Gửi yêu cầu"}</button>
      </form>
    </div>
    {user&&contacts.length>0&&<section className="my-contact-list"><h2>Yêu cầu của bạn</h2>{contacts.map(contact=><article key={contact.id}><header><strong>Yêu cầu #{contact.id}</strong><span className={`contact-status ${contact.status}`}>{contact.status==="open"?"Chờ xử lý":contact.status==="replied"?"Đã phản hồi":"Đã xử lý"}</span></header><p>{contact.message}</p>{contact.response&&<blockquote><strong>Phản hồi từ GreenFarm</strong>{contact.response}</blockquote>}</article>)}</section>}
  </main>;
}
