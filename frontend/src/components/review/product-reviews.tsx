"use client";

import { LoaderCircle, Star } from "lucide-react";
import Link from "next/link";
import { FormEvent, useCallback, useEffect, useMemo, useState } from "react";

import { getApiErrorMessage } from "@/lib/api-error";
import { formatDateTime } from "@/lib/order-format";
import { createReview, getProductReviews, getReviewEligibility, updateReview } from "@/services/engagement-service";
import { useAuthStore } from "@/stores/auth-store";
import type { Review, ReviewEligibility } from "@/types/engagement";

export function ProductReviews({ productId }: { productId:number }) {
  const { user, hasHydrated } = useAuthStore();
  const userRole = user?.role;
  const [reviews,setReviews]=useState<Review[]>([]); const [eligibility,setEligibility]=useState<ReviewEligibility|null>(null);
  const [rating,setRating]=useState(5); const [comment,setComment]=useState(""); const [loading,setLoading]=useState(true); const [busy,setBusy]=useState(false); const [error,setError]=useState<string|null>(null); const [message,setMessage]=useState<string|null>(null);
  const load=useCallback(async()=>{try{const page=await getProductReviews(productId);setReviews(page.content);if(userRole==="customer"){const result=await getReviewEligibility(productId);setEligibility(result);if(result.existingReview){setRating(result.existingReview.rating);setComment(result.existingReview.comment||"");}}setError(null);}catch(cause){setError(getApiErrorMessage(cause));}finally{setLoading(false);}},[productId,userRole]);
  useEffect(()=>{if(!hasHydrated)return;const timer=window.setTimeout(()=>void load(),0);return()=>window.clearTimeout(timer);},[hasHydrated,load]);
  const average=useMemo(()=>reviews.length?reviews.reduce((sum,item)=>sum+item.rating,0)/reviews.length:0,[reviews]);
  const submit=async(event:FormEvent)=>{event.preventDefault();setBusy(true);setError(null);setMessage(null);try{const input={productId,rating,comment};const saved=eligibility?.existingReview?await updateReview(eligibility.existingReview.id,input):await createReview(input);setReviews(current=>[saved,...current.filter(item=>item.id!==saved.id)]);setEligibility({purchased:true,canReview:false,existingReview:saved});setMessage(eligibility?.existingReview?"Đã cập nhật đánh giá.":"Cảm ơn bạn đã đánh giá sản phẩm.");}catch(cause){setError(getApiErrorMessage(cause));}finally{setBusy(false);}};

  return <section className="product-reviews"><header><div><span className="eyebrow">Trải nghiệm khách hàng</span><h2>Đánh giá sản phẩm</h2></div><div className="review-average"><Star size={21} fill="currentColor"/><strong>{average?average.toFixed(1):"—"}</strong><span>{reviews.length} đánh giá</span></div></header>
    {userRole==="customer"&&eligibility?.purchased&&<form className="review-form" onSubmit={submit}><h3>{eligibility.existingReview?"Cập nhật đánh giá của bạn":"Bạn thấy sản phẩm thế nào?"}</h3><div className="review-stars" aria-label={`${rating} sao`}>{[1,2,3,4,5].map(value=><button type="button" onClick={()=>setRating(value)} aria-label={`${value} sao`} key={value}><Star size={24} fill={value<=rating?"currentColor":"none"}/></button>)}</div><textarea rows={4} maxLength={1000} value={comment} onChange={event=>setComment(event.target.value)} placeholder="Chia sẻ cảm nhận về chất lượng sản phẩm..."/>{message&&<p className="form-message success">{message}</p>}<button className="primary-button" disabled={busy} type="submit">{busy?"Đang lưu...":eligibility.existingReview?"Cập nhật đánh giá":"Gửi đánh giá"}</button></form>}
    {userRole==="customer"&&eligibility&&!eligibility.purchased&&<p className="review-guidance">Bạn có thể đánh giá sau khi đơn chứa sản phẩm này đã được giao.</p>}
    {!user&&hasHydrated&&<p className="review-guidance"><Link href="/login">Đăng nhập</Link> để đánh giá sản phẩm đã mua.</p>}
    {error&&<p className="catalog-notice error">{error}</p>}
    <div className="review-list">{loading&&<div className="admin-empty"><LoaderCircle className="spin" size={18}/>Đang tải đánh giá...</div>}{!loading&&reviews.length===0&&<div className="admin-empty">Sản phẩm chưa có đánh giá.</div>}{reviews.map(review=><article key={review.id}><div className="review-avatar">{review.userName.slice(0,1).toUpperCase()}</div><div><header><strong>{review.userName}</strong><span>{[1,2,3,4,5].map(value=><Star key={value} size={14} fill={value<=review.rating?"currentColor":"none"}/>)}</span></header>{review.comment&&<p>{review.comment}</p>}<small>{formatDateTime(review.createdAt)}</small></div></article>)}</div>
  </section>;
}
