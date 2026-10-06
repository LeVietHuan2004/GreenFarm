"use client";

import { Bot, MessageCircle, Send, X } from "lucide-react";
import { isAxiosError } from "axios";
import { FormEvent, useEffect, useRef, useState } from "react";
import Link from "next/link";
import { getApiErrorMessage } from "@/lib/api-error";
import { CatalogImage } from "@/components/catalog/catalog-image";
import { formatPrice } from "@/lib/catalog-format";
import { chatService } from "@/services/chat-service";
import type { ChatMessage, ChatProduct } from "@/types/chat";

const welcome: ChatMessage = { id: 0, sender: "assistant", content: "Xin chào! Mình có thể hỗ trợ tìm sản phẩm, giá, tồn kho và chính sách mua hàng GreenFarm.", createdAt: "" };
const productLink = /(?:\b(?:xem|liên kết|link|sản_phẩm)\s*:\s*)?\/products\/[a-z0-9-]+/gi;

function visibleText(content: string, products: ChatProduct[] | undefined) {
  if (!products?.length) return content;
  const text = content.replace(productLink, "").replace(/[ \t]+\n/g, "\n").replace(/\n{3,}/g, "\n\n").trim();
  return text || "Sản phẩm phù hợp:";
}

function ProductCards({ products }: { products: ChatProduct[] }) {
  return <div className="chat-product-list">{products.map((product) => <Link href={`/products/${product.slug}`} className="chat-product-card" key={product.id}>
    <CatalogImage src={product.image} alt={product.name} className="chat-product-image"/>
    <span className="chat-product-copy"><strong>{product.name}</strong><small>{formatPrice(product.price)}{product.unit ? ` / ${product.unit}` : ""} · {product.stock > 0 ? `Còn ${product.stock}` : "Tạm hết hàng"}</small><em>Xem sản phẩm →</em></span>
  </Link>)}</div>;
}

export function ChatWidget() {
  const [open, setOpen] = useState(false);
  const [loaded, setLoaded] = useState(false);
  const [messages, setMessages] = useState<ChatMessage[]>([welcome]);
  const [input, setInput] = useState("");
  const [sending, setSending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const scrollRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!open || loaded) return;
    chatService.history().then((history) => {
      if (history.length) setMessages(history);
    }).catch(() => undefined).finally(() => setLoaded(true));
  }, [loaded, open]);

  useEffect(() => { scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: "smooth" }); }, [messages, open]);

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const content = input.trim();
    if (!content || sending) return;
    const optimistic: ChatMessage = { id: -Date.now(), sender: "user", content, createdAt: new Date().toISOString() };
    setMessages((current) => [...current, optimistic]); setInput(""); setSending(true); setError(null);
    try { const assistant = await chatService.send(content); setMessages((current) => [...current, assistant]); }
    catch (reason) {
      setError(
        isAxiosError(reason) && reason.code === "ECONNABORTED"
          ? "Trợ lý đang phản hồi lâu hơn bình thường. Vui lòng thử lại sau ít giây."
          : getApiErrorMessage(reason)
      );
    }
    finally { setSending(false); }
  };

  return <aside className="chat-widget" aria-label="Trợ lý GreenFarm">
    {open && <section className="chat-panel" role="dialog" aria-modal="false" aria-label="Chat cùng GreenFarm">
      <header className="chat-panel-header"><div><span><Bot size={18}/></span><div><strong>Trợ lý GreenFarm</strong><small>Tư vấn sản phẩm & chính sách</small></div></div><button type="button" onClick={() => setOpen(false)} aria-label="Đóng chat"><X size={18}/></button></header>
      <div className="chat-messages" ref={scrollRef}>{messages.map((message) => <article className={`chat-message ${message.sender}`} key={message.id}><div className="chat-message-content"><span className="chat-message-text">{visibleText(message.content, message.products)}</span>{message.sender === "assistant" && message.products?.length ? <ProductCards products={message.products}/> : null}</div></article>)}{sending && <article className="chat-message assistant"><span className="chat-typing">Đang trả lời…</span></article>}</div>
      {error && <p className="chat-error">{error}</p>}
      <form className="chat-composer" onSubmit={submit}><textarea value={input} maxLength={1500} rows={2} onChange={(event) => setInput(event.target.value)} placeholder="Hỏi về sản phẩm, giá hoặc tồn kho…" aria-label="Tin nhắn cho trợ lý"/><button type="submit" disabled={!input.trim() || sending} aria-label="Gửi tin nhắn"><Send size={17}/></button></form>
      <p className="chat-privacy">Không gửi số điện thoại, email, mã đơn hoặc thông tin thanh toán. Chat chỉ tư vấn dữ liệu công khai.</p>
    </section>}
    <button type="button" className="chat-launcher" onClick={() => setOpen((value) => !value)} aria-expanded={open} aria-label={open ? "Đóng trợ lý" : "Mở trợ lý GreenFarm"}>{open ? <X size={22}/> : <MessageCircle size={23}/>}<span>Hỏi GreenFarm</span></button>
  </aside>;
}
