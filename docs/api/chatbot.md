# AI Chatbot

Chatbot chỉ được phép tư vấn dữ liệu công khai: sản phẩm đang hiển thị, giá, tồn kho và chính sách mua hàng. Nó không truy cập đơn hàng, hồ sơ khách, thông tin thanh toán, dữ liệu nhân sự hay cấu hình nội bộ.

## API

- `GET /api/public/chat/messages?guestToken={token}`: lấy tối đa 20 tin nhắn gần nhất của phiên khách hoặc của tài khoản đang đăng nhập.
- `POST /api/public/chat/messages`

```json
{
  "message": "Cà chua còn hàng không?",
  "guestToken": "optional-token-for-anonymous-visitor"
}
```

Khách chưa đăng nhập nhận `guestToken` trong response đầu tiên. Frontend lưu token này ở `localStorage`; backend chỉ lưu SHA-256 hash, không lưu token thô. Với tài khoản đã đăng nhập, lịch sử được tách theo `user_id` và bỏ qua guest token gửi lên.

## Cấu hình AI

Thêm vào `.env` (không commit khóa):

```dotenv
GEMINI_API_KEY=your_gemini_api_key
GEMINI_CHAT_MODEL=gemini-3.5-flash
```

Nếu chưa có `GEMINI_API_KEY` hoặc Gemini API lỗi, chatbot vẫn trả lời theo dữ liệu catalog/chính sách công khai bằng bộ trả lời dự phòng.

## An toàn dữ liệu

- Email, số điện thoại và chuỗi số giống thẻ thanh toán được che trước khi lưu hoặc gửi tới AI; lịch sử cũ cũng được lọc lại trước khi dùng làm context.
- Các câu hỏi về tài khoản, đơn hàng cá nhân, thanh toán, nhân viên, quản trị, doanh thu và dữ liệu nội bộ bị từ chối.
- Context gửi AI được tạo hoàn toàn ở backend từ catalog công khai; frontend không thể gửi giá, tồn kho hoặc dữ liệu nội bộ vào context.
