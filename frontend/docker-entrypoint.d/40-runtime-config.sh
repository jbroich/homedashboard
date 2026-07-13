#!/bin/sh
set -eu

api_base_url="${API_BASE_URL:-}"

escape_json() {
  printf '%s' "$1" | sed 's/\\/\\\\/g; s/"/\\"/g'
}

escaped_api_base_url="$(escape_json "$api_base_url")"

cat > /usr/share/nginx/html/runtime-config.js <<EOF
window.__HOMEDASHBOARD_CONFIG__ = {
  apiBaseUrl: "${escaped_api_base_url}"
};
EOF

for index_file in /usr/share/nginx/html/index.html /usr/share/nginx/html/*/index.html; do
  [ -f "$index_file" ] || continue

  if ! grep -q 'runtime-config.js' "$index_file"; then
    sed -i 's#</head>#<script src="/runtime-config.js"></script></head>#' "$index_file"
  fi
done
