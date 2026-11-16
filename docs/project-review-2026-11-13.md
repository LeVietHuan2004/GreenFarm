# Rà soát GreenFarm — 13/11/2026

## Kết luận

GreenFarm đã có phần lớn chức năng của một website thương mại điện tử và đã triển khai được frontend Vercel, backend/MySQL Railway. Có thể tiếp tục nghiệm thu bản demo. Chưa đủ bằng chứng để coi là hoàn thiện cho bán hàng thật: tích hợp còn ở môi trường thử, có cảnh báo thư viện, thiếu đối soát tiền và một số lỗi vận hành/giao diện cụ thể.

Phạm vi: đọc mã nguồn, chạy kiểm tra hiện có, kiểm tra HTTP công khai và truy vấn chỉ đọc trên database Railway. Không tạo đơn mới, không hoàn tiền, không thay đổi dữ liệu hoặc cấu hình triển khai trong lần rà soát này. Không chạy thử tấn công hay xác nhận khả năng khai thác các cảnh báo thư viện.

## Bằng chứng đã kiểm tra

- Commit đang rà soát: `c395483`; working tree sạch trước khi tạo báo cáo này.
- Frontend `https://green-farm-rosy.vercel.app/`: HTTP 200.
- Backend `/api/health`: HTTP 200; catalog công khai trả 42 sản phẩm.
- `/api/public/guest/payment-methods`: COD và VNPAY available; `/api/public/ghn/status`: available. Đây là trạng thái cấu hình, không tự chứng minh giao dịch thành công.
- GET `/api/admin/orders` và `/api/users/me` không có token đều trả 401. Chưa thay thế kiểm tra phân quyền đầy đủ giữa các tài khoản/vai trò.
- Đơn #42 tạo ngày 12/11: VNPAY `completed`, mã giao dịch thành công `00` đã kiểm tra ở lần nghiệm thu trước, số tiền 80.000đ. Lần rà soát này xác nhận database vẫn giữ `completed`; đơn ở `ready_for_delivery`.
- Lịch sử vận chuyển hiện có sự kiện `create` và `sync`, chưa có `switch_status`/`cod`. Chưa chứng minh webhook thật đã đi qua toàn bộ hành trình giao hàng.
- Backend Maven test hoàn tất: 151 test, 148 đạt, 3 bỏ qua, 0 failure/error. Hai test MySQL/Testcontainers bỏ qua vì không có Docker; một Cloudinary live test chỉ chạy khi bật biến môi trường riêng. Chưa có kết quả coverage hay kiểm thử tải.
- `npm audit --omit=dev` báo 6 gói: 1 critical, 5 high. Kết quả là phát hiện theo phiên bản phụ thuộc, không phải bằng chứng ứng dụng đã bị khai thác.
- Frontend `npm run quality` hoàn tất: ESLint, biên dịch production, TypeScript và sinh trang đều đạt. 13/13 test hồi phục phiên guest đạt. Đây chưa phải kiểm thử E2E trình duyệt.

## Các việc cần ưu tiên

### 1. Cập nhật các thư viện có cảnh báo — trước khi mở bán

`frontend/package.json` ghim Next.js 16.3.3. Lockfile có Axios 1.19.0, Zod 4.4.3, Sharp 0.35.3, follow-redirects 1.16.0, source-map-js 1.2.1. Audit đánh dấu Next.js critical; năm gói còn lại high. Một số lỗi phụ thuộc tính năng/adapter cụ thể, nên không thể suy ra tất cả đều khai thác được trong GreenFarm.

Đề xuất: nâng Next.js cùng eslint-config-next lên bản vá tương thích, cập nhật các gói còn lại và lockfile; chạy lại audit, build và kiểm thử mua hàng. Audit hiện đề xuất Next.js 16.4.6; cần đối chiếu lại thời điểm thực hiện. Không dùng `npm audit fix --force` một cách tự động.

