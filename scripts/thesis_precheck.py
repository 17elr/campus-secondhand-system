import hashlib
import json
import re
from collections import defaultdict
from datetime import datetime
from difflib import SequenceMatcher
from pathlib import Path
from xml.etree import ElementTree
from zipfile import ZipFile

from reportlab.lib import colors
from reportlab.lib.styles import ParagraphStyle
from reportlab.lib.units import mm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, PageBreak
from xml.sax.saxutils import escape


def normalize(text):
    return ''.join(re.findall(r'[\w\u4e00-\u9fff]', text)).lower()


assert normalize('校园，Web 3！') == '校园web3'
assert SequenceMatcher(None, normalize('相同文字'), normalize('相同文字')).ratio() == 1

workspace = Path(__file__).resolve().parents[1]
package = workspace / '毕业设计交付' / '20245533131缪浩林毕业设计提交包'
source = next((package / '01论文').glob('*.docx'))
output = package / '08查重报告'
output.mkdir(exist_ok=True)
with ZipFile(source) as archive:
    document = ElementTree.fromstring(archive.read('word/document.xml'))
namespace = {'w': 'http://schemas.openxmlformats.org/wordprocessingml/2006/main'}
paragraphs = [
    ''.join(node.text or '' for node in paragraph.findall('.//w:t', namespace))
    for paragraph in document.findall('.//w:p', namespace)
]
start = paragraphs.index('一、引言')
end = paragraphs.index('参考文献', start)
table_paragraph_ids = {
    id(paragraph)
    for table in document.findall('.//w:tbl', namespace)
    for paragraph in table.findall('.//w:p', namespace)
}
all_nodes = document.findall('.//w:p', namespace)
body = [
    (index + 1, paragraphs[index], normalize(paragraphs[index]))
    for index in range(start, end)
    if id(all_nodes[index]) not in table_paragraph_ids and len(normalize(paragraphs[index])) >= 40
]
assert body and start < end
groups = defaultdict(list)
for index, original, normalized in body:
    groups[normalized].append(index)
duplicates = [locations for locations in groups.values() if len(locations) > 1]
sentence_groups = defaultdict(list)
for index, original, normalized in body:
    for sentence in re.split(r'[。！？!?；;\n]', original):
        normalized_sentence = normalize(sentence)
        if len(normalized_sentence) >= 25:
            sentence_groups[normalized_sentence].append({'paragraph': index, 'text': sentence.strip()})
repeated_sentences = [locations for locations in sentence_groups.values() if len(locations) > 1]
similar_pairs = []
for position, (index, original, normalized) in enumerate(body):
    for other_index, other_original, other_normalized in body[position + 1:]:
        matcher = SequenceMatcher(None, normalized, other_normalized, autojunk=False)
        if matcher.quick_ratio() < 0.70:
            continue
        similarity = matcher.ratio()
        if similarity >= 0.70:
            similar_pairs.append({'paragraphs': [index, other_index], 'similarity': round(similarity, 4), 'excerpts': [original, other_original]})
similar_pairs.sort(key=lambda pair: pair['similarity'], reverse=True)
queries = [
    '"新的聊天内容会很快覆盖原有消息" "商品"',
    '"商品需要具有稳定的详情入口和明确的状态"',
    '"本文重点分析这些实现如何相互配合"',
    '"聊天群易于使用，但缺少统一筛选和状态管理"',
    '"数量查询使用同一组过滤条件" "MyBatis"',
    '"商品筛选在数据库分页之前执行"',
]
timestamp = datetime.now().astimezone().isoformat(timespec='seconds')
result = {
    'report_type': '论文查重预检，非正式数据库查重',
    'generated_at': timestamp,
    'source': source.name,
    'sha256': hashlib.sha256(source.read_bytes()).hexdigest(),
    'body_range': '一、引言 至 七、总结与展望，参考文献之前',
    'eligible_paragraphs': len(body),
    'normalized_characters': sum(len(normalized) for _, _, normalized in body),
    'paragraph_pairs_compared': len(body) * (len(body) - 1) // 2,
    'exact_duplicate_paragraph_groups': duplicates,
    'repeated_sentence_groups': repeated_sentences,
    'paragraph_pairs_at_least_070': similar_pairs,
    'web_queries': queries,
    'web_review': '已执行六条公开网页检索；返回结果中未确认所选完整句子的相同来源。搜索会放宽匹配，未逐一核验所有返回网页全文，故不计作零匹配或通过证明。',
    'official_similarity_rate': None,
    'aigc_check': '未检测',
    'code_plagiarism_check': '未检测，未提供外部项目比对语料',
}
(output / '预检结果与检索记录.json').write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding='utf-8')
sections = [
    ('检测结论', [
        '已完成本地正文重复检查和六条公开网页检索抽查。正式数据库查重未执行，不能给出知网、万方、维普的整体重复率，也不能判断是否低于30%。',
        f'完全相同长段落组：{len(duplicates)}；重复长句组：{len(repeated_sentences)}；相似度达到0.70的段落对：{len(similar_pairs)}。这些是同一论文内部的统计，不能换算为论文查重率。',
        '公开网页返回结果中未确认所选完整句子的相同来源。搜索结果包含宽泛相关内容，抽查不覆盖学位论文数据库、未公开文献或全部网页。',
    ]),
    ('文件和方法', [
        f'论文：{source.name}',
        f'检查记录时间：{timestamp}',
        f'文件SHA-256：{result["sha256"]}',
        f'正文范围：{result["body_range"]}。提取Word正文文本，排除表格；选择去标点与空白后至少40字符的{len(body)}个段落，共{result["normalized_characters"]}字符，比较{result["paragraph_pairs_compared"]}对。',
        '规范化保留文字、数字和下划线，统一大小写；完全一致段落分组；按句号、问号、感叹号和分号切句，检查至少25字符的重复句。近似段落使用Python SequenceMatcher，阈值0.70，未使用学术数据库算法。',
        '未检测范围：封面、目录、摘要、参考文献、附件、表格、图片内文字、短段落、源代码相似度和AIGC。未修改原论文。',
    ]),
    ('公开网页抽查记录', queries + [result['web_review']]),
    ('引用和内容复核建议', [
        '参考文献列有7项，涉及Fielding及Spring、MyBatis、Vue、H2、Vite、Spring Security官方资料。已有引用标号不等于已验证不存在不当引用；技术说明应核对原文和引用位置。',
        '重点复核相关技术章节中的框架介绍、分层设计和分页描述，确认对应来源；总结和摘要中的功能、数量与测试结果须与提交版本一致。',
        '原需求提到JavaWeb SSM，当前论文描述Spring Boot、Spring MVC和MyBatis。提交前应向指导教师确认该技术组合是否满足框架要求；这是内容一致性问题，不属于重复率结果。',
    ]),
    ('正式检测工具和补交步骤', [
        '优先使用学校指定平台和检测版本，确认需检测的正文范围、引用排除规则及是否另需AIGC检测。现有交付说明记载指导材料要求相似度≤30%，附件名要求低于30%，建议按低于30%准备；尚未确认学校指定平台。',
        '知网个人查重：https://cx.cnki.net/。万方科研诚信个人服务：https://jc.wanfangdata.com.cn/。维普检测：https://vpcs.fanyu.com/，机构平台：https://vims.cqvip.com/；入口依据维普官方介绍和关联平台确认。',
        '本次未登录上述平台，未上传完整论文、未付费，未产生正式报告编号。平台费用和学校是否认可个人版需在检测前确认。',
        '在指定平台登录，填写论文题目和本人信息，上传同一终稿；如需付费由本人确认付款。完成检测后下载原始PDF及详细标注报告，核对题目、姓名、时间和送检正文。',
        '将平台原始报告放入本目录，再更新提交清单。若修改论文，应重新检测最终版本；本预检PDF不能作为学校要求的正式查重证明。',
        '官方来源核对日期：2026-10-10。维普官方产品介绍：https://ccd1.cqvip.com/productor/pro_vpcs.shtml；JPlag官方说明：https://github.com/jplag/jplag（用于提供语料之间的代码比对，不搜索互联网，不替代论文查重）。',
    ]),
]
details = []
for locations in duplicates:
    details.append('重复段落XML序号：' + ', '.join(map(str, locations)))
