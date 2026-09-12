from pathlib import Path

from docx import Document
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Pt


ROOT = Path(__file__).resolve().parent
OUT = ROOT / "饭店点餐系统数据库课程设计报告.docx"


def set_font(run, size=12, bold=False):
    run.font.name = "宋体"
    run._element.rPr.rFonts.set(qn("w:eastAsia"), "宋体")
    run.font.size = Pt(size)
    run.bold = bold


def add_paragraph(doc, text="", size=12, bold=False, align=None):
    p = doc.add_paragraph()
    if align is not None:
      p.alignment = align
    run = p.add_run(text)
    set_font(run, size=size, bold=bold)
    return p


def add_heading(doc, text, level=1):
    p = doc.add_paragraph()
    run = p.add_run(text)
    set_font(run, size=16 if level == 1 else 14, bold=True)
    return p


def add_table(doc, headers, rows):
    table = doc.add_table(rows=1, cols=len(headers))
    table.style = "Table Grid"
    hdr = table.rows[0].cells
    for i, h in enumerate(headers):
        hdr[i].text = h
    for row in rows:
        cells = table.add_row().cells
        for i, value in enumerate(row):
            cells[i].text = str(value)
    for row in table.rows:
        for cell in row.cells:
            for p in cell.paragraphs:
                for run in p.runs:
                    set_font(run, size=10.5)
    return table


def set_doc_defaults(doc):
    styles = doc.styles
    normal = styles["Normal"]
    normal.font.name = "宋体"
    normal._element.rPr.rFonts.set(qn("w:eastAsia"), "宋体")
    normal.font.size = Pt(12)
    sect = doc.sections[0]
    sect.top_margin = Pt(72)
    sect.bottom_margin = Pt(72)
    sect.left_margin = Pt(72)
    sect.right_margin = Pt(72)
    for section in doc.sections:
        section.header_distance = Pt(36)
        section.footer_distance = Pt(36)


def add_page_break(doc):
    doc.add_page_break()


doc = Document()
set_doc_defaults(doc)

add_paragraph(doc, "人工智能学院", size=18, bold=True, align=WD_ALIGN_PARAGRAPH.CENTER)
add_paragraph(doc, "《数据库系统原理课程设计》课程报告", size=20, bold=True, align=WD_ALIGN_PARAGRAPH.CENTER)
add_paragraph(doc, "", size=12)
add_paragraph(doc, "题    目：饭店点餐系统数据库设计与实现", size=14, align=WD_ALIGN_PARAGRAPH.CENTER)
add_paragraph(doc, "专    业：计算机科学与技术", size=14, align=WD_ALIGN_PARAGRAPH.CENTER)
add_paragraph(doc, "年级班级：24级计算机科学与技术    班", size=14, align=WD_ALIGN_PARAGRAPH.CENTER)
add_paragraph(doc, "学生1姓名及学号：            ", size=14, align=WD_ALIGN_PARAGRAPH.CENTER)
add_paragraph(doc, "学生2姓名及学号：            ", size=14, align=WD_ALIGN_PARAGRAPH.CENTER)
add_paragraph(doc, "学生3姓名及学号：            ", size=14, align=WD_ALIGN_PARAGRAPH.CENTER)
add_paragraph(doc, "学生4姓名及学号：            ", size=14, align=WD_ALIGN_PARAGRAPH.CENTER)
add_paragraph(doc, "学生5姓名及学号：            ", size=14, align=WD_ALIGN_PARAGRAPH.CENTER)
add_paragraph(doc, "指导教师：            ", size=14, align=WD_ALIGN_PARAGRAPH.CENTER)
add_paragraph(doc, "2026年6月", size=14, align=WD_ALIGN_PARAGRAPH.CENTER)
add_page_break(doc)

