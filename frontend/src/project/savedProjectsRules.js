export function savedProjectCount(count) {
  return `${count} saved ${count === 1 ? 'project' : 'projects'}`
}

export function withoutSavedProject(projects, removedId) {
  return projects.filter((project) => String(project.id) !== String(removedId))
}
