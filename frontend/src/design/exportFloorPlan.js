/** Render the current inline SVG plan to a downloadable PNG without external services. */
export async function exportFloorPlan(svg, title) {
  if (!svg) throw new Error('No floor plan to export')
  const copy = svg.cloneNode(true)
  copy.setAttribute('xmlns', 'http://www.w3.org/2000/svg')
  copy.setAttribute('width', '1200')
  copy.setAttribute('height', '900')
  // Keep export self-contained: the SVG uses only inline fill/stroke/text; no external images.
  const content = new XMLSerializer().serializeToString(copy)
  const url = URL.createObjectURL(new Blob([content], { type: 'image/svg+xml;charset=utf-8' }))
  try {
    const picture = new Image()
    await new Promise((resolve, reject) => {
      picture.onload = resolve
      picture.onerror = reject
      picture.src = url
    })
    const canvas = document.createElement('canvas')
    canvas.width = 2400
    canvas.height = 1800
    const ctx = canvas.getContext('2d')
    if (!ctx) throw new Error('Canvas export unavailable')
    ctx.fillStyle = '#f4f7fa'
    ctx.fillRect(0, 0, canvas.width, canvas.height)
    ctx.drawImage(picture, 0, 0, canvas.width, canvas.height)
    const name = (title || 'renovation-design').toLowerCase().replace(/[^a-z0-9]+/g, '-')
      .replace(/^-|-$/g, '') || 'renovation-design'
    const download = document.createElement('a')
    download.href = canvas.toDataURL('image/png')
    download.download = `${name}.png`
    document.body.append(download)
    download.click()
    download.remove()
  } finally { URL.revokeObjectURL(url) }
}
