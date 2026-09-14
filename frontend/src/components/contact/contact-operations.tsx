"use client";

import { CheckCircle2, ChevronLeft, ChevronRight, LoaderCircle, MessageSquareReply } from "lucide-react";
import { useCallback, useEffect, useState } from "react";
import { getApiErrorMessage } from "@/lib/api-error";
import { formatDateTime } from "@/lib/order-format";
import { getOperationalContacts, replyContact, resolveContact } from "@/services/engagement-service";
import type { Contact, ContactStatus } from "@/types/engagement";
import type { PageData } from "@/types/catalog";

const emptyPage: PageData<Contact>={content:[],page:0,size:20,totalElements:0,totalPages:0,first:true,last:true};
const labels:Record<ContactStatus,string>={open:"Chờ xử lý",replied:"Đã phản hồi",resolved:"Đã xử lý"};

export function ContactOperations(){
  const [data,setData]=useState(emptyPage); const [status,setStatus]=useState<""|ContactStatus>(""); const [page,setPage]=useState(0);
  const [loading,setLoading]=useState(true); const [busy,setBusy]=useState<number|null>(null); const [error,setError]=useState<string|null>(null);
  const load=useCallback(async()=>{setLoading(true);try{setData(await getOperationalContacts(status||undefined,page));setError(null);}catch(cause){setError(getApiErrorMessage(cause));}finally{setLoading(false);}},[page,status]);
  useEffect(()=>{const timer=window.setTimeout(()=>void load(),0);return()=>window.clearTimeout(timer);},[load]);
  const replace=(contact:Contact)=>setData(current=>({...current,content:current.content.map(item=>item.id===contact.id?contact:item)}));
  const reply=async(contact:Contact)=>{const response=window.prompt("Nội dung phản hồi:",contact.response||"");if(!response?.trim())return;setBusy(contact.id);try{replace(await replyContact(contact.id,response.trim()));}catch(cause){setError(getApiErrorMessage(cause));}finally{setBusy(null);}};
  const resolve=async(contact:Contact)=>{setBusy(contact.id);try{replace(await resolveContact(contact.id));}catch(cause){setError(getApiErrorMessage(cause));}finally{setBusy(null);}};
  return <main className="admin-catalog-main contact-operations-main"><header className="admin-page-heading"><div><span className="eyebrow">Chăm sóc khách hàng</span><h1>Liên hệ hỗ trợ</h1><p>Phản hồi câu hỏi và đánh dấu các yêu cầu đã xử lý.</p></div><span className="admin-count"><MessageSquareReply size={15}/>{data.totalElements} liên hệ</span></header>
    {error&&<p className="catalog-notice error">{error}</p>}
    <section className="admin-list-card"><div className="admin-operations-toolbar"><label>Trạng thái<select value={status} onChange={event=>{setStatus(event.target.value as typeof status);setPage(0);}}><option value="">Tất cả</option>{Object.entries(labels).map(([value,label])=><option value={value} key={value}>{label}</option>)}</select></label></div>
      <div className="contact-ticket-list">{loading&&<div className="admin-empty"><LoaderCircle className="spin" size={18}/>Đang tải...</div>}{!loading&&data.content.length===0&&<div className="admin-empty">Không có liên hệ phù hợp.</div>}{data.content.map(contact=><article className="contact-ticket" key={contact.id}><header><div><strong>#{contact.id} · {contact.fullName}</strong><small>{contact.email||contact.phoneNumber||"Không có thông tin liên lạc"} · {formatDateTime(contact.createdAt)}</small></div><span className={`contact-status ${contact.status}`}>{labels[contact.status]}</span></header><p>{contact.message}</p>{contact.response&&<blockquote><strong>Phản hồi của {contact.respondedByName||"GreenFarm"}</strong>{contact.response}</blockquote>}<footer><button type="button" className="secondary-button" disabled={busy===contact.id} onClick={()=>void reply(contact)}><MessageSquareReply size={15}/>Phản hồi</button>{contact.status!=="resolved"&&<button type="button" className="primary-button" disabled={busy===contact.id} onClick={()=>void resolve(contact)}><CheckCircle2 size={15}/>Đã xử lý</button>}</footer></article>)}</div>
      {data.totalPages>1&&<nav className="pagination"><button disabled={data.first||loading} onClick={()=>setPage(value=>Math.max(0,value-1))}><ChevronLeft size={16}/>Trước</button><span>Trang {data.page+1}/{data.totalPages}</span><button disabled={data.last||loading} onClick={()=>setPage(value=>value+1)}>Sau<ChevronRight size={16}/></button></nav>}
    </section></main>;
}
