# Báo cáo tiến độ dự án GreenFarm

**Website thương mại điện tử nông sản — đối chiếu mã nguồn ngày 06/10/2026**

## 1. Cách đọc báo cáo

Báo cáo này đối chiếu nội dung người dùng cung cấp với mã nguồn, migration, cấu hình và tài liệu trong workspace hiện tại. Ký hiệu **Đã có trong mã nguồn** nghĩa là tìm thấy luồng xử lý hoặc giao diện tương ứng; **Có điều kiện / chưa trọn vẹn** nghĩa là cần cấu hình dịch vụ ngoài hoặc còn một bước nghiệp vụ chưa khép kín; **Chưa có** nghĩa là không tìm thấy triển khai trong dự án. Việc có mã nguồn **không đồng nghĩa** đã chạy kiểm thử nghiệm thu, kiểm thử tải hoặc xác nhận vận hành production.

Workspace đang có nhiều thay đổi chưa commit. Trạng thái dưới đây phản ánh **các tệp hiện có**, không chỉ bản commit gần nhất. Kiến trúc chi tiết nằm trong [GREENFARM_CONTEXT.md](GREENFARM_CONTEXT.md).

## 2. Định hướng và kiến trúc

GreenFarm phục vụ khách vãng lai, khách có tài khoản, nhân viên vận hành, nhân viên giao hàng và quản trị viên. Frontend dùng Next.js 16, React 19 và TypeScript; backend dùng Java 21, Spring Boot 4, REST API và Spring Security; dữ liệu lưu trong MySQL 8, thay đổi lược đồ bằng Flyway. Docker Compose phục vụ môi trường local. Backend kiểm tra quyền và quyết định giá, khuyến mãi, tồn kho, thanh toán và chuyển trạng thái đơn; kiểm soát quyền ở giao diện chỉ hỗ trợ trải nghiệm người dùng.

## 3. Checklist chức năng đã đối chiếu

