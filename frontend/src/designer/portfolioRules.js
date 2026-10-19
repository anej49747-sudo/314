export const PORTFOLIO_FILTERS = ['ALL', 'CURRENT', 'COMPLETED']

export function portfolioSummary(projects) {
  return {
    total: projects.length,
    current: projects.filter((project) => project.status === 'CURRENT').length,
    completed: projects.filter((project) => project.status === 'COMPLETED').length,
  }
}

export function filteredPortfolio(projects, filter) {
  return filter === 'ALL' ? projects : projects.filter((project) => project.status === filter)
}

export function portfolioStatusLabel(status) {
  return status === 'CURRENT' ? 'Current' : status === 'COMPLETED' ? 'Completed' : 'Unknown'
}
