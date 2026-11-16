# Email hóa đơn trên Railway Trial

Railway Trial/Free/Hobby chặn SMTP. GreenFarm hỗ trợ gửi hóa đơn qua Resend HTTPS;
SMTP vẫn là mặc định cho local hoặc host có hỗ trợ SMTP.

## Tạo tài khoản và chọn địa chỉ gửi

1. Đăng ký tại https://resend.com và mở API Keys để tạo key có quyền gửi email.
2. Chưa có tên miền riêng: dùng `GreenFarm <onboarding@resend.dev>` để thử, với email
   đặt hàng trùng chính email đăng ký tài khoản Resend. Chế độ này không gửi được
   cho khách hàng khác.
3. Để gửi cho khách hàng, mở Domains, thêm tên miền riêng bạn quản lý, cấu hình DNS
   theo Resend rồi xác minh. Dùng địa chỉ gửi trên tên miền đã xác minh.

`https://green-farm-rosy.vercel.app` là địa chỉ website. Bạn không quản lý DNS của
`vercel.app`, nên không thể dùng địa chỉ đó để xác minh tên miền gửi email. Địa chỉ
website vẫn được dùng làm STOREFRONT_URL và không cần đổi khi thử Resend.

## Biến môi trường backend

Triển khai mã backend hỗ trợ Resend, sau đó vào Railway → GreenFarm backend →
Variables → Raw Editor. Giữ các biến có sẵn và bổ sung:

```dotenv
STOREFRONT_URL=https://green-farm-rosy.vercel.app
INVOICE_EMAIL_ENABLED=true
INVOICE_EMAIL_PROVIDER=resend
INVOICE_EMAIL_FROM=GreenFarm <onboarding@resend.dev>
RESEND_API_KEY=<API key của bạn>
```

Đổi INVOICE_EMAIL_FROM khi đã xác minh tên miền riêng. Không dùng địa chỉ Gmail làm
from trên Resend. Không đưa API key vào Git, frontend, ảnh chụp hoặc chat. Sau khi
lưu biến, deploy lại backend. Thêm biến vào bản mã cũ chưa đủ để bật Resend.

Không cần MAIL_HOST/MAIL_PORT/MAIL_USERNAME/MAIL_PASSWORD cho hóa đơn khi dùng
provider resend. Phần khôi phục mật khẩu hiện vẫn dùng SMTP; provider hóa đơn này
không tự chuyển email khôi phục mật khẩu sang HTTPS.

## Nghiệm thu

- Đặt đơn COD thử: gửi hóa đơn sau khi tạo đơn thành công.
- Với VNPAY: chỉ gửi khi thanh toán thành công, không gửi lúc đang chờ hoặc thất bại.
- Kiểm tra Resend → Emails, hộp thư đến/thư rác và dữ liệu payment:
  invoice_email_sent_at chỉ có khi provider đã tiếp nhận email thành công.
- Nếu bị lỗi API, không đánh dấu đã gửi; lưu lần lỗi và retry theo backoff. Request
  dùng Idempotency-Key ổn định theo đơn để hạn chế gửi lặp khi bị timeout; Resend
  giữ key 24 giờ. Đây không phải cam kết gửi đúng một lần trong mọi tình huống.
- Giữ nguyên người nhận hóa đơn theo tài khoản/email checkout. Khi chưa xác minh
  domain, chọn tài khoản thử của chính bạn thay vì chuyển mọi hóa đơn về một hộp thư.
- Giao diện hiển thị theo invoiceEmailStatus (sent/retrying/failed/disabled/
  not_configured/not_sent/not_applicable); không báo đã gửi chỉ vì đặt hàng xong.
- Các đơn cũ #42/#43 chưa có lịch retry sẽ không tự được gửi bù khi bật cấu hình.
  Gửi bù là thao tác riêng, cần xác định đúng đơn và người nhận.

Việc cấu hình provider/key không đồng nghĩa đã nghiệm thu email thực tế. Cần một
đơn thử và kiểm tra kết quả nhận thư sau khi deploy. Không có email thật được gửi
trong unit test; test HTTP dùng server mock.

## Tài liệu chính thức

- [Railway: SMTP và gói dịch vụ](https://docs.railway.com/networking/outbound-networking)
- [Resend Send Email](https://resend.com/docs/api-reference/emails/send-email)
- [Giới hạn domain resend.dev](https://resend.com/docs/knowledge-base/403-error-resend-dev-domain)
- [Resend Idempotency Keys](https://resend.com/docs/dashboard/emails/idempotency-keys)
