Get-ChildItem *.png | Where-Object { $_.Name -ne "app_banner.png" } | ForEach-Object {
    ffmpeg -i $_.FullName -vcodec libwebp -lossless 0 -qscale 80 -vf "scale=300:300" -map_metadata -1 "$($_.BaseName).webp"
}
