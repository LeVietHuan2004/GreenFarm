# CHƯƠNG 3 PHÂN TÍCH THIẾT KẾ HỆ THỐNG

Chương này xác định yêu cầu và thiết kế hệ thống kinh doanh nông sản trực tuyến GreenFarm. Nội dung được tổ chức từ phạm vi, tác nhân và quy tắc nghiệp vụ đến use case, kiến trúc, cơ sở dữ liệu, API và giao diện. Thiết kế tập trung vào tính nhất quán giữa đơn hàng, thanh toán và tồn kho; đồng thời hỗ trợ khách vãng lai, khách hàng có tài khoản và các bộ phận vận hành.

## 3.1 Phạm vi và tác nhân của hệ thống

### 3.1.1 Phạm vi hệ thống

GreenFarm phục vụ hoạt động bán hàng của một đơn vị kinh doanh nông sản. Khách có thể tìm sản phẩm, đặt hàng và theo dõi đơn; bộ phận vận hành quản lý hàng hóa, kho, giao nhận và chăm sóc khách hàng. Hệ thống không tổ chức gian hàng cho nhiều nhà bán độc lập.

Bảng 3.1. Phạm vi của hệ thống GreenFarm
| Khía cạnh | Nội dung trong phạm vi | Giới hạn |
| --- | --- | --- |
| Mua sắm | Danh mục, tìm kiếm, bộ lọc, chi tiết, nguồn gốc, chứng nhận và bảo quản; giỏ hàng, yêu thích | Không có sàn nhiều nhà bán hoặc đấu giá |
| Tài khoản | Đăng ký, đăng nhập theo vai trò, hồ sơ, avatar, đổi và khôi phục mật khẩu | Chưa có đăng nhập Google hoặc quy trình xác minh email |
| Đơn hàng | Địa chỉ, xem trước tiền, đặt hàng tài khoản/khách, hóa đơn, lịch sử, hủy và yêu cầu hoàn tiền | Khách vãng lai không có đầy đủ quyền lợi tài khoản |
| Thanh toán | COD, VNPAY, xác minh callback, hết hạn và hoàn tiền | VNPAY sử dụng cấu hình sandbox của đề tài |
| Kho và nguồn cung | Nhà cung cấp, nhập lô, hạn dùng, giữ hàng, xuất FEFO, điều chỉnh và lịch sử | Không có quản lý nhiều kho hoặc kế toán giá vốn đầy đủ |
| Giao nhận | Phân công giao nội bộ; báo phí, vận đơn, sự kiện và đối soát COD GHN | GHN staging; một kiện ước tính và chỉ hàng đủ điều kiện |
| Khuyến mại | Coupon đơn/sản phẩm/danh mục, miễn phí vận chuyển, điểm thưởng | Không có ví tiền hoặc quy đổi điểm thành tiền mặt |
| Hỗ trợ và quản trị | Đánh giá, liên hệ, thông báo SSE, báo cáo/CSV và chatbot Gemini | Chatbot chỉ tư vấn dữ liệu công khai, không thao tác giao dịch |
| Triển khai | Frontend trên Vercel; backend và MySQL trên Railway; ảnh trên Cloudinary | Mô hình triển khai đích của báo cáo; không đặt ra số liệu SLA chưa đo |

### 3.1.2 Các tác nhân của hệ thống

Các tác nhân nghiệp vụ gồm năm nhóm người dùng. Dịch vụ ngoài đóng vai trò tác nhân hỗ trợ; MySQL là thành phần lưu trữ bên trong kiến trúc, không phải người dùng của use case.

Bảng 3.2. Các tác nhân của hệ thống
| Mã | Tác nhân | Vai trò và phạm vi truy cập |
| --- | --- | --- |
| ACT-01 | Khách vãng lai | Xem nội dung công khai, giỏ theo phiên, đặt COD/VNPAY, tra cứu đơn bằng thông tin xác minh, gửi liên hệ và chat |
| ACT-02 | Khách hàng | Vai trò customer; sử dụng mua sắm, quản lý dữ liệu cá nhân, đánh giá, điểm và yêu cầu hoàn tiền |
| ACT-03 | Nhân viên | Vai trò staff; xử lý đơn, phân công giao nội bộ, sản phẩm theo quyền, liên hệ và điều chỉnh kho được phép |
| ACT-04 | Nhân viên giao hàng | Vai trò delivery_staff; chỉ nhận và cập nhật đơn nội bộ được phân công |
| ACT-05 | Quản trị viên | Vai trò admin; quản lý tài khoản, catalog, coupon, nguồn cung, kho, đơn, vận đơn, hoàn tiền và báo cáo |
| ACT-06 | VNPAY | Tiếp nhận thanh toán, gửi Return/IPN và xử lý yêu cầu hoàn tiền có chữ ký |
| ACT-07 | GHN | Báo phí, tạo/tra cứu/hủy/in vận đơn; gửi sự kiện giao hàng và chuyển tiền COD |
| ACT-08 | Dịch vụ hỗ trợ | SMTP gửi email; Cloudinary lưu ảnh; Gemini cung cấp phản hồi tư vấn |

### 3.1.3 Sơ đồ ngữ cảnh

Hình ngữ cảnh thể hiện GreenFarm là đầu mối xử lý nghiệp vụ. Trình duyệt gọi API của hệ thống; khóa VNPAY, GHN, SMTP, Cloudinary và Gemini được sử dụng tại backend.

![Hình 3.1. Sơ đồ ngữ cảnh hệ thống GreenFarm](figures_chuong3/context.png)

Hình 3.1. Sơ đồ ngữ cảnh hệ thống GreenFarm

## 3.2 Phân tích yêu cầu hệ thống

### 3.2.1 Yêu cầu chức năng

Các yêu cầu được nhóm theo mục tiêu sử dụng. Mức ưu tiên Cao dành cho chức năng hình thành giao dịch và bảo vệ dữ liệu; mức Trung bình dành cho chức năng hỗ trợ trải nghiệm và quản trị.

Bảng 3.3. Yêu cầu chức năng của GreenFarm
| Mã | Yêu cầu | Tác nhân | Xử lý và kết quả chính | Ưu tiên |
| --- | --- | --- | --- | --- |
| FR-01 | Tài khoản và phiên | Khách, mọi vai trò | Đăng ký customer; đăng nhập đúng cổng; refresh xoay vòng; đăng xuất thu hồi phiên | Cao |
| FR-02 | Hồ sơ và mật khẩu | Người đã đăng nhập | Cập nhật hồ sơ/ảnh; đổi mật khẩu; khôi phục qua email bằng token dùng một lần | Cao |
| FR-03 | Catalog | Mọi khách | Tìm/lọc/phân trang; xem danh mục, ảnh, giá, đơn vị, nguồn gốc, chứng nhận và bảo quản | Cao |
| FR-04 | Giỏ và yêu thích | Khách, khách hàng | Giỏ tài khoản hoặc phiên khách; kiểm tra số lượng/tồn; yêu thích dành cho customer | Cao |
| FR-05 | Gộp giỏ khách | Khách hàng | Gộp sau đăng nhập; khóa và kiểm tra cả giỏ; không cộng lặp cùng phiên | Cao |
| FR-06 | Địa chỉ và báo phí | Khách, khách hàng | Địa chỉ mặc định; chọn giao nội bộ/GHN; kiểm tra tuyến và điều kiện vận chuyển | Cao |
| FR-07 | Checkout và đặt hàng | Khách, khách hàng | Tính tiền tại server; giữ kho/coupon; lưu đơn, dòng hàng, thanh toán và lịch sử | Cao |
| FR-08 | Thanh toán | Khách, khách hàng, VNPAY | COD hoặc URL VNPAY; xác minh chữ ký/mã/số tiền; xử lý lặp và hết hạn | Cao |
| FR-09 | Theo dõi và hóa đơn | Khách, khách hàng | Xem đơn đã xác thực; trạng thái, lịch sử, vận đơn; xem/in hóa đơn và gửi email | Cao |
| FR-10 | Hủy và hoàn tiền | Khách hàng, admin | Hủy đơn chờ chưa trả tiền; gửi/duyệt yêu cầu; hoàn COD hoặc qua VNPAY | Cao |
| FR-11 | Vận hành đơn | Staff, admin | Xác nhận, chuẩn bị, sẵn sàng giao, giao lại; kiểm tra chuyển trạng thái | Cao |
| FR-12 | Giao hàng nội bộ | Staff, admin, giao hàng | Phân công người đang hoạt động; nhận đơn; giao thành công/thất bại và ghi lý do | Cao |
| FR-13 | Vận đơn GHN | Admin, GHN | Tạo, đồng bộ, hủy, in; nhận webhook; đối soát COD; chặn sự kiện cũ/lặp | Cao |
| FR-14 | Catalog quản trị | Admin, staff theo quyền | Quản lý danh mục/sản phẩm/ảnh; trạng thái bán, xuất xứ và thông số gói hàng | Cao |
| FR-15 | Nguồn cung và nhập kho | Admin | Quản lý nhà cung cấp; nhập lô có hạn dùng, giá nhập và người thực hiện | Cao |
| FR-16 | Vận hành kho | Admin, staff | Xem tồn/lô; FEFO; điều chỉnh, hư hỏng, hết hạn, trả hàng và lịch sử | Cao |
| FR-17 | Coupon và miễn phí giao | Admin, người mua | Thời hạn, phạm vi, lượt dùng, mức tối thiểu/tối đa; giữ/dùng/trả lượt theo đơn | Cao |
| FR-18 | Điểm GreenFarm Rewards | Khách hàng | Xem số dư/lịch sử; nhận điểm đơn/đánh giá; dùng điểm và hoàn/điều chỉnh theo giao dịch | Trung bình |
| FR-19 | Đánh giá sản phẩm | Khách hàng | Chỉ sản phẩm đã nhận; 1–5 sao; một đánh giá mỗi tài khoản/sản phẩm, có thể sửa | Trung bình |
| FR-20 | Liên hệ và thông báo | Mọi khách, vận hành | Gửi/phản hồi/đóng liên hệ; thông báo riêng, số chưa đọc và SSE | Trung bình |
| FR-21 | Tư vấn AI | Mọi khách, Gemini | Hội thoại theo tài khoản/phiên; catalog công khai; che dữ liệu; có trả lời dự phòng | Trung bình |
| FR-22 | Quản trị người dùng | Admin | Tìm/lọc, đổi vai trò/trạng thái; xem quyền; bảo vệ tài khoản admin đang thao tác | Cao |
| FR-23 | Báo cáo kinh doanh | Admin | Doanh thu, đơn, khách mới, hàng bán chạy, tồn thấp, coupon; lọc ngày và xuất CSV | Trung bình |

### 3.2.2 Yêu cầu phi chức năng

Các tiêu chí dưới đây là yêu cầu thiết kế và kiểm chứng, không được dùng thay cho kết quả đo hiệu năng hoặc nghiệm thu triển khai.

Bảng 3.4. Yêu cầu phi chức năng
| Mã | Nhóm | Yêu cầu và tiêu chí kiểm chứng | Ưu tiên |
| --- | --- | --- | --- |
| NFR-01 | Xác thực | JWT hợp lệ, trạng thái tài khoản và phiên bản thông tin xác thực được kiểm tra; thiếu/sai token trả 401 | Cao |
| NFR-02 | Phân quyền | Kiểm tra role/permission tại backend; kiểm tra chủ sở hữu đơn, địa chỉ, giỏ và thông báo; trái quyền bị từ chối | Cao |
| NFR-03 | Bảo vệ bí mật | BCrypt cho mật khẩu; hash token nhạy cảm; khóa dịch vụ chỉ ở backend; không trả bí mật trong lỗi | Cao |
| NFR-04 | Toàn vẹn giao dịch | Đặt/hủy đơn đồng bộ kho, coupon, điểm và payment bằng transaction; lỗi giữa chừng phải rollback | Cao |
| NFR-05 | Đồng thời và chống lặp | Khóa tài nguyên quan trọng, unique nghiệp vụ, khóa checkout khách và callback idempotent | Cao |
| NFR-06 | Tính đúng tiền | Tính bằng BigDecimal tại backend; kiểm tra tiền VNPAY; không dùng tổng tiền client làm nguồn quyết định | Cao |
| NFR-07 | Khả năng phục hồi | Email có retry; AI có fallback; thông báo được lưu để tải lại khi SSE gián đoạn | Trung bình |
| NFR-08 | Hiệu năng | Phân trang danh sách, chỉ mục tìm kiếm/quan hệ, cache dữ liệu frontend; đo tải trước khi kết luận tốc độ | Trung bình |
| NFR-09 | Giao diện | Responsive; có loading, rỗng, lỗi và phản hồi thao tác; không khóa luồng khi thiếu cấu hình dịch vụ tùy chọn | Trung bình |
| NFR-10 | Bảo trì | Controller–Service–Repository, DTO/validation, lỗi chuẩn hóa và Flyway cho thay đổi schema | Trung bình |
| NFR-11 | Vận hành | HTTPS, biến môi trường, log và health endpoint; dữ liệu MySQL dùng lưu trữ bền vững; quy trình sao lưu/khôi phục | Cao |
| NFR-12 | Giới hạn AI | Không truy xuất dữ liệu cá nhân hoặc thực hiện đặt hàng/thanh toán; phản hồi sản phẩm đối chiếu catalog | Cao |

