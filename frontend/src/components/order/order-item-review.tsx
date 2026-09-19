"use client";

import { Star } from "lucide-react";
import { FormEvent, useEffect, useState } from "react";

import { getApiErrorMessage } from "@/lib/api-error";
import { createReview, getReviewEligibility, updateReview } from "@/services/engagement-service";
import type { ReviewEligibility } from "@/types/engagement";

export function OrderItemReview({ productId, productName }: { productId: number; productName: string }) {
  const [eligibility, setEligibility] = useState<ReviewEligibility | null>(null);
  const [open, setOpen] = useState(false);
  const [rating, setRating] = useState(5);
  const [comment, setComment] = useState("");
  const [busy, setBusy] = useState(false);
  const [feedback, setFeedback] = useState<string | null>(null);

  useEffect(() => { let active = true; getReviewEligibility(productId).then((value) => { if (!active) return; setEligibility(value); setRating(value.existingReview?.rating ?? 5); setComment(value.existingReview?.comment ?? ""); }).catch(() => undefined); return () => { active = false; }; }, [productId]);
  if (!eligibility?.purchased) return null;
  const submit = async (event: FormEvent) => { event.preventDefault(); setBusy(true); setFeedback(null); try { const input = { productId, rating, comment }; const saved = eligibility.existingReview ? await updateReview(eligibility.existingReview.id, input) : await createReview(input); setEligibility({ purchased: true, canReview: false, existingReview: saved }); setFeedback(eligibility.existingReview ? "Đã cập nhật đánh giá." : "Đã gửi đánh giá và cộng điểm tích lũy."); setOpen(false); } catch (cause) { setFeedback(getApiErrorMessage(cause)); } finally { setBusy(false); } };
  return <div className="order-item-review"><div><strong>{eligibility.existingReview ? "Đánh giá của bạn" : "Bạn đã nhận được sản phẩm?"}</strong><small>{eligibility.existingReview ? `${eligibility.existingReview.rating}/5 sao` : "Đánh giá để nhận thêm điểm tích lũy."}</small></div><button type="button" className="secondary-button" onClick={() => setOpen((value) => !value)}>{eligibility.existingReview ? "Cập nhật đánh giá" : "Đánh giá"}</button>{open && <form onSubmit={submit}><p>Đánh giá {productName}</p><div className="review-stars">{[1,2,3,4,5].map((value) => <button type="button" onClick={() => setRating(value)} aria-label={`${value} sao`} key={value}><Star size={19} fill={value <= rating ? "currentColor" : "none"} /></button>)}</div><textarea rows={3} maxLength={1000} value={comment} onChange={(event) => setComment(event.target.value)} placeholder="Chia sẻ trải nghiệm của bạn..."/><button type="submit" className="primary-button" disabled={busy}>{busy ? "Đang gửi..." : "Gửi đánh giá"}</button></form>}{feedback && <p className="order-review-feedback">{feedback}</p>}</div>;
}
