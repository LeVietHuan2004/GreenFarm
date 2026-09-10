"use client";

import { Eye, EyeOff, UserPlus } from "lucide-react";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { useForm } from "react-hook-form";

import { getApiErrorMessage } from "@/lib/api-error";
import { authService } from "@/services/auth-service";
import { useAuthStore } from "@/stores/auth-store";

type RegisterValues = {
  name: string;
  email: string;
  phoneNumber: string;
  password: string;
  confirmPassword: string;
};

export function RegisterForm() {
  const router = useRouter();
  const setSession = useAuthStore((state) => state.setSession);
  const [showPassword, setShowPassword] = useState(false);
  const [serverError, setServerError] = useState<string | null>(null);
  const {
    register,
    handleSubmit,
    getValues,
    formState: { errors, isSubmitting }
  } = useForm<RegisterValues>();

  const onSubmit = handleSubmit(async (values) => {
    setServerError(null);
    try {
      const session = await authService.register({
        name: values.name.trim(),
        email: values.email.trim(),
        phoneNumber: values.phoneNumber.trim() || undefined,
        password: values.password
      });
      setSession(session);
      router.replace("/");
    } catch (error) {
      setServerError(getApiErrorMessage(error));
    }
  });

  return (
    <form onSubmit={onSubmit} className="form-stack compact" noValidate>
      <div className="field-group">
        <label htmlFor="name">Họ và tên</label>
        <input
          id="name"
          autoComplete="name"
          placeholder="Nguyễn Văn An"
          aria-invalid={Boolean(errors.name)}
          {...register("name", {
            required: "Vui lòng nhập họ tên.",
            minLength: { value: 2, message: "Họ tên cần ít nhất 2 ký tự." },
            maxLength: { value: 100, message: "Họ tên không vượt quá 100 ký tự." }
          })}
        />
        {errors.name && <p className="field-error">{errors.name.message}</p>}
      </div>

      <div className="two-column-fields">
        <div className="field-group">
          <label htmlFor="email">Email</label>
          <input
            id="email"
            type="email"
            autoComplete="email"
            placeholder="ban@greenfarm.vn"
            aria-invalid={Boolean(errors.email)}
            {...register("email", {
              required: "Vui lòng nhập email.",
              pattern: {
                value: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
                message: "Email chưa đúng định dạng."
              }
            })}
          />
          {errors.email && <p className="field-error">{errors.email.message}</p>}
        </div>

        <div className="field-group">
          <label htmlFor="phoneNumber">Số điện thoại</label>
          <input
            id="phoneNumber"
            type="tel"
            autoComplete="tel"
            placeholder="0901234567"
            aria-invalid={Boolean(errors.phoneNumber)}
            {...register("phoneNumber", {
              pattern: {
                value: /^(?:\+84|0)[0-9]{9,10}$/,
                message: "Số điện thoại chưa đúng định dạng."
              }
            })}
          />
          {errors.phoneNumber && <p className="field-error">{errors.phoneNumber.message}</p>}
        </div>
      </div>

      <div className="field-group">
        <label htmlFor="password">Mật khẩu</label>
        <div className="password-field">
          <input
            id="password"
            type={showPassword ? "text" : "password"}
            autoComplete="new-password"
            placeholder="Từ 8 đến 72 ký tự"
            aria-invalid={Boolean(errors.password)}
            {...register("password", {
              required: "Vui lòng nhập mật khẩu.",
              minLength: { value: 8, message: "Mật khẩu cần ít nhất 8 ký tự." },
              maxLength: { value: 72, message: "Mật khẩu không vượt quá 72 ký tự." }
            })}
          />
          <button
            type="button"
            className="password-toggle"
            onClick={() => setShowPassword((value) => !value)}
            aria-label={showPassword ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
            title={showPassword ? "Ẩn mật khẩu" : "Hiện mật khẩu"}
          >
            {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
          </button>
        </div>
        {errors.password && <p className="field-error">{errors.password.message}</p>}
      </div>

      <div className="field-group">
        <label htmlFor="confirmPassword">Xác nhận mật khẩu</label>
        <input
          id="confirmPassword"
          type={showPassword ? "text" : "password"}
          autoComplete="new-password"
          placeholder="Nhập lại mật khẩu"
          aria-invalid={Boolean(errors.confirmPassword)}
          {...register("confirmPassword", {
            required: "Vui lòng xác nhận mật khẩu.",
            validate: (value) =>
              value === getValues("password") || "Hai mật khẩu chưa trùng nhau."
          })}
        />
        {errors.confirmPassword && (
          <p className="field-error">{errors.confirmPassword.message}</p>
        )}
      </div>

      {serverError && <p className="form-message error" role="alert">{serverError}</p>}

      <button type="submit" className="primary-button" disabled={isSubmitting}>
        <UserPlus size={18} aria-hidden="true" />
        {isSubmitting ? "Đang tạo tài khoản..." : "Tạo tài khoản"}
      </button>

    </form>
  );
}
