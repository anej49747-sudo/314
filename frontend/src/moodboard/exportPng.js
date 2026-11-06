import { CANVAS_HEIGHT, CANVAS_WIDTH } from './canvasRules.js'

function loadImage(source) {
  return new Promise((resolve, reject) => {
    const image = new Image()
    image.onload = () => resolve(image)
    image.onerror = () => reject(new Error('Image could not be loaded'))
    image.src = source
  })
}
function drawText(context, text, x, y, width, fontSize) {
  const lineHeight = fontSize * 1.24
  let row = y
  for (const paragraph of String(text || '').split('\n')) {
    let line = ''
    for (const word of paragraph.split(/\s+/)) {
      const next = line ? `${line} ${word}` : word
      if (line && context.measureText(next).width > width) {
        if (row + lineHeight > CANVAS_HEIGHT) break
        context.fillText(line, x, row)
        row += lineHeight
        line = word
      } else line = next
    }
    context.fillText(line, x, row)
    row += lineHeight
  }
}
export async function renderMoodboardPng(canvas) {
  const target = document.createElement('canvas')
  target.width = CANVAS_WIDTH * 2
  target.height = CANVAS_HEIGHT * 2
  const ctx = target.getContext('2d')
  if (!ctx) throw new Error('Canvas export is not supported in this browser')
  ctx.scale(2, 2)
  ctx.fillStyle = canvas.backgroundColor
  ctx.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT)
  for (const el of canvas.elements) {
    if (el.kind === 'SWATCH') {
      ctx.fillStyle = el.color
      ctx.fillRect(el.x, el.y, el.width, el.height)
    } else if (el.kind === 'TEXT') {
      ctx.save()
      ctx.beginPath()
      ctx.rect(el.x, el.y, el.width, el.height)
      ctx.clip()
      ctx.fillStyle = el.color
      ctx.textBaseline = 'top'
      ctx.font = `650 ${el.fontSize}px system-ui, sans-serif`
      drawText(ctx, el.text, el.x + 5, el.y + 5, el.width - 10, el.fontSize)
      ctx.restore()
    } else if (el.kind === 'PHOTO') {
      if (!el.photoUrl) {
        ctx.fillStyle = '#EDF2F7'
        ctx.fillRect(el.x, el.y, el.width, el.height)
        ctx.fillStyle = '#526581'
        ctx.font = '15px system-ui'
        ctx.fillText('Photo unavailable', el.x + 15, el.y + 25)
        continue
      }
      // Same-origin project image endpoints; no canvas-tainting third-party URLs.
      try {
        const image = await loadImage(el.photoUrl)
        const scale = Math.max(el.width / image.naturalWidth, el.height / image.naturalHeight)
        const croppedWidth = el.width / scale
        const croppedHeight = el.height / scale
        const offsetX = (image.naturalWidth - croppedWidth) / 2
        const offsetY = (image.naturalHeight - croppedHeight) / 2
        ctx.drawImage(image, offsetX, offsetY, croppedWidth, croppedHeight,
          el.x, el.y, el.width, el.height)
      } catch {
        ctx.fillStyle = '#EDF2F7'
        ctx.fillRect(el.x, el.y, el.width, el.height)
        ctx.fillStyle = '#526581'
        ctx.font = '15px system-ui'
        ctx.fillText('Photo unavailable', el.x + 15, el.y + 25)
      }
    }
  }
  return new Promise((resolve, reject) => {
    target.toBlob((blob) => blob ? resolve(blob) : reject(new Error('PNG rendering failed')), 'image/png')
  })
}

export function downloadPng(blob, title) {
  const url = URL.createObjectURL(blob)
  const anchor = document.createElement('a')
  anchor.href = url
  anchor.download = `${String(title || 'moodboard').toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/^-|-$/g, '') || 'moodboard'}.png`
  document.body.appendChild(anchor)
  anchor.click()
  anchor.remove()
  window.setTimeout(() => URL.revokeObjectURL(url), 1000)
}