for locations in repeated_sentences:
    details.append('重复句XML序号：' + ', '.join(str(item['paragraph']) for item in locations) + '；文本：' + locations[0]['text'])
for pair in similar_pairs[:10]:
    details.append(f'近似段落XML序号{pair["paragraphs"]}，内部比较值{pair["similarity"]}；内容：' + ' / '.join(pair['excerpts']))
if not details:
    details.append('本次规则未发现符合阈值的正文内部重复项；这不代表对外部文献零重复。')
sections.insert(2, ('内部重复检查明细', details))
markdown = '# 校园二手交易系统论文查重预检报告\n\n非正式数据库查重报告，不作为查重通过证明。\n\n'
for title, paragraphs_in_section in sections:
    markdown += '## ' + title + '\n\n' + '\n\n'.join(paragraphs_in_section) + '\n\n'
(output / '论文查重预检报告.md').write_text(markdown, encoding='utf-8')
pdfmetrics.registerFont(TTFont('ReportChinese', 'C:/Windows/Fonts/simsun.ttc', subfontIndex=0))
text_style = ParagraphStyle('Body', fontName='ReportChinese', fontSize=10, leading=16, spaceAfter=9, wordWrap='CJK', textColor=colors.HexColor('#243344'))
heading_style = ParagraphStyle('Heading', parent=text_style, fontSize=13, leading=20, spaceBefore=12, spaceAfter=8, keepWithNext=True)
title_style = ParagraphStyle('Title', parent=text_style, fontSize=20, leading=28, spaceAfter=16)
story = [Paragraph('校园二手交易系统论文查重预检报告', title_style), Paragraph('非正式数据库查重报告，不作为查重通过证明。', text_style)]
for section_index, (title, paragraphs_in_section) in enumerate(sections):
    if title in ['公开网页抽查记录', '正式检测工具和补交步骤']:
        story.append(PageBreak())
    story.append(Paragraph(escape(title), heading_style))
    for content in paragraphs_in_section:
        story.append(Paragraph(escape(content), text_style))
    story.append(Spacer(1, 3 * mm))


def footer(canvas, document):
    canvas.setFont('ReportChinese', 9)
    canvas.drawString(20 * mm, 12 * mm, '预检报告 | 正式数据库重复率尚未检测')
    canvas.drawRightString(190 * mm, 12 * mm, str(document.page))


pdf_path = output / '论文查重预检报告.pdf'
SimpleDocTemplate(str(pdf_path), pagesize=(210 * mm, 297 * mm), rightMargin=20 * mm, leftMargin=20 * mm, topMargin=18 * mm, bottomMargin=22 * mm, title='校园二手交易系统论文查重预检报告', author='').build(story, onFirstPage=footer, onLaterPages=footer)
print(json.dumps({key: result[key] for key in ['eligible_paragraphs', 'normalized_characters', 'paragraph_pairs_compared', 'exact_duplicate_paragraph_groups', 'repeated_sentence_groups']}, ensure_ascii=False))
print('近似段落对数：', len(similar_pairs))
print(pdf_path)
