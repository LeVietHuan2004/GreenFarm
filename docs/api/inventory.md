# Quản lý kho GreenFarm

## Dữ liệu và quy tắc

- Migration `V22__add_inventory_batches_and_movements.sql` tạo `suppliers`, `inventory_batches`, `order_inventory_allocations`, `inventory_transactions` và chuyển `Product.stock` hiện có thành lô `LEGACY-{productId}`. Lô cũ không có hạn sử dụng vì dữ liệu gốc không lưu thông tin này. Đơn cũ đang `pending` được giữ hàng; đơn đã xử lý được ghi là đã xuất. Không thay đổi số tồn đang bán tại thời điểm chuyển đổi.
- Mỗi lần nhập mới tạo một lô có mã `GF-...` duy nhất và hạn sử dụng bắt buộc. Ngày hết hạn bằng hôm nay vẫn bán được; quá ngày đó thì không được dùng. Ngưỡng cảnh báo mặc định 3 ngày, cấu hình bằng `INVENTORY_EXPIRY_WARNING_DAYS`.
- Sản phẩm còn từ 1 đến 10 đơn vị khả dụng được gắn nhãn `LOW_STOCK`.
- `remainingQuantity` là số lượng vật lý còn trong lô; `reservedQuantity` là phần đang giữ cho đơn chưa xử lý. Số khả dụng = tổng `remainingQuantity - reservedQuantity` của các lô chưa hết hạn. `Product.stock` là bản sao số khả dụng để catalog và giỏ hàng đang có tiếp tục hoạt động. Tác vụ mỗi phút cập nhật số này khi có lô hết hạn (`app.inventory.expiry-scan-ms`).
- Tạo đơn giữ lô theo FEFO trong transaction, có khóa ghi theo sản phẩm rồi theo lô. Chuyển đơn sang `processing` trừ đúng các lô đã giữ và ghi `EXPORT`. Nếu lô đã hết hạn trong lúc chờ xử lý, hệ thống phân bổ lại vào lô còn hạn. Hủy đơn giải phóng phần giữ hoặc hoàn chính xác vào lô đã xuất, mỗi phân bổ chỉ được hoàn một lần.
- Tồn kho chỉ được tăng bằng nhập lô hoặc điều chỉnh có lý do; không thể thay `stock` từ API Sản phẩm. Điều chỉnh không được làm âm số tồn hoặc xâm phạm phần đã giữ cho đơn hàng. Mọi thay đổi đều ghi lịch sử gồm lô, loại, trước/sau, người thực hiện và tham chiếu đơn.

## API (JWT admin)

| Phương thức | Đường dẫn | Mục đích |
| --- | --- | --- |
| GET | `/api/admin/inventory/summary` | Số sản phẩm còn hàng, sắp hết; lô sắp hết hạn, hết hạn |
| GET | `/api/admin/inventory?search=&status=&expiry=&page=&size=` | Tồn theo sản phẩm. `status`: `IN_STOCK`, `LOW_STOCK`, `OUT_OF_STOCK`; `expiry`: `VALID`, `EXPIRING`, `EXPIRED` |
| GET | `/api/admin/inventory/{productId}` | Sản phẩm và toàn bộ lô |
| GET | `/api/admin/inventory/batches?productId=&search=&expiry=&page=&size=` | Danh sách lô và hạn dùng |
| GET | `/api/admin/inventory/transactions?productId=&type=&search=&from=&to=&page=&size=` | Lịch sử. Ngày theo `yyyy-MM-dd` |
| POST | `/api/admin/inventory/import` | Nhập lô mới |
| POST | `/api/admin/inventory/adjust` | Điều chỉnh lô |
| GET/POST | `/api/admin/suppliers` | Danh sách và thêm nhà cung cấp |
| GET/PUT/DELETE | `/api/admin/suppliers/{id}` | Chi tiết, sửa, ngừng sử dụng nhà cung cấp |

Ví dụ nhập kho:

```json
{"productId": 12, "quantity": 100, "importPrice": 15000, "manufactureDate": "2026-10-01", "expiryDate": "2026-10-15", "supplierId": null, "note": "Thu hoạch đợt 1"}
```

Ví dụ ghi nhận hỏng 4 đơn vị:

```json
{"batchId": 51, "quantityChange": -4, "type": "DAMAGED", "reason": "Dập trong vận chuyển"}
```

`type` điều chỉnh gồm `ADJUSTMENT`, `DAMAGED`, `EXPIRED`, `RETURN`. Số âm giảm tồn, số dương tăng tồn; `DAMAGED`/`EXPIRED` chỉ giảm và `RETURN` chỉ tăng. Cả SecurityFilterChain và controller đều yêu cầu role `ADMIN`.

## Kiểm thử thủ công

1. Mở `/admin/inventory`, xem bốn chỉ số và các tab.
2. Tạo nhà cung cấp, nhập hai lô cùng sản phẩm, lô gần hạn 10 và lô xa hạn 20.
3. Đặt đơn 15: tab lô hiển thị phần giữ 10 và 5. Chuyển đơn sang `processing`: lô đầu còn 0, lô sau còn 15; lịch sử có hai dòng `EXPORT`.
4. Hủy đơn: hai lô trở về 10 và 20. Thử nhập lô đã hết hạn, đặt vượt tồn, điều chỉnh quá số khả dụng và truy cập API bằng tài khoản khách; các yêu cầu phải bị từ chối.
5. Thử lô còn đúng 3 ngày: xuất hiện cảnh báo sắp hết hạn. Qua ngày hết hạn, lô bị loại khỏi số khả dụng.

Backend dùng Flyway khi khởi động. Để áp dụng trên môi trường Docker Compose đang chạy, cần sao lưu database rồi build lại image backend và tạo lại container backend theo quy trình triển khai của dự án; chỉ sửa file nguồn không cập nhật container đang chạy.
