-- ============================================================
-- schema-baseline.sql — PhytoTrack 業務初始基準（測試／開發用）
-- 內容＝業務初始：完整表結構＋參照種子，不含作物種子與業務資料。
-- 測試與開發以此為基礎加入案件、送件人、作物等資料。
-- 注意：與 schema.sql 保持結構同步；種子變更時兩檔同步更新。
-- 建立順序考量 FK 依賴，請依序執行
-- ============================================================

-- ============================================================
-- sender_types（送件人身分別）
-- ============================================================
CREATE TABLE IF NOT EXISTS sender_types (
  sender_type_id INTEGER PRIMARY KEY,
  sender_type    TEXT    NOT NULL
);

INSERT OR IGNORE INTO sender_types (sender_type_id, sender_type) VALUES
  (1, '農民'),
  (2, '農藥商'),
  (3, '其他');

-- ============================================================
-- cities（縣市）
-- ============================================================
CREATE TABLE IF NOT EXISTS cities (
  city_id    INTEGER PRIMARY KEY,
  city       TEXT    NOT NULL
);

INSERT OR IGNORE INTO cities (city_id, city) VALUES
  (1,  '未知'  ),
  (2,  '臺北市'),
  (3,  '臺中市'),
  (4,  '基隆市'),
  (5,  '臺南市'),
  (6,  '高雄市'),
  (7,  '新北市'),
  (8,  '宜蘭縣'),
  (9,  '桃園市'),
  (10, '嘉義市'),
  (11, '新竹縣'),
  (12, '苗栗縣'),
  (13, '南投縣'),
  (14, '彰化縣'),
  (15, '新竹市'),
  (16, '雲林縣'),
  (17, '嘉義縣'),
  (18, '屏東縣'),
  (19, '花蓮縣'),
  (20, '臺東縣'),
  (21, '金門縣'),
  (22, '澎湖縣'),
  (23, '連江縣');

-- ============================================================
-- districts（鄉鎮市區）
-- ============================================================
CREATE TABLE IF NOT EXISTS districts (
  district_id INTEGER PRIMARY KEY,
  district    TEXT    NOT NULL,
  city_id     INTEGER NOT NULL REFERENCES cities(city_id)
);