### 3.2.3 Quy tắc nghiệp vụ

Bảng 3.5. Các quy tắc nghiệp vụ trọng tâm
| Mã | Quy tắc |
| --- | --- |
| BR-01 | Tài khoản đăng ký nhận role customer. Sai mật khẩu 5 lần liên tiếp khóa tạm 15 phút theo mặc định; đăng nhập khác cổng vai trò bị từ chối. |
| BR-02 | Refresh token mặc định 30 ngày và xoay vòng. Token khôi phục mật khẩu hết hạn sau 30 phút, dùng một lần; đổi/đặt lại mật khẩu làm mất hiệu lực thông tin xác thực cũ. |
| BR-03 | Giá và trạng thái bán được đọc lại khi checkout. Sản phẩm ẩn không được phục vụ qua catalog công khai; không sửa trực tiếp tồn bằng API sản phẩm. |
| BR-04 | Tồn khả dụng của một sản phẩm bằng tổng remaining_quantity − reserved_quantity của lô chưa hết hạn. Ngày hết hạn bằng ngày hiện tại vẫn hợp lệ; lô cũ chưa biết hạn được xử lý riêng. |
| BR-05 | Đặt hàng giữ lô theo FEFO; chuyển processing mới xuất kho. Nếu lô giữ đã hết hạn thì phân bổ lại; hủy giải phóng/hoàn đúng lô một lần. |
| BR-06 | Tồn thấp là 1–10 đơn vị khả dụng. Cảnh báo gần hạn mặc định 3 ngày; tác vụ định kỳ đồng bộ tồn khi lô hết hạn. Điều chỉnh không được làm âm tồn hoặc xâm phạm phần đang giữ. |
| BR-07 | Mỗi đơn dùng tối đa một coupon ORDER_DISCOUNT và một FREESHIP, có thể kết hợp điểm. Phạm vi ALL/CATEGORY/PRODUCT và hạn mức được kiểm tra lại khi tạo đơn. |
| BR-08 | Tiền thanh toán = tạm tính − giảm hàng − giảm điểm + phí giao − giảm phí giao. Các khoản giảm được giới hạn theo điều kiện để không tạo tổng tiền âm. |
| BR-09 | Mỗi 10.000 đồng giá trị đơn hợp lệ nhận 1 điểm; 1 điểm đổi 100 đồng. Mức đổi tối đa 50% tạm tính sau coupon hàng hóa, chưa cộng phí giao; điểm đánh giá không được cộng lặp. |
| BR-10 | VNPAY chờ thanh toán tối đa 15 phút; chỉ chấp nhận kết quả khớp chữ ký, mã tham chiếu và số tiền. Đơn trực tuyến chưa thanh toán không được xác nhận xử lý. |
| BR-11 | Giao nội bộ chỉ dành cho nhân viên đang hoạt động được phân công; phải nhận đơn trước khi đi giao. Giao thất bại cần lý do và có thể chuẩn bị giao lại. |
| BR-12 | GHN chỉ áp dụng khi toàn bộ giỏ đủ điều kiện; không tự gán khả năng vận chuyển cho hàng tươi/sống/đông lạnh. Mã khách hàng GF-<orderId> dùng để tránh tạo vận đơn trùng. |
| BR-13 | Giao thành công nội bộ xác nhận COD; GHN delivered chưa đồng nghĩa đã chuyển tiền COD. Chỉ ghi thanh toán GHN khi thông tin chuyển tiền và số tiền phù hợp. |
| BR-14 | Khách hàng chỉ tự hủy đơn pending chưa thanh toán. Đơn đã thanh toán cần quy trình hoàn tiền; yêu cầu hoàn tiền đang xử lý không được tạo lặp. |
| BR-15 | Chỉ khách đã nhận sản phẩm được đánh giá; mỗi cặp tài khoản–sản phẩm có một đánh giá. Khách vãng lai không nhận điểm, không đánh giá hoặc tự gửi yêu cầu hoàn tiền theo API tài khoản. |
| BR-16 | Doanh thu báo cáo dựa vào payment completed và paid_at trong kỳ; đơn hoàn tiền toàn phần không còn tính doanh thu. Doanh số sản phẩm là giá dòng hàng trước phân bổ ưu đãi, không đồng nhất với doanh thu thuần. |
| BR-17 | Giao nội bộ tiêu chuẩn thu 30.000 đồng, miễn phí khi tạm tính từ 500.000 đồng. Luồng khách có thêm express 50.000 đồng; miễn phí theo ngưỡng áp dụng cho standard. Phí GHN lấy từ báo giá, không dùng biểu phí nội bộ. |
| BR-18 | GHN ước tính một kiện, tối đa 50 kg và 200 cm mỗi chiều; tiền VND nguyên, COD tối đa 50 triệu đồng. Chưa hỗ trợ chia kiện hoặc thay tiền COD sau đặt. |

## 3.3 Phân tích use case

### 3.3.1 Sơ đồ use case tổng quát

Use case mô tả mục tiêu của từng tác nhân; xác thực và quyền sở hữu là tiền điều kiện của chức năng bảo vệ. Sơ đồ không gắn quan hệ include vào mọi use case để tránh che khuất các mục tiêu nghiệp vụ. Các bước tính tiền, giữ kho và xác minh thanh toán được thể hiện chi tiết trong đặc tả và biểu đồ tuần tự.

![Hình 3.2. Sơ đồ use case tổng quát của GreenFarm](figures_chuong3/use_overview.png)

Hình 3.2. Sơ đồ use case tổng quát của GreenFarm

### 3.3.2 Use case mua sắm và vận hành

Khách vãng lai có thể hoàn thành giao dịch bằng phiên khách; khách hàng có thêm dữ liệu cá nhân, điểm và quyền hậu mãi. Staff xử lý đơn và kho trong phạm vi cho phép, còn quản trị viên quản lý cấu hình nghiệp vụ và quyết định hoàn tiền. Nhân viên giao hàng chỉ thao tác trên các đơn nội bộ được giao cho mình.

![Hình 3.3. Sơ đồ use case của khách vãng lai và khách hàng](figures_chuong3/use_shopping.png)

Hình 3.3. Sơ đồ use case của khách vãng lai và khách hàng

![Hình 3.4. Sơ đồ use case của quản trị và nhân viên vận hành](figures_chuong3/use_operations.png)

Hình 3.4. Sơ đồ use case của quản trị và nhân viên vận hành

### 3.3.3 Danh sách use case

Bảng 3.6. Danh sách use case và yêu cầu liên quan
| Mã | Tên use case | Yêu cầu |
| --- | --- | --- |
| UC-01 | Đăng ký tài khoản | FR-01 |
| UC-02 | Đăng nhập đăng xuất và làm mới phiên | FR-01 |
| UC-03 | Quản lý hồ sơ và khôi phục mật khẩu | FR-02 |
| UC-04 | Tra cứu catalog và chi tiết sản phẩm | FR-03 |
| UC-05 | Quản lý giỏ hàng và yêu thích | FR-04 |
| UC-06 | Gộp giỏ khách vào tài khoản | FR-05 |
| UC-07 | Quản lý địa chỉ và đặt hàng tài khoản | FR-06, FR-07, FR-17, FR-18 |
| UC-08 | Đặt và tra cứu đơn không cần tài khoản | FR-06, FR-07, FR-09 |
| UC-09 | Thanh toán và nhận hóa đơn | FR-08, FR-09 |
| UC-10 | Hủy đơn và xử lý hoàn tiền | FR-10 |
| UC-11 | Tiếp nhận và chuẩn bị đơn hàng | FR-11 |
| UC-12 | Phân công nhận và cập nhật giao nội bộ | FR-12 |
| UC-13 | Quản lý vận đơn và sự kiện GHN | FR-13 |
| UC-14 | Quản lý danh mục sản phẩm và ảnh | FR-14 |
| UC-15 | Quản lý nhà cung cấp và nhập lô | FR-15 |
| UC-16 | Theo dõi điều chỉnh và phân bổ kho | FR-16 |
| UC-17 | Quản lý và sử dụng coupon | FR-17 |
| UC-18 | Đánh giá sản phẩm và điểm thưởng | FR-18, FR-19 |
| UC-19 | Liên hệ hỗ trợ và nhận thông báo | FR-20 |
| UC-20 | Tư vấn mua sắm bằng chatbot | FR-21 |
| UC-21 | Quản lý tài khoản vai trò và trạng thái | FR-22 |
| UC-22 | Xem báo cáo và xuất dữ liệu kinh doanh | FR-23 |


### 3.3.4 Đặc tả các use case

Các đặc tả dưới đây dùng cùng biểu mẫu Thuộc tính–Nội dung của báo cáo tham chiếu. Những thao tác cùng mục tiêu như thêm/sửa/xóa hoặc tải lại dữ liệu được gộp trong một use case; các giao dịch có quy tắc riêng vẫn được tách để thể hiện điều kiện, luồng thay thế và ngoại lệ.

#### 3.3.4.1 UC-01 Đăng ký tài khoản

Bảng 3.7. Đặc tả use case UC-01
| Thuộc tính | Nội dung |
| --- | --- |
| Mã và tên | UC-01 — Đăng ký tài khoản |
| Tác nhân và ưu tiên | Khách vãng lai; ưu tiên cao. FR-01. |
| Kích hoạt và tiền điều kiện | Khách chọn Đăng ký; họ tên, email và mật khẩu hợp lệ; email chưa được sử dụng. |
| Điều kiện sau | Tài khoản customer hoạt động và phiên đăng nhập được tạo. |
| Luồng chính | 1. Nhập thông tin. 2. Frontend gửi đăng ký. 3. Backend validate và kiểm tra email. 4. Băm BCrypt, lưu user với role customer. 5. Cấp token và trả hồ sơ. |
| Luồng thay thế | Khách đã có tài khoản chuyển sang đăng nhập. |
| Ngoại lệ | Email trùng hoặc dữ liệu sai: trả lỗi, không tạo tài khoản trùng. |

#### 3.3.4.2 UC-02 Đăng nhập đăng xuất và làm mới phiên

Bảng 3.8. Đặc tả use case UC-02
| Thuộc tính | Nội dung |
| --- | --- |
| Mã và tên | UC-02 — Đăng nhập đăng xuất và làm mới phiên |
| Tác nhân và ưu tiên | Mọi vai trò; ưu tiên cao. FR-01. |
| Kích hoạt và tiền điều kiện | Người dùng mở cổng tương ứng; tài khoản tồn tại, hoạt động và không bị khóa tạm. |
| Điều kiện sau | Có phiên hợp lệ; refresh được xoay vòng hoặc thu hồi khi đăng xuất. |
| Luồng chính | 1. Gửi email, mật khẩu, vai trò cổng. 2. Kiểm tra mật khẩu/khóa/role. 3. Cấp JWT và refresh, lưu hash refresh. 4. Client dùng Bearer token. 5. Khi cần, đổi refresh lấy cặp mới. |
| Luồng thay thế | Đăng xuất thu hồi refresh của phiên. Phát hiện dùng lại refresh đã thay thế thì thu hồi các phiên tài khoản. |
| Ngoại lệ | Sai mật khẩu tăng bộ đếm; mặc định sai 5 lần khóa 15 phút. Sai cổng, tài khoản khóa hoặc refresh không hợp lệ bị từ chối. |

![Hình 3.5. Biểu đồ tuần tự xác thực và làm mới phiên](figures_chuong3/seq_auth.png)

Hình 3.5. Biểu đồ tuần tự xác thực và làm mới phiên

#### 3.3.4.3 UC-03 Quản lý hồ sơ và khôi phục mật khẩu

Bảng 3.9. Đặc tả use case UC-03
| Thuộc tính | Nội dung |
| --- | --- |
| Mã và tên | UC-03 — Quản lý hồ sơ và khôi phục mật khẩu |
| Tác nhân và ưu tiên | Người dùng; SMTP; ưu tiên cao. FR-02. |
| Kích hoạt và tiền điều kiện | Người dùng mở hồ sơ hoặc chọn Quên mật khẩu; sửa hồ sơ yêu cầu xác thực. |
| Điều kiện sau | Hồ sơ/ảnh được cập nhật hoặc mật khẩu mới được lưu; phiên cũ mất hiệu lực khi đổi mật khẩu. |
| Luồng chính | 1. Tải hồ sơ của principal. 2. Sửa trường được phép hoặc tải ảnh. 3. Backend validate và lưu. 4. Với quên mật khẩu, tạo token hash và gửi liên kết email. 5. Xác minh token rồi băm/lưu mật khẩu mới. |
| Luồng thay thế | Đổi mật khẩu trong tài khoản phải xác minh mật khẩu hiện tại. Reset dùng token một lần, hạn 30 phút. |
| Ngoại lệ | Ảnh sai loại/quá 5 MB, token hết hạn/đã dùng, dữ liệu sai: từ chối. Yêu cầu quên mật khẩu không tiết lộ email có tài khoản. |

#### 3.3.4.4 UC-04 Tra cứu catalog và chi tiết sản phẩm

