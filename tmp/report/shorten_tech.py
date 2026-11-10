from pathlib import Path
import re
p=Path('tmp/report/authoring.md');s=p.read_text(encoding='utf-8')
sections={
'2.5.2':'''Next.js tổ chức trang, layout và điều hướng trên nền React. GreenFarm dùng App Router, chia các khu vực khách hàng, quản trị, nhân viên và giao hàng theo tuyến đường. React hỗ trợ component tái sử dụng; TypeScript kiểm tra kiểu dữ liệu.

Tailwind CSS định dạng giao diện responsive; Axios và TanStack Query truy cập, đồng bộ API; Zustand quản lý trạng thái chung. React Hook Form và Zod hỗ trợ biểu mẫu. Kiểm tra phía client cải thiện trải nghiệm, không thay thế xác minh tại backend.''',
'2.5.3':'''Java 21 và Spring Boot 4.1.0 xây dựng backend với Web MVC, validation, JPA, bảo mật và email. Controller, Service, Repository tách trách nhiệm; nghiệp vụ tạo đơn và phân bổ kho được điều phối tại service.

Spring Security kết hợp bộ lọc JWT với kiểm tra quyền. Spring Data JPA ánh xạ và truy cập dữ liệu; springdoc cung cấp hợp đồng OpenAPI cùng Swagger. Lombok giảm mã lặp trong lớp Java.''',
'2.5.4':'''MySQL lưu dữ liệu nghiệp vụ; Flyway quản lý migration và JPA hỗ trợ truy cập. Ràng buộc SQL bảo vệ toàn vẹn dữ liệu.

Ảnh được lưu trên Cloudinary khi có cấu hình hoặc trong thư mục upload local; database giữ đường dẫn. Docker Compose có volume cho MySQL và upload để dữ liệu tồn tại khi tạo lại container. Volume vẫn cần sao lưu độc lập.''',
'2.5.5':'''VNPAY hỗ trợ thanh toán trực tuyến, backend xác minh chữ ký callback; sandbox phục vụ kiểm thử. GHN hỗ trợ giao nhận bên ngoài, tách với luồng phân công nhân viên giao nội bộ.

SMTP gửi hóa đơn và hỗ trợ retry; Gemini phục vụ tư vấn, Cloudinary lưu ảnh. Thông tin xác thực đặt tại backend, các chức năng hoạt động theo cấu hình dịch vụ thực tế.''',
'2.5.6':'''Spring Boot Test và Testcontainers hỗ trợ kiểm thử backend, gồm những tình huống cần MySQL thật. Frontend dùng ESLint và build Next.js để kiểm tra mã và cấu hình; các bước này không thay thế kiểm thử chức năng đầu cuối.

Git quản lý mã nguồn, npm quản lý frontend và Maven build backend. Các ca kiểm thử và kết quả thực tế được trình bày tại chương 5.''',
}
for num,body in sections.items():
 pattern=r'(### '+re.escape(num)+r'[^\n]*\n\n).*?(?=\n\n### |\n\n## |\Z)'
 s,n=re.subn(pattern,lambda m:m[1]+body,s,flags=re.S);assert n==1,num
p.write_text(s,encoding='utf-8')
