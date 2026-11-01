# CHƯƠNG 2 CƠ SỞ LÝ THUYẾT VÀ CÔNG NGHỆ

Chương này trình bày các cơ sở lý thuyết và công nghệ được vận dụng để xây dựng GreenFarm, một website kinh doanh nông sản trực tuyến. Nội dung bao gồm nghiệp vụ thương mại điện tử nông sản, kiến trúc ứng dụng web, quản lý dữ liệu và bảo mật, trợ lý tư vấn sử dụng trí tuệ nhân tạo và các công nghệ trong dự án. Đây là cơ sở cho việc phân tích yêu cầu và thiết kế hệ thống ở chương tiếp theo.

## 2.1 Cơ sở lý thuyết về thương mại điện tử nông sản

### 2.1.1 Hệ thống thương mại điện tử

Hệ thống thương mại điện tử là hệ thống thông tin hỗ trợ giới thiệu sản phẩm, tiếp nhận yêu cầu mua hàng và tổ chức các hoạt động liên quan thông qua môi trường mạng. Trong website bán hàng, giao diện sản phẩm là điểm tiếp xúc ban đầu; phía sau còn có nghiệp vụ quản lý khách hàng, giỏ hàng, đơn hàng, thanh toán, giao nhận và chăm sóc sau mua.

Đối với GreenFarm, người mua có thể tìm kiếm nông sản, xem mô tả, lựa chọn số lượng, cung cấp thông tin nhận hàng và theo dõi quá trình xử lý đơn. Phía cửa hàng cần quản lý danh mục, sản phẩm, nguồn cung, số lượng tồn, chương trình ưu đãi và yêu cầu của khách hàng. Dữ liệu giữa các khâu phải thống nhất để hàng đã hết không tiếp tục được nhận đặt hoặc giao dịch thất bại không bị ghi nhận là đã thanh toán.

Trong phạm vi đề tài, GreenFarm được tổ chức theo mô hình cửa hàng bán trực tiếp cho người tiêu dùng. Nhà cung cấp được quản lý phục vụ nhập hàng và truy vết nguồn cung; hệ thống không được mô tả như sàn giao dịch nhiều nhà bán có gian hàng và cơ chế chia doanh thu riêng.

### 2.1.2 Đặc điểm của nông sản trong kinh doanh trực tuyến

Nông sản có những đặc điểm ảnh hưởng trực tiếp đến thiết kế phần mềm. Sản phẩm có thể được bán theo kilôgam, túi hoặc hộp; thời gian bảo quản và chất lượng phụ thuộc từng loại hàng. Với các lô có hạn sử dụng, số lượng còn trong kho chưa chắc đã là số lượng có thể tiếp tục bán.

Thông tin mô tả cần giúp người mua nhận biết tên sản phẩm, đơn vị bán, giá, hình ảnh, nguồn gốc, chứng nhận và cách bảo quản khi có dữ liệu tương ứng. Các trường này hỗ trợ minh bạch nội dung giới thiệu. Việc lưu nguồn gốc hoặc chứng nhận trong phần mềm không đồng nghĩa hệ thống đã kiểm định chất lượng hoặc xác minh độc lập chứng nhận đó.

Đặc điểm dễ hư hỏng yêu cầu cửa hàng theo dõi nhập hàng, giữ hàng cho đơn, xuất hàng và điều chỉnh do hỏng hoặc hết hạn. Do đó, quản lý kho nông sản cần gắn với lô hàng thay vì chỉ cập nhật một con số tổng trên sản phẩm.

Bảng 2.1 Đặc điểm nghiệp vụ nông sản và yêu cầu đối với hệ thống

| Đặc điểm | Yêu cầu đối với hệ thống | Vận dụng trong GreenFarm |
| --- | --- | --- |
| Có nhiều đơn vị bán | Gắn giá và số lượng với đơn vị | Hiển thị đơn vị trên catalog và đơn |
| Hàng có hạn sử dụng | Phân biệt tồn vật lý và hàng còn bán được | Theo dõi hạn dùng, trạng thái lô |
| Nhiều lần nhập cùng sản phẩm | Ghi nhận riêng từng đợt nhập | Quản lý lô và nhà cung cấp |
| Giá, mô tả có thể thay đổi | Bảo toàn thông tin lúc mua | Lưu bản chụp trong đơn |
| Nhiều khách cùng đặt | Kiểm soát số lượng đồng thời | Khóa dữ liệu trong giao dịch |
| Mua hàng từ xa | Cung cấp thông tin và phản hồi | Nguồn gốc, đánh giá, liên hệ |

### 2.1.3 Giỏ hàng và quy trình đặt hàng

Giỏ hàng biểu diễn nhu cầu mua tạm thời. Mỗi dòng giỏ tham chiếu một sản phẩm và số lượng mong muốn. Giỏ chưa phải đơn hàng đã được cửa hàng chấp nhận; giá, trạng thái bán và số lượng khả dụng có thể thay đổi từ khi thêm giỏ đến lúc thanh toán.

Checkout cần kiểm tra lại dữ liệu trên máy chủ. Máy khách gửi lựa chọn sản phẩm, địa chỉ, phương thức giao nhận, thanh toán và ưu đãi; máy chủ xác định mức giá và điều kiện hợp lệ. Dù giao diện đã hiển thị bản xem trước, yêu cầu tạo đơn vẫn cần được kiểm tra lại để tránh sử dụng dữ liệu cũ.