Bảng 3.10. Đặc tả use case UC-04
| Thuộc tính | Nội dung |
| --- | --- |
| Mã và tên | UC-04 — Tra cứu catalog và chi tiết sản phẩm |
| Tác nhân và ưu tiên | Khách; khách hàng; ưu tiên cao. FR-03. |
| Kích hoạt và tiền điều kiện | Người dùng truy cập cửa hàng, nhập từ khóa hoặc chọn danh mục/bộ lọc. |
| Điều kiện sau | Nhận danh sách phân trang hoặc chi tiết sản phẩm công khai. |
| Luồng chính | 1. Nhập tìm kiếm, danh mục, giá hoặc tình trạng hàng. 2. Backend lọc dữ liệu được công khai. 3. Trả ảnh, giá và tồn. 4. Mở chi tiết theo slug để xem mô tả, nguồn gốc, chứng nhận, bảo quản và đánh giá. |
| Luồng thay thế | Không có kết quả hiển thị trạng thái rỗng và cho thay bộ lọc. |
| Ngoại lệ | Slug không tồn tại hoặc sản phẩm bị ẩn: không trả chi tiết công khai; lỗi tải dữ liệu cho phép thử lại. |

#### 3.3.4.5 UC-05 Quản lý giỏ hàng và yêu thích

Bảng 3.11. Đặc tả use case UC-05
| Thuộc tính | Nội dung |
| --- | --- |
| Mã và tên | UC-05 — Quản lý giỏ hàng và yêu thích |
| Tác nhân và ưu tiên | Khách; khách hàng; ưu tiên cao. FR-04. |
| Kích hoạt và tiền điều kiện | Khách có phiên guest hợp lệ hoặc customer đã đăng nhập; sản phẩm còn được bán. |
| Điều kiện sau | Giỏ lưu tại server; yêu thích lưu theo tài khoản. |
| Luồng chính | 1. Thêm sản phẩm/số lượng. 2. Xác định chủ giỏ từ JWT hoặc guest token. 3. Kiểm tra sản phẩm, số lượng và tồn. 4. Lưu/cập nhật dòng. 5. Trả giỏ với giá hiện tại. |
| Luồng thay thế | Sửa số lượng, xóa một dòng hoặc xóa giỏ. Customer thêm/xóa sản phẩm yêu thích. |
| Ngoại lệ | Vượt tồn, sản phẩm ẩn/ngừng bán, phiên hết hạn hoặc dòng khác chủ: từ chối; giỏ không đồng nghĩa hàng đã được giữ. |

#### 3.3.4.6 UC-06 Gộp giỏ khách vào tài khoản

Bảng 3.12. Đặc tả use case UC-06
| Thuộc tính | Nội dung |
| --- | --- |
| Mã và tên | UC-06 — Gộp giỏ khách vào tài khoản |
| Tác nhân và ưu tiên | Khách hàng; ưu tiên cao. FR-05. |
| Kích hoạt và tiền điều kiện | Đăng nhập/đăng ký xong; có guest token và giỏ khách còn hợp lệ. |
| Điều kiện sau | Giỏ tài khoản chứa lượng hợp lệ đã gộp; phiên guest không được gộp lần hai. |
| Luồng chính | 1. Client gửi token khách với JWT. 2. Backend khóa user/guest session. 3. Tổng hợp giỏ hai phía. 4. Kiểm tra toàn bộ sản phẩm và tổng số lượng. 5. Lưu giỏ, đánh dấu phiên đã gộp trong một transaction. |
| Luồng thay thế | Gửi lại phiên đã gộp không cộng số lượng lần nữa. |
| Ngoại lệ | Bất kỳ dòng vượt tồn hoặc không bán: rollback toàn bộ; người dùng chỉnh giỏ trước khi thử lại. |

#### 3.3.4.7 UC-07 Quản lý địa chỉ và đặt hàng tài khoản

Bảng 3.13. Đặc tả use case UC-07
| Thuộc tính | Nội dung |
| --- | --- |
| Mã và tên | UC-07 — Quản lý địa chỉ và đặt hàng tài khoản |
| Tác nhân và ưu tiên | Khách hàng; ưu tiên cao. FR-06, FR-07, FR-17, FR-18. |
| Kích hoạt và tiền điều kiện | Customer có giỏ hợp lệ và địa chỉ của mình; chọn phương thức thanh toán/giao nhận khả dụng. |
| Điều kiện sau | Đơn, dòng snapshot, payment và lịch sử được lưu; kho/coupon được giữ, điểm được ghi nhận sử dụng. |
| Luồng chính | 1. Chọn/tạo địa chỉ, coupon và điểm. 2. Gọi preview để đọc lại giá/phí. 3. Xác nhận đặt. 4. Backend khóa tài nguyên và kiểm tra lại. 5. Giữ lô FEFO, coupon, trừ điểm, lưu đơn/payment. 6. Trả kết quả COD hoặc URL VNPAY. |
| Luồng thay thế | Địa chỉ có thể đặt mặc định. GHN chỉ chọn khi sản phẩm/địa chỉ và báo phí hợp lệ. |
| Ngoại lệ | Giá/tổng thay đổi, thiếu tồn/điểm, coupon sai hoặc báo phí lỗi: trả lỗi; không lưu giao dịch dở dang. |

![Hình 3.6. Biểu đồ tuần tự đặt hàng và giữ tồn kho](figures_chuong3/seq_checkout.png)

Hình 3.6. Biểu đồ tuần tự đặt hàng và giữ tồn kho

#### 3.3.4.8 UC-08 Đặt và tra cứu đơn không cần tài khoản

Bảng 3.14. Đặc tả use case UC-08
| Thuộc tính | Nội dung |
| --- | --- |
| Mã và tên | UC-08 — Đặt và tra cứu đơn không cần tài khoản |
| Tác nhân và ưu tiên | Khách vãng lai; ưu tiên cao. FR-06, FR-07, FR-09. |
| Kích hoạt và tiền điều kiện | Có guest session/giỏ hợp lệ; nhập liên hệ, địa chỉ và idempotencyKey. |
| Điều kiện sau | Tạo đơn guest không tạo user; có lookupToken để xem đơn và hóa đơn. |
| Luồng chính | 1. Nhập thông tin và preview. 2. Gửi đặt với token/khóa checkout. 3. Backend kiểm tra, giữ lô/coupon và lưu đơn. 4. Trả order, lookupToken, paymentUrl và replayed. 5. Tra cứu bằng mã đơn, email và lookupToken. |
| Luồng thay thế | Gửi lại cùng token/khóa trả đơn cũ; recovery lấy kết quả đã commit. Sau thành công giỏ/phiên checkout được vô hiệu hóa. |
| Ngoại lệ | Sai token/khóa hoặc thông tin tra cứu: không trả đơn. Mất mã xác minh cần liên hệ hỗ trợ; không tự cấp lại qua OTP. |

![Hình 3.7. Biểu đồ tuần tự đặt hàng khách và khôi phục kết quả](figures_chuong3/seq_guest.png)

Hình 3.7. Biểu đồ tuần tự đặt hàng khách và khôi phục kết quả

#### 3.3.4.9 UC-09 Thanh toán và nhận hóa đơn

Bảng 3.15. Đặc tả use case UC-09
| Thuộc tính | Nội dung |
| --- | --- |
| Mã và tên | UC-09 — Thanh toán và nhận hóa đơn |
| Tác nhân và ưu tiên | Người mua; VNPAY; SMTP; ưu tiên cao. FR-08, FR-09. |
| Kích hoạt và tiền điều kiện | Đơn đã được tạo; VNPAY có cấu hình hợp lệ nếu chọn trực tuyến. |
| Điều kiện sau | Payment phản ánh kết quả xác minh; hóa đơn xem/in được; email được gửi hoặc chờ retry. |
| Luồng chính | 1. COD tạo payment pending; VNPAY tạo URL ký. 2. Khách thanh toán. 3. Backend nhận Return/IPN, kiểm tra HMAC, tham chiếu và số tiền. 4. Cập nhật kết quả một lần. 5. Hiển thị hóa đơn và gửi email theo điều kiện. |
| Luồng thay thế | COD gửi hóa đơn sau tạo đơn; VNPAY sau xác nhận thành công. SMTP lỗi được lưu để retry, không rollback đơn đã xác nhận. |
| Ngoại lệ | Chữ ký/số tiền sai không ghi nhận thành công. VNPAY hết 15 phút hoặc thất bại kích hoạt hủy, trả kho/coupon/điểm theo nghiệp vụ. |

![Hình 3.8. Biểu đồ tuần tự xác minh thanh toán VNPAY](figures_chuong3/seq_payment.png)

Hình 3.8. Biểu đồ tuần tự xác minh thanh toán VNPAY

#### 3.3.4.10 UC-10 Hủy đơn và xử lý hoàn tiền

Bảng 3.16. Đặc tả use case UC-10
| Thuộc tính | Nội dung |
| --- | --- |
| Mã và tên | UC-10 — Hủy đơn và xử lý hoàn tiền |
| Tác nhân và ưu tiên | Khách hàng; quản trị viên; VNPAY; ưu tiên cao. FR-10. |
| Kích hoạt và tiền điều kiện | Khách là chủ đơn. Tự hủy chỉ khi pending chưa trả tiền; yêu cầu hoàn tiền cần payment completed và đơn chưa hủy. |
| Điều kiện sau | Đơn được hủy hợp lệ hoặc yêu cầu hoàn có quyết định; thay đổi tiền/kho/điểm có lịch sử. |
| Luồng chính | 1. Khách hủy hoặc gửi lý do hoàn tiền. 2. Backend kiểm tra/khóa đơn. 3. Admin xét yêu cầu. 4. Nếu duyệt, hoàn COD nội bộ hoặc gọi refund VNPAY. 5. Chỉ khi đủ điều kiện mới xác nhận hoàn và hủy; thông báo khách. |
| Luồng thay thế | Admin có API hoàn một phần VNPAY; yêu cầu khách mặc định xử lý hoàn toàn phần. Hoàn một phần không tự hủy toàn bộ đơn. |
| Ngoại lệ | Không tạo hai yêu cầu đang chờ. Gateway còn processing không được báo hoàn tất; lỗi chữ ký/kết quả không xác nhận thì giữ trạng thái phù hợp. |

![Hình 3.9. Biểu đồ tuần tự yêu cầu và xác nhận hoàn tiền](figures_chuong3/seq_refund.png)

Hình 3.9. Biểu đồ tuần tự yêu cầu và xác nhận hoàn tiền

#### 3.3.4.11 UC-11 Tiếp nhận và chuẩn bị đơn hàng

Bảng 3.17. Đặc tả use case UC-11
| Thuộc tính | Nội dung |
| --- | --- |
| Mã và tên | UC-11 — Tiếp nhận và chuẩn bị đơn hàng |
| Tác nhân và ưu tiên | Nhân viên; quản trị viên; ưu tiên cao. FR-11. |
| Kích hoạt và tiền điều kiện | Có quyền xử lý đơn; đơn ở trạng thái cho phép; VNPAY phải thanh toán thành công. |
| Điều kiện sau | Đơn chuyển trạng thái hợp lệ; processing xuất đúng lô; có lịch sử/thông báo. |
| Luồng chính | 1. Mở danh sách theo trạng thái. 2. Kiểm tra chi tiết/thanh toán. 3. Xác nhận processing. 4. Backend xuất lô đã giữ, kiểm tra lại hạn dùng. 5. Chuẩn bị và chuyển ready_for_delivery. |
| Luồng thay thế | Từ chối/hủy khi nghiệp vụ cho phép. Đơn giao nội bộ thất bại có thể đưa về hàng chờ để giao lại. |
| Ngoại lệ | Không bỏ bước trạng thái; đơn đã trả tiền phải hoàn trước khi hủy. Thiếu lô hợp lệ khi xuất thì dừng xử lý và trả lỗi. |

#### 3.3.4.12 UC-12 Phân công nhận và cập nhật giao nội bộ

Bảng 3.18. Đặc tả use case UC-12
| Thuộc tính | Nội dung |
| --- | --- |
| Mã và tên | UC-12 — Phân công nhận và cập nhật giao nội bộ |
| Tác nhân và ưu tiên | Staff; admin; nhân viên giao hàng; ưu tiên cao. FR-12. |
| Kích hoạt và tiền điều kiện | Đơn internal ở ready_for_delivery; người được giao có role delivery_staff và đang hoạt động. |
| Điều kiện sau | Lưu người phân công, thời điểm nhận/giao và kết quả; COD nội bộ hoàn tất khi giao thành công. |
| Luồng chính | 1. Staff/admin phân công. 2. Người được phân công xem và nhận đơn. 3. Chuyển out_for_delivery. 4. Giao thành công hoặc ghi thất bại và lý do. 5. Backend cập nhật lịch sử, payment COD, điểm và thông báo theo kết quả. |
| Luồng thay thế | Nhận lại cùng đơn không ghi trùng. Giao lại xóa thông tin nhận/phân công cũ trước lần phân công mới. |
| Ngoại lệ | Người khác, đơn chưa được nhận hoặc đơn GHN không được cập nhật bằng luồng giao nội bộ. |

