"use client";

import { Award, Coins, KeyRound, LogOut, MapPin, Package, Phone, RotateCcw, Save, ShieldCheck, ShoppingBag, UserRound } from "lucide-react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { useForm, useWatch } from "react-hook-form";
import { CatalogImage } from "@/components/catalog/catalog-image";
import { LoyaltyPointsClient } from "@/components/loyalty/loyalty-points-client";
import { AddressBook } from "@/components/order/address-book";
import { OrdersClient } from "@/components/order/orders-client";
import { getRoleHome, getRoleLogin } from "@/config/login-portals";
import { getApiErrorMessage } from "@/lib/api-error";
import { authService } from "@/services/auth-service";
import { getLoyaltySummary } from "@/services/loyalty-service";
import { useAuthStore } from "@/stores/auth-store";
import type { LoyaltySummary } from "@/types/loyalty";

type ProfileValues = { name: string; phoneNumber: string; address: string; avatar: string };
type PasswordValues = { currentPassword: string; newPassword: string; confirmPassword: string };
type Feedback = { type: "success" | "error"; message: string } | null;
type ProfileTab = "personal" | "addresses" | "security" | "orders" | "points";

const roleLabels = { customer: "Khách hàng", staff: "Nhân viên", delivery_staff: "Giao hàng", admin: "Quản trị viên" };

