export function buildCompanyInformationPayload(values) {
  const companyName = values.companyName?.trim() ?? ''
  const address = values.address?.trim() ?? ''
  const description = values.description?.trim() ?? ''

  if (!companyName || companyName.length > 150) {
    throw new Error('Enter a company name with 1–150 characters.')
  }
  if (address.length > 300) throw new Error('Company address must be 300 characters or fewer.')
  if (description.length > 3000) throw new Error('Company description must be 3000 characters or fewer.')
  return { companyName, address, description }
}

export function companyInformationFailureMessage(error) {
  const status = error.response?.status
  if (status === 400) return error.response?.data?.message || 'Please correct the company details.'
  if (status === 401 || status === 403) return 'Your account cannot update company information.'
  return 'Unable to save company information. Please try again.'
}
