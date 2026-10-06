# Mua hàng không cần tài khoản

Khách chưa đăng nhập có thể xem/tìm/lọc sản phẩm công khai, dùng giỏ hàng và đặt
hàng mà không bị tạo tài khoản tự động. Giỏ được lưu ở MySQL theo một guest token
ngẫu nhiên; trình duyệt chỉ giữ token và thời hạn phiên trong `localStorage`.
Thời hạn mặc định là 30 ngày (`GUEST_SESSION_DAYS`).

| Method | Endpoint | Mục đích |
| --- | --- | --- |
| POST | `/api/public/guest/sessions` | Tạo phiên và nhận token |
| GET/POST/PATCH/DELETE | `/api/public/guest/cart...` | Xem, thêm, sửa, xóa giỏ; gửi `X-Guest-Token` |
| POST | `/api/public/guest/checkout/preview` | Tính lại giá, tồn kho, vận chuyển, voucher |
| POST | `/api/public/guest/orders` | Đặt hàng COD/VNPAY; gửi khóa `idempotencyKey` |
| GET | `/api/public/guest/orders/recovery?key=...` | Lấy lại kết quả checkout đã commit bằng guest token + khóa checkout |
| POST | `/api/public/guest/orders/lookup` | Tra cứu bằng `orderId`, email và lookup token |
| POST | `/api/cart/merge-guest` | Sau đăng nhập/đăng ký, gộp giỏ guest vào giỏ tài khoản |

Checkout yêu cầu họ tên, số điện thoại, email, địa chỉ, tỉnh/thành phố, phương
thức vận chuyển, phương thức thanh toán; voucher là tùy chọn. Backend tự tính
tiền và giữ chỗ lượt dùng voucher. Phản hồi tạo đơn có `order`, `lookupToken`,
`replayed`, `paymentUrl`. Frontend lưu mã tra cứu và email trên thiết bị trước
khi chuyển sang VNPAY. Nếu cùng guest token và khóa checkout được gửi lại,
backend trả đơn cũ, không trừ tồn kho lần hai; URL VNPAY chỉ được cấp lại khi
payment còn `pending` và chưa hết hạn. Sau khi đặt hàng thành công, guest cart
và phiên checkout bị vô hiệu hóa.
Trình duyệt giữ khóa checkout cho đến khi nhận được response. Nếu bị tải lại
trang sau khi server tạo đơn, nó gọi recovery để lấy mã tra cứu và tiếp tục
thanh toán mà không tạo đơn mới.

Merge khóa tài khoản và phiên guest, kiểm tra lại **toàn bộ** giỏ sau khi gộp:
trạng thái bán, tồn kho và tổng số lượng. Giá trong giỏ lấy từ sản phẩm hiện tại;
voucher/ưu đãi được tính và kiểm tra lại khi preview hoặc checkout. Nếu bất kỳ
dòng nào không hợp lệ, toàn bộ merge rollback và khách có thể chỉnh giỏ trước
khi thử lại. Merge cùng phiên không cộng hai lần.

Lookup token là bí mật: không chia sẻ, không đưa vào URL. Backend kiểm tra cả
email và token trước khi trả chi tiết đơn. Trình duyệt chỉ lưu ID, email và
lookup token cho tối đa 10 đơn; trạng thái đơn luôn được lấy lại từ backend.
Sau khi xác minh, khách có thể mở `/guest-orders/{id}/invoice` để xem hoặc in
hóa đơn; trang này cũng yêu cầu email và lookup token nếu thiết bị chưa lưu mã.
Nếu mất dữ liệu trình duyệt hoặc đổi thiết bị, khách cần liên hệ hỗ trợ để xác
minh đơn — hiện chưa có quy trình cấp lại lookup token qua email/OTP.
