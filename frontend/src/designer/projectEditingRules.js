import { PROJECT_IMAGE_LIMIT, buildProjectDetails,
  validateProjectImages } from './projectCreationRules.js'

export function buildUpdatedProjectDetails(values, keepImageIds) {
  return { ...buildProjectDetails(values), keepImageIds: [...keepImageIds] }
}

export function validateEditedImages(keepImageIds, files) {
  if (new Set(keepImageIds).size !== keepImageIds.length) return 'Invalid existing photo selection.'
  if (keepImageIds.length + files.length > PROJECT_IMAGE_LIMIT)
    return 'Keep and upload no more than 6 photos in total.'
  return validateProjectImages(files)
}

export function projectEditingFailureMessage(error) {
  if (error.response?.status === 404) return 'This project is no longer available in your portfolio.'
  if (error.response?.status === 400 || error.response?.status === 413)
    return error.response?.data?.message || 'Check the project details and photos.'
  if ([401, 403].includes(error.response?.status))
    return 'Your designer session is no longer available. Please sign in again.'
  return 'Unable to save your changes. Please try again.'
}
