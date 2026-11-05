#!/usr/bin/env bash
# 心屿 · 本地一键复现脚本
#
# 依次完成：
#   1. 检查 JDK 17+ 与 Android SDK
#   2. 自动补齐 local.properties 的 sdk.dir（不写入任何模型 Key）
#   3. 运行 JVM 单测
#   4. 构建 arm64 debug APK
#   5. 输出 APK 路径、大小与 SHA-256
#
# 用法：
#   ./scripts/reproduce.sh
#   ./scripts/reproduce.sh --offline   # 依赖已缓存时使用
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

OFFLINE=0
for arg in "$@"; do
  case "$arg" in
    --offline) OFFLINE=1 ;;
    -h|--help)
      sed -n '2,16p' "$0"
      exit 0
      ;;
    *)
      echo "未知参数：$arg" >&2
      echo "用法：./scripts/reproduce.sh [--offline]" >&2
      exit 2
      ;;
  esac
done

if ! command -v java >/dev/null 2>&1; then
  echo "✗ 未找到 java，请先安装 JDK 17。" >&2
  exit 1
fi

JAVA_MAJOR="$(java -version 2>&1 | awk -F'"' '/version/ {print $2}' | cut -d. -f1)"
if [[ ! "$JAVA_MAJOR" =~ ^[0-9]+$ ]] || (( JAVA_MAJOR < 17 )); then
  echo "✗ 需要 JDK 17+，当前为：$(java -version 2>&1 | head -1)" >&2
  exit 1
fi
echo "✓ JDK：$(java -version 2>&1 | head -1)"

SDK_DIR=""
if [[ -f local.properties ]]; then
  SDK_DIR="$(awk -F= '/^[[:space:]]*sdk\.dir[[:space:]]*=/{sub(/^[^=]*=/, ""); sub(/\r$/, ""); print; exit}' local.properties)"
fi

if [[ -z "$SDK_DIR" ]]; then
  for candidate in "${ANDROID_HOME:-}" "${ANDROID_SDK_ROOT:-}" "$HOME/Library/Android/sdk" "$HOME/Android/Sdk"; do
    if [[ -n "$candidate" && -d "$candidate" ]]; then
      SDK_DIR="$candidate"
      break
    fi
  done
fi

if [[ -z "$SDK_DIR" || ! -d "$SDK_DIR" ]]; then
  echo "✗ 未找到 Android SDK。" >&2
  echo "  请安装 Android SDK Platform 37 / Build-Tools 36，并在 local.properties 写入：" >&2
  echo "  sdk.dir=/你的/Android/sdk" >&2
  exit 1
fi
echo "✓ Android SDK：$SDK_DIR"

if ! grep -q '^[[:space:]]*sdk\.dir[[:space:]]*=' local.properties 2>/dev/null; then
  printf '\nsdk.dir=%s\n' "$SDK_DIR" >> local.properties
  echo "✓ 已补齐 local.properties 的 sdk.dir"
fi

if [[ ! -x ./gradlew ]]; then
  echo "✗ 未找到可执行的 Gradle Wrapper：./gradlew" >&2
  exit 1
fi

GRADLE_ARGS=(:app:testDebugUnitTest :app:assembleDebug --rerun-tasks)
if (( OFFLINE )); then
  GRADLE_ARGS+=(--offline)
  echo "→ 使用离线依赖缓存"
fi

echo "→ 运行 JVM 单测与 Debug 构建"
./gradlew "${GRADLE_ARGS[@]}"

APK="app/build/outputs/apk/debug/app-arm64-v8a-debug.apk"
if [[ ! -f "$APK" ]]; then
  echo "✗ 构建结束但未找到 APK：$APK" >&2
  exit 1
fi

echo
echo "✓ 复现完成"
echo "  APK：$APK"
ls -lh "$APK"
if command -v shasum >/dev/null 2>&1; then
  shasum -a 256 "$APK"
elif command -v sha256sum >/dev/null 2>&1; then
  sha256sum "$APK"
fi