INSERT OR IGNORE INTO districts (district_id, district, city_id) VALUES
  (1,   '未知',     1),
  (2,   '未知',     2),
  (3,   '中正區',   2),
  (4,   '大同區',   2),
  (5,   '中山區',   2),
  (6,   '松山區',   2),
  (7,   '大安區',   2),
  (8,   '萬華區',   2),
  (9,   '信義區',   2),
  (10,  '士林區',   2),
  (11,  '北投區',   2),
  (12,  '內湖區',   2),
  (13,  '南港區',   2),
  (14,  '文山區',   2),
  (15,  '未知',     3),
  (16,  '中區',     3),
  (17,  '東區',     3),
  (18,  '南區',     3),
  (19,  '西區',     3),
  (20,  '北區',     3),
  (21,  '北屯區',   3),
  (22,  '西屯區',   3),
  (23,  '南屯區',   3),
  (24,  '太平區',   3),
  (25,  '大里區',   3),
  (26,  '霧峰區',   3),
  (27,  '烏日區',   3),
  (28,  '豐原區',   3),
  (29,  '后里區',   3),
  (30,  '石岡區',   3),
  (31,  '東勢區',   3),
  (32,  '和平區',   3),
  (33,  '新社區',   3),
  (34,  '潭子區',   3),
  (35,  '大雅區',   3),
  (36,  '神岡區',   3),
  (37,  '大肚區',   3),
  (38,  '沙鹿區',   3),
  (39,  '龍井區',   3),
  (40,  '梧棲區',   3),
  (41,  '清水區',   3),
  (42,  '大甲區',   3),
  (43,  '外埔區',   3),
  (44,  '大安區',   3),
  (45,  '未知',     4),
  (46,  '仁愛區',   4),
  (47,  '信義區',   4),
  (48,  '中正區',   4),
  (49,  '中山區',   4),
  (50,  '安樂區',   4),
  (51,  '暖暖區',   4),
  (52,  '七堵區',   4),
  (53,  '未知',     5),
  (54,  '中西區',   5),
  (55,  '東區',     5),
  (56,  '南區',     5),
  (57,  '北區',     5),
  (58,  '安平區',   5),
  (59,  '安南區',   5),
  (60,  '永康區',   5),
  (61,  '歸仁區',   5),
  (62,  '新化區',   5),
  (63,  '左鎮區',   5),
  (64,  '玉井區',   5),
  (65,  '楠西區',   5),
  (66,  '南化區',   5),
  (67,  '仁德區',   5),
  (68,  '關廟區',   5),
  (69,  '龍崎區',   5),
  (70,  '官田區',   5),
  (71,  '麻豆區',   5),
  (72,  '佳里區',   5),
  (73,  '西港區',   5),
  (74,  '七股區',   5),
  (75,  '將軍區',   5),
  (76,  '學甲區',   5),
  (77,  '北門區',   5),
  (78,  '新營區',   5),
  (79,  '後壁區',   5),
  (80,  '白河區',   5),
  (81,  '東山區',   5),
  (82,  '六甲區',   5),
  (83,  '下營區',   5),
  (84,  '柳營區',   5),
  (85,  '鹽水區',   5),
  (86,  '善化區',   5),
  (87,  '大內區',   5),
  (88,  '山上區',   5),
  (89,  '新市區',   5),
  (90,  '安定區',   5),
  (91,  '未知',     6),
  (92,  '新興區',   6),
  (93,  '前金區',   6),
  (94,  '苓雅區',   6),
  (95,  '鹽埕區',   6),
  (96,  '鼓山區',   6),
  (97,  '旗津區',   6),
  (98,  '前鎮區',   6),
  (99,  '三民區',   6),
  (100, '楠梓區',   6),
  (101, '小港區',   6),
  (102, '左營區',   6),
  (103, '仁武區',   6),
  (104, '大社區',   6),
  (105, '岡山區',   6),
  (106, '路竹區',   6),
  (107, '阿蓮區',   6),
  (108, '田寮區',   6),
  (109, '燕巢區',   6),
  (110, '橋頭區',   6),
  (111, '梓官區',   6),
  (112, '彌陀區',   6),
  (113, '永安區',   6),
  (114, '湖內區',   6),
  (115, '鳳山區',   6),
  (116, '大寮區',   6),
  (117, '林園區',   6),
  (118, '鳥松區',   6),
  (119, '大樹區',   6),
  (120, '旗山區',   6),
  (121, '美濃區',   6),
  (122, '六龜區',   6),
  (123, '內門區',   6),
  (124, '杉林區',   6),
  (125, '甲仙區',   6),
  (126, '桃源區',   6),
  (127, '那瑪夏區', 6),
  (128, '茂林區',   6),
  (129, '茄萣區',   6),
  (130, '未知',     7),
  (131, '萬里區',   7),
  (132, '金山區',   7),
  (133, '板橋區',   7),
  (134, '汐止區',   7),
  (135, '深坑區',   7),
  (136, '石碇區',   7),
  (137, '瑞芳區',   7),
  (138, '平溪區',   7),
  (139, '雙溪區',   7),
  (140, '貢寮區',   7),
  (141, '新店區',   7),
  (142, '坪林區',   7),
  (143, '烏來區',   7),
  (144, '永和區',   7),
  (145, '中和區',   7),
  (146, '土城區',   7),
  (147, '三峽區',   7),
  (148, '樹林區',   7),
  (149, '鶯歌區',   7),
  (150, '三重區',   7),
  (151, '新莊區',   7),
  (152, '泰山區',   7),
  (153, '林口區',   7),
  (154, '蘆洲區',   7),
  (155, '五股區',   7),
  (156, '八里區',   7),
  (157, '淡水區',   7),
  (158, '三芝區',   7),
  (159, '石門區',   7),
  (160, '未知',     8),
  (161, '宜蘭市',   8),
  (162, '頭城鎮',   8),
  (163, '礁溪鄉',   8),
  (164, '壯圍鄉',   8),
  (165, '員山鄉',   8),
  (166, '羅東鎮',   8),
  (167, '三星鄉',   8),
  (168, '大同鄉',   8),
  (169, '五結鄉',   8),
  (170, '冬山鄉',   8),
  (171, '蘇澳鎮',   8),
  (172, '南澳鄉',   8),
  (173, '未知',     9),
  (174, '中壢區',   9),
  (175, '平鎮區',   9),
  (176, '龍潭區',   9),
  (177, '楊梅區',   9),
  (178, '新屋區',   9),
  (179, '觀音區',   9),
  (180, '桃園區',   9),
  (181, '龜山區',   9),
  (182, '八德區',   9),
  (183, '大溪區',   9),
  (184, '復興區',   9),
  (185, '大園區',   9),
  (186, '蘆竹區',   9),
  (187, '未知',     10),
  (188, '西區',     10),
  (189, '東區',     10),
  (190, '未知',     11),
  (191, '竹北市',   11),
  (192, '湖口鄉',   11),
  (193, '新豐鄉',   11),
  (194, '新埔鎮',   11),
  (195, '關西鎮',   11),
  (196, '芎林鄉',   11),
  (197, '寶山鄉',   11),
  (198, '竹東鎮',   11),
  (199, '五峰鄉',   11),
  (200, '橫山鄉',   11),
  (201, '尖石鄉',   11),
  (202, '北埔鄉',   11),
  (203, '峨嵋鄉',   11),
  (204, '未知',     12),
  (205, '竹南鎮',   12),
  (206, '頭份市',   12),
  (207, '三灣鄉',   12),
  (208, '南庄鄉',   12),
  (209, '獅潭鄉',   12),
  (210, '後龍鎮',   12),
  (211, '通霄鎮',   12),
  (212, '苑裡鎮',   12),
  (213, '苗栗市',   12),
  (214, '造橋鄉',   12),
  (215, '頭屋鄉',   12),
  (216, '公館鄉',   12),
  (217, '大湖鄉',   12),
  (218, '泰安鄉',   12),
  (219, '銅鑼鄉',   12),
  (220, '三義鄉',   12),
  (221, '西湖鄉',   12),
  (222, '卓蘭鎮',   12),
  (223, '未知',     13),
  (224, '南投市',   13),
  (225, '中寮鄉',   13),
  (226, '草屯鎮',   13),
  (227, '國姓鄉',   13),
  (228, '埔里鎮',   13),
  (229, '仁愛鄉',   13),
  (230, '名間鄉',   13),
  (231, '集集鎮',   13),
  (232, '水里鄉',   13),
  (233, '魚池鄉',   13),
  (234, '信義鄉',   13),
  (235, '竹山鎮',   13),
  (236, '鹿谷鄉',   13),
  (237, '未知',     14),
  (238, '彰化市',   14),
  (239, '芬園鄉',   14),
  (240, '花壇鄉',   14),
  (241, '秀水鄉',   14),
  (242, '鹿港鎮',   14),
  (243, '福興鄉',   14),
  (244, '線西鄉',   14),
  (245, '和美鎮',   14),
  (246, '伸港鄉',   14),
  (247, '員林市',   14),
  (248, '社頭鄉',   14),
  (249, '永靖鄉',   14),
  (250, '埔心鄉',   14),
  (251, '溪湖鎮',   14),
  (252, '大村鄉',   14),
  (253, '埔鹽鄉',   14),
  (254, '田中鎮',   14),
  (255, '北斗鎮',   14),
  (256, '田尾鄉',   14),
  (257, '埤頭鄉',   14),
  (258, '溪州鄉',   14),
  (259, '竹塘鄉',   14),
  (260, '二林鎮',   14),
  (261, '大城鄉',   14),
  (262, '芳苑鄉',   14),
  (263, '二水鄉',   14),
  (264, '未知',     15),
  (265, '東區',     15),
  (266, '北區',     15),
  (267, '香山區',   15),
  (268, '未知',     16),
  (269, '斗南鎮',   16),
  (270, '大埤鄉',   16),
  (271, '虎尾鎮',   16),
  (272, '土庫鎮',   16),
  (273, '褒忠鄉',   16),
  (274, '東勢鄉',   16),
  (275, '臺西鄉',   16),
  (276, '崙背鄉',   16),
  (277, '麥寮鄉',   16),
  (278, '斗六市',   16),
  (279, '林內鄉',   16),
  (280, '古坑鄉',   16),
  (281, '莿桐鄉',   16),
  (282, '西螺鎮',   16),
  (283, '二崙鄉',   16),
  (284, '北港鎮',   16),
  (285, '水林鄉',   16),
  (286, '口湖鄉',   16),
  (287, '四湖鄉',   16),
  (288, '元長鄉',   16),
  (289, '未知',     17),
  (290, '番路鄉',   17),
  (291, '梅山鄉',   17),
  (292, '竹崎鄉',   17),
  (293, '阿里山鄉', 17),
  (294, '中埔鄉',   17),
  (295, '大埔鄉',   17),
  (296, '水上鄉',   17),
  (297, '鹿草鄉',   17),
  (298, '太保市',   17),
  (299, '朴子市',   17),
  (300, '東石鄉',   17),
  (301, '六腳鄉',   17),
  (302, '新港鄉',   17),
  (303, '民雄鄉',   17),
  (304, '大林鎮',   17),
  (305, '溪口鄉',   17),
  (306, '義竹鄉',   17),
  (307, '布袋鎮',   17),
  (308, '未知',     18),
  (309, '屏東市',   18),
  (310, '三地門鄉', 18),
  (311, '霧臺鄉',   18),
  (312, '瑪家鄉',   18),
  (313, '九如鄉',   18),
  (314, '里港鄉',   18),
  (315, '高樹鄉',   18),
  (316, '盬埔鄉',   18),
  (317, '長治鄉',   18),
  (318, '麟洛鄉',   18),
  (319, '竹田鄉',   18),
  (320, '內埔鄉',   18),
  (321, '萬丹鄉',   18),
  (322, '潮州鎮',   18),
  (323, '泰武鄉',   18),
  (324, '來義鄉',   18),
  (325, '萬巒鄉',   18),
  (326, '崁頂鄉',   18),
  (327, '新埤鄉',   18),
  (328, '南州鄉',   18),
  (329, '林邊鄉',   18),
  (330, '東港鎮',   18),
  (331, '琉球鄉',   18),
  (332, '佳冬鄉',   18),
  (333, '新園鄉',   18),
  (334, '枋寮鄉',   18),
  (335, '枋山鄉',   18),
  (336, '春日鄉',   18),
  (337, '獅子鄉',   18),
  (338, '車城鄉',   18),
  (339, '牡丹鄉',   18),
  (340, '恆春鎮',   18),
  (341, '滿州鄉',   18),
  (342, '未知',     19),
  (343, '花蓮市',   19),
  (344, '新城鄉',   19),
  (345, '秀林鄉',   19),
  (346, '吉安鄉',   19),
  (347, '壽豐鄉',   19),
  (348, '鳳林鎮',   19),
  (349, '光復鄉',   19),
  (350, '豐濱鄉',   19),
  (351, '瑞穗鄉',   19),
  (352, '萬榮鄉',   19),
  (353, '玉里鎮',   19),
  (354, '卓溪鄉',   19),
  (355, '富里鄉',   19),
  (356, '未知',     20),
  (357, '臺東市',   20),
  (358, '綠島鄉',   20),
  (359, '蘭嶼鄉',   20),
  (360, '延平鄉',   20),
  (361, '卑南鄉',   20),
  (362, '鹿野鄉',   20),
  (363, '關山鎮',   20),
  (364, '海端鄉',   20),
  (365, '池上鄉',   20),
  (366, '東河鄉',   20),
  (367, '成功鎮',   20),
  (368, '長濱鄉',   20),
  (369, '太麻里鄉', 20),
  (370, '金峰鄉',   20),
  (371, '大武鄉',   20),
  (372, '達仁鄉',   20),
  (373, '未知',     21),
  (374, '金沙鎮',   21),
  (375, '金湖鎮',   21),
  (376, '金寧鄉',   21),
  (377, '金城鎮',   21),
  (378, '烈嶼鄉',   21),
  (379, '烏坵鄉',   21),
  (380, '未知',     22),
  (381, '馬公市',   22),
  (382, '西嶼鄉',   22),
  (383, '望安鄉',   22),
  (384, '七美鄉',   22),
  (385, '白沙鄉',   22),
  (386, '湖西鄉',   22),
  (387, '未知',     23),
  (388, '南竿鄉',   23),
  (389, '北竿鄉',   23),
  (390, '莒光鄉',   23),
  (391, '東引鄉',   23);