GreenFarm hỗ trợ người có tài khoản và khách chưa đăng nhập. Khách vãng lai được nhận diện bằng phiên khách để quản lý giỏ; khi đăng nhập có thể gộp giỏ theo quy tắc hệ thống. Đơn khách vãng lai cần cơ chế xác minh riêng khi tra cứu, thay vì công khai dữ liệu chỉ từ mã đơn có thể đoán được.

Quan hệ tính tiền tổng quát là: tổng thanh toán bằng tiền hàng cộng phí vận chuyển, trừ giảm giá hợp lệ và phần điểm được sử dụng. Các khoản giảm cần giới hạn để tổng tiền không âm và tránh áp dụng quyền lợi nhiều lần. GreenFarm tính toán tại backend, không lấy tổng tiền do trình duyệt gửi lên làm kết quả quyết định.

[FIGURE:commerce]

Hình 2.1 Chu trình nghiệp vụ bán hàng trực tuyến ở mức khái quát

Hình 2.1 thể hiện quan hệ giữa mua hàng và vận hành cửa hàng. Thanh toán, trạng thái đơn và giao nhận có liên quan nhưng cần được theo dõi riêng; xác nhận thu tiền không tự động có nghĩa đã giao hàng.

### 2.1.4 Quản lý tồn kho theo lô và nguyên tắc FEFO

Quản lý theo lô ghi nhận riêng từng nhóm hàng nhập có chung thông tin như thời điểm nhập, nhà cung cấp, giá nhập và hạn sử dụng. Một sản phẩm có thể có nhiều lô với thời hạn bảo quản khác nhau.

FEFO, viết tắt của First Expired First Out, là nguyên tắc ưu tiên xuất lô có hạn sử dụng sớm hơn. Nguyên tắc này phù hợp với hàng dễ hỏng vì ưu tiên hàng gần hết hạn trước hàng có thời hạn dài hơn. FEFO khác FIFO ở tiêu chí sắp xếp: FIFO dựa vào thứ tự nhập, FEFO ưu tiên hạn dùng. Hai thứ tự có thể trùng nhau nhưng không luôn tương đương.

Trong GreenFarm, lượng vật lý còn lại và lượng đã giữ cho đơn được theo dõi riêng. Với lô còn đủ điều kiện bán, lượng khả dụng bằng lượng còn lại trừ lượng đã giữ. Lô hết hạn không được phân bổ cho đơn mới dù còn hàng vật lý. Khi đơn chuyển sang bước xử lý thích hợp, phần đã giữ chuyển thành lượng đã xuất; việc hủy cần giải phóng hoặc hoàn lại hàng theo trạng thái thực tế.

Ví dụ, lô A còn 10 đơn vị và đã giữ 3; lô B còn 20 đơn vị và chưa giữ. Nếu cả hai còn hạn thì tổng khả dụng là 27 đơn vị. Khi nhận thêm đơn 12 đơn vị và A hết hạn trước, hệ thống ưu tiên 7 đơn vị từ A, sau đó 5 từ B. Đây là ví dụ minh họa, không phải số liệu bán hàng thực tế của dự án.

### 2.1.5 Vòng đời đơn hàng và thanh toán

Vòng đời đơn hàng mô tả trạng thái từ khi tiếp nhận đến khi hoàn tất hoặc hủy. Kiểm soát chuyển trạng thái ngăn những thao tác không phù hợp, chẳng hạn đánh dấu giao thành công khi chưa giao hoặc phân công người giao cho đơn chưa sẵn sàng.

GreenFarm quản lý các bước tiếp nhận, xử lý, sẵn sàng giao, đang giao, giao thành công và hoàn tất; đồng thời ghi nhận hủy hoặc giao thất bại. Lịch sử trạng thái cho phép tra cứu diễn biến. Các thay đổi tồn kho, coupon và thanh toán phải được thực hiện theo điều kiện của từng bước.

COD là hình thức thu tiền khi giao hàng. Với thanh toán trực tuyến, hệ thống chuyển người mua tới cổng thanh toán và nhận kết quả qua callback. Trang trình duyệt quay về chỉ là một phần của luồng; máy chủ phải kiểm tra chữ ký, mã giao dịch và số tiền trước khi ghi nhận kết quả. GreenFarm tích hợp VNPAY với cấu hình mặc định phục vụ sandbox; tích hợp này không đồng nghĩa dự án đã được chấp thuận thu tiền thật.

### 2.1.6 Chăm sóc khách hàng và chương trình tích điểm

Chăm sóc khách hàng bao gồm cung cấp thông tin trước mua, hỗ trợ theo dõi đơn, tiếp nhận phản hồi và xử lý yêu cầu sau mua. Đánh giá sản phẩm cần gắn với trải nghiệm mua hợp lệ để hạn chế phản hồi không phản ánh giao dịch thực tế.

Điểm thưởng là một loại quyền lợi có giá trị sử dụng trong hệ thống. Do điểm có thể đổi thành giảm giá, việc cộng, trừ và hoàn điểm cần lịch sử, điều kiện áp dụng và kiểm soát lặp. Nếu thao tác xác nhận đơn hoặc đánh giá được gửi lại, hệ thống không được cấp điểm nhiều lần cho cùng sự kiện.

GreenFarm kết hợp đánh giá, liên hệ hỗ trợ, thông báo và điểm thưởng. Các chức năng này bổ sung cho quá trình mua hàng; dữ liệu thanh toán, kho và đơn vẫn là căn cứ quyết định của nghiệp vụ bán hàng.

