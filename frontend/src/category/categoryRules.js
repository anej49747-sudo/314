export function buildCategoryPayload(values) {
  const name = values.name?.normalize('NFKC').trim() ?? ''
  if (!name || name.length > 100) throw new Error('Enter a category name with 1–100 characters.')
  return { name }
}

export function categoryCreationFailureMessage(error) {
  const status = error.response?.status
  if (status === 409) return 'A category with this name already exists.'
  if (status === 400) return error.response?.data?.message || 'Enter a valid category name.'
  if (status === 401 || status === 403) return 'Your account cannot create categories.'
  return 'Unable to create the category. Please try again.'
}

export function categoryDeleteFailureMessage(error) {
  const status = error.response?.status
  if (status === 404) return 'This category no longer exists. Refresh the list.'
  if (status === 401 || status === 403) return 'Platform Manager access is required to delete categories.'
  return 'Unable to delete this category. Please try again.'
}