CREATE INDEX IF NOT EXISTS idx_districts_city_id ON districts(city_id);

-- ============================================================
-- users（系統使用者）
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
  user_id      INTEGER PRIMARY KEY,
  username     TEXT    NOT NULL UNIQUE,
  display_name TEXT    NOT NULL,
  password     TEXT    NOT NULL,
  email        TEXT,
  role         TEXT    NOT NULL DEFAULT 'ROLE_VIEWER',
  active       INTEGER NOT NULL DEFAULT 1
);
DROP INDEX IF EXISTS idx_users_email_nocase;

-- ============================================================
-- deactivate_requests（停用帳號請求）
-- ============================================================
CREATE TABLE IF NOT EXISTS deactivate_requests (
  request_id INTEGER PRIMARY KEY,
  user_id    INTEGER NOT NULL REFERENCES users(user_id),
  status     TEXT    NOT NULL DEFAULT 'PENDING',
  created_at TEXT    NOT NULL DEFAULT (datetime('now', 'localtime')),
  reviewed_by INTEGER REFERENCES users(user_id),
  reviewed_at TEXT
);
CREATE INDEX IF NOT EXISTS idx_deactivate_requests_user_id ON deactivate_requests(user_id);
CREATE INDEX IF NOT EXISTS idx_deactivate_requests_status ON deactivate_requests(status);

