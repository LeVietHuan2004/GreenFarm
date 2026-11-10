from pathlib import Path
s=Path('output/report/GreenFarm_Chuong_2.md').read_text(encoding='utf-8')
p=s.split('\n\n')
assert len(p)>160 and p[145].startswith('### 2.5.7')
remove={10,25,33,41,44,55,59,65,73,81,87,93,95,104,107,111,127,136,149}
replace={
1:'Chương này trình bày cơ sở lý thuyết về thương mại điện tử nông sản, kiến trúc phần mềm, dữ liệu, bảo mật và trí tuệ nhân tạo, cùng các công nghệ sử dụng trong GreenFarm. Nội dung được đối chiếu với mã nguồn local để làm cơ sở phân tích và thiết kế hệ thống.',
5:'Trong GreenFarm, khách tìm sản phẩm, chọn số lượng, đặt hàng và theo dõi đơn. Cửa hàng quản lý catalog, nguồn cung, tồn kho, ưu đãi và yêu cầu hỗ trợ. Dữ liệu giữa các khâu cần thống nhất để tránh bán hàng hết tồn hoặc ghi nhận sai thanh toán.',
6:'GreenFarm là mô hình cửa hàng bán trực tiếp cho người tiêu dùng. Nhà cung cấp phục vụ quản lý nhập hàng; hệ thống không tổ chức gian hàng và chia doanh thu cho nhiều nhà bán.',
9:'Thông tin sản phẩm gồm tên, đơn vị, giá, ảnh, nguồn gốc, chứng nhận và cách bảo quản khi có dữ liệu. Các trường này hỗ trợ người mua lựa chọn; phần mềm không thay thế việc kiểm định chất lượng và xác minh chứng nhận.',
15:'Checkout kiểm tra lại giá, số lượng, địa chỉ, giao nhận và ưu đãi tại máy chủ. Dù đã có bản xem trước trên giao diện, yêu cầu tạo đơn vẫn phải được xác minh để tránh sử dụng dữ liệu cũ.',
16:'GreenFarm hỗ trợ khách có tài khoản và khách vãng lai. Phiên khách quản lý giỏ và hỗ trợ gộp giỏ khi đăng nhập. Tra cứu đơn khách cần thông tin xác minh riêng, không chỉ dựa vào mã đơn.',
17:'Tổng thanh toán bằng tiền hàng cộng phí vận chuyển, trừ giảm giá hợp lệ và điểm sử dụng. Backend kiểm tra giới hạn và tính lại toàn bộ số tiền, không lấy tổng do trình duyệt gửi làm kết quả quyết định.',
23:'FEFO (First Expired First Out) ưu tiên xuất lô hết hạn sớm nhất để hạn chế hao hụt hàng dễ hỏng. Tiêu chí này khác FIFO, vốn ưu tiên thứ tự nhập hàng; lô nhập trước không nhất thiết hết hạn trước.',
24:'GreenFarm theo dõi lượng vật lý còn lại và lượng đã giữ cho đơn. Với lô còn hạn, lượng khả dụng bằng lượng còn lại trừ lượng đã giữ. Khi xử lý đơn, hàng giữ chuyển thành hàng xuất; khi hủy, hệ thống giải phóng hoặc hoàn hàng theo trạng thái thực tế.',
29:'COD thu tiền khi giao hàng. Thanh toán trực tuyến chuyển người mua tới cổng và nhận callback; backend kiểm tra chữ ký, mã giao dịch và số tiền trước khi ghi nhận. GreenFarm tích hợp VNPAY với cấu hình mặc định sandbox để kiểm thử.',
32:'Điểm thưởng có thể đổi thành giảm giá nên cần lịch sử cộng, trừ, hoàn và điều kiện áp dụng. GreenFarm kiểm soát xử lý lặp để một sự kiện xác nhận đơn hoặc đánh giá không cấp điểm nhiều lần.',
45:'GreenFarm kết hợp các cơ chế kết xuất của Next.js với component tương tác, thay vì chỉ có SPA thuần phía trình duyệt. Các thao tác liên quan tiền, quyền và kho luôn được kiểm tra tại backend.',
51:'Kiểm tra JSON và kiểm tra nghiệp vụ là hai bước riêng. Yêu cầu đúng định dạng vẫn bị từ chối nếu sai quyền sở hữu hoặc thiếu hàng. Tạo đơn bằng POST cần cơ chế chống lặp theo nghiệp vụ.',
54:'Khi tạo đơn, Controller nhận lựa chọn; Service kiểm tra địa chỉ, hàng, kho và ưu đãi rồi phối hợp Repository lưu dữ liệu. Quy tắc chung được tập trung ở service để hạn chế sai lệch giữa các luồng mua hàng.',
70:'Giao dịch database không tự bao phủ dịch vụ ngoài. Email thất bại cần gửi lại riêng; rollback MySQL không tự hủy tác động đã phát sinh tại cổng thanh toán.',
74:'Idempotency bảo đảm xử lý lặp không tạo tác động nghiệp vụ bổ sung. GreenFarm dùng khóa checkout cho khách vãng lai và kiểm soát callback để hạn chế tạo đơn, trừ kho hoặc ghi nhận thanh toán hai lần.',
78:'Backend kiểm tra mật khẩu, trạng thái tài khoản và cổng đăng nhập, rồi cấp access token cùng refresh token. Các yêu cầu được bảo vệ gửi access token trong header Authorization dạng Bearer để backend xác minh.',
79:'Access token có thời hạn; refresh token phục vụ làm mới phiên. GreenFarm xoay refresh token và thu hồi phiên khi đăng xuất hoặc đổi mật khẩu, kết hợp kiểm tra trạng thái tài khoản.',
100:'Đề tài tích hợp mô hình có sẵn qua API, tập trung vào điều phối câu hỏi, lựa chọn ngữ cảnh và kiểm soát phản hồi; không tự huấn luyện mô hình ngôn ngữ.',
114:'Yêu cầu tiết lộ cấu hình hoặc dữ liệu cá nhân phải bị từ chối. Lọc nội dung và hướng dẫn mô hình giúp giảm rủi ro prompt injection nhưng không bảo đảm loại bỏ mọi cách khai thác.',
115:'Khóa AI chỉ đặt ở backend. Phản hồi được kiểm soát khi hiển thị; thẻ sản phẩm được đối chiếu catalog, không mặc nhiên tin mọi nội dung mô hình tạo ra.',
119:'AI chỉ hỗ trợ tư vấn, không quyết định tiền, quyền hay trạng thái giao dịch. Độ chính xác cần được đánh giá bằng bộ câu hỏi và tiêu chí cụ thể.',
122:'Bảng 2.4 tổng hợp công nghệ theo cấu hình GreenFarm local. Phiên bản là khai báo trong dự án; những phụ thuộc theo dải được quản lý thêm bằng tệp lock.',
144:'Git quản lý mã nguồn, npm quản lý frontend và Maven build backend. Kết quả kiểm thử cụ thể được trình bày ở chương 5, không suy ra từ việc khai báo thư viện.',
148:'Frontend mặc định gọi http://localhost:8080/api để lấy dữ liệu backend và MySQL local. VNPAY, GHN, SMTP, Cloudinary và Gemini hoạt động theo cấu hình dịch vụ thực tế.',
}
for i,v in replace.items():p[i]=v
s='\n\n'.join(v for i,v in enumerate(p) if i not in remove)
for i,k in enumerate(['commerce','architecture','auth','ai'],1):s=s.replace(f'![Sơ đồ GreenFarm {i}](figures/hinh-2-{i}.png)','[FIGURE:'+k+']')
Path('tmp/report/authoring.md').write_text(s,encoding='utf-8')
print('Words before:',len(Path('output/report/GreenFarm_Chuong_2.md').read_text(encoding='utf-8').split()),'after:',len(s.split()))