## 2.2 Cơ sở lý thuyết về kiến trúc phần mềm

### 2.2.1 Mô hình client server

Mô hình client server phân chia ứng dụng thành bên cung cấp giao diện và bên xử lý yêu cầu. Trình duyệt đảm nhiệm tương tác; máy chủ kiểm tra dữ liệu, thực hiện nghiệp vụ và đọc ghi dữ liệu. Sự phân chia giúp giao diện và phần xử lý có thể phát triển, triển khai riêng [1].

GreenFarm sử dụng Next.js và React ở frontend, Spring Boot ở backend. Hai thành phần trao đổi qua API. MySQL được backend truy cập, không công khai trực tiếp để trình duyệt truy vấn. Các dịch vụ thanh toán, lưu ảnh và AI được kết nối theo trách nhiệm của từng thành phần.

Khi frontend và backend ở hai tên miền khác nhau, backend cần cho phép nguồn frontend thích hợp qua CORS. CORS là cơ chế kiểm soát truy cập của trình duyệt; không thay thế xác thực và phân quyền tại API.

[FIGURE:architecture]

Hình 2.2 Mô hình giao tiếp giữa các thành phần GreenFarm

Hình 2.2 biểu diễn quan hệ logic, không xác định nhà cung cấp hosting bắt buộc. Thay đổi nơi đặt frontend hoặc backend không làm thay đổi trách nhiệm nghiệp vụ của các thành phần.

### 2.2.2 Kết xuất giao diện và tương tác trong ứng dụng web

Ứng dụng web hiện đại có thể kết hợp nội dung kết xuất phía máy chủ với tương tác phía trình duyệt. Trong Next.js App Router, Server Components và Client Components có vai trò khác nhau; thành phần cần trạng thái tương tác, sự kiện hoặc API của trình duyệt được đặt ở phía client phù hợp [2].

Website bán hàng cần phối hợp trình bày và điều hướng với đổi bộ lọc, thêm giỏ, nhập địa chỉ và theo dõi thao tác. React tổ chức giao diện thành component tái sử dụng như thẻ sản phẩm, hộp thoại và biểu mẫu.

GreenFarm sử dụng Next.js nên không nên đồng nhất toàn bộ ứng dụng với SPA chỉ kết xuất ở trình duyệt. Cách kết xuất phụ thuộc cấu trúc trang và component. Dù tải dữ liệu theo cách nào, thao tác ảnh hưởng tới tiền, quyền và kho vẫn phải được kiểm tra tại backend.

### 2.2.3 Kiến trúc REST và giao tiếp HTTP

REST là phong cách kiến trúc cho hệ thống phân tán được Roy Fielding trình bày. Phân tách client server, yêu cầu tự chứa thông tin cần để xử lý và giao diện thống nhất là những nền tảng để tổ chức trao đổi tài nguyên trên Web [1].

Trong API theo hướng REST, tài nguyên được định danh bằng URI và phương thức HTTP diễn đạt thao tác. GreenFarm có các nhóm sản phẩm, giỏ, đơn và tài khoản. Dữ liệu chủ yếu ở dạng JSON; phản hồi kèm mã HTTP giúp giao diện phân biệt thành công, lỗi đầu vào, thiếu xác thực hoặc không đủ quyền.

Bảng 2.2 Phương thức HTTP và ví dụ trong GreenFarm

| Phương thức | Ý nghĩa thường dùng | Ví dụ trong dự án |
| --- | --- | --- |
| GET | Đọc tài nguyên | /api/orders |
| POST | Tạo tài nguyên hoặc thực hiện nghiệp vụ | /api/orders |
| PUT | Cập nhật theo hợp đồng API | /api/shipping-addresses/{id} |
| PATCH | Cập nhật một phần hoặc trạng thái | /api/admin/orders/{id}/status |
| DELETE | Xóa tài nguyên được phép xóa | /api/shipping-addresses/{id} |

Yêu cầu có thể bị từ chối dù JSON đúng, chẳng hạn địa chỉ không thuộc người dùng hoặc không đủ hàng. Kiểm tra cấu trúc và kiểm tra nghiệp vụ là hai bước bổ sung. POST quan trọng cần chống lặp theo nghiệp vụ; không thể mặc định gửi lại yêu cầu tạo đơn là an toàn.

### 2.2.4 Kiến trúc phân lớp

Backend GreenFarm tổ chức Controller tiếp nhận HTTP và chuyển dữ liệu tới Service; Service xử lý điều kiện nghiệp vụ và điều phối giao dịch; Repository đọc ghi qua JPA. Entity ánh xạ đối tượng tới bảng, còn DTO mô tả dữ liệu đầu vào và đầu ra API.

Khi tạo đơn, Controller nhận lựa chọn, Service kiểm tra địa chỉ, hàng hóa, kho và ưu đãi rồi phối hợp Repository để lưu dữ liệu. Quy tắc tính tiền không nên được sao chép ở từng Controller vì có thể làm kết quả khác nhau giữa khách đăng nhập và khách vãng lai.

Phân lớp hỗ trợ kiểm thử theo trách nhiệm: Service kiểm tra nghiệp vụ, Controller kiểm tra HTTP và quyền truy cập. Tuy nhiên, nhiều lớp không tự bảo đảm thiết kế tốt; quy tắc chung vẫn cần được đặt đúng vị trí và tránh phụ thuộc vòng.