add_heading(doc, "★小组分工与合作情况", 1)
add_paragraph(doc, "本系统以饭店点餐业务为背景，围绕顾客点餐、员工处理订单、管理员维护基础数据等业务过程展开数据库设计。小组成员可根据实际完成情况填写下表。")
add_table(
    doc,
    ["成员", "主要任务", "完成内容"],
    [
        ["成员1", "需求分析、业务流程梳理", "整理系统角色、业务需求和数据需求"],
        ["成员2", "概念结构设计", "设计 E-R 图，确定实体、属性和联系"],
        ["成员3", "逻辑结构设计", "将 E-R 图转换为关系模式，确定主键与外键"],
        ["成员4", "物理结构设计", "编写 MySQL 建库建表语句，设计字段类型"],
        ["成员5", "系统功能设计与报告整理", "整理管理员、员工、顾客功能模块及课程报告"],
    ],
)

add_heading(doc, "一、需求分析", 1)
add_heading(doc, "（一）系统背景", 2)
add_paragraph(doc, "饭店点餐系统面向餐饮门店的日常经营管理，主要解决顾客点餐、员工处理订单、餐桌使用管理、菜品信息维护和管理员统计查询等问题。传统人工记录方式容易出现订单遗漏、餐桌状态不清、菜品销售统计困难等情况，因此需要建立一个结构清晰、数据一致性较高的数据库系统。")
add_heading(doc, "（二）用户角色需求", 2)
add_paragraph(doc, "1. 管理员：管理员负责系统基础数据维护和综合查询，主要包括员工表、顾客表、餐桌表、菜谱表的维护，同时可查询订单信息和菜品销售排行。")
add_paragraph(doc, "2. 员工：员工负责日常业务处理，主要包括维护个人信息、查看订单表、订单细则、餐桌表、顾客表，以及查询顾客评价信息。")
add_paragraph(doc, "3. 顾客：顾客负责维护个人信息、浏览菜谱、提交订单、查询个人消费记录和餐桌相关信息。")
add_heading(doc, "（三）数据需求", 2)
add_paragraph(doc, "系统需要保存员工、顾客、管理员、餐桌、菜谱、订单和订单细则等数据。订单是系统中的核心业务数据，它与员工、顾客、餐桌存在联系；订单细则用于记录一张订单中包含的具体菜品、数量、消费金额和备注；菜谱表为订单细则提供菜品编号、菜名、菜品类别和售价等基础数据。")
add_heading(doc, "（四）功能需求", 2)
add_paragraph(doc, "系统功能可分为数据维护、数据查询、排行榜查询和点餐业务处理。数据维护主要由管理员和员工完成；数据查询面向管理员、员工和顾客；排行榜查询主要由管理员根据订单细则和菜谱统计菜品销售情况；点餐业务则由顾客提交订单并形成订单表和订单细则表记录。")

add_heading(doc, "二、概念结构设计", 1)
add_heading(doc, "（一）实体及属性", 2)
add_table(
    doc,
    ["实体", "属性", "主键说明"],
    [
        ["员工", "工号、姓名、年龄、性别、密码", "工号"],
        ["顾客", "顾客编号、姓名、性别、电话", "顾客编号"],
        ["订单", "订单编号、消费金额、订单时间", "订单编号"],
        ["餐桌", "餐桌号、座位数、使用状态", "餐桌号"],
        ["订单细则", "细则编号、订单编号、菜品编号、菜品数量、消费金额、备注", "细则编号"],
        ["菜谱", "菜品编号、菜名、菜品类别、菜品售价", "菜品编号"],
        ["管理员", "编号、姓名、密码", "编号"],
    ],
)
add_heading(doc, "（二）实体之间的联系", 2)
add_table(
    doc,
    ["联系", "联系类型", "说明"],
    [
        ["员工—管理—订单", "1:n", "一名员工可管理多张订单，一张订单仅由一名员工处理。"],
        ["顾客—消费—订单", "1:n", "一名顾客可产生多张订单，一张订单属于一名顾客。"],
        ["订单—组成—订单细则", "1:n", "一张订单包含多条订单细则，一条订单细则只属于一张订单。"],
        ["订单—使用—餐桌", "n:1", "一张订单对应一张餐桌，一张餐桌可以被多张订单先后使用。"],
        ["菜谱—参照—订单细则", "1:n", "一道菜品可出现在多条订单细则中，一条订单细则对应一道菜品。"],
    ],
)
add_paragraph(doc, "概念结构设计阶段的核心任务是从现实业务中抽象出实体、属性和联系。本系统的 E-R 设计以订单为中心，将员工、顾客、餐桌、菜谱等实体联系起来，能够较完整地描述饭店点餐业务过程。")

