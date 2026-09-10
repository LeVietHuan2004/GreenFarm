"use client";

import { KeyRound, LogOut, Save, ShieldCheck, UserRound } from "lucide-react";
import { useRouter } from "next/navigation";
import { useEffect, useState } from "react";
import { useForm } from "react-hook-form";

import { getApiErrorMessage } from "@/lib/api-error";
import { getRoleLogin } from "@/config/login-portals";
import { authService } from "@/services/auth-service";
import { useAuthStore } from "@/stores/auth-store";

type ProfileValues = {
  name: string;
  phoneNumber: string;
  address: string;
};

type PasswordValues = {
  currentPassword: string;
  newPassword: string;
  confirmPassword: string;
};

type Feedback = { type: "success" | "error"; message: string } | null;

export function ProfileClient() {
  const router = useRouter();
  const { token, user, hasHydrated, setUser, clearSession } = useAuthStore();
  const [loading, setLoading] = useState(true);
  const [profileFeedback, setProfileFeedback] = useState<Feedback>(null);
  const [passwordFeedback, setPasswordFeedback] = useState<Feedback>(null);
  const profileForm = useForm<ProfileValues>();
  const passwordForm = useForm<PasswordValues>();
  const { reset: resetProfileForm } = profileForm;

  useEffect(() => {
    if (!hasHydrated) return;
    if (!token) {
      router.replace(getRoleLogin(user?.role));
      return;
    }

    let active = true;
    authService
      .getProfile()
      .then((freshUser) => {
        if (!active) return;
        setUser(freshUser);
        resetProfileForm({
          name: freshUser.name,
          phoneNumber: freshUser.phoneNumber ?? "",
          address: freshUser.address ?? ""
        });
      })
      .catch(() => {
        if (active) router.replace(getRoleLogin(user?.role));
      })
      .finally(() => {
        if (active) setLoading(false);
      });

    return () => {
      active = false;
    };
  }, [hasHydrated, token, user?.role, router, setUser, resetProfileForm]);

  const updateProfile = profileForm.handleSubmit(async (values) => {
    setProfileFeedback(null);
    try {
      const updatedUser = await authService.updateProfile({
        name: values.name.trim(),
        phoneNumber: values.phoneNumber.trim() || undefined,
        address: values.address.trim()
      });
      setUser(updatedUser);
      setProfileFeedback({ type: "success", message: "Đã cập nhật hồ sơ." });
    } catch (error) {
      setProfileFeedback({ type: "error", message: getApiErrorMessage(error) });
    }
  });

  const changePassword = passwordForm.handleSubmit(async (values) => {
    setPasswordFeedback(null);
    try {
      await authService.changePassword({
        currentPassword: values.currentPassword,
        newPassword: values.newPassword
      });
      passwordForm.reset();
      setPasswordFeedback({ type: "success", message: "Đã đổi mật khẩu." });
    } catch (error) {
      setPasswordFeedback({ type: "error", message: getApiErrorMessage(error) });
    }
  });

  const logout = () => {
    const loginHref = getRoleLogin(user?.role);
    clearSession();
    router.push(loginHref);
  };

  if (!hasHydrated || loading || !user) {
    return <main className="profile-page"><div className="profile-loading">Đang tải hồ sơ...</div></main>;
  }

  return (
    <main className="profile-page">
      <div className="profile-shell">
        <header className="profile-heading">
          <div>
            <span className="eyebrow">Tài khoản GreenFarm</span>
            <h1>Hồ sơ của bạn</h1>
            <p>Thông tin định danh và bảo mật tài khoản.</p>
          </div>
          <button type="button" className="secondary-button" onClick={logout}>
            <LogOut size={17} aria-hidden="true" /> Đăng xuất
          </button>
        </header>

        <section className="identity-strip">
          <div className="avatar-placeholder"><UserRound size={28} aria-hidden="true" /></div>
          <div>
            <strong>{user.name}</strong>
            <span>{user.email}</span>
          </div>
          <span className="role-label"><ShieldCheck size={15} /> {user.role}</span>
        </section>

        <div className="profile-grid">
          <section className="profile-section">
            <div className="section-heading">
              <UserRound size={20} aria-hidden="true" />
              <div><h2>Thông tin cá nhân</h2><p>Cập nhật thông tin liên hệ.</p></div>
            </div>
            <form onSubmit={updateProfile} className="form-stack" noValidate>
              <div className="field-group">
                <label htmlFor="profile-name">Họ và tên</label>
                <input
                  id="profile-name"
                  {...profileForm.register("name", {
                    required: "Vui lòng nhập họ tên.",
                    minLength: { value: 2, message: "Họ tên cần ít nhất 2 ký tự." }
                  })}
                />
                {profileForm.formState.errors.name && (
                  <p className="field-error">{profileForm.formState.errors.name.message}</p>
                )}
              </div>
              <div className="field-group">
                <label htmlFor="profile-phone">Số điện thoại</label>
                <input
                  id="profile-phone"
                  type="tel"
                  placeholder="0901234567"
                  {...profileForm.register("phoneNumber", {
                    pattern: {
                      value: /^(?:\+84|0)[0-9]{9,10}$|^$/,
                      message: "Số điện thoại chưa đúng định dạng."
                    }
                  })}
                />
                {profileForm.formState.errors.phoneNumber && (
                  <p className="field-error">{profileForm.formState.errors.phoneNumber.message}</p>
                )}
              </div>
              <div className="field-group">
                <label htmlFor="profile-address">Địa chỉ</label>
                <textarea
                  id="profile-address"
                  rows={4}
                  placeholder="Địa chỉ nhận hàng"
                  {...profileForm.register("address", {
                    maxLength: { value: 500, message: "Địa chỉ không vượt quá 500 ký tự." }
                  })}
                />
                {profileForm.formState.errors.address && (
                  <p className="field-error">{profileForm.formState.errors.address.message}</p>
                )}
              </div>
              {profileFeedback && (
                <p className={`form-message ${profileFeedback.type}`} role="status">
                  {profileFeedback.message}
                </p>
              )}
              <button type="submit" className="primary-button" disabled={profileForm.formState.isSubmitting}>
                <Save size={17} aria-hidden="true" />
                {profileForm.formState.isSubmitting ? "Đang lưu..." : "Lưu thay đổi"}
              </button>
            </form>
          </section>

          <section className="profile-section">
            <div className="section-heading">
              <KeyRound size={20} aria-hidden="true" />
              <div><h2>Đổi mật khẩu</h2><p>Dùng mật khẩu từ 8 đến 72 ký tự.</p></div>
            </div>
            <form onSubmit={changePassword} className="form-stack" noValidate>
              <div className="field-group">
                <label htmlFor="current-password">Mật khẩu hiện tại</label>
                <input
                  id="current-password"
                  type="password"
                  autoComplete="current-password"
                  {...passwordForm.register("currentPassword", {
                    required: "Vui lòng nhập mật khẩu hiện tại."
                  })}
                />
                {passwordForm.formState.errors.currentPassword && (
                  <p className="field-error">{passwordForm.formState.errors.currentPassword.message}</p>
                )}
              </div>
              <div className="field-group">
                <label htmlFor="new-password">Mật khẩu mới</label>
                <input
                  id="new-password"
                  type="password"
                  autoComplete="new-password"
                  {...passwordForm.register("newPassword", {
                    required: "Vui lòng nhập mật khẩu mới.",
                    minLength: { value: 8, message: "Mật khẩu cần ít nhất 8 ký tự." },
                    maxLength: { value: 72, message: "Mật khẩu không vượt quá 72 ký tự." }
                  })}
                />
                {passwordForm.formState.errors.newPassword && (
                  <p className="field-error">{passwordForm.formState.errors.newPassword.message}</p>
                )}
              </div>
              <div className="field-group">
                <label htmlFor="confirm-new-password">Xác nhận mật khẩu mới</label>
                <input
                  id="confirm-new-password"
                  type="password"
                  autoComplete="new-password"
                  {...passwordForm.register("confirmPassword", {
                    required: "Vui lòng xác nhận mật khẩu.",
                    validate: (value) =>
                      value === passwordForm.getValues("newPassword")
                      || "Hai mật khẩu chưa trùng nhau."
                  })}
                />
                {passwordForm.formState.errors.confirmPassword && (
                  <p className="field-error">{passwordForm.formState.errors.confirmPassword.message}</p>
                )}
              </div>
              {passwordFeedback && (
                <p className={`form-message ${passwordFeedback.type}`} role="status">
                  {passwordFeedback.message}
                </p>
              )}
              <button type="submit" className="secondary-button" disabled={passwordForm.formState.isSubmitting}>
                <KeyRound size={17} aria-hidden="true" />
                {passwordForm.formState.isSubmitting ? "Đang đổi..." : "Đổi mật khẩu"}
              </button>
            </form>
          </section>
        </div>
      </div>
    </main>
  );
}