### 2.2.5 Quản lý trạng thái phía client

Trạng thái client gồm giao diện, biểu mẫu, phiên và dữ liệu máy chủ. Trạng thái giao diện có tab hoặc hộp thoại đang mở; biểu mẫu có giá trị đang nhập. Sản phẩm, giỏ và đơn thuộc trạng thái máy chủ vì có thể được thay đổi bởi người khác hoặc tiến trình backend.

TanStack Query hỗ trợ lấy, lưu đệm và đồng bộ dữ liệu máy chủ. Sau một thay đổi thành công, ứng dụng có thể làm mất hiệu lực truy vấn liên quan để lấy lại dữ liệu [3]. GreenFarm kết hợp thư viện này với Zustand cho trạng thái dùng chung, React Hook Form và Zod cho biểu mẫu.

Phân chia trách nhiệm hạn chế lưu nhiều bản sao cùng dữ liệu. Tồn kho từ lần tải trước chỉ hỗ trợ lựa chọn, không thay thế kiểm tra khi tạo đơn. Việc ẩn nút quản trị cũng chỉ phục vụ trải nghiệm; quyền thao tác phải do backend quyết định.

## 2.3 Cơ sở lý thuyết về dữ liệu và bảo mật

### 2.3.1 Cơ sở dữ liệu quan hệ

Cơ sở dữ liệu quan hệ tổ chức dữ liệu thành bảng, mỗi dòng là một bản ghi và mỗi cột mô tả thuộc tính. Khóa chính xác định bản ghi, khóa ngoại liên kết bảng, còn ràng buộc duy nhất ngăn trùng lặp ở những trường cần bảo đảm tính duy nhất.

GreenFarm có nhiều quan hệ rõ ràng: một người dùng có nhiều địa chỉ và đơn; một đơn có nhiều dòng sản phẩm; một sản phẩm có nhiều ảnh và lô nhập; một vai trò có nhiều quyền. Mô hình quan hệ phù hợp để biểu diễn liên kết và truy vấn tổng hợp phục vụ quản trị.

MySQL lưu dữ liệu nghiệp vụ. Với bảng InnoDB, giao dịch và khóa hỗ trợ xử lý đồng thời [4]. Chỉ mục được xây dựng theo nhu cầu tra cứu như lọc trạng thái hoặc tìm đơn theo người dùng; việc thêm chỉ mục cũng phát sinh chi phí ghi và dung lượng.

Tiền cần dùng kiểu số thập phân phù hợp thay vì số thực nhị phân. Số lượng, tham chiếu và trạng thái cần kiểm tra trong chương trình và ràng buộc dữ liệu khi thích hợp. Kiểm tra ở frontend chỉ hỗ trợ phát hiện lỗi sớm.

### 2.3.2 Giao dịch và tính nhất quán

Giao dịch là tập hợp thao tác dữ liệu được xử lý như một đơn vị công việc. ACID gồm tính nguyên tử, nhất quán, cô lập và bền vững. Tính nguyên tử yêu cầu các bước liên quan cùng xác nhận hoặc hủy khi có lỗi; tính cô lập hỗ trợ kiểm soát tác động giữa các thao tác đồng thời [4].

Khi tạo đơn, hệ thống phải ghi đơn hàng, các dòng sản phẩm, phân bổ kho, thông tin thanh toán và lượt dùng ưu đãi. Nếu lưu được đơn nhưng không giữ được hàng, dữ liệu không phản ánh đúng cam kết bán. Spring cung cấp quản lý giao dịch khai báo để nhóm các thao tác theo ranh giới nghiệp vụ [5].

Khóa dữ liệu điều phối truy cập đồng thời. Hai khách cùng mua số hàng cuối không thể chỉ đọc tồn độc lập rồi cùng ghi giảm. GreenFarm kiểm tra và điều phối tồn trong giao dịch để hạn chế bán vượt số lượng khả dụng. Tính đúng đắn còn phụ thuộc cách đặt ranh giới giao dịch và truy cập dữ liệu.

Giao dịch MySQL không tự bao phủ tác động ở dịch vụ ngoài. Nếu đơn đã lưu nhưng email thất bại, gửi lại email cần theo dõi riêng. Yêu cầu tới cổng thanh toán cần mã tham chiếu và xử lý kết quả phù hợp; rollback database không tự hủy mọi tác động đã xảy ra bên ngoài.

### 2.3.3 Bản chụp dữ liệu và xử lý lặp

Bản chụp dữ liệu, thường gọi là snapshot, lưu giá trị tại thời điểm phát sinh nghiệp vụ. Với đơn hàng, tên sản phẩm, đơn vị, đơn giá và thông tin người nhận cần được bảo toàn để thay đổi catalog hoặc địa chỉ sau này không làm đổi nội dung đơn cũ.

Snapshot không thay thế hoàn toàn liên kết gốc. Dòng đơn vẫn có thể tham chiếu sản phẩm để thống kê, trong khi trường chụp giúp xem lại giao dịch đúng ngữ cảnh. Việc chọn trường cần gắn nhu cầu nghiệp vụ thay vì sao chép mọi thuộc tính.

Tính idempotent ở mức nghiệp vụ có nghĩa xử lý lặp một yêu cầu hoặc sự kiện không tạo thêm tác động ngoài lần hợp lệ đầu tiên. GreenFarm dùng khóa checkout cho khách vãng lai và kiểm soát callback để giảm nguy cơ tạo đơn, trừ kho hoặc ghi nhận thanh toán lặp. Một sự kiện đến hai lần không nhất thiết là hai giao dịch mua khác nhau.

