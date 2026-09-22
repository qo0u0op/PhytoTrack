# Maintainer: d0w0b <qo0u0op@outlook.com>
# PhytoTrack — 農作物病蟲害診斷記錄系統（Spring Boot + Vue 3 + SQLite）
# AUR: phytotrack（Release tag 對應 GitHub Releases）/ 本地 makepkg 皆可用
# 參考：https://wiki.archlinux.org/title/PKGBUILD / https://wiki.archlinux.org/title/Java_package_guidelines

pkgname=phytotrack
pkgver=0.0.1
pkgrel=2
pkgdesc="農作物病蟲害診斷記錄系統 (Spring Boot 4 + Vue 3 + SQLite)"
arch=('any')
url="https://github.com/qo0u0op/PhytoTrack"
license=('GPL-3.0-only')
depends=('java-runtime>=21' 'hicolor-icon-theme')
makedepends=('java-environment>=21' 'maven' 'nodejs' 'npm')
optdepends=(
  'llama.cpp: 本機 AI 診斷（llama-server --port 11435）'
)
backup=('etc/phytotrack/phytotrack.toml')
install="$pkgname.install"
# Release tarball（tag v$pkgver）；本地遞交前以 `makepkg --printsrcinfo > .SRCINFO` 更新
source=("$pkgname-$pkgver.tar.gz::https://github.com/qo0u0op/PhytoTrack/archive/v$pkgver.tar.gz")
sha256sums=('SKIP')
# 若需從 git 直接打包（開發版），改用下列兩行並執行 `updpkgsums`：
# source=("$pkgname::git+https://github.com/qo0u0op/PhytoTrack.git#tag=v$pkgver")
# sha256sums=('SKIP')

# VCS 版自動 pkgver（僅 git source 時生效，tarball 版忽略）
pkgver() {
  if [[ -d "$pkgname" && -d "$pkgname/.git" ]]; then
    cd "$pkgname"
    # v0.0.1-5-gabc123 → 0.0.1.r5.gabc123
    git describe --long --tags --abbrev=7 2>/dev/null |
      sed 's/^v//;s/\([^-]*-g\)/r\1/;s/-/./g' || echo "$pkgver"
  else
    echo "$pkgver"
  fi
}

prepare() {
  # tarball 解壓後目錄為 PhytoTrack-$pkgver；git source 為 $pkgname
  local srcdir_name
  if [[ -d "$srcdir/PhytoTrack-$pkgver" ]]; then
    srcdir_name="PhytoTrack-$pkgver"
  else
    srcdir_name="$pkgname"
  fi
  cd "$srcdir/$srcdir_name"

  # 前端依賴建議走 npm ci（可重現），此處僅檢查 lock 存在
  if [[ ! -f frontend/package-lock.json ]]; then
    echo "警告：frontend/package-lock.json 不存在，將改用 npm install" >&2
  fi
}

build() {
  local srcdir_name
  if [[ -d "$srcdir/PhytoTrack-$pkgver" ]]; then
    srcdir_name="PhytoTrack-$pkgver"
  else
    srcdir_name="$pkgname"
  fi
  cd "$srcdir/$srcdir_name"

  # 1. 前端建置（含 vue-tsc 型別檢查）
  echo "[PKGBUILD] Frontend build"
  cd frontend
  if [[ -f package-lock.json ]]; then
    npm ci
  else
    npm install
  fi
  npm run build
  # 2. 嵌入後端 static（Spring Boot 靜態資源）
  rm -rf ../backend/src/main/resources/static
  cp -r dist ../backend/src/main/resources/static

  # 3. 後端 Fat Jar（跳過測試以加速打包；如需完整驗證改為 mvn package）
  echo "[PKGBUILD] Backend Fat Jar"
  cd ../backend
  mvn --batch-mode clean package -DskipTests
}