add_heading(doc, "三、逻辑结构设计", 1)
add_heading(doc, "（一）关系模式转换", 2)
add_paragraph(doc, "根据概念结构设计，将 E-R 图转换为以下关系模式：")
relations = [
    "员工(工号, 姓名, 年龄, 性别, 密码)",
    "顾客(顾客编号, 姓名, 性别, 电话)",
    "餐桌(餐桌号, 座位数, 使用状态)",
    "菜谱(菜品编号, 菜名, 菜品类别, 菜品售价)",
    "管理员(编号, 姓名, 密码)",
    "订单(订单编号, 消费金额, 订单时间, 工号, 顾客编号, 餐桌号)",
    "订单细则(细则编号, 订单编号, 菜品编号, 菜品数量, 消费金额, 备注)",
]
for item in relations:
    add_paragraph(doc, item)
add_heading(doc, "（二）主键与外键设计", 2)
add_table(
    doc,
    ["关系模式", "主键", "外键"],
    [
        ["员工", "工号", "无"],
        ["顾客", "顾客编号", "无"],
        ["餐桌", "餐桌号", "无"],
        ["菜谱", "菜品编号", "无"],
        ["管理员", "编号", "无"],
        ["订单", "订单编号", "工号参照员工表，顾客编号参照顾客表，餐桌号参照餐桌表"],
        ["订单细则", "细则编号", "订单编号参照订单表，菜品编号参照菜谱表"],
    ],
)
add_heading(doc, "（三）规范化分析", 2)
add_paragraph(doc, "本系统各关系模式均以主键唯一标识记录。员工、顾客、餐桌、菜谱和管理员表只保存各自实体的基本属性，避免重复存储。订单表只保存订单主信息和外键，不直接保存顾客姓名、员工姓名、菜名等可由其他表获得的信息。订单细则表用于保存一张订单中不同菜品的数量和金额，避免在订单表中重复设置多个菜品字段。整体设计基本满足第三范式要求，减少了数据冗余，提高了数据一致性。")

add_heading(doc, "四、物理结构设计", 1)
add_heading(doc, "（一）数据库与表名设计", 2)
add_paragraph(doc, "本系统数据库名称为 resjk。物理表名采用英文命名，便于在 MySQL、Spring Boot 和 Vue 项目中统一调用。主要表包括 employee、customer、dining_table、menu、administrator、orders、order_detail。")
add_heading(doc, "（二）字段类型设计", 2)
add_table(
    doc,
    ["表名", "字段设计说明"],
    [
        ["employee", "eno 使用 char(9) 作为主键；ename、esex、epassword 使用字符类型；eage 使用 smallint。"],
        ["customer", "cno 使用 char(9) 作为主键；cname、csex、cphone 使用字符类型。"],
        ["dining_table", "tno 使用 char(9) 作为主键；seats 使用 smallint；tstatus 保存餐桌使用状态。"],
        ["menu", "mno 使用 char(9) 作为主键；mprice 使用 decimal(8,2) 保存菜品售价。"],
        ["administrator", "ano 使用 char(9) 作为主键；aname 和 apassword 保存管理员姓名和密码。"],
        ["orders", "ono 使用 char(9) 作为主键；total_amount 使用 decimal(8,2)；order_time 使用 datetime；eno、cno、tno 为外键。"],
        ["order_detail", "dno 使用 char(9) 作为主键；ono、mno 为外键；dish_count 保存数量；amount 保存金额；remark 保存备注。"],
    ],
)
add_heading(doc, "（三）建库建表 SQL 说明", 2)
add_paragraph(doc, "本系统使用 MySQL 建库建表，数据库文件为 resjk_restaurant.sql。建表时先建立基础实体表，再建立包含外键的订单表和订单细则表，以保证外键参照对象已经存在。")
add_paragraph(doc, "示例：create database resjk; use resjk; 随后依次创建 employee、customer、dining_table、menu、administrator、orders、order_detail 等表。")

