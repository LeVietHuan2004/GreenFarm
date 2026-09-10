import axios from "axios";

import type { ApiError } from "@/types/auth";

export function getApiErrorMessage(error: unknown): string {
  if (!axios.isAxiosError<ApiError>(error)) {
    return "Đã có lỗi xảy ra. Vui lòng thử lại.";
  }

  if (!error.response) {
    return "Không thể kết nối đến máy chủ. Hãy kiểm tra Docker và backend.";
  }

  const fieldMessage = error.response.data?.fieldErrors?.[0]?.message;
  return fieldMessage ?? error.response.data?.message ?? "Yêu cầu chưa thể xử lý.";
}
