-- ============================================================
-- schema.sql — PhytoTrack 完整資料表定義 + 初始種子資料
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
  city       TEXT    NOT NULL,
  sort_order INTEGER NOT NULL DEFAULT 0
);

INSERT OR IGNORE INTO cities (city_id, city, sort_order) VALUES
  (1,  '臺北市', 1),
  (2,  '臺中市', 2),
  (3,  '基隆市', 3),
  (4,  '臺南市', 4),
  (5,  '高雄市', 5),
  (6,  '新北市', 6),
  (7,  '宜蘭縣', 7),
  (8,  '桃園市', 8),
  (9,  '嘉義市', 9),
  (10, '新竹縣', 10),
  (11, '苗栗縣', 11),
  (12, '南投縣', 12),
  (13, '彰化縣', 13),
  (14, '新竹市', 14),
  (15, '雲林縣', 15),
  (16, '嘉義縣', 16),
  (17, '屏東縣', 17),
  (18, '花蓮縣', 18),
  (19, '臺東縣', 19),
  (20, '金門縣', 20),
  (21, '澎湖縣', 21),
  (22, '連江縣', 22),
  (23, '未知', 23);
CREATE INDEX IF NOT EXISTS idx_cities_sort ON cities(sort_order);

-- ============================================================
-- districts（鄉鎮市區）
-- ============================================================
CREATE TABLE IF NOT EXISTS districts (
  district_id INTEGER PRIMARY KEY,
  district    TEXT    NOT NULL,
  city_id     INTEGER NOT NULL REFERENCES cities(city_id),
  sort_order  INTEGER NOT NULL DEFAULT 0
);