-- ============================================================
-- identifiers（診斷簽名人，可選關聯系統使用者）
-- ============================================================
CREATE TABLE IF NOT EXISTS identifiers (
  identifier_id INTEGER PRIMARY KEY,
  identifier    TEXT    NOT NULL,
  user_id       INTEGER REFERENCES users(user_id),
  former_user_id INTEGER REFERENCES users(user_id),
  active        INTEGER NOT NULL DEFAULT 1
);

-- ============================================================
-- pest_types（害物類型）
-- ============================================================
CREATE TABLE IF NOT EXISTS pest_types (
  pest_type_id INTEGER PRIMARY KEY,
  pest_type    TEXT    NOT NULL
);

INSERT OR IGNORE INTO pest_types (pest_type_id, pest_type) VALUES
  (1, '病害'),
  (2, '蟲害'),
  (3, '有害動物'),
  (4, '生理因子'),
  (5, '其他');

-- ============================================================
-- pest_categories（病蟲害小分類）
-- ============================================================
CREATE TABLE IF NOT EXISTS pest_categories (
  pest_category_id   INTEGER PRIMARY KEY,
  pest_category_code TEXT    NOT NULL,
  pest_category      TEXT    NOT NULL,
  pest_type_id       INTEGER NOT NULL REFERENCES pest_types(pest_type_id),
  UNIQUE(pest_type_id, pest_category_code),
  UNIQUE(pest_type_id, pest_category)
);

