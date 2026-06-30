#!/bin/sh
# PBMP Deployment Script
# ----------------------
# This is the real deploy command that runs at PBMP's Deployment stage.
# What it does: builds/packages the project folder into a release and
# "deploys" it to releases/<TASK>/<timestamp>/ (staging -> release).
#
# Usage: sh deploy.sh <projectFolder> <taskId> <releasesDir>
#
# In real production this would be docker build / scp / kubectl / rsync.
# Here it is a safe, local, genuinely working release process.

set -e

PROJECT_FOLDER="$1"
TASK_ID="$2"
RELEASES_DIR="$3"

ROOT_DIR="$(cd "$(dirname "$0")" && pwd)"
SRC="$ROOT_DIR/projects/$PROJECT_FOLDER"

if [ ! -d "$SRC" ]; then
  echo "ERROR: project folder not found: $SRC"
  exit 1
fi

TS="$(date +%Y%m%d-%H%M%S)"
DEST="$RELEASES_DIR/$TASK_ID/$TS"
mkdir -p "$DEST"

echo "==> [1/4] Build: collecting project files..."
# build the artifact, excluding .git and .cursor
ARTIFACT="$DEST/$TASK_ID-$TS.tar.gz"
tar --exclude='.git' --exclude='.cursor' -czf "$ARTIFACT" -C "$ROOT_DIR/projects" "$PROJECT_FOLDER"
echo "    artifact: $ARTIFACT"

echo "==> [2/4] Writing release manifest..."
FILE_COUNT=$(find "$SRC" -type f -not -path '*/.git/*' | wc -l | tr -d ' ')
ARTIFACT_SIZE=$(du -h "$ARTIFACT" | cut -f1)
cat > "$DEST/manifest.json" <<EOF
{
  "task": "$TASK_ID",
  "project": "$PROJECT_FOLDER",
  "releasedAt": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "artifact": "$(basename "$ARTIFACT")",
  "artifactSize": "$ARTIFACT_SIZE",
  "fileCount": $FILE_COUNT,
  "environment": "production"
}
EOF
echo "    manifest: $DEST/manifest.json"

echo "==> [3/4] Pointing the 'current' symlink to the new release..."
CURRENT="$RELEASES_DIR/$TASK_ID/current"
rm -f "$CURRENT"
ln -s "$TS" "$CURRENT"
echo "    current -> $TS"

echo "==> [4/4] Health check..."
if [ -f "$ARTIFACT" ]; then
  echo "    OK: artifact deployed."
else
  echo "    FAIL: artifact was not created."
  exit 1
fi

echo "DEPLOY_SUCCESS releasePath=$DEST"
