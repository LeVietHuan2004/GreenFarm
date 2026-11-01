from pathlib import Path
p=Path('output/report/GreenFarm_Chuong_2.md');s=p.read_text(encoding='utf-8')
s=s.replace('| Đóng gói, hosting | Docker, Docker Compose, Vercel | Môi trường chạy và frontend |','| Môi trường local | Docker, Docker Compose, Node.js | Chạy backend, MySQL và frontend local |')
s=s.replace('### 2.5.7 Công nghệ đóng gói và triển khai','### 2.5.7 Công nghệ đóng gói và môi trường local')
a=s.index('Frontend đã triển khai lên Vercel.');b=s.index('Những cơ sở trong chương',a)
s=s[:a]+'''Trong môi trường local, frontend chạy bằng Node.js tại cổng mặc định 3000; backend cung cấp API tại cổng 8080. MySQL trong mạng Docker dùng cổng 3306 và được ánh xạ ra cổng 3307 của máy host theo cấu hình mặc định. Địa chỉ và cổng có thể được thay đổi qua cấu hình dự án.

Frontend kết nối backend qua biến cấu hình địa chỉ API; cấu hình mặc định dùng http://localhost:8080/api. Dữ liệu nghiệp vụ được lấy từ backend và MySQL local. Các chức năng phụ thuộc dịch vụ ngoài như VNPAY, GHN, SMTP, Cloudinary hoặc Gemini chỉ hoạt động theo cấu hình và thông tin xác thực thực tế của môi trường này.

Môi trường local là căn cứ để mô tả chức năng và kiểm thử trong báo cáo. Docker Compose giúp tái lập backend và database; frontend chạy bằng lệnh npm tương ứng trong thư mục frontend. Sự có mặt của mã tích hợp dịch vụ ngoài được phân biệt với kết quả kiểm thử thành công của dịch vụ đó.

'''+s[b:]
for i,k in enumerate(['commerce','architecture','auth','ai'],1):s=s.replace(f'![Sơ đồ GreenFarm {i}](figures/hinh-2-{i}.png)','[FIGURE:'+k+']')
Path('tmp/report/authoring.md').write_text(s,encoding='utf-8')
p=Path('tmp/report/build_report.py');s=p.read_text(encoding='utf-8-sig')
s=s.replace("source=(ROOT/'GreenFarm_Chuong_2.md').read_text(encoding='utf-8-sig')","source=Path('tmp/report/authoring.md').read_text(encoding='utf-8-sig')")
s=s.replace("cap=Paragraph(txt(parts[j-1]),styles['cap']);story.extend([cap,t,Spacer(1,8)]);tables+=1","capstyle=ParagraphStyle('tablecap',parent=styles['cap'],keepWithNext=True);cap=Paragraph(txt(parts[j-1]),capstyle);story.extend([cap,t,Spacer(1,8)]);tables+=1")
s=s[:s.index('for i,p in enumerate(pdf):print')]+"for i,p in enumerate(pdf):print(i+1,len(p.get_text()))\n"
p.write_text(s,encoding='utf-8')