INSERT OR IGNORE INTO pest_categories (pest_category_id, pest_category_code, pest_category, pest_type_id) VALUES
  (1,  'A00', '未知', 1),
  (2,  'A01', '真菌', 1),
  (3,  'A02', '細菌', 1),
  (4,  'A03', '病毒', 1),
  (5,  'A04', '線蟲', 1),
  (6,  'A05', '藻類與高等植物', 1),
  (7,  'A06', '其他病害', 1),
  (8,  'B00', '未知', 2),
  (9,  'B01', '椿象類', 2),
  (10, 'B02', '薊馬類', 2),
  (11, 'B03', '粉蝨類', 2),
  (12, 'B04', '木蝨類', 2),
  (13, 'B05', '飛蝨類', 2),
  (14, 'B06', '介殼蟲類', 2),
  (15, 'B07', '蚜蟲類', 2),
  (16, 'B08', '葉蟬類', 2),
  (17, 'B09', '捲葉蛾類', 2),
  (18, 'B10', '螟蛾類', 2),
  (19, 'B11', '夜蛾類', 2),
  (20, 'B12', '潛葉蛾類', 2),
  (21, 'B13', '毒蛾類', 2),
  (22, 'B14', '其他蛾類', 2),
  (23, 'B15', '蝶類', 2),
  (24, 'B16', '金龜子類', 2),
  (25, 'B17', '天牛類', 2),
  (26, 'B18', '象鼻蟲類', 2),
  (27, 'B19', '金花蟲類', 2),
  (28, 'B20', '果實蠅類', 2),
  (29, 'B21', '潛蠅類', 2),
  (30, 'B22', '蟻類', 2),
  (31, 'B23', '直翅類', 2),
  (32, 'B24', '其他甲蟲類', 2),
  (33, 'B25', '其他雙翅類', 2),
  (34, 'B26', '其他蟲害', 2),
  (35, 'C00', '未知', 3),
  (36, 'C01', '蟎類', 3),
  (37, 'C02', '鳥類', 3),
  (38, 'C03', '鼠類', 3),
  (39, 'C04', '哺乳動物', 3),
  (40, 'C05', '軟體動物', 3),
  (41, 'C06', '其他有害動物', 3),
  (42, 'D00', '未知', 4),
  (43, 'D01', '肥料問題', 4),
  (44, 'D02', '藥害', 4),
  (45, 'D03', '鹽害', 4),
  (46, 'D04', '土壤酸鹼度或電導度問題', 4),
  (47, 'D05', '光照', 4),
  (48, 'D06', '氣候問題', 4),
  (49, 'D07', '污染', 4),
  (50, 'D08', '生長調節劑使用問題', 4),
  (51, 'D09', '草害', 4),
  (52, 'D10', '傷害', 4),
  (53, 'D11', '水分管理', 4),
  (54, 'D12', '其他生理因子', 4),
  (55, 'E00', '未知', 5),
  (56, 'E01', '諮詢', 5),
  (57, 'E02', '資訊索取', 5),
  (58, 'E03', '其他服務', 5);