INSERT OR IGNORE INTO districts (district_id, district, city_id, sort_order) VALUES
  (1,   '中正區', 1, 1),
  (2,   '大同區', 1, 2),
  (3,   '中山區', 1, 3),
  (4,   '松山區', 1, 4),
  (5,   '大安區', 1, 5),
  (6,   '萬華區', 1, 6),
  (7,   '信義區', 1, 7),
  (8,   '士林區', 1, 8),
  (9,   '北投區', 1, 9),
  (10,  '內湖區', 1, 10),
  (11,  '南港區', 1, 11),
  (12,  '文山區', 1, 12),
  (13,  '未知', 1, 13),
  (14,  '中區', 2, 1),
  (15,  '東區', 2, 2),
  (16,  '南區', 2, 3),
  (17,  '西區', 2, 4),
  (18,  '北區', 2, 5),
  (19,  '北屯區', 2, 6),
  (20,  '西屯區', 2, 7),
  (21,  '南屯區', 2, 8),
  (22,  '太平區', 2, 9),
  (23,  '大里區', 2, 10),
  (24,  '霧峰區', 2, 11),
  (25,  '烏日區', 2, 12),
  (26,  '豐原區', 2, 13),
  (27,  '后里區', 2, 14),
  (28,  '石岡區', 2, 15),
  (29,  '東勢區', 2, 16),
  (30,  '和平區', 2, 17),
  (31,  '新社區', 2, 18),
  (32,  '潭子區', 2, 19),
  (33,  '大雅區', 2, 20),
  (34,  '神岡區', 2, 21),
  (35,  '大肚區', 2, 22),
  (36,  '沙鹿區', 2, 23),
  (37,  '龍井區', 2, 24),
  (38,  '梧棲區', 2, 25),
  (39,  '清水區', 2, 26),
  (40,  '大甲區', 2, 27),
  (41,  '外埔區', 2, 28),
  (42,  '大安區', 2, 29),
  (43,  '未知', 2, 30),
  (44,  '仁愛區', 3, 1),
  (45,  '信義區', 3, 2),
  (46,  '中正區', 3, 3),
  (47,  '中山區', 3, 4),
  (48,  '安樂區', 3, 5),
  (49,  '暖暖區', 3, 6),
  (50,  '七堵區', 3, 7),
  (51,  '未知', 3, 8),
  (52,  '中西區', 4, 1),
  (53,  '東區', 4, 2),
  (54,  '南區', 4, 3),
  (55,  '北區', 4, 4),
  (56,  '安平區', 4, 5),
  (57,  '安南區', 4, 6),
  (58,  '永康區', 4, 7),
  (59,  '歸仁區', 4, 8),
  (60,  '新化區', 4, 9),
  (61,  '左鎮區', 4, 10),
  (62,  '玉井區', 4, 11),
  (63,  '楠西區', 4, 12),
  (64,  '南化區', 4, 13),
  (65,  '仁德區', 4, 14),
  (66,  '關廟區', 4, 15),
  (67,  '龍崎區', 4, 16),
  (68,  '官田區', 4, 17),
  (69,  '麻豆區', 4, 18),
  (70,  '佳里區', 4, 19),
  (71,  '西港區', 4, 20),
  (72,  '七股區', 4, 21),
  (73,  '將軍區', 4, 22),
  (74,  '學甲區', 4, 23),
  (75,  '北門區', 4, 24),
  (76,  '新營區', 4, 25),
  (77,  '後壁區', 4, 26),
  (78,  '白河區', 4, 27),
  (79,  '東山區', 4, 28),
  (80,  '六甲區', 4, 29),
  (81,  '下營區', 4, 30),
  (82,  '柳營區', 4, 31),
  (83,  '鹽水區', 4, 32),
  (84,  '善化區', 4, 33),
  (85,  '大內區', 4, 34),
  (86,  '山上區', 4, 35),
  (87,  '新市區', 4, 36),
  (88,  '安定區', 4, 37),
  (89,  '未知', 4, 38),
  (90,  '新興區', 5, 1),
  (91,  '前金區', 5, 2),
  (92,  '苓雅區', 5, 3),
  (93,  '鹽埕區', 5, 4),
  (94,  '鼓山區', 5, 5),
  (95,  '旗津區', 5, 6),
  (96,  '前鎮區', 5, 7),
  (97,  '三民區', 5, 8),
  (98,  '楠梓區', 5, 9),
  (99,  '小港區', 5, 10),
  (100, '左營區', 5, 11),
  (101, '仁武區', 5, 12),
  (102, '大社區', 5, 13),
  (103, '岡山區', 5, 14),
  (104, '路竹區', 5, 15),
  (105, '阿蓮區', 5, 16),
  (106, '田寮區', 5, 17),
  (107, '燕巢區', 5, 18),
  (108, '橋頭區', 5, 19),
  (109, '梓官區', 5, 20),
  (110, '彌陀區', 5, 21),
  (111, '永安區', 5, 22),
  (112, '湖內區', 5, 23),
  (113, '鳳山區', 5, 24),
  (114, '大寮區', 5, 25),
  (115, '林園區', 5, 26),
  (116, '鳥松區', 5, 27),
  (117, '大樹區', 5, 28),
  (118, '旗山區', 5, 29),
  (119, '美濃區', 5, 30),
  (120, '六龜區', 5, 31),
  (121, '內門區', 5, 32),
  (122, '杉林區', 5, 33),
  (123, '甲仙區', 5, 34),
  (124, '桃源區', 5, 35),
  (125, '那瑪夏區', 5, 36),
  (126, '茂林區', 5, 37),
  (127, '茄萣區', 5, 38),
  (128, '未知', 5, 39),
  (129, '萬里區', 6, 1),
  (130, '金山區', 6, 2),
  (131, '板橋區', 6, 3),
  (132, '汐止區', 6, 4),
  (133, '深坑區', 6, 5),
  (134, '石碇區', 6, 6),
  (135, '瑞芳區', 6, 7),
  (136, '平溪區', 6, 8),
  (137, '雙溪區', 6, 9),
  (138, '貢寮區', 6, 10),
  (139, '新店區', 6, 11),
  (140, '坪林區', 6, 12),
  (141, '烏來區', 6, 13),
  (142, '永和區', 6, 14),
  (143, '中和區', 6, 15),
  (144, '土城區', 6, 16),
  (145, '三峽區', 6, 17),
  (146, '樹林區', 6, 18),
  (147, '鶯歌區', 6, 19),
  (148, '三重區', 6, 20),
  (149, '新莊區', 6, 21),
  (150, '泰山區', 6, 22),
  (151, '林口區', 6, 23),
  (152, '蘆洲區', 6, 24),
  (153, '五股區', 6, 25),
  (154, '八里區', 6, 26),
  (155, '淡水區', 6, 27),
  (156, '三芝區', 6, 28),
  (157, '石門區', 6, 29),
  (158, '未知', 6, 30),
  (159, '宜蘭市', 7, 1),
  (160, '頭城鎮', 7, 2),
  (161, '礁溪鄉', 7, 3),
  (162, '壯圍鄉', 7, 4),
  (163, '員山鄉', 7, 5),
  (164, '羅東鎮', 7, 6),
  (165, '三星鄉', 7, 7),
  (166, '大同鄉', 7, 8),
  (167, '五結鄉', 7, 9),
  (168, '冬山鄉', 7, 10),
  (169, '蘇澳鎮', 7, 11),
  (170, '南澳鄉', 7, 12),
  (171, '未知', 7, 13),
  (172, '中壢區', 8, 1),
  (173, '平鎮區', 8, 2),
  (174, '龍潭區', 8, 3),
  (175, '楊梅區', 8, 4),
  (176, '新屋區', 8, 5),
  (177, '觀音區', 8, 6),
  (178, '桃園區', 8, 7),
  (179, '龜山區', 8, 8),
  (180, '八德區', 8, 9),
  (181, '大溪區', 8, 10),
  (182, '復興區', 8, 11),
  (183, '大園區', 8, 12),
  (184, '蘆竹區', 8, 13),
  (185, '未知', 8, 14),
  (186, '西區', 9, 1),
  (187, '東區', 9, 2),
  (188, '未知', 9, 3),
  (189, '竹北市', 10, 1),
  (190, '湖口鄉', 10, 2),
  (191, '新豐鄉', 10, 3),
  (192, '新埔鎮', 10, 4),
  (193, '關西鎮', 10, 5),
  (194, '芎林鄉', 10, 6),
  (195, '寶山鄉', 10, 7),
  (196, '竹東鎮', 10, 8),
  (197, '五峰鄉', 10, 9),
  (198, '橫山鄉', 10, 10),
  (199, '尖石鄉', 10, 11),
  (200, '北埔鄉', 10, 12),
  (201, '峨嵋鄉', 10, 13),
  (202, '未知', 10, 14),
  (203, '竹南鎮', 11, 1),
  (204, '頭份市', 11, 2),
  (205, '三灣鄉', 11, 3),
  (206, '南庄鄉', 11, 4),
  (207, '獅潭鄉', 11, 5),
  (208, '後龍鎮', 11, 6),
  (209, '通霄鎮', 11, 7),
  (210, '苑裡鎮', 11, 8),
  (211, '苗栗市', 11, 9),
  (212, '造橋鄉', 11, 10),
  (213, '頭屋鄉', 11, 11),
  (214, '公館鄉', 11, 12),
  (215, '大湖鄉', 11, 13),
  (216, '泰安鄉', 11, 14),
  (217, '銅鑼鄉', 11, 15),
  (218, '三義鄉', 11, 16),
  (219, '西湖鄉', 11, 17),
  (220, '卓蘭鎮', 11, 18),
  (221, '未知', 11, 19),
  (222, '南投市', 12, 1),
  (223, '中寮鄉', 12, 2),
  (224, '草屯鎮', 12, 3),
  (225, '國姓鄉', 12, 4),
  (226, '埔里鎮', 12, 5),
  (227, '仁愛鄉', 12, 6),
  (228, '名間鄉', 12, 7),
  (229, '集集鎮', 12, 8),
  (230, '水里鄉', 12, 9),
  (231, '魚池鄉', 12, 10),
  (232, '信義鄉', 12, 11),
  (233, '竹山鎮', 12, 12),
  (234, '鹿谷鄉', 12, 13),
  (235, '未知', 12, 14),
  (236, '彰化市', 13, 1),
  (237, '芬園鄉', 13, 2),
  (238, '花壇鄉', 13, 3),
  (239, '秀水鄉', 13, 4),
  (240, '鹿港鎮', 13, 5),
  (241, '福興鄉', 13, 6),
  (242, '線西鄉', 13, 7),
  (243, '和美鎮', 13, 8),
  (244, '伸港鄉', 13, 9),
  (245, '員林市', 13, 10),
  (246, '社頭鄉', 13, 11),
  (247, '永靖鄉', 13, 12),
  (248, '埔心鄉', 13, 13),
  (249, '溪湖鎮', 13, 14),
  (250, '大村鄉', 13, 15),
  (251, '埔鹽鄉', 13, 16),
  (252, '田中鎮', 13, 17),
  (253, '北斗鎮', 13, 18),
  (254, '田尾鄉', 13, 19),
  (255, '埤頭鄉', 13, 20),
  (256, '溪州鄉', 13, 21),
  (257, '竹塘鄉', 13, 22),
  (258, '二林鎮', 13, 23),
  (259, '大城鄉', 13, 24),
  (260, '芳苑鄉', 13, 25),
  (261, '二水鄉', 13, 26),
  (262, '未知', 13, 27),
  (263, '東區', 14, 1),
  (264, '北區', 14, 2),
  (265, '香山區', 14, 3),
  (266, '未知', 14, 4),
  (267, '斗南鎮', 15, 1),
  (268, '大埤鄉', 15, 2),
  (269, '虎尾鎮', 15, 3),
  (270, '土庫鎮', 15, 4),
  (271, '褒忠鄉', 15, 5),
  (272, '東勢鄉', 15, 6),
  (273, '臺西鄉', 15, 7),
  (274, '崙背鄉', 15, 8),
  (275, '麥寮鄉', 15, 9),
  (276, '斗六市', 15, 10),
  (277, '林內鄉', 15, 11),
  (278, '古坑鄉', 15, 12),
  (279, '莿桐鄉', 15, 13),
  (280, '西螺鎮', 15, 14),
  (281, '二崙鄉', 15, 15),
  (282, '北港鎮', 15, 16),
  (283, '水林鄉', 15, 17),
  (284, '口湖鄉', 15, 18),
  (285, '四湖鄉', 15, 19),
  (286, '元長鄉', 15, 20),
  (287, '未知', 15, 21),
  (288, '番路鄉', 16, 1),
  (289, '梅山鄉', 16, 2),
  (290, '竹崎鄉', 16, 3),
  (291, '阿里山鄉', 16, 4),
  (292, '中埔鄉', 16, 5),
  (293, '大埔鄉', 16, 6),
  (294, '水上鄉', 16, 7),
  (295, '鹿草鄉', 16, 8),
  (296, '太保市', 16, 9),
  (297, '朴子市', 16, 10),
  (298, '東石鄉', 16, 11),
  (299, '六腳鄉', 16, 12),
  (300, '新港鄉', 16, 13),
  (301, '民雄鄉', 16, 14),
  (302, '大林鎮', 16, 15),
  (303, '溪口鄉', 16, 16),
  (304, '義竹鄉', 16, 17),
  (305, '布袋鎮', 16, 18),
  (306, '未知', 16, 19),
  (307, '屏東市', 17, 1),
  (308, '三地門鄉', 17, 2),
  (309, '霧臺鄉', 17, 3),
  (310, '瑪家鄉', 17, 4),
  (311, '九如鄉', 17, 5),
  (312, '里港鄉', 17, 6),
  (313, '高樹鄉', 17, 7),
  (314, '盬埔鄉', 17, 8),
  (315, '長治鄉', 17, 9),
  (316, '麟洛鄉', 17, 10),
  (317, '竹田鄉', 17, 11),
  (318, '內埔鄉', 17, 12),
  (319, '萬丹鄉', 17, 13),
  (320, '潮州鎮', 17, 14),
  (321, '泰武鄉', 17, 15),
  (322, '來義鄉', 17, 16),
  (323, '萬巒鄉', 17, 17),
  (324, '崁頂鄉', 17, 18),
  (325, '新埤鄉', 17, 19),
  (326, '南州鄉', 17, 20),
  (327, '林邊鄉', 17, 21),
  (328, '東港鎮', 17, 22),
  (329, '琉球鄉', 17, 23),
  (330, '佳冬鄉', 17, 24),
  (331, '新園鄉', 17, 25),
  (332, '枋寮鄉', 17, 26),
  (333, '枋山鄉', 17, 27),
  (334, '春日鄉', 17, 28),
  (335, '獅子鄉', 17, 29),
  (336, '車城鄉', 17, 30),
  (337, '牡丹鄉', 17, 31),
  (338, '恆春鎮', 17, 32),
  (339, '滿州鄉', 17, 33),
  (340, '未知', 17, 34),
  (341, '花蓮市', 18, 1),
  (342, '新城鄉', 18, 2),
  (343, '秀林鄉', 18, 3),
  (344, '吉安鄉', 18, 4),
  (345, '壽豐鄉', 18, 5),
  (346, '鳳林鎮', 18, 6),
  (347, '光復鄉', 18, 7),
  (348, '豐濱鄉', 18, 8),
  (349, '瑞穗鄉', 18, 9),
  (350, '萬榮鄉', 18, 10),
  (351, '玉里鎮', 18, 11),
  (352, '卓溪鄉', 18, 12),
  (353, '富里鄉', 18, 13),
  (354, '未知', 18, 14),
  (355, '臺東市', 19, 1),
  (356, '綠島鄉', 19, 2),
  (357, '蘭嶼鄉', 19, 3),
  (358, '延平鄉', 19, 4),
  (359, '卑南鄉', 19, 5),
  (360, '鹿野鄉', 19, 6),
  (361, '關山鎮', 19, 7),
  (362, '海端鄉', 19, 8),
  (363, '池上鄉', 19, 9),
  (364, '東河鄉', 19, 10),
  (365, '成功鎮', 19, 11),
  (366, '長濱鄉', 19, 12),
  (367, '太麻里鄉', 19, 13),
  (368, '金峰鄉', 19, 14),
  (369, '大武鄉', 19, 15),
  (370, '達仁鄉', 19, 16),
  (371, '未知', 19, 17),
  (372, '金沙鎮', 20, 1),
  (373, '金湖鎮', 20, 2),
  (374, '金寧鄉', 20, 3),
  (375, '金城鎮', 20, 4),
  (376, '烈嶼鄉', 20, 5),
  (377, '烏坵鄉', 20, 6),
  (378, '未知', 20, 7),
  (379, '馬公市', 21, 1),
  (380, '西嶼鄉', 21, 2),
  (381, '望安鄉', 21, 3),
  (382, '七美鄉', 21, 4),
  (383, '白沙鄉', 21, 5),
  (384, '湖西鄉', 21, 6),
  (385, '未知', 21, 7),
  (386, '南竿鄉', 22, 1),
  (387, '北竿鄉', 22, 2),
  (388, '莒光鄉', 22, 3),
  (389, '東引鄉', 22, 4),
  (390, '未知', 22, 5),
  (391, '未知', 23, 1);
