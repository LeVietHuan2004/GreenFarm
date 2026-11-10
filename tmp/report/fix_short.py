from pathlib import Path
import re
p=Path('tmp/report/authoring.md');s=p.read_text(encoding='utf-8')
s=re.sub(r'Người dùng GreenFarm đăng nhập bằng thông tin tài khoản\..*?\n\n','',s,count=1,flags=re.S)
s=s.replace('Hình 2.3 Luồng xác thực và kiểm tra quyền ở mức khái quát','[FIGURE:auth]\n\nHình 2.3 Luồng xác thực và kiểm tra quyền ở mức khái quát')
assert s.count('[FIGURE:')==4
p.write_text(s,encoding='utf-8')
