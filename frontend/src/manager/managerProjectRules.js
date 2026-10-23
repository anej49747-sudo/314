export const UNCATEGORISED = 'Uncategorised'

export function categoryDisplayName(project) {
  return project.categoryName || UNCATEGORISED
}

export function filterManagerProjects(projects, { query = '', category = 'all', status = 'all' } = {}) {
  const keyword = query.trim().toLocaleLowerCase()
  return projects.filter((project) => {
    if (keyword && !`${project.title} ${project.designerName}`.toLocaleLowerCase().includes(keyword)) return false
    if (category === 'uncategorised' && project.categoryId != null) return false
    if (category !== 'all' && category !== 'uncategorised' && String(project.categoryId) !== String(category)) return false
    if (status !== 'all' && project.status !== status) return false
    return true
  })
}

export function managerProjectPayload(values) {
  const title = values.title?.trim() ?? ''
  const description = values.description?.trim() ?? ''
  if (!title || title.length > 160) throw new Error('Enter a project title of 1–160 characters.')
  if (!description || description.length > 3000) throw new Error('Enter a description of 1–3000 characters.')
  if (!['CURRENT', 'COMPLETED'].includes(values.status)) throw new Error('Choose a valid project status.')
  const categoryId = values.categoryId == null || values.categoryId === 'uncategorised' ? null : values.categoryId
  if (categoryId !== null && (!Number.isSafeInteger(categoryId) || categoryId <= 0)) {
    throw new Error('Choose an existing project category.')
  }
  return { title, description, categoryId, status: values.status }
}

export function managerProjectFailureMessage(error) {
  const status = error.response?.status
  if (status === 404) return 'This project is no longer available for correction.'
  if (status === 400) return error.response?.data?.message || 'Please check the project information.'
  if (status === 401 || status === 403) return 'Platform Manager access is required to edit projects.'
  return 'Unable to save the project changes. Please try again.'
}
