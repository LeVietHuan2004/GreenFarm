# Nội dung catalog ngày 17/11/2026

Đã áp dụng trực tiếp trên database Railway đang phục vụ GreenFarm và kiểm tra qua
API public: 42 sản phẩm có mô tả tiếng Việt/Anh mới, đầy đủ đơn vị ở hai ngôn ngữ.
Không cần deploy lại ứng dụng để đọc dữ liệu mới.

- `product-copy-2026-11-17.json`: nội dung đọc được để rà soát từng sản phẩm.
- `refresh-product-copy-2026-11-17.sql`: bản cập nhật dữ liệu có điều kiện, không phải
  Flyway migration. Chỉ cập nhật khi cả 42 sản phẩm còn khớp ID/slug/tên, nội dung
  cũ, đơn vị và giá dự kiến. Nếu không khớp sẽ không cập nhật sản phẩm nào; kiểm tra
  kết quả `matchedExpectedProducts`/`updatedProducts` thay vì chạy lại mù quáng.

14 đơn vị tiếng Anh trống đã được bổ sung; `Bich` → `pouch`, `Loc` → `multipack`,
`lóc` → `lốc`. Một lô kho của sữa TH được sửa cùng lỗi chính tả đơn vị. Giá, tồn kho,
trạng thái và dữ liệu đơn hàng lịch sử giữ nguyên.

MySQL local tại localhost:3307 không kết nối được ở thời điểm thực hiện, nên chưa
áp dụng lên `veggie_main`. Có thể dùng SQL này sau khi local hoạt động và đã sao lưu;
script có guard nên sẽ không ghi đè khi dữ liệu local đã khác bản dự kiến.

Bản trước khi sửa và báo cáo xác minh lưu trong thư mục `.tmp-railway-migration`
được Git bỏ qua. Khôi phục nội dung cần đối chiếu lại các trường hiện tại để không
ghi đè chỉnh sửa mới hơn.

Các quy cách còn cần chủ shop xác nhận: sản phẩm tên có 200g/250g/500g đang để đơn vị
bán kg; trứng gà tên hộp 10 quả đang tính theo quả; sữa Dutch Lady tên dạng bịch đang
để hộp. Không tự đổi các đơn vị này vì có thể làm sai cách hiểu giá và số lượng.