CREATE INDEX IF NOT EXISTS idx_pest_categories_pest_type_id ON pest_categories(pest_type_id);

-- ============================================================
-- crop_categories（作物分類）
-- ============================================================
CREATE TABLE IF NOT EXISTS crop_categories (
  crop_category_id INTEGER PRIMARY KEY,
  crop_category    TEXT    NOT NULL
);

INSERT OR IGNORE INTO crop_categories (crop_category_id, crop_category) VALUES
  (1, '糧食作物'),
  (2, '雜糧'),
  (3, '特用作物'),
  (4, '蔬菜及瓜果類'),
  (5, '果樹'),
  (6, '花卉及觀賞作物'),
  (7, '雜草'),
  (8, '林木'),
  (9, '其他');

-- ============================================================
-- crops（作物）
-- ============================================================
CREATE TABLE IF NOT EXISTS crops (
  crop_id          INTEGER PRIMARY KEY,
  crop             TEXT    NOT NULL,
  crop_category_id INTEGER NOT NULL REFERENCES crop_categories(crop_category_id),
  UNIQUE(crop_category_id, crop)
);
CREATE INDEX IF NOT EXISTS idx_crops_crop_category_id ON crops(crop_category_id);

-- 業務初始狀態不預建作物：新庫 crops 為空，由各站後續經管理頁或案件內聯建立。

-- ============================================================
-- damages（危害部位）
-- ============================================================
CREATE TABLE IF NOT EXISTS damages (
  damage_id INTEGER PRIMARY KEY,
  damage    TEXT    NOT NULL
);

INSERT OR IGNORE INTO damages (damage_id, damage) VALUES
  (1, '根'),
  (2, '莖'),
  (3, '葉'),
  (4, '花'),
  (5, '果'),
  (6, '全株'),
  (7, '不適用');

-- ============================================================
-- hints（防治建議）
-- ============================================================
CREATE TABLE IF NOT EXISTS hints (
  hint_id INTEGER PRIMARY KEY,
  hint    TEXT    NOT NULL
);

INSERT OR IGNORE INTO hints (hint_id, hint) VALUES
  (1, '耕作防治'),
  (2, '物理防治'),
  (3, '生物防治'),
  (4, '化學防治'),
  (5, '友善資材'),
  (6, '其他回覆');
UPDATE hints SET hint='其他回覆' WHERE hint_id=6 AND hint='其他';

-- ============================================================
-- methods（耕種方式）
-- ============================================================
CREATE TABLE IF NOT EXISTS methods (
  method_id INTEGER PRIMARY KEY,
  method    TEXT    NOT NULL
);

