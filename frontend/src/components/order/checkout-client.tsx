"use client";

import { ArrowLeft, BadgePercent, Banknote, CheckCircle2, CreditCard, LockKeyhole, PackageCheck } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { toast } from "sonner";
import { AddressBook } from "@/components/order/address-book";
import { useCommerce } from "@/components/commerce/commerce-provider";
import { getApiErrorMessage } from "@/lib/api-error";
import { formatPrice } from "@/lib/catalog-format";
import { orderService, paymentService } from "@/services/order-service";
import { useAuthStore } from "@/stores/auth-store";
import type { CheckoutPreview, PaymentMethod, PaymentMethodOptions, ShippingAddress } from "@/types/order";

export function CheckoutClient(){
  const router=useRouter(); const commerce=useCommerce(); const {hasHydrated,user}=useAuthStore();
  const [selected,setSelected]=useState<number|null>(null); const [addresses,setAddresses]=useState<ShippingAddress[]>([]);
  const [coupon,setCoupon]=useState(""); const [appliedCoupon,setAppliedCoupon]=useState("");
  const [preview,setPreview]=useState<CheckoutPreview|null>(null); const [loading,setLoading]=useState(false); const [placing,setPlacing]=useState(false);
  const [paymentMethod,setPaymentMethod]=useState<PaymentMethod>("cod"); const [paymentOptions,setPaymentOptions]=useState<PaymentMethodOptions>({codAvailable:true,vnpayAvailable:false});
  useEffect(()=>{if(commerce.enabled)paymentService.methods().then(setPaymentOptions).catch(()=>setPaymentOptions({codAvailable:true,vnpayAvailable:false}));},[commerce.enabled]);
  const getPreview=async(addressId:number,code:string)=>{setLoading(true);try{const result=await orderService.preview({shippingAddressId:addressId,couponCode:code||undefined});setPreview(result);setAppliedCoupon(code);if(code)toast.success(result.discountDescription??"Đã áp dụng mã");return true;}catch(error){toast.error(getApiErrorMessage(error));if(code){setAppliedCoupon("");setPreview(null);}return false;}finally{setLoading(false);}};
  const placeOrder=async()=>{if(!selected)return toast.error("Vui lòng chọn địa chỉ giao hàng");setPlacing(true);try{const order=await orderService.create({shippingAddressId:selected,couponCode:appliedCoupon||undefined,paymentMethod});await commerce.refresh();if(order.payment?.method==="vnpay"&&order.payment.paymentUrl){window.location.assign(order.payment.paymentUrl);return;}toast.success("Đặt hàng thành công");router.replace(`/orders/${order.id}`);}catch(error){toast.error(getApiErrorMessage(error));}finally{setPlacing(false);}};
  if(!hasHydrated||commerce.loading)return <main className="catalog-main"><p className="commerce-state">Đang chuẩn bị thanh toán...</p></main>;
  if(!commerce.enabled)return <main className="catalog-main"><div className="catalog-empty"><LockKeyhole size={38}/><h1>Đăng nhập để thanh toán</h1><Link className="primary-button" href="/login">Đăng nhập khách hàng</Link></div></main>;
  if(commerce.cart.items.length===0)return <main className="catalog-main"><div className="catalog-empty"><PackageCheck size={42}/><h1>Giỏ hàng đang trống</h1><p>Thêm sản phẩm trước khi thanh toán.</p><Link className="primary-button" href="/products">Chọn sản phẩm</Link></div></main>;
  return <main className="catalog-main checkout-main"><Link className="commerce-back" href="/cart"><ArrowLeft size={16}/>Quay lại giỏ hàng</Link><header className="commerce-heading"><span className="eyebrow">Thanh toán an toàn</span><h1>Hoàn tất đơn hàng</h1><p>Kiểm tra địa chỉ và tổng tiền trước khi đặt hàng.</p></header>
    <div className="checkout-layout"><div className="checkout-content"><AddressBook selectable selectedId={selected} onSelect={id=>{setSelected(id);void getPreview(id,appliedCoupon);}} onChange={setAddresses}/>
      <section className="coupon-panel"><div><BadgePercent size={22}/><span><strong>Mã giảm giá</strong><small>Nhập mã coupon nếu bạn có</small></span></div><form onSubmit={async e=>{e.preventDefault();if(selected)await getPreview(selected,coupon.trim());else toast.error("Hãy chọn địa chỉ trước");}}><input value={coupon} onChange={e=>setCoupon(e.target.value.toUpperCase())} placeholder="Nhập mã" maxLength={255}/><button disabled={loading||!selected}>{loading?"Đang tính...":"Áp dụng"}</button></form>{appliedCoupon&&<button type="button" className="coupon-remove" onClick={()=>{setCoupon("");setAppliedCoupon("");if(selected)void getPreview(selected,"");}}>Bỏ mã {appliedCoupon}</button>}</section>
      <section className="payment-methods"><h2>Phương thức thanh toán</h2><label className={`payment-method${paymentMethod==="cod"?" selected":""}`}><input type="radio" name="paymentMethod" checked={paymentMethod==="cod"} onChange={()=>setPaymentMethod("cod")}/><Banknote size={22}/><span><strong>Thanh toán khi nhận hàng</strong><small>Thanh toán bằng tiền mặt cho nhân viên giao hàng.</small></span></label><label className={`payment-method${paymentMethod==="vnpay"?" selected":""}${!paymentOptions.vnpayAvailable?" disabled":""}`}><input type="radio" name="paymentMethod" checked={paymentMethod==="vnpay"} disabled={!paymentOptions.vnpayAvailable} onChange={()=>setPaymentMethod("vnpay")}/><CreditCard size={22}/><span><strong>VNPAY</strong><small>{paymentOptions.vnpayAvailable?"Chuyển đến cổng VNPAY sandbox để thanh toán an toàn.":"VNPAY sandbox đang chờ cấu hình merchant."}</small></span></label></section>
    </div><aside className="checkout-summary"><h2>Đơn hàng</h2><div className="checkout-products">{commerce.cart.items.map(item=><div key={item.id}><span>{item.product.name}<small>× {item.quantity}</small></span><strong>{formatPrice(item.lineTotal)}</strong></div>)}</div><div><span>Tạm tính</span><strong>{formatPrice(preview?.subtotal??commerce.cart.subtotal)}</strong></div><div><span>Phí giao hàng</span><strong>{preview?.shippingFee===0?"Miễn phí":formatPrice(preview?.shippingFee??30000)}</strong></div>{(preview?.discountAmount??0)>0&&<div className="discount-row"><span>Giảm giá</span><strong>−{formatPrice(preview!.discountAmount)}</strong></div>}<div className="checkout-total"><span>Tổng cộng</span><strong>{formatPrice(preview?.total??commerce.cart.subtotal+30000)}</strong></div><button type="button" className="primary-button" disabled={placing||loading||!selected||addresses.length===0} onClick={()=>void placeOrder()}>{placing?"Đang tạo đơn...":<><CheckCircle2 size={18}/>Đặt hàng</>}</button><p><LockKeyhole size={14}/>Tồn kho và giá sẽ được kiểm tra lại khi đặt hàng.</p></aside></div>
  </main>;
}