Nguồn đối chiếu: [Next.js ImageResponse advisory](https://github.com/vercel/next.js/security/advisories/GHSA-vcvr-r3jv-pc5j), [Next.js Image Optimization advisory](https://github.com/vercel/next.js/security/advisories/GHSA-2464-fhr4-w9xw), [Zod advisory](https://github.com/advisories/GHSA-mrhx-c7xc-m3w5). Chưa thấy ứng dụng dùng ImageResponse/next/og; không coi advisory riêng của tính năng đó là bằng chứng đường khai thác trong GreenFarm.

### 2. Sửa điều kiện vận chuyển hàng tươi sống — trước khi dùng GHN thật

Database hiện bật `ghn_eligible` cho cả sản phẩm thịt/cá, bao gồm Cá BASA cắt khúc đã dùng trong đơn #42. `GhnShippingService` có chặn theo cờ này và thông số đóng gói, nhưng việc bật cờ không chứng minh GHN nhận loại hàng đó.

Chính sách GHN liệt kê hàng tươi sống, hàng đông lạnh ngoài phạm vi cung ứng. Tạo được vận đơn staging không chứng minh kiện hàng được nhận ở production.

Đề xuất: rà soát cờ đủ điều kiện theo từng sản phẩm và dịch vụ thực tế; dùng giao nội bộ hoặc đơn vị có dịch vụ phù hợp cho hàng cần bảo quản lạnh. Giữ việc lựa chọn nhà vận chuyển rõ ràng khi giỏ có hàng không đủ điều kiện.

Nguồn: [GHN — Hàng hóa không nhận vận chuyển, mục II(a)](https://ghn.vn/pages/hang-hoa-ghn-khong-nhan-van-chuyen).

### 3. Khép kín thanh toán, IPN và hoàn tiền — trước khi thu tiền thật

- `PaymentService.expirePendingVnpayPayments` đánh dấu failed/hủy đơn sau hạn; callback tới khi payment failed hoặc order canceled bị từ chối. Chưa tìm thấy bước tra cứu VNPAY để xử lý khách đã trả tiền nhưng callback đến trễ/mất. Rủi ro suy ra từ mã nguồn, chưa tạo giao dịch trễ để tái hiện trên Railway.
- `RefundRequestService.approve` giữ yêu cầu ở PROCESSING nếu gateway đang xử lý. `VnpayRefundPersistenceService` coi PENDING/PROCESSING là đang chạy và ngăn gửi yêu cầu mới. Chưa tìm thấy job/API đối soát để chốt các trạng thái này sau timeout hoặc phản hồi chưa cuối cùng.
- Chưa kiểm chứng riêng IPN từ VNPAY trong tình huống khách đóng trình duyệt trước khi về trang kết quả. Việc đơn #42 thành công chưa phân biệt được Return hay IPN đã chốt trạng thái.

Đề xuất: bổ sung tra cứu/đối soát gateway và hàng chờ cần xử lý, giới hạn timeout cho Refund API, kiểm thử callback lặp/trễ, mất mạng và hoàn tiền chưa có trạng thái cuối. Ghi rõ kết quả gateway khác với trạng thái đơn và chỉ hoàn tồn/hủy theo quy trình đã xác minh.

### 4. Phân tách demo và production

VNPAY đã thử thành công trên sandbox, chưa chứng minh thu tiền thật. GHN cũng đang staging; `GhnProperties.requireEnabled` và `available` chỉ chấp nhận `https://dev-online-gateway.ghn.vn`, nên không thể chỉ đổi biến URL sang production rồi dùng.

Đề xuất: thêm lựa chọn môi trường có kiểm tra cấu hình, kiểm thử cả hai; khi bán thật dùng tài khoản/token/shop và webhook production riêng. Xác minh địa chỉ lấy/trả hàng và quy tắc phí. Nếu mục tiêu hiện tại là bảo vệ đồ án/demo, giữ sandbox/staging và ghi rõ trên giao diện/tài liệu.

Nguồn: [GHN — tài khoản và token theo môi trường](https://developer.ghn.dev/vi/docs/token/get-token).

### 5. Sửa thông báo email và nghiệm thu SMTP

`InvoiceDocument` mặc định luôn hiển thị “Hóa đơn đã được gửi đến email đăng ký của bạn”; guest invoice cũng luôn truyền thông báo tương tự. Trong khi đó, đơn #42 có `invoice_email_sent_at=NULL`, attempts=0 ở thời điểm kiểm tra. Không đủ bằng chứng để khẳng định nguyên nhân là SMTP chưa cấu hình hay email đang tắt, nhưng câu thông báo hiện tại không dựa trên kết quả gửi.

Đề xuất: API trả trạng thái email (chưa bật/chờ gửi/đã gửi/lỗi), UI hiển thị theo trạng thái. Sau đó nghiệm thu email hóa đơn và quên mật khẩu với hộp thư thử. Mã nguồn quên mật khẩu đã có; tài liệu cũ ghi chưa có là lỗi thời.

Cập nhật mã nguồn 17/11: đã bổ sung provider Resend HTTPS cho email hóa đơn để dùng
với Railway Trial/Hobby, giữ SMTP cho local; API trả trạng thái gửi và giao diện hóa
đơn hiển thị theo kết quả. Phần này cần deploy mã mới và cấu hình API key trước khi
nghiệm thu gửi thật. Email khôi phục mật khẩu chưa chuyển sang HTTPS. Xem
[hướng dẫn cấu hình](railway-invoice-email.md).

### 6. Chuẩn hóa thời gian

`Order.@PrePersist` dùng `LocalDateTime.now()` theo giờ mặc định máy chủ; PaymentService lại ghi paidAt với Asia/Ho_Chi_Minh. Đơn #42 có createdAt 15:14:52 và paidAt 22:15:29 cho cùng lượt mua chỉ cách nhau khoảng một phút; giao diện cũng từng hiển thị hai mốc lệch 7 giờ. DTO dùng LocalDateTime không kèm offset và frontend dùng `new Date(value)`.

Đề xuất: thống nhất lưu thời điểm theo UTC/Instant hoặc chuẩn có offset, hiển thị giờ Việt Nam rõ ràng; rà soát báo cáo ngày, coupon và dữ liệu lịch sử trước khi chuyển đổi. Chỉ sửa format frontend sẽ không giải quyết dữ liệu đang dùng hai quy ước.

### 7. Nghiệm thu tự động và vận hành

- Chưa có cấu hình Playwright/Cypress hay bộ E2E frontend; hiện chỉ tìm thấy test hồi phục phiên guest.
- Chưa có workflow `.github` trong repo. `npm run quality` chỉ lint + build, không chạy test guest.
- Hai integration test bị bỏ qua khi không có Docker; cần một job CI chủ động chạy MySQL và không âm thầm bỏ qua.
- `/api/health` hiện trả giá trị cố định; không kiểm tra kết nối database. Chưa thấy Actuator/Micrometer hoặc cấu hình giám sát tương đương trong repo.
- Có bản sao lưu phục vụ migration trước đây, nhưng chưa kiểm chứng lịch backup tự động, diễn tập restore định kỳ và cảnh báo ở Railway trong lần này. Không kết luận Railway không có backup chỉ dựa vào repo.
- Ảnh cũ đã được chuyển sang URL Cloudinary trong đợt migration trước. Cần nghiệm thu riêng thao tác tải ảnh mới trên Railway rồi redeploy: ImageStorageService vẫn lưu `/uploads` local nếu không có đủ ba biến Cloudinary. Dockerfile/railway.json không tự tạo volume cho thư mục này; chưa kiểm tra cấu hình volume live trong lần rà soát.
- `.env`/`.env.railway` và dump cá nhân không được Git theo dõi. Cần xác minh JWT_SECRET riêng trên môi trường chạy thật; application.yml còn fallback secret local. Chưa xác minh giá trị secret live trong lần này.

Đề xuất: thêm CI lint/build/test/audit, E2E các luồng quan trọng, giám sát lỗi và kiểm thử khôi phục backup trước khi nhận đơn thật.

### 8. Hoàn thiện kho và tài liệu sau các việc trên

Kho đã có FEFO, giữ/xuất/hoàn, lọc lô sắp/hết hạn, loại lô hết hạn khỏi tồn khả dụng và điều chỉnh EXPIRED/DAMAGED. Database hiện có 1 lô hết hạn vẫn còn số lượng vật lý; điều đó không đồng nghĩa đang được bán. Chưa thấy cảnh báo chủ động tồn thấp/sắp hết hạn.

Đề xuất: bổ sung thông báo quản trị, quy trình kiểm kê/tiêu hủy và lịch sử xử lý có người thực hiện. Sau đó cập nhật GREENFARM_PROGRESS_REPORT.md và tài liệu API cũ: báo cáo 06/10 vẫn ghi chưa có GHN/quên mật khẩu và số test cũ, không còn phản ánh mã nguồn hiện tại.

## Thứ tự thực hiện đề xuất

1. Vá thư viện; rà soát vận chuyển hàng tươi sống.
2. Sửa thông báo email và thời gian; chạy lại luồng mua hàng trên điện thoại và desktop.
3. Kiểm thử IPN/GHN webhook thật trong môi trường thử; hoàn thiện đối soát thanh toán/hoàn tiền và tình huống lỗi.
4. Bổ sung CI/E2E, backup/restore và giám sát.
5. Nếu bán thật: cấu hình và nghiệm thu production riêng; nếu demo: hoàn thiện kịch bản trình bày, dữ liệu mẫu và tài liệu.

Không cần ưu tiên thêm đăng nhập Google, ứng dụng di động hay tính năng AI mới trước các công việc trên.

## Các ca nghiệm thu cần hoàn tất

| Ca | Kết quả mong đợi |
| --- | --- |
| Guest và customer đặt COD | Tổng tiền do backend chốt; tạo đúng một đơn; giữ đúng lô; tra cứu đúng chủ sở hữu. |
| VNPAY thành công và khách đóng trình duyệt | IPN hợp lệ vẫn chốt payment đúng một lần; không phụ thuộc trang Return. |
| Callback VNPAY trùng/sai tiền/sai chữ ký | Không chốt sai, không trừ kho/coupon/điểm hoặc gửi hóa đơn lặp do callback. |
| Callback trễ sau hết hạn hoặc mất callback | Có đối soát và hành động rõ ràng khi tiền gateway khác trạng thái nội bộ. |
| Hoàn tiền thành công/từ chối/timeout/PROCESSING | Không gửi hoàn lặp; xác minh được trạng thái cuối; lịch sử và số tiền khớp gateway. |
| GHN picked → delivering → delivered | Sự kiện GHN cập nhật Railway và giao diện; sự kiện cũ/trùng không làm lùi trạng thái. |
| Đơn GHN COD | Chỉ ghi nhận thu tiền khi có bằng chứng đối soát COD phù hợp, không chỉ vì đơn đã giao. |
| Lô hết hạn trước khi staff xử lý | Không xuất lô hết hạn; phân bổ lại lô hợp lệ hoặc báo thiếu hàng rõ ràng. |
| Hai khách mua lượng tồn cuối | Không bán vượt tồn; hoàn reservation đúng một lần khi hủy. |
| Phân quyền admin/staff/delivery/customer/guest | Không đọc/sửa đơn người khác; mỗi vai trò chỉ thực hiện thao tác được cấp. |
| Email và khôi phục mật khẩu | UI khớp kết quả gửi; token hết hạn/một lần dùng hoạt động đúng. |
| Điện thoại, redeploy và restore | Checkout dùng được trên màn hình nhỏ; ảnh mới còn sau deploy; backup khôi phục được trên môi trường riêng. |
