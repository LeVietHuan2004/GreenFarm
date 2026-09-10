"use client";

import { ChevronLeft, ChevronRight, Search, ShieldCheck, UserRound, UsersRound } from "lucide-react";
import { FormEvent, useCallback, useEffect, useState } from "react";

import { AdminShell } from "@/components/admin/admin-shell";
import { getApiErrorMessage } from "@/lib/api-error";
import { getAdminRoles, getAdminUsers, updateAdminUser } from "@/services/admin-service";
import { useAuthStore } from "@/stores/auth-store";
import type { AdminRole, AdminUserStatus } from "@/types/admin";
import type { User, UserRole } from "@/types/auth";
import type { PageData } from "@/types/catalog";

const emptyPage: PageData<User> = {
  content: [], page: 0, size: 20, totalElements: 0, totalPages: 0, first: true, last: true
};

const roleLabels: Record<UserRole, string> = {
  admin: "Quản trị viên",
  staff: "Nhân viên",
  delivery_staff: "Giao hàng",
  customer: "Khách hàng"
};

const statusLabels: Record<AdminUserStatus, string> = {
  active: "Hoạt động",
  pending: "Chờ kích hoạt",
  banned: "Đã khóa",
  deleted: "Đã xóa"
};

export function AdminUsers() {
  const { user: currentUser, token, hasHydrated } = useAuthStore();
  const adminReady = hasHydrated && Boolean(token) && currentUser?.role === "admin";
  const [users, setUsers] = useState<PageData<User>>(emptyPage);
  const [roles, setRoles] = useState<AdminRole[]>([]);
  const [searchInput, setSearchInput] = useState("");
  const [search, setSearch] = useState("");
  const [role, setRole] = useState<"" | UserRole>("");
  const [status, setStatus] = useState<"" | AdminUserStatus>("");
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [updatingId, setUpdatingId] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [message, setMessage] = useState<string | null>(null);

  const loadUsers = useCallback(async () => {
    setLoading(true);
    try {
      setUsers(await getAdminUsers({
        search,
        role: role || undefined,
        status: status || undefined,
        page,
        size: 20,
        sort: "createdAt,desc"
      }));
      setError(null);
    } catch (requestError) {
      setError(getApiErrorMessage(requestError));
    } finally {
      setLoading(false);
    }
  }, [page, role, search, status]);

  useEffect(() => {
    if (!adminReady) return;
    getAdminRoles().then(setRoles).catch((requestError) => setError(getApiErrorMessage(requestError)));
  }, [adminReady]);

  useEffect(() => {
    if (!adminReady) return;
    const timer = window.setTimeout(() => { void loadUsers(); }, 0);
    return () => window.clearTimeout(timer);
  }, [adminReady, loadUsers]);

  const applySearch = (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setPage(0);
    setSearch(searchInput.trim());
  };

  const updateRole = async (user: User, nextRole: UserRole) => {
    setUpdatingId(user.id);
    setError(null);
    try {
      const updated = await updateAdminUser(user.id, { role: nextRole });
      setUsers((current) => ({ ...current, content: current.content.map((item) => item.id === updated.id ? updated : item) }));
      setMessage(`Đã đổi vai trò của ${updated.name}.`);
    } catch (requestError) {
      setError(getApiErrorMessage(requestError));
    } finally {
      setUpdatingId(null);
    }
  };

  const updateStatus = async (user: User, nextStatus: AdminUserStatus) => {
    setUpdatingId(user.id);
    setError(null);
    try {
      const updated = await updateAdminUser(user.id, { status: nextStatus });
      setUsers((current) => ({ ...current, content: current.content.map((item) => item.id === updated.id ? updated : item) }));
      setMessage(`Đã cập nhật trạng thái của ${updated.name}.`);
    } catch (requestError) {
      setError(getApiErrorMessage(requestError));
    } finally {
      setUpdatingId(null);
    }
  };

  return (
    <AdminShell active="users">
      <main className="admin-catalog-main admin-users-main">
        <header className="admin-page-heading">
          <div><span className="eyebrow">Tài khoản · Phân quyền</span><h1>Quản lý người dùng</h1><p>Tìm kiếm tài khoản, điều chỉnh vai trò và kiểm soát trạng thái truy cập.</p></div>
          <span className="admin-count"><UsersRound size={15} /> {users.totalElements} người dùng</span>
        </header>

        {error && <p className="catalog-notice error">{error}</p>}
        {message && <p className="catalog-notice success">{message}</p>}

        <section className="admin-list-card admin-users-card">
          <div className="admin-user-toolbar">
            <form onSubmit={applySearch}>
              <Search size={18} />
              <input value={searchInput} onChange={(event) => setSearchInput(event.target.value)} placeholder="Tìm theo tên hoặc email" />
              <button type="submit">Tìm kiếm</button>
            </form>
            <select value={role} onChange={(event) => { setRole(event.target.value as typeof role); setPage(0); }} aria-label="Lọc theo vai trò">
              <option value="">Mọi vai trò</option>
              {roles.map((item) => <option value={item.name} key={item.id}>{roleLabels[item.name]}</option>)}
            </select>
            <select value={status} onChange={(event) => { setStatus(event.target.value as typeof status); setPage(0); }} aria-label="Lọc theo trạng thái">
              <option value="">Mọi trạng thái</option>
              {Object.entries(statusLabels).map(([value, label]) => <option value={value} key={value}>{label}</option>)}
            </select>
          </div>

          <div className="admin-user-table" role="table" aria-label="Danh sách người dùng">
            <div className="admin-user-table-head" role="row">
              <span>Người dùng</span><span>Vai trò</span><span>Trạng thái</span><span>Đăng nhập gần nhất</span>
            </div>
            {users.content.map((user) => {
              const isCurrentUser = currentUser?.id === user.id;
              return (
                <article className="admin-user-row" role="row" key={user.id}>
                  <div className="admin-user-identity">
                    <span className="admin-user-list-avatar"><UserRound size={20} /></span>
                    <div><strong>{user.name}</strong><small>{user.email}</small>{isCurrentUser && <em>Tài khoản của bạn</em>}</div>
                  </div>
                  <label>
                    <span className="mobile-only-label">Vai trò</span>
                    <select value={user.role} disabled={isCurrentUser || updatingId === user.id} onChange={(event) => void updateRole(user, event.target.value as UserRole)}>
                      {roles.map((item) => <option value={item.name} key={item.id}>{roleLabels[item.name]}</option>)}
                    </select>
                  </label>
                  <label>
                    <span className="mobile-only-label">Trạng thái</span>
                    <select className={`user-status-select ${user.status}`} value={user.status} disabled={isCurrentUser || updatingId === user.id} onChange={(event) => void updateStatus(user, event.target.value as AdminUserStatus)}>
                      {Object.entries(statusLabels).map(([value, label]) => <option value={value} key={value}>{label}</option>)}
                    </select>
                  </label>
                  <div className="admin-last-login">
                    <ShieldCheck size={16} />
                    <span>{user.lastLoginAt ? new Intl.DateTimeFormat("vi-VN", { dateStyle: "short", timeStyle: "short" }).format(new Date(user.lastLoginAt)) : "Chưa đăng nhập"}</span>
                  </div>
                </article>
              );
            })}
            {loading && <div className="admin-empty">Đang tải người dùng...</div>}
            {!loading && users.content.length === 0 && <div className="admin-empty">Không tìm thấy tài khoản phù hợp.</div>}
          </div>

          {users.totalPages > 1 && (
            <nav className="pagination admin-user-pagination" aria-label="Phân trang người dùng">
              <button type="button" disabled={users.first || loading} onClick={() => setPage((value) => Math.max(0, value - 1))}><ChevronLeft size={17} /> Trước</button>
              <span>Trang {users.page + 1} / {users.totalPages}</span>
              <button type="button" disabled={users.last || loading} onClick={() => setPage((value) => value + 1)}>Sau <ChevronRight size={17} /></button>
            </nav>
          )}
        </section>
      </main>
    </AdminShell>
  );
}
