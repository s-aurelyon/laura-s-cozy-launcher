#!/usr/bin/env bash
# Downloads the three open-source fonts the design uses (Fredoka, Nunito, Fraunces, Caveat)
# into app/src/main/assets/fonts. The app still runs without them, using system fonts.
set -u
cd "$(dirname "$0")/.."
DEST=app/src/main/assets/fonts
mkdir -p "$DEST"
BASE=https://raw.githubusercontent.com/google/fonts/main/ofl

fetch() {
  local url="$1" out="$2"
  if curl -fsSL "$url" -o "$DEST/$out"; then
    echo "  got $out"
  else
    echo "  could not download $out (the app will fall back to system fonts)"
    rm -f "$DEST/$out"
  fi
}

echo "Fetching fonts..."
fetch "$BASE/fredoka/Fredoka%5Bwdth%2Cwght%5D.ttf" fredoka.ttf
fetch "$BASE/nunito/Nunito%5Bwght%5D.ttf" nunito.ttf
fetch "$BASE/fraunces/Fraunces%5BSOFT%2CWONK%2Copsz%2Cwght%5D.ttf" fraunces.ttf
fetch "$BASE/fraunces/Fraunces-Italic%5BSOFT%2CWONK%2Copsz%2Cwght%5D.ttf" fraunces_italic.ttf
fetch "$BASE/caveat/Caveat%5Bwght%5D.ttf" caveat.ttf
exit 0