CREATE INDEX IF NOT EXISTS idx_districts_city_id ON districts(city_id);
CREATE INDEX IF NOT EXISTS idx_districts_sort   ON districts(city_id, sort_order);

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
  user_id       INTEGER REFERENCES users(user_id)
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
  (1,  'A01', '真菌', 1),
  (2,  'A02', '細菌', 1),
  (3,  'A03', '病毒', 1),
  (4,  'A04', '線蟲', 1),
  (5,  'A05', '藻類與高等植物', 1),
  (6,  'A06', '其他病害', 1),
  (7,  'A00', '未知', 1),
  (8,  'B01', '椿象類', 2),
  (9,  'B02', '薊馬類', 2),
  (10, 'B03', '粉蝨類', 2),
  (11, 'B04', '木蝨類', 2),
  (12, 'B05', '飛蝨類', 2),
  (13, 'B06', '介殼蟲類', 2),
  (14, 'B07', '蚜蟲類', 2),
  (15, 'B08', '葉蟬類', 2),
  (16, 'B09', '捲葉蛾類', 2),
  (17, 'B10', '螟蛾類', 2),
  (18, 'B11', '夜蛾類', 2),
  (19, 'B12', '潛葉蛾類', 2),
  (20, 'B13', '毒蛾類', 2),
  (21, 'B14', '其他蛾類', 2),
  (22, 'B15', '蝶類', 2),
  (23, 'B16', '金龜子類', 2),
  (24, 'B17', '天牛類', 2),
  (25, 'B18', '象鼻蟲類', 2),
  (26, 'B19', '金花蟲類', 2),
  (27, 'B20', '果實蠅類', 2),
  (28, 'B21', '潛蠅類', 2),
  (29, 'B22', '蟻類', 2),
  (30, 'B23', '直翅類', 2),
  (31, 'B24', '其他甲蟲類', 2),
  (32, 'B25', '其他雙翅類', 2),
  (33, 'B26', '其他蟲害', 2),
  (34, 'B00', '未知', 2),
  (35, 'C01', '蟎類', 3),
  (36, 'C02', '鳥類', 3),
  (37, 'C03', '鼠類', 3),
  (38, 'C04', '哺乳動物', 3),
  (39, 'C05', '軟體動物', 3),
  (40, 'C06', '其他有害動物', 3),
  (41, 'C00', '未知', 3),
  (42, 'D01', '肥料問題', 4),
  (43, 'D02', '藥害', 4),
  (44, 'D03', '鹽害', 4),
  (45, 'D04', '土壤酸鹼度或電導度問題', 4),
  (46, 'D05', '光照', 4),
  (47, 'D06', '氣候問題', 4),
  (48, 'D07', '污染', 4),
  (49, 'D08', '生長調節劑使用問題', 4),
  (50, 'D09', '草害', 4),
  (51, 'D10', '傷害', 4),
  (52, 'D11', '水分管理', 4),
  (53, 'D12', '其他生理因子', 4),
  (54, 'D00', '未知', 4),
  (55, 'E01', '諮詢', 5),
  (56, 'E02', '資訊索取', 5),
  (57, 'E03', '其他服務', 5),
  (58, 'E00', '未知', 5);
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

