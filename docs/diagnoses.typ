#set page(
  margin: (x: 1.5cm, y: 1.5cm),
)

#set text(
  font: ("Noto Serif CJK TC", "Noto Sans CJK TC"),
  size: 9pt,
  lang: "zh",
  region: "tw",
)

#let checkbox = sym.circle
#let checked = sym.circle.filled

#align(center)[
  #text(size: 14pt, weight: "bold")[農作物病蟲害診斷諮詢服務記錄表]\
]

#v(0.5em)

#table(
  columns: (80pt, 1fr, 80pt, 1fr),
  inset: 6pt,
  align: horizon,
  [收件日期\ ], [], [收件編號\ ], [],

  [病蟲害發生地點],
  table.cell(colspan: 3)[
    #checkbox 同寄件人 #h(1em) #checkbox 其他：
  ],

  [送件人\ ],
  table.cell(colspan: 3)[
    #checkbox 1.農民#h(1em) #checkbox 2.農藥商 #h(1em) #checkbox 3.其他
  ],

  [基本資料\ ],
  table.cell(colspan: 3)[
    #grid(
      columns: (50pt, 1fr, 50pt, 1fr),
      gutter: 0.6em,
      [姓名：\ ], [], [電話：], [],
      [住址：], grid.cell(colspan: 3)[],
      [電子信箱：], grid.cell(colspan: 3)[],
    )
  ],

  [耕作方式\ ],
  table.cell(colspan: 3)[
    #checkbox 1.有機 #h(1em) #checkbox 2.非農藥防治 #h(1em) #checkbox 3.慣行
  ],

  [作物種類\ ],
  table.cell(colspan: 3)[
    #grid(
      columns: (1fr, 1fr, 1fr),
      row-gutter: 0.4em,
      [#checkbox 1.糧食作物], [#checkbox 4.蔬菜及瓜果類], [#checkbox 7.雜草],
      [#checkbox 2.雜糧], [#checkbox 5.果樹], [#checkbox 8.林木],
      [#checkbox 3.特用作物], [#checkbox 6.花卉及觀賞作物], [#checkbox 9.其他],
    )
    #v(0.4em)
    作物名稱：#h(1fr)
  ],

  [被害部位\ ],
  table.cell(colspan: 3)[
    #grid(
      columns: (1fr, 1fr, 1fr, 1fr, 1fr, 1fr),
      [#checkbox 1.根], [#checkbox 2.莖], [#checkbox 3.葉], [#checkbox 4.花], [#checkbox 5.果], [#checkbox 6.全株],
    )
    #v(0.4em)
    土壤、栽培、用藥紀錄：\
    \
  ],

  [栽培與被害面積], [], [被害描述\ ], [],

  [服務類別\ ],
  table.cell(colspan: 3)[
    #checkbox 1.診斷 #h(1em) #checkbox 2.處理 #h(1em) #checkbox 3.諮詢
  ],

  [送件方式\ ],
  table.cell(colspan: 3)[
    #grid(
      columns: (1fr, 1fr, 1fr),
      row-gutter: 0.4em,
      [#checkbox 1.郵寄], [#checkbox 4.傳真], [#checkbox 7.轉診],
      [#checkbox 2.自送], [#checkbox 5.現場採樣], [#checkbox 8.Email/FB],
      [#checkbox 3.電話], [#checkbox 6.會議諮詢], [#checkbox 9.Line],
    )
  ],
)

#align(center)[
  #text(size: 11pt, weight: "bold")[診斷結果]
]

#v(0.5em)

#table(
  columns: (80pt, 1fr),
  inset: 6pt,
  align: horizon,
  [鑑定者\ ],
  [
    #checkbox A #h(1em) #checkbox B #h(1em) #checkbox C #h(1em) #checkbox 其它
  ],

  [#checkbox 病害\ ],
  [
    #grid(
      columns: (1fr, 1fr, 1fr),
      row-gutter: 0.4em,
      [#checkbox 1.真菌], [#checkbox 3.病毒], [#checkbox 5.藻類與高等植物],
      [#checkbox 2.細菌], [#checkbox 4.線蟲], [#checkbox 6.其他],
    )
    #v(0.4em)
    病害名稱：#h(1fr)
  ],

  [#checkbox 蟲害\ ],
  [
    #grid(
      columns: (1fr, 1fr),
      row-gutter: 0.4em,
      column-gutter: 1em,
      [#checkbox 1.椿象類], [#checkbox 14.其他蛾類],
      [#checkbox 2.薊馬類], [#checkbox 15.蝶類],
      [#checkbox 3.粉蝨類], [#checkbox 16.金龜子類],
      [#checkbox 4.木蝨類], [#checkbox 17.天牛類],
      [#checkbox 5.飛蝨類], [#checkbox 18.象鼻蟲類],
      [#checkbox 6.介殼蟲類], [#checkbox 19.金花蟲類],
      [#checkbox 7.蚜蟲類], [#checkbox 20.果實蠅類],
      [#checkbox 8.葉蟬類], [#checkbox 21.潛蠅類],
      [#checkbox 9.捲葉蛾類], [#checkbox 22.白蟻類],
      [#checkbox 10.螟蛾類], [#checkbox 23.直翅類],
      [#checkbox 11.夜蛾類], [#checkbox 24.其他甲蟲類],
      [#checkbox 12.潛葉蛾類], [#checkbox 25.其他雙翅類],
      [#checkbox 13.毒蛾類], [#checkbox 26.其他],
    )
    #v(0.4em)
    害蟲名稱 Pest: #h(1fr)
  ],

  [#checkbox 有害動物\ ],
  [
    #grid(
      columns: (1fr, 1fr, 1fr),
      row-gutter: 0.4em,
      [#checkbox 1.蟎類], [#checkbox 3.鼠類], [#checkbox 5.軟體動物],
      [#checkbox 2.鳥類], [#checkbox 4.哺乳動物], [#checkbox 6.其他],
    )
    #v(0.4em)
    有害動物名稱：#h(1fr)
  ],

  [#checkbox 生理因子\ ],
  [
    #grid(
      columns: (1fr, 1fr),
      row-gutter: 0.4em,
      [#checkbox 1.肥料問題], [#checkbox 7.污染],
      [#checkbox 2.藥害], [#checkbox 8.生長調節劑使用問題],
      [#checkbox 3.鹽害], [#checkbox 9.雜草],
      [#checkbox 4.土壤酸鹼度或電導度問題], [#checkbox 10.傷害],
      [#checkbox 5.光照], [#checkbox 11.水分管理問題],
      [#checkbox 6.氣候問題], [#checkbox 12.其他],
    )
    #v(0.4em)
    生理因子：#h(1fr)
  ],

  [#checkbox 其他\ ],
  [
    #grid(
      columns: (1fr, 1fr, 1fr),
      [#checkbox 1.諮詢], [#checkbox 2.資訊索取], [#checkbox 3.其他],
    )
    #v(0.4em)
    描述：#h(1fr)
  ],

  [建議事項\ ],
  [
    #checkbox 1.耕作防治\ #checkbox 2.物理防治\ #checkbox 3.生物防治\
    #checkbox 4.化學防治\ #checkbox 5.友善資材\ #checkbox 6.其他回覆\
    \ \ \ \ \ \
  ],
)