### 2.3.4 Xác thực bằng JWT và quản lý phiên

Xác thực xác định danh tính người gửi yêu cầu. JWT biểu diễn tập claim theo RFC 7519; dạng có chữ ký thường gồm header, payload và signature. Chữ ký hỗ trợ phát hiện sửa đổi; payload được mã hóa biểu diễn không có nghĩa đã được mã hóa bảo mật [6].

Người dùng GreenFarm đăng nhập bằng thông tin tài khoản. Backend kiểm tra mật khẩu đã băm, trạng thái tài khoản và cổng đăng nhập tương ứng, sau đó cấp access token và refresh token. Yêu cầu cần xác thực gửi access token trong header Authorization dạng Bearer. Backend xác minh token và người dùng trước khi cho phép truy cập.

Access token có thời hạn; refresh token duy trì phiên theo quy tắc hệ thống. GreenFarm có xoay refresh token và thu hồi phiên khi đăng xuất hoặc đổi mật khẩu. Quản lý phiên vì vậy không chỉ là kiểm tra chữ ký JWT mà còn liên quan trạng thái tài khoản, thời hạn và chính sách thu hồi.

[FIGURE:auth]

Hình 2.3 Luồng xác thực và kiểm tra quyền ở mức khái quát

Trong Hình 2.3, kiểm tra quyền diễn ra sau khi xác minh danh tính. Token hợp lệ không có nghĩa được truy cập mọi dữ liệu; hệ thống còn kiểm tra vai trò, quyền và phạm vi sở hữu tài nguyên.

### 2.3.5 Phân quyền theo vai trò và quyền sở hữu

Phân quyền theo vai trò, hay RBAC, gắn nhóm trách nhiệm với tập quyền. GreenFarm có customer, staff, delivery_staff và admin. Khách chưa đăng nhập dùng các chức năng công khai trong phạm vi phiên khách, không được coi là có quyền của tài khoản khách hàng.

Bảng 2.3 Vai trò và phạm vi trách nhiệm khái quát

| Vai trò | Trách nhiệm | Giới hạn cần kiểm tra |
| --- | --- | --- |
| Khách vãng lai | Xem hàng, dùng giỏ khách, đặt và tra cứu đơn khách | Phiên khách và thông tin xác minh |
| customer | Mua hàng, quản lý hồ sơ, địa chỉ, đơn, đánh giá, điểm | Dữ liệu thuộc tài khoản |
| staff | Xử lý đơn, liên hệ, thao tác kho được cấp | Quyền và điều kiện nghiệp vụ |
| delivery_staff | Nhận và cập nhật giao nội bộ | Đơn được phân công |
| admin | Quản lý người dùng, hàng hóa, kho, vận hành | Quyền từng API và lịch sử thay đổi |

Quyền sở hữu bổ sung cho vai trò. Hai khách hàng cùng vai trò không được xem đơn của nhau. Người giao chỉ được nhận và cập nhật đơn phân công cho mình. Phân quyền phải kết hợp danh tính, nhóm quyền và quan hệ giữa tài khoản với dữ liệu.

Bảng 2.3 mô tả trách nhiệm tổng quát; ma trận API và quyền sẽ được xác định ở phần thiết kế. Tên cổng đăng nhập hoặc đường dẫn giao diện không đủ để kết luận thao tác đã được bảo vệ.

### 2.3.6 Bảo vệ dữ liệu và thông tin nhạy cảm

Dữ liệu nhạy cảm gồm mật khẩu, token, thông tin nhận hàng, khóa dịch vụ và giao dịch. Mật khẩu cần lưu dạng băm một chiều thích hợp để đối chiếu, thay vì lưu nguyên văn hoặc thiết kế chức năng giải mã. Spring Security cung cấp PasswordEncoder; GreenFarm cấu hình BCrypt cho mục đích này [7].

Khóa JWT, thông tin MySQL, bí mật thanh toán và khóa Gemini được tách khỏi mã nguồn bằng biến môi trường. Cấu hình đưa vào JavaScript phía trình duyệt phải được xem là công khai; chỉ các giá trị như địa chỉ API mới phù hợp để công bố ở frontend.

Bảo vệ dữ liệu còn gồm giới hạn đăng nhập sai, kiểm tra đầu vào, quyền sở hữu, kiểm soát tệp tải và không trả dấu vết lỗi nội bộ. Khi triển khai Internet cần HTTPS. Lưu token trong trình duyệt cũng đặt ra yêu cầu hạn chế XSS; có token không tự làm ứng dụng an toàn.

### 2.3.7 Quản lý phiên bản cấu trúc dữ liệu

Schema thay đổi khi phát triển, chẳng hạn thêm nguồn gốc sản phẩm hoặc quản lý lô. Cập nhật thủ công khác nhau giữa các môi trường có thể khiến chương trình chạy với cấu trúc không tương thích.

GreenFarm dùng migration SQL có phiên bản do Flyway quản lý. Database mới khởi tạo từ schema nền rồi áp dụng thay đổi theo thứ tự. Database tồn tại có thể baseline phù hợp để không tạo lại bảng cũ. Migration đã chạy cần giữ ổn định; thay đổi tiếp theo được viết thành phiên bản mới.