![Hình 3.10. Biểu đồ tuần tự vận hành và giao hàng nội bộ](figures_chuong3/seq_delivery.png)

Hình 3.10. Biểu đồ tuần tự vận hành và giao hàng nội bộ

Các trạng thái dưới đây là trạng thái đơn GreenFarm; trạng thái vận đơn GHN được ánh xạ riêng. Nhánh hoàn tiền là nghiệp vụ có điều kiện, không phải đường hủy tự do từ mọi trạng thái.

![Hình 3.11. Sơ đồ trạng thái vòng đời đơn hàng](figures_chuong3/order_states.png)

Hình 3.11. Sơ đồ trạng thái vòng đời đơn hàng

#### 3.3.4.13 UC-13 Quản lý vận đơn và sự kiện GHN

Bảng 3.19. Đặc tả use case UC-13
| Thuộc tính | Nội dung |
| --- | --- |
| Mã và tên | UC-13 — Quản lý vận đơn và sự kiện GHN |
| Tác nhân và ưu tiên | Quản trị viên; GHN; ưu tiên cao. FR-13. |
| Kích hoạt và tiền điều kiện | GHN staging được bật; sản phẩm đủ điều kiện; đơn ready_for_delivery; VNPAY đã trả nếu áp dụng. |
| Điều kiện sau | Có vận đơn và lịch sử sự kiện; trạng thái/COD chỉ cập nhật sau xác minh phù hợp. |
| Luồng chính | 1. Admin yêu cầu tạo. 2. Tra theo GF-<orderId> và kiểm tra lại phí. 3. Tạo hoặc khôi phục vận đơn. 4. Nhận webhook/đồng bộ. 5. Xác minh secret, shop, mã đơn, chống lặp/cũ. 6. Cập nhật giao nhận và đối soát COD. |
| Luồng thay thế | Admin lấy phiếu in, sync thủ công hoặc hủy khi GHN cho phép. Timeout tạo phải tra cứu lại trước tạo mới. |
| Ngoại lệ | Phí đổi, hàng không đủ điều kiện, webhook sai hoặc COD chưa khớp: không xác nhận tương ứng. Phải hủy vận đơn GHN trước khi hủy đơn có vận đơn còn hiệu lực. |

![Hình 3.12. Biểu đồ tuần tự tạo vận đơn và nhận sự kiện GHN](figures_chuong3/seq_ghn.png)

Hình 3.12. Biểu đồ tuần tự tạo vận đơn và nhận sự kiện GHN

#### 3.3.4.14 UC-14 Quản lý danh mục sản phẩm và ảnh

Bảng 3.20. Đặc tả use case UC-14
| Thuộc tính | Nội dung |
| --- | --- |
| Mã và tên | UC-14 — Quản lý danh mục sản phẩm và ảnh |
| Tác nhân và ưu tiên | Admin; staff theo quyền; ưu tiên cao. FR-14. |
| Kích hoạt và tiền điều kiện | Đã xác thực và có manage_categories hoặc manage_products theo tài nguyên. |
| Điều kiện sau | Catalog lưu dữ liệu hợp lệ; ảnh có URL; thay đổi không làm sửa snapshot đơn cũ. |
| Luồng chính | 1. Xem danh sách/tìm sản phẩm. 2. Nhập tên, giá, đơn vị, danh mục, mô tả và nguồn gốc. 3. Tải ảnh hoặc khai báo URL. 4. Kiểm tra slug/quan hệ. 5. Lưu và chọn trạng thái bán. |
| Luồng thay thế | Admin cấu hình khả năng GHN và kích thước/khối lượng khi đã xác minh hàng được vận chuyển. Có thể ẩn sản phẩm. |
| Ngoại lệ | Dữ liệu sai, slug trùng, ảnh sai loại/kích thước hoặc danh mục có ràng buộc: trả lỗi. Không cập nhật stock trực tiếp. |

#### 3.3.4.15 UC-15 Quản lý nhà cung cấp và nhập lô

Bảng 3.21. Đặc tả use case UC-15
| Thuộc tính | Nội dung |
| --- | --- |
| Mã và tên | UC-15 — Quản lý nhà cung cấp và nhập lô |
| Tác nhân và ưu tiên | Quản trị viên; ưu tiên cao. FR-15. |
| Kích hoạt và tiền điều kiện | Role admin; sản phẩm tồn tại; lượng/giá nhập hợp lệ và lô mới có hạn dùng. |
| Điều kiện sau | Nhà cung cấp/lô được lưu; tồn khả dụng và lịch sử nhập cập nhật. |
| Luồng chính | 1. Thêm/chọn nhà cung cấp. 2. Nhập sản phẩm, số lượng, giá, ngày và ghi chú. 3. Backend kiểm tra ngày và nhà cung cấp. 4. Tạo mã lô duy nhất. 5. Ghi lịch sử/người nhập và đồng bộ stock. |
| Luồng thay thế | Có thể nhập không gắn nhà cung cấp. Nhà cung cấp đã có liên kết được ngừng sử dụng theo điều kiện nghiệp vụ. |
| Ngoại lệ | Nhập lô quá hạn, lượng không dương hoặc dữ liệu tham chiếu sai bị từ chối. Staff không được gọi API nhập lô admin. |

#### 3.3.4.16 UC-16 Theo dõi điều chỉnh và phân bổ kho

Bảng 3.22. Đặc tả use case UC-16
| Thuộc tính | Nội dung |
| --- | --- |
| Mã và tên | UC-16 — Theo dõi điều chỉnh và phân bổ kho |
| Tác nhân và ưu tiên | Admin; staff; ưu tiên cao. FR-16. |
| Kích hoạt và tiền điều kiện | Dùng endpoint đúng vai trò; lô tồn tại; điều chỉnh có loại và lý do. |
| Điều kiện sau | Tồn/giữ và lịch sử thống nhất; không xuất hàng quá hạn hoặc hoàn lặp. |
| Luồng chính | 1. Xem sản phẩm, lô, hạn dùng và lịch sử. 2. Chọn lô để kiểm kê/hư hỏng/hết hạn/trả hàng. 3. Khóa dữ liệu và kiểm tra số khả dụng. 4. Ghi trước/sau và người thực hiện. 5. Đồng bộ stock. |
| Luồng thay thế | Tạo đơn tự giữ lô FEFO; processing tự xuất; hủy tự giải phóng/hoàn. Scheduler cập nhật khả dụng khi lô hết hạn. |
| Ngoại lệ | Không cho số còn âm hoặc thấp hơn phần giữ. DAMAGED/EXPIRED chỉ giảm, RETURN chỉ tăng; lô quá hạn không tham gia bán mới. |

![Hình 3.13. Biểu đồ tuần tự giữ xuất và hoàn kho theo lô](figures_chuong3/seq_inventory.png)

Hình 3.13. Biểu đồ tuần tự giữ xuất và hoàn kho theo lô

#### 3.3.4.17 UC-17 Quản lý và sử dụng coupon

Bảng 3.23. Đặc tả use case UC-17
| Thuộc tính | Nội dung |
| --- | --- |
| Mã và tên | UC-17 — Quản lý và sử dụng coupon |
| Tác nhân và ưu tiên | Admin; người mua; ưu tiên cao. FR-17. |
| Kích hoạt và tiền điều kiện | Admin có manage_coupons; người mua có giỏ và coupon thỏa điều kiện. |
| Điều kiện sau | Ưu đãi áp dụng đúng phạm vi; lượt dùng có vòng đời và không vượt hạn mức. |
| Luồng chính | 1. Admin tạo mã, loại, mức giảm, phạm vi/thời hạn/giới hạn. 2. Người mua chọn mã và preview. 3. Khi đặt, backend khóa coupon, kiểm tra lại và giữ lượt RESERVED. 4. Thanh toán/xử lý đánh USED; hủy đủ điều kiện RELEASED. |
| Luồng thay thế | Kết hợp một giảm hàng và một miễn phí vận chuyển; dùng cùng điểm. Admin tạm dừng mã hoặc xem lịch sử. |
| Ngoại lệ | Mã hết hạn/hết lượt/sai phạm vi bị từ chối. Sau có lịch sử không sửa điều kiện làm thay đổi quyền lợi đã ghi nhận; hoàn sau hoàn tất không tự phát lại mã. |

#### 3.3.4.18 UC-18 Đánh giá sản phẩm và điểm thưởng

Bảng 3.24. Đặc tả use case UC-18
| Thuộc tính | Nội dung |
| --- | --- |
| Mã và tên | UC-18 — Đánh giá sản phẩm và điểm thưởng |
| Tác nhân và ưu tiên | Khách hàng; ưu tiên trung bình. FR-18, FR-19. |
| Kích hoạt và tiền điều kiện | Customer đã nhận sản phẩm; dùng điểm cần số dư đủ và checkout hợp lệ. |
| Điều kiện sau | Lưu đánh giá duy nhất; số dư và lịch sử điểm có căn cứ, không cộng/trừ lặp. |
| Luồng chính | 1. Xem đơn đã giao và chọn sản phẩm. 2. Gửi 1–5 sao/nội dung. 3. Backend kiểm tra đã mua và chủ đánh giá. 4. Lưu/tính thưởng hợp lệ. 5. Khách xem điểm và chọn số dùng khi checkout. |
| Luồng thay thế | Sửa đánh giá không nhận thưởng lần nữa. Đơn giao thành công thưởng theo quy tắc; hủy/hoàn điều chỉnh theo lịch sử điểm. |
| Ngoại lệ | Chưa mua/chưa nhận, sửa đánh giá người khác hoặc vượt số dư bị từ chối. Mức đổi được chặn ở 50% tạm tính sau coupon hàng. |

#### 3.3.4.19 UC-19 Liên hệ hỗ trợ và nhận thông báo

Bảng 3.25. Đặc tả use case UC-19
| Thuộc tính | Nội dung |
| --- | --- |
| Mã và tên | UC-19 — Liên hệ hỗ trợ và nhận thông báo |
| Tác nhân và ưu tiên | Khách; người dùng; staff; admin; ưu tiên trung bình. FR-20. |
| Kích hoạt và tiền điều kiện | Gửi liên hệ công khai; xem lịch sử/thông báo cần tài khoản; xử lý hỗ trợ cần manage_contacts. |
| Điều kiện sau | Yêu cầu có trạng thái open/replied/resolved; thông báo lưu đúng người nhận. |
| Luồng chính | 1. Khách gửi họ tên, email, điện thoại và nội dung. 2. Lưu và báo bộ phận vận hành. 3. Staff/admin phản hồi hoặc đóng. 4. Khách có tài khoản xem lịch sử. 5. Sự kiện được phát SSE và hiển thị số chưa đọc. |
| Luồng thay thế | Người dùng đánh dấu một/tất cả đã đọc; khi mất kết nối tải lại từ dữ liệu lưu. |
| Ngoại lệ | Liên hệ khách không gắn tài khoản không có trang lịch sử riêng. Không được đọc/đánh dấu thông báo người khác; nội dung sai validation bị từ chối. |

#### 3.3.4.20 UC-20 Tư vấn mua sắm bằng chatbot

Bảng 3.26. Đặc tả use case UC-20
| Thuộc tính | Nội dung |
| --- | --- |
| Mã và tên | UC-20 — Tư vấn mua sắm bằng chatbot |
| Tác nhân và ưu tiên | Khách; khách hàng; Gemini; ưu tiên trung bình. FR-21. |
| Kích hoạt và tiền điều kiện | Người dùng gửi câu hỏi; phiên chat tách theo tài khoản hoặc token khách. |
| Điều kiện sau | Trả tư vấn trong phạm vi công khai; lưu hội thoại đã lọc, không thay đổi giao dịch. |
| Luồng chính | 1. Nhận câu hỏi/phiên. 2. Che mẫu dữ liệu nhạy cảm, kiểm tra phạm vi. 3. Đọc catalog/chính sách và lịch sử giới hạn. 4. Gọi Gemini với prompt backend. 5. Kiểm tra phản hồi/sản phẩm và trả client. |
| Luồng thay thế | Thiếu khóa hoặc API lỗi dùng bộ trả lời dự phòng. Câu hỏi tiếp nối dùng lịch sử cùng chủ thể. |
| Ngoại lệ | Yêu cầu hồ sơ riêng, đơn cá nhân, doanh thu, bí mật hoặc thao tác thanh toán bị từ chối; không tin giá/tồn do client đưa vào prompt. |

![Hình 3.14. Biểu đồ tuần tự tư vấn bằng chatbot](figures_chuong3/seq_chat.png)

Hình 3.14. Biểu đồ tuần tự tư vấn bằng chatbot

#### 3.3.4.21 UC-21 Quản lý tài khoản vai trò và trạng thái