package() {
  local srcdir_name
  if [[ -d "$srcdir/PhytoTrack-$pkgver" ]]; then
    srcdir_name="PhytoTrack-$pkgver"
  else
    srcdir_name="$pkgname"
  fi
  cd "$srcdir/$srcdir_name"

  local jar
  jar=$(ls backend/target/phytotrack-*.jar 2>/dev/null | head -n1)
  if [[ -z "$jar" || ! -f "$jar" ]]; then
    echo "錯誤：找不到 backend/target/phytotrack-*.jar" >&2
    ls -lh backend/target/*.jar 2>&1 | head -20 >&2
    return 1
  fi

  # ——— Jar ———
  install -Dm644 "$jar" "$pkgdir/usr/share/java/$pkgname/$pkgname.jar"
  # 兼容部分腳本預期路徑 /usr/share/phytotrack/
  install -d "$pkgdir/usr/share/$pkgname"
  ln -s "/usr/share/java/$pkgname/$pkgname.jar" "$pkgdir/usr/share/$pkgname/$pkgname.jar"

  # ——— 啟動 wrapper（prod，XDG 路徑由 BinaryPaths 決定） ———
  install -Dm755 /dev/stdin "$pkgdir/usr/bin/$pkgname" <<'WRAPPER'
#!/bin/sh
# PhytoTrack 啟動器 — 委派至系統 JRE，不綁定 JRE（遵循 Arch Java 指引）
# 額外參數透傳；可覆蓋：phytotrack --server.port=9090 / --spring.profiles.active=dev
exec java -Xmx512m -Dfile.encoding=UTF-8 -Dspring.profiles.active=prod -jar /usr/share/java/phytotrack/phytotrack.jar "$@"
WRAPPER

  # ——— 設定檔 ———
  # 範例（唯讀參照）
  install -Dm644 backend/phytotrack.toml.example "$pkgdir/etc/phytotrack/phytotrack.toml.example"
  install -Dm644 backend/phytotrack.toml.example "$pkgdir/usr/share/doc/$pkgname/phytotrack.toml.example"
  # 空的系統級 toml（由首次啟動生成邏輯補齊，不覆蓋使用者已建檔）
  # 僅在 /etc/phytotrack/phytotrack.toml 不存在時由備份機制保留，此處不主動建立
  # 保留 backup 條目以供 pacman 追蹤

  # ——— systemd user service ———
  install -Dm644 /dev/stdin "$pkgdir/usr/lib/systemd/user/$pkgname.service" <<'SERVICE'
[Unit]
Description=PhytoTrack — 農作物病蟲害診斷記錄系統
After=network.target

[Service]
Type=simple
ExecStart=/usr/bin/phytotrack
Restart=on-failure
RestartSec=3
# 日誌由應用內 logback 寫至 $XDG_STATE_HOME/phytotrack/phytotrack.log；同時導向 journal
StandardOutput=journal
StandardError=journal

[Install]
WantedBy=default.target
SERVICE

  # ——— Desktop Entry ———
  install -Dm644 /dev/stdin "$pkgdir/usr/share/applications/$pkgname.desktop" <<'DESKTOP'
[Desktop Entry]
Name=PhytoTrack
GenericName=農作物病蟲害診斷記錄系統
Comment=農作物病蟲害診斷記錄系統
Exec=phytotrack
Icon=phytotrack
Type=Application
Categories=Office;Database;
Terminal=false
StartupWMClass=phytotrack
Keywords=phytotrack;diagnosis;agriculture;
DESKTOP

  # ——— Icons ———
  if [[ -f docs/img/icon.svg ]]; then
    install -Dm644 docs/img/icon.svg "$pkgdir/usr/share/icons/hicolor/scalable/apps/$pkgname.svg"
  fi
  if [[ -f docs/img/icon.png ]]; then
    install -Dm644 docs/img/icon.png "$pkgdir/usr/share/icons/hicolor/512x512/apps/$pkgname.png"
    install -Dm644 docs/img/icon.png "$pkgdir/usr/share/pixmaps/$pkgname.png"
  fi
  if [[ -f docs/img/icon.ico ]]; then
    install -Dm644 docs/img/icon.ico "$pkgdir/usr/share/pixmaps/$pkgname.ico"
  fi

  # ——— 文件與授權 ———
  install -Dm644 LICENSE "$pkgdir/usr/share/licenses/$pkgname/LICENSE"
  install -Dm644 README.md "$pkgdir/usr/share/doc/$pkgname/README.md"
  install -Dm644 docs/DEPLOY.md "$pkgdir/usr/share/doc/$pkgname/DEPLOY.md"
  install -Dm644 docs/ARCHITECTURE.md "$pkgdir/usr/share/doc/$pkgname/ARCHITECTURE.md"
  install -Dm644 backend/phytotrack.toml.example "$pkgdir/usr/share/doc/$pkgname/examples/phytotrack.toml.example"
  if [[ -f docs/img/icon.svg ]]; then
    install -Dm644 docs/img/icon.svg "$pkgdir/usr/share/doc/$pkgname/icon.svg"
  fi
}