Triển khai lại không tự mất dữ liệu nếu tiếp tục dùng cùng database bền vững và không thực hiện thao tác xóa. Migration cũng không thay thế sao lưu; cần bản sao và quy trình phục hồi trước thay đổi có tác động lớn.

## 2.4 Cơ sở lý thuyết về trí tuệ nhân tạo trong tư vấn mua hàng

### 2.4.1 Trí tuệ nhân tạo tạo sinh và mô hình ngôn ngữ

Trí tuệ nhân tạo tạo sinh tạo nội dung từ đầu vào. Với trợ lý văn bản, mô hình ngôn ngữ tiếp nhận câu hỏi cùng ngữ cảnh và sinh phản hồi theo yêu cầu. Gemini API cung cấp khả năng sinh văn bản, được GreenFarm sử dụng cho hội thoại tư vấn [8].

Diễn đạt tự nhiên phù hợp với hỏi loại rau, giá hoặc chính sách mua hàng. Tuy nhiên, đầu ra mô hình không tự có quyền xác nhận giao dịch hay thay đổi dữ liệu. AI trong GreenFarm hỗ trợ tư vấn; đặt hàng, tính tiền và kiểm tra quyền vẫn do mã chương trình xử lý.

Tích hợp API mô hình có sẵn khác với tự xây dựng và huấn luyện mô hình. Phạm vi đề tài là lớp ứng dụng điều phối câu hỏi, lựa chọn dữ liệu công khai và kiểm soát phản hồi AI.

### 2.4.2 Prompt và ngữ cảnh nghiệp vụ

Prompt là hướng dẫn gửi tới mô hình để xác định nhiệm vụ và cách trả lời. Với trợ lý mua sắm, prompt cần xác định vai trò, phạm vi tư vấn, dữ liệu được dùng và cách phản hồi khi thiếu thông tin.

Ngữ cảnh phải được tạo từ dữ liệu đáng tin cậy. Nếu chỉ gửi câu hỏi mà không có catalog, mô hình có thể trả thông tin không khớp hàng hóa. GreenFarm tạo ngữ cảnh ở backend từ sản phẩm công khai và chính sách; frontend không tự khai báo giá hay tồn kho làm căn cứ tư vấn.

Ngữ cảnh có thời điểm hiệu lực. Trả lời còn hàng khi hội thoại không giữ chỗ cho khách. Khi checkout, máy chủ vẫn kiểm tra lại giá và số lượng, tránh hiểu câu trả lời tư vấn là cam kết giao dịch đã xác nhận.

### 2.4.3 Tư vấn dựa trên dữ liệu công khai

Phạm vi dữ liệu đưa vào mô hình được giới hạn ở thông tin được phép hiển thị công khai. Với GreenFarm, đó là sản phẩm đang hiển thị, giá, tồn kho và chính sách. Tài khoản, đơn riêng, doanh thu, nhân sự và cấu hình nội bộ không thuộc phạm vi tư vấn.

Lấy thông tin công khai bổ sung câu hỏi là cung cấp ngữ cảnh từ dữ liệu ứng dụng. Điều này không đủ để kết luận dự án có hệ thống RAG đầy đủ với kho vector, embedding hay mô hình gợi ý được huấn luyện. Các thành phần đó không được xem là kết quả hiện có.

Lịch sử hội thoại giúp hiểu câu hỏi tiếp nối nhưng cần giới hạn và tách theo chủ thể. GreenFarm phân biệt lịch sử tài khoản với phiên khách. Backend lưu giá trị băm token phiên chat khách để hạn chế lưu token thô trong database.

[FIGURE:ai]

Hình 2.4 Luồng cung cấp ngữ cảnh và phản hồi của trợ lý GreenFarm

Hình 2.4 thể hiện backend là trung gian. AI không kết nối trực tiếp database và không có quyền cập nhật đơn, thanh toán hoặc kho. Khi không gọi được mô hình, backend dùng trả lời dự phòng từ thông tin công khai.

### 2.4.4 Bảo vệ dữ liệu khi tích hợp AI

Câu hỏi có thể chứa email, số điện thoại hoặc thông tin thanh toán. Trước khi lưu và gửi AI, hệ thống cần giảm dữ liệu không cần thiết. GreenFarm che một số mẫu thông tin cá nhân trong hội thoại và lọc lại lịch sử khi tạo ngữ cảnh.

Yêu cầu vượt phạm vi như hỏi đơn cá nhân hoặc tiết lộ cấu hình phải bị từ chối. Chỉ dẫn trong câu hỏi không tự trở thành quyền đổi quy tắc hệ thống. Lọc từ khóa và hướng dẫn mô hình là các lớp giảm rủi ro, không phải bằng chứng mọi cách khai thác prompt injection đã bị loại bỏ.

Khóa AI chỉ cấu hình tại backend. Khi hiển thị, ứng dụng kiểm soát nội dung và liên kết sản phẩm; văn bản mô hình sinh không được mặc nhiên trở thành mã thực thi trong trình duyệt. Thẻ sản phẩm cần được đối chiếu catalog công khai backend đã chọn.

### 2.4.5 Hạn chế và cơ chế dự phòng

Chất lượng phản hồi có thể thay đổi theo cách hỏi, ngữ cảnh và phiên bản mô hình. Mô hình có thể suy diễn sai hoặc tạo thông tin thiếu căn cứ. API ngoài có thể lỗi, chậm hoặc không sẵn sàng, ảnh hưởng trải nghiệm tư vấn.

