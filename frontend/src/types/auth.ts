export type ApiResponse<T> = {
  success: boolean;
  message: string;
  data: T;
  timestamp: string;
};

export type FieldValidationError = {
  field: string;
  message: string;
};

export type ApiError = {
  status: number;
  code: string;
  message: string;
  path: string;
  fieldErrors: FieldValidationError[];
};

export type UserRole = "customer" | "staff" | "delivery_staff" | "admin";

export type User = {
  id: number;
  name: string;
  email: string;
  status: "pending" | "active" | "banned" | "deleted";
  phoneNumber: string | null;
  avatar: string | null;
  address: string | null;
  role: UserRole;
  permissions: string[];
  lastLoginAt: string | null;
  createdAt: string;
};

export type AuthSession = {
  accessToken: string;
  tokenType: "Bearer";
  expiresIn: number;
  user: User;
};

export type LoginInput = {
  email: string;
  password: string;
  role?: UserRole;
};

export type RegisterInput = LoginInput & {
  name: string;
  phoneNumber?: string;
};

export type UpdateProfileInput = {
  name?: string;
  phoneNumber?: string;
  address?: string;
  avatar?: string;
};

export type ChangePasswordInput = {
  currentPassword: string;
  newPassword: string;
};
