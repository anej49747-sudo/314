export const PROJECT_IMAGE_LIMIT = 6
export const PROJECT_IMAGE_MAX_BYTES = 5 * 1024 * 1024
export const PROJECT_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp']

export function buildProjectDetails(values) {
  return {
    title: (values.title ?? '').trim(),
    description: (values.description ?? '').trim(),
    categoryId: values.categoryId,
    status: values.status,
  }
}

export function validateProjectImages(files) {
  if (files.length > PROJECT_IMAGE_LIMIT) return 'Select no more than 6 photos.'
  for (const file of files) {
    if (!PROJECT_IMAGE_TYPES.includes(file.type)) return 'Photos must be JPG, PNG or WebP files.'
    if (file.size === 0 || file.size > PROJECT_IMAGE_MAX_BYTES)
      return 'Each photo must be between 1 byte and 5 MB.'
  }
  return ''
}

export function projectCreationFailureMessage(error) {
  const status = error.response?.status
  if (status === 400 || status === 413) return error.response?.data?.message || 'Check the project details and photos.'
  if (status === 401 || status === 403) return 'Your designer session is no longer available. Please sign in again.'
  return 'Unable to create your project. Please try again.'
}