GreenFarm trả lời dự phòng bằng catalog và chính sách khi chưa có khóa Gemini hoặc dịch vụ lỗi. Cách này duy trì hỗ trợ cơ bản, nhưng không bảo đảm linh hoạt tương đương mô hình ngôn ngữ.

AI là chức năng bổ trợ; không dùng để quyết định tiền thanh toán, cấp quyền, xác nhận giao dịch hay thay thế đánh giá chất lượng thực phẩm của đơn vị chuyên môn. Chưa thể khẳng định độ chính xác định lượng nếu chưa đánh giá bằng bộ câu hỏi và tiêu chí cụ thể.

## 2.5 Các công nghệ sử dụng

### 2.5.1 Tổng quan công nghệ

GreenFarm sử dụng frontend và backend tổ chức riêng trong cùng kho mã nguồn. Bảng 2.4 tổng hợp thành phần theo cấu hình dự án. Phiên bản phản ánh khai báo hiện tại, không được hiểu là phiên bản mới nhất của nhà cung cấp. Thư viện khai báo theo dải có thể được khóa cụ thể trong tệp lock.

Bảng 2.4 Các công nghệ chính sử dụng trong GreenFarm

| Nhóm | Công nghệ và phiên bản khai báo | Vai trò |
| --- | --- | --- |
| Giao diện | Next.js 16.3.3, React 19.2.4 | Trang, điều hướng, component |
| Ngôn ngữ frontend | TypeScript 5.x | Kiểm tra kiểu dữ liệu |
| Định dạng giao diện | Tailwind CSS 4.x | Giao diện responsive |
| Dữ liệu frontend | TanStack Query 5.x, Axios 1.x | Truy cập và đồng bộ API |
| Trạng thái, biểu mẫu | Zustand 5.x, React Hook Form 7.x, Zod 4.x | Trạng thái và kiểm tra đầu vào |
| Backend | Java 21, Spring Boot 4.1.0 | API và nghiệp vụ |
| Bảo mật | Spring Security, JJWT 0.12.6, BCrypt | Xác thực, kiểm tra quyền |
| Dữ liệu | MySQL 8.0.44 trong Compose, Spring Data JPA | Lưu trữ và truy cập dữ liệu |
| Migration | Flyway, SQL có phiên bản | Quản lý thay đổi schema |
| Hình ảnh | Cloudinary SDK 2.4.0 hoặc thư mục upload | Ảnh sản phẩm, danh mục, avatar |
| Dịch vụ nghiệp vụ | VNPAY, GHN, SMTP | Thanh toán, giao nhận, email |
| Trợ lý tư vấn | Gemini API | Phản hồi theo dữ liệu công khai |
| Kiểm thử, hợp đồng | Spring Boot Test, Testcontainers 1.20.6, OpenAPI | Kiểm thử và mô tả API |
| Môi trường local | Docker, Docker Compose, Node.js | Chạy backend, MySQL và frontend local |

### 2.5.2 Công nghệ frontend

Next.js cung cấp cấu trúc ứng dụng trên nền React, gồm trang, layout và điều hướng. GreenFarm dùng App Router trong thư mục frontend/src/app. Khu vực khách hàng, quản trị, nhân viên và giao hàng được tổ chức theo tuyến đường phục vụ từng nhóm người dùng.

React chia giao diện thành thành phần tái sử dụng. Thẻ sản phẩm, bộ lọc, bảng và biểu mẫu được kết hợp thành catalog và màn hình vận hành. TypeScript kiểm tra kiểu khi làm việc với API, nhưng dữ liệu qua mạng vẫn cần xác minh ở ranh giới thích hợp.

Tailwind CSS xây dựng giao diện và điều chỉnh bố cục theo màn hình. Axios gọi HTTP; TanStack Query quản lý dữ liệu máy chủ; Zustand phục vụ trạng thái dùng chung. React Hook Form và Zod hỗ trợ biểu mẫu và đầu vào. Kiểm tra phía client cải thiện trải nghiệm, không thay thế xác thực dữ liệu backend.

### 2.5.3 Công nghệ backend

Java 21 là ngôn ngữ và nền tảng chạy backend. Spring Boot 4.1.0 cấu hình ứng dụng và kết hợp các thành phần Spring để xây dựng API. Các starter phục vụ Web MVC, kiểm tra dữ liệu, JPA, bảo mật, email và migration.

Controller, Service và Repository tách trách nhiệm. Spring Data JPA truy cập dữ liệu qua repository và ánh xạ entity; tạo đơn, phân bổ kho và cập nhật quyền lợi được điều phối ở service. Spring Security kết hợp bộ lọc JWT với kiểm tra quyền tại API và phương thức.

OpenAPI qua springdoc mô tả hợp đồng API và cung cấp Swagger khi được phép truy cập, hỗ trợ đối chiếu tham số và phản hồi giữa hai phía. Lombok giảm mã lặp trong Java, không thay thế việc xác định đúng mô hình nghiệp vụ.

### 2.5.4 Công nghệ dữ liệu và lưu trữ

MySQL lưu người dùng, catalog, giỏ, đơn, thanh toán, lô hàng và dữ liệu liên quan. Flyway quản lý migration để cập nhật schema. JPA hỗ trợ truy cập, còn ràng buộc SQL bảo vệ toàn vẹn ở database.

