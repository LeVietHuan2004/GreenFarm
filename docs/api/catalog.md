# Module catalog sản phẩm

## Phạm vi giai đoạn 2

Module sử dụng trực tiếp ba bảng có trong baseline cũ: `categories`, `products`
và `product_images`. Trạng thái sản phẩm gồm:

- `in_stock`: đang bán và còn hàng.
- `out_of_stock`: đang hiển thị nhưng tạm hết hàng.
- `hidden`: bị ẩn khỏi toàn bộ API và UI công khai.

Tất cả response thành công dùng envelope chung `success`, `message`, `data` và
`timestamp`. Danh sách sản phẩm trả `PageResponse` với `content`, `page`, `size`,
`totalElements`, `totalPages`, `first` và `last`.

## API công khai

Không yêu cầu JWT.

| Method | Endpoint | Chức năng |
| --- | --- | --- |
| `GET` | `/api/public/categories` | Danh sách danh mục và số sản phẩm đang hiển thị |
| `GET` | `/api/public/categories/{slug}` | Chi tiết danh mục |
| `GET` | `/api/public/products` | Tìm kiếm, lọc và phân trang sản phẩm |
| `GET` | `/api/public/products/{slug}` | Chi tiết sản phẩm và danh sách ảnh |

Query của danh sách sản phẩm:

| Tham số | Kiểu | Ý nghĩa |
| --- | --- | --- |
| `search` | string | Tìm trong tên và mô tả |
| `category` | string | Slug danh mục |
| `minPrice` | decimal | Giá tối thiểu, không âm |
| `maxPrice` | decimal | Giá tối đa, không âm |
| `status` | string | `in_stock` hoặc `out_of_stock` |
| `page`, `size`, `sort` | pageable | Phân trang; `size` tối đa 50 |

Ví dụ:

```http
GET /api/public/products?search=rau&category=rau-cu&minPrice=5000&maxPrice=50000&status=in_stock&page=0&size=12
```

## API quản trị danh mục

Yêu cầu JWT có permission `manage_categories`.

| Method | Endpoint | Chức năng |
| --- | --- | --- |
| `GET` | `/api/admin/categories` | Danh sách kèm tổng sản phẩm, gồm cả sản phẩm ẩn |
| `POST` | `/api/admin/categories` | Tạo danh mục |
| `PUT` | `/api/admin/categories/{categoryId}` | Cập nhật danh mục |
| `DELETE` | `/api/admin/categories/{categoryId}` | Xóa danh mục rỗng |

Slug là tùy chọn; backend tự chuẩn hóa tiếng Việt và thêm hậu tố nếu bị trùng.
Danh mục đang có sản phẩm trả `409 CATEGORY_IN_USE` khi xóa.

## API quản trị sản phẩm và ảnh

Yêu cầu JWT có permission `manage_products`.

| Method | Endpoint | Chức năng |
| --- | --- | --- |
| `GET` | `/api/admin/products` | Tìm kiếm/lọc tất cả sản phẩm, gồm `hidden` |
| `POST` | `/api/admin/products` | Tạo sản phẩm |
| `PUT` | `/api/admin/products/{productId}` | Cập nhật sản phẩm |
| `PATCH` | `/api/admin/products/{productId}/status` | Đổi trạng thái/ẩn sản phẩm |
| `POST` | `/api/admin/products/{productId}/images` | Thêm URL ảnh |
| `DELETE` | `/api/admin/products/{productId}/images/{imageId}` | Xóa ảnh |

Payload sản phẩm chính:

```json
{
  "name": "Cà chua bi",
  "slug": "ca-chua-bi",
  "categoryId": 1,
  "description": "Cà chua tươi chọn lọc",
  "price": 35000,
  "stock": 20,
  "status": "in_stock",
  "unit": "kg"
}
```

Khi tồn kho bằng 0, trạng thái `in_stock` được tự chuẩn hóa thành
`out_of_stock`. Ảnh được quản lý bằng payload `{ "image": "https://..." }` để
tương thích dữ liệu Cloudinary và đường dẫn ảnh cũ.

## UI

- `/`: trang chủ catalog và sản phẩm mới.
- `/products`: tìm kiếm, lọc và phân trang.
- `/categories/{slug}`: sản phẩm theo danh mục.
- `/products/{slug}`: chi tiết và gallery ảnh.
- `/admin/products`: quản lý sản phẩm, trạng thái và ảnh.
- `/admin/categories`: quản lý danh mục.
- `/admin`: dashboard thống kê người dùng và sức khỏe catalog bằng dữ liệu thật.
- `/admin/users`: tìm kiếm, lọc, đổi vai trò và trạng thái tài khoản.

Các trang quản trị dùng chung sidebar responsive; menu đơn hàng và giao hàng chỉ
hiển thị trạng thái giai đoạn kế tiếp cho đến khi các module tương ứng được triển
khai.