INSERT OR IGNORE INTO methods (method_id, method) VALUES
  (1, '未知'),
  (2, '有機'),
  (3, '非農藥防治'),
  (4, '慣行');

-- ============================================================
-- deliveries（送件方式）
-- ============================================================
CREATE TABLE IF NOT EXISTS deliveries (
  deliver_id INTEGER PRIMARY KEY,
  deliver    TEXT    NOT NULL
);

INSERT OR IGNORE INTO deliveries (deliver_id, deliver) VALUES
  (1, '郵寄'),
  (2, '親送'),
  (3, '電話'),
  (4, '傳真'),
  (5, '現場採樣'),
  (6, '會議諮詢'),
  (7, '轉診'),
  (8, '網路諮詢');

-- ============================================================
-- services（服務類別）
-- ============================================================
CREATE TABLE IF NOT EXISTS services (
  service_id INTEGER PRIMARY KEY,
  service    TEXT    NOT NULL
);

INSERT OR IGNORE INTO services (service_id, service) VALUES
  (1, '診斷'),
  (2, '處理'),
  (3, '諮詢');

-- ============================================================
-- senders（送件人）
-- ============================================================
CREATE TABLE IF NOT EXISTS senders (
  sender_id      INTEGER PRIMARY KEY,
  name           TEXT,
  display_name   TEXT,
  phone          TEXT,
  address        TEXT,
  district_id    INTEGER NOT NULL REFERENCES districts(district_id),
  sender_type_id INTEGER NOT NULL REFERENCES sender_types(sender_type_id)
);
CREATE INDEX IF NOT EXISTS idx_senders_district_id    ON senders(district_id);
CREATE INDEX IF NOT EXISTS idx_senders_sender_type_id ON senders(sender_type_id);
CREATE INDEX IF NOT EXISTS idx_senders_phone ON senders(phone);

-- ============================================================
-- cases（核心案件）
-- ============================================================
CREATE TABLE IF NOT EXISTS cases (
  case_id          INTEGER PRIMARY KEY,
  receive_date     DATE    NOT NULL,
  crop_scale       TEXT,
  damage_scale     TEXT,
  case_description TEXT,
  hint_description TEXT,
  status           INTEGER NOT NULL DEFAULT 0,
  created_at       TEXT    NOT NULL DEFAULT (datetime('now', 'localtime')),
  updated_at       TEXT    NOT NULL DEFAULT (datetime('now', 'localtime')),
  sender_id        INTEGER NOT NULL REFERENCES senders(sender_id),
  method_id        INTEGER NOT NULL REFERENCES methods(method_id),
  crop_id          INTEGER NOT NULL REFERENCES crops(crop_id),
  service_id       INTEGER NOT NULL REFERENCES services(service_id),
  deliver_id       INTEGER NOT NULL REFERENCES deliveries(deliver_id),
  field_district_id INTEGER NOT NULL REFERENCES districts(district_id),
  created_by       INTEGER NOT NULL REFERENCES users(user_id)
);
CREATE INDEX IF NOT EXISTS idx_cases_sender_id      ON cases(sender_id);
CREATE INDEX IF NOT EXISTS idx_cases_field_district_id ON cases(field_district_id);
CREATE INDEX IF NOT EXISTS idx_cases_status         ON cases(status);
CREATE INDEX IF NOT EXISTS idx_cases_receive_date   ON cases(receive_date);

-- ============================================================
-- case_damages（案件 × 危害部位 junction）
-- ============================================================
CREATE TABLE IF NOT EXISTS case_damages (
  cd_id     INTEGER PRIMARY KEY,
  case_id   INTEGER NOT NULL REFERENCES cases(case_id),
  damage_id INTEGER NOT NULL REFERENCES damages(damage_id),
  UNIQUE(case_id, damage_id)
);
CREATE INDEX IF NOT EXISTS idx_case_damages_case_id   ON case_damages(case_id);
CREATE INDEX IF NOT EXISTS idx_case_damages_damage_id ON case_damages(damage_id);