add_heading(doc, "五、应用系统功能概要设计", 1)
add_heading(doc, "（一）各角色功能导航设计", 2)
add_table(
    doc,
    ["角色", "功能模块", "说明"],
    [
        ["管理员", "数据维护", "维护员工表、顾客表、餐桌表、菜谱表。"],
        ["管理员", "数据查询", "查询员工、顾客、餐桌、菜谱、订单等基础数据。"],
        ["管理员", "排行榜查询", "根据订单细则和菜谱统计菜品销售排行。"],
        ["员工", "数据维护", "维护员工个人信息和评价表。"],
        ["员工", "数据查询", "查询评价表、订单表、订单细则、餐桌表、顾客表。"],
        ["顾客", "数据维护", "维护个人信息。"],
        ["顾客", "数据查询", "查询个人消费记录、菜谱视图图、餐桌表。"],
        ["顾客", "点餐业务", "浏览菜谱，提交订单，生成订单和订单细则。"],
    ],
)
add_heading(doc, "（二）各功能模块数据设计", 2)
add_paragraph(doc, "1. 管理员—员工管理模块：管理员可对员工表进行新增、查询、修改和删除操作。该模块主要调用 employee 表，涉及字段包括 eno、ename、eage、esex、epassword。")
add_paragraph(doc, "2. 管理员—菜谱管理模块：管理员可维护菜谱信息，包括菜品编号、菜名、菜品类别和菜品售价。该模块调用 menu 表。")
add_paragraph(doc, "3. 管理员—排行榜查询模块：管理员根据 order_detail 表中的菜品数量进行汇总，再关联 menu 表得到菜品名称和菜品售价，从而形成菜品销售排行。")
add_paragraph(doc, "4. 员工—订单处理模块：员工查询 orders 表获得订单主信息，查询 order_detail 表获得菜品明细，并可结合 customer 表和 dining_table 表查看顾客和餐桌信息。")
add_paragraph(doc, "5. 顾客—菜谱浏览与点餐模块：顾客浏览 menu 表中的菜品信息，提交点餐后向 orders 表写入订单主记录，并向 order_detail 表写入每道菜品的数量、金额和备注。")
add_paragraph(doc, "6. 顾客—个人消费记录模块：顾客通过 customer 表确定身份，再查询 orders 表和 order_detail 表查看历史消费记录。")

add_heading(doc, "六、参考文献", 1)
refs = [
    "[1] 王珊, 萨师煊. 数据库系统概论[M]. 北京: 高等教育出版社.",
    "[2] Abraham Silberschatz, Henry F. Korth, S. Sudarshan. Database System Concepts[M]. McGraw-Hill.",
    "[3] MySQL 8.0 Reference Manual[EB/OL]. https://dev.mysql.com/doc/",
    "[4] Spring Boot Reference Documentation[EB/OL]. https://docs.spring.io/spring-boot/",
    "[5] Vue.js Documentation[EB/OL]. https://vuejs.org/",
]
for ref in refs:
    add_paragraph(doc, ref)

add_heading(doc, "七、总结与体会", 1)
add_heading(doc, "（一）技术性总结", 2)
add_paragraph(doc, "通过本次课程设计，完成了从需求分析到数据库实现的完整过程。首先根据饭店点餐业务识别出员工、顾客、订单、餐桌、订单细则、菜谱和管理员等实体；其次通过 E-R 图描述实体属性和联系；然后将概念模型转换为关系模式，确定主键、外键和字段类型；最后使用 MySQL 建立数据库，并结合 Spring Boot 和 Vue3 设计系统功能页面。该过程加深了对数据库设计阶段、规范化思想、外键约束和业务数据流转的理解。")
add_heading(doc, "（二）非技术性总结", 2)
add_paragraph(doc, "本次课程设计也体现了团队协作的重要性。系统设计并不是单纯编写 SQL 语句，而是需要先理解实际业务，再逐步抽象为数据库模型。在讨论过程中，小组成员需要对实体划分、字段命名、联系类型和功能模块进行统一，避免后续实现时出现概念不一致的问题。通过本次实践，进一步认识到良好的前期分析和规范的文档整理对项目完成质量具有重要影响。")

doc.save(OUT)
print(OUT)
