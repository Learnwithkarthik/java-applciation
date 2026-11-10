#!/usr/bin/env bash
# Render a temporary overlay without editing the committed base or env overlay.
# Usage: bash k8s/scripts/render.sh dev REGION-docker.pkg.dev/PROJECT/REPO/IMAGE TAG
set -euo pipefail

if [[ $# -ne 3 || ( "$1" != "dev" && "$1" != "prod" ) ]]; then
  echo "Usage: bash k8s/scripts/render.sh {dev|prod} IMAGE_REPOSITORY IMAGE_TAG" >&2
  exit 2
fi

target="$1"
image_repo="$2"
image_tag="$3"

if [[ ! "$image_repo" =~ ^[A-Za-z0-9._/-]+$ ]]; then
  echo "Invalid image repository: $image_repo" >&2
  exit 2
fi
if [[ ! "$image_tag" =~ ^[A-Za-z0-9._-]+$ ]]; then
  echo "Invalid image tag: $image_tag" >&2
  exit 2
fi

k8s_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
runtime_dir="${k8s_dir}/runtime/${target}"
mkdir -p "$runtime_dir"

cat > "${runtime_dir}/kustomization.yaml" <<EOF
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization
resources:
  - ../../overlays/${target}
images:
  - name: globalshop-app
    newName: ${image_repo}
    newTag: ${image_tag}
EOF

echo "${runtime_dir}"
