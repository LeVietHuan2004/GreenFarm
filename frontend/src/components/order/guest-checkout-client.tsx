"use client";

import { Banknote, CreditCard, PackageCheck, Truck, UserRound } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { toast } from "sonner";
import { useCommerce } from "@/components/commerce/commerce-provider";
import { VoucherPicker, type VoucherSelection } from "@/components/order/voucher-picker";
import { getApiErrorMessage } from "@/lib/api-error";
import { formatPrice } from "@/lib/catalog-format";
import { clearGuestSession, getGuestCheckoutAttempt, getGuestSession, guestCheckoutKey, saveGuestOrder } from "@/lib/guest-session";
import { guestCommerceService } from "@/services/guest-commerce-service";
import { ghnService } from "@/services/ghn-service";
import type { CheckoutPreview, GhnLocation, GuestCheckoutInput, PaymentMethod, PaymentMethodOptions, ShippingProvider } from "@/types/order";
import type { AvailableCoupon } from "@/types/coupon";

export function GuestCheckoutClient({coupons,couponsLoading,couponsError,ghnAvailable}:{coupons:AvailableCoupon[];couponsLoading:boolean;couponsError:boolean;ghnAvailable:boolean}){
  const router=useRouter(); const commerce=useCommerce();
  const [form,setForm]=useState({name:"",phone:"",email:"",shippingAddress:"",shippingCity:"",shippingMethod:"standard" as "standard"|"express"});
  const [voucher,setVoucher]=useState<VoucherSelection|null>(null);
  const [paymentMethod,setPaymentMethod]=useState<PaymentMethod>("cod"); const [preview,setPreview]=useState<CheckoutPreview|null>(null);
  const [options,setOptions]=useState<PaymentMethodOptions>({codAvailable:true,vnpayAvailable:false}); const [busy,setBusy]=useState(false);
  const [shippingProvider,setShippingProvider]=useState<ShippingProvider>("internal");
  const [provinces,setProvinces]=useState<GhnLocation[]>([]); const [wards,setWards]=useState<GhnLocation[]>([]);
  const [ghnProvinceId,setGhnProvinceId]=useState<number|null>(null); const [ghnWardId,setGhnWardId]=useState<number|null>(null);
  useEffect(()=>{guestCommerceService.paymentMethods().then(setOptions).catch(()=>undefined);},[]);
  useEffect(()=>{if(!ghnAvailable)return;let active=true;void ghnService.provinces().then(items=>{if(active)setProvinces(items);}).catch(()=>undefined);return()=>{active=false;};},[ghnAvailable]);
  useEffect(()=>{if(!ghnProvinceId)return;let active=true;void ghnService.wards(ghnProvinceId).then(items=>{if(active)setWards(items);}).catch(()=>undefined);return()=>{active=false;};},[ghnProvinceId]);
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
  const input=async():Promise<GuestCheckoutInput>=>({...form,couponCode:voucher?.type==="ORDER_DISCOUNT"?voucher.code:undefined,freeShippingCouponCode:voucher?.type==="FREESHIP"?voucher.code:undefined,paymentMethod,idempotencyKey:guestCheckoutKey(await guestCommerceService.token(),form.email),shippingProvider,ghnProvinceId,ghnWardId,expectedTotal:shippingProvider==="ghn"?preview?.total:undefined});
  const validate=()=>{if(form.name.trim().length<2||!/^(?:\+84|0)[0-9]{9,10}$/.test(form.phone.replaceAll(" ",""))||!/^\S+@\S+\.\S+$/.test(form.email)||!form.shippingAddress.trim()||!form.shippingCity.trim()){toast.error("Vui lòng nhập đầy đủ và đúng thông tin nhận hàng");return false;}if(shippingProvider==="ghn"&&(!ghnProvinceId||!ghnWardId)){toast.error("Hãy chọn tỉnh và phường/xã GHN");return false;}return true;};
  const calculate=async()=>{if(!validate())return;setPreview(null);setBusy(true);try{setPreview(await guestCommerceService.preview(await input()));toast.success("Đã tính lại đơn hàng");}catch(error){toast.error(getApiErrorMessage(error));}finally{setBusy(false);}};
  const place=async()=>{if(!validate()||!preview)return;setBusy(true);try{
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
      <section className="guest-checkout-form"><h2><UserRound size={21}/>Thông tin khách hàng</h2><div className="two-column-fields"><label>Họ và tên<input value={form.name} onChange={e=>{setForm({...form,name:e.target.value});setPreview(null);}} maxLength={100}/></label><label>Số điện thoại<input value={form.phone} onChange={e=>{setForm({...form,phone:e.target.value});setPreview(null);}} maxLength={15}/></label></div><label>Email nhận hóa đơn<input type="email" value={form.email} onChange={e=>{setForm({...form,email:e.target.value});setPreview(null);}} maxLength={255}/></label><label>Địa chỉ giao hàng<textarea value={form.shippingAddress} onChange={e=>{setForm({...form,shippingAddress:e.target.value});setPreview(null);}} maxLength={255}/></label><div className="two-column-fields"><label>Tỉnh/thành phố<input value={form.shippingCity} onChange={e=>{setForm({...form,shippingCity:e.target.value});setPreview(null);}} maxLength={100}/></label><label>Phương thức giao hàng<select value={form.shippingMethod} onChange={e=>{setForm({...form,shippingMethod:e.target.value as "standard"|"express"});setPreview(null);}}><option value="standard">Tiêu chuẩn</option><option value="express">Hỏa tốc</option></select></label></div></section>
      <section className="payment-methods"><h2><Truck size={20}/> Đơn vị vận chuyển</h2><label className={`payment-method${shippingProvider==="internal"?" selected":""}`}><input type="radio" name="guestShippingProvider" checked={shippingProvider==="internal"} onChange={()=>{setShippingProvider("internal");setPreview(null);}}/><Truck size={22}/><span><strong>GreenFarm giao hàng</strong><small>Giữ cách giao hàng hiện tại.</small></span></label>
      {ghnAvailable&&<label className={`payment-method${shippingProvider==="ghn"?" selected":""}${!commerce.cart.items.every(item=>item.product.ghnEligible)?" disabled":""}`}><input type="radio" name="guestShippingProvider" checked={shippingProvider==="ghn"} disabled={!commerce.cart.items.every(item=>item.product.ghnEligible)} onChange={()=>{setShippingProvider("ghn");setPreview(null);}}/><Truck size={22}/><span><strong>Giao Hàng Nhanh</strong><small>{commerce.cart.items.every(item=>item.product.ghnEligible)?"Phí tính trực tiếp từ GHN staging.":"Giỏ có sản phẩm chưa đủ điều kiện gửi GHN."}</small></span></label>}
      {shippingProvider==="ghn"&&<div className="two-column-fields ghn-address-fields"><label>Tỉnh/thành GHN<select value={ghnProvinceId??""} onChange={e=>{const id=Number(e.target.value)||null;setGhnProvinceId(id);setGhnWardId(null);setWards([]);setForm({...form,shippingCity:provinces.find(item=>item.id===id)?.name??""});setPreview(null);}}><option value="">Chọn tỉnh/thành</option>{provinces.map(item=><option key={item.id} value={item.id}>{item.name}</option>)}</select></label><label>Phường/xã GHN<select value={ghnWardId??""} disabled={!ghnProvinceId} onChange={e=>{setGhnWardId(Number(e.target.value)||null);setPreview(null);}}><option value="">Chọn phường/xã</option>{wards.map(item=><option key={item.id} value={item.id}>{item.name}</option>)}</select></label></div>}</section>
      <VoucherPicker coupons={coupons} cart={commerce.cart} shippingFee={form.shippingMethod==="express"?50000:commerce.cart.subtotal>=500000?0:30000} loading={couponsLoading} error={couponsError} selected={voucher} onConfirm={chosen => {setVoucher(chosen);setPreview(null);return true;}}/>
      <section className="payment-methods"><h2>Phương thức thanh toán</h2><label className={`payment-method${paymentMethod==="cod"?" selected":""}`}><input type="radio" checked={paymentMethod==="cod"} onChange={()=>{setPaymentMethod("cod");setPreview(null);}}/><Banknote size={22}/><span><strong>Thanh toán khi nhận hàng</strong><small>Thanh toán cho nhân viên giao hàng.</small></span></label><label className={`payment-method${paymentMethod==="vnpay"?" selected":""}${!options.vnpayAvailable?" disabled":""}`}><input type="radio" checked={paymentMethod==="vnpay"} disabled={!options.vnpayAvailable} onChange={()=>{setPaymentMethod("vnpay");setPreview(null);}}/><CreditCard size={22}/><span><strong>VNPAY</strong><small>{options.vnpayAvailable?"Thanh toán qua VNPAY sandbox.":"VNPAY chưa được cấu hình."}</small></span></label></section>
    </div><aside className="checkout-summary"><h2>Đơn hàng khách</h2><div className="checkout-products">{commerce.cart.items.map(item=><div key={item.id}><span>{item.product.name}<small>× {item.quantity}</small></span><strong>{formatPrice(item.lineTotal)}</strong></div>)}</div><div><span>Tạm tính</span><strong>{formatPrice(preview?.subtotal??commerce.cart.subtotal)}</strong></div>{preview&&preview.discountAmount>0&&<div className="discount-row"><span>Giảm voucher</span><strong>−{formatPrice(preview.discountAmount)}</strong></div>}<div><span>Phí giao hàng</span><strong>{preview?formatPrice(preview.shippingFee):shippingProvider==="ghn"?"Chưa tính":formatPrice(form.shippingMethod==="express"?50000:30000)}</strong></div>{preview&&preview.shippingDiscountAmount>0&&<div className="discount-row"><span>Giảm vận chuyển</span><strong>−{formatPrice(preview.shippingDiscountAmount)}</strong></div>}<div className="checkout-total"><span>Tổng cộng</span><strong>{preview?formatPrice(preview.total):shippingProvider==="ghn"?"Chờ tính phí":formatPrice(commerce.cart.subtotal+(form.shippingMethod==="express"?50000:30000))}</strong></div><button className="secondary-button" disabled={busy} onClick={()=>void calculate()}>Tính lại ưu đãi</button><button className="primary-button" disabled={busy||!preview} onClick={()=>void place()}>{busy?"Đang xử lý...":"Đặt hàng"}</button><small>Hãy tính lại đơn sau khi thay đổi thông tin hoặc voucher.</small></aside></div></main>;
}