Bảng 3.27. Đặc tả use case UC-21
| Thuộc tính | Nội dung |
| --- | --- |
| Mã và tên | UC-21 — Quản lý tài khoản vai trò và trạng thái |
| Tác nhân và ưu tiên | Quản trị viên; ưu tiên cao. FR-22. |
| Kích hoạt và tiền điều kiện | Tài khoản có manage_users và truy cập khu vực quản trị. |
| Điều kiện sau | Lưu role/trạng thái hợp lệ; người dùng bị kiểm soát theo cấu hình mới. |
| Luồng chính | 1. Tìm/lọc người dùng. 2. Xem role/permission hiện có. 3. Chọn vai trò hoặc trạng thái. 4. Backend kiểm tra tự bảo vệ và dữ liệu. 5. Lưu, tải lại danh sách. |
| Luồng thay thế | Không tạo mô hình role/permission tùy ý ngoài chức năng hiện có; sử dụng các role được seed. |
| Ngoại lệ | Admin không tự đổi role/trạng thái của chính mình qua API này. Thiếu quyền hoặc role không hợp lệ bị từ chối. |

#### 3.3.4.22 UC-22 Xem báo cáo và xuất dữ liệu kinh doanh

Bảng 3.28. Đặc tả use case UC-22
| Thuộc tính | Nội dung |
| --- | --- |
| Mã và tên | UC-22 — Xem báo cáo và xuất dữ liệu kinh doanh |
| Tác nhân và ưu tiên | Quản trị viên; ưu tiên trung bình. FR-23. |
| Kích hoạt và tiền điều kiện | Role admin; khoảng ngày hợp lệ, tối đa hai năm. |
| Điều kiện sau | Trả các chỉ số và CSV cùng kỳ, cùng quy tắc tổng hợp. |
| Luồng chính | 1. Chọn kỳ 7/30/90 ngày hoặc nhập ngày. 2. Backend tổng hợp payment completed theo paid_at. 3. Lấy đơn theo trạng thái, sản phẩm, khách mới, tồn thấp và coupon. 4. Hiển thị dashboard. 5. Xuất CSV UTF-8 khi yêu cầu. |
| Luồng thay thế | Không truyền ngày dùng 30 ngày gần nhất tính cả hôm nay. Không có dữ liệu trả tập rỗng/chỉ số phù hợp. |
| Ngoại lệ | Khoảng ngày đảo hoặc quá giới hạn bị từ chối; không cộng đơn chỉ mới tạo vào doanh thu đã thu. |


## 3.4 Thiết kế kiến trúc hệ thống

### 3.4.1 Lựa chọn mô hình kiến trúc

GreenFarm sử dụng mô hình web client–server, frontend Next.js và backend Spring Boot cung cấp REST API. Backend là một ứng dụng nguyên khối được tổ chức theo các lớp và dịch vụ nghiệp vụ, cùng đóng gói trong một JAR. Lựa chọn này giúp đặt đơn, giữ kho, ghi coupon và trừ điểm trong cùng giao dịch dữ liệu.

Bảng 3.29. Quyết định kiến trúc và đánh đổi
| Quyết định | Căn cứ và lợi ích | Đánh đổi |
| --- | --- | --- |
| Next.js tách Spring Boot | Phát triển giao diện và API riêng; tổ chức trang theo vai trò | Cần quản lý token, CORS, cache và địa chỉ API |
| Backend nguyên khối phân lớp | Nghiệp vụ liên kết chặt; transaction tập trung, dễ triển khai đồ án | Các dịch vụ cùng tiến trình và phiên bản triển khai |
| MySQL, JPA và Flyway | Quan hệ rõ, khóa/ràng buộc, truy cập ORM và migration có phiên bản | Phải đồng bộ entity với SQL, kiểm soát truy vấn và khóa |
| Snapshot đơn hàng | Giữ tên, giá, đơn vị, ảnh, địa chỉ và gói hàng lúc mua | Lặp một phần dữ liệu để bảo toàn lịch sử |
| SSE và tác vụ định kỳ | Thông báo một chiều, retry email và quét hết hạn phù hợp một backend | Luồng SSE trong bộ nhớ cần thiết kế thêm khi chạy nhiều replica |
| Tích hợp tại backend | Giữ bí mật và tập trung xác minh dữ liệu dịch vụ ngoài | Phải xử lý timeout, callback lặp và trạng thái chưa xác định |
| Vercel và Railway | Tách nơi phục vụ frontend với tiến trình Java và MySQL | Cần thống nhất domain, biến môi trường và cấu hình callback |

### 3.4.2 Kiến trúc tổng thể và thành phần

Trình duyệt tải giao diện Next.js, gửi yêu cầu HTTPS đến Spring Boot. JwtAuthenticationFilter xác minh access token và tạo principal; Spring Security cùng kiểm tra tại phương thức quyết định quyền truy cập. Controller nhận DTO hợp lệ, Service xử lý nghiệp vụ, Repository đọc/ghi MySQL qua JPA. GlobalExceptionHandler và các bộ xử lý bảo mật chuẩn hóa lỗi.

![Hình 3.15. Kiến trúc tổng thể của hệ thống GreenFarm](figures_chuong3/architecture.png)

Hình 3.15. Kiến trúc tổng thể của hệ thống GreenFarm

Bảng 3.30. Trách nhiệm các thành phần
| Thành phần | Trách nhiệm |
| --- | --- |
| Trang và component Next.js | Cửa hàng, tài khoản, portal admin/staff/giao hàng; biểu mẫu và trạng thái tương tác |
| Axios, TanStack Query, Zustand | Gọi API, quản lý dữ liệu/cache, làm mới phiên và trạng thái dùng chung |
| Security và AuthService | JWT, BCrypt, phiên refresh, khóa đăng nhập, khôi phục mật khẩu và quyền |
| Catalog, Cart, GuestCommerce | Dữ liệu công khai, giỏ tài khoản/khách, gộp giỏ và đặt hàng khách |
| OrderService, OrderLifecycleService | Báo giá, snapshot, tạo đơn, chuyển trạng thái và phối hợp thanh toán–kho–điểm |
| Inventory, Supplier | Lô, FEFO, giữ/xuất/hoàn hàng, kiểm kê, hạn dùng và nhà cung cấp |
| CouponEngine, Loyalty | Kiểm tra ưu đãi, vòng đời lượt dùng, số dư và lịch sử điểm |
| Payment, VnpayRefund, RefundRequest | Callback, hết hạn thanh toán, yêu cầu/ghi nhận hoàn tiền |
| GhnShipment, GhnWebhook | Tính phí, vận đơn, xác minh sự kiện, đồng bộ trạng thái và đối soát COD |
| Review, Contact, Notification | Đánh giá đã mua, hỗ trợ, thông báo lưu bền và luồng SSE |
| Chatbot, ImageStorage, InvoiceEmail | Tư vấn công khai, lưu ảnh, gửi hóa đơn và retry email |
| BusinessReport và scheduler | Tổng hợp/CSV; quét payment/lô hết hạn và gửi lại email |

Một đơn hàng không được cập nhật trực tiếp từ giao diện bằng cách ghi cơ sở dữ liệu. Mọi thay đổi trạng thái đi qua nghiệp vụ kiểm tra điều kiện và ghi lịch sử. Các lời gọi ngoài có thể thành công dù kết nối trả kết quả bị gián đoạn; mã yêu cầu, mã tham chiếu và thao tác tra cứu lại giúp hạn chế xử lý trùng.

### 3.4.3 Mô hình triển khai trên Railway và Vercel

Trong mô hình triển khai của đề tài, frontend được triển khai trên Vercel, backend Spring Boot được đóng gói Docker và chạy trên Railway. MySQL được tổ chức thành dịch vụ dữ liệu riêng trong Railway với volume bền vững; ảnh mới được lưu trên Cloudinary. Cách bố trí này tách giao diện, tiến trình nghiệp vụ và dữ liệu, đồng thời cho phép backend nhận Return/IPN và webhook qua địa chỉ HTTPS công khai.

![Hình 3.16. Sơ đồ triển khai GreenFarm trên Vercel và Railway](figures_chuong3/deployment.png)

Hình 3.16. Sơ đồ triển khai GreenFarm trên Vercel và Railway

Frontend sử dụng thư mục gốc frontend, cài đặt theo package-lock.json và build Next.js. NEXT_PUBLIC_API_BASE_URL trỏ tới URL backend kèm /api. Biến có tiền tố NEXT_PUBLIC_ có thể xuất hiện trong mã gửi xuống trình duyệt, vì vậy chỉ chứa địa chỉ công khai. Việc thay biến build của frontend được áp dụng qua bản triển khai mới theo cơ chế Vercel [1].

Backend được build từ Dockerfile tại gốc repository; server.port đọc PORT do nền tảng cung cấp. railway.json khai báo healthcheck /api/health, thời gian chờ 300 giây và khởi động lại khi lỗi tối đa 10 lần. Healthcheck hỗ trợ quyết định đưa bản triển khai vào phục vụ, không thay thế giám sát liên tục hoặc kiểm tra toàn bộ nghiệp vụ [2].

Bảng 3.31. Cấu hình các thành phần triển khai
| Thành phần | Cấu hình chính | Mục đích |
| --- | --- | --- |
| Vercel | Root frontend; framework Next.js; npm run build; NEXT_PUBLIC_API_BASE_URL | Phục vụ web và trỏ tới REST API công khai |
| Railway backend | Dockerfile; PORT; /api/health; restart ON_FAILURE | Chạy JVM, scheduler và endpoint HTTPS |
| Railway MySQL | MySQL tương thích schema; volume /var/lib/mysql; DB_URL hoặc DB_HOST/PORT/NAME | Lưu dữ liệu nghiệp vụ bền vững, kết nối từ backend |
| Spring Security | APP_CORS_ALLOWED_ORIGINS chứa origin Vercel hợp lệ; JWT_SECRET và tham số phiên | Cho phép frontend đã xác định, bảo vệ API |
| Flyway | FLYWAY_ENABLED; migration V1–V25; backup trước nâng cấp | Quản lý cấu trúc và kiểm soát thay đổi dữ liệu |
| Email và ảnh | MAIL_*; INVOICE_EMAIL_*; STOREFRONT_URL; CLOUDINARY_* | Hóa đơn, khôi phục mật khẩu và tài nguyên ảnh |
| VNPAY | Merchant/secret; VNPAY_RETURN_URL; URL trang kết quả frontend; đăng ký IPN backend | Callback đúng môi trường và xác minh thanh toán |
| GHN | GHN_ENABLED; token/shop/secret staging; webhook backend | Báo phí, vận đơn và nhận sự kiện được xác thực |
| Gemini | GEMINI_API_KEY; GEMINI_CHAT_MODEL tại backend | Tư vấn theo catalog mà không lộ khóa |

MySQL không được truy cập trực tiếp từ trình duyệt. Các URL frontend/backend ở cấu hình email và thanh toán phải đồng nhất với bản triển khai. Nếu tiếp tục dùng ảnh local cũ, backend cần volume cho UPLOAD_DIRECTORY; hệ thống tệp tạm của container không được coi là nơi lưu bền. Với mô hình chọn Cloudinary, URL ảnh được lưu trong MySQL để giao diện tải tài nguyên trực tiếp.

Thông báo SSE kết nối đến backend Railway và gửi kèm xác thực theo cách client đang sử dụng. Phương án một backend phù hợp cơ chế phát thông báo trong bộ nhớ hiện tại; khi chạy nhiều replica cần bổ sung cơ chế chia sẻ sự kiện. Việc sao lưu MySQL, thử khôi phục, kiểm tra callback và theo dõi log là các công việc vận hành riêng, không suy ra từ healthcheck thành công.

### 3.4.4 Thiết kế trợ lý tư vấn AI

Chatbot tiếp nhận câu hỏi và lấy lịch sử theo user_id hoặc hash token phiên khách. Backend che các mẫu email, số điện thoại và chuỗi số nhạy cảm trước khi lưu/gửi; giới hạn ngữ cảnh vào sản phẩm công khai, giá, tồn và chính sách mua hàng. Câu hỏi yêu cầu dữ liệu cá nhân, doanh thu hoặc cấu hình nội bộ bị từ chối.

![Hình 3.17. Luồng xử lý và kiểm soát dữ liệu của chatbot](figures_chuong3/ai.png)

Hình 3.17. Luồng xử lý và kiểm soát dữ liệu của chatbot

ChatbotService tạo prompt tại backend, gọi Gemini khi có cấu hình và xử lý phản hồi trước khi trả giao diện. Thẻ sản phẩm được đối chiếu catalog. Khi không có khóa hoặc dịch vụ lỗi, bộ trả lời dự phòng sử dụng dữ liệu công khai. Cơ chế này hỗ trợ tư vấn nhưng không bảo đảm loại bỏ mọi sai sót của mô hình. AI không được quyết định giá, trừ kho, tạo đơn hoặc xác nhận thanh toán.

## 3.5 Thiết kế cơ sở dữ liệu

### 3.5.1 Mô hình dữ liệu khái niệm

Mô hình xoay quanh sản phẩm, người mua và đơn hàng. Danh mục phân loại sản phẩm; mỗi sản phẩm có nhiều ảnh và nhiều lô kho. Người mua có thể là tài khoản hoặc phiên khách. Đơn lưu các dòng hàng, lịch sử, giao dịch thanh toán và thông tin giao nhận; phân bổ kho nối từng dòng hàng với các lô thực tế.

![Hình 3.18. Sơ đồ thực thể liên kết mức khái quát](figures_chuong3/er_concept.png)

Hình 3.18. Sơ đồ thực thể liên kết mức khái quát