Ảnh là tệp có đặc điểm khác dữ liệu quan hệ. GreenFarm hỗ trợ Cloudinary khi cấu hình đủ; nếu dùng local, ứng dụng ghi ảnh vào thư mục upload. Database lưu đường dẫn và thông tin liên quan. Trong container, thư mục ảnh cần nơi lưu bền vững nếu tiếp tục dùng local.

Docker Compose khai báo volume cho MySQL và upload. Tách dữ liệu khỏi vòng đời container giúp tạo lại container không tự loại bỏ dữ liệu trong volume. Volume không thay thế sao lưu độc lập; xóa volume hoặc cấu hình sai vẫn có thể gây mất hoặc không tìm thấy dữ liệu.

### 2.5.5 Công nghệ tích hợp dịch vụ ngoài

VNPAY phục vụ thanh toán trực tuyến. Backend tạo dữ liệu và xác minh kết quả bằng chữ ký theo hợp đồng tích hợp. Sandbox dùng để kiểm thử; thu tiền thật cần cấu hình và điều kiện sử dụng riêng của nhà cung cấp.

GHN phục vụ giao nhận bên ngoài theo cấu hình, bên cạnh giao nội bộ. Hai hình thức có cách vận hành và nguồn cập nhật trạng thái khác nhau; phân công người giao nội bộ không đồng nghĩa tạo vận đơn GHN.

SMTP gửi email hóa đơn, có theo dõi và retry để lỗi gửi không làm mất đơn đã xác nhận. Gemini API phục vụ hội thoại, Cloudinary lưu ảnh. Các kết nối dùng thông tin xác thực tại backend và cần xử lý khi dịch vụ ngoài không khả dụng.

### 2.5.6 Công nghệ kiểm thử và công cụ phát triển

Backend khai báo Spring Boot Test và Testcontainers, hỗ trợ kiểm thử logic và trường hợp cần MySQL tương ứng. Database thật trong container có ý nghĩa khi kiểm tra truy vấn, ràng buộc và giao dịch mà giả lập không phản ánh đầy đủ.

Frontend có lệnh ESLint và build Next.js để phát hiện một số lỗi mã, kiểu và cấu hình. Các lệnh này không thay thế kiểm thử chức năng đầu cuối. Đặt hàng, phân quyền và callback cần được kiểm tra với dữ liệu và môi trường thích hợp.

Git quản lý mã nguồn, npm quản lý phụ thuộc frontend, Maven quản lý build backend. Chương kiểm thử sẽ trình bày ca, điều kiện và kết quả thực tế; không suy ra tỷ lệ thành công chỉ từ khai báo thư viện kiểm thử.

### 2.5.7 Công nghệ đóng gói và môi trường local

Docker đóng gói ứng dụng cùng môi trường chạy trong image và thực thi bằng container [9]. Dockerfile GreenFarm có nhiều giai đoạn: Maven xây dựng ứng dụng, JRE chạy JAR. Compose tổ chức backend, MySQL và volume cho local.

Trong môi trường local, frontend chạy bằng Node.js tại cổng mặc định 3000; backend cung cấp API tại cổng 8080. MySQL trong mạng Docker dùng cổng 3306 và được ánh xạ ra cổng 3307 của máy host theo cấu hình mặc định. Địa chỉ và cổng có thể được thay đổi qua cấu hình dự án.

Frontend kết nối backend qua biến cấu hình địa chỉ API; cấu hình mặc định dùng http://localhost:8080/api. Dữ liệu nghiệp vụ được lấy từ backend và MySQL local. Các chức năng phụ thuộc dịch vụ ngoài như VNPAY, GHN, SMTP, Cloudinary hoặc Gemini chỉ hoạt động theo cấu hình và thông tin xác thực thực tế của môi trường này.

Môi trường local là căn cứ để mô tả chức năng và kiểm thử trong báo cáo. Docker Compose giúp tái lập backend và database; frontend chạy bằng lệnh npm tương ứng trong thư mục frontend. Sự có mặt của mã tích hợp dịch vụ ngoài được phân biệt với kết quả kiểm thử thành công của dịch vụ đó.

Những cơ sở trong chương xác lập cách xử lý giao dịch, tổ chức dữ liệu, phân quyền và tích hợp dịch vụ. Chương tiếp theo cụ thể hóa thành yêu cầu, tác nhân, use case, thiết kế dữ liệu, API và giao diện.

## Tài liệu tham khảo dùng cho chương 2

[1] Roy Thomas Fielding. Architectural Styles and the Design of Network-based Software Architectures. Chương 5 Representational State Transfer. https://ics.uci.edu/~fielding/pubs/dissertation/rest_arch_style.htm

[2] Next.js. Server and Client Components. https://nextjs.org/docs/app/getting-started/server-and-client-components

[3] TanStack. TanStack Query Overview. https://tanstack.com/query/latest/docs/framework/react/overview

[4] Oracle. MySQL 8.0 Reference Manual InnoDB Transaction Model. https://dev.mysql.com/doc/refman/8.0/en/innodb-transaction-model.html

[5] Spring. Declarative Transaction Management. https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative.html

[6] IETF. RFC 7519 JSON Web Token. https://www.rfc-editor.org/rfc/rfc7519

[7] Spring Security. Password Storage. https://docs.spring.io/spring-security/reference/features/authentication/password-storage.html

[8] Google AI for Developers. Text generation Gemini API. https://ai.google.dev/gemini-api/docs/text-generation

[9] Docker. What is Docker. https://docs.docker.com/get-started/docker-overview/
