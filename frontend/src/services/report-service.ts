import { apiClient } from "@/lib/axios-client";
import type { ApiResponse } from "@/types/auth";

export type BusinessReport = {
  from:string; to:string; netRevenue:number; paidOrders:number; averageOrderValue:number;
  dailyRevenue:{date:string;revenue:number;orders:number}[];
  topProducts:{productId:number;productName:string;quantity:number;revenue:number}[];
  orderStatuses:{status:string;count:number}[];
  newCustomers:number;
  lowStockThreshold:number;
  lowStockCount:number;
  lowStockProducts:{productId:number;productName:string;stock:number}[];
  couponPerformance:{couponId:number;code:string;couponType:"ORDER_DISCOUNT"|"FREESHIP";uses:number;discountAmount:number}[];
};

export type ReportRange = {from:string;to:string};

export async function getBusinessReport(range:ReportRange) {
  return (await apiClient.get<ApiResponse<BusinessReport>>("/admin/reports", {params:range})).data.data;
}

export async function downloadBusinessReport(range:ReportRange) {
  const response = await apiClient.get<Blob>("/admin/reports/export.csv", { responseType:"blob", params:range });
  const url = URL.createObjectURL(response.data);
  const anchor = document.createElement("a"); anchor.href=url; anchor.download=`greenfarm-report-${range.from}-${range.to}.csv`; anchor.click();
  setTimeout(() => URL.revokeObjectURL(url), 1000);
}