INSERT OR IGNORE INTO crops (crop_id, crop, crop_category_id) VALUES
  -- 糧食作物
  (1,  '水稻', 1),
  -- 雜糧
  (2,  '甘藷', 2),
  (3,  '落花生', 2),
  (4,  '玉米', 2),
  (5,  '大豆', 2),
  (6,  '小麥', 2),
  -- 特用作物
  (7,  '茶', 3),
  (8,  '甘蔗', 3),
  (9,  '香茅', 3),
  -- 蔬菜及瓜果類
  (10, '高麗菜', 4),
  (11, '小白菜', 4),
  (12, '青蔥', 4),
  (13, '番茄', 4),
  (14, '茄子', 4),
  (15, '青椒', 4),
  (16, '小黃瓜', 4),
  (17, '南瓜', 4),
  (18, '絲瓜', 4),
  (19, '苦瓜', 4),
  (20, '冬瓜', 4),
  (21, '四季豆', 4),
  (22, '甘藍', 4),
  (23, '花椰菜', 4),
  (24, '白蘿蔔', 4),
  (25, '胡蘿蔔', 4),
  (26, '洋蔥', 4),
  (27, '芹菜', 4),
  (28, '空心菜', 4),
  (29, '莧菜', 4),
  (30, '萵苣', 4),
  (31, '菠菜', 4),
  (32, '韭菜', 4),
  (33, '蘆筍', 4),
  (34, '馬鈴薯', 4),
  (35, '芋頭', 4),
  -- 果樹
  (36, '柑橘', 5),
  (37, '柳橙', 5),
  (38, '檸檬', 5),
  (39, '芒果', 5),
  (40, '荔枝', 5),
  (41, '龍眼', 5),
  (42, '蓮霧', 5),
  (43, '香蕉', 5),
  (44, '鳳梨', 5),
  (45, '木瓜', 5),
  (46, '番石榴', 5),
  (47, '葡萄', 5),
  (48, '梨', 5),
  (49, '水蜜桃', 5),
  (50, '柿子', 5),
  (51, '棗', 5),
  (52, '梅', 5),
  (53, '李', 5),
  (54, '枇杷', 5),
  (55, '楊桃', 5),
  (56, '釋迦', 5),
  (57, '百香果', 5),
  -- 花卉及觀賞作物
  (58, '玫瑰', 6),
  (59, '菊花', 6),
  (60, '百合', 6),
  (61, '蝴蝶蘭', 6),
  (62, '文心蘭', 6),
  (63, '火鶴花', 6),
  (64, '康乃馨', 6),
  -- 雜草 / 林木 / 其他
  (65, '一般雜草', 7),
  (66, '榕樹', 8),
  (67, '樟樹', 8),
  (68, '其他', 9);

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
  (1, '有機'),
  (2, '非農藥防治'),
  (3, '慣行'),
  (4, '未知');

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
  address        TEXT    NOT NULL,
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
