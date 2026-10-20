export function withoutDeletedProject(projects, deletedId) {
  return projects.filter((project) => String(project.id) !== String(deletedId))
}

export function projectDeletionFailureMessage(error) {
  if (error.response?.status === 404)
    return 'This project is no longer available in your portfolio.'
  if ([401, 403].includes(error.response?.status))
    return 'Your designer session is no longer available. Please sign in again.'
  return 'Unable to delete this project. Please try again.'
}
