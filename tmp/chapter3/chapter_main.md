# CHƯƠNG 3 PHÂN TÍCH THIẾT KẾ HỆ THỐNG

Chương này xác định yêu cầu và thiết kế hệ thống kinh doanh nông sản trực tuyến GreenFarm. Nội dung được tổ chức từ phạm vi, tác nhân và quy tắc nghiệp vụ đến use case, kiến trúc, cơ sở dữ liệu, API và giao diện. Thiết kế tập trung vào tính nhất quán giữa đơn hàng, thanh toán và tồn kho; đồng thời hỗ trợ khách vãng lai, khách hàng có tài khoản và các bộ phận vận hành.

## 3.1 Phạm vi và tác nhân của hệ thống

### 3.1.1 Phạm vi hệ thống

GreenFarm phục vụ hoạt động bán hàng của một đơn vị kinh doanh nông sản. Khách có thể tìm sản phẩm, đặt hàng và theo dõi đơn; bộ phận vận hành quản lý hàng hóa, kho, giao nhận và chăm sóc khách hàng. Hệ thống không tổ chức gian hàng cho nhiều nhà bán độc lập.

@TABLE Phạm vi của hệ thống GreenFarm
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

@TABLE Các tác nhân của hệ thống
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

@FIG context Sơ đồ ngữ cảnh hệ thống GreenFarm

## 3.2 Phân tích yêu cầu hệ thống

### 3.2.1 Yêu cầu chức năng

Các yêu cầu được nhóm theo mục tiêu sử dụng. Mức ưu tiên Cao dành cho chức năng hình thành giao dịch và bảo vệ dữ liệu; mức Trung bình dành cho chức năng hỗ trợ trải nghiệm và quản trị.

@TABLE Yêu cầu chức năng của GreenFarm
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

@TABLE Yêu cầu phi chức năng
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

@TABLE Các quy tắc nghiệp vụ trọng tâm
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

@FIG use_overview Sơ đồ use case tổng quát của GreenFarm

### 3.3.2 Use case mua sắm và vận hành

Khách vãng lai có thể hoàn thành giao dịch bằng phiên khách; khách hàng có thêm dữ liệu cá nhân, điểm và quyền hậu mãi. Staff xử lý đơn và kho trong phạm vi cho phép, còn quản trị viên quản lý cấu hình nghiệp vụ và quyết định hoàn tiền. Nhân viên giao hàng chỉ thao tác trên các đơn nội bộ được giao cho mình.

@FIG use_shopping Sơ đồ use case của khách vãng lai và khách hàng

@FIG use_operations Sơ đồ use case của quản trị và nhân viên vận hành

### 3.3.3 Danh sách use case

@UC_INDEX

### 3.3.4 Đặc tả các use case

Các đặc tả dưới đây dùng cùng biểu mẫu Thuộc tính–Nội dung của báo cáo tham chiếu. Những thao tác cùng mục tiêu như thêm/sửa/xóa hoặc tải lại dữ liệu được gộp trong một use case; các giao dịch có quy tắc riêng vẫn được tách để thể hiện điều kiện, luồng thay thế và ngoại lệ.

@UC_DETAILS

## 3.4 Thiết kế kiến trúc hệ thống

### 3.4.1 Lựa chọn mô hình kiến trúc

GreenFarm sử dụng mô hình web client–server, frontend Next.js và backend Spring Boot cung cấp REST API. Backend là một ứng dụng nguyên khối được tổ chức theo các lớp và dịch vụ nghiệp vụ, cùng đóng gói trong một JAR. Lựa chọn này giúp đặt đơn, giữ kho, ghi coupon và trừ điểm trong cùng giao dịch dữ liệu.

@TABLE Quyết định kiến trúc và đánh đổi
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

@FIG architecture Kiến trúc tổng thể của hệ thống GreenFarm

@TABLE Trách nhiệm các thành phần
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

@FIG deployment Sơ đồ triển khai GreenFarm trên Vercel và Railway

Frontend sử dụng thư mục gốc frontend, cài đặt theo package-lock.json và build Next.js. NEXT_PUBLIC_API_BASE_URL trỏ tới URL backend kèm /api. Biến có tiền tố NEXT_PUBLIC_ có thể xuất hiện trong mã gửi xuống trình duyệt, vì vậy chỉ chứa địa chỉ công khai. Việc thay biến build của frontend được áp dụng qua bản triển khai mới theo cơ chế Vercel [1].

Backend được build từ Dockerfile tại gốc repository; server.port đọc PORT do nền tảng cung cấp. railway.json khai báo healthcheck /api/health, thời gian chờ 300 giây và khởi động lại khi lỗi tối đa 10 lần. Healthcheck hỗ trợ quyết định đưa bản triển khai vào phục vụ, không thay thế giám sát liên tục hoặc kiểm tra toàn bộ nghiệp vụ [2].

@TABLE Cấu hình các thành phần triển khai
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

@FIG ai Luồng xử lý và kiểm soát dữ liệu của chatbot

ChatbotService tạo prompt tại backend, gọi Gemini khi có cấu hình và xử lý phản hồi trước khi trả giao diện. Thẻ sản phẩm được đối chiếu catalog. Khi không có khóa hoặc dịch vụ lỗi, bộ trả lời dự phòng sử dụng dữ liệu công khai. Cơ chế này hỗ trợ tư vấn nhưng không bảo đảm loại bỏ mọi sai sót của mô hình. AI không được quyết định giá, trừ kho, tạo đơn hoặc xác nhận thanh toán.

## 3.5 Thiết kế cơ sở dữ liệu

### 3.5.1 Mô hình dữ liệu khái niệm

