"use client";

import { KeyRound, RotateCcw, Save, ShieldCheck, UserRound } from "lucide-react";
import { useEffect, useState } from "react";
import { useForm, useWatch } from "react-hook-form";
import { AdminShell } from "@/components/admin/admin-shell";
import { CatalogImage } from "@/components/catalog/catalog-image";
import { getApiErrorMessage } from "@/lib/api-error";
import { authService } from "@/services/auth-service";
import { useAuthStore } from "@/stores/auth-store";
import type { User } from "@/types/auth";

type ProfileValues = { name:string; phoneNumber:string; address:string; avatar:string };
type PasswordValues = { currentPassword:string; newPassword:string; confirmPassword:string };
type Feedback = { type:"success"|"error"; message:string } | null;

export function AdminProfile() {
  const { user, hasHydrated, setUser } = useAuthStore();
  const [loading, setLoading] = useState(true);
  const [tab, setTab] = useState<"profile"|"security">("profile");
  const [profileFeedback, setProfileFeedback] = useState<Feedback>(null);
  const [passwordFeedback, setPasswordFeedback] = useState<Feedback>(null);
  const [uploadingAvatar, setUploadingAvatar] = useState(false);
  const profileForm = useForm<ProfileValues>();
  const passwordForm = useForm<PasswordValues>();
  const avatar = useWatch({ control: profileForm.control, name: "avatar" });

  const fill = (account:User) => profileForm.reset({ name:account.name, phoneNumber:account.phoneNumber??"", address:account.address??"", avatar:account.avatar??"" });

  useEffect(() => {
    if (!hasHydrated || user?.role !== "admin") return;
    let active = true;
    authService.getProfile().then((account) => { if (!active) return; setUser(account); fill(account); }).catch((error) => active && setProfileFeedback({type:"error",message:getApiErrorMessage(error)})).finally(() => active && setLoading(false));
    return () => { active = false; };
    // The form reset function is stable and this request only follows the authenticated account.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [hasHydrated, user?.id, user?.role, setUser]);

  const updateProfile = profileForm.handleSubmit(async(values) => {
    setProfileFeedback(null);
    try {
      const updated = await authService.updateProfile({ name:values.name.trim(), phoneNumber:values.phoneNumber.trim()||undefined, address:values.address.trim(), avatar:values.avatar.trim() });
      setUser(updated); fill(updated); setProfileFeedback({type:"success",message:"Đã cập nhật hồ sơ quản trị viên."});
    } catch(error) { setProfileFeedback({type:"error",message:getApiErrorMessage(error)}); }
  });

  const changePassword = passwordForm.handleSubmit(async(values) => {
    setPasswordFeedback(null);
    try { await authService.changePassword({currentPassword:values.currentPassword,newPassword:values.newPassword}); passwordForm.reset(); setPasswordFeedback({type:"success",message:"Đã thay đổi mật khẩu."}); }
    catch(error) { setPasswordFeedback({type:"error",message:getApiErrorMessage(error)}); }
  });

  const uploadAvatar = async(file?:File) => {
    if (!file) return;
    setUploadingAvatar(true); setProfileFeedback(null);
    try { const updated=await authService.uploadAvatar(file); setUser(updated); fill(updated); setProfileFeedback({type:"success",message:"Đã cập nhật ảnh đại diện."}); }
    catch(error) { setProfileFeedback({type:"error",message:getApiErrorMessage(error)}); }
    finally { setUploadingAvatar(false); }
  };

  return <AdminShell active="profile"><main className="admin-catalog-main admin-profile-main">
    <header className="admin-page-heading"><div><span className="eyebrow">Tài khoản quản trị</span><h1>Hồ sơ của tôi</h1><p>Cập nhật thông tin hiển thị và bảo mật mà không rời khu vực quản trị.</p></div><span className="admin-count"><ShieldCheck size={15}/>Quản trị viên</span></header>
    {!hasHydrated || loading || !user ? <div className="admin-list-card profile-loading">Đang tải hồ sơ...</div> : <>
      <section className="admin-profile-summary admin-list-card"><div className="admin-profile-avatar">{user.avatar?<CatalogImage src={user.avatar} alt={user.name}/>:<UserRound size={30}/>}</div><div><strong>{user.name}</strong><span>{user.email}</span><small>Tài khoản quản trị đang hoạt động</small></div><nav><button type="button" className={tab==="profile"?"active":""} onClick={()=>setTab("profile")}><UserRound size={16}/>Thông tin cá nhân</button><button type="button" className={tab==="security"?"active":""} onClick={()=>setTab("security")}><KeyRound size={16}/>Đổi mật khẩu</button></nav></section>

      {tab==="profile"&&<section className="admin-list-card admin-profile-card"><header><div><h2>Thông tin cá nhân</h2><p>Thông tin này được sử dụng trong khu vực quản trị GreenFarm.</p></div><ShieldCheck size={20}/></header><form className="admin-profile-form" onSubmit={updateProfile} noValidate><div className="admin-profile-fields"><label>Họ và tên<input {...profileForm.register("name",{required:"Vui lòng nhập họ tên.",minLength:{value:2,message:"Họ tên cần ít nhất 2 ký tự."}})}/>{profileForm.formState.errors.name&&<small className="field-error">{profileForm.formState.errors.name.message}</small>}</label><label>Email đăng nhập<input value={user.email} disabled/><small>Email đăng nhập không thể thay đổi tại đây.</small></label><label>Số điện thoại<input type="tel" placeholder="0901234567" {...profileForm.register("phoneNumber",{pattern:{value:/^(?:\+84|0)[0-9]{9,10}$|^$/,message:"Số điện thoại chưa đúng định dạng."}})}/>{profileForm.formState.errors.phoneNumber&&<small className="field-error">{profileForm.formState.errors.phoneNumber.message}</small>}</label><label>Địa chỉ liên hệ<textarea rows={4} {...profileForm.register("address",{maxLength:{value:500,message:"Địa chỉ không vượt quá 500 ký tự."}})}/>{profileForm.formState.errors.address&&<small className="field-error">{profileForm.formState.errors.address.message}</small>}</label></div><aside className="admin-avatar-editor"><label>Ảnh đại diện</label><div>{avatar?<CatalogImage src={avatar} alt="Ảnh đại diện quản trị viên"/>:<UserRound size={42}/>}</div><label className="secondary-button compact-button">{uploadingAvatar?"Đang tải...":"Chọn ảnh"}<input type="file" accept="image/jpeg,image/png,image/webp" hidden disabled={uploadingAvatar} onChange={(event)=>void uploadAvatar(event.target.files?.[0])}/></label>{avatar&&<button type="button" onClick={()=>profileForm.setValue("avatar","",{shouldDirty:true})}>Xóa ảnh</button>}<small>JPG, PNG hoặc WEBP, tối đa 5 MB.</small></aside><footer>{profileFeedback&&<p className={`form-message ${profileFeedback.type}`}>{profileFeedback.message}</p>}<div><button className="primary-button" disabled={profileForm.formState.isSubmitting}><Save size={16}/>{profileForm.formState.isSubmitting?"Đang lưu...":"Lưu thay đổi"}</button><button type="button" className="secondary-button" onClick={()=>fill(user)}><RotateCcw size={15}/>Tải lại</button></div></footer></form></section>}

      {tab==="security"&&<section className="admin-list-card admin-profile-card"><header><div><h2>Đổi mật khẩu</h2><p>Sử dụng mật khẩu từ 8 đến 72 ký tự và không chia sẻ với người khác.</p></div><KeyRound size={20}/></header><form className="admin-password-form" onSubmit={changePassword} noValidate><label>Mật khẩu hiện tại<input type="password" autoComplete="current-password" {...passwordForm.register("currentPassword",{required:"Vui lòng nhập mật khẩu hiện tại."})}/>{passwordForm.formState.errors.currentPassword&&<small className="field-error">{passwordForm.formState.errors.currentPassword.message}</small>}</label><label>Mật khẩu mới<input type="password" autoComplete="new-password" {...passwordForm.register("newPassword",{required:"Vui lòng nhập mật khẩu mới.",minLength:{value:8,message:"Mật khẩu cần ít nhất 8 ký tự."},maxLength:{value:72,message:"Mật khẩu không vượt quá 72 ký tự."}})}/>{passwordForm.formState.errors.newPassword&&<small className="field-error">{passwordForm.formState.errors.newPassword.message}</small>}</label><label>Xác nhận mật khẩu<input type="password" autoComplete="new-password" {...passwordForm.register("confirmPassword",{required:"Vui lòng xác nhận mật khẩu.",validate:value=>value===passwordForm.getValues("newPassword")||"Hai mật khẩu chưa trùng nhau."})}/>{passwordForm.formState.errors.confirmPassword&&<small className="field-error">{passwordForm.formState.errors.confirmPassword.message}</small>}</label><button className="primary-button" disabled={passwordForm.formState.isSubmitting}><KeyRound size={16}/>{passwordForm.formState.isSubmitting?"Đang đổi...":"Đổi mật khẩu"}</button>{passwordFeedback&&<p className={`form-message ${passwordFeedback.type}`}>{passwordFeedback.message}</p>}</form></section>}
    </>}
  </main></AdminShell>;
}
