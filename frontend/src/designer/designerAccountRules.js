export function buildDesignerPayload(values) {
  return {
    salutation: values.salutation || null,
    name: values.name.trim(),
    email: values.email.trim(),
  }
}

export function designerCreationFailureMessage(failure) {
  switch (failure?.response?.status) {
    case 409: return 'An account with this email already exists.'
    case 400: return 'Please correct the highlighted fields and try again.'
    case 403: return 'Your security session has expired or access is denied. Refresh or log in again.'
    case 401: return 'Please log in as a User Admin.'
    default: return 'Unable to create the designer account. Please try again.'
  }
}