### 3.5.2 Tổng quan lược đồ quan hệ

Các migration V1–V25 khai báo 36 tên bảng khác nhau, gồm 35 bảng phục vụ mô hình hiện tại và bảng password_reset_tokens kế thừa. Chức năng khôi phục mật khẩu hiện dùng greenfarm_password_reset_tokens. flyway_schema_history là bảng kỹ thuật do Flyway quản lý, không tính vào 36 bảng này.

Bảng 3.32. Phân nhóm các bảng dữ liệu
| Nhóm | Số bảng | Các bảng |
| --- | --- | --- |
| Tài khoản và địa chỉ | 8 | users, roles, permissions, role_permissions, refresh_tokens, greenfarm_password_reset_tokens, password_reset_tokens, shipping_addresses |
| Catalog | 3 | categories, products, product_images |
| Giỏ và phiên khách | 4 | cart_items, wishlists, guest_sessions, guest_cart_items |
| Đơn và thanh toán | 6 | orders, order_items, order_status_history, payments, refund_requests, vnpay_refunds |
| Ưu đãi | 4 | coupons, coupon_categories, coupon_products, coupon_usages |
| Kho và nguồn cung | 4 | suppliers, inventory_batches, order_inventory_allocations, inventory_transactions |
| Giao nhận GHN | 2 | shipments, shipment_events |
| Tương tác và điểm | 5 | reviews, contacts, notifications, loyalty_point_transactions, chat_messages |
| Tổng | 36 | Không bao gồm bảng metadata của Flyway |

Các bảng nghiệp vụ thường dùng khóa số tự tăng, khóa ngoại và thời điểm tạo/cập nhật. role_permissions và các bảng nối phạm vi coupon dùng cặp khóa liên kết. Bảng password_reset_tokens cũ được giữ để tương thích dữ liệu, không đại diện cho một quy trình khôi phục đang hoạt động khác.

### 3.5.3 Quan hệ và ràng buộc toàn vẹn

Bảng 3.33. Quan hệ chính và ràng buộc nghiệp vụ
| Quan hệ hoặc tài nguyên | Ràng buộc thiết kế |
| --- | --- |
| roles–users; roles–permissions | Một role có nhiều users; role và permission N–N qua role_permissions; mỗi user hiện gắn một role |
| categories–products–product_images | Danh mục 1–N sản phẩm; sản phẩm 1–N ảnh; slug sản phẩm/danh mục duy nhất |
| Giỏ và yêu thích | Mỗi cặp user–product hoặc guest_session–product là một dòng; số lượng phải dương |
| Chủ đơn | user_id được phép rỗng với khách; guest_session_id và snapshot liên hệ xác định ngữ cảnh mua; kiểm tra chủ sở hữu ở service |
| orders–order_items/history | Một đơn có nhiều dòng và mốc lịch sử; dòng giữ snapshot giá/tên/đơn vị/ảnh/gói hàng |
| orders–payments/shipments | Luồng hiện tại tạo một payment cho đơn nhưng SQL chưa unique payments.order_id; shipments.order_id có unique nên tối đa một hồ sơ vận chuyển mỗi đơn |
| coupon_usages | Unique (coupon_id, order_id); khóa coupon trước kiểm tra hạn mức; vòng đời RESERVED/USED/RELEASED |
| Lô và phân bổ | Một sản phẩm có nhiều lô; một dòng đơn phân bổ nhiều lô; không âm tồn/giữ và không hoàn một phân bổ hai lần |
| reviews và điểm | Unique user–product; đánh giá kiểm tra đã mua; chỉ mục/khóa nghiệp vụ chống thưởng, dùng và hoàn điểm lặp |
| Vận đơn và sự kiện | order_id, client_order_code, provider_order_code duy nhất; event_key duy nhất trong một shipment |
| Token nhạy cảm | Refresh, phiên khách, tra cứu đơn, chat khách và reset dùng hash trong lưu trữ; thời hạn/trạng thái dùng được kiểm tra |
| Hoàn tiền | Lưu request ID duy nhất và trạng thái gateway; không gửi yêu cầu mới khi yêu cầu trước còn chờ xử lý |

Các quan hệ khai báo trong database bảo vệ tham chiếu; điều kiện như vai trò nhân viên giao, chủ đơn, hạn coupon hoặc đã mua sản phẩm được kiểm tra trong service. Không coi mọi điều kiện nghiệp vụ đều là CHECK constraint của MySQL.

### 3.5.4 Sơ đồ thực thể liên kết theo nhóm

Các ERD dưới đây giữ tên bảng và trường tham chiếu theo mã nguồn, lược bớt trường mô tả và thời gian để dễ đọc. PK là khóa chính, FK là khóa ngoại; số lượng 0..1 và 0..N thể hiện liên kết tùy chọn. Những thực thể xuất hiện ở nhiều hình là điểm nối giữa các nhóm.

![Hình 3.19. ERD nhóm tài khoản phiên và giỏ hàng](figures_chuong3/er_accounts.png)

Hình 3.19. ERD nhóm tài khoản phiên và giỏ hàng

![Hình 3.20. ERD nhóm catalog đơn hàng và thanh toán](figures_chuong3/er_commerce.png)

Hình 3.20. ERD nhóm catalog đơn hàng và thanh toán

![Hình 3.21. ERD nhóm nhà cung cấp kho và phân bổ lô](figures_chuong3/er_inventory.png)

Hình 3.21. ERD nhóm nhà cung cấp kho và phân bổ lô

![Hình 3.22. ERD nhóm ưu đãi điểm tương tác và giao nhận](figures_chuong3/er_engagement.png)

Hình 3.22. ERD nhóm ưu đãi điểm tương tác và giao nhận

### 3.5.5 Từ điển dữ liệu trọng tâm

Từ điển tập trung vào trường nhận diện, quan hệ và dữ liệu quyết định nghiệp vụ; các cột mô tả phụ, created_at và updated_at được lược khi không ảnh hưởng cách hiểu. Kiểu ghi theo SQL migration; cột trạng thái có thể là ENUM hoặc chuỗi được chuyển đổi từ enum Java tùy bảng.

Bảng 3.34. Từ điển dữ liệu tài khoản và phiên
| Bảng | Trường trọng tâm | Kiểu chính | Ý nghĩa và ràng buộc |
| --- | --- | --- | --- |
| users | id; role_id; email; password | BIGINT; VARCHAR | PK; FK roles; email unique; password BCrypt. Có status, credential_version, bộ đếm khóa và loyalty_points_balance. |
| roles; permissions | id; name | BIGINT; VARCHAR | PK; tên role/permission duy nhất; role_permissions(role_id, permission_id) là bảng nối N–N. |
| refresh_tokens | id; user_id; token_hash; expires_at; revoked_at; replaced_by_hash | BIGINT; CHAR(64); TIMESTAMP | Lưu hash duy nhất, hạn và quan hệ xoay vòng; không lưu refresh token thô. |
| greenfarm_password_reset_tokens | id; user_id; token_hash; credential_version; expires_at; used_at | BIGINT; CHAR(64); INT; DATETIME | Token reset hiện tại; hash unique; kiểm tra hạn, phiên bản và thời điểm đã dùng. |
| password_reset_tokens | email; token; created_at | VARCHAR; TIMESTAMP | Bảng kế thừa từ baseline; email là khóa chính, không dùng cho luồng reset hiện tại. |
| shipping_addresses | id; user_id; full_name; phone; address; city; default | BIGINT; VARCHAR; BOOLEAN | Địa chỉ theo user; cột default xác định mặc định; có ghn_province_id/name và ghn_ward_id/name cho GHN. |

Bảng 3.35. Từ điển dữ liệu catalog giỏ và phiên khách
| Bảng | Trường trọng tâm | Kiểu chính | Ý nghĩa và ràng buộc |
| --- | --- | --- | --- |
| categories | id; name; slug; description; image | BIGINT; VARCHAR; TEXT | Tên/slug unique; ảnh URL. Có trường tên/mô tả tiếng Anh trong schema. |
| products | id; category_id; slug; price; stock; status; unit | BIGINT; VARCHAR; DECIMAL; INT; ENUM | FK danh mục; stock là tồn khả dụng đồng bộ; có origin, certification, storage_instructions. |
| products | ghn_eligible; package_weight_g; package_length_cm/width_cm/height_cm | BOOLEAN; INT | Điều kiện GHN và gói hàng; thông số không tự chứng minh hàng được nhận vận chuyển. |
| product_images | id; product_id; image | BIGINT; VARCHAR | Ảnh 1–N theo sản phẩm; lưu đường dẫn, không lưu nhị phân ảnh. |
| cart_items; wishlists | id; user_id; product_id; quantity (giỏ) | BIGINT; INT | Unique user–product từng bảng; chỉ giỏ có quantity dương. |
| guest_sessions | id; token_hash; expires_at; consumed_at; merged_user_id | BIGINT; CHAR(64); TIMESTAMP | Hash unique, thời hạn, mốc tiêu thụ và tài khoản gộp; không tự tạo user. |
| guest_cart_items | id; guest_session_id; product_id; quantity | BIGINT; INT | FK phiên/sản phẩm; unique cặp phiên–sản phẩm; quantity dương. |

Bảng 3.36. Từ điển dữ liệu đơn hàng và thanh toán
| Bảng | Trường trọng tâm | Kiểu chính | Ý nghĩa và ràng buộc |
| --- | --- | --- | --- |
| orders | id; user_id; guest_session_id; delivery_staff_id; shipping_address_id | BIGINT | PK và các FK; user/địa chỉ tài khoản có thể rỗng với đơn khách. |
| orders | subtotal; discount_amount; loyalty_discount_amount; shipping_fee; shipping_discount_amount; total_price | DECIMAL | Các thành phần tiền được tính tại backend; có loyalty_points_used và snapshot mã coupon. |
| orders | status; recipient_name/phone; shipping_address_line/city; shipping_provider | ENUM; VARCHAR | Trạng thái và snapshot giao nhận; có tỉnh/phường GHN, mốc nhận/giao, lý do thất bại. |
| orders | guest_lookup_token_hash; guest_checkout_key | CHAR(64); VARCHAR(64) | Xác minh tra cứu và chống đặt lặp; khóa checkout gắn ngữ cảnh guest session. |
| order_items | id; order_id; product_id; product_name/unit/image; quantity; price | BIGINT; VARCHAR; INT; DECIMAL | FK đơn/sản phẩm; snapshot giá/tên/ảnh/đơn vị và thông số gói hàng. |
| order_status_history | id; order_id; status; changed_at; note | BIGINT; ENUM; TIMESTAMP; TEXT | Dòng lịch sử theo đơn, trạng thái và thời điểm; note ghi ngữ cảnh thao tác. |
| payments | id; order_id; payment_method; amount; status; reference_code; transaction_id | BIGINT; ENUM; DECIMAL; VARCHAR | Mã reference_code unique; order_id có index nhưng chưa unique; nghiệp vụ hiện dùng một payment cho đơn. |
| payments | invoice_email_sent_at; invoice_email_attempts; invoice_email_next_retry_at; invoice_email_last_error | TIMESTAMP; INT; VARCHAR(500) | Theo dõi gửi hóa đơn và retry; gateway_payload lưu dữ liệu phục vụ đối chiếu. |
| refund_requests | id; order_id; reason; details; status; reviewed_by; reviewed_at; admin_note | BIGINT; VARCHAR; DATETIME | Lý do khách, trạng thái và quyết định admin; không thay thế bản ghi gateway. |
| vnpay_refunds | id; payment_id; request_id; refund_amount; transaction_type; status | BIGINT; VARCHAR; DECIMAL | request_id unique; lưu mã phản hồi/trạng thái giao dịch và response_payload. |

Bảng 3.37. Từ điển dữ liệu coupon và điểm thưởng
| Bảng | Trường trọng tâm | Kiểu chính | Ý nghĩa và ràng buộc |
| --- | --- | --- | --- |
| coupons | id; code; coupon_type; discount_type; scope_type | BIGINT; VARCHAR | Mã unique; loại giảm hàng/miễn giao, cách giảm và phạm vi ALL/CATEGORY/PRODUCT. |
| coupons | discount_percentage; discount_amount; max_discount_amount; minimum_order_amount | INT; DECIMAL | Giá trị, trần giảm và tối thiểu; có starts_at, expires_at, is_active và giới hạn lượt. |
| coupon_categories; coupon_products | coupon_id; category_id hoặc product_id | BIGINT | PK ghép và FK; định nghĩa phạm vi áp dụng coupon. |
| coupon_usages | id; coupon_id; order_id; user_id; guest_session_id; discount_amount; status | BIGINT; DECIMAL; VARCHAR | Unique coupon–order; chủ tài khoản hoặc khách; RESERVED/USED/RELEASED và các mốc thời gian. |
| loyalty_point_transactions | id; user_id; order_id; review_id; product_id; type; points; description | BIGINT; VARCHAR; INT | Điểm có dấu; truy vết thưởng, đổi, hoàn/điều chỉnh; khóa nghiệp vụ chống xử lý lặp. |

