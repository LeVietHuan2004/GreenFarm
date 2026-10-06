# Giỏ hàng và yêu thích — Giai đoạn 3

Các endpoint yêu cầu JWT của khách hàng (`CUSTOMER`). Backend lấy tài khoản
từ token, không nhận `userId` từ client. Response dùng envelope `ApiResponse`.

| Method | Endpoint | Nội dung |
| --- | --- | --- |
| GET | `/api/cart` | Giỏ hàng của tài khoản hiện tại |
| POST | `/api/cart/items` | `{ "productId": 1, "quantity": 2 }`, cộng vào số lượng đã có |
| PATCH | `/api/cart/items/{itemId}` | `{ "quantity": 3 }`, thay thế số lượng |
| DELETE | `/api/cart/items/{itemId}` | Xóa một dòng thuộc giỏ của tài khoản |
| DELETE | `/api/cart` | Xóa toàn bộ giỏ của tài khoản |
| POST | `/api/cart/merge` | `{ "items": [{ "productId": 1, "quantity": 2 }] }` |
| GET | `/api/wishlist` | Danh sách yêu thích |
| POST | `/api/wishlist/items` | `{ "productId": 1 }`, lưu sản phẩm, không tạo trùng |
| DELETE | `/api/wishlist/items/{productId}` | Bỏ yêu thích theo mã sản phẩm, gọi lại an toàn |

Giỏ hàng trả `items`, `totalItems` (tổng số lượng), `subtotal`. Mỗi dòng gồm
`id`, `quantity`, `lineTotal`, `product`, `createdAt`, `updatedAt`. Tổng tiền
được tính bằng giá hiện tại trên backend. Yêu thích trả `items` và `count`.

## Quy tắc

- Số lượng là số nguyên dương. Số lượng 0 không có nghĩa là xóa; dùng DELETE.
- Khi thêm, backend kiểm tra **số lượng đang có + số lượng thêm** với tồn kho.
- Khi cập nhật, kiểm tra số lượng mới. Chỉ sản phẩm `in_stock`, stock > 0 được thêm/cập nhật.
- Giỏ hàng không giữ chỗ và không trừ tồn kho. Tồn kho/giá cần được kiểm tra lại
  khi triển khai đặt hàng; giỏ hiện tại cảnh báo nếu số lượng đã vượt tồn kho.
- Có thể xóa sản phẩm đã ẩn/hết hàng khỏi giỏ. Yêu thích cho phép sản phẩm hết hàng,
  nhưng không cho thêm sản phẩm ẩn. Sản phẩm đã lưu trước khi ẩn vẫn có thể bỏ yêu thích.
- Mọi thao tác ghi khóa dòng tài khoản trong transaction để tuần tự hóa thay đổi
  đồng thời, kể cả khi giỏ/danh sách chưa có dòng nào. Unique constraint bảo vệ
  một dòng duy nhất cho mỗi cặp tài khoản/sản phẩm.
- Merge nhận tối đa 100 dòng, gộp các mã sản phẩm lặp lại rồi cộng vào giỏ đã lưu.
  Toàn bộ merge là một transaction; lỗi bất kỳ dòng nào sẽ rollback toàn bộ.
  Endpoint merge là phép cộng, **không idempotent**: không tự retry sau timeout.
- Lỗi dữ liệu trả 400; thiếu token trả 401; sai vai trò trả 403; dòng không thuộc
  tài khoản hoặc sản phẩm không tồn tại trả 404; vượt tồn kho/sản phẩm không khả dụng trả 409.

## Giao diện và đồng bộ

- `/cart`: tăng/giảm hoặc nhập số lượng, xóa từng dòng, xóa hết, tổng tiền và trạng thái rỗng/lỗi.
- `/wishlist`: các sản phẩm đã lưu, bỏ yêu thích và thêm vào giỏ.
- Nút trái tim và thêm giỏ trên thẻ sản phẩm; chi tiết có thêm ô chọn số lượng.
- Header hiển thị số lượng giỏ/yêu thích và liên kết đến hai trang.
- Khách chưa đăng nhập dùng giỏ guest lưu trên backend. Khi đăng nhập/đăng ký,
  giỏ guest được gộp một lần vào giỏ tài khoản; xem `guest-commerce.md`.
- Provider tự lấy giỏ/yêu thích sau đăng nhập, tải lại khi quay lại cửa sổ.
  Khi đổi tài khoản hoặc đăng xuất, dữ liệu hiển thị được reset; response từ
  phiên cũ không ghi đè tài khoản mới. Không lưu bản sao giỏ vào localStorage.
- `/api/cart/merge` vẫn dành cho danh sách item cũ; `/api/cart/merge-guest`
  gộp phiên guest hiện tại và chống cộng trùng khi gọi lại.

## Kiểm thử

`mvn test` chạy `CartServiceTest`, `WishlistServiceTest`, `CommerceValidationTest`
cùng bộ test hiện có. `npm run quality` chạy ESLint và production build frontend.
Để kiểm tra thủ công: đăng nhập khách hàng, thêm cùng sản phẩm hai lần, thay đổi
số lượng, thử vượt tồn kho, lưu/bỏ yêu thích, đăng xuất/đăng nhập và đổi tài khoản.