-- ============================================================
-- case_hints（案件 × 防治建議 junction）
-- ============================================================
CREATE TABLE IF NOT EXISTS case_hints (
  ch_id   INTEGER PRIMARY KEY,
  case_id INTEGER NOT NULL REFERENCES cases(case_id),
  hint_id INTEGER NOT NULL REFERENCES hints(hint_id),
  UNIQUE(case_id, hint_id)
);
CREATE INDEX IF NOT EXISTS idx_case_hints_case_id ON case_hints(case_id);
CREATE INDEX IF NOT EXISTS idx_case_hints_hint_id ON case_hints(hint_id);

-- ============================================================
-- case_pest_categories（案件 × 病蟲害小分類 junction）
-- ============================================================
CREATE TABLE IF NOT EXISTS case_pest_categories (
  cpc_id            INTEGER PRIMARY KEY,
  case_id           INTEGER NOT NULL REFERENCES cases(case_id),
  pest_category_id  INTEGER NOT NULL REFERENCES pest_categories(pest_category_id),
  pest_note         TEXT
);
CREATE INDEX IF NOT EXISTS idx_case_pest_categories_case_id   ON case_pest_categories(case_id);
CREATE INDEX IF NOT EXISTS idx_case_pest_categories_pest_id   ON case_pest_categories(pest_category_id);

-- ============================================================
-- case_identifiers（案件 × 診斷簽名人 junction）
-- ============================================================
CREATE TABLE IF NOT EXISTS case_identifiers (
  ci_id         INTEGER PRIMARY KEY,
  case_id       INTEGER NOT NULL REFERENCES cases(case_id),
  identifier_id INTEGER NOT NULL REFERENCES identifiers(identifier_id),
  UNIQUE(case_id, identifier_id)
);
CREATE INDEX IF NOT EXISTS idx_case_identifiers_case_id       ON case_identifiers(case_id);
CREATE INDEX IF NOT EXISTS idx_case_identifiers_identifier_id ON case_identifiers(identifier_id);

-- ============================================================
-- v_case_search（案件篩選視圖，供列表篩選、匯出與 dashboard 共用）
-- 以 LEFT OUTER JOIN 涵蓋可空關聯，多對多以 GROUP_CONCAT 頓號聚合供顯示，
-- 篩選仍以 EXISTS 子查詢精確匹配（見 CaseSpecifications.buildView）
-- ============================================================
DROP VIEW IF EXISTS v_case_search;
CREATE VIEW v_case_search AS
SELECT
  c.case_id,
  c.receive_date,
  c.status,
  c.created_at,
  s.name AS sender_name,
  s.display_name AS sender_display_name,
  s.phone AS sender_phone,
  s.sender_type_id AS sender_type_id,
  fd.district_id AS district_id,
  fd.city_id AS city_id,
  cr.crop_id,
  cc.crop_category_id,
  c.service_id,
  c.deliver_id,
  c.method_id,
  CAST(COUNT(DISTINCT cpc.cpc_id) AS INTEGER) AS pest_category_count,
  CAST(REPLACE(GROUP_CONCAT(DISTINCT pc.pest_category), ',', '、') AS TEXT) AS pest_category_names,
  CAST(REPLACE(GROUP_CONCAT(DISTINCT h.hint), ',', '、') AS TEXT) AS hint_names,
  CAST(REPLACE(GROUP_CONCAT(DISTINCT dm.damage), ',', '、') AS TEXT) AS damage_names
FROM cases c
LEFT JOIN senders s ON s.sender_id = c.sender_id
LEFT JOIN districts fd ON fd.district_id = c.field_district_id
LEFT JOIN crops cr ON cr.crop_id = c.crop_id
LEFT JOIN crop_categories cc ON cc.crop_category_id = cr.crop_category_id
LEFT JOIN case_pest_categories cpc ON cpc.case_id = c.case_id
LEFT JOIN pest_categories pc ON pc.pest_category_id = cpc.pest_category_id
LEFT JOIN case_hints ch ON ch.case_id = c.case_id
LEFT JOIN hints h ON h.hint_id = ch.hint_id
LEFT JOIN case_damages cd ON cd.case_id = c.case_id
LEFT JOIN damages dm ON dm.damage_id = cd.damage_id
GROUP BY c.case_id, c.receive_date, c.status, c.created_at, s.name, s.display_name, s.phone, s.sender_type_id, fd.district_id, fd.city_id, cr.crop_id, cc.crop_category_id, c.service_id, c.deliver_id, c.method_id;
