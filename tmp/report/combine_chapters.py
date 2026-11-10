from pathlib import Path
import re
src=Path(r'C:\Users\LEGION\.codex\attachments\b2ea2ec4-01e0-4d27-9907-0301b7a9c759\Pasted text.txt')
s=src.read_text(encoding='utf-8-sig')
changes={
'Từ những nhu cầu đó, đề tài GreenFarm được thực hiện nhằm xây dựng một hệ thống web hỗ trợ kinh doanh nông sản trực tuyến. Hệ thống cung cấp các chức năng xem và tìm kiếm sản phẩm, quản lý giỏ hàng và danh sách yêu thích, lưu địa chỉ giao hàng, tạo đơn hàng và tra cứu thông tin đơn. Đồng thời, khu vực quản trị hỗ trợ quản lý người dùng, danh mục và sản phẩm.':
'Từ những nhu cầu đó, đề tài GreenFarm được thực hiện nhằm xây dựng một hệ thống web hỗ trợ kinh doanh nông sản trực tuyến. Hệ thống kết hợp tìm kiếm sản phẩm, giỏ hàng, đặt hàng, thanh toán và theo dõi đơn; đồng thời hỗ trợ cửa hàng quản lý tài khoản, hàng hóa, kho theo lô và quá trình giao nhận. Các chức năng đánh giá, tích điểm, liên hệ và trợ lý tư vấn bổ sung cho trải nghiệm mua hàng.',
'GreenFarm hướng tới giải quyết một phần những nhu cầu trên bằng cách xây dựng nền tảng web có các khu vực dành cho khách hàng và quản trị viên. Hệ thống tổ chức các chức năng thương mại chính trên cùng một nền tảng, đồng thời kiểm tra dữ liệu quan trọng ở phía máy chủ trong các thao tác như cập nhật giỏ hàng và tạo đơn.':
'GreenFarm hướng tới giải quyết những nhu cầu trên bằng một nền tảng web có khu vực dành cho khách hàng, quản trị viên, nhân viên và nhân viên giao hàng. Hệ thống tập trung các nghiệp vụ thương mại và kiểm tra dữ liệu quan trọng ở phía máy chủ, đặc biệt khi đặt hàng, cập nhật kho và ghi nhận thanh toán.',
'-\tKhách hàng: xem và tìm kiếm sản phẩm, quản lý giỏ hàng và danh sách yêu thích, quản lý địa chỉ giao hàng, tạo đơn và xem lịch sử đơn hàng.':
'- Khách hàng: tìm kiếm sản phẩm, quản lý giỏ và yêu thích, đặt hàng, theo dõi đơn, đánh giá và sử dụng điểm thưởng. Khách chưa đăng nhập có thể mua qua phiên khách và tra cứu đơn bằng thông tin xác minh.',
'-\tQuản trị viên: quản lý người dùng, danh mục và sản phẩm thông qua khu vực quản trị.':
'- Quản trị viên: quản lý tài khoản, danh mục, sản phẩm, nhà cung cấp, nhập kho, ưu đãi, đơn hàng và các hoạt động vận hành.',
'-\tNhân viên và nhân viên giao hàng: hệ thống có các vai trò tài khoản tương ứng; các nghiệp vụ quản lý đơn giao và giao hàng chưa thuộc phạm vi chức năng đã triển khai trong phiên bản hiện tại.':
'- Nhân viên: xử lý đơn, liên hệ hỗ trợ và thao tác kho theo quyền được cấp.\n- Nhân viên giao hàng: nhận và cập nhật trạng thái các đơn giao nội bộ được phân công.',
'Mục tiêu tổng quát của đề tài là xây dựng hệ thống web GreenFarm hỗ trợ hoạt động kinh doanh nông sản trực tuyến, từ việc giới thiệu sản phẩm đến quản lý giỏ hàng, địa chỉ và đơn hàng.':
'Mục tiêu tổng quát là xây dựng GreenFarm hỗ trợ quy trình kinh doanh nông sản trực tuyến, từ giới thiệu sản phẩm, đặt hàng và thanh toán đến vận hành kho, giao nhận và chăm sóc sau mua.',
'Đối tượng nghiên cứu là quy trình kinh doanh nông sản trực tuyến, bao gồm việc quản lý sản phẩm, người dùng, giỏ hàng, địa chỉ giao hàng và đơn hàng trên nền tảng web.':
'Đối tượng nghiên cứu là quy trình kinh doanh nông sản trực tuyến và cách tổ chức phần mềm để quản lý sản phẩm, khách hàng, đặt hàng, thanh toán, tồn kho và giao nhận nhất quán.',
'Đối tượng sử dụng hệ thống gồm khách hàng, quản trị viên và các vai trò nhân viên đã được định nghĩa trong hệ thống xác thực.':
'Đối tượng sử dụng gồm khách vãng lai, khách hàng có tài khoản, nhân viên, nhân viên giao hàng và quản trị viên. GreenFarm được tổ chức theo mô hình một cửa hàng bán trực tiếp cho người tiêu dùng.',
'Đề tài tập trung vào các chức năng đã mô tả và triển khai trong phiên bản hiện tại. Thanh toán trực tuyến và quy trình quản lý, điều phối giao hàng chưa được triển khai; hệ thống chưa hướng tới thay thế các nền tảng kế toán, vận chuyển hoặc quản lý chuỗi cung ứng chuyên dụng.':
'Phạm vi báo cáo dựa trên phiên bản GreenFarm local hiện tại. Các dịch vụ VNPAY, GHN, SMTP, Cloudinary và Gemini được sử dụng theo cấu hình thực tế; có mã tích hợp không đồng nghĩa đã được xác nhận vận hành thương mại hoặc kiểm thử đầy đủ với nhà cung cấp. VNPAY mặc định dùng sandbox. Hệ thống không thay thế phần mềm kế toán, kiểm định chất lượng thực phẩm hoặc quản lý chuỗi cung ứng chuyên dụng.',
'-\tPhát triển phần mềm: triển khai theo các nhóm chức năng như tài khoản, catalog, giỏ hàng, yêu thích, địa chỉ và đơn hàng.':
'- Phát triển phần mềm: triển khai theo nhóm tài khoản, catalog, đặt hàng, thanh toán, kho, vận hành đơn và chăm sóc khách hàng.',
'GreenFarm cung cấp một nền tảng tập trung để khách hàng xem và đặt mua nông sản, đồng thời hỗ trợ người quản trị quản lý những dữ liệu cốt lõi của hoạt động bán hàng.':
'GreenFarm cung cấp nền tảng tập trung cho mua bán nông sản và vận hành cửa hàng. Các đóng góp được xem xét trên chức năng đã xây dựng; hiệu quả sử dụng và kết quả kiểm thử cần được đánh giá bằng các trường hợp cụ thể.',
'Ngoài Chương 1 trình bày tổng quan đề tài, báo cáo có thể được tổ chức thành các chương sau:':
'Báo cáo được tổ chức thành sáu chương. Chương 1 trình bày bối cảnh, bài toán, mục tiêu và phạm vi; các chương tiếp theo gồm:',
}
for old,new in changes.items():
 assert old in s,old[:70]
 s=s.replace(old,new)
