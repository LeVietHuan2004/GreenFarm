"use client";

import { Check, MapPin, Pencil, Plus, Star, Trash2, X } from "lucide-react";
import { useEffect, useState } from "react";
import { toast } from "sonner";
import { getApiErrorMessage } from "@/lib/api-error";
import { addressWithWard, addressWithoutTrailingWard } from "@/lib/address-location";
import { addressService } from "@/services/order-service";
import { ghnService } from "@/services/ghn-service";
import type { GhnLocation, ShippingAddress, ShippingAddressInput } from "@/types/order";

const emptyForm:ShippingAddressInput={fullName:"",phone:"",address:"",city:"",defaultAddress:false};

export function AddressBook({selectable=false,selectedId,onSelect,onChange}:{selectable?:boolean;selectedId?:number|null;onSelect?:(id:number)=>void;onChange?:(items:ShippingAddress[])=>void}){
  const [items,setItems]=useState<ShippingAddress[]>([]); const [loading,setLoading]=useState(true);
  const [editing,setEditing]=useState<number|null>(null); const [showForm,setShowForm]=useState(false);
  const [form,setForm]=useState<ShippingAddressInput>(emptyForm); const [busy,setBusy]=useState(false);
  const [ghnAvailable,setGhnAvailable]=useState(false);
  const [provinces,setProvinces]=useState<GhnLocation[]>([]);
  const [wards,setWards]=useState<GhnLocation[]>([]);
  const publish=(next:ShippingAddress[])=>{setItems(next);onChange?.(next);};
  const load=()=>addressService.findAll().then(next=>{publish(next);if(selectable&&!selectedId&&next.length)onSelect?.((next.find(item=>item.defaultAddress)??next[0]).id);}).catch(error=>toast.error(getApiErrorMessage(error))).finally(()=>setLoading(false));
  useEffect(()=>{void load();},[]); // eslint-disable-line react-hooks/exhaustive-deps
  useEffect(()=>{let active=true;void ghnService.status().then(result=>{
    if(!active || !result.available)return;
    setGhnAvailable(true);
    void ghnService.provinces().then(items=>{if(active)setProvinces(items);}).catch(()=>undefined);
  }).catch(()=>undefined);return()=>{active=false;};},[]);
  useEffect(()=>{if(!ghnAvailable || !form.ghnProvinceId)return;
    const provinceId=form.ghnProvinceId;
    let active=true;void ghnService.wards(provinceId).then(items=>{if(active){setWards(items);setForm(current=>{
      if(!editing||current.ghnProvinceId!==provinceId||current.ghnWardId)return current;
      const parts=current.address.split(",").map(part=>part.trim().toLocaleLowerCase("vi-VN"));
      const matches=items.filter(item=>parts.includes(item.name.trim().toLocaleLowerCase("vi-VN")));
      return matches.length===1?{...current,ghnWardId:matches[0].id}:current;
    });}}).catch(()=>{if(active)setWards([]);});
    return()=>{active=false;};},[ghnAvailable,form.ghnProvinceId,editing]);
  const openCreate=()=>{setEditing(null);setWards([]);setForm(emptyForm);setShowForm(true);};
  const openEdit=(item:ShippingAddress)=>{const matches=provinces.filter(p=>p.name.trim().toLocaleLowerCase("vi-VN")===item.city.trim().toLocaleLowerCase("vi-VN"));setEditing(item.id);setWards([]);setForm({fullName:item.fullName,phone:item.phone,address:addressWithoutTrailingWard(item.address,item.ghnWardName),city:item.city,defaultAddress:item.defaultAddress,ghnProvinceId:item.ghnProvinceId??(matches.length===1?matches[0].id:null),ghnWardId:item.ghnWardId});setShowForm(true);};
  const save=async(event:React.FormEvent)=>{event.preventDefault();if((ghnAvailable&&provinces.length>0&&(!form.ghnProvinceId||!form.ghnWardId))||(form.ghnProvinceId&&!form.ghnWardId)){toast.error("Hãy chọn tỉnh/thành và phường/xã");return;}const ward=wards.find(item=>item.id===form.ghnWardId)?.name??items.find(item=>item.id===editing)?.ghnWardName;const address=addressWithWard(form.address,ward);if(address.length>255){toast.error("Địa chỉ đầy đủ không được vượt quá 255 ký tự");return;}setBusy(true);try{await(editing?addressService.update(editing,{...form,address}):addressService.create({...form,address}));toast.success(editing?"Đã cập nhật địa chỉ":"Đã thêm địa chỉ");setShowForm(false);await load();if(selectable&&editing&&selectedId===editing)onSelect?.(editing);}catch(error){toast.error(getApiErrorMessage(error));}finally{setBusy(false);}};
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
      <label className="address-wide">Địa chỉ cụ thể<input required maxLength={255} placeholder={ghnAvailable&&provinces.length>0?"Số nhà, tên đường":"Số nhà, đường, phường/xã"} value={form.address} onChange={e=>setForm({...form,address:e.target.value})}/></label>
      {ghnAvailable&&provinces.length>0?<>
        <label>Tỉnh/thành phố<select required value={form.ghnProvinceId??""} onChange={e=>{const id=Number(e.target.value)||null;const province=provinces.find(item=>item.id===id);setWards([]);setForm({...form,ghnProvinceId:id,ghnWardId:null,city:province?.name??""});}}><option value="">Chọn tỉnh/thành</option>{provinces.map(item=><option key={item.id} value={item.id}>{item.name}</option>)}</select></label>
        <label>Phường/xã<select required value={form.ghnWardId??""} disabled={!form.ghnProvinceId} onChange={e=>setForm({...form,ghnWardId:Number(e.target.value)||null})}><option value="">Chọn phường/xã</option>{wards.map(item=><option key={item.id} value={item.id}>{item.name}</option>)}</select></label>
      </>:<label>Tỉnh/thành phố<input required maxLength={100} value={form.city} onChange={e=>setForm({...form,city:e.target.value})}/></label>}
      <label className="address-checkbox"><input type="checkbox" checked={form.defaultAddress} onChange={e=>setForm({...form,defaultAddress:e.target.checked})}/>Đặt làm địa chỉ mặc định</label>
      <button className="primary-button" disabled={busy}>{busy?"Đang lưu...":"Lưu địa chỉ"}</button>
    </form>}
  </section>;
}