Mô hình xoay quanh sản phẩm, người mua và đơn hàng. Danh mục phân loại sản phẩm; mỗi sản phẩm có nhiều ảnh và nhiều lô kho. Người mua có thể là tài khoản hoặc phiên khách. Đơn lưu các dòng hàng, lịch sử, giao dịch thanh toán và thông tin giao nhận; phân bổ kho nối từng dòng hàng với các lô thực tế.

@FIG er_concept Sơ đồ thực thể liên kết mức khái quát

### 3.5.2 Tổng quan lược đồ quan hệ

Các migration V1–V25 khai báo 36 tên bảng khác nhau, gồm 35 bảng phục vụ mô hình hiện tại và bảng password_reset_tokens kế thừa. Chức năng khôi phục mật khẩu hiện dùng greenfarm_password_reset_tokens. flyway_schema_history là bảng kỹ thuật do Flyway quản lý, không tính vào 36 bảng này.

@TABLE Phân nhóm các bảng dữ liệu
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

@TABLE Quan hệ chính và ràng buộc nghiệp vụ
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

@FIG er_accounts ERD nhóm tài khoản phiên và giỏ hàng

@FIG er_commerce ERD nhóm catalog đơn hàng và thanh toán

@FIG er_inventory ERD nhóm nhà cung cấp kho và phân bổ lô

@FIG er_engagement ERD nhóm ưu đãi điểm tương tác và giao nhận

### 3.5.5 Từ điển dữ liệu trọng tâm

Từ điển tập trung vào trường nhận diện, quan hệ và dữ liệu quyết định nghiệp vụ; các cột mô tả phụ, created_at và updated_at được lược khi không ảnh hưởng cách hiểu. Kiểu ghi theo SQL migration; cột trạng thái có thể là ENUM hoặc chuỗi được chuyển đổi từ enum Java tùy bảng.

@DATA_DICTIONARY

### 3.5.6 Chỉ mục lưu trữ và quản lý thay đổi

Chỉ mục ưu tiên email/slug/mã duy nhất, cột khóa ngoại, trạng thái và thời gian dùng trong danh sách. Các bảng lịch sử theo đơn, token theo hash, lô theo sản phẩm/hạn dùng và sự kiện theo shipment hỗ trợ truy vấn nghiệp vụ tương ứng. Sản phẩm.stock là giá trị đồng bộ để catalog đọc nhanh; nguồn quyết định khả dụng là các lô còn hạn và phần đang giữ.

Flyway thực hiện thay đổi có thứ tự, Hibernate không tự sinh schema ở cấu hình hiện tại. Khi bổ sung nghiệp vụ, tạo migration mới và kiểm tra tương thích dữ liệu đang có. Trước thay đổi cấu trúc hoặc nhập dữ liệu lên Railway cần có bản sao lưu; volume bền vững không thay thế bản sao lưu độc lập.

## 3.6 Thiết kế API

### 3.6.1 Quy ước thiết kế và phản hồi

API đặt dưới tiền tố /api, trao đổi JSON UTF-8, tách đường dẫn công khai, tài khoản, vận hành và quản trị. Tên biến {id}, {productId} hoặc {orderId} biểu diễn định danh tài nguyên tương ứng. Các endpoint trong bảng giữ đúng đường dẫn đang được controller khai báo; tham số tìm kiếm/phân trang được truyền qua query khi chức năng hỗ trợ.

@TABLE Quy ước API
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

@TABLE Nhóm mã lỗi tiêu biểu
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

@API_CATALOG

### 3.6.3 Đặc tả các giao dịch API trọng tâm

@TABLE Hợp đồng dữ liệu của các giao dịch trọng tâm
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

@TABLE Ma trận truy cập nghiệp vụ
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

@FIG navigation Sơ đồ điều hướng giao diện GreenFarm

### 3.7.2 Danh sách màn hình theo use case

Các màn hình động dùng [id] hoặc [slug] theo App Router. Modal/khung chat và thông báo là thành phần dùng chung, không được tính như tuyến trang độc lập.

@SCREEN_CATALOG

### 3.7.3 Bố cục các màn hình trọng tâm

Trang catalog đặt tìm kiếm và bộ lọc cạnh danh sách sản phẩm; thẻ thể hiện ảnh, tên, giá, đơn vị và khả dụng. Chi tiết bổ sung nguồn gốc, chứng nhận, bảo quản, đánh giá và lựa chọn thêm giỏ. Nội dung phụ thuộc điều kiện được ẩn hoặc thông báo phù hợp khi chưa có dữ liệu.

@FIG wire_store Thiết kế bố cục trang mua sắm và checkout

Checkout chia vùng địa chỉ/phương thức với vùng tổng tiền. Tạm tính, giảm hàng, điểm, phí và giảm phí được tách để khách hiểu số thanh toán. Mọi thay đổi địa chỉ, coupon, điểm hoặc nhà vận chuyển phải gọi preview lại. Nút đặt hàng phản ánh trạng thái đang gửi; khóa checkout khách được giữ để khôi phục kết quả khi mất phản hồi.

Portal quản trị dùng sidebar và vùng nội dung chính. Màn hình đơn hiển thị nhóm trạng thái, bộ lọc và thao tác theo điều kiện; chi tiết tập hợp dòng hàng, payment, vận chuyển, lịch sử và hoàn tiền. Màn kho ưu tiên tồn khả dụng, phần giữ, lô gần/hết hạn và lịch sử để tránh hiểu nhầm stock với số lượng vật lý.

@FIG wire_admin Thiết kế bố cục portal quản trị và màn kho

### 3.7.4 Thiết kế tương tác và phản hồi

@TABLE Tương tác của các màn hình trọng tâm
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
