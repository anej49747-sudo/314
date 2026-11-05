/** Display rules shared by Projects, Favourites, Details and Preferences.
 * The catalogue is manager-defined; no UI code should invent size boundaries.
 */
function readableArea(value) {
  const n = Number(value)
  return Number.isFinite(n) ? `${new Intl.NumberFormat('en-SG', { maximumFractionDigits: 2 }).format(n)} m²` : null
}

export function sizeRangeLabel(range) {
  if (!range) return ''
  const min = range.min_sqm ?? range.minSqm
  const max = range.max_sqm ?? range.maxSqm
  const lower = min == null || Number(min) === 0 ? null : readableArea(min)
  const upper = max == null ? null : readableArea(max)
  const bounds = lower && upper ? `${lower} to under ${upper}`
    : lower ? `${lower} and above` : upper ? `Under ${upper}` : 'Any floor area'
  return range.name ? `${range.name} — ${bounds}` : bounds
}

export function projectAttributes(project, { secondary = false } = {}) {
  if (!project) return []
  const metadata = project.metadata || project
  const styles = metadata.styles || []
  const primary = styles.find(style => style.role === 'PRIMARY')?.name
  const additional = secondary ? styles.filter(style => style.role === 'SECONDARY').map(style => style.name).filter(Boolean) : []
  const area = metadata.floorAreaSqm == null ? null : readableArea(metadata.floorAreaSqm)
  return [
    { key: 'category', label: 'Category', value: project.categoryName },
    { key: 'style', label: 'Design style', value: primary },
    ...(additional.length ? [{ key: 'secondary', label: 'Other styles', value: additional.join(', ') }] : []),
    { key: 'property', label: 'Property type', value: metadata.propertyTypeName },
    { key: 'area', label: 'Floor area', value: area },
  ].filter(attribute => attribute.value != null && String(attribute.value).trim() !== '')
}

export function activeProjectFilters({ query, categoryId, categoryName, styleIds = [], styles = [], propertyTypeId, types = [], sizeRangeId, sizes = [] }) {
  const result = []
  if (query) result.push({ key: 'title', label: `Title: “${query}”` })
  if (categoryId != null) result.push({ key: 'category', label: `Category: ${categoryName || 'Selected category'}` })
  for (const id of styleIds) result.push({ key: `style-${id}`, label: `Style: ${styles.find(x => String(x.id) === String(id))?.name || 'Selected style'}`, id })
  if (propertyTypeId != null) result.push({ key: 'property', label: `Property: ${types.find(x => String(x.id) === String(propertyTypeId))?.name || 'Selected property'}` })
  if (sizeRangeId != null) result.push({ key: 'size', label: `Area: ${sizeRangeLabel(sizes.find(x => String(x.id) === String(sizeRangeId))) || 'Selected range'}` })
  return result
}
