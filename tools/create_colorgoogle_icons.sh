#!/bin/bash
# Package the edited artwork with a white background; Android adds adaptive padding.
set -euo pipefail
project_dir="$(cd "$(dirname "$0")/.." && pwd)"
res_dir="$project_dir/app/src/main/res"
source_png="$project_dir/artwork/colorgoogle-spaced-master.png"
temp_dir="$(mktemp -d)"
trap 'rm -rf "$temp_dir"' EXIT
encoded_png="$(base64 < "$source_png" | tr -d '\n')"
cat > "$temp_dir/icon.svg" <<EOF
<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="1254" height="1254" viewBox="0 0 1254 1254">
<rect width="1254" height="1254" fill="white"/>
<image width="1254" height="1254" xlink:href="data:image/png;base64,$encoded_png"/>
</svg>
EOF

resize_icon() {
    rsvg-convert -w "$1" -h "$1" -o "$2" "$temp_dir/icon.svg"
}

/usr/bin/sips --resampleHeightWidth 432 432 "$source_png" --out "$res_dir/drawable-nodpi/colorgoogle_icon.png" >/dev/null
resize_icon 432 "$res_dir/drawable-nodpi/ic_launcher_v72.png"
resize_icon 512 "$project_dir/artwork/colorgoogle-preview.png"

for item in mdpi:48 hdpi:72 xhdpi:96 xxhdpi:144 xxxhdpi:192; do
    density="${item%:*}"
    size="${item#*:}"
    resize_icon "$size" "$res_dir/mipmap-$density/ic_launcher.png"
    cp "$res_dir/mipmap-$density/ic_launcher.png" "$res_dir/mipmap-$density/ic_launcher_round.png"
done
