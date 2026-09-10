"use client";

import { Check, MapPin, Pencil, Plus, Star, Trash2, X } from "lucide-react";
import { useEffect, useState } from "react";
import { toast } from "sonner";
import { getApiErrorMessage } from "@/lib/api-error";
import { addressService } from "@/services/order-service";
import type { ShippingAddress, ShippingAddressInput } from "@/types/order";

const emptyForm:ShippingAddressInput={fullName:"",phone:"",address:"",city:"",defaultAddress:false};

export function AddressBook({selectable=false,selectedId,onSelect,onChange}:{selectable?:boolean;selectedId?:number|null;onSelect?:(id:number)=>void;onChange?:(items:ShippingAddress[])=>void}){
  const [items,setItems]=useState<ShippingAddress[]>([]); const [loading,setLoading]=useState(true);
  const [editing,setEditing]=useState<number|null>(null); const [showForm,setShowForm]=useState(false);
  const [form,setForm]=useState<ShippingAddressInput>(emptyForm); const [busy,setBusy]=useState(false);
  const publish=(next:ShippingAddress[])=>{setItems(next);onChange?.(next);};
  const load=()=>addressService.findAll().then(next=>{publish(next);if(selectable&&!selectedId&&next.length)onSelect?.((next.find(item=>item.defaultAddress)??next[0]).id);}).catch(error=>toast.error(getApiErrorMessage(error))).finally(()=>setLoading(false));
  useEffect(()=>{void load();},[]); // eslint-disable-line react-hooks/exhaustive-deps
  const openCreate=()=>{setEditing(null);setForm(emptyForm);setShowForm(true);};
  const openEdit=(item:ShippingAddress)=>{setEditing(item.id);setForm({fullName:item.fullName,phone:item.phone,address:item.address,city:item.city,defaultAddress:item.defaultAddress});setShowForm(true);};
  const save=async(event:React.FormEvent)=>{event.preventDefault();setBusy(true);try{await(editing?addressService.update(editing,form):addressService.create(form));toast.success(editing?"Đã cập nhật địa chỉ":"Đã thêm địa chỉ");setShowForm(false);await load();}catch(error){toast.error(getApiErrorMessage(error));}finally{setBusy(false);}};
  const remove=async(id:number)=>{setBusy(true);try{const next=await addressService.remove(id);publish(next);toast.success("Đã xóa địa chỉ");const fallback=next.find(item=>item.defaultAddress)??next[0];if(selectedId===id&&fallback)onSelect?.(fallback.id);}catch(error){toast.error(getApiErrorMessage(error));}finally{setBusy(false);}};
  const makeDefault=async(id:number)=>{setBusy(true);try{await addressService.setDefault(id);await load();toast.success("Đã đặt làm địa chỉ mặc định");}catch(error){toast.error(getApiErrorMessage(error));}finally{setBusy(false);}};
  if(loading)return <p className="commerce-state">Đang tải địa chỉ...</p>;
  return <section className="address-book">
    <div className="address-book-title"><h2>Địa chỉ giao hàng</h2><button type="button" className="secondary-button" onClick={openCreate}><Plus size={16}/>Thêm địa chỉ</button></div>
    {items.length===0&&!showForm&&<div className="address-empty"><MapPin size={28}/><p>Bạn chưa có địa chỉ giao hàng.</p><button type="button" className="primary-button" onClick={openCreate}>Thêm địa chỉ đầu tiên</button></div>}
    <div className="address-grid">{items.map(item=><article key={item.id} className={`address-card${selectedId===item.id?" selected":""}`} onClick={()=>selectable&&onSelect?.(item.id)}>
      <div><strong>{item.fullName}</strong>{item.defaultAddress&&<span className="default-chip"><Star size={11}/>Mặc định</span>}</div><p>{item.phone}</p><p>{item.address}, {item.city}</p>
      {selectable&&<span className="address-select-indicator">{selectedId===item.id?<><Check size={14}/>Đã chọn</>:"Chọn địa chỉ"}</span>}
      <div className="address-actions"><button type="button" onClick={event=>{event.stopPropagation();openEdit(item)}}><Pencil size={14}/>Sửa</button>{!item.defaultAddress&&<button type="button" onClick={event=>{event.stopPropagation();void makeDefault(item.id)}}><Star size={14}/>Mặc định</button>}<button type="button" disabled={busy} onClick={event=>{event.stopPropagation();void remove(item.id)}}><Trash2 size={14}/>Xóa</button></div>
    </article>)}</div>
    {showForm&&<form className="address-form" onSubmit={save}><div className="address-form-heading"><h3>{editing?"Cập nhật địa chỉ":"Địa chỉ mới"}</h3><button type="button" onClick={()=>setShowForm(false)} aria-label="Đóng"><X size={18}/></button></div>
      <label>Họ và tên<input required maxLength={100} value={form.fullName} onChange={e=>setForm({...form,fullName:e.target.value})}/></label>
      <label>Số điện thoại<input required inputMode="tel" pattern="(?:\+84|0)[0-9]{9,10}" value={form.phone} onChange={e=>setForm({...form,phone:e.target.value})}/></label>
      <label className="address-wide">Địa chỉ cụ thể<input required maxLength={255} placeholder="Số nhà, đường, phường/xã" value={form.address} onChange={e=>setForm({...form,address:e.target.value})}/></label>
      <label>Tỉnh/thành phố<input required maxLength={100} value={form.city} onChange={e=>setForm({...form,city:e.target.value})}/></label>
      <label className="address-checkbox"><input type="checkbox" checked={form.defaultAddress} onChange={e=>setForm({...form,defaultAddress:e.target.checked})}/>Đặt làm địa chỉ mặc định</label>
      <button className="primary-button" disabled={busy}>{busy?"Đang lưu...":"Lưu địa chỉ"}</button>
    </form>}
  </section>;
}
