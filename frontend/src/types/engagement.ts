import type { ApiResponse } from "@/types/auth";
import type { PageData } from "@/types/catalog";

export type Review = { id:number; productId:number; userId:number; userName:string; rating:number; comment:string|null; createdAt:string; updatedAt:string };
export type ReviewEligibility = { purchased:boolean; canReview:boolean; existingReview:Review|null };
export type ReviewInput = { productId:number; rating:number; comment?:string };
export type ContactStatus = "open" | "replied" | "resolved";
export type Contact = { id:number; userId:number|null; fullName:string; phoneNumber:string|null; email:string|null; message:string; status:ContactStatus; response:string|null; respondedById:number|null; respondedByName:string|null; respondedAt:string|null; createdAt:string; updatedAt:string };
export type ContactInput = { fullName:string; phoneNumber?:string; email?:string; message:string };
export type Notification = { id:number; type:"order"|"delivery"|"contact"|string; message:string; link:string|null; read:boolean; createdAt:string };
export type EngagementResponse<T> = ApiResponse<T>;
export type EngagementPage<T> = PageData<T>;
