/** Pure, defensive selectors for the visitor homepage. No demo content is injected. */
export function homepageArticles(entries, limit = 3) {
  return (Array.isArray(entries) ? entries : [])
    .filter((entry) => entry?.id != null && entry?.title)
    .slice().sort((a, b) => {
      const dated = (Date.parse(b.publishedAt || '') || 0) - (Date.parse(a.publishedAt || '') || 0)
      return dated || Number(b.id) - Number(a.id)
    }).slice(0, limit)
}

export function homepageRecognisedDesigners(designers, awards, limit = 3) {
  if (!Array.isArray(designers) || !Array.isArray(awards)) return []
  const counts = new Map()
  for (const award of awards) {
    if (award?.designerId == null) continue
    const key = String(award.designerId)
    counts.set(key, (counts.get(key) || 0) + 1)
  }
  return designers.filter((designer) => designer?.id != null && counts.has(String(designer.id)))
    .slice().sort((a, b) => (counts.get(String(b.id)) - counts.get(String(a.id))) ||
      String(a.name || '').localeCompare(String(b.name || '')))
    .slice(0, limit)
}

export function homepageFeaturedDetails(featured, projects) {
  if (!featured?.projectId) return null
  const project = (Array.isArray(projects) ? projects : [])
    .find((item) => String(item.id) === String(featured.projectId))
  return {
    id: featured.projectId,
    title: featured.projectTitle || project?.title || 'Featured renovation project',
    designerName: project?.designer?.name || '',
    category: project?.categoryName || 'Uncategorised',
    description: project?.description || '',
    status: project?.status || '',
    coverUrl: featured.coverUrl || null,
  }
}