a=s.index('Các chức năng chính đã được triển khai gồm:');b=s.index('Khi tạo đơn, backend',a)
s=s[:a]+'''Các nhóm chức năng được xây dựng trong phiên bản local gồm:
- Tài khoản: đăng ký, đăng nhập, hồ sơ, đổi và khôi phục mật khẩu, làm mới phiên; JWT, phân quyền và giới hạn đăng nhập sai.
- Catalog: danh mục, sản phẩm, ảnh, tìm kiếm, lọc và phân trang; quản trị thông tin và trạng thái bán.
- Mua hàng: giỏ và yêu thích theo tài khoản, giỏ khách vãng lai, địa chỉ, xem trước chi phí, tạo và tra cứu đơn.
- Thanh toán và ưu đãi: COD, tích hợp VNPAY, coupon, tích và đổi điểm, hóa đơn cùng yêu cầu hoàn tiền theo điều kiện.
- Kho: nhà cung cấp, nhập lô, hạn sử dụng, giữ hàng, xuất theo FEFO và điều chỉnh kho.
- Vận hành đơn: xử lý trạng thái, phân công giao nội bộ, nhân viên nhận giao, tích hợp GHN và lưu lịch sử.
- Chăm sóc khách hàng: đánh giá, liên hệ hỗ trợ, thông báo và trợ lý tư vấn từ catalog công khai.
'''+s[b:]
s=s.replace('Khi tạo đơn, backend kiểm tra lại sản phẩm, tồn kho, địa chỉ và coupon; tính lại các khoản tiền; sau đó cập nhật đơn hàng, tồn kho và giỏ hàng trong một giao dịch. Cách xử lý này giúp hạn chế sai lệch dữ liệu giữa thông tin khách hàng gửi lên và trạng thái thực tế của hệ thống.','Khi tạo đơn, backend kiểm tra sản phẩm, số lượng khả dụng, thông tin nhận hàng và ưu đãi; tính lại tiền rồi ghi đơn, phân bổ hàng và cập nhật giỏ trong giao dịch. Thông tin sản phẩm, giá và người nhận được lưu tại thời điểm mua để bảo toàn lịch sử. Các tác động ở dịch vụ ngoài được xử lý theo cơ chế tích hợp riêng.')
a=s.index('Các mục tiêu cụ thể gồm:');b=s.index('1.5.',a)
s=s[:a]+'''Các mục tiêu cụ thể gồm:
- Xây dựng giao diện mua hàng và khu vực vận hành phù hợp từng vai trò.
- Quản lý tài khoản, xác thực, phiên và quyền sở hữu dữ liệu.
- Cung cấp catalog, giỏ hàng, địa chỉ, đặt hàng và tra cứu cho khách có tài khoản hoặc khách vãng lai.
- Tính lại giá, phí, ưu đãi và số lượng khả dụng tại backend; bảo toàn thông tin đơn hàng.
- Quản lý kho theo lô và hạn sử dụng, ưu tiên phân bổ theo FEFO.
- Hỗ trợ thanh toán, xử lý đơn, giao nhận, hóa đơn và chăm sóc sau mua theo cấu hình hệ thống.
- Tích hợp trợ lý tư vấn chỉ sử dụng dữ liệu công khai.
- Tổ chức kiến trúc, dữ liệu và kiểm thử để thuận tiện bảo trì, mở rộng.
'''+s[b:]
a=s.index('Phạm vi chức năng tập trung vào:');b=s.index('Phạm vi công nghệ:',a)
s=s[:a]+'''Phạm vi chức năng tập trung vào:
- Cửa hàng công khai với catalog, tìm kiếm, lọc và thông tin sản phẩm.
- Tài khoản, hồ sơ, giỏ, yêu thích, địa chỉ và mua hàng khách vãng lai.
- Checkout, ưu đãi, đơn hàng, thanh toán, hóa đơn và theo dõi trạng thái.
- Quản trị tài khoản, hàng hóa, nhà cung cấp, kho, coupon và báo cáo.
- Xử lý đơn, phân công giao nội bộ và tích hợp giao nhận bên ngoài.
- Đánh giá, điểm thưởng, liên hệ, thông báo và trợ lý tư vấn.
'''+s[b:]
s=s.replace('-\tCung cấp công cụ quản trị danh mục, sản phẩm và tài khoản.','- Cung cấp công cụ quản trị tài khoản, catalog, nguồn cung và kho theo lô.\n- Kết nối xử lý đơn, thanh toán và giao nhận theo quyền; hỗ trợ chăm sóc sau mua và tư vấn thông tin công khai.')
s=s.replace('1.8. Bố cục báo cáo','1.8. Bố cục báo cáo')
lines=[]
for line in s.splitlines():
 line=line.strip()
 if not line:continue
 if line.startswith('CHƯƠNG 1.'):line='# CHƯƠNG 1 TỔNG QUAN ĐỀ TÀI'
 elif re.match(r'^1\.[1-8]\. ',line):line='## '+re.sub(r'^(1\.[1-8])\. ',r'\1 ',line)
 elif line.startswith('-'):line='- '+line[1:].strip()
 lines.append(line)
