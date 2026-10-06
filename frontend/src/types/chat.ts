export type ChatProduct = { id:number; name:string; slug:string; price:number; unit:string|null; stock:number; image:string|null };
export type ChatMessage = { id:number; sender:"user"|"assistant"; content:string; createdAt:string; products?:ChatProduct[] };
export type ChatConversation = { guestToken:string|null; assistant:ChatMessage };