Bảng 3.38. Từ điển dữ liệu kho và nguồn cung
| Bảng | Trường trọng tâm | Kiểu chính | Ý nghĩa và ràng buộc |
| --- | --- | --- | --- |
| suppliers | id; supplier_code; name; phone; email; address; status; note | BIGINT; VARCHAR | supplier_code unique; thông tin nguồn cung, liên hệ và trạng thái sử dụng. |
| inventory_batches | id; batch_code; product_id; supplier_id; quantity; remaining_quantity; reserved_quantity | BIGINT; VARCHAR(64); INT | Mã lô unique; số nhập, còn vật lý và đang giữ; tham chiếu sản phẩm/nhà cung cấp. |
| inventory_batches | import_price; manufacture_date; expiry_date; imported_at; created_by | DECIMAL; DATE; DATETIME; BIGINT | Giá nhập, ngày sản xuất/hạn, thời điểm và người nhập; lô mới yêu cầu hạn dùng. |
| order_inventory_allocations | id; order_item_id; batch_id; quantity; status; exported_at; restored_at | BIGINT; INT; VARCHAR; DATETIME | Nối dòng đơn với lô; trạng thái giữ/xuất/hoàn và mốc chống hoàn lặp. |
| inventory_transactions | id; product_id; batch_id; type; quantity; quantity_before/after; reserved_before/after | BIGINT; VARCHAR; INT | Nhật ký biến động lô và phần giữ; có created_by, reference_id, reason và created_at. |

Bảng 3.39. Từ điển dữ liệu giao nhận và tương tác
| Bảng | Trường trọng tâm | Kiểu chính | Ý nghĩa và ràng buộc |
| --- | --- | --- | --- |
| shipments | id; order_id; provider; provider_order_code; client_order_code; shop_id | BIGINT; VARCHAR; INT | Unique order/mã khách/mã GHN; liên kết một hồ sơ vận chuyển theo đơn. |
| shipments | customer_shipping_fee; carrier_shipping_fee; cod_amount; shipping_status; last_event_at; cod_transferred_at | DECIMAL; VARCHAR; DATETIME | Phí khách/phí hãng, COD, trạng thái và mốc sự kiện/chuyển tiền; có last_error. |
| shipment_events | id; shipment_id; event_key; event_type; shipping_status; event_at | BIGINT; VARCHAR; DATETIME | Unique shipment–event_key; lịch sử webhook/sync và mô tả. |
| reviews | id; user_id; product_id; rating; comment | BIGINT; INT; TEXT | Unique user–product; rating 1–5; được service kiểm tra đã mua/nhận. |
| contacts | id; user_id; full_name; email; phone_number; message; status; response; responded_by/at | BIGINT; VARCHAR; TEXT; TIMESTAMP | Khách có thể không có user_id; dữ liệu phản hồi và người xử lý. |
| notifications | id; user_id; type; message; link; is_read | BIGINT; VARCHAR; TEXT; BOOLEAN | Thông báo riêng theo user; link dẫn tới tài nguyên liên quan. |
| chat_messages | id; user_id; guest_token_hash; sender; message; created_at | BIGINT; CHAR(64); ENUM; TEXT; TIMESTAMP | Tách lịch sử user/guest hash; nội dung được lọc trước lưu/gửi AI. |


### 3.5.6 Chỉ mục lưu trữ và quản lý thay đổi

Chỉ mục ưu tiên email/slug/mã duy nhất, cột khóa ngoại, trạng thái và thời gian dùng trong danh sách. Các bảng lịch sử theo đơn, token theo hash, lô theo sản phẩm/hạn dùng và sự kiện theo shipment hỗ trợ truy vấn nghiệp vụ tương ứng. Sản phẩm.stock là giá trị đồng bộ để catalog đọc nhanh; nguồn quyết định khả dụng là các lô còn hạn và phần đang giữ.

Flyway thực hiện thay đổi có thứ tự, Hibernate không tự sinh schema ở cấu hình hiện tại. Khi bổ sung nghiệp vụ, tạo migration mới và kiểm tra tương thích dữ liệu đang có. Trước thay đổi cấu trúc hoặc nhập dữ liệu lên Railway cần có bản sao lưu; volume bền vững không thay thế bản sao lưu độc lập.

## 3.6 Thiết kế API

### 3.6.1 Quy ước thiết kế và phản hồi

API đặt dưới tiền tố /api, trao đổi JSON UTF-8, tách đường dẫn công khai, tài khoản, vận hành và quản trị. Tên biến {id}, {productId} hoặc {orderId} biểu diễn định danh tài nguyên tương ứng. Các endpoint trong bảng giữ đúng đường dẫn đang được controller khai báo; tham số tìm kiếm/phân trang được truyền qua query khi chức năng hỗ trợ.

Bảng 3.40. Quy ước API
| Nội dung | Quy ước |
| --- | --- |
| Phương thức | GET đọc; POST tạo/thao tác nghiệp vụ; PUT thay dữ liệu; PATCH cập nhật từng phần/trạng thái; DELETE xóa khi được phép |
| Xác thực | Authorization: Bearer <access-token>; principal quyết định user_id, role và permission |
| Khách vãng lai | X-Guest-Token cho giỏ/checkout/recovery; tra cứu đơn gửi orderId, email và lookupToken trong body |
| Thành công | ApiResponse gồm success, message, data, timestamp; tạo mới có thể trả HTTP 201 |
| Lỗi | ApiError gồm timestamp, status, error, code, message, path và fieldErrors |
| Phân trang | page, size, sort theo Spring Pageable; PageResponse chứa nội dung và metadata phân trang |
| File và dòng sự kiện | Ảnh dùng multipart/form-data; xuất báo cáo text/csv; SSE dùng text/event-stream |
| Ngoại lệ hợp đồng | VNPAY IPN trả RspCode/Message theo gateway; Return chuyển hướng; webhook GHN hợp lệ trả 204 |
| Kiểm tra đầu vào | Bean Validation tại DTO và quy tắc tại service; backend đọc lại giá, tồn và quyền |

Bảng 3.41. Nhóm mã lỗi tiêu biểu
| HTTP | Mã nghiệp vụ ví dụ | Ý nghĩa |
| --- | --- | --- |
| 400 | VALIDATION_ERROR, INVALID_ORDER_STATUS | Dữ liệu đầu vào hoặc giá trị trạng thái không hợp lệ |
| 401 | INVALID_CREDENTIALS | Chưa xác thực, thông tin đăng nhập/token không hợp lệ; thông báo cụ thể theo endpoint |
| 403 | ACCESS_DENIED, ROLE_MISMATCH | Thiếu quyền hoặc đăng nhập sai cổng vai trò |
| 404 | ORDER_NOT_FOUND, PRODUCT_NOT_FOUND | Không có tài nguyên trong phạm vi truy cập của người dùng |
| 409 | EMAIL_EXISTS, INVALID_ORDER_TRANSITION | Xung đột dữ liệu hoặc điều kiện trạng thái nghiệp vụ |
| 409 | PAYMENT_NOT_COMPLETED, PAYMENT_REFUND_REQUIRED | Chưa xác nhận thanh toán hoặc phải hoàn tiền trước khi hủy |
| 409 | REFUND_REQUEST_EXISTS, LOYALTY_INSUFFICIENT_POINTS | Có yêu cầu hoàn tiền đang xử lý hoặc thiếu điểm |
| 409 | GHN_STATUS_SYNC_ONLY, GHN_PAYMENT_PENDING | Trạng thái phải lấy từ GHN hoặc chưa đối soát thanh toán |
| 423 | ACCOUNT_LOCKED | Tài khoản tạm khóa do vượt giới hạn đăng nhập sai |

HTTP cụ thể của một lỗi phụ thuộc nơi phát sinh; bảng không thay thế hợp đồng trả lỗi của từng endpoint. Chi tiết trường sai nằm trong fieldErrors; không trả stack trace hoặc khóa kết nối cho client.

### 3.6.2 Danh mục API theo phân hệ

Để bao quát chức năng mà không lặp đặc tả CRUD, các đường dẫn cùng tài nguyên được nhóm trong một dòng. Dấu {id} là tham số đường dẫn, không phải query. Toàn bộ thao tác vẫn chịu kiểm tra quyền ở backend kể cả khi frontend đã ẩn nút.

Các hậu tố trong cột API được nối vào đường dẫn gốc ở đầu ô; dấu / đứng riêng nghĩa là chính đường dẫn gốc, không thêm một cấp tài nguyên.

Bảng 3.42. API công khai và xác thực
| Nhóm chức năng | Đường dẫn gốc và phương thức theo hậu tố | Use case |
| --- | --- | --- |
| Xác thực và phiên | /api/auth<br>POST /forgot-password; POST /reset-password; POST /register; POST /login; POST /refresh; POST /logout | UC-01–03 |
| Danh mục công khai | /api/public/categories<br>GET /; GET /{slug} | UC-04 |
| Hội thoại tư vấn | /api/public/chat/messages<br>GET/POST / | UC-20 |
| Gửi liên hệ | /api/public/contacts<br>POST / | UC-19 |
| Gợi ý coupon | /api/public/coupons<br>GET / | UC-17 |
| Trạng thái và địa chỉ GHN | /api/public/ghn<br>GET /status; GET /provinces; GET /wards | UC-07,13 |
| Webhook GHN | /api/webhooks/ghn/orders<br>POST / | UC-13 |
| Phiên giỏ và đơn khách | /api/public/guest<br>POST /sessions; GET /payment-methods; GET/DELETE /cart; POST /cart/items; PATCH/DELETE /cart/items/{id}; POST /checkout/preview; POST /orders; GET /orders/recovery; POST /orders/lookup | UC-05,08 |
| Health backend | /api/health<br>GET / | Vận hành |
| Sản phẩm công khai | /api/public/products<br>GET /; GET /{slug} | UC-04 |
| Đánh giá công khai | /api/public/products/{productId}/reviews<br>GET / | UC-04,18 |

Bảng 3.43. API tài khoản và mua sắm
| Nhóm chức năng | Đường dẫn gốc và phương thức theo hậu tố | Use case |
| --- | --- | --- |
| Giỏ và gộp giỏ | /api/cart<br>POST /merge-guest; GET/DELETE /; POST /items; PATCH/DELETE /items/{itemId}; POST /merge | UC-05,06 |
| Preview checkout | /api/checkout/preview<br>POST / | UC-07 |
| Liên hệ của tài khoản | /api/contacts/mine<br>GET / | UC-19 |
| Điểm thưởng | /api/loyalty<br>GET / | UC-18 |
| Thông báo và SSE | /api/notifications<br>GET /stream; GET /; GET /unread-count; PATCH /{id}/read; PATCH /read-all | UC-19 |
| Đơn và hậu mãi khách | /api/orders<br>POST/GET /; GET /{id}; PATCH /{id}/cancel; POST /{id}/refund-requests; GET /{id}/refund-request | UC-07,09,10 |
| Thanh toán và callback | /api/payments<br>GET /methods; GET /vnpay/return; GET /vnpay/ipn | UC-09 |
| Đánh giá đã mua | /api/reviews<br>GET /eligibility/{productId}; POST /; PUT /{id} | UC-18 |
| Địa chỉ | /api/shipping-addresses<br>GET/POST /; PUT/DELETE /{id}; PATCH /{id}/default | UC-07 |
| Hồ sơ và mật khẩu | /api/users/me<br>POST /avatar; GET/PATCH /; PUT /password | UC-03 |
| Yêu thích | /api/wishlist<br>GET /; POST /items; DELETE /items/{productId} | UC-05 |

Bảng 3.44. API quản trị
| Nhóm chức năng | Đường dẫn gốc và phương thức theo hậu tố | Use case |
| --- | --- | --- |
| Báo cáo/CSV | /api/admin/reports<br>GET /; GET /export.csv | UC-22 |
| Quản lý danh mục | /api/admin/categories<br>GET/POST /; PUT/DELETE /{categoryId} | UC-14 |
| Quản lý coupon | /api/admin/coupons<br>GET/POST /; GET/PUT/DELETE /{id}; GET /{id}/usages; PATCH /{id}/active | UC-17 |
| Vận đơn GHN | /api/admin/orders/{orderId}<br>POST /ghn/create; POST /ghn/sync; POST /ghn/cancel; GET /ghn/print; GET /ghn/events | UC-13 |
| Kho admin | /api/admin/inventory<br>GET /summary; GET /; GET /batches; GET /transactions; GET /{productId}; POST /import; POST /adjust | UC-15,16 |
| Đơn và hoàn tiền admin | /api/admin/orders<br>GET /; GET /{id}; PATCH /{id}/status; PATCH /{id}/refund-confirmation; PATCH /{id}/delivery-staff | UC-10–12 |
| Quản lý sản phẩm/ảnh | /api/admin/products<br>POST /{productId}/images/upload; GET/POST /; PUT /{productId}; PATCH /{productId}/status; POST /{productId}/images; DELETE /{productId}/images/{imageId} | UC-14 |
| Xét yêu cầu hoàn tiền | /api/admin/refund-requests<br>GET /; PATCH /{id}/approve; PATCH /{id}/reject | UC-10 |
| Danh sách role/quyền | /api/roles<br>GET / | UC-21 |
| Danh sách giao nhận | /api/admin/shipments<br>GET / | UC-13 |
| Nhà cung cấp | /api/admin/suppliers<br>GET/POST /; GET/PUT/DELETE /{id} | UC-15 |
| Upload ảnh dùng chung | /api/admin/uploads/images<br>POST / | UC-14 |
| Quản lý người dùng | /api/admin/users<br>GET /; PATCH /{userId} | UC-21 |