ch1='\n\n'.join(lines)
ch1+='\n\nTừ bài toán và phạm vi trên, chương 2 trình bày các nền tảng về thương mại điện tử nông sản, kiến trúc web, giao dịch, bảo mật và AI. Những cơ sở này giải thích các lựa chọn công nghệ và làm tiền đề cho thiết kế hệ thống ở chương 3.'
ch2=Path('output/report/GreenFarm_Chuong_2.md').read_text(encoding='utf-8')
ch2=ch2.replace('Chương này trình bày cơ sở lý thuyết về thương mại điện tử nông sản, kiến trúc phần mềm, dữ liệu, bảo mật và trí tuệ nhân tạo, cùng các công nghệ sử dụng trong GreenFarm. Nội dung được đối chiếu với mã nguồn local để làm cơ sở phân tích và thiết kế hệ thống.','Trên cơ sở bài toán và phạm vi đã xác định ở chương 1, chương này trình bày nền tảng về thương mại điện tử nông sản, kiến trúc phần mềm, dữ liệu, bảo mật và trí tuệ nhân tạo. Các công nghệ được lựa chọn cho GreenFarm được giới thiệu gắn với nghiệp vụ và cấu hình local của hệ thống.')
Path('output/report/GreenFarm_Chuong_1_2.md').write_text(ch1+'\n\n'+ch2,encoding='utf-8')
script=Path('tmp/report/build_word.py').read_text(encoding='utf-8-sig')
script=script.replace('GreenFarm_Chuong_2.md','GreenFarm_Chuong_1_2.md').replace('GreenFarm_Chuong_2.docx','GreenFarm_Chuong_1_2.docx')
script=script.replace("if p.startswith('# '):d.add_paragraph(p[2:],'Title')","if p.startswith('# '):\n  if len(d.paragraphs)>0:d.add_page_break()\n  d.add_paragraph(p[2:],'Title')")
script=script.replace(" else:\n  para=d.add_paragraph(p)"," elif p.startswith('- '):\n  para=d.add_paragraph(p[2:],'List Bullet');para.paragraph_format.first_line_indent=Pt(-10);para.paragraph_format.left_indent=Pt(22)\n else:\n  para=d.add_paragraph(p)")
script=script.replace("['Normal','Title','Heading 1','Heading 2','Caption']","['Normal','Title','Heading 1','Heading 2','Caption','List Bullet']")
script=script.replace("d.core_properties.title='GreenFarm Chương 2 Cơ sở lý thuyết và công nghệ'","d.core_properties.title='GreenFarm Chương 1 và Chương 2'")
Path('tmp/report/build_combined.py').write_text(script,encoding='utf-8')
print('Prepared chapters 1 and 2')
