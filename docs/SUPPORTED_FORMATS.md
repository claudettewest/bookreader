# Supported formats

| Format | Reading | Search/TTS | Notes |
|---|---|---|---|
| EPUB 2/3 | Reflowable chapters and extracted images | Yes | DRM/encrypted EPUB is unsupported. Complex CSS is simplified. |
| TXT | Chunked reflowable text | Yes | UTF-8 is the initial decoding path. |
| HTML/HTM | Native extracted structure | Yes | No browser or scripts. Remote assets are not fetched. |
| Markdown | Native plain reading structure | Yes | Core headings and emphasis are simplified. |
| RTF | Practical plain-text conversion | Yes | Advanced layout is not preserved. |
| FB2 | Practical XML text conversion | Yes | Advanced styling is not preserved. |
| PDF | Native offline visual mode | When extractable text exists | Scanned/image-only documents do not support TTS; offline OCR is not bundled. |

The parser boundary is format-based so additional local formats can be added without changing library storage or reader navigation.
