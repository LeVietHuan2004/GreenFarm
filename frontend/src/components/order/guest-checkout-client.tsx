"use client";

import { Banknote, CreditCard, PackageCheck, UserRound } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { toast } from "sonner";
import { useCommerce } from "@/components/commerce/commerce-provider";
import { CouponCodeInput } from "@/components/order/coupon-code-input";
import { getApiErrorMessage } from "@/lib/api-error";
import { formatPrice } from "@/lib/catalog-format";
import { clearGuestSession, getGuestCheckoutAttempt, getGuestSession, guestCheckoutKey, saveGuestOrder } from "@/lib/guest-session";
import { guestCommerceService } from "@/services/guest-commerce-service";
import type { CheckoutPreview, GuestCheckoutInput, PaymentMethod, PaymentMethodOptions } from "@/types/order";
import type { AvailableCoupon } from "@/types/coupon";

export function GuestCheckoutClient({coupons,couponsLoading,couponsError}:{coupons:AvailableCoupon[];couponsLoading:boolean;couponsError:boolean}){
  const router=useRouter(); const commerce=useCommerce();
  const [form,setForm]=useState({name:"",phone:"",email:"",shippingAddress:"",shippingCity:"",shippingMethod:"standard" as "standard"|"express"});
  const [coupon,setCoupon]=useState(""); const [shippingCoupon,setShippingCoupon]=useState("");
  const [paymentMethod,setPaymentMethod]=useState<PaymentMethod>("cod"); const [preview,setPreview]=useState<CheckoutPreview|null>(null);
  const [options,setOptions]=useState<PaymentMethodOptions>({codAvailable:true,vnpayAvailable:false}); const [busy,setBusy]=useState(false);
  useEffect(()=>{guestCommerceService.paymentMethods().then(setOptions).catch(()=>undefined);},[]);
  useEffect(()=>{
    let active=true;
    const session=getGuestSession(); const attempt=session&&getGuestCheckoutAttempt(session.token);
    if(attempt) void guestCommerceService.recover(session.token,attempt.key).then(result=>{
      if(!active)return;
      saveGuestOrder({orderId:result.order.id,email:attempt.email,token:result.lookupToken});
      clearGuestSession();
      if(result.paymentUrl) window.location.assign(result.paymentUrl);
      else router.replace(`/guest-orders?orderId=${result.order.id}`);
    }).catch(()=>undefined);
    return ()=>{active=false;};
  },[router]);
  const input=async():Promise<GuestCheckoutInput>=>({...form,couponCode:coupon.trim()||undefined,freeShippingCouponCode:shippingCoupon.trim()||undefined,paymentMethod,idempotencyKey:guestCheckoutKey(await guestCommerceService.token(),form.email)});
  const validate=()=>{if(form.name.trim().length<2||!/^(?:\+84|0)[0-9]{9,10}$/.test(form.phone.replaceAll(" ",""))||!/^\S+@\S+\.\S+$/.test(form.email)||!form.shippingAddress.trim()||!form.shippingCity.trim()){toast.error("Vui lòng nhập đầy đủ và đúng thông tin nhận hàng");return false;}return true;};
  const calculate=async()=>{if(!validate())return;setBusy(true);try{setPreview(await guestCommerceService.preview(await input()));toast.success("Đã tính lại đơn hàng");}catch(error){toast.error(getApiErrorMessage(error));}finally{setBusy(false);}};
  const place=async()=>{if(!validate())return;setBusy(true);try{
    const result=await guestCommerceService.checkout(await input());
    saveGuestOrder({orderId:result.order.id,email:form.email.trim().toLowerCase(),token:result.lookupToken});
    clearGuestSession();
    if(result.paymentUrl){window.location.assign(result.paymentUrl);return;}
    if(result.order.payment?.method==="vnpay"&&result.order.payment.status==="pending") toast.warning("Đơn đã được tạo nhưng phiên VNPAY đã hết hạn. Vui lòng tra cứu đơn hoặc liên hệ GreenFarm.");
    else toast.success(result.replayed?"Đơn hàng đã được tạo trước đó":"Đặt hàng thành công");
    router.replace(`/guest-orders?orderId=${result.order.id}`);
  }catch(error){toast.error(getApiErrorMessage(error));}finally{setBusy(false);}};
  if(commerce.cart.items.length===0)return <main className="catalog-main"><div className="catalog-empty"><PackageCheck size={42}/><h1>Giỏ hàng đang trống</h1><Link className="primary-button" href="/products">Chọn sản phẩm</Link></div></main>;
  return <main className="catalog-main checkout-main"><header className="commerce-heading"><span className="eyebrow">Thanh toán không cần tài khoản</span><h1>Thông tin nhận hàng</h1><p>GreenFarm chỉ dùng thông tin này để xử lý và xác minh đơn hàng; không tự động tạo tài khoản.</p></header>
    <div className="checkout-layout"><div className="checkout-content">
      <section className="guest-checkout-form"><h2><UserRound size={21}/>Thông tin khách hàng</h2><div className="two-column-fields"><label>Họ và tên<input value={form.name} onChange={e=>setForm({...form,name:e.target.value})} maxLength={100}/></label><label>Số điện thoại<input value={form.phone} onChange={e=>setForm({...form,phone:e.target.value})} maxLength={15}/></label></div><label>Email nhận hóa đơn<input type="email" value={form.email} onChange={e=>setForm({...form,email:e.target.value})} maxLength={255}/></label><label>Địa chỉ giao hàng<textarea value={form.shippingAddress} onChange={e=>setForm({...form,shippingAddress:e.target.value})} maxLength={255}/></label><div className="two-column-fields"><label>Tỉnh/thành phố<input value={form.shippingCity} onChange={e=>setForm({...form,shippingCity:e.target.value})} maxLength={100}/></label><label>Phương thức giao hàng<select value={form.shippingMethod} onChange={e=>{setForm({...form,shippingMethod:e.target.value as "standard"|"express"});setPreview(null);}}><option value="standard">Tiêu chuẩn</option><option value="express">Hỏa tốc</option></select></label></div></section>
      <section className="coupon-panel"><div><span><strong>Voucher sản phẩm</strong><small>Chọn mã giảm trên đơn hoặc nhập mã của bạn.</small></span></div><CouponCodeInput type="ORDER_DISCOUNT" value={coupon} onChange={code=>{setCoupon(code);setPreview(null);}} coupons={coupons} loading={couponsLoading} error={couponsError} placeholder="GREEN10"/></section>
      <section className="coupon-panel"><div><span><strong>Voucher vận chuyển</strong><small>Mỗi đơn chỉ áp dụng một mã freeship.</small></span></div><CouponCodeInput type="FREESHIP" value={shippingCoupon} onChange={code=>{setShippingCoupon(code);setPreview(null);}} coupons={coupons} loading={couponsLoading} error={couponsError} placeholder="FREESHIP"/></section>
      <section className="payment-methods"><h2>Phương thức thanh toán</h2><label className={`payment-method${paymentMethod==="cod"?" selected":""}`}><input type="radio" checked={paymentMethod==="cod"} onChange={()=>setPaymentMethod("cod")}/><Banknote size={22}/><span><strong>Thanh toán khi nhận hàng</strong><small>Thanh toán cho nhân viên giao hàng.</small></span></label><label className={`payment-method${paymentMethod==="vnpay"?" selected":""}${!options.vnpayAvailable?" disabled":""}`}><input type="radio" checked={paymentMethod==="vnpay"} disabled={!options.vnpayAvailable} onChange={()=>setPaymentMethod("vnpay")}/><CreditCard size={22}/><span><strong>VNPAY</strong><small>{options.vnpayAvailable?"Thanh toán qua VNPAY sandbox.":"VNPAY chưa được cấu hình."}</small></span></label></section>
    </div><aside className="checkout-summary"><h2>Đơn hàng khách</h2><div className="checkout-products">{commerce.cart.items.map(item=><div key={item.id}><span>{item.product.name}<small>× {item.quantity}</small></span><strong>{formatPrice(item.lineTotal)}</strong></div>)}</div><div><span>Tạm tính</span><strong>{formatPrice(preview?.subtotal??commerce.cart.subtotal)}</strong></div>{preview&&preview.discountAmount>0&&<div className="discount-row"><span>Giảm voucher</span><strong>−{formatPrice(preview.discountAmount)}</strong></div>}<div><span>Phí giao hàng</span><strong>{formatPrice(preview?.shippingFee??(form.shippingMethod==="express"?50000:30000))}</strong></div>{preview&&preview.shippingDiscountAmount>0&&<div className="discount-row"><span>Giảm vận chuyển</span><strong>−{formatPrice(preview.shippingDiscountAmount)}</strong></div>}<div className="checkout-total"><span>Tổng cộng</span><strong>{formatPrice(preview?.total??commerce.cart.subtotal+(form.shippingMethod==="express"?50000:30000))}</strong></div><button className="secondary-button" disabled={busy} onClick={()=>void calculate()}>Tính lại ưu đãi</button><button className="primary-button" disabled={busy||!preview} onClick={()=>void place()}>{busy?"Đang xử lý...":"Đặt hàng"}</button><small>Hãy tính lại đơn sau khi thay đổi thông tin hoặc voucher.</small></aside></div></main>;
}