export function ProfileClient() {
  const router = useRouter();
  const { token, refreshToken, user, hasHydrated, setUser, clearSession } = useAuthStore();
  const [loading, setLoading] = useState(true);
  const [loyalty, setLoyalty] = useState<LoyaltySummary | null>(null);
  const [profileFeedback, setProfileFeedback] = useState<Feedback>(null);
  const [passwordFeedback, setPasswordFeedback] = useState<Feedback>(null);
  const [uploadingAvatar, setUploadingAvatar] = useState(false);
  const [activeTab, setActiveTab] = useState<ProfileTab>("personal");
  const profileForm = useForm<ProfileValues>();
  const passwordForm = useForm<PasswordValues>();
  const avatarValue = useWatch({ control: profileForm.control, name: "avatar" });

  const fillProfile = (profile: NonNullable<typeof user>) => profileForm.reset({
    name: profile.name, phoneNumber: profile.phoneNumber ?? "", address: profile.address ?? "", avatar: profile.avatar ?? "",
  });

  useEffect(() => {
    if (!hasHydrated) return;
    if (!token) { router.replace(getRoleLogin(user?.role)); return; }
    if (user?.role === "admin") { router.replace("/admin/profile"); return; }
    let active = true;
    authService.getProfile().then((freshUser) => {
      if (!active) return;
      setUser(freshUser); fillProfile(freshUser);
      if (freshUser.role === "customer") getLoyaltySummary().then((summary) => active && setLoyalty(summary)).catch(() => undefined);
    }).catch(() => active && router.replace(getRoleLogin(user?.role))).finally(() => active && setLoading(false));
    return () => { active = false; };
    // React Hook Form reset is stable; the current role is only used for the login redirect.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [hasHydrated, token, user?.role, router, setUser]);

  const updateProfile = profileForm.handleSubmit(async (values) => {
    setProfileFeedback(null);
    try {
      const updated = await authService.updateProfile({ name: values.name.trim(), phoneNumber: values.phoneNumber.trim() || undefined, address: values.address.trim(), avatar: values.avatar.trim() });
      setUser(updated); fillProfile(updated);
      setProfileFeedback({ type: "success", message: "Thông tin hồ sơ đã được cập nhật." });
    } catch (error) { setProfileFeedback({ type: "error", message: getApiErrorMessage(error) }); }
  });

  const changePassword = passwordForm.handleSubmit(async (values) => {
    setPasswordFeedback(null);
    try {
      await authService.changePassword({ currentPassword: values.currentPassword, newPassword: values.newPassword });
      passwordForm.reset(); clearSession(); router.replace(getRoleLogin(user?.role));
    } catch (error) { setPasswordFeedback({ type: "error", message: getApiErrorMessage(error) }); }
  });

  const uploadAvatar = async (file?: File) => {
    if (!file) return;
    setUploadingAvatar(true); setProfileFeedback(null);
    try {
      const updated = await authService.uploadAvatar(file);
      setUser(updated); fillProfile(updated);
      setProfileFeedback({ type: "success", message: "Ảnh đại diện đã được cập nhật." });
    } catch (error) { setProfileFeedback({ type: "error", message: getApiErrorMessage(error) }); }
    finally { setUploadingAvatar(false); }
  };

  const logout = () => { const href = getRoleLogin(user?.role); if (refreshToken) void authService.logout(refreshToken).catch(() => undefined); clearSession(); router.push(href); };
  if (!hasHydrated || loading || !user) return <main className="profile-page"><div className="profile-loading">Đang tải hồ sơ...</div></main>;

  return <main className="profile-page profile-page-redesign"><div className="profile-shell profile-shell-wide">
    <header className="profile-topbar"><div><span className="eyebrow">Tài khoản GreenFarm</span><h1>Hồ sơ khách hàng</h1><p>Quản lý thông tin cá nhân, điểm thưởng và bảo mật tài khoản.</p></div><div><Link className="secondary-button" href={getRoleHome(user.role)}><ShoppingBag size={16}/>Mua hàng</Link><button type="button" className="profile-logout" onClick={logout}><LogOut size={16}/>Đăng xuất</button></div></header>

    <div className="profile-workspace">
      <aside className="profile-sidebar">
        <div className="profile-sidebar-user"><div className="profile-side-avatar">{user.avatar ? <CatalogImage src={user.avatar} alt={user.name}/> : <span>{user.name.slice(0, 1).toUpperCase()}</span>}</div><div><strong>{user.name}</strong><small><ShieldCheck size={12}/>Hồ sơ đã đăng nhập</small></div></div>
        <p>Tài khoản của tôi</p>
        <button type="button" className={activeTab === "personal" ? "active" : ""} onClick={() => setActiveTab("personal")}><UserRound size={16}/>Hồ sơ cá nhân</button>
        {user.role === "customer" && <button type="button" className={activeTab === "addresses" ? "active" : ""} onClick={() => setActiveTab("addresses")}><MapPin size={16}/>Địa chỉ nhận hàng</button>}
        <button type="button" className={activeTab === "security" ? "active" : ""} onClick={() => setActiveTab("security")}><KeyRound size={16}/>Đổi mật khẩu</button>
        {user.role === "customer" && <><p>Quản lý giao dịch</p><button type="button" className={activeTab === "orders" ? "active" : ""} onClick={() => setActiveTab("orders")}><Package size={16}/>Đơn mua của tôi</button><button type="button" className={activeTab === "points" ? "active" : ""} onClick={() => setActiveTab("points")}><Coins size={16}/>Điểm GreenFarm</button></>}
      </aside>

      <div className="profile-content">
        {activeTab === "personal" && <section className="profile-main-card" id="personal">
          <header><div><h2>Hồ sơ cá nhân</h2><p>Thông tin hồ sơ để bảo mật tài khoản và hỗ trợ giao hàng.</p></div><span className="role-label"><ShieldCheck size={14}/>{roleLabels[user.role]}</span></header>
          <div className="profile-stat-grid">
            <article className="points"><span><Coins size={20}/></span><small>Điểm tích lũy</small><strong>{(loyalty?.pointsBalance ?? 0).toLocaleString("vi-VN")} điểm</strong><p>{loyalty ? `1 điểm đổi ${loyalty.discountPerPoint.toLocaleString("vi-VN")}₫ khi thanh toán` : "Điểm thưởng từ đơn hàng và đánh giá"}</p></article>
            <article className="member"><span><Award size={20}/></span><small>Hạng thành viên</small><strong>Tiêu chuẩn</strong><p>Đồng hành cùng nông sản xanh GreenFarm</p></article>
            <article className="phone"><span><Phone size={20}/></span><small>Số điện thoại</small><strong>{user.phoneNumber || "Chưa cập nhật"}</strong><p>Dùng để liên hệ khi giao nhận</p></article>
          </div>

          <form className="profile-info-form" onSubmit={updateProfile} noValidate>
            <div className="profile-form-fields">
              <div className="field-group"><label htmlFor="profile-name">Họ và tên</label><input id="profile-name" {...profileForm.register("name", { required: "Vui lòng nhập họ tên.", minLength: { value: 2, message: "Họ tên cần ít nhất 2 ký tự." } })}/>{profileForm.formState.errors.name && <p className="field-error">{profileForm.formState.errors.name.message}</p>}</div>
              <div className="field-group"><label>Địa chỉ email</label><input value={user.email} disabled/><small>Email đăng nhập không thể thay đổi tại đây.</small></div>
              <div className="field-group"><label htmlFor="profile-phone">Số điện thoại</label><input id="profile-phone" type="tel" placeholder="0901234567" {...profileForm.register("phoneNumber", { pattern: { value: /^(?:\+84|0)[0-9]{9,10}$|^$/, message: "Số điện thoại chưa đúng định dạng." } })}/>{profileForm.formState.errors.phoneNumber && <p className="field-error">{profileForm.formState.errors.phoneNumber.message}</p>}</div>
              <div className="field-group"><label htmlFor="profile-address">Địa chỉ liên hệ</label><textarea id="profile-address" rows={3} placeholder="Địa chỉ liên hệ" {...profileForm.register("address", { maxLength: { value: 500, message: "Địa chỉ không vượt quá 500 ký tự." } })}/>{profileForm.formState.errors.address && <p className="field-error">{profileForm.formState.errors.address.message}</p>}</div>
            </div>
            <aside className="profile-avatar-editor"><label>Ảnh đại diện</label><div className="profile-avatar-preview">{avatarValue ? <CatalogImage src={avatarValue} alt="Ảnh đại diện"/> : <UserRound size={38}/>}</div><label className="secondary-button compact-button">{uploadingAvatar ? "Đang tải..." : "Chọn ảnh"}<input type="file" accept="image/jpeg,image/png,image/webp" hidden disabled={uploadingAvatar} onChange={(event) => void uploadAvatar(event.target.files?.[0])}/></label>{avatarValue && <button type="button" onClick={() => profileForm.setValue("avatar", "", { shouldDirty: true })}>Xóa ảnh</button>}<small>JPG, PNG hoặc WEBP, tối đa 5 MB.</small></aside>
            <footer>{profileFeedback && <p className={`form-message ${profileFeedback.type}`}>{profileFeedback.message}</p>}<div><button className="primary-button" disabled={profileForm.formState.isSubmitting}><Save size={16}/>{profileForm.formState.isSubmitting ? "Đang lưu..." : "Lưu thay đổi"}</button><button type="button" className="secondary-button" onClick={() => fillProfile(user)}><RotateCcw size={15}/>Tải lại</button></div></footer>
          </form>
        </section>}

        {activeTab === "addresses" && <section className="profile-main-card profile-tab-card"><header><div><h2>Địa chỉ nhận hàng</h2><p>Thêm, chỉnh sửa và chọn địa chỉ mặc định khi thanh toán.</p></div><MapPin size={20}/></header><AddressBook/></section>}
        {activeTab === "security" && <section className="profile-main-card profile-security" id="security"><header><div><h2>Bảo mật tài khoản</h2><p>Đổi mật khẩu định kỳ để bảo vệ tài khoản của bạn.</p></div><KeyRound size={20}/></header><form onSubmit={changePassword} noValidate><div className="field-group"><label>Mật khẩu hiện tại</label><input type="password" autoComplete="current-password" {...passwordForm.register("currentPassword", { required: "Vui lòng nhập mật khẩu hiện tại." })}/>{passwordForm.formState.errors.currentPassword && <p className="field-error">{passwordForm.formState.errors.currentPassword.message}</p>}</div><div className="field-group"><label>Mật khẩu mới</label><input type="password" autoComplete="new-password" {...passwordForm.register("newPassword", { required: "Vui lòng nhập mật khẩu mới.", minLength: { value: 8, message: "Mật khẩu cần ít nhất 8 ký tự." }, maxLength: { value: 72, message: "Mật khẩu không vượt quá 72 ký tự." } })}/>{passwordForm.formState.errors.newPassword && <p className="field-error">{passwordForm.formState.errors.newPassword.message}</p>}</div><div className="field-group"><label>Xác nhận mật khẩu</label><input type="password" autoComplete="new-password" {...passwordForm.register("confirmPassword", { required: "Vui lòng xác nhận mật khẩu.", validate: (value) => value === passwordForm.getValues("newPassword") || "Hai mật khẩu chưa trùng nhau." })}/>{passwordForm.formState.errors.confirmPassword && <p className="field-error">{passwordForm.formState.errors.confirmPassword.message}</p>}</div><button className="secondary-button" disabled={passwordForm.formState.isSubmitting}><KeyRound size={16}/>{passwordForm.formState.isSubmitting ? "Đang đổi..." : "Đổi mật khẩu"}</button>{passwordFeedback && <p className={`form-message ${passwordFeedback.type}`}>{passwordFeedback.message}</p>}</form></section>}
        {activeTab === "orders" && <OrdersClient embedded/>}
        {activeTab === "points" && <LoyaltyPointsClient embedded/>}
      </div>
    </div>
  </div></main>;
}
