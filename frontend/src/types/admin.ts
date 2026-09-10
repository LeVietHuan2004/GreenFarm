import type { User, UserRole } from "@/types/auth";

export type AdminUserStatus = User["status"];

export type AdminUserFilters = {
  search?: string;
  role?: UserRole;
  status?: AdminUserStatus;
  page?: number;
  size?: number;
  sort?: string;
};

export type AdminUserUpdate = {
  role?: UserRole;
  status?: AdminUserStatus;
};

export type AdminRole = {
  id: number;
  name: UserRole;
  permissions: string[];
};
