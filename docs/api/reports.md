# Báo cáo kinh doanh quản trị

Các endpoint yêu cầu tài khoản `admin`:

- `GET /api/admin/reports?from=YYYY-MM-DD&to=YYYY-MM-DD`: dữ liệu dashboard.
- `GET /api/admin/reports/export.csv?from=YYYY-MM-DD&to=YYYY-MM-DD`: xuất cùng kỳ dữ liệu ra CSV UTF-8.

Nếu không truyền ngày, kỳ mặc định là 30 ngày gần nhất tính cả hôm nay. `from` và `to` đều được tính trọn ngày; kỳ báo cáo tối đa 2 năm. Dashboard có lựa chọn nhanh 7, 30 và 90 ngày.

Báo cáo gồm doanh thu thực nhận, số đơn đã thanh toán, giá trị đơn trung bình, doanh thu theo ngày, sản phẩm bán chạy, đơn theo trạng thái, khách hàng mới, tồn kho thấp và hiệu quả coupon.

- **Doanh thu** dựa trên `payments.status = completed` và `paid_at` trong kỳ; đơn đã hoàn tiền không còn nằm trong doanh thu. COD được ghi nhận khi đã thu tiền lúc giao hàng.
- **Đơn theo trạng thái** đếm các đơn *được tạo trong kỳ* theo trạng thái hiện tại, bao gồm đơn khách vãng lai. Vì vậy tổng này có thể khác số đơn đã thanh toán trong kỳ.
- **Sản phẩm bán chạy** chỉ tính đơn đã thanh toán trong kỳ. Doanh số từng sản phẩm là giá dòng hàng trước khi phân bổ coupon và điểm.
- **Khách hàng mới** đếm tài khoản có vai trò `customer` được tạo trong kỳ, không tính khách vãng lai.
- **Tồn kho thấp** là sản phẩm không ẩn, còn từ 1 đến 10 đơn vị tại thời điểm mở báo cáo. Danh sách hiển thị tối đa 10 sản phẩm ít hàng nhất; `lowStockCount` là tổng số sản phẩm thỏa điều kiện.
- **Hiệu quả coupon** đếm lượt giảm giá trên đơn đã thanh toán trong kỳ, gồm coupon còn giữ lượt và đã sử dụng. Đơn hoàn tiền không được tính. Mỗi loại coupon trên một đơn là một lượt; tổng giảm giá cộng từ các lượt này. API và CSV trả toàn bộ mã có phát sinh; dashboard hiển thị 10 mã nhiều lượt nhất.