| Nhóm | Trạng thái | Phạm vi xác nhận từ mã nguồn |
| --- | --- | --- |
| Tài khoản và phân quyền | **Đã có trong mã nguồn** | Đăng ký, đăng nhập, đăng xuất, JWT, refresh token xoay vòng, hồ sơ, đổi mật khẩu, ảnh đại diện, khóa đăng nhập tạm thời và các vai trò `admin`, `staff`, `delivery_staff`, `customer`. Xem [authentication.md](docs/api/authentication.md). |
| Danh mục và sản phẩm | **Đã có trong mã nguồn** | Quản lý danh mục, sản phẩm và ảnh; hiển thị công khai, tìm kiếm, lọc, phân trang và ẩn/hiện. Xem `ProductPublicController`, `ProductAdminController` và các trang catalog. |
| Lưu trữ ảnh Cloudinary | **Đã tích hợp, cần chuyển ảnh cũ** | Ảnh catalog, sản phẩm và avatar mới được tải lên Cloudinary khi có đủ ba biến cấu hình; các URL local cũ tiếp tục được phục vụ nếu còn file gốc. Xem [image-storage.md](docs/api/image-storage.md) và migration V23. |
| Giỏ hàng và yêu thích | **Đã có trong mã nguồn** | Thao tác giỏ hàng, wishlist, giỏ khách vãng lai và đồng bộ giỏ khi đăng nhập. Checkout xác nhận lại dữ liệu phía backend. Xem [cart-wishlist.md](docs/api/cart-wishlist.md). |
| Đơn hàng | **Đã có trong mã nguồn** | Xem trước và tạo đơn, lưu thông tin tại thời điểm mua, xem chi tiết/lịch sử trạng thái, phân công giao hàng. UI quản trị tách đơn đang xử lý và lịch sử đơn hoàn tất/đã hủy; các đơn lịch sử vẫn được giữ để tra cứu và báo cáo. Xem [order-operations.md](docs/api/order-operations.md). |
| Thanh toán COD | **Đã có trong mã nguồn** | Tạo đơn COD; ghi nhận thanh toán theo luồng giao hàng. |
| Thanh toán VNPAY | **Có điều kiện** | Tạo URL thanh toán, xử lý và kiểm tra chữ ký/số tiền của phản hồi, xử lý hết hạn thanh toán. Cần cấu hình merchant và môi trường VNPAY để chạy giao dịch thực tế. Xem [payments.md](docs/api/payments.md). |
| Yêu cầu hoàn tiền | **Có điều kiện / chưa trọn vẹn** | Khách có tài khoản gửi yêu cầu cho đơn đủ điều kiện; admin duyệt/từ chối; backend có lời gọi hoàn tiền VNPAY và lưu kết quả/yêu cầu. Trường hợp gateway trả trạng thái `PROCESSING` chưa thấy tác vụ đối soát tự động để kết thúc yêu cầu. Xem `RefundRequestService` và `VnpayRefundService`. |
| Kho và nhà cung cấp | **Đã có trong mã nguồn** | Quản lý nhà cung cấp, lô nhập và hạn sử dụng; giữ/xuất/hoàn hàng theo đơn; phân bổ FEFO, điều chỉnh tồn, lịch sử biến động và xử lý lô hết hạn. Migration [V22](backend/src/main/resources/db/migration/V22__add_inventory_batches_and_movements.sql) và `InventoryService` là bằng chứng chính. |
| Coupon và điểm thưởng | **Đã có trong mã nguồn** | Mã giảm trên đơn, miễn phí vận chuyển, điều kiện/phạm vi/giới hạn, gợi ý mã khi checkout; tích lũy, đổi và hoàn điểm. Xem [coupons.md](docs/api/coupons.md), `CouponEngineService` và `LoyaltyService`. |
| Giao hàng nội bộ | **Đã có trong mã nguồn** | Nhân viên xử lý đơn, phân công nhân viên giao hàng, nhận/giao đơn, báo giao thất bại và ghi lịch sử trạng thái. Đây là quy trình dùng tài khoản `delivery_staff` của GreenFarm. |
| Đánh giá, liên hệ, thông báo | **Đã có trong mã nguồn** | Đánh giá sản phẩm trong đơn đủ điều kiện, gửi/xử lý liên hệ, thông báo và SSE. Nên diễn đạt “đánh giá sản phẩm của đơn đủ điều kiện” thay vì “đã mua/giao” một cách bao quát. |
| Chatbot | **Có điều kiện** | Tư vấn dựa trên catalog công khai; Gemini là phần tích hợp tùy cấu hình. Xem `ChatbotService` và `ChatPublicController`. |
| Dashboard và báo cáo admin | **Đã có trong mã nguồn** | Doanh thu, đơn theo trạng thái, sản phẩm bán chạy, tồn thấp, khách mới, hiệu quả coupon, lọc kỳ và xuất CSV. Quy tắc đếm/chốt số được ghi rõ trong [reports.md](docs/api/reports.md). |
| Khách không đăng nhập | **Đã có trong mã nguồn** | Phiên khách, giỏ và checkout COD/VNPAY, tra cứu bằng email và mã tra cứu, idempotency khi tạo đơn. Xem `GuestCommerceService` và migration V21. |
| Hóa đơn | **Đã có trong mã nguồn; email có điều kiện** | Giao diện hóa đơn/in và luồng gửi email có cơ chế thử lại. Gửi email thực tế cần SMTP được cấu hình. |

### Những điểm cần sửa trong bản báo cáo ban đầu

1. Dòng “xử lý callback, hết hạn thanh toán và **đối soát**” đang khẳng định quá mức. Mã nguồn có xác thực phản hồi và lưu thông tin giao dịch/hoàn tiền, nhưng chưa tìm thấy tác vụ **đối soát VNPAY tự động** hay tra cứu trạng thái định kỳ cho giao dịch còn chờ. Nên tách đối soát thành việc chưa hoàn thiện/chưa xác nhận.
2. Luồng giao hàng hiện tại là **giao hàng nội bộ**. Chưa tìm thấy kết nối API **Giao Hàng Nhanh (GHN)** để tính phí, tạo vận đơn, lấy trạng thái hoặc nhận webhook. Mục GHN ở phần chưa làm là đúng.
3. Tồn thấp và lô sắp hết hạn đã được tính/hiển thị trong quản trị, đồng thời có job xử lý hạn lô. Chưa tìm thấy luồng **gửi cảnh báo chủ động** qua email/push khi chạm ngưỡng. Đề xuất cảnh báo vẫn hợp lý nhưng cần nêu rõ phần sẵn có.
4. “Đã triển khai” ở tất cả các mục trên chỉ là kết luận về mã nguồn. Chưa thể khẳng định mọi vai trò, thiết bị và tình huống lỗi đã được nghiệm thu.

## 4. Chưa triển khai hoặc chưa xác nhận

### Chưa tìm thấy triển khai

