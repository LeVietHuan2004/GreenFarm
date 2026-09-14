import { apiClient } from "@/lib/axios-client";
import type { Contact, ContactInput, ContactStatus, EngagementPage, EngagementResponse, Review, ReviewEligibility, ReviewInput } from "@/types/engagement";

export async function getProductReviews(productId: number) {
  return (await apiClient.get<EngagementResponse<EngagementPage<Review>>>(`/public/products/${productId}/reviews`, { params: { page: 0, size: 20 } })).data.data;
}
export async function getReviewEligibility(productId: number) {
  return (await apiClient.get<EngagementResponse<ReviewEligibility>>(`/reviews/eligibility/${productId}`)).data.data;
}
export async function createReview(input: ReviewInput) {
  return (await apiClient.post<EngagementResponse<Review>>("/reviews", input)).data.data;
}
export async function updateReview(id: number, input: ReviewInput) {
  return (await apiClient.put<EngagementResponse<Review>>(`/reviews/${id}`, input)).data.data;
}
export async function createContact(input: ContactInput) {
  return (await apiClient.post<EngagementResponse<Contact>>("/public/contacts", input)).data.data;
}
export async function getMyContacts() {
  return (await apiClient.get<EngagementResponse<Contact[]>>("/contacts/mine")).data.data;
}
export async function getOperationalContacts(status?: ContactStatus, page = 0) {
  return (await apiClient.get<EngagementResponse<EngagementPage<Contact>>>("/operations/contacts", { params: { status: status || undefined, page, size: 20 } })).data.data;
}
export async function replyContact(id: number, response: string) {
  return (await apiClient.patch<EngagementResponse<Contact>>(`/operations/contacts/${id}/reply`, { response })).data.data;
}
export async function resolveContact(id: number) {
  return (await apiClient.patch<EngagementResponse<Contact>>(`/operations/contacts/${id}/resolve`)).data.data;
}
