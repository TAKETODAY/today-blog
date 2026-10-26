#!/usr/bin/env bash
# 构建完整签名包。BUILD_NUMBER 应来自 CI 单调递增编号（默认 UTC 时间戳）。
set -Eeuo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT"
: "${BLOG_UPGRADE_PRIVATE_KEY:?必须指定构建机上的 RSA 私钥}"
: "${ROLLBACK_SAFE:?确认数据库迁移向后兼容后设置 ROLLBACK_SAFE=true}"
[[ "$ROLLBACK_SAFE" == true ]] || { echo '不可逆迁移请人工部署'; exit 1; }
VERSION="${VERSION:-$(sed -n 's/^version=//p' gradle.properties | head -1)}"
BUILD_NUMBER="${BUILD_NUMBER:-$(date -u +%Y%m%d%H%M%S)}"
[[ "$VERSION" =~ ^[A-Za-z0-9][A-Za-z0-9._-]{0,99}$ && "$BUILD_NUMBER" =~ ^[1-9][0-9]{0,17}$ ]] || exit 1
./gradlew :blog-api:installDist :blog-updater:jar
export NODE_OPTIONS="${NODE_OPTIONS:-} --openssl-legacy-provider"
pnpm --dir blog-frontend install --frozen-lockfile
pnpm --dir blog-frontend/console build
pnpm --dir blog-frontend/desktop build
mkdir -p dist
WORK="$(mktemp -d "$ROOT/dist/.package-XXXXXX")"
trap 'rm -rf "$WORK"' EXIT
mkdir -p "$WORK/today-blog/api" "$WORK/today-blog/META-INF"
cp -R blog-api/build/install/blog-api/bin blog-api/build/install/blog-api/lib "$WORK/today-blog/api/"
cp -R blog-frontend/console/dist "$WORK/today-blog/console"
cp -R blog-frontend/desktop/build "$WORK/today-blog/desktop"
python3 - "$WORK/today-blog" "$VERSION" "$BUILD_NUMBER" <<'PY'
import hashlib, pathlib, sys
root = pathlib.Path(sys.argv[1])
lines = ['format=1', 'product=today-blog', 'version='+sys.argv[2],
         'build='+sys.argv[3], 'minimumJava=17', 'rollbackSafe=true']
for path in sorted(root.rglob('*')):
    if path.is_symlink():
        raise SystemExit('发行包不得包含符号链接: '+str(path))
    if path.is_file():
        lines.append('sha256.'+path.relative_to(root).as_posix()+'='+hashlib.sha256(path.read_bytes()).hexdigest())
(root/'META-INF/upgrade.properties').write_text('\n'.join(lines)+'\n', encoding='ascii')
PY
openssl dgst -sha256 -sign "$BLOG_UPGRADE_PRIVATE_KEY" -out "$WORK/signature" "$WORK/today-blog/META-INF/upgrade.properties"
openssl base64 -A -in "$WORK/signature" -out "$WORK/today-blog/META-INF/upgrade.sig"
python3 - "$WORK" "$ROOT/dist/today-blog-$VERSION-$BUILD_NUMBER.zip" <<'PY'
import pathlib, sys, zipfile
root = pathlib.Path(sys.argv[1])
with zipfile.ZipFile(sys.argv[2], 'w', zipfile.ZIP_DEFLATED) as archive:
    for path in sorted((root/'today-blog').rglob('*')):
        if path.is_file(): archive.write(path, path.relative_to(root))
PY
cp blog-updater/build/libs/today-blog-updater.jar dist/today-blog-updater.jar
echo "发行包: dist/today-blog-$VERSION-$BUILD_NUMBER.zip"
