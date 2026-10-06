import { apiClient } from "@/lib/axios-client";
import type { ApiResponse } from "@/types/auth";
import type { ChatConversation, ChatMessage } from "@/types/chat";

const guestTokenKey = "greenfarm_chat_guest_token";
// Gemini can take longer than regular CRUD requests. Keep the general API
// timeout short, but give a single chat response enough time to complete.
const chatResponseTimeoutMs = 55_000;
function guestToken() { return typeof window === "undefined" ? null : window.localStorage.getItem(guestTokenKey); }
function storeGuestToken(token: string | null) { if (typeof window !== "undefined" && token) window.localStorage.setItem(guestTokenKey, token); }

export const chatService = {
  async history() {
    const response = await apiClient.get<ApiResponse<ChatMessage[]>>("/public/chat/messages", { params: { guestToken: guestToken() ?? undefined } });
    return response.data.data;
  },
  async send(message: string) {
    const response = await apiClient.post<ApiResponse<ChatConversation>>(
      "/public/chat/messages",
      { message, guestToken: guestToken() ?? undefined },
      { timeout: chatResponseTimeoutMs }
    );
    storeGuestToken(response.data.data.guestToken);
    return response.data.data.assistant;
  }
};