Bảng 3.45. API vận hành và giao hàng
| Nhóm chức năng | Đường dẫn gốc và phương thức theo hậu tố | Use case |
| --- | --- | --- |
| Giao hàng nội bộ | /api/delivery/orders<br>GET /; POST /{id}/claim; PATCH /{id}/status | UC-12 |
| Liên hệ vận hành | /api/operations/contacts<br>GET /; PATCH /{id}/reply; PATCH /{id}/resolve | UC-19 |
| Kho staff | /api/staff/inventory<br>GET /; GET /{productId}; POST /adjust | UC-16 |
| Xử lý/phân công đơn | /api/staff/orders<br>GET /delivery-staff; PATCH /{id}/delivery-staff; GET /; PATCH /{id}/status | UC-11,12 |


### 3.6.3 Đặc tả các giao dịch API trọng tâm

Bảng 3.46. Hợp đồng dữ liệu của các giao dịch trọng tâm
| API | Đầu vào chính | Kết quả và điều kiện |
| --- | --- | --- |
| POST /api/auth/login | email, password, role tùy chọn | Access/refresh token và hồ sơ; kiểm tra khóa, trạng thái và vai trò cổng |
| POST /api/checkout/preview | shippingAddressId, shippingMethod/provider, mã coupon, điểm | Breakdown giá/giảm/phí/tổng; chưa giữ kho hoặc lượt coupon |
| POST /api/orders | Dữ liệu checkout, paymentMethod, expectedTotal khi áp dụng | Order/payment và URL VNPAY nếu chọn; giữ lô, coupon, trừ điểm trong transaction |
| POST /api/public/guest/orders | Token header; liên hệ, địa chỉ, coupon, payment, idempotencyKey | order, lookupToken, replayed, paymentUrl; gửi lại cùng khóa trả đơn cũ |
| POST /api/public/guest/orders/lookup | orderId, email, lookupToken | Chi tiết đơn khi cả thông tin xác minh hợp lệ; không đưa lookupToken lên URL |
| GET /api/payments/vnpay/ipn | Tham số VNPAY và vnp_SecureHash | Kiểm tra chữ ký/tham chiếu/số tiền; cập nhật một lần và trả mã gateway |
| POST /api/admin/inventory/import | productId, quantity, importPrice, manufactureDate, expiryDate, supplierId, note | Lô mới, tồn và lịch sử nhập; hạn dùng bắt buộc với lô mới |
| POST /api/staff/inventory/adjust | batchId, quantityChange, type, reason | Điều chỉnh có lịch sử người thực hiện; không xâm phạm phần giữ |
| POST /api/webhooks/ghn/orders | Header secret và payload sự kiện GHN | Xác minh shop/vận đơn/ClientOrderCode; chống cũ/lặp; 204 nếu được tiếp nhận |
| POST /api/public/chat/messages | message, guestToken tùy chọn; JWT nếu có | Phản hồi, sản phẩm phù hợp và định danh phiên; fallback khi Gemini lỗi |

### 3.6.4 Ma trận vai trò và quyền truy cập

Bảng 3.47. Ma trận truy cập nghiệp vụ
| Nhóm chức năng | Khách | Customer | Staff | Delivery | Admin |
| --- | --- | --- | --- | --- | --- |
| Catalog, liên hệ công khai, chat | Có | Có | Công khai | Công khai | Có |
| Giỏ và đặt hàng guest | Phiên khách | Khi dùng phiên guest | — | — | — |
| Giỏ/yêu thích/địa chỉ/đơn tài khoản | — | Của mình | — | — | — |
| Đánh giá, điểm, yêu cầu hoàn tiền | — | Của mình, đủ điều kiện | — | — | Duyệt hoàn tiền |
| Hồ sơ và thông báo | — | Của mình | Của mình | Của mình | Của mình |
| Quản lý sản phẩm | — | — | manage_products | — | Có |
| Quản lý danh mục/coupon/tài khoản | — | — | — | — | Quyền tương ứng |
| Xử lý và phân công đơn nội bộ | — | — | manage_orders | — | Có |
| Nhận/cập nhật giao nội bộ | — | — | — | Đơn được giao | Quản lý/phân công |
| Xem/điều chỉnh kho | — | — | Endpoint staff | — | Có |
| Nhập kho và nhà cung cấp | — | — | — | — | Có |
| Liên hệ vận hành | — | — | manage_contacts | — | Có |
| GHN, báo cáo, duyệt hoàn tiền | — | — | — | — | Có |

Các quyền hiện có gồm manage_users, manage_products, manage_orders, manage_categories, manage_contacts, manage_deliveries và manage_coupons. Một số API như kho, vận đơn, báo cáo và hoàn tiền yêu cầu role ADMIN trực tiếp; không thể suy ra quyền chỉ từ tiền tố /admin. Khách đọc đơn guest bằng bộ thông tin xác minh, không nhận quyền tài khoản. Khóa quản trị không được cấp từ dữ liệu role do client tự gửi.

## 3.7 Thiết kế giao diện

### 3.7.1 Nguyên tắc và sơ đồ điều hướng

Giao diện tổ chức thành cửa hàng và ba portal vận hành. Cửa hàng ưu tiên tìm sản phẩm, kiểm tra giỏ và thanh toán; portal dùng menu theo nhiệm vụ. Biểu mẫu hiển thị lỗi tại trường và phản hồi từ API. Khi một dịch vụ thanh toán/giao hàng không khả dụng, giao diện thể hiện trạng thái thay vì cho người dùng lựa chọn một phương án không thực hiện được.

![Hình 3.23. Sơ đồ điều hướng giao diện GreenFarm](figures_chuong3/navigation.png)

Hình 3.23. Sơ đồ điều hướng giao diện GreenFarm

### 3.7.2 Danh sách màn hình theo use case

Các màn hình động dùng [id] hoặc [slug] theo App Router. Modal/khung chat và thông báo là thành phần dùng chung, không được tính như tuyến trang độc lập.

Bảng 3.48. Danh sách màn hình và chức năng tương ứng
| Nhóm màn hình | Tuyến giao diện | Use case | Dữ liệu và thao tác |
| --- | --- | --- | --- |
| Cửa hàng | /, /store, /products, /categories/[slug], /products/[slug] | UC-04 | Catalog công khai, danh mục, ảnh và đánh giá |
| Xác thực khách | /login, /register, /forgot-password, /reset-password | UC-01–03 | Đăng ký/đăng nhập/khôi phục; auth API |
| Hồ sơ cá nhân | /profile | UC-03 | Hồ sơ, avatar, mật khẩu |
| Giỏ và yêu thích | /cart, /wishlist | UC-05,06 | Giỏ guest hoặc tài khoản; gộp và yêu thích |
| Địa chỉ và checkout | /addresses, /checkout | UC-07,08,17,18 | Địa chỉ, preview, coupon/điểm, provider và payment |
| Đơn tài khoản | /orders, /orders/[id], /orders/[id]/invoice | UC-09,10,18 | Chi tiết, lịch sử, hủy/hoàn, đánh giá và in hóa đơn |
| Đơn khách và kết quả trả tiền | /guest-orders, /guest-orders/[id]/invoice, /payment-result | UC-08,09 | Xác minh tra cứu, hóa đơn khách và kết quả backend |
| Điểm và hỗ trợ | /points, /contact | UC-18,19 | Số dư/lịch sử điểm, gửi và xem liên hệ |
| Cổng admin | /admin/login, /admin, /admin/profile | UC-02,03,22 | Đăng nhập, dashboard/báo cáo và hồ sơ |
| Catalog và tài khoản admin | /admin/categories, /admin/products, /admin/users | UC-14,21 | Quản lý danh mục/sản phẩm/ảnh và người dùng |
| Đơn và giao nhận admin | /admin/orders, /admin/orders/[id], /admin/delivery | UC-10–13 | Vận hành đơn, hoàn tiền, phân công và vận đơn GHN |
| Kho ưu đãi hỗ trợ admin | /admin/inventory, /admin/coupons, /admin/contacts | UC-15–17,19 | Lô/nhà cung cấp, coupon và liên hệ |
| Portal staff | /staff/login, /staff, /staff/inventory, /staff/contacts | UC-02,11,12,16,19 | Xử lý đơn, phân công, kiểm kê và hỗ trợ |
| Portal giao hàng | /delivery/login, /delivery | UC-02,12 | Đơn được giao, nhận và cập nhật kết quả |

Khung chatbot và menu thông báo có mặt trên các layout phù hợp, tương ứng UC-20 và UC-19; báo cáo nằm ở dashboard admin, quản lý nhà cung cấp nằm trong khu vực kho, không tạo thêm tuyến trang không có trong mã nguồn.


### 3.7.3 Bố cục các màn hình trọng tâm

Trang catalog đặt tìm kiếm và bộ lọc cạnh danh sách sản phẩm; thẻ thể hiện ảnh, tên, giá, đơn vị và khả dụng. Chi tiết bổ sung nguồn gốc, chứng nhận, bảo quản, đánh giá và lựa chọn thêm giỏ. Nội dung phụ thuộc điều kiện được ẩn hoặc thông báo phù hợp khi chưa có dữ liệu.

![Hình 3.24. Thiết kế bố cục trang mua sắm và checkout](figures_chuong3/wire_store.png)

Hình 3.24. Thiết kế bố cục trang mua sắm và checkout

Checkout chia vùng địa chỉ/phương thức với vùng tổng tiền. Tạm tính, giảm hàng, điểm, phí và giảm phí được tách để khách hiểu số thanh toán. Mọi thay đổi địa chỉ, coupon, điểm hoặc nhà vận chuyển phải gọi preview lại. Nút đặt hàng phản ánh trạng thái đang gửi; khóa checkout khách được giữ để khôi phục kết quả khi mất phản hồi.

Portal quản trị dùng sidebar và vùng nội dung chính. Màn hình đơn hiển thị nhóm trạng thái, bộ lọc và thao tác theo điều kiện; chi tiết tập hợp dòng hàng, payment, vận chuyển, lịch sử và hoàn tiền. Màn kho ưu tiên tồn khả dụng, phần giữ, lô gần/hết hạn và lịch sử để tránh hiểu nhầm stock với số lượng vật lý.

![Hình 3.25. Thiết kế bố cục portal quản trị và màn kho](figures_chuong3/wire_admin.png)

Hình 3.25. Thiết kế bố cục portal quản trị và màn kho

### 3.7.4 Thiết kế tương tác và phản hồi

Bảng 3.49. Tương tác của các màn hình trọng tâm
| Màn hình hoặc thao tác | Cách tương tác và xử lý lỗi |
| --- | --- |
| Đăng nhập/khôi phục mật khẩu | Hiển thị lỗi đúng trường; thông báo khóa/sai cổng; yêu cầu quên mật khẩu dùng thông điệp chung để không lộ email có tài khoản |
| Catalog/giỏ | Loading, không có kết quả, hết hàng; đọc lại giỏ từ server; khi đổi tài khoản làm mới dữ liệu liên quan |
| Checkout | Kiểm tra bắt buộc, preview lại giá, chặn gửi trùng tại UI; lỗi tồn/coupon/phí yêu cầu rà soát trước khi đặt lại |
| Kết quả VNPAY | Hiển thị theo dữ liệu backend; không coi query hoặc thông báo thành công ở trình duyệt là căn cứ thanh toán |
| Tra cứu đơn guest | Nhập mã đơn, email và mã xác minh; không đặt mã bí mật trong URL; thiết bị lưu tối đa các đơn theo cơ chế hiện tại |
| Xử lý/giao đơn | Chỉ hiện bước hợp lệ; phân công người đang hoạt động; yêu cầu nhận đơn và lý do giao thất bại |
| Kho/coupon/hoàn tiền | Hiển thị lý do bị từ chối từ backend; xác nhận thao tác ảnh hưởng nghiệp vụ; khóa gửi khi đang xử lý |
| Thông báo/hỗ trợ | Đếm chưa đọc, đánh dấu đọc, tải lại danh sách khi cần; khách theo dõi phản hồi liên hệ của mình |
| Chatbot | Trạng thái đang trả lời, gợi ý/thẻ sản phẩm; nội dung tư vấn không được thể hiện như xác nhận đơn hoặc thanh toán |

Các thiết kế trên là cơ sở triển khai và kiểm thử ở các chương sau. Phần nghiệm thu cần đối chiếu giao diện thực tế, dữ liệu vận hành và kết quả kiểm thử; không lấy sơ đồ thiết kế thay cho bằng chứng chạy thành công.

### Tài liệu đối chiếu cho nội dung triển khai

[1] Vercel. Environment variables và Next.js on Vercel. https://vercel.com/docs/environment-variables ; https://vercel.com/docs/frameworks/full-stack/nextjs . Truy cập ngày 06/11/2026.

[2] Railway. Healthchecks. https://docs.railway.com/deployments/healthchecks . Truy cập ngày 06/11/2026.