- Tích hợp GHN: kết nối tài khoản, địa chỉ/đơn vị giao, phí vận chuyển động, tạo/hủy vận đơn, đồng bộ trạng thái và xử lý webhook.
- Đối soát VNPAY tự động, nhất là các giao dịch hoàn tiền đang `PROCESSING`; cần bổ sung quy trình xác minh trạng thái cuối và xử lý sai lệch.
- Gửi cảnh báo chủ động cho tồn thấp/lô sắp hết hạn. Hiện có số liệu và nhãn trên dashboard.
- Xác minh email, quên mật khẩu và đăng nhập Google. [Tài liệu xác thực](docs/api/authentication.md) xác nhận các mục này ngoài phạm vi hiện tại; chỉ đưa vào kế hoạch nếu có yêu cầu sản phẩm.
- Bộ kiểm thử tự động end-to-end cho toàn bộ chuỗi mua hàng trên frontend. `frontend/package.json` có `lint`, `build`, `quality`, chưa khai báo lệnh E2E.

### Cần kiểm tra thực tế trước khi kết luận

- Chạy nghiệm thu liên thông: guest/customer → checkout COD/VNPAY → coupon/điểm → giữ và xuất kho → giao hàng → hủy/hoàn tiền → hóa đơn/thông báo/báo cáo.
- Đo mức bao phủ và chạy lại test trong môi trường phù hợp. Hiện có **25 tệp test Java**; một test tích hợp về tranh chấp điểm thưởng dùng Testcontainers và được cấu hình bỏ qua khi không có Docker. Con số tệp test không phải tỷ lệ bao phủ và không chứng minh toàn bộ luồng liên thông.
- Kiểm thử tải và đồng thời khi nhiều người đặt cùng mặt hàng, callback thanh toán lặp, hết hạn thanh toán và hoàn tiền.
- Kiểm tra responsive, thao tác trên điện thoại và trình duyệt thực tế; sự tồn tại của CSS responsive chưa thay thế nghiệm thu giao diện.
- Xác nhận triển khai production: cấu hình secrets, HTTPS/CORS, backup và diễn tập restore MySQL/ảnh tải lên, health/metrics, tập trung log, cảnh báo lỗi, kế hoạch nâng cấp/rollback migration. Compose có volume lưu dữ liệu và backend có health endpoint; chưa đủ để kết luận đã có quy trình production hoàn chỉnh.

## 5. Định hướng phát triển đề xuất

1. **Nghiệm thu nghiệp vụ:** lập ca kiểm thử theo năm vai trò và chạy luồng đặt hàng đến giao/hoàn tiền với COD, VNPAY, khách vãng lai, coupon và điểm. Ghi kết quả kèm ngày, môi trường và bằng chứng.
2. **Khép kín các trạng thái thanh toán:** thiết kế đối soát VNPAY và cách xử lý yêu cầu hoàn tiền còn `PROCESSING`, callback trùng hoặc bị trễ. Bổ sung test cho các nhánh lỗi và đồng thời tồn kho.
3. **GHN nếu là yêu cầu bắt buộc:** xác định tài khoản và môi trường thử nghiệm GHN, quy tắc phí/địa chỉ, ánh xạ trạng thái giữa GreenFarm và GHN, quy trình hủy/hoàn và webhook. Chỉ đánh dấu hoàn thành sau khi thử nghiệm với vận đơn thực.
4. **Vận hành:** bổ sung cảnh báo chủ động tồn thấp/lô sắp hết hạn; chuẩn hóa log, metrics, sao lưu/khôi phục và hướng dẫn triển khai. Kiểm thử phục hồi dữ liệu trước khi dùng production.
5. **Trải nghiệm và mở rộng:** đo tải trang, kiểm tra thiết bị thực, tối ưu checkout; chỉ mở rộng báo cáo hao hụt/doanh thu theo nhóm/dự báo sau khi xác định nhu cầu và dữ liệu đủ tin cậy.

## 6. Kết luận dùng cho báo cáo tiến độ

GreenFarm **đã có mã nguồn cho phần lớn chức năng cốt lõi** của website thương mại điện tử nông sản: tài khoản, catalog, giỏ/đơn, COD, tích hợp VNPAY theo cấu hình, giao hàng nội bộ, quản trị, báo cáo, khách vãng lai và quản lý kho theo lô. **GHN và đối soát VNPAY tự động chưa được xác nhận là đã triển khai**; các tích hợp ngoài như VNPAY, SMTP và Gemini cần cấu hình để chạy thực tế. Trước khi tuyên bố hệ thống hoàn thiện hoặc sẵn sàng production, cần nghiệm thu liên thông, kiểm thử tải, xác nhận giao diện trên thiết bị thực và kiểm tra quy trình vận hành/sao lưu.
